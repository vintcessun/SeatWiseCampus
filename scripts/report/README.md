# SeatWise Campus · 全站截图报告

本目录是对 SeatWise Campus 前端**全部页面**在**多端 / 多语言**下的截图存档，用于验收「系统完全完善、功能完全正常」。

- 生成时间：2026-07-14
- 站点：`http://localhost:8888`（生产构建，docker compose）
- 驱动方式：真实 Chrome（Playwright，设备模拟）
- 结论：**所有页面、所有端在中英双语下均零控制台错误**（见 `_errors.json`）。

## 目录结构

| 目录 | 端 / 设备 | 语言 | 张数 |
| --- | --- | --- | --- |
| `desktop/`     | 桌面 1440×900        | 中文 | 20 |
| `desktop-en/`  | 桌面 1440×900        | English | 20 |
| `ios/`         | 苹果 iPhone 13（Safari 模拟） | 中文 | 20 |
| `android/`     | 安卓 Pixel 5（Chrome 模拟）   | 中文 | 20 |

每个目录含：`00-login.png`（登录页）、`student-*.png`（学生端 9 页）、`admin-*.png`（管理端 10 页）。

> `shots/` 为早期会话留存的历史截图，保留未动。

## 覆盖的页面

**学生端**：首页概览、选座预约、时空座位图、我的预约、我的候补、自习报告、专注番茄钟、附近空位、积分排行。
**管理端**：概览首页、自习室与座位、时空占用图、学生预约追踪、数据报表、公告管理、位置管理、黑名单管理、积分排行、管理员管理。

## 快速浏览

用浏览器打开 **`index.html`** 即可分端、分页缩略图查看全部截图。

## 本次为「完善」所做的关键改动

1. **国际化 i18n（中/英一键切换）**：自研轻量 i18n，联动 Element Plus 与 `<html lang>`；覆盖全局框架、登录、首页/概览、选座、通知、AI 助手等。
2. **移动端响应式**：新增汉堡抽屉式导航（桌面保持侧边栏），窄屏栅格纵向堆叠、横幅/AI 面板自适应 —— 修复了原本在手机上侧边栏挤压、内容溢出、文字竖排的问题。
3. **无障碍 a11y**：跳转主内容 skip-link、`:focus-visible` 焦点环、`prefers-reduced-motion`、图标按钮 `aria-label`、快捷卡片键盘可达。
4. **细节**：SVG favicon、theme-color/description meta。

## 复现

```bash
# 前端本地开发（代理到 18080 后端）
cd client && npm install
VITE_API_TARGET=http://localhost:18080 npm run dev   # :5173

# 重新生成本报告：见提交历史中的 Playwright 脚本
```
