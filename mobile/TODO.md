# Mobile TODO

## 待办
- [ ] **重新触发 CI 出双端产物**（内置新默认地址 `http://java.vintces.icu`）
  - 后端已部署到 `java.vintces.icu`（nginx:80 反代 → 后端 /api），默认地址已在
    `Platform.android.kt` / `Platform.ios.kt` 的 `platformBaseUrl` 填为 `http://java.vintces.icu`。
  - 需在 GitHub Actions 手动 Run workflow（或 push `mobile/**`）重新构建，
    产物 APK/IPA 即内置该地址、用户无需手填。（Actions 若关闭需先在仓库 Settings 开启。）

## 已完成
- [x] KMP + Compose Multiplatform 学生端（登录/自习室/选座预约/我的预约）
- [x] GitHub Actions 工作流：ubuntu 出 APK、macOS 出未签名 IPA
- [x] 登录页可手动填写后端地址（在默认地址部署前的过渡方案）
- [x] **CI 已跑通并产出双端二进制**
  - `SeatWise-android-apk` → **androidApp-release.apk（v2/v3 已签名，手机可直接安装）**
    - 签名：CI 用 keytool 生成演示 keystore（不入库）；minSdk 24，Android 7.0+ 可装
  - `SeatWise-ios-ipa-unsigned` → SeatWise-unsigned.ipa（未签名，真机需 Apple 证书/侧载工具）
  - 获取：仓库 Actions → 对应 run → Artifacts 下载；或 `gh run download <run-id>`
