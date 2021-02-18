package com.example

import com.example.exception.AuthenticationException
import com.example.exception.AuthorizationException
import com.example.model.User
import com.example.service.impl.UserServiceImpl1
import com.squareup.moshi.*
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
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.spec.ChaCha20ParameterSpec
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
    "/".mappingPath(this)
    routing {
        get("/") {
            val moshi = Moshi.Builder().add(MsgSendToAdapter()).add(MsgTypeAdapter()).build()
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
        get("/error"){
            throw AuthorizationException()
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
        val moshi = Moshi.Builder().add(MsgSendToAdapter()).add(MsgTypeAdapter()).add(KotlinJsonAdapterFactory()).build()
        val msgAdapter = moshi.adapter(Msg::class.java)
        webSocket("/chat-server") {
            val client = ChatClient(this)
            clients += client
            println("客户端[${client.name}]已连接")
            val msg = Msg(client.id, Msg.MsgSendTo.SEND_TO_PRECISE_USER(client.id), Msg.MsgType.TYPE_LOGIN,"登录")
            val msgJsonStr = msgAdapter.toJson(msg)
            println("登录消息:${msgJsonStr}")
            client.session.outgoing.send(Frame.Text(msgJsonStr))
            try {
                while (true) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            // 迭代所有连接
                            println("${client.name} said:\t${text}")
                            val textToSend = "${client.name} said: $text"

                            try {
                                val msgFromUser = msgAdapter.fromJson(text)
                                msgFromUser?.let {
                                    MsgHandler.handleMsg(clients, it.sendTo, text)
                                }
                                println("收到消息,转换成功!")
                            }catch (e:IOException){
                                println("收到消息,转换失败!")
                            }
                            val others = clients.filter { it!=client }
                            println("others:")
                            others.forEach {
                                println(it.name)
                            }
                            /*for (other in others.toList()) {
                                other.session.outgoing.send(Frame.Text(textToSend))
                            }*/
                        }
                        else -> {}
                    }
                }
            } finally {
                println("客户端断开连接")
                //val msg1 = Msg(client.id, Msg.MsgSendTo.SEND_TO_PRECISE_USER(client.id), Msg.MsgType.TYPE_LOGOUT,"退出登录")
                //val msgJsonStr1 = msgAdapter.toJson(msg1)
                //client.session.outgoing.send(Frame.Text(msgJsonStr1))
                clients -= client
            }
        }
    }
}


data class ThymeleafUser(val id: Int, val name: String)
class ChatClient(val session: DefaultWebSocketSession) {
    companion object { var lastId = AtomicInteger(0) }
    val id = lastId.getAndIncrement()
    val name = "user$id"
}
//@JsonClass(generateAdapter = true)
data class Msg(var userId:Int,var sendTo:MsgSendTo, var msgType:MsgType, var content:String){
    enum class MsgType{
        TYPE_LOGIN,
        TYPE_LOGOUT,
        TYPE_NORMAL
    }
    sealed class MsgSendTo{
        data class SEND_TO_PRECISE_USER(var id:Int):MsgSendTo()//精确发送到人
        data class SEND_TO_PRECISE_GROUP(var id:Int):MsgSendTo()//精确发送到群
        data class SEND_TO_FUZZY_GROUP(var ids:List<Int>): MsgSendTo()//模糊发送到群
        data class SEND_TO_FUZZY_USER(var ids:List<Int>): MsgSendTo()//模糊发送到人
    }

    val id = lastId.getAndIncrement()
    companion object { var lastId = AtomicInteger(0) }
}
object MsgHandler{
    private val moshi: Moshi = Moshi.Builder().add(MsgSendToAdapter()).add(MsgTypeAdapter()).add(KotlinJsonAdapterFactory()).build()
    private val msgAdapter: JsonAdapter<Msg> = moshi.adapter(Msg::class.java)
    suspend fun handleMsg(clients: MutableSet<ChatClient>, sendTo: Msg.MsgSendTo, msgJsonText:String){
        println("处理消息中...")
        when (sendTo) {
            is Msg.MsgSendTo.SEND_TO_PRECISE_USER -> {//发送给指定的人
                val sendToUserId = (sendTo as Msg.MsgSendTo.SEND_TO_PRECISE_USER).id
                println("发送给指定人(单人)==>$sendToUserId\t内容[${msgJsonText}]")
                clients.find { it.id == sendToUserId }?.let {
                    it.session.outgoing.send(Frame.Text(msgJsonText))
                }
            }
            is Msg.MsgSendTo.SEND_TO_PRECISE_GROUP -> {

            }
            is Msg.MsgSendTo.SEND_TO_FUZZY_USER -> {
                print("发送给指定人(多人)==>${(sendTo as Msg.MsgSendTo.SEND_TO_FUZZY_USER).ids}")
               /* val sendToUserIds = (sendTo as Msg.MsgSendTo.SEND_TO_FUZZY_USER).ids
                val msgJson = msgAdapter.toJson(msg)
                val sendToClients = mutableSetOf<ChatClient>()
                sendToUserIds.forEach { sendToId->
                    clients.find { it.id==sendToId}?.let {
                        sendToClients.add(it)
                    }
                }
                sendToClients.forEach {
                    it.session.outgoing.send(Frame.Text(msgJson))
                }*/
            }
            is Msg.MsgSendTo.SEND_TO_FUZZY_GROUP -> {

            }
        }
    }
}
class MsgTypeAdapter{
    @FromJson
    fun fromJson(value:Int):Msg.MsgType{
        return when(value){
            0->{Msg.MsgType.TYPE_LOGIN}
            1->{Msg.MsgType.TYPE_LOGOUT}
            2->{Msg.MsgType.TYPE_NORMAL}
            else->{Msg.MsgType.TYPE_NORMAL}
        }
    }
    @ToJson
    fun toJson(msgType:Msg.MsgType):Int{
        return when(msgType){
            Msg.MsgType.TYPE_LOGIN->{0}
            Msg.MsgType.TYPE_LOGOUT->{1}
            Msg.MsgType.TYPE_NORMAL->{2}
        }
    }
}

data class PRECISE_USER(var id:Int)
data class FUZZY_USER(var ids:List<Int>)
data class PRECISE_GROUP(var id:Int)
data class FUZZY_GROUP(var ids:List<Int>)

class MsgSendToAdapter{
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val PRECISE_USER_Adapter = moshi.adapter(PRECISE_USER::class.java)
    val PRECISE_GROUP_Adapter = moshi.adapter(PRECISE_GROUP::class.java)
    val FUZZY_USER_Adapter = moshi.adapter(FUZZY_USER::class.java)
    val FUZZY_GROUP_Adapter = moshi.adapter(FUZZY_GROUP::class.java)
    @FromJson
    fun fromJson(value:String?): Msg.MsgSendTo{
        var msgSendTo: Msg.MsgSendTo = Msg.MsgSendTo.SEND_TO_PRECISE_USER(-1)
        try {
            val PRECISE_USER:PRECISE_USER? = PRECISE_USER_Adapter.fromJson(value)
            /*val PRECISE_GROUP:PRECISE_GROUP? = PRECISE_GROUP_Adapter.fromJson(value)
            val FUZZY_USER:FUZZY_USER? = FUZZY_USER_Adapter.fromJson(value)
            val FUZZY_GROUP:FUZZY_GROUP? = FUZZY_GROUP_Adapter.fromJson(value)*/
            when {
                PRECISE_USER!=null -> {
                    msgSendTo = Msg.MsgSendTo.SEND_TO_PRECISE_USER(PRECISE_USER.id)
                }
                /* PRECISE_GROUP!=null -> {
                     msgSendTo = Msg.MsgSendTo.SEND_TO_PRECISE_GROUP(PRECISE_GROUP.id)
                 }
                 FUZZY_USER!=null -> {
                     msgSendTo = Msg.MsgSendTo.SEND_TO_FUZZY_USER(FUZZY_USER.ids)
                 }
                 FUZZY_GROUP!=null -> {
                     msgSendTo = Msg.MsgSendTo.SEND_TO_FUZZY_GROUP(FUZZY_GROUP.ids)
                 }*/
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
        return msgSendTo
    }
    @ToJson
    fun toJson(msgSendTo: Msg.MsgSendTo): String? {
        return when(msgSendTo){
            is Msg.MsgSendTo.SEND_TO_PRECISE_USER ->{
                PRECISE_USER_Adapter.toJson(PRECISE_USER(msgSendTo.id))
            }
            else->{
                PRECISE_USER_Adapter.toJson(PRECISE_USER(-1))
            }
            /*is Msg.MsgSendTo.SEND_TO_PRECISE_GROUP ->{
                PRECISE_GROUP_Adapter.toJson(PRECISE_GROUP(msgSendTo.id))
            }
            is Msg.MsgSendTo.SEND_TO_FUZZY_USER ->{
                FUZZY_USER_Adapter.toJson(FUZZY_USER(msgSendTo.ids))
            }
            is Msg.MsgSendTo.SEND_TO_FUZZY_GROUP ->{
                FUZZY_GROUP_Adapter.toJson(FUZZY_GROUP(msgSendTo.ids))
            }*/
        }
    }
}