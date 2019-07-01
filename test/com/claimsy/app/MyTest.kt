package com.claimsy.app

import io.restassured.RestAssured.get
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import strikt.api.expectThat

class WidgetResourceTest: ServerTest() {

    @Test
    fun testCreateWidget() {
        // when
        val retrieved = get("/")
            .then().statusCode(200)
            .body(equalTo("HELLO WORLD!"))

    }
}