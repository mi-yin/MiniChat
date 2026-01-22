import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.CopyOnWriteArraySet

fun main() {
    println("正在启动服务器...")
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) // 开启实时聊天支持

        routing {
            val sessions = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

            webSocket("/chat") {
                sessions.add(this)
                println("有人进来了！当前在线: ${sessions.size}人")

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            // 把收到的消息转发给所有人
                            sessions.forEach { it.send(text) }
                        }
                    }
                } finally {
                    sessions.remove(this)
                    println("有人离开了，剩余在线: ${sessions.size}人")
                }
            }
        }
    }.start(wait = true)
}