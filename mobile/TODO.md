# Mobile TODO

## 待办
- [ ] **部署后端后更新移动端默认后端地址**（当前留空）
  - 位置：
    - `mobile/shared/src/androidMain/kotlin/com/seatwise/mobile/Platform.android.kt` → `platformBaseUrl`
    - `mobile/shared/src/iosMain/kotlin/com/seatwise/mobile/Platform.ios.kt` → `platformBaseUrl`
  - 现状：默认 `""`（留空），需在登录页手动输入后端地址才能登录。
  - 部署后：改为实际公网/LAN 地址，例如 `http://<server-ip>:18080` 或 `https://api.example.com`，
    并重新触发 CI 构建，产物即内置该默认地址（用户无需手填）。
  - 责任：**等用户部署后端并给出访问地址后，由我更新并重新构建。**

## 已完成
- [x] KMP + Compose Multiplatform 学生端（登录/自习室/选座预约/我的预约）
- [x] GitHub Actions 工作流：ubuntu 出 APK、macOS 出未签名 IPA
- [x] 登录页可手动填写后端地址（在默认地址部署前的过渡方案）
- [x] **CI 已跑通并产出双端二进制**（run 29106680706，Android 4m36s / iOS 8m55s 均 ✓）
  - 产物：`SeatWise-android-apk`（androidApp-debug.apk）、`SeatWise-ios-ipa-unsigned`（Payload/iosApp.app）
  - 获取：仓库 Actions → 对应 run → Artifacts 下载；或 `gh run download <run-id>`
