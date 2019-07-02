package com.claimsy.app

import io.restassured.RestAssured.get
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs

class WidgetResourceTest : ServerTest() {

    @Test
    fun testHelloWorld() {
        // when
        val retrieved = get("/")
            .then().statusCode(200)
            .body(equalTo("HELLO WORLD!"))

    }

    @Test
    fun testJsonContent() {
        val jsonBody = """
            {
                "id":"ABC123U&ME_GRL",
                "name":"Whiz Kid",
                "fizzer":{"fizziness":"really fizzy", "temperature": 98}
            } 
            """.trimIndent()
        val retrieved = given().body(
            jsonBody
        ).contentType("application/json").When().post("/wizz-banger")
            .then().statusCode(201)
            .body(
                sameJSONAs(
                    jsonBody
                )
            )

    }
}