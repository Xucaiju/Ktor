package com.example

import com.example.model.User
import com.example.service.impl.UserServiceImpl1
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.ThymeleafContent
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import io.ktor.application.*
import io.ktor.gson.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.sessions.*
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import java.net.http.WebSocket
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

/**
 * Please note that you can use any other name instead of *module*.
 * Also note that you can have more then one modules in your application.
 * */
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Sessions) {
        cookie<User>("userSession")
    }
    install(ContentNegotiation){
        gson{

        }
    }
    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/thymeleaf/"
            suffix = ".html"
            characterEncoding = "utf-8"
        })
    }
    install(WebSockets)
    routing {
        static("/static"){
            resources("static")
        }
    }
    "/".mappingPath(this,
        {

        }
    )
    routing {
        get("/") {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val userAdapter = moshi.adapter(User::class.java)
            val user = User("leijun", 22)
            val userJson = userAdapter.toJson(user)
            val queryParameters = call.request.queryParameters
            println("queryParameters is:")
            queryParameters.forEach { s, list ->
                println(s)
            }
            call.sessions.set("userSession",  User("雷军", 33))
            call.respond(mapOf("user" to user))
        }
    }
    routing {
        install(StatusPages) {
            exception<AuthenticationException> { cause->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to cause.msg))
            }
            exception<AuthorizationException> { cause ->
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
    routing {
        get("/chat") {
            val userServiceImpl1 = UserServiceImpl1()
            call.respond(
                ThymeleafContent(
                    "index",
                    mapOf(
                        "users" to userServiceImpl1.getUsers()
                    )
                )
            )
        }
    }
    routing {
        val clients = Collections.synchronizedSet(LinkedHashSet<ChatClient>())
        webSocket("/chat-server") {
            val client = ChatClient(this)
            clients += client
            println("客户端[${client.name}]已连接")
            client.session.outgoing.send(Frame.Text(client.name))
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            // 迭代所有连接
                            println("${client.name} said:\t${text}")
                            val textToSend = "${client.name} said: $text"
                            val others = clients.filter { it!=client }
                            println("others:")
                            others.forEach {
                                println(it.name)
                            }
                            for (other in others.toList()) {
                                other.session.outgoing.send(Frame.Text(textToSend))
                            }
                        }
                        else -> {}
                    }
                }
            } finally {
                clients -= client
            }
        }
    }
}

class AuthenticationException(var msg:String) : RuntimeException()
class AuthorizationException : RuntimeException()
data class ThymeleafUser(val id: Int, val name: String)
class ChatClient(val session: DefaultWebSocketSession) {
    companion object { var lastId = AtomicInteger(0) }
    val id = lastId.getAndIncrement()
    val name = "user$id"
}