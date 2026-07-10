# SeatWise Mobile · 跨平台学生端（Kotlin Multiplatform + Compose Multiplatform）

用 **Kotlin Multiplatform（KMP）+ Compose Multiplatform（CMP）** 一套代码同时构建 **Android（APK）** 与 **iOS（IPA）**，复用 Web 后端的 REST 接口，实现学生端核心功能。

## 已实现功能（学生端子集）
- 登录（可在登录页填写后端地址；内置演示快捷登录）
- 自习室列表（`GET /api/study-rooms`）
- 选座预约：座位网格 + 点击空闲座位一键预约（`GET /board`、`POST /reservations`，默认「明天 14:00-16:00」）
- 我的预约：列表 + 取消（`GET /reservations/me`、`POST /reservations/{id}/cancel`）

> 认证复用后端 `satoken` 头；网络层用 **Ktor**（Android=OkHttp 引擎，iOS=Darwin 引擎），序列化用 kotlinx.serialization。

## 工程结构
```
mobile/
├─ shared/                     # KMP 共享模块（业务 + Compose UI）
│  ├─ commonMain/…/Models.kt   # 数据模型
│  ├─ commonMain/…/Api.kt      # Ktor REST 客户端
│  ├─ commonMain/…/App.kt      # Compose Multiplatform UI（登录/自习室/选座/我的预约）
│  ├─ androidMain/…            # Android 平台实现（baseUrl=10.0.2.2、日期）
│  └─ iosMain/…                # iOS 平台实现 + MainViewController（Compose→UIViewController）
├─ androidApp/                 # Android 应用（产出 APK）
├─ iosApp/                     # iOS 应用（SwiftUI 承载 Compose；XcodeGen 生成工程，产出 IPA）
└─ .github/workflows/mobile-build.yml  （位于仓库根）CI：ubuntu 出 APK、macOS 出 IPA
```

## 后端地址配置
- **Android 模拟器**：默认 `http://10.0.2.2:18080`（10.0.2.2 映射到宿主机的后端 18080 端口）。
- **iOS 模拟器**：默认 `http://localhost:18080`。
- **真机**：在登录页把「后端地址」改成后端所在电脑的 **LAN IP**（如 `http://192.168.1.10:18080`），并确保手机与后端同一局域网。

## 构建 Android APK
需要 JDK 17 + Android SDK。
```bash
cd mobile
./gradlew :androidApp:assembleDebug
# 产物：mobile/androidApp/build/outputs/apk/debug/androidApp-debug.apk
```
或直接用 CI（见下）。

## 构建 iOS IPA
> **iOS 的 IPA 必须在 macOS + Xcode 上编译**，Windows/Linux 无法本地产出。用仓库自带的 GitHub Actions（macOS runner）即可产出**未签名** IPA（演示/侧载用）。正式分发需配置 Apple 开发者证书与描述文件。

本地（macOS）：
```bash
brew install xcodegen
cd mobile/iosApp && xcodegen generate        # 生成 iosApp.xcodeproj
open iosApp.xcodeproj                          # Xcode 运行到模拟器/真机
```

## CI 一键出双端产物
仓库根 `.github/workflows/mobile-build.yml`：
- `android-apk`（ubuntu）：`./gradlew :androidApp:assembleDebug` → 上传 `SeatWise-android-apk`。
- `ios-ipa`（macos-14）：XcodeGen 生成工程 → `xcodebuild archive`（未签名）→ 打包 → 上传 `SeatWise-ios-ipa-unsigned`。

在 GitHub 仓库 Actions 页手动触发（workflow_dispatch）或 push `mobile/**` 即自动运行，完成后在 run 的 **Artifacts** 下载 APK / IPA。

## 技术栈
Kotlin 2.0.21 · Compose Multiplatform 1.7.1 · AGP 8.5.2 · Ktor 2.3.12 · kotlinx.serialization/coroutines · Gradle 8.9。
