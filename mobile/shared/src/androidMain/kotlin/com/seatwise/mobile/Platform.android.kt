package com.seatwise.mobile

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 生产后端地址：经 java.vintces.icu (nginx:80) 反代到后端 /api。
// 登录页仍可手动覆盖；Android 模拟器访问本机后端可改用 http://10.0.2.2:18080
actual val platformBaseUrl: String = "http://java.vintces.icu"

actual fun tomorrowDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}
