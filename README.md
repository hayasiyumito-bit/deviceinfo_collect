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

## 许可证与商业权利

| 主体 | 权利 |
|------|------|
| **公众 / 第三方** | [CC BY-NC 4.0](LICENSE) — 仅非商业使用，须署名 |
| **著作权人 Yumito** | **保留完整商业使用权**，不受 NC 限制 |

详见 [COMMERCIAL.md](COMMERCIAL.md)。

第三方禁止未经授权商用；Yumito 本人未来可基于本项目商业化。

## 溯源追踪（Provenance）

指纹：**`YDC-7F3A9C2E-202607`**

| 位置 | 说明 |
|------|------|
| `NOTICE` | 法律与指纹声明 |
| `ProjectProvenance.java` | 源码常量 |
| APK `meta-data` | `com.android.device.provenance.*` |
| `BuildConfig` | `PROVENANCE_FINGERPRINT` |
| `libdevice.so` | Native 字符串水印 |
| `debug_output.json` | 顶层 `_provenance` |

```bash
jq '._provenance' debug_output.json
strings lib/arm64-v8a/libdevice.so | grep YDC-7F3A9C2E
```

## debug_output.json 结构（节选）

```json
{
  "_provenance": {
    "fingerprint": "YDC-7F3A9C2E-202607",
    "projectId": "device-collection",
    "copyrightHolder": "Yumito",
    "licensePublic": "CC-BY-NC-4.0",
    "commercialRights": "Yumito exclusive — commercial use reserved"
  },
  "collectedAt": 1751772000000,
  "anyRisk": true,
  "security": { ... }
}
```

## 许可证（摘要）

- 允许：学习、研究、个人与非商业场景下的使用与修改（须署名）
- 禁止：第三方未经授权的商业使用
- 著作权人 Yumito 保留商业使用权

完整条款见 [LICENSE](LICENSE)、[NOTICE](NOTICE)。

## 免责声明

本工具仅用于设备环境研究与安全检测学习。请在合法合规前提下使用，作者不对滥用行为负责。
