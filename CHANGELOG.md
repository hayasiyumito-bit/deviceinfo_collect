# Changelog / 更新日志

All notable changes to this project are documented here. English first, 中文在后。

---

## v3.0

A full UI overhaul plus a real-time monitoring suite.
一次彻底的界面重构，并新增实时监测能力。

### ✨ Brand-new UI / 全新界面
- **EN** — Home rebuilt with a bottom navigation of 5 tabs (Risk / System / Hardware / Network / Apps) and a card-based layout. The UI layer was rewritten in Kotlin + ViewBinding while reusing the existing Java data-collection pipeline.
- **中文** — 首页重构为底部 5 个 tab（风控 / 系统 / 硬件 / 网络 / 应用）+ 卡片式布局；UI 层用 Kotlin + ViewBinding 重写，复用原有 Java 采集链。
- **EN** — 6 switchable theme colors (Cyber Blue / Neon Green / Sakura Pink / Sunset Orange / Deep Purple / Graphite); full English–Chinese bilingual throughout.
- **中文** — 6 套可切换主题配色（赛博蓝 / 霓虹绿 / 樱花粉 / 日暮橙 / 深邃紫 / 石墨灰）；全程中英双语。
- **EN** — Self-drawn loading spinner that keeps animating even when system animations are disabled; status-bar / navigation-bar icons adapt to light & dark themes.
- **中文** — 自绘加载动画，即使系统动画被关闭也能转动；状态栏 / 导航栏图标随深浅色主题适配。
- **EN** — Exit confirmation dialog on Back; a floating button to re-scan on demand.
- **中文** — 返回键退出前确认弹窗；悬浮按钮支持手动重新采集。

### 📡 Real-time monitor / 实时监测
- **EN** — A dedicated page (entry at the top-left) with live: battery (level, current, temperature, voltage, health, cycle count), per-core CPU frequency, CPU/GPU temperature, GPU load, memory, and network up/down speed.
- **中文** — 独立页面（入口在左上角），实时显示：电量（电量、电流、温度、电压、健康度、循环次数）、CPU 各核频率、CPU/GPU 温度、GPU 负载、内存、网络上下行速率。
- **EN** — Live sensors (accelerometer, gyroscope, magnetometer, gravity, light, proximity, etc.) and live location (latitude/longitude, accuracy, altitude, speed, bearing).
- **中文** — 实时传感器（加速度计、陀螺仪、磁力计、重力、光线、距离等）与实时定位（经纬度、精度、海拔、速度、方位角）。
- **EN** — Satellite sky map colored by constellation (GPS / BeiDou / GLONASS / Galileo / QZSS / SBAS) with a legend.
- **中文** — 卫星星图按星座分色（GPS / 北斗 / GLONASS / Galileo / QZSS / SBAS），并配图例。
- **EN** — All listeners start on entering the page and stop on leaving; collection I/O runs on a background thread to avoid leaks and ANR.
- **中文** — 所有监听在进入页面时开启、离开时关闭；采集 I/O 在后台线程执行，避免内存泄漏与 ANR。

### 🧩 Home-screen widget / 桌面小组件
- **EN** — Added a home-screen widget (add it via long-press on the launcher) showing live battery / CPU / GPU / memory, with a refresh button; tap to open the real-time page.
- **中文** — 新增桌面小组件（长按桌面即可添加），展示实时电量 / CPU / GPU / 内存，带刷新按钮；点击可进入实时监测页。

### 🔍 Data & parsing / 数据与解析
- **EN** — Raw snapshot fields are now expanded and translated into concrete parameters instead of a generic "JSON object" placeholder; obscure, oversized, or nested values move into a detail dialog.
- **中文** — 原始快照字段现在会展开并翻译成具体参数，不再是笼统的"JSON 对象"占位；晦涩、过大或嵌套的值收进详情弹窗。
- **EN** — Deep parsing on the Hardware tab: CPU cores/architecture, camera resolution (megapixels)/focal length/field-of-view, and per-sensor power/vendor/range.
- **中文** — 硬件页深度解析：CPU 核心数/架构、摄像头分辨率（像素）/焦距/视场角、各传感器功耗/厂商/量程。
- **EN** — Empty values and redundant raw codes are filtered; location fields are fully labeled.
- **中文** — 过滤空值与冗余裸码；位置信息字段完整标注。

### 🛡️ Risk control / 风控
- **EN** — Risk overview dashboard with Root / Hook / Environment / Remediation sections.
- **中文** — 风险总览仪表盘，含 Root / Hook / 运行环境 / 修复建议分区。
- **EN** — A debuggable build (FLAG_DEBUGGABLE) and ADB debugging are no longer counted as risks.
- **中文** — 应用可调试（FLAG_DEBUGGABLE）与 ADB 调试不再计入风险。

### ⚡ Performance & internals / 性能与内部改动
- **EN** — Removed a heavy parse step that ran on every collection but was no longer used by the UI; shortened the root-probe wait to speed up collection.
- **中文** — 移除了每次采集都执行、但 UI 已不再使用的重量级解析步骤；缩短 root 探测等待，加快采集。
- **EN** — Removed dead legacy list-UI code; hardened the real-time page against memory leaks.
- **中文** — 清理旧列表 UI 死代码；加固实时页防止内存泄漏。
