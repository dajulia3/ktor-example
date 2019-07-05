package com.claimsy.app.form_serialization

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.POJONode
import io.ktor.http.decodeURLPart
import java.io.InputStream

internal class FormParamsToJsonTranslator {
    private val nodeFactory = JsonNodeFactory.instance

    fun jsonFromFormBody(formBody: String): ObjectNode? {
        val params = makeParams()

        val keyValPairs = formBody.split("&")
        val keysAlreadyPresent: HashSet<String> = HashSet()
        keyValPairs.forEach { pair ->
            val keyVal = pair.split("=").map { it.decodeURLPart() }

            if (keyVal.size != 2) {
                throw RuntimeException("invalid form body")
            }
            val (name, value) = keyVal
            if (isRailsStyleParam(name)) {
                normalizeParamsWithRootObjectUnwrapped(params, name, value, PARAMS_DEPTH_LIMIT)
                keysAlreadyPresent.add(name)
            } else if (keysAlreadyPresent.contains(name)) {
                val valAlreadyPresent = params.get(name)
                val arrayNode = params.putArray(name)
                if(valAlreadyPresent.isArray){
                    arrayNode.addAll(valAlreadyPresent as ArrayNode)
                }else{
                    arrayNode.add(valAlreadyPresent)
                }
                arrayNode.add(nodeFactory.pojoNode(value))
            }else{
                params.putPOJO(name,value)
                keysAlreadyPresent.add(name)
            }
        }

        return params
    }

    private fun normalizeParamsWithRootObjectUnwrapped(params: ObjectNode, name: String, value: Any, depth: Int) {
        val nameForUnwrappedRootObject = name.substring(name.indexOf("["))
        normalizeParams(params, nameForUnwrappedRootObject, value, depth)
    }

    private fun isRailsStyleParam(fullParamName: String): Boolean {
        return fullParamName.matches("\\A.*\\[.*\\].*".toRegex())
    }


    private fun normalizeParams(params: ObjectNode, name: String, value: Any?, depth: Int): JsonNode? {
        if (depth <= 0) {
            throw IllegalArgumentException("Exceeded the form params depth limit of $PARAMS_DEPTH_LIMIT")
        }

        val firstNameMatchResult = Regex("\\A[\\[\\]]*([^\\[\\]]+)\\]*").find(name, 0)

        var k = ""
        var indexAfterMatch = name.length

        if (firstNameMatchResult?.groups != null) {
            val firstNameMatchGroup = firstNameMatchResult.groups[1]
            k = firstNameMatchGroup!!.value
            val fullMatchGroup = firstNameMatchResult.groups[0]
            indexAfterMatch = fullMatchGroup!!.range.endInclusive + 1
        }
        val after = name.substring(indexAfterMatch)


        if (k.isEmpty()) {
            return if (value != null && name == "[]") {
                nodeFactory.arrayNode().add(nodeFactory.pojoNode(value))
            } else {
                null
            }
        }


        val matchesFirstWordAfterArray = Regex("^\\[\\]\\[([^\\[\\]]+)\\]$").find(after, 0)
        val matchesArrayAfterArray = Regex("^\\[\\](.+)$").find(after, 0)

        if (after == "") {
            params.set(k, nodeFactory.pojoNode(value))
        } else if (after == "[") {
            val jsonNode = params.get(name)
            if (jsonNode == null) {
                params.putPOJO(name, value)
            }
            if (jsonNode != null) {
                if (jsonNode.isObject) {
                    (jsonNode as ObjectNode).putPOJO(name, value)
                }
            }
        } else if (after == "[]") {
            ensureCurrentParamIsArray(params, name, k)
            (params.get(k) as ArrayNode).add(nodeFactory.pojoNode(value))
        } else if (matchesArrayAfterArray != null && !matchesFirstWordAfterArray!!.groups.isEmpty()) {
            normalizeParamsForElementOfArray(params, name, value, depth, k, matchesFirstWordAfterArray)

        } else if (matchesArrayAfterArray != null && !matchesArrayAfterArray.groups.isEmpty()) {
            normalizeParamsForElementOfArray(params, name, value, depth, k, matchesArrayAfterArray)
        } else {
            ensureCurrentParamIsObject(params, name, k)
            params.set(k, normalizeParams(params.get(k) as ObjectNode, after, value, depth - 1))
        }

        return params
    }

    private fun normalizeParamsForElementOfArray(
        params: ObjectNode,
        name: String,
        v: Any?,
        depth: Int,
        k: String,
        matchesFirstWordAfterArray: MatchResult
    ) {
        val childKey = matchesFirstWordAfterArray.groups[1]!!.value
        ensureCurrentParamIsArray(params, name, k)
        val jsonNode = params.get(k) as ArrayNode

        if (jsonNode.size() > 0 && getLastEltOfArrayNode(jsonNode).isObject &&
            !paramsHashHasKey(getLastEltOfArrayNode(jsonNode) as ObjectNode, childKey))
        {
            normalizeParams(getLastEltOfArrayNode(jsonNode) as ObjectNode, childKey, v, depth - 1)
        } else {
            (params.get(k) as ArrayNode).add(normalizeParams(makeParams(), childKey, v, depth - 1))
        }
    }

    private fun makeParams(): ObjectNode {
        return nodeFactory.objectNode()
    }

    private fun getLastEltOfArrayNode(jsonNode: ArrayNode): JsonNode {
        return jsonNode.get(jsonNode.size() - 1)
    }

    private fun paramsHashHasKey(lastEltOfParam: ObjectNode, key: String): Boolean {
        val matchesKey = Regex("\\[\\]").find(key, 0)

        if (matchesKey?.groups?.isEmpty() == true) {
            return false
        }

        var map: JsonNode = lastEltOfParam.deepCopy() //basically a map
        val split = key.split("[\\[\\]]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (part in split) {
            if (part == "") {
                continue
            }
            if (!map.isObject && map.get(part) == null) {
                return false
            }
            map = map.get(part)
        }

        return true
    }

    private fun ensureCurrentParamIsArray(params: ObjectNode, name: String, k: String) {
        if (params.get(k) == null) {
            params.putArray(k)
        }
        if (!params.get(k).isArray) {
            throw ParameterTypeError("expected " + name + "to be an array type, but was" + params.get(k).nodeType)
        }
    }

    private fun ensureCurrentParamIsObject(params: ObjectNode, name: String, k: String) {
        if (params.get(k) == null) {
            params.putObject(k)
        }
        if (!params.get(k).isObject) {
            throw ParameterTypeError("expected " + name + "to be a Json Object type, but was " + params.get(k).nodeType)
        }
    }


    internal class ParameterTypeError(message: String) : RuntimeException(message)

    companion object {
        private const val PARAMS_DEPTH_LIMIT = 20
    }
}