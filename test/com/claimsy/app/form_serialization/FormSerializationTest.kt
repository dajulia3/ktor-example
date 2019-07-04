package com.claimsy.app.form_serialization

import com.claimsy.app.ServerTest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.Ignore
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs
import java.util.*

//embeddedServer(factory = CIO, environment = commandLineEnvironment(mergedArgs))

//class FormController {
//
//    @Post(uri = "/form", consumes = [MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED])
//    fun createWidget(widgetReq: HttpRequest<Widget>): Widget {
//        return widgetReq.body.get()
//    }
//
//    @Post(uri = "/rack-style-form-params", consumes = [MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED])
//    fun createFizzer(wizzBangerReq: HttpRequest<WizzBanger>): WizzBanger {
//        return wizzBangerReq.body.get()
//    }
//
//}

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


//class FormToJsonFilter(
//        val environment: ConversionService<ConversionService<*>>,
//        val serverConfiguration: HttpServerConfiguration) : HttpServerFilter {
//    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain?): Publisher<MutableHttpResponse<*>> {
//        val headers = request?.headers
//        if (headers.contentTypeIsFormUrlEncoded() &&
//                headers is MutableHttpHeaders && request.body.isPresent) {
//            headers.remove("Content-Type")
//            headers.add("Content-Type", MediaType.APPLICATION_JSON)
//
//            val decorator = FormEncodedToJsonRequestDecorator(request as NettyHttpRequest<LinkedHashMap<String, Any>>,
//                    environment = environment,
//                    serverConfiguration = serverConfiguration
//            )
//            return chain?.proceed(decorator) as Publisher<MutableHttpResponse<*>>
//        }
//
//
//        return chain?.proceed(request)!!
//    }
//
//    private fun HttpHeaders?.contentTypeIsFormUrlEncoded() : Boolean =
//            this?.contentType()?.get()?.name == MediaType.APPLICATION_FORM_URLENCODED
//
//    class FormEncodedToJsonRequestDecorator(val request: NettyHttpRequest<LinkedHashMap<String, Any>>,
//                                            val environment: ConversionService<ConversionService<*>>,
//                                            serverConfiguration: HttpServerConfiguration
//    ) : NettyHttpRequest<ObjectNode>(
//            request.nettyRequest,
//            request.channelHandlerContext,
//            environment,
//            serverConfiguration
//    ) {
//        override fun getCookies(): Cookies = request.cookies
//
//        override fun getMethod(): HttpMethod = request.method
//
//        override fun getUri() = request.uri
//
//        override fun getAttributes() = request.attributes
//
//        override fun getHeaders(): HttpHeaders = request.headers //TODO use this toType change the content type header
//
//        override fun getParameters(): HttpParameters = request.parameters
//
//        override fun getBody(): Optional<ObjectNode> {
//            return FormParamsToJsonTranslator().jsonFromFormBody(request.body.get())
//        }
//    }
//}

class FormParamsToJsonTranslatorTest {
    private val translator = FormParamsToJsonTranslator()

    @Test
    @DisplayName("it creates nested json Objects when params follow naming convention of outerObj[innerObj][field]")
    fun createsNestedJson() {
        val params = LinkedHashMap(
            listOf(
                "notConventionNameParam" to "woohoo",
                "car[make]" to "BMW",
                "car[model]" to "330i",
                "car[features][]" to listOf("windows", "doors"),
                "car[engine][id]" to listOf("12345"),
                "car[engine][volume]" to listOf("2276cc"),
                "car[engine][cylinders]" to listOf("4")
            ).toMap()
        )

        val result = translator.jsonFromFormBody(params).get()

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

    @Ignore
    fun `it works for arbitrary levels of nesting`() {
    }

    @Ignore
    fun `always interprets repeated names as arrays`() {
        val params = LinkedHashMap(
            listOf(
                "car[features]" to listOf("windows", "doors"),
                "dealerships" to listOf("SuperAuto", "MegaAuto"),
                "discount" to "50%"

            ).toMap()
        )

        val result = translator.jsonFromFormBody(params).get()

        val expected = JsonNodeFactory.instance.objectNode().apply {
            put("discount", "50%")
            putArray("features").add("windows").add("doors")
            putArray("dealerships").add("SuperAuto").add("MegaAuto")
        }

        expectThat(result.toJson()).isSameJsonAs(expected.toJson())
    }
}

private fun Assertion.Builder<String>.isSameJsonAs(expected: String): Assertion.Builder<String> =
    assert("is same json") {
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
    fun `still works with an actual json request`() {
        val requestJson = """{"name": "wizzy woozy", "jimbo":["jambooo", "jingo"], "age": 999}"""
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(requestJson)
            .When()
            .post("/test/form")
            .then()
            .statusCode(200)
            .body(sameJSONAs(requestJson))
    }

    fun `processes basic form data into json for the controller`() {
        RestAssured.given()
            .contentType(ContentType.URLENC)
            .formParam("name", "wizzy woozy")
            .formParam("jimbo", "jambo")
            .formParam("jimbo", 12345)
            .formParam("age", 21)
            .When()
            .post("/test/form")
            .then()
            .statusCode(200)
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
            .post("/test/rack-style-form-params")
            .then()
            .statusCode(200)
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

