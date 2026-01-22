package com.example.miniapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.unit.sp
import android.util.Base64 // 必须导入这个
import androidx.compose.ui.layout.ContentScale // 导入缩放模式
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ChatViewModel = viewModel()
            // 界面启动时连接服务器
            LaunchedEffect(Unit) { vm.connect() }

            ChatAppUI(vm)
        }
    }
}

@Composable
fun ChatAppUI(vm: ChatViewModel) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current // 获取当前上下文
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 2. 定义图片选择“启动器”
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // 获取内容
    ) { uri: Uri? ->
        // 当用户选完图片回来后执行这里
        uri?.let { vm.sendImage(context, it) }
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 3. 点击按钮打开相册
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Text("🖼️", fontSize = 20.sp) // 图片图标
                    }

                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") }
                    )

                    Button(onClick = {
                        if (text.isNotBlank()) {
                            vm.sendMessage(text)
                            text = ""
                            scope.launch { listState.animateScrollToItem(vm.messages.size) }
                        }
                    }) { Text("发送") }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF3F3F3)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 使用 key 保证滑动流畅不卡顿
            items(vm.messages, key = { it.id }) { msg ->
                ChatBubble(msg, isMe = msg.senderId == vm.myId)
            }
        }
    }
}



@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // 增加上下间距
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // 显示发送者 ID
        Text(
            text = msg.senderId,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isMe) Color(0xFF95EC69) else Color.White,
            shadowElevation = 1.dp
        ) {
            if (msg.isImage) {
                // 1. 处理数据源
                val imageData = if (msg.content.startsWith("base64:")) {
                    val pureBase64 = msg.content.substringAfter("base64:")
                    try {
                        Base64.decode(pureBase64, Base64.DEFAULT)
                    } catch (e: Exception) {
                        null // 如果解码失败，返回 null
                    }
                } else {
                    msg.content // 支持普通的 URL 链接
                }

                // 2. 高性能显示图片
                AsyncImage(
                    model = imageData,
                    contentDescription = null,
                    // 限制图片最大尺寸，防止超长图撑破屏幕
                    modifier = Modifier
                        .sizeIn(maxWidth = 200.dp, maxHeight = 300.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit // 适应气泡大小
                )
            } else {
                // 3. 显示普通文本
                Text(
                    text = msg.content,
                    modifier = Modifier.padding(10.dp),
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}
