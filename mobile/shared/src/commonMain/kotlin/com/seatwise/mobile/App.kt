package com.seatwise.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Brand = Color(0xFF3B6CFF)
private val Purple = Color(0xFF8F5BFF)

@Composable
fun App(api: SeatWiseApi = remember { SeatWiseApi() }) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Brand, secondary = Purple)) {
        var user by remember { mutableStateOf<UserInfo?>(null) }
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF4F6FB)) {
            if (user == null) {
                LoginScreen(api) { user = it }
            } else {
                MainScreen(api, user!!) { user = null; api.token = null }
            }
        }
    }
}

@Composable
private fun LoginScreen(api: SeatWiseApi, onLoggedIn: (UserInfo) -> Unit) {
    var baseUrl by remember { mutableStateOf(api.baseUrl) }
    var username by remember { mutableStateOf("student1") }
    var password by remember { mutableStateOf("123456") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun doLogin() {
        if (baseUrl.isBlank()) { error = "请先填写后端地址（部署后端后填公网/LAN 地址）"; return }
        api.baseUrl = baseUrl.trimEnd('/')
        loading = true; error = null
        scope.launch {
            try {
                val d = api.login(username.trim(), password)
                onLoggedIn(d.userInfo)
            } catch (e: Throwable) {
                error = e.message ?: "登录失败"
            } finally { loading = false }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Brand, Purple)))
                .padding(24.dp)
        ) {
            Column {
                Text("🎓 SeatWise Campus", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("智能校园自习室 · 移动端", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            baseUrl, { baseUrl = it },
            label = { Text("后端地址") },
            placeholder = { Text("如 http://192.168.1.10:18080（部署后填写）") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(username, { username = it }, label = { Text("用户名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("密码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Color(0xFFD64545), fontSize = 13.sp) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { doLogin() }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(if (loading) "登录中…" else "登 录", fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { username = "student1"; password = "123456"; doLogin() }) { Text("学生 张三") }
            OutlinedButton(onClick = { username = "student2"; password = "123456"; doLogin() }) { Text("学生 李四") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(api: SeatWiseApi, user: UserInfo, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var openRoom by remember { mutableStateOf<Room?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("你好，${user.realName}") },
                actions = { TextButton(onClick = onLogout) { Text("退出") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("🪑") }, label = { Text("自习室") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("📋") }, label = { Text("我的预约") })
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> RoomsTab(api) { openRoom = it }
                else -> ReservationsTab(api)
            }
        }
    }

    openRoom?.let { room ->
        SeatsScreen(api, room) { openRoom = null }
    }
}

@Composable
private fun RoomsTab(api: SeatWiseApi, onOpen: (Room) -> Unit) {
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        try { rooms = api.rooms() } catch (_: Throwable) {} finally { loading = false }
    }
    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rooms) { r ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(r) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            AssistChip(onClick = {}, label = { Text(if (r.status == "OPEN") "开放" else "关闭") })
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${r.floorNo} 楼 · ${r.openStart.take(5)}-${r.openEnd.take(5)}", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatsScreen(api: SeatWiseApi, room: Room, onClose: () -> Unit) {
    val date = remember { tomorrowDate() }
    val start = "14:00"; val end = "16:00"
    var board by remember { mutableStateOf(Board()) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() { board = api.board(room.id, date, start, end) }
    LaunchedEffect(room.id) {
        try { reload() } catch (_: Throwable) {} finally { loading = false }
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
        title = { Text("${room.name} · 明天 $start-$end") },
        text = {
            Column {
                status?.let { Text(it, color = Brand, fontSize = 13.sp); Spacer(Modifier.height(8.dp)) }
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Text("点击绿色空闲座位预约", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(board.cols.coerceAtLeast(4)), modifier = Modifier.height(320.dp)) {
                        items(board.seats.sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))) { s ->
                            SeatCell(s) {
                                if (s.cellType == "SEAT" && s.status == "FREE") {
                                    scope.launch {
                                        status = try {
                                            api.createReservation(room.id, s.seatId, date, start, end); reload(); "已预约 ${s.seatNo}"
                                        } catch (e: Throwable) { e.message ?: "预约失败" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SeatCell(s: BoardSeat, onClick: () -> Unit) {
    val (bg, fg) = when {
        s.cellType != "SEAT" -> Color(0x11000000) to Color.Gray
        s.status == "FREE" -> Color(0xFFE3F6E9) to Color(0xFF1F9D55)
        s.status == "RESERVED" -> Color(0xFFFFF2DD) to Color(0xFFD98A00)
        s.status == "USING" -> Color(0xFFFFE1E1) to Color(0xFFD64545)
        else -> Color(0xFFE9ECF2) to Color.Gray
    }
    Box(
        Modifier.padding(3.dp).height(38.dp).clip(RoundedCornerShape(8.dp)).background(bg).clickable { onClick() },
        Alignment.Center
    ) {
        Text(
            if (s.cellType == "SEAT") (s.seatNo?.replace("-", "") ?: "") else if (s.cellType == "AISLE") "·" else "",
            color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReservationsTab(api: SeatWiseApi) {
    var list by remember { mutableStateOf<List<Reservation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(tick) {
        loading = true
        try { list = api.myReservations() } catch (_: Throwable) {} finally { loading = false }
    }
    if (loading) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
    } else if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("暂无预约，去自习室选座吧", color = Color.Gray) }
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { r ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${r.roomName} · ${r.seatNo}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${r.date} ${r.startTime}-${r.endTime}", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = {}, label = { Text(statusText(r.status)) })
                            if (r.status == "PENDING_SIGN_IN" || r.status == "IN_USE") {
                                TextButton(onClick = {
                                    scope.launch { try { api.cancel(r.id); tick++ } catch (_: Throwable) {} }
                                }) { Text("取消", color = Color(0xFFD64545)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusText(s: String) = when (s) {
    "PENDING_SIGN_IN" -> "待签到"; "IN_USE" -> "使用中"; "COMPLETED" -> "已完成"
    "CANCELLED" -> "已取消"; "EXPIRED_RELEASED" -> "已释放(爽约)"; else -> s
}
