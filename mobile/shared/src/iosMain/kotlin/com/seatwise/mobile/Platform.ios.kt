package com.seatwise.mobile

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

// 生产后端地址：经 java.vintces.icu (nginx:80) 反代到后端 /api。
// 登录页仍可手动覆盖；iOS 模拟器访问本机后端可改用 http://localhost:18080
actual val platformBaseUrl: String = "http://java.vintces.icu"

actual fun tomorrowDate(): String {
    val cal = NSCalendar.currentCalendar
    val tomorrow = cal.dateByAddingUnit(NSCalendarUnitDay, 1, NSDate(), 0u) ?: NSDate()
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyy-MM-dd"
    return fmt.stringFromDate(tomorrow)
}
