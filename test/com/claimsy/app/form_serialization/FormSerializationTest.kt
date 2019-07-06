package com.claimsy.app.form_serialization

import com.claimsy.app.ServerTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs

data class Fizzer(
    var fizziness: String? = null,
    var temperature: Int? = null
)

data class WizzBanger(var id: String? = null, var name: String? = null, var fizzer: Fizzer? = null)

data class Widget(
    var name: String? = null,
    val jimbo: List<String>? = emptyList(),
    val age: Int? = null
)

class FormParamsToJsonTranslatorTest {
    private val translator = FormParamsToJsonTranslator()

    @Test
    @DisplayName("it creates nested json Objects when params follow naming convention of outerObj[innerObj][field]")
    fun createsNestedJson() {
        val params =
            "notConventionNameParam=woohoo&" +
                    "car[make]=BMW&" +
                    "car[model]=330i&" +
                    "car[features][]=windows&" + "car[features][]=doors&" +
                    "car[engine][id]=12345&" +
                    "car[engine][volume]=2276cc&" +
                    "car[engine][cylinders]=4"


        val result = translator.jsonFromFormBody(params)

        val expected = JsonNodeFactory.instance.objectNode().apply {
            put("notConventionNameParam", "woohoo")
            putArray("features").add("windows").add("doors")
            put("make", "BMW")
            put("model", "330i")
            putObject("engine")
                .put("id", "12345")
                .put("volume", "2276cc")
                .put("cylinders", "4")
        }

        expectThat(result.toJson()).isSameJsonAs(expected.toJson())
    }

    @Test
    fun `it returns an empty json object for an empty body`(){
        val result = translator.jsonFromFormBody("")
        expectThat(result.toJson()).isSameJsonAs("{}")
    }

    @Test
    fun `it can return arrays of one element`() {
        val params = "zoo[animals][]=elephant"

        val result = translator.jsonFromFormBody(params)

        expectThat(result.toJson()).isSameJsonAs(
            """
            { 
                "animals": ["elephant"]
            }
        """.trimIndent()
        )
    }

    @Test
    fun `it creates deeply nested json`() {
        val params =
            "icecream[cone][wrapper]=paper&" +
                    "icecream[cone][flavors][][name]=vanilla&" +
                    "icecream[cone][flavors][][name]=chocolate&" +
                    "icecream[topping][sprinkles][type]=rainbow"


        val result = translator.jsonFromFormBody(params)

        expectThat(result.toJson()).isSameJsonAs(
            """
            { 
                "cone": { 
                        "wrapper": "paper", 
                        "flavors":[{"name":"vanilla"}, {"name":"chocolate"}] 
                },
             "topping": {"sprinkles": {"type": "rainbow"} }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `always interprets repeated non-rails style param names as as arrays`() {
        val params = "discount=50%25&" + "car[features][]=windows&" + "car[features][]=doors&" +
                "dealerships=SuperAuto&" + "dealerships=MegaAuto"


        val result = translator.jsonFromFormBody(params)

        val expected = JsonNodeFactory.instance.objectNode().apply {
            put("discount", "50%")
            putArray("features").add("windows").add("doors")
            putArray("dealerships").add("SuperAuto").add("MegaAuto")
        }

        expectThat(result.toJson()).isSameJsonAs(expected.toJson())
    }
}

private fun Assertion.Builder<String>.isSameJsonAs(expected: String): Assertion.Builder<String> =
    assert(description = "is same json", expected = expected) {
        val matcher = sameJSONAs(expected)
        when (matcher.matches(it)) {
            true -> pass()
            false -> fail(actual = it, description = "json did not match")
        }
    }


private fun Any?.toJson(): String {
    if (this == null) {
        fail("expected object not to be null")
    }
    return ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(JavaTimeModule())
    }.writeValueAsString(this)
}

class FormSerializationTest : ServerTest() {
    @Test
    fun `processes basic form data into json for the controller`() {
        RestAssured.given()
            .contentType(ContentType.URLENC)
            .formParam("name", "wizzy woozy")
            .formParam("jimbo", "jambo")
            .formParam("jimbo", 12345)
            .formParam("age", 21)
            .When()
            .post("/widget")
            .then()
            .statusCode(201)
            .body(
                sameJSONAs(
                    """
                        {
                            "name": "wizzy woozy",
                            "jimbo":["jambo","12345"],
                            "age": 21
                        }
                        """.trimIndent()
                )
            )
    }

    @Test
    fun `deals with nested objects like Rack & Rails does`() {
        val result = RestAssured.given()
            .contentType(ContentType.URLENC)
            .accept(ContentType.JSON)
            .formParam("id", "ABC123U&ME_GRL")
            .formParam("wizzbanger[name]", "Whiz Kid") // Now do we need the naming convention???
            .formParam("wizzbanger[fizzer][fizziness]", "really fizzy") // Now do we need the naming convention???
            .formParam("wizzbanger[fizzer][temperature]", 98) // Now do we need the naming convention???
            .When()
            .post("/wizz-banger")
            .then()
            .statusCode(201)
            .body(
                sameJSONAs(
                    """
                        {
                            "id":"ABC123U&ME_GRL",
                            "name":"Whiz Kid",
                            "fizzer":{"fizziness":"really fizzy", "temperature": 98}
                        }
                    """.trimIndent()
                )
            )
            .extract()
            .toObject<WizzBanger>()

        expectThat(result).isEqualTo(
            WizzBanger(
                id = "ABC123U&ME_GRL",
                name = "Whiz Kid",
                fizzer = Fizzer(fizziness = "really fizzy", temperature = 98)
            )
        )
    }


}

