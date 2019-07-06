package com.claimsy.app

import com.claimsy.app.form_serialization.ActiveFormUrlEncodedToContentTypeConverter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
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
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.event.Level
import java.util.*

object Main {
    @UseExperimental(KtorExperimentalAPI::class)
    @JvmStatic
    fun main(args: Array<String>) {
        startServer(port = 8080, wait = true, args = args)
    }
}

fun startServer(port: Int?, wait: Boolean, args: Array<String> = emptyArray()): BaseApplicationEngine {
    var mergedArgs = args
    if (port != null) {
        mergedArgs = arrayOf("-port=$port") + args
    }
    val server = embeddedServer(factory = Netty, environment = commandLineEnvironment(mergedArgs))

    server.start(wait = wait)
    return server
}

//fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        lateinit var mapper: ObjectMapper
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule())
            disableDefaultTyping()

            mapper = this
        }

        register(ContentType.Application.FormUrlEncoded, ActiveFormUrlEncodedToContentTypeConverter(mapper))

    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }


        post("/wizz-banger") {
            val wizzBanger: WizzBanger = call.receive()
            call.respond(HttpStatusCode.Created, wizzBanger)
        }

        post("/widget") {
            val widget = call.receive<Widget>()
            call.respond(HttpStatusCode.Created, widget)
        }

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

            exception<JsonMappingException> { cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(type = "MISSING_OR_INCORRECT_FIELDS", message = errorMessageForJsonMappingException(cause))
                )
            }

        }

        install(CallLogging) {
            level = Level.INFO

            mdc("corId") {
                it.request.headers["X-NewRelic-Transaction"]?: UUID.randomUUID().toString()
            }
        }
    }
}

private fun errorMessageForJsonMappingException(cause: JsonMappingException): String {
    val badPath = Regex(".*[.](.*)").find(cause.pathReference, 0)?.groups?.last()?.value
    val errorMessage =
        "could not process request. ${badPath ?: "A field"} was either missing or of the incorrect type."
    return errorMessage
}

data class ErrorResponse(val type: String, val message: String)

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

data class Widget(
    var name: String,
    val jimbo: List<String>,
    val age: Int
)