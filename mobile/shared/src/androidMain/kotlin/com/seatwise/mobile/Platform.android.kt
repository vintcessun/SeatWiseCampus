package com.seatwise.mobile

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TODO(部署后填写): 后端尚未部署，默认地址留空。部署后改为公网/LAN 地址，
// 例如 "http://<server-ip>:18080" 或 "https://api.example.com"。
// 目前可在登录页手动输入；Android 模拟器访问本机后端用 http://10.0.2.2:18080
actual val platformBaseUrl: String = ""

actual fun tomorrowDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}
