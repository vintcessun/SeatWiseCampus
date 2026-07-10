package com.seatwise.mobile

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** iOS 入口：Compose 内容托管为 UIViewController，供 SwiftUI 承载 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
