# 商业使用说明

## 公众（第三方）

本项目对公众采用 **[CC BY-NC 4.0](LICENSE)**：**禁止商业使用**。

未经 Yumito 书面授权，不得将本项目用于任何商业目的。

## 著作权人（Yumito）

**Yumito 作为著作权人，保留对本项目的完整商业使用权**，不受上述非商业许可限制。

未来基于本项目开展的商业化、产品化、对外授权，均由 Yumito 行使。

## 溯源追踪

为便于识别未经授权的商用分发，项目在以下位置嵌入统一指纹 `YDC-7F3A9C2E-202607`：

| 位置 | 说明 |
|------|------|
| `NOTICE` | 法律与指纹说明 |
| `ProjectProvenance.java` | 源码常量与 JSON 序列化 |
| `AndroidManifest.xml` | APK `meta-data` |
| `BuildConfig` | 构建注入字段 |
| `libdevice.so` | Native 字符串水印 |
| `debug_output.json` | 顶层 `_provenance` 块 |

查验方式示例：

```bash
# APK 元数据
aapt dump badging app.apk | grep provenance

# 导出 JSON
jq '._provenance' debug_output.json

# 原生库字符串
strings lib/arm64-v8a/libdevice.so | grep YDC-7F3A9C2E
```

## 商业授权联系

第三方商业合作或授权请通过 Gitee 仓库 Issue 联系维护者 **Yumito**。
