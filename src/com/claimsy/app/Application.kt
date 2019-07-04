package com.claimsy.app

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.event.Level

object Main {
    @UseExperimental(KtorExperimentalAPI::class)
    @JvmStatic
    fun main(args: Array<String>) {
        startServer(port = 8080, wait = true, args = args)
    }
}

fun startServer(port: Int?, wait: Boolean, args: Array<String> = emptyArray()): CIOApplicationEngine {
    var mergedArgs = args
    if (port != null) {
        mergedArgs = arrayOf("-port=$port") + args
    }
    val server = embeddedServer(factory = CIO, environment = commandLineEnvironment(mergedArgs))

    server.start(wait = wait)
    return server
}

//fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule())
            disableDefaultTyping()
        }

    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }


        post("/wizz-banger") {
            val wizzBanger: WizzBanger = call.receive()
            call.respond(HttpStatusCode.Created, wizzBanger)
        }

//        get("/html-thymeleaf") {
//            call.respond(ThymeleafContent("index", mapOf("user" to ThymeleafUser(1, "user1"))))
//        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        install(StatusPages) {
            exception<AuthenticationException> { cause ->
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }

        install(CallLogging){
            level = Level.INFO
        }
    }
}

data class ThymeleafUser(val id: Int, val name: String)

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

//@GraalReflectable
data class Fizzer(
    var fizziness: String,
    var temperature: Int
)

//@GraalReflectable
data class WizzBanger(var id: String, var name: String, var fizzer: Fizzer)
