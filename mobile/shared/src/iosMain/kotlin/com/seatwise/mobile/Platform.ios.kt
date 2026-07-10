package com.seatwise.mobile

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

// iOS 模拟器可用 localhost 访问宿主；真机请在登录页改成后端所在 LAN 地址
actual val platformBaseUrl: String = "http://localhost:18080"

actual fun tomorrowDate(): String {
    val cal = NSCalendar.currentCalendar
    val tomorrow = cal.dateByAddingUnit(NSCalendarUnitDay, 1, NSDate(), 0u) ?: NSDate()
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyy-MM-dd"
    return fmt.stringFromDate(tomorrow)
}
