package com.claimsy.app

import io.ktor.server.engine.ApplicationEngine
import io.restassured.RestAssured
import io.restassured.response.ResponseBodyExtractionOptions
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeAll
import java.util.concurrent.TimeUnit

abstract class ServerTest {

    protected fun RequestSpecification.When(): RequestSpecification {
        return this.`when`()
    }

    protected inline fun <reified T> ResponseBodyExtractionOptions.toObject(): T {
        return this.`as`(T::class.java)
    }

    companion object {

        private var serverStarted = false

        private lateinit var server: ApplicationEngine

        @BeforeAll
        @JvmStatic
        fun startServer() {
            if (!serverStarted) {
                val port = 8888

                server = startServer(port = port, wait = false)
                serverStarted = true

                RestAssured.baseURI = "http://0.0.0.0"
                RestAssured.port = port
                Runtime.getRuntime().addShutdownHook(Thread { server.stop(0, 0, TimeUnit.SECONDS) })
            }
        }

    }

}
