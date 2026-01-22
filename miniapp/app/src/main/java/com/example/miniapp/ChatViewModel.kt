package com.example.miniapp

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import okhttp3.*
import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.InputStream


class ChatViewModel : ViewModel() {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // 线程安全的列表，用于存储消息，UI会自动监听它
    val messages = mutableStateListOf<ChatMessage>()
    
    // 给当前用户生成一个随机 ID
    val myId = "User_${(10..99).random()}"

    fun connect() {
        // 如果是模拟器连电脑 Server，用 10.0.2.2；如果是真机，用电脑 IP
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/chat") 
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 收到服务器的消息，解析并添加到列表
                val msg = gson.fromJson(text, ChatMessage::class.java)
                messages.add(msg)
            }
        })
    }

    fun sendMessage(text: String, isImage: Boolean = false) {
        val msg = ChatMessage(senderId = myId, content = text, isImage = isImage)
        val json = gson.toJson(msg)
        webSocket?.send(json)
    }

    // 性能优化：当 Activity 关闭时，彻底关闭连接，防止资源泄露
    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "App Destroyed")
    }

    fun sendImage(context: Context, uri: Uri) {
        try {
            // 1. 将图片读取为字节数组
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes != null) {
                // 2. 压缩并转换成 Base64 字符串 (为了性能，建议不要选太大的图)
                val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

                // 3. 发送。我们在内容前面加个前缀 "base64:" 方便显示时判断
                sendMessage("base64:$base64String", isImage = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}