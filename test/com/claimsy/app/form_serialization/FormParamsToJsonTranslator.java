package com.claimsy.app.form_serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import kotlin.text.MatchGroup;
import kotlin.text.MatchResult;
import kotlin.text.Regex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


//class FormCodec : io.micronaut.http.codec.MediaTypeCodec {
//
//}

class FormParamsToJsonTranslator {
    private static final int PARAMS_DEPTH_LIMIT = 20;
    private JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    Optional<ObjectNode> jsonFromFormBody(LinkedHashMap<String, Object> formBody) {
        ObjectNode params = makeParams();
        formBody.entrySet().forEach(entry -> {

            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof List && ((List) value).size() > 1) {
                if (isRailsStyleParam(name)) {
                    ((List<String>) value).stream().forEach(val -> {
                        normalizeParamsWithRootObjectUnwrapped(params, name, val, PARAMS_DEPTH_LIMIT);
                    });
                } else {
                    List<ValueNode> arrayElementNodes = ((List<String>) value).stream().map(val ->
                            nodeFactory.pojoNode(val)
                    ).collect(Collectors.toList());

                    params.putArray(name).addAll(arrayElementNodes);
                }
            } else if (value instanceof List && ((List) value).size() == 1) {
                if (isRailsStyleParam(name)) {
                    normalizeParamsWithRootObjectUnwrapped(params, name, ((List) value).get(0), PARAMS_DEPTH_LIMIT);
                } else {
                    params.putPOJO(name, ((List) value).get(0));
                }
            } else {
                if (isRailsStyleParam(name)) {
                    normalizeParamsWithRootObjectUnwrapped(params, name, value, PARAMS_DEPTH_LIMIT);
                } else {
                    params.putPOJO(name, value);
                }

            }

        });

        return Optional.of(params);
    }

    private void normalizeParamsWithRootObjectUnwrapped(ObjectNode params, String name, Object value, int depth) {
        String nameForUnwrappedRootObject = name.substring(name.indexOf("["));
        normalizeParams(params, nameForUnwrappedRootObject, value, depth);
    }

    private boolean isRailsStyleParam(String fullParamName) {
        return fullParamName.matches("\\A.*\\[.*\\].*");
    }


    JsonNode normalizeParams(ObjectNode params, String name, Object value, int depth) {
        if (depth <= 0) {
            throw new IllegalArgumentException("Exceeded the form params depth limit of " + PARAMS_DEPTH_LIMIT);
        }

        MatchResult firstNameMatchResult = new Regex("\\A[\\[\\]]*([^\\[\\]]+)\\]*").find(name, 0);

        String k = "";
        int indexAfterMatch = name.length();

        if (firstNameMatchResult != null && firstNameMatchResult.getGroups() != null) {
            MatchGroup firstNameMatchGroup = firstNameMatchResult.getGroups().get(1);
            k = firstNameMatchGroup.getValue();
            MatchGroup fullMatchGroup = firstNameMatchResult.getGroups().get(0);
            indexAfterMatch = fullMatchGroup.getRange().getEndInclusive() + 1;
        }
        String after = name.substring(indexAfterMatch);


        if (k.isEmpty()) {
            if (value != null && name.equals("[]")) {
                return nodeFactory.arrayNode().add(nodeFactory.pojoNode(value));
            } else {
                return null;
            }
        }


        MatchResult matchesFirstWordAfterArray = new Regex("^\\[\\]\\[([^\\[\\]]+)\\]$").find(after, 0);
        MatchResult matchesArrayAfterArray = new Regex("^\\[\\](.+)$").find(after, 0);

        if (after.equals("")) {
            params.set(k, nodeFactory.pojoNode(value));
        } else if (after.equals("[")) {
            JsonNode jsonNode = params.get(name);
            if (jsonNode == null) {
                params.putPOJO(name, value);
            }
            if (jsonNode != null) {
                if (jsonNode.isObject()) {
                    ((ObjectNode) jsonNode).putPOJO(name, value);
                }
            }
        } else if (after.equals("[]")) {
            ensureCurrentParamIsArray(params, name, k);
            ((ArrayNode) params.get(k)).add(nodeFactory.pojoNode(value));
        } else if (matchesArrayAfterArray != null && !matchesFirstWordAfterArray.getGroups().isEmpty()) {
            normalizeParamsForElementOfArray(params, name, value, depth, k, matchesFirstWordAfterArray);

        } else if (matchesArrayAfterArray != null && !matchesArrayAfterArray.getGroups().isEmpty()) {
            normalizeParamsForElementOfArray(params, name, value, depth, k, matchesArrayAfterArray);
        } else {
            ensureCurrentParamIsObject(params, name, k);
            params.set(k, normalizeParams((ObjectNode) params.get(k), after, value, depth - 1));
        }

        return params;
    }

    private void normalizeParamsForElementOfArray(ObjectNode params, String name, Object v, int depth, String k, MatchResult matchesFirstWordAfterArray) {
        String childKey = matchesFirstWordAfterArray.getGroups().get(1).getValue();
        ensureCurrentParamIsArray(params, name, k);
        JsonNode lastEltOfParam = getLastEltOfArrayNode((ArrayNode) params.get(k));
        if (lastEltOfParam.isObject() && !paramsHashHasKey((ObjectNode) lastEltOfParam, childKey)) {
            normalizeParams((ObjectNode) getLastEltOfArrayNode((ArrayNode) params.get(k)), childKey, v, depth - 1);
        } else {
            ((ArrayNode) params.get(k)).add(normalizeParams(makeParams(), childKey, v, depth - 1));
        }
    }

    private ObjectNode makeParams() {
        return nodeFactory.objectNode();
    }

    private JsonNode getLastEltOfArrayNode(ArrayNode jsonNode) {
        return jsonNode.get(jsonNode.size() - 1);
    }

    private boolean paramsHashHasKey(ObjectNode lastEltOfParam, String key) {
        MatchResult matchesKey = new Regex("\\[\\]").find(key, 0);

        if (matchesKey.getGroups().isEmpty()) {
            return false;
        }

        // { h => { foo

        JsonNode map = lastEltOfParam.deepCopy(); //basically a map
        String[] split = key.split("[\\[\\]]+");
        for (String part : split) {
            if (part.equals("")) {
                continue;
            }
            if (!map.isObject() && map.get(part) == null) {
                return false;
            }
            map = map.get(part);
        }

        return true;
    }

    private void ensureCurrentParamIsArray(ObjectNode params, String name, String k) {
        if (params.get(k) == null) {
            params.putArray(k);
        }
        if (!params.get(k).isArray()) {
            throw new ParameterTypeError("expected " + name + "to be an array type, but was" + params.get(k).getNodeType());
        }
    }

    private void ensureCurrentParamIsObject(ObjectNode params, String name, String k) {
        if (params.get(k) == null) {
            params.putObject(k);
        }
        if (!params.get(k).isObject()) {
            throw new ParameterTypeError("expected " + name + "to be a Json Object type, but was " + params.get(k).getNodeType());
        }
    }


    static class ParameterTypeError extends RuntimeException {
        ParameterTypeError(String message) {
            super(message);
        }
    }
}