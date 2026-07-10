package com.seatwise.mobile

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Android 模拟器用 10.0.2.2 访问宿主机；真机请在登录页改成后端所在 LAN 地址
actual val platformBaseUrl: String = "http://10.0.2.2:18080"

actual fun tomorrowDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}
