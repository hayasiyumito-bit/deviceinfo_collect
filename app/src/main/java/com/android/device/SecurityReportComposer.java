package com.android.device;

import android.content.Context;
import android.util.Log;

import com.android.device.env.HackChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 将 Hook / Root / 环境 / 模拟器检测合并为单一 {@code security} 块，供 UI 与 debug_output.json 使用。
 */
public final class SecurityReportComposer {

    private static final String TAG = "SecurityReportComposer";

    private SecurityReportComposer() {
    }

    public static JSONObject build(Context context) {
        JSONObject security = new JSONObject();
        try {
            JSONObject hookSection = CheckEmu.buildHookSection();
            JSONObject rootSection = CheckEmu.buildRootSection(context);
            JSONObject environment = HackChecker.getEnvCheckerInfo(context);
            JSONObject simulator = SimulatorChecker.buildDetail(context);

            JSONArray hookReasons = CheckEmu.buildHookReasons(hookSection);
            JSONArray rootReasons = CheckEmu.buildRootReasons(rootSection);
            JSONArray propertyTamperReasons = CheckEmu.buildPropertyTamperReasons(hookSection);
            JSONArray environmentReasons = HackChecker.collectDetectionReasons(context);
            JSONArray simulatorReasons = SimulatorChecker.collectDetectionReasons(context);
            JSONArray adbReasons = buildAdbReasons(context);

            JSONObject summary = buildSummary(
                    hookSection,
                    rootSection,
                    environment,
                    simulator,
                    context
            );
            JSONObject reasons = buildReasons(
                    hookReasons,
                    rootReasons,
                    propertyTamperReasons,
                    environmentReasons,
                    simulatorReasons,
                    adbReasons
            );

            security.put("summary", summary);
            security.put("reasons", reasons);
            boolean anyRisk = summary.optBoolean("anyRisk", false);
            security.put("anyRisk", anyRisk);
            security.put("anyRiskReasons", buildAnyRiskReasons(summary, reasons));
            security.put("hook", hookSection);
            security.put("root", rootSection);
            security.put("environment", environment);
            security.put("simulator", simulator);
            security.put("collection", buildCollectionMeta());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build security report", e);
            try {
                security.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return security;
    }

    private static JSONObject buildSummary(
            JSONObject hookSection,
            JSONObject rootSection,
            JSONObject environment,
            JSONObject simulator,
            Context context
    ) throws JSONException {
        JSONObject summary = new JSONObject();
        boolean hookFrameworkDetected = hookSection.optBoolean("frameworkDetected", false);
        boolean propertyTampered = hookSection.optBoolean("propertyTampered", false);
        boolean isRooted = rootSection.optBoolean("isRooted", false);
        boolean magiskDetected = rootSection.optBoolean("magiskDetected", false);
        boolean kernelsuDetected = rootSection.optBoolean("kernelsuDetected", false);
        boolean apatchDetected = rootSection.optBoolean("apatchDetected", false);
        boolean systemSuDetected = rootSection.optBoolean("systemSuDetected", false);
        boolean magiskHideSuspected = rootSection.optBoolean("magiskHideSuspected", false);
        boolean rootAccessGranted = rootSection.optBoolean("accessGranted", false);
        boolean isEmulator = environment.optBoolean("isEmulator", false);
        boolean isVpn = environment.optBoolean("isVPN", false);
        boolean isDebug = environment.optBoolean("isDebug", false);
        boolean simulatorDetected = simulator.optBoolean("detected", false);
        boolean isAdbEnabled = CheckEmu.isAdbEnabled(context);

        summary.put("hookFrameworkDetected", hookFrameworkDetected);
        summary.put("propertyTampered", propertyTampered);
        summary.put("isRooted", isRooted);
        summary.put("magiskDetected", magiskDetected);
        summary.put("kernelsuDetected", kernelsuDetected);
        summary.put("apatchDetected", apatchDetected);
        summary.put("systemSuDetected", systemSuDetected);
        summary.put("magiskHideSuspected", magiskHideSuspected);
        summary.put("rootAccessGranted", rootAccessGranted);
        summary.put("isEmulator", isEmulator);
        summary.put("isVpn", isVpn);
        summary.put("isDebug", isDebug);
        summary.put("simulatorDetected", simulatorDetected);
        summary.put("isAdbEnabled", isAdbEnabled);
        summary.put("anyHookSignal", hookFrameworkDetected || propertyTampered);
        summary.put("anyRootSignal", isRooted || rootAccessGranted || magiskDetected
                || kernelsuDetected || apatchDetected || systemSuDetected || magiskHideSuspected);
        summary.put("anyRisk", hookFrameworkDetected
                || propertyTampered
                || isRooted
                || magiskDetected
                || kernelsuDetected
                || apatchDetected
                || systemSuDetected
                || magiskHideSuspected
                || rootAccessGranted
                || isEmulator
                || simulatorDetected
                || isVpn
                || isDebug
                || isAdbEnabled);
        return summary;
    }

    private static JSONArray buildAnyRiskReasons(JSONObject summary, JSONObject reasons) {
        Set<String> unique = new LinkedHashSet<>();
        if (reasons == null) {
            return new JSONArray();
        }
        appendReasonsIf(summary, reasons, unique, "hookFrameworkDetected", "propertyTampered", "hook");
        appendReasonsIf(summary, reasons, unique, "propertyTampered", "propertyTamper");
        appendReasonsIf(summary, reasons, unique, "isRooted", "rootAccessGranted", "root");
        appendReasonsIf(summary, reasons, unique, "magiskDetected", "magiskHideSuspected", "root");
        appendReasonsIf(summary, reasons, unique, "kernelsuDetected", "kernelsuDetected", "root");
        appendReasonsIf(summary, reasons, unique, "apatchDetected", "apatchDetected", "root");
        appendReasonsIf(summary, reasons, unique, "systemSuDetected", "rootAccessGranted", "root");
        if (summary.optBoolean("isEmulator", false)
                || summary.optBoolean("isVpn", false)
                || summary.optBoolean("isDebug", false)) {
            appendAll(reasons.optJSONArray("environment"), unique);
        }
        appendReasonsIf(summary, reasons, unique, "simulatorDetected", "simulator");
        appendReasonsIf(summary, reasons, unique, "isAdbEnabled", "adb");
        JSONArray all = new JSONArray();
        for (String reason : unique) {
            all.put(reason);
        }
        return all;
    }

    private static void appendAll(JSONArray source, Set<String> target) {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.length(); i++) {
            String value = source.optString(i);
            if (!value.isEmpty()) {
                target.add(value);
            }
        }
    }

    private static void appendReasonsIf(
            JSONObject summary,
            JSONObject reasons,
            Set<String> target,
            String summaryFlag,
            String reasonKey
    ) {
        appendReasonsIf(summary, reasons, target, summaryFlag, summaryFlag, reasonKey);
    }

    private static void appendReasonsIf(
            JSONObject summary,
            JSONObject reasons,
            Set<String> target,
            String summaryFlag,
            String alternateFlag,
            String reasonKey
    ) {
        if (!summary.optBoolean(summaryFlag, false) && !summary.optBoolean(alternateFlag, false)) {
            return;
        }
        appendAll(reasons.optJSONArray(reasonKey), target);
    }

    private static JSONObject buildReasons(
            JSONArray hookReasons,
            JSONArray rootReasons,
            JSONArray propertyTamperReasons,
            JSONArray environmentReasons,
            JSONArray simulatorReasons,
            JSONArray adbReasons
    ) throws JSONException {
        JSONObject reasons = new JSONObject();
        reasons.put("hook", hookReasons);
        reasons.put("root", rootReasons);
        reasons.put("propertyTamper", propertyTamperReasons);
        reasons.put("environment", environmentReasons);
        reasons.put("simulator", simulatorReasons);
        reasons.put("adb", adbReasons);
        return reasons;
    }

    private static JSONArray buildAdbReasons(Context context) throws JSONException {
        JSONArray reasons = new JSONArray();
        if (CheckEmu.isAdbEnabled(context)) {
            reasons.put("ADB 调试已开启 (Settings.Secure.ADB_ENABLED=1)");
        }
        return reasons;
    }

    private static JSONObject buildCollectionMeta() throws JSONException {
        JSONObject meta = new JSONObject();
        meta.put("rootProbeSequence", RootAccessHelper.getAttemptSequence());
        return meta;
    }
}
