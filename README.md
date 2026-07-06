# device-collection

Android 设备信息采集与 Hook/Root/环境安全检测示例工程。

## 功能

- **设备快照**：硬件、系统、网络、软件、标识等多维度 JSON 采集（`collections` 模块）
- **安全检测**：Hook 框架、属性篡改、Root、模拟器、VPN、ADB 等
- **结果导出**：每次采集覆盖写入 `Download/debug_output.json`
- **UI 展示**：按分类浏览采集结果，支持 FAB 刷新

## 模块结构

```
.
├── app/           # 演示 App（MainActivity、安全检测、JNI）
├── collections/   # 设备信息采集库（AAR）
└── java-se/       # Java SE 工具（开发辅助）
```

## 构建要求

- JDK 11+
- Android Studio / AGP 7.x
- Android SDK 34
- NDK（app 模块 native 属性读取）

## 快速开始

```bash
# 克隆
git clone git@gitee.com:Yumito/device-collection.git
cd device-collection

# 编译 Debug APK
./gradlew :app:assembleDebug
```

安装后启动 App，会自动采集；点击右下角 FAB 可重新采集。  
导出文件：`/sdcard/Download/debug_output.json`（Android 10+ 通过 MediaStore 写入）。

## debug_output.json 结构（节选）

```json
{
  "collectedAt": 1751772000000,
  "anyRisk": true,
  "anyRiskReasons": ["..."],
  "security": {
    "summary": { "hookFrameworkDetected": true, "anyRisk": true },
    "reasons": { "hook": ["..."], "root": ["..."] },
    "hook": { "propertyProbes": { ... } },
    "root": { ... }
  },
  "build": { ... },
  "ids": { ... }
}
```

## 许可证

本项目采用 **[CC BY-NC 4.0（署名-非商业性使用）](LICENSE)**。

- 允许：学习、研究、个人与非商业场景下的使用与修改
- 禁止：未经作者书面许可的商业使用
- 要求：保留署名并说明修改

如需商业授权，请通过 Gitee Issue 或仓库维护者联系。

## 免责声明

本工具仅用于设备环境研究与安全检测学习。请在合法合规前提下使用，作者不对滥用行为负责。
