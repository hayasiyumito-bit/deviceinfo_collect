# 安全风险判定分析报告

> 基于 `debug_output.json`（采集时间戳 `1783417475685`）与当前代码逻辑整理。  
> 设备型号：`2201123G`（Xiaomi / cupid，Android 14）。

---

## 1. 结论摘要

| 项目 | 结果 |
|------|------|
| `anyRisk` | **true** |
| 主要触发原因 | **疑似 Root 隐藏**（`magiskHideSuspected`） |
| Hook 框架 | 未检出 |
| 属性篡改 | 未检出 |
| su 授权 | 未获取（DenyList 生效） |
| Java 层框架路径 | 全部不可见（Hide 生效） |
| **Native JNI 层** | **暴露 Magisk / Zygisk / KernelSU 痕迹** |

**判定结论：合理。** 设备存在 Magisk + Zygisk（可能叠加 KernelSU 模块），DenyList 对 Java/Shell/属性层隐藏有效，但 Native C++ 探测未被绕过。

---

## 2. 判定链路（代码路径）

```
DeviceSnapshotMerger.collectFresh()
  └─ SecurityReportComposer.build(context)
       ├─ CheckEmu.buildHookSection()      → hook / propertyProbes
       └─ CheckEmu.buildRootSection()      → RootFrameworkDetector.probe()
            └─ summary.anyRisk 聚合
```

`anyRisk` 在 `SecurityReportComposer.buildSummary()` 中为以下任一为真：

- `hookFrameworkDetected` / `propertyTampered`
- `isRooted` / `magiskDetected` / `kernelsuDetected` / `apatchDetected` / `systemSuDetected`
- `magiskHideSuspected` / `rootAccessGranted`
- 模拟器 / VPN / Debug / ADB 等环境项

本次 JSON 仅 Root 相关项为真。

---

## 3. 本次 JSON 关键字段解读

### 3.1 Summary

```json
{
  "hookFrameworkDetected": false,
  "propertyTampered": false,
  "isRooted": true,
  "magiskDetected": false,
  "kernelsuDetected": false,
  "apatchDetected": false,
  "systemSuDetected": false,
  "magiskHideSuspected": true,
  "rootAccessGranted": false,
  "anyRisk": true
}
```

- **Hook / 属性**：`procMapsMatches` 为空；8 个探测属性在 getprop / SystemProperties / jniGet / jniFind 间一致（`tampered: false`）。
- **libcutils 报错**：`Error: libcutils property_get unavailable` 为设备上 so 不可用，**不计入** `propertyTampered`。
- **Root 经典指标**：`suBinaryExists`、`magiskPathExists` 等均为 false。

### 3.2 决定性证据：`sharedIndicators.nativeProbe`

```json
{
  "accessiblePaths": ["/debug_ramdisk/magisk"],
  "mapsHits": ["zygisk", "zygisk", "zygisk", "ksu", "ksu", "ksu"],
  "mountHits": ["magisk", "magisk", "magisk", "magisk", "magisk"],
  "anyHit": true
}
```

| 信号 | 含义 |
|------|------|
| `/debug_ramdisk/magisk` Native `access()` 成功 | Magisk 典型路径 |
| `zygisk` in maps | Magisk Zygisk 注入 |
| `ksu` in maps | KernelSU 或相关模块 |
| `magisk` in mountinfo | Magisk overlay 挂载 |

### 3.3 Java / Native 不一致

```json
"javaNativeMismatches": [
  "/debug_ramdisk/magisk (native可访问, Java=false)"
]
```

说明 **Java `File.exists()` 已被 Hide**，但 **Native `access()` 未 Hook**——这是「Hide 疑似」的核心逻辑。

### 3.4 `hideSuspected` 触发条件（`RootFrameworkDetector`）

```
hideSuspected =
  (maps/mount 命中 OR native 命中 OR boot 解锁信号 OR Java/Native 路径不一致)
  AND Java 层路径全不可见
  AND su 未授权
```

本次满足：**native 命中 + Java/Native 不一致 + 路径不可见 + su 拒绝**。

---

## 4. 各检测模块表现

| 模块 | 文件 | 本次结果 |
|------|------|----------|
| Hook 框架类/文件/maps | `CheckEmu` | 未命中（Java maps 已过滤） |
| 属性多通道 | `CheckEmu.probeProperty` | 未篡改 |
| Root 路径 / su / which | `CheckEmu.buildRootIndicators` | 未命中 |
| 框架分类（Java） | `RootFrameworkDetector.probeFramework` | 四类均为 false |
| **Native 探测** | `device.cpp` → `getMagiskNativeProbe` | **命中** |
| su 授权 | `RootAccessHelper` | 拒绝 |

---

## 5. Hide 已生效 vs 未生效

### 已 Hide

- Java 文件路径扫描
- Shell（`getprop`、`which su`）
- su 授权（DenyList）
- 系统属性（`release-keys`、`verifiedbootstate=green`）
- Java 层 `/proc/self/maps` 关键词

### 未 Hide（导致风控命中）

- Native `JniInterface.getMagiskNativeProbe()`
- Native `access()` 与 `fopen(/proc/self/maps|mountinfo)`

---

## 6. 绕过 Hide 需补齐的 Hook 面（测试参考）

1. **Native JNI**：Hook `getMagiskNativeProbe` 返回值，或 Hook `access` / `fopen` / `fgets` 过滤 magisk/zygisk/ksu 行。
2. **Java/Native 一致性**：对 `/debug_ramdisk/magisk` 等路径，Native `access` 也需返回失败。
3. **Zygisk maps 痕迹**：仅 Hook Java `BufferedReader` 不够，需在 Native 读 maps 处过滤。

---

## 7. 误判修复记录（已实现）

| 风险 | 说明 | 修复 |
|------|------|------|
| 仅 Bootloader 解锁 | `verifiedbootstate=orange` 不等于 Root Hide | Boot 解锁不再触发 `hideSuspected`；新增 `bootloaderUnlocked` 独立字段 |
| `system_overlay` | 正版 Android overlayfs 挂载 | 移除通用 overlay 命中 |
| 短关键词 `ksu`/`apd` | maps 子串误匹配 | Java/Native 改为 ≥5 字符或路径前缀匹配 |
| Native 命中未归因 | `magiskDetected=false` 但 Native 有信号 | `applyNativeProbeToFrameworks` 回写框架 `detected` |
| `isRooted` 仅因疑似 | 疑似隐藏不应等同确认 Root | `isRooted` 仅 `frameworkConfirmed` 或经典指标 |
| 重复 reason | rootProbe 与 buildRootReasons 双写 | 去除 buildRootReasons 中重复文案 |
| Native 检测系统 su 路径 | `/system/bin/su` 引发误不一致 | Native 路径列表移除系统 su |

---

## 8. 相关源码索引

| 职责 | 路径 |
|------|------|
| 安全报告聚合 | `app/.../SecurityReportComposer.java` |
| Hook / 属性探测 | `app/.../CheckEmu.java` |
| Root 框架探测 | `app/.../RootFrameworkDetector.java` |
| su 授权探测 | `app/.../RootAccessHelper.java` |
| JNI 属性封装 | `app/.../Jni/JniPropertyHelper.java` |
| Native Root 探测 | `app/src/main/cpp/device.cpp` |
| 采集与导出 | `app/.../DeviceSnapshotMerger.java` |

---

*文档生成：device-collection 安全模块分析 · Yumito*
