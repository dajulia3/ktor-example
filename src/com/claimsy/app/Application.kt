package com.claimsy.app

import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.util.KtorExperimentalAPI
import ktor_moshi.moshi
import java.util.*


@UseExperimental(KtorExperimentalAPI::class)
fun main(args: Array<String>) {
    val server = embeddedServer(CIO, 8080, module = Application::module)
    server.start(wait = true)
}

//fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
//    install(Thymeleaf) {
//        setTemplateResolver(ClassLoaderTemplateResolver().apply {
//            prefix = "templates/thymeleaf/"
//            suffix = ".html"
//            characterEncoding = "utf-8"
//        })
//    }

    install(ContentNegotiation) {
        moshi {
            // Configure the Moshi.Builder here.
            add(Date::class.java, Rfc3339DateJsonAdapter())
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
    }
}

data class ThymeleafUser(val id: Int, val name: String)

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

@JsonClass(generateAdapter = true)
data class Fizzer(
    var fizziness: String,
    var temperature: Int
)
@JsonClass(generateAdapter = true)
data class WizzBanger(var id: String, var name: String, var fizzer: Fizzer)
