package com.seatwise.mobile

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

// TODO(部署后填写): 后端尚未部署，默认地址留空。部署后改为公网/LAN 地址。
// 目前可在登录页手动输入；iOS 模拟器访问本机后端用 http://localhost:18080
actual val platformBaseUrl: String = ""

actual fun tomorrowDate(): String {
    val cal = NSCalendar.currentCalendar
    val tomorrow = cal.dateByAddingUnit(NSCalendarUnitDay, 1, NSDate(), 0u) ?: NSDate()
    val fmt = NSDateFormatter()
    fmt.dateFormat = "yyyy-MM-dd"
    return fmt.stringFromDate(tomorrow)
}
