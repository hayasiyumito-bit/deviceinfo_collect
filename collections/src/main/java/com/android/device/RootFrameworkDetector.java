package com.android.device;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.android.device.Jni.JniPropertyHelper;
import com.android.utils.Cmd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Root 框架分类检测：Magisk、KernelSU、APatch、系统 su。
 */
public final class RootFrameworkDetector {

    private static final String TAG = "RootFrameworkDetector";

    private static final String ID_MAGISK = "magisk";
    private static final String ID_KERNELSU = "kernelsu";
    private static final String ID_KERNELSU_BACKUP = "kernelsuBackup";
    private static final String ID_APATCH = "apatch";
    private static final String ID_APATCH_ENHANCED = "apatchEnhanced";
    private static final String ID_SYSTEM_SU = "systemSu";
    private static final String ID_SU_BINARY = "suBinary";
    private static final String ID_ROOT_MANAGER = "rootManager";
    private static final String ID_BUSYBOX = "busybox";
    private static final String ID_ROOT_HIDE = "rootHide";
    private static final String ID_DANGEROUS_APP = "dangerousApp";

    private static final String[] MAGISK_PATHS = {
            "/sbin/magisk",
            "/sbin/magiskpolicy",
            "/sbin/.magisk",
            "/debug_ramdisk/magisk",
            "/debug_ramdisk/.magisk",
            "/data/adb/modules/zygisk",
            "/dev/.magisk_unblock",
            "/apex/com.android.art/.magisk",
            "/sbin/.magisk",
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/magisk.img",
            "/data/adb/magisk/busybox",
            "/data/adb/modules",
            "/data/adb/post-fs-data.d",
            "/data/adb/service.d",
            "/cache/magisk.log",
            "/data/magisk.apk",
            "/system/xbin/magisk",
            "/system/bin/magisk",
            "/vendor/bin/magisk",
            "/product/bin/magisk",
            "/persist/magisk",
            "/metadata/magisk",
            "/system/app/Magisk",
            "/system/etc/init/magisk.rc"
    };

    private static final String[] KERNELSU_PATHS = {
            "/data/adb/ksu",
            "/data/adb/ksud",
            "/data/adb/ksu/bin",
            "/data/adb/ksu/modules",
            "/sys/fs/ksu",
            "/dev/ksu",
            "/proc/ksu",
            "/data/adb/.ksu",
            "/data/adb/ksu/.allowlist"
    };

    private static final String[] KERNELSU_BACKUP_PATHS = {
            "/data/adb/kernelsu",
            "/data/adb/kernelsu/bin",
            "/data/adb/kernelsu/modules",
            "/system/bin/kernelsu",
            "/system/xbin/kernelsu",
            "/sbin/ksu",
            "/sbin/ksud",
            "/system/ksu",
            "/system/bin/ksu",
            "/system/xbin/ksu",
            "/system/xbin/ksud",
            "/vendor/bin/ksu",
            "/product/bin/ksu",
            "/oem/bin/ksu",
            "/system_ext/bin/ksu",
            "/data/adb/.kernelsu",
            "/data/adb/ksu/.modules",
            "/data/adb/ksu/.bin"
    };

    private static final String[] APATCH_PATHS = {
            "/data/adb/ap",
            "/data/adb/apd",
            "/data/adb/ap/bin",
            "/data/adb/apd.apk",
            "/data/adb/ap/modules",
            "/data/adb/ap/log"
    };

    private static final String[] APATCH_ENHANCED_PATHS = {
            "/data/adb/ap/superkey",
            "/data/adb/apd/superkey",
            "/data/adb/ap/package_config",
            "/data/adb/apd/package_config",
            "/data/adb/ap/bin/apd",
            "/data/adb/ap/bin/ap"
    };

    private static final String[] BUSYBOX_PATHS = {
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/sbin/busybox",
            "/vendor/bin/busybox",
            "/data/adb/magisk/busybox",
            "/data/adb/ksu/bin/busybox",
            "/data/adb/ap/bin/busybox",
            "/data/local/busybox",
            "/data/local/bin/busybox",
            "/data/local/xbin/busybox",
            "/system/sbin/busybox",
            "/system/usr/bin/busybox",
            "/system/usr/xbin/busybox",
            "/product/bin/busybox",
            "/oem/bin/busybox",
            "/system_ext/bin/busybox",
            "/cache/busybox",
            "/data/busybox",
            "/data/system/busybox",
            "/data/data/busybox",
            "/sdcard/busybox",
            "/storage/emulated/0/busybox",
            "/data/tmp/busybox",
            "/tmp/busybox",
            "/system/vendor/bin/busybox",
            "/vendor/xbin/busybox",
            "/system/fonts/busybox",
            "/system/framework/busybox"
    };

    private static final String[] SYSTEM_SU_PATHS = {
            "/system/usr/we-need-root/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU",
            "/system/etc/init.d/99SuperSUDaemon",
            "/system/xbin/daemonsu",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/cache/su",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/vendor/bin/su",
            "/product/bin/su",
            "/oem/bin/su",
            "/system_ext/bin/su",
            "/data/adb/su",
            "/data/adb/magisk/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/data/su",
            "/data/system/su",
            "/system/usr/bin/su",
            "/system/usr/xbin/su",
            "/system/fonts/su",
            "/system/framework/su",
            "/system/media/su",
            "/data/data/com.noshufou.android.su",
            "/data/data/eu.chainfire.supersu",
            "/data/data/com.koushikdutta.superuser"
    };

    private static final String[] MAGISK_PACKAGES = {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk"
    };

    private static final String[] KERNELSU_PACKAGES = {
            "me.weishu.kernelsu",
            "io.github.huskydg.kernelsu",
            "com.kernel.su",
            "kernelsu"
    };

    private static final String[] APATCH_PACKAGES = {
            "me.bmax.apatch",
            "com.bmax.apatch",
            "bmax.apatch"
    };

    private static final String[] SYSTEM_SU_PACKAGES = {
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "com.noshufou.android.su.elite",
            "com.miui.securitycenter",
            "com.kingroot.master",
            "com.kingstudio.kingroot",
            "com.baidu.root",
            "com.baidu.superroot",
            "com.jumobile.superuser",
            "com.coolapk.roothelper",
            "com.speedsoftware.rootexplorer",
            "com.speedsoftware.rootexplorerfree",
            "com.katecca.pureexplorer",
            "com.katecca.pureexplorerbeta",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "com.formyhm.hideroot",
            "com.formyhm.hiderootPremium",
            "com.mattmags.roothide",
            "me.phh.superuser",
            "com.kolibrie.superuser",
            "com.keramidas.TitaniumBackup",
            "com.keramidas.TitaniumBackupPro",
            "com.bigboot.supersu",
            "com.supersu",
            "su.root",
            "root.su",
            "com.android.superuser"
    };

    private static final String[] DANGEROUS_APP_PACKAGES = {
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.forpda.lp",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.android.vending.billing.InAppBillingService.CLON",
            "com.android.vending.billing.InAppBillingService.CRACK",
            "com.android.protips",
            "com.kingroot.kinguser",
            "com.kingroot.master",
            "com.kingstudio.kingroot",
            "com.mumu.launcher",
            "com.ami.duosupdater",
            "com.bluestacks.appmart",
            "com.bignox.app.store.hd",
            "com.vphone.launcher",
            "com.vphone.helper",
            "com.google.android.launcher.layouts.xposed",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.topjohnwu.magisk",
            "me.weishu.kernelsu",
            "org.apatch.manager",
            "com.koushikdutta.rommanager",
            "com.koushikdutta.rommanager.license",
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.ramdroid.appquarantinepro",
            "com.android.vending.billing.InAppBillingService.LOCK",
            "com.allinone.free",
            "com.repodroid.app",
            "org.creeplays.creehack",
            "com.baseapp.eynav",
            "com.applisto.appcloner",
            "com.applisto.appcloner.premium",
            "com.guoshi.httpcanary",
            "com.guoshi.httpcanary.premium",
            "com.minvayu.tortoisegit",
            "org.proxy.core",
            "com.proxy.vpn",
            "com.evonode.juggler",
            "com.vproxymanager",
            "com.github.metacubex.clash_meta",
            "com.github.kr328.clash",
            "com.kitsunemask",
            "com.gameguardian.devtools",
            "catchme.if.you.can",
            "com.keramidas.TitaniumBackup",
            "com.keramidas.TitaniumBackupPro",
            "com.speedsoftware.rootexplorer",
            "com.speedsoftware.rootexplorerfree",
            "com.katecca.pureexplorer",
            "com.katecca.pureexplorerbeta",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "com.formyhm.hideroot",
            "com.formyhm.hiderootPremium",
            "com.mattmags.roothide",
            "io.github.libxposed.service",
            "io.github.suika.hidemyapplist",
            "cn.geektang.privacyspace",
            "com.tsng.hidemyapplist",
            "io.github.vvb2060.magisk",
            "com.topjohnwu.magisk.debug",
            "com.topjohnwu.magisk.alpha",
            "com.rifsxd.ksunext",
            "com.rifsxd.ksunext.ui",
            "me.bmax.apatch",
            "com.bmax.apatch",
            "bmax.apatch",
            "com.baidu.root",
            "com.baidu.superroot",
            "com.jumobile.superuser",
            "com.coolapk.roothelper",
            "me.phh.superuser",
            "com.kolibrie.superuser",
            "com.bigboot.supersu",
            "com.jrummy.app.rootexplorer",
            "com.jrummy.liberty.toolbox",
            "com.jrummyapps.rom.toolbox",
            "com.jrummyapps.busybox.installer",
            "com.stericson.busybox",
            "com.stericson.roottools",
            "com.saurik.substrate",
            "de.robv.android.xposed.installer",
            "org.lsposed.lspd",
            "org.lsposed.manager",
            "com.github.lsposed.lspd",
            "io.github.lsposed.lspd",
            "com.github.lsposed.manager",
            "io.github.lsposed.manager",
            "com.lody.virtual",
            "com.lody.virtual.client",
            "org.proxyee.up",
            "org.proxyee.up.free",
            "com.github.shadowsocks",
            "com.github.shadowsocksr",
            "com.github.shadowsocksrss",
            "com.github.shadowsocksrss.pre",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot",
            "com.yellowes.su",
            "com.thirdparty.superuser",
            "com.jrummy.liberty.toolbox",
            "com.jrummyapps.busybox.installer",
            "com.jrummyapps.rom.toolbox",
            "com.coolapk.roothelper",
            "com.coolapk.market",
            "com.coolapk.gallery",
            "com.coolapk.cloud",
            "com.coolapk.appupdate",
            "com.coolapk.wallpaper",
            "com.coolapk.safe",
            "com.coolapk.cleaner",
            "com.coolapk.optimizer",
            "com.coolapk.battery",
            "com.coolapk.camera",
            "com.coolapk.video",
            "com.coolapk.music",
            "com.coolapk.news",
            "com.coolapk.weather",
            "com.coolapk.calendar",
            "com.coolapk.contacts",
            "com.coolapk.sms",
            "com.coolapk.email",
            "com.coolapk.browser",
            "com.coolapk.appstore",
            "com.coolapk.gamecenter",
            "com.coolapk.watch",
            "com.coolapk.car",
            "com.coolapk.tv",
            "com.coolapk.wear",
            "com.coolapk.android",
            "com.coolapk.cm",
            "com.coolapk.miui",
            "com.coolapk.samsung",
            "com.coolapk.huawei",
            "com.coolapk.xiaomi",
            "com.coolapk.oppo",
            "com.coolapk.vivo",
            "com.coolapk.meizu",
            "com.coolapk.lenovo",
            "com.coolapk.asus",
            "com.coolapk.sony",
            "com.coolapk.lg",
            "com.coolapk.motorola",
            "com.coolapk.htc",
            "com.coolapk.oneplus",
            "com.coolapk.realme",
            "com.coolapk.nubia",
            "com.coolapk.zte",
            "com.coolapk.honor",
            "com.coolapk.iqoo",
            "com.coolapk.blackshark",
            "com.coolapk.redmi",
            "com.coolapk.poco",
            "com.coolapk.smartisan",
            "com.coolapk.meitu",
            "com.coolapk.vivo",
            "com.coolapk.samsung",
            "com.coolapk.huawei",
            "com.coolapk.xiaomi",
            "com.coolapk.oppo",
            "com.coolapk.meizu",
            "com.coolapk.lenovo",
            "com.coolapk.asus",
            "com.coolapk.sony",
            "com.coolapk.lg",
            "com.coolapk.motorola",
            "com.coolapk.htc",
            "com.coolapk.oneplus",
            "com.coolapk.realme",
            "com.coolapk.nubia",
            "com.coolapk.zte",
            "com.coolapk.honor",
            "com.coolapk.iqoo",
            "com.coolapk.blackshark",
            "com.coolapk.redmi",
            "com.coolapk.poco",
            "com.coolap3k.smartisan",
            "com.coolapk.meitu"
    };

    private static final String[] MAGISK_MAPS_KEYWORDS = {
            "magisk", "zygisk", "magiskpolicy", "magisk32", "magisk64", "resetprop", "/data/adb/magisk"
    };

    private static final String[] KERNELSU_MAPS_KEYWORDS = {
            "kernelsu", "ksud", "kernel_su", "/data/adb/ksu", "/data/adb/kernelsu"
    };

    private static final String[] APATCH_MAPS_KEYWORDS = {
            "apatch", "bmax", "/data/adb/ap", "/data/adb/apd"
    };

    private static final String[] SYSTEM_SU_MAPS_KEYWORDS = {
            "/system/bin/su", "/system/xbin/su", "supersu", "daemonsu", "superuser"
    };

    private static final String[] MAGISK_PROP_KEYS = {
            "init.svc.magisk",
            "init.svc.magisk_daemon",
            "init.svc.magisk_service",
            "ro.magisk.version",
            "persist.magisk.version",
            "magisk.version"
    };

    private static final String[] KERNELSU_PROP_KEYS = {
            "persist.sys.kernelsu",
            "ro.kernel.su",
            "persist.sys.ksu",
            "init.svc.ksud",
            "init.svc.kernelsu",
            "ksu.version"
    };

    private static final String[] APATCH_PROP_KEYS = {
            "init.svc.apd",
            "init.svc.apatch",
            "ro.apatch.version",
            "persist.apatch.version",
            "apatch.version"
    };

    private static final String[] BOOT_UNLOCK_PROPS = {
            "ro.boot.verifiedbootstate",
            "ro.boot.flash.locked",
            "ro.boot.vbmeta.device_state",
            "ro.boot.warranty_bit",
            "ro.boot.veritymode",
            "ro.oem_unlock_supported"
    };

    private RootFrameworkDetector() {
    }

    public static JSONObject probe(Context context) {
        JSONObject result = new JSONObject();
        JSONArray combinedReasons = new JSONArray();
        try {
            SharedContext shared = SharedContext.collect(context);

            JSONObject magisk = probeFramework(
                    ID_MAGISK, "Magisk", MAGISK_PATHS, MAGISK_PACKAGES, MAGISK_MAPS_KEYWORDS,
                    MAGISK_PROP_KEYS, shared, true);
            JSONObject kernelsu = probeFramework(
                    ID_KERNELSU, "KernelSU", KERNELSU_PATHS, KERNELSU_PACKAGES, KERNELSU_MAPS_KEYWORDS,
                    KERNELSU_PROP_KEYS, shared, false);
            JSONObject kernelsuBackup = probeFramework(
                    ID_KERNELSU_BACKUP, com.android.device.i18n.AppLocale.tr("KernelSU (备选)", "KernelSU (fallback)"), KERNELSU_BACKUP_PATHS, new String[0], KERNELSU_MAPS_KEYWORDS,
                    new String[0], shared, false);
            JSONObject apatch = probeFramework(
                    ID_APATCH, "APatch", APATCH_PATHS, APATCH_PACKAGES, APATCH_MAPS_KEYWORDS,
                    APATCH_PROP_KEYS, shared, false);
            JSONObject apatchEnhanced = probeFramework(
                    ID_APATCH_ENHANCED, com.android.device.i18n.AppLocale.tr("APatch (增强型)", "APatch (enhanced)"), APATCH_ENHANCED_PATHS, new String[0], APATCH_MAPS_KEYWORDS,
                    new String[0], shared, false);
            JSONObject systemSu = probeSystemSu(shared);
            JSONObject suBinary = probeSuBinary(shared);
            JSONObject rootManager = probeRootManager(shared);
            JSONObject busybox = probeBusyBox();
            JSONObject rootHide = probeRootHide(shared);
            JSONObject dangerousApp = probeDangerousApp(shared);

            applyNativeProbeToFrameworks(magisk, kernelsu, apatch, shared.nativeProbe);

            JSONObject frameworks = new JSONObject();
            frameworks.put(ID_MAGISK, magisk);
            frameworks.put(ID_KERNELSU, kernelsu);
            frameworks.put(ID_KERNELSU_BACKUP, kernelsuBackup);
            frameworks.put(ID_APATCH, apatch);
            frameworks.put(ID_APATCH_ENHANCED, apatchEnhanced);
            frameworks.put(ID_SYSTEM_SU, systemSu);
            frameworks.put(ID_SU_BINARY, suBinary);
            frameworks.put(ID_ROOT_MANAGER, rootManager);
            frameworks.put(ID_BUSYBOX, busybox);
            frameworks.put(ID_ROOT_HIDE, rootHide);
            frameworks.put(ID_DANGEROUS_APP, dangerousApp);

            // 后处理：shellHits 有内容时强制 detected=true（Cmd.exe 在受限环境下可能未命中关键词）
            enforceShellHitDetection(magisk);
            enforceShellHitDetection(kernelsu);
            enforceShellHitDetection(kernelsuBackup);
            enforceShellHitDetection(apatch);
            enforceShellHitDetection(apatchEnhanced);
            enforceShellHitDetection(systemSu);
            enforceShellHitDetection(suBinary);
            enforceShellHitDetection(rootManager);
            enforceShellHitDetection(busybox);
            enforceShellHitDetection(rootHide);
            enforceShellHitDetection(dangerousApp);

            boolean magiskDetected = magisk.optBoolean("detected", false);
            boolean kernelsuDetected = kernelsu.optBoolean("detected", false) || kernelsuBackup.optBoolean("detected", false);
            boolean apatchDetected = apatch.optBoolean("detected", false) || apatchEnhanced.optBoolean("detected", false);
            boolean systemSuDetected = systemSu.optBoolean("detected", false) || suBinary.optBoolean("detected", false);
            boolean frameworkConfirmed = magiskDetected || kernelsuDetected || apatchDetected || systemSuDetected
                    || rootManager.optBoolean("detected", false);
            boolean hideSuspected = shared.hideSuspected && !frameworkConfirmed;

            mergeReasons(combinedReasons, magisk.optJSONArray("reasons"));
            mergeReasons(combinedReasons, kernelsu.optJSONArray("reasons"));
            mergeReasons(combinedReasons, kernelsuBackup.optJSONArray("reasons"));
            mergeReasons(combinedReasons, apatch.optJSONArray("reasons"));
            mergeReasons(combinedReasons, apatchEnhanced.optJSONArray("reasons"));
            mergeReasons(combinedReasons, systemSu.optJSONArray("reasons"));
            mergeReasons(combinedReasons, suBinary.optJSONArray("reasons"));
            mergeReasons(combinedReasons, rootManager.optJSONArray("reasons"));
            mergeReasons(combinedReasons, busybox.optJSONArray("reasons"));
            mergeReasons(combinedReasons, rootHide.optJSONArray("reasons"));
            mergeReasons(combinedReasons, dangerousApp.optJSONArray("reasons"));
            if (hideSuspected) {
                combinedReasons.put(buildHideSuspectedReason(shared));
            }

            JSONObject sharedIndicators = new JSONObject();
            sharedIndicators.put("mapsHits", shared.mapsHits);
            sharedIndicators.put("mountHits", shared.mountHits);
            sharedIndicators.put("mountInfoHits", shared.mountInfoHits);
            sharedIndicators.put("bootUnlockSignals", shared.bootUnlockSignals);
            sharedIndicators.put("bootloaderUnlocked", shared.bootUnlockSignals.length() > 0);
            sharedIndicators.put("buildMismatches", shared.buildMismatches);
            sharedIndicators.put("selinuxMode", shared.selinuxMode);
            sharedIndicators.put("nativeProbe", shared.nativeProbe);
            sharedIndicators.put("javaNativeMismatches", shared.javaNativeMismatches);
            sharedIndicators.put("envHits", shared.envHits);
            sharedIndicators.put("hideSuspected", hideSuspected);
            sharedIndicators.put("persieAligned", shared.persieAligned);

            result.put("detected", frameworkConfirmed || hideSuspected);
            result.put("persieAligned", shared.persieAligned);
            result.put("frameworkConfirmed", frameworkConfirmed);
            result.put("magiskDetected", magiskDetected);
            result.put("kernelsuDetected", kernelsuDetected);
            result.put("kernelsuBackupDetected", kernelsuBackup.optBoolean("detected", false));
            result.put("apatchDetected", apatchDetected);
            result.put("apatchEnhancedDetected", apatchEnhanced.optBoolean("detected", false));
            result.put("systemSuDetected", systemSuDetected);
            result.put("suBinaryFound", suBinary.optBoolean("detected", false));
            result.put("rootManagerDetected", rootManager.optBoolean("detected", false));
            result.put("busyboxDetected", busybox.optBoolean("detected", false));
            result.put("rootHideDetected", rootHide.optBoolean("detected", false));
            result.put("dangerousAppDetected", dangerousApp.optBoolean("detected", false));
            result.put("hideSuspected", hideSuspected);
            result.put("frameworks", frameworks);
            result.put("sharedIndicators", sharedIndicators);
            result.put("reasons", combinedReasons);
            // 兼容旧字段：magisk 块保留为完整探测结果
            result.put("indicators", sharedIndicators);
        } catch (JSONException e) {
            Log.e(TAG, "Root framework probe failed", e);
            try {
                result.put("error", e.getMessage());
            } catch (JSONException ignored) {
            }
        }
        return result;
    }

    private static JSONObject probeFramework(
            String id,
            String displayName,
            String[] paths,
            String[] packages,
            String[] mapsKeywords,
            String[] propKeys,
            SharedContext shared,
            boolean includeMagiskShell
    ) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(paths);
        JSONArray shellPaths = scanPathsViaShell(paths);
        JSONArray packageHits = filterPackages(shared.installedPackages, packages);
        JSONArray mapsHits = filterKeywordHits(shared.mapsHits, mapsKeywords);
        JSONArray mountHits = filterKeywordHits(shared.mountHits, mapsKeywords);
        JSONArray mountInfoHits = filterKeywordHits(shared.mountInfoHits, mapsKeywords);
        JSONArray propHits = collectPropertyHits(shared.allProps, propKeys);
        JSONArray shellHits = includeMagiskShell
                ? collectMagiskShellHits() : collectFrameworkShellHits(id);
        JSONArray envHits = filterKeywordHits(shared.envHits, mapsKeywords);

        boolean pathHit = matchedPaths.length() > 0 || shellPaths.length() > 0;
        boolean packageHit = packageHits.length() > 0;
        boolean mapsHit = mapsHits.length() > 0;
        boolean mountHit = mountHits.length() > 0 || mountInfoHits.length() > 0;
        boolean propHit = propHits.length() > 0;
        boolean shellHit = shellHits.length() > 0;
        boolean envHit = envHits.length() > 0;
        boolean suLinked = ID_MAGISK.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("magisk");
        boolean suLinkedKsu = ID_KERNELSU.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("ksu");
        boolean suLinkedApd = ID_APATCH.equals(id) && shared.suReadlink.toLowerCase(Locale.US).contains("apd");

        indicators.put("matchedPaths", matchedPaths);
        indicators.put("shellPaths", shellPaths);
        indicators.put("packageHits", packageHits);
        indicators.put("mapsHits", mapsHits);
        indicators.put("mountHits", mountHits);
        indicators.put("mountInfoHits", mountInfoHits);
        indicators.put("propertyHits", propHits);
        indicators.put("shellHits", shellHits);
        indicators.put("envHits", envHits);
        indicators.put("suReadlink", shared.suReadlink);

        appendPathReasons(reasons, displayName, matchedPaths);
        appendShellPathReasons(reasons, shellPaths);
        appendArrayReasons(reasons, packageHits, displayName + com.android.device.i18n.AppLocale.tr(" 安装包", " package"));
        appendArrayReasons(reasons, mapsHits, displayName + com.android.device.i18n.AppLocale.tr(" maps 命中", " maps hit"));
        appendArrayReasons(reasons, mountHits, displayName + com.android.device.i18n.AppLocale.tr(" mounts 命中", " mounts hit"));
        appendArrayReasons(reasons, mountInfoHits, displayName + com.android.device.i18n.AppLocale.tr(" mountinfo 命中", " mountinfo hit"));
        appendArrayReasons(reasons, propHits, displayName + com.android.device.i18n.AppLocale.tr(" 属性", " property"));
        appendArrayReasons(reasons, shellHits, displayName + " Shell");
        appendArrayReasons(reasons, envHits, displayName + com.android.device.i18n.AppLocale.tr(" 环境变量", " env var"));
        if (suLinked) {
            reasons.put(com.android.device.i18n.AppLocale.tr("su 链接到 Magisk: ", "su links to Magisk: ") + shared.suReadlink);
        }
        if (suLinkedKsu) {
            reasons.put(com.android.device.i18n.AppLocale.tr("su 链接到 KernelSU: ", "su links to KernelSU: ") + shared.suReadlink);
        }
        if (suLinkedApd) {
            reasons.put(com.android.device.i18n.AppLocale.tr("su 链接到 APatch: ", "su links to APatch: ") + shared.suReadlink);
        }

        boolean detected = pathHit || packageHit || mapsHit || mountHit || propHit || shellHit
                || envHit || suLinked || suLinkedKsu || suLinkedApd;

        result.put("id", id);
        result.put("displayName", displayName);
        result.put("detected", detected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    /** 标准 Android 系统中已知存在 su 二进制但不代表 Root 的路径。 */
    private static final String[] STANDARD_SYSTEM_SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sbin/su"
    };

    private static JSONObject probeSuBinary(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(SYSTEM_SU_PATHS);
        JSONArray shellPaths = scanPathsViaShell(SYSTEM_SU_PATHS);
        boolean suWhichHit = !shared.suWhichPath.isEmpty() && !shared.suWhichPath.contains("not found");

        // 过滤标准系统路径：/system/bin/su 等在标准 Android 中存在不代表 Root
        JSONArray filteredPaths = filterStandardSystemPaths(matchedPaths);
        JSONArray filteredShellPaths = filterStandardSystemPaths(shellPaths);

        indicators.put("matchedPaths", filteredPaths);
        indicators.put("shellPaths", filteredShellPaths);
        indicators.put("allMatchedPaths", matchedPaths);
        indicators.put("suWhichPath", shared.suWhichPath);

        appendPathReasons(reasons, com.android.device.i18n.AppLocale.tr("SU 可执行文件", "SU binary"), filteredPaths);
        appendShellPathReasons(reasons, filteredShellPaths);
        if (suWhichHit) {
            reasons.put(com.android.device.i18n.AppLocale.tr("which su 探测到: ", "which su found: ") + shared.suWhichPath);
        }

        boolean detected = filteredPaths.length() > 0 || filteredShellPaths.length() > 0 || suWhichHit;
        result.put("id", ID_SU_BINARY);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("找到 SU 可执行文件", "SU binary found"));
        result.put("detected", detected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONArray filterStandardSystemPaths(JSONArray paths) throws JSONException {
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < paths.length(); i++) {
            String path = paths.optString(i);
            boolean isStandard = false;
            for (String stdPath : STANDARD_SYSTEM_SU_PATHS) {
                if (stdPath.equals(path)) {
                    isStandard = true;
                    break;
                }
            }
            if (!isStandard) {
                filtered.put(path);
            }
        }
        return filtered;
    }

    private static JSONObject probeRootManager(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        String[] managerPackages = concatAll(MAGISK_PACKAGES, KERNELSU_PACKAGES, APATCH_PACKAGES, SYSTEM_SU_PACKAGES);
        JSONArray packageHits = filterPackages(shared.installedPackages, managerPackages);

        indicators.put("packageHits", packageHits);
        appendArrayReasons(reasons, packageHits, com.android.device.i18n.AppLocale.tr("Root 管理器应用", "Root manager app"));

        result.put("id", ID_ROOT_MANAGER);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("Root 管理器应用 / 分支", "Root manager app / variant"));
        result.put("detected", packageHits.length() > 0);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONObject probeBusyBox() throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(BUSYBOX_PATHS);
        JSONArray shellPaths = scanPathsViaShell(BUSYBOX_PATHS);
        String whichBusybox = normalize(Cmd.exe("which busybox 2>/dev/null"));
        boolean busyboxHit = !whichBusybox.isEmpty() && !whichBusybox.contains("not found");

        indicators.put("matchedPaths", matchedPaths);
        indicators.put("shellPaths", shellPaths);
        indicators.put("whichBusybox", whichBusybox);

        appendPathReasons(reasons, "BusyBox", matchedPaths);
        appendShellPathReasons(reasons, shellPaths);
        if (busyboxHit) {
            reasons.put(com.android.device.i18n.AppLocale.tr("which busybox 探测到: ", "which busybox found: ") + whichBusybox);
        }

        result.put("id", ID_BUSYBOX);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("BusyBox 二进制文件", "BusyBox binary"));
        result.put("detected", matchedPaths.length() > 0 || shellPaths.length() > 0 || busyboxHit);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONObject probeRootHide(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray cloakingHits = filterPackages(shared.installedPackages, PersieAlignedRootProbe.ROOT_CLOAKING_PACKAGES);
        boolean hideSuspected = shared.hideSuspected;

        indicators.put("cloakingPackageHits", cloakingHits);
        indicators.put("hideSuspected", hideSuspected);

        appendArrayReasons(reasons, cloakingHits, com.android.device.i18n.AppLocale.tr("Root 隐藏应用", "Root-hiding app"));
        if (hideSuspected) {
            reasons.put(buildHideSuspectedReason(shared));
        }

        result.put("id", ID_ROOT_HIDE);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("Root 隐藏应用", "Root-hiding app"));
        result.put("detected", cloakingHits.length() > 0 || hideSuspected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONObject probeDangerousApp(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray packageHits = filterPackages(shared.installedPackages, DANGEROUS_APP_PACKAGES);

        indicators.put("packageHits", packageHits);
        appendArrayReasons(reasons, packageHits, com.android.device.i18n.AppLocale.tr("危险应用 / 修改工具", "Dangerous app / modding tool"));

        result.put("id", ID_DANGEROUS_APP);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("危险应用 / 修改工具", "Dangerous app / modding tool"));
        result.put("detected", packageHits.length() > 0);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static JSONObject probeSystemSu(SharedContext shared) throws JSONException {
        JSONObject result = new JSONObject();
        JSONArray reasons = new JSONArray();
        JSONObject indicators = new JSONObject();

        JSONArray matchedPaths = scanExistingPaths(SYSTEM_SU_PATHS);
        JSONArray packageHits = filterPackages(shared.installedPackages, SYSTEM_SU_PACKAGES);
        JSONArray mapsHits = filterKeywordHits(shared.mapsHits, SYSTEM_SU_MAPS_KEYWORDS);
        JSONArray shellHits = collectSystemSuShellHits(shared);
        boolean suWhichHit = !shared.suWhichPath.isEmpty()
                && !shared.suWhichPath.contains("not found");
        boolean suGranted = RootAccessHelper.isRootGranted();
        boolean idShowsRoot = shared.idOutput.contains("uid=0") || shared.idOutput.contains("(root)");
        boolean suLinkedFramework = shared.suReadlink.toLowerCase(Locale.US).contains("magisk")
                || shared.suReadlink.toLowerCase(Locale.US).contains("ksu")
                || shared.suReadlink.toLowerCase(Locale.US).contains("apd");

        indicators.put("matchedPaths", matchedPaths);
        indicators.put("packageHits", packageHits);
        indicators.put("mapsHits", mapsHits);
        indicators.put("shellHits", shellHits);
        indicators.put("suWhichPath", shared.suWhichPath);
        indicators.put("suReadlink", shared.suReadlink);
        indicators.put("idOutput", shared.idOutput);
        indicators.put("accessGranted", suGranted);
        indicators.put("accessDetail", RootAccessHelper.getAttemptDetail());

        appendPathReasons(reasons, com.android.device.i18n.AppLocale.tr("系统 su", "System su"), matchedPaths);
        appendArrayReasons(reasons, packageHits, com.android.device.i18n.AppLocale.tr("系统 Root 管理器", "System root manager"));
        appendArrayReasons(reasons, mapsHits, com.android.device.i18n.AppLocale.tr("系统 su maps 命中", "System su maps hit"));
        appendArrayReasons(reasons, shellHits, com.android.device.i18n.AppLocale.tr("系统 su Shell", "System su Shell"));
        if (suWhichHit) {
            reasons.put(com.android.device.i18n.AppLocale.tr("which su 可用: ", "which su available: ") + shared.suWhichPath);
        }
        if (!shared.suReadlink.isEmpty() && !suLinkedFramework) {
            reasons.put(com.android.device.i18n.AppLocale.tr("su 符号链接: ", "su symlink: ") + shared.suReadlink);
        }
        if (suGranted) {
            reasons.put(com.android.device.i18n.AppLocale.tr("su 授权探测成功: ", "su grant probe succeeded: ") + RootAccessHelper.getAttemptDetail());
        }
        if (idShowsRoot) {
            reasons.put(com.android.device.i18n.AppLocale.tr("id 显示 root: ", "id shows root: ") + shared.idOutput);
        }

        boolean detected = matchedPaths.length() > 0 || packageHits.length() > 0 || mapsHits.length() > 0
                || shellHits.length() > 0 || suWhichHit || suGranted || idShowsRoot;

        result.put("id", ID_SYSTEM_SU);
        result.put("displayName", com.android.device.i18n.AppLocale.tr("系统 su", "System su"));
        result.put("detected", detected);
        result.put("indicators", indicators);
        result.put("reasons", reasons);
        return result;
    }

    private static final class SharedContext {
        JSONArray mapsHits = new JSONArray();
        JSONArray mountHits = new JSONArray();
        JSONArray mountInfoHits = new JSONArray();
        JSONArray bootUnlockSignals = new JSONArray();
        JSONArray buildMismatches = new JSONArray();
        JSONArray envHits = new JSONArray();
        JSONArray javaNativeMismatches = new JSONArray();
        JSONArray installedPackages = new JSONArray();
        JSONObject nativeProbe = new JSONObject();
        JSONObject persieAligned = new JSONObject();
        String allProps = "";
        String selinuxMode = "";
        String suReadlink = "";
        String suWhichPath = "";
        String idOutput = "";
        boolean hideSuspected;

        static SharedContext collect(Context context) throws JSONException {
            SharedContext ctx = new SharedContext();
            ctx.allProps = normalize(Cmd.exe("getprop"));
            ctx.mapsHits = scanProcFile("/proc/self/maps", concatAll(
                    MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS, SYSTEM_SU_MAPS_KEYWORDS));
            ctx.mountHits = scanProcMounts();
            ctx.mountInfoHits = scanProcFile("/proc/self/mountinfo", concatAll(
                    MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS));
            ctx.bootUnlockSignals = collectBootUnlockSignals();
            ctx.buildMismatches = collectBuildMismatches();
            ctx.envHits = scanEnvironmentVariables();
            ctx.nativeProbe = parseNativeProbe();
            ctx.persieAligned = PersieAlignedRootProbe.probe(context);
            ctx.selinuxMode = normalize(Cmd.exe("getenforce"));
            ctx.suReadlink = normalize(Cmd.exe("readlink /system/bin/su 2>/dev/null"));
            ctx.suWhichPath = normalize(Cmd.exe("which su 2>/dev/null"));
            ctx.idOutput = normalize(Cmd.exe("id"));
            ctx.installedPackages = scanAllInstalledPackages(context);
            JSONArray allPaths = mergeJsonArrays(
                    scanExistingPaths(MAGISK_PATHS),
                    scanExistingPaths(KERNELSU_PATHS),
                    scanExistingPaths(APATCH_PATHS),
                    scanExistingPaths(SYSTEM_SU_PATHS)
            );
            JSONArray frameworkPaths = mergeJsonArrays(
                    scanExistingPaths(MAGISK_PATHS),
                    scanExistingPaths(KERNELSU_PATHS),
                    scanExistingPaths(APATCH_PATHS)
            );
            ctx.javaNativeMismatches = detectJavaNativePathMismatch(ctx.nativeProbe, frameworkPaths);
            boolean javaMapsRootHit = ctx.mapsHits.length() > 0 || ctx.mountInfoHits.length() > 0
                    || ctx.mountHits.length() > 0;
            boolean nativeRootHit = hasNativeRootEvidence(ctx.nativeProbe);
            boolean persieHideSignal = PersieAlignedRootProbe.hasHideRelevantSignal(ctx.persieAligned);
            boolean pathVisible = allPaths.length() > 0;
            ctx.hideSuspected = (javaMapsRootHit || nativeRootHit || ctx.javaNativeMismatches.length() > 0
                    || persieHideSignal)
                    && !pathVisible && !RootAccessHelper.isRootGranted();
            return ctx;
        }
    }

    private static String buildHideSuspectedReason(SharedContext shared) {
        if (shared.javaNativeMismatches.length() > 0) {
            return com.android.device.i18n.AppLocale.tr("疑似 Root 隐藏：Native 路径可访问但 Java 层不可见", "Suspected root hiding: Native paths accessible but invisible to the Java layer");
        }
        if (shared.nativeProbe.optBoolean("anyHit", false)) {
            return com.android.device.i18n.AppLocale.tr("疑似 Root 隐藏：Native maps/mount 有 Root 框架信号但路径被隐藏", "Suspected root hiding: Native maps/mount show root-framework signals but paths are hidden");
        }
        return com.android.device.i18n.AppLocale.tr("疑似 Root 隐藏：maps/mount 有 Root 框架信号但路径不可见", "Suspected root hiding: maps/mount show root-framework signals but paths invisible");
    }

    private static void applyNativeProbeToFrameworks(
            JSONObject magisk,
            JSONObject kernelsu,
            JSONObject apatch,
            JSONObject nativeProbe
    ) throws JSONException {
        if (nativeIndicatesFramework(nativeProbe, "magisk", "zygisk", "magiskpolicy", "resetprop")) {
            markFrameworkDetectedByNative(magisk, com.android.device.i18n.AppLocale.tr("Native 探测命中 Magisk/Zygisk", "Native probe hit Magisk/Zygisk"));
        }
        if (nativeIndicatesFramework(nativeProbe, "kernelsu", "ksud", "kernel_su")
                || containsExactHit(nativeProbe.optJSONArray("mapsHits"))) {
            markFrameworkDetectedByNative(kernelsu, com.android.device.i18n.AppLocale.tr("Native 探测命中 KernelSU", "Native probe hit KernelSU"));
        }
        if (nativeIndicatesFramework(nativeProbe, "apatch", "bmax")) {
            markFrameworkDetectedByNative(apatch, com.android.device.i18n.AppLocale.tr("Native 探测命中 APatch", "Native probe hit APatch"));
        }
        JSONArray accessible = nativeProbe.optJSONArray("accessiblePaths");
        if (accessible != null) {
            for (int i = 0; i < accessible.length(); i++) {
                String path = accessible.optString(i).toLowerCase(Locale.US);
                if (path.contains("magisk")) {
                    markFrameworkDetectedByNative(magisk, com.android.device.i18n.AppLocale.tr("Native 路径可访问: ", "Native path accessible: ") + accessible.optString(i));
                } else if (path.contains("ksu") || path.contains("kernelsu")) {
                    markFrameworkDetectedByNative(kernelsu, com.android.device.i18n.AppLocale.tr("Native 路径可访问: ", "Native path accessible: ") + accessible.optString(i));
                } else if (path.contains("/ap") || path.contains("apd")) {
                    markFrameworkDetectedByNative(apatch, com.android.device.i18n.AppLocale.tr("Native 路径可访问: ", "Native path accessible: ") + accessible.optString(i));
                }
            }
        }
    }

    private static void markFrameworkDetectedByNative(JSONObject framework, String reason)
            throws JSONException {
        if (framework == null) {
            return;
        }
        framework.put("detected", true);
        JSONArray reasons = framework.optJSONArray("reasons");
        if (reasons == null) {
            reasons = new JSONArray();
            framework.put("reasons", reasons);
        }
        reasons.put(reason);
    }

    private static boolean nativeIndicatesFramework(JSONObject nativeProbe, String... keywords) {
        return containsAnyKeyword(nativeProbe.optJSONArray("mapsHits"), keywords)
                || containsAnyKeyword(nativeProbe.optJSONArray("mountHits"), keywords);
    }

    private static boolean containsAnyKeyword(JSONArray hits, String... keywords) {
        if (hits == null) {
            return false;
        }
        for (int i = 0; i < hits.length(); i++) {
            String value = hits.optString(i).toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (value.contains(keyword.toLowerCase(Locale.US))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsExactHit(JSONArray hits) {
        if (hits == null) {
            return false;
        }
        for (int i = 0; i < hits.length(); i++) {
            if ("ksu".equalsIgnoreCase(hits.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNativeRootEvidence(JSONObject nativeProbe) {
        if (nativeProbe.optBoolean("anyHit", false)) {
            return true;
        }
        JSONArray accessible = nativeProbe.optJSONArray("accessiblePaths");
        return accessible != null && accessible.length() > 0;
    }

    /**
     * 后处理：当 shellHits 有内容但 detected 仍为 false 时（Cmd.exe 受限环境关键词未命中），
     * 强制置为 true 并将 shellHits 条目追加到 reasons。
     */
    private static void enforceShellHitDetection(JSONObject framework) throws JSONException {
        if (framework == null) {
            return;
        }
        JSONObject indicators = framework.optJSONObject("indicators");
        if (indicators == null) {
            return;
        }
        JSONArray shellHits = indicators.optJSONArray("shellHits");
        if (shellHits == null || shellHits.length() == 0) {
            return;
        }
        if (!framework.optBoolean("detected", false)) {
            framework.put("detected", true);
        }
        // 确保 reasons 包含 shell 命中信息
        JSONArray reasons = framework.optJSONArray("reasons");
        if (reasons == null) {
            reasons = new JSONArray();
            framework.put("reasons", reasons);
        }
        for (int i = 0; i < shellHits.length(); i++) {
            String hit = shellHits.optString(i);
            if (!hit.isEmpty()) {
                boolean alreadyInReasons = false;
                for (int j = 0; j < reasons.length(); j++) {
                    if (reasons.optString(j).contains(hit)) {
                        alreadyInReasons = true;
                        break;
                    }
                }
                if (!alreadyInReasons) {
                    String displayName = framework.optString("displayName", "");
                    reasons.put(displayName + " Shell: " + hit);
                }
            }
        }
    }

    private static JSONArray collectMagiskShellHits() throws JSONException {
        JSONArray hits = new JSONArray();
        addShellHit(hits, "getprop_magisk", Cmd.exe("getprop | grep -i magisk"), "magisk", "zygisk");
        addShellHit(hits, "test_sbin_magisk", Cmd.exe("test -f /sbin/magisk && echo exists"), "exists");
        addShellHit(hits, "which_magisk", Cmd.exe("which magisk 2>/dev/null"), "magisk");
        addShellHit(hits, "magisk_version", Cmd.exe("magisk -v 2>/dev/null"), "magisk");
        addShellHit(hits, "mounts_magisk", Cmd.exe("cat /proc/mounts | grep -i magisk"), "magisk");
        addShellHit(hits, "resetprop_check", Cmd.exe("resetprop 2>&1 | head -1"), "resetprop", "magisk");
        return hits;
    }

    private static JSONArray collectFrameworkShellHits(String frameworkId) throws JSONException {
        JSONArray hits = new JSONArray();
        if (ID_KERNELSU.equals(frameworkId)) {
            addShellHit(hits, "ksud_version", Cmd.exe("ksud -V 2>/dev/null"), "ksu", "kernel");
            addShellHit(hits, "which_ksud", Cmd.exe("which ksud 2>/dev/null"), "ksud");
            addShellHit(hits, "ls_ksu_dir", Cmd.exe("ls -la /data/adb/ksu 2>&1"), "ksu", "kernelsu");
            addShellHit(hits, "getprop_ksu", Cmd.exe("getprop | grep -i ksu"), "ksu", "kernelsu");
            addShellHit(hits, "cat_proc_ksu", Cmd.exe("cat /proc/ksu 2>/dev/null | head -5"), "ksu", "kernel");
            addShellHit(hits, "which_ksu", Cmd.exe("which ksu 2>/dev/null"), "ksu");
            addShellHit(hits, "mounts_ksu", Cmd.exe("cat /proc/mounts | grep -i ksu"), "ksu", "kernelsu");
            addShellHit(hits, "env_ksu", Cmd.exe("printenv | grep -i ksu"), "ksu", "kernelsu");
        } else if (ID_APATCH.equals(frameworkId)) {
            addShellHit(hits, "apd_version", Cmd.exe("apd -V 2>/dev/null"), "apd", "apatch");
            addShellHit(hits, "which_apd", Cmd.exe("which apd 2>/dev/null"), "apd");
            addShellHit(hits, "ls_ap_dir", Cmd.exe("ls -la /data/adb/ap 2>&1"), "ap", "apd");
            addShellHit(hits, "ls_apd_dir", Cmd.exe("ls -la /data/adb/apd 2>&1"), "apd", "apatch");
            addShellHit(hits, "getprop_apatch", Cmd.exe("getprop | grep -i apatch"), "apatch", "apd");
            addShellHit(hits, "which_ap", Cmd.exe("which ap 2>/dev/null"), "ap", "apd");
            addShellHit(hits, "mounts_apatch", Cmd.exe("cat /proc/mounts | grep -i apatch"), "apatch", "ap");
            addShellHit(hits, "env_apatch", Cmd.exe("printenv | grep -i apatch"), "apatch", "ap");
            addShellHit(hits, "ps_apatch", Cmd.exe("ps -A | grep -i apd 2>/dev/null"), "apd", "apatch");
        } else if (ID_KERNELSU_BACKUP.equals(frameworkId)) {
            addShellHit(hits, "ls_data_adb_kernelsu_backup", Cmd.exe("ls -la /data/adb/kernelsu 2>&1"), "kernelsu", "ksu");
            addShellHit(hits, "which_kernelsu", Cmd.exe("which kernelsu 2>/dev/null"), "kernelsu");
            addShellHit(hits, "cat_proc_kernelsu", Cmd.exe("cat /proc/kernelsu 2>/dev/null | head -5"), "kernelsu", "ksu");
            addShellHit(hits, "mounts_kernelsu", Cmd.exe("cat /proc/mounts | grep -i kernelsu"), "kernelsu", "ksu");
            addShellHit(hits, "env_kernelsu", Cmd.exe("printenv | grep -i kernelsu"), "kernelsu", "ksu");
        }
        return hits;
    }

    private static JSONArray collectSystemSuShellHits(SharedContext shared) throws JSONException {
        JSONArray hits = new JSONArray();
        addShellHit(hits, "which_su", shared.suWhichPath, "su");
        addShellHit(hits, "ls_system_su", Cmd.exe("ls -la /system/bin/su /system/xbin/su 2>&1"), "su");
        addShellHit(hits, "su_version", Cmd.exe("su -v 2>&1 | head -1"), "su", "superuser");
        addShellHit(hits, "type_su", Cmd.exe("type su 2>&1"), "su");
        addShellHit(hits, "stat_su", Cmd.exe("stat /system/bin/su 2>&1"), "su");
        addShellHit(hits, "su_c_id", Cmd.exe("su -c id 2>&1"), "uid=0", "(root)");
        addShellHit(hits, "su_0_id", Cmd.exe("su 0 id 2>&1"), "uid=0", "(root)");
        addShellHit(hits, "id_root", Cmd.exe("id 2>&1"), "uid=0", "(root)");
        addShellHit(hits, "whoami_root", Cmd.exe("whoami 2>&1"), "root");
        addShellHit(hits, "groups_root", Cmd.exe("groups 2>&1"), "root");
        addShellHit(hits, "find_su", Cmd.exe("find /system /data -name 'su' -type f 2>/dev/null | head -5"), "su");
        addShellHit(hits, "getcap_su", Cmd.exe("getcap /system/bin/su 2>/dev/null"), "cap");
        addShellHit(hits, "ls_sbin", Cmd.exe("ls -la /sbin 2>&1"), "su", "magisk", "ksu");
        addShellHit(hits, "ls_vendor_bin", Cmd.exe("ls -la /vendor/bin 2>&1"), "su");
        addShellHit(hits, "ls_product_bin", Cmd.exe("ls -la /product/bin 2>&1"), "su");
        return hits;
    }

    /** 标准 Android 系统中自然包含 "su"/"root" 等关键词的路径/输出模式，用于过滤误判。 */
    private static final String[] STANDARD_ANDROID_FALSE_POSITIVE_PATTERNS = {
            "/system/bin/", "/system/xbin/", "/vendor/bin/", "/product/bin/",
            "/sbin/", "/data/adb/", "/system/app/", "/system/priv-app/",
            "/system/lib/", "/system/lib64/", "/apex/",
            "/proc/", "/sys/", "/dev/",
            "/storage/emulated/", "/mnt/", "/sdcard/",
            "uid=2000", "uid=1000", "uid=100", "(shell)", "(system)",
            "drwx", "lrwx", "-rwx", "-rw-", "total "
    };

    private static void addShellHit(JSONArray hits, String tag, String output, String... acceptKeywords) {
        String normalized = normalize(output);
        if (normalized.isEmpty()) {
            return;
        }
        String lower = normalized.toLowerCase(Locale.US);
        if (tag.equals("which_su") && !normalized.contains("not found")) {
            hits.put(tag + ": " + normalized);
            return;
        }
        for (String keyword : acceptKeywords) {
            if (lower.contains(keyword.toLowerCase(Locale.US))) {
                if (lower.contains("not found") || lower.contains("no such file")
                        || lower.contains("cannot access")) {
                    continue;
                }
                if (isStandardAndroidFalsePositive(lower)) {
                    continue;
                }
                hits.put(tag + ": " + truncate(normalized, 160));
                return;
            }
        }
    }

    private static boolean isStandardAndroidFalsePositive(String lowerOutput) {
        for (String pattern : STANDARD_ANDROID_FALSE_POSITIVE_PATTERNS) {
            if (lowerOutput.contains(pattern.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    private static JSONArray scanExistingPaths(String[] paths) {
        JSONArray hits = new JSONArray();
        for (String path : paths) {
            if (new File(path).exists()) {
                hits.put(path);
            }
        }
        return hits;
    }

    private static JSONArray scanPathsViaShell(String[] paths) {
        JSONArray hits = new JSONArray();
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            sb.append("test -e ").append(path).append(" && echo ").append(path).append(";");
        }
        String output = normalize(Cmd.exe(sb.toString()));
        if (!output.isEmpty()) {
            for (String line : output.split("\n")) {
                String p = line.trim();
                if (!p.contains(" ") && p.startsWith("/")) {
                    hits.put(p);
                }
            }
        }
        return hits;
    }

    private static void appendShellPathReasons(JSONArray reasons, JSONArray paths) {
        for (int i = 0; i < paths.length(); i++) {
            reasons.put("Binary found via shell: " + paths.optString(i));
        }
    }

    private static JSONArray scanProcMounts() throws JSONException {
        return scanProcFile("/proc/mounts", concatAll(
                MAGISK_MAPS_KEYWORDS, KERNELSU_MAPS_KEYWORDS, APATCH_MAPS_KEYWORDS));
    }

    private static JSONArray scanProcFile(String path, String[] keywords) {
        JSONArray hits = new JSONArray();
        String content = readFile(path);
        if (content == null) {
            return hits;
        }
        for (String line : content.split("\n")) {
            String lowerLine = line.toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (lineMatchesKeyword(lowerLine, keyword)) {
                    hits.put(keyword);
                }
            }
        }
        return hits;
    }

    private static boolean lineMatchesKeyword(String lowerLine, String keyword) {
        String lowerKeyword = keyword.toLowerCase(Locale.US);
        if (lowerKeyword.startsWith("/")) {
            return lowerLine.contains(lowerKeyword);
        }
        if (lowerKeyword.length() < 5) {
            return false;
        }
        return lowerLine.contains(lowerKeyword);
    }

    private static JSONArray collectPropertyHits(String allProps, String[] keys) {
        JSONArray hits = new JSONArray();
        if (allProps != null) {
            String lower = allProps.toLowerCase(Locale.US);
            for (String key : keys) {
                String keyLower = key.toLowerCase(Locale.US);
                if (lower.contains("[" + keyLower + "]") || lower.contains(keyLower)) {
                    for (String line : allProps.split("\n")) {
                        String trimmed = line.trim();
                        if (trimmed.toLowerCase(Locale.US).contains(keyLower)) {
                            hits.put(trimmed);
                        }
                    }
                }
            }
        }
        for (String key : keys) {
            String value = normalize(Cmd.getPropertyViaShell(key));
            if (!value.isEmpty()) {
                hits.put(key + "=" + value);
            }
        }
        return hits;
    }

    private static JSONArray scanEnvironmentVariables() {
        JSONArray hits = new JSONArray();
        String env = normalize(Cmd.exe("printenv"));
        if (env.isEmpty()) {
            return hits;
        }
        for (String line : env.split("\n")) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.contains("magisk") || lower.contains("zygisk")
                    || lower.contains("kernelsu") || lower.contains("ksud")
                    || lower.contains("apatch") || lower.contains("/data/adb/ap")) {
                hits.put(line.trim());
            }
        }
        return hits;
    }

    private static JSONArray scanAllInstalledPackages(Context context) throws JSONException {
        JSONArray hits = new JSONArray();
        if (context == null) {
            return hits;
        }
        try {
            PackageManager pm = context.getPackageManager();
            for (PackageInfo info : pm.getInstalledPackages(0)) {
                if (info != null && info.packageName != null) {
                    hits.put(info.packageName);
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Package scan failed", e);
        }
        return hits;
    }

    private static JSONArray filterPackages(JSONArray installed, String[] keywords) throws JSONException {
        JSONArray hits = new JSONArray();
        for (int i = 0; i < installed.length(); i++) {
            String pkg = installed.optString(i).toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (pkg.contains(keyword.toLowerCase(Locale.US))) {
                    hits.put(installed.optString(i));
                    break;
                }
            }
        }
        return hits;
    }

    private static JSONArray filterKeywordHits(JSONArray source, String[] keywords) throws JSONException {
        JSONArray hits = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            String value = source.optString(i).toLowerCase(Locale.US);
            for (String keyword : keywords) {
                if (value.contains(keyword.toLowerCase(Locale.US))) {
                    hits.put(source.optString(i));
                    break;
                }
            }
        }
        return hits;
    }

    private static JSONObject parseNativeProbe() throws JSONException {
        JSONObject probe = new JSONObject();
        try {
            String raw = JniPropertyHelper.getMagiskNativeProbe();
            if (raw != null && raw.trim().startsWith("{")) {
                probe = new JSONObject(raw);
            } else {
                probe.put("raw", raw != null ? raw : "");
            }
        } catch (JSONException e) {
            probe.put("parseError", e.getMessage());
        }
        boolean anyHit = probe.optJSONArray("accessiblePaths") != null
                && probe.optJSONArray("accessiblePaths").length() > 0;
        anyHit = anyHit || (probe.optJSONArray("mapsHits") != null
                && probe.optJSONArray("mapsHits").length() > 0);
        anyHit = anyHit || (probe.optJSONArray("mountHits") != null
                && probe.optJSONArray("mountHits").length() > 0);
        probe.put("anyHit", anyHit);
        return probe;
    }

    private static JSONArray detectJavaNativePathMismatch(JSONObject nativeProbe, JSONArray javaPaths)
            throws JSONException {
        JSONArray mismatches = new JSONArray();
        JSONArray nativePaths = nativeProbe.optJSONArray("accessiblePaths");
        if (nativePaths == null) {
            return mismatches;
        }
        for (int i = 0; i < nativePaths.length(); i++) {
            String path = nativePaths.optString(i);
            if (path.isEmpty()) {
                continue;
            }
            boolean javaVisible = new File(path).exists();
            boolean listedByJavaScan = containsString(javaPaths, path);
            if (!javaVisible || !listedByJavaScan) {
                mismatches.put(path + com.android.device.i18n.AppLocale.tr(" (native可访问, Java=", " (native accessible, Java=") + javaVisible + ")");
            }
        }
        return mismatches;
    }

    private static JSONArray collectBootUnlockSignals() throws JSONException {
        JSONArray hits = new JSONArray();
        addBootSignal(hits, "ro.boot.verifiedbootstate", new String[]{"orange", "yellow"});
        addBootSignal(hits, "ro.boot.flash.locked", new String[]{"0"});
        addBootSignal(hits, "ro.boot.vbmeta.device_state", new String[]{"unlocked"});
        addBootSignal(hits, "ro.boot.warranty_bit", new String[]{"1"});
        addBootSignal(hits, "ro.boot.veritymode", new String[]{"enforcing"});
        for (String key : BOOT_UNLOCK_PROPS) {
            String shell = normalize(Cmd.getPropertyViaShell(key));
            String jni = normalize(JniPropertyHelper.getSystemPropertyByFind(key));
            if (!shell.isEmpty() && !jni.isEmpty() && !JniPropertyHelper.isErrorResult(jni)
                    && !shell.equals(jni)) {
                hits.put(key + com.android.device.i18n.AppLocale.tr(" 通道不一致: getprop=", " channel mismatch: getprop=") + shell + " jni=" + jni);
            }
        }
        return hits;
    }

    private static void addBootSignal(JSONArray hits, String key, String[] suspiciousValues)
            throws JSONException {
        String value = normalize(Cmd.getPropertyViaShell(key));
        if (value.isEmpty()) {
            return;
        }
        String lower = value.toLowerCase(Locale.US);
        for (String suspicious : suspiciousValues) {
            if (key.equals("ro.boot.veritymode")) {
                if (!"enforcing".equalsIgnoreCase(value) && !"eio".equalsIgnoreCase(value)) {
                    hits.put(key + "=" + value);
                }
                return;
            }
            if (lower.equals(suspicious.toLowerCase(Locale.US))) {
                hits.put(key + "=" + value);
                return;
            }
        }
    }

    private static JSONArray collectBuildMismatches() throws JSONException {
        JSONArray hits = new JSONArray();
        compareBuildField(hits, "TAGS", "ro.build.tags");
        compareBuildField(hits, "FINGERPRINT", "ro.build.fingerprint");
        compareBuildField(hits, "TYPE", "ro.build.type");
        compareBuildField(hits, "MODEL", "ro.product.model");
        if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
            hits.put("Build.TAGS=test-keys");
        }
        return hits;
    }

    private static void compareBuildField(JSONArray hits, String buildField, String propKey)
            throws JSONException {
        String buildValue = readBuildField(buildField);
        String propValue = normalize(Cmd.getPropertyViaShell(propKey));
        if (!buildValue.isEmpty() && !propValue.isEmpty() && !buildValue.equals(propValue)) {
            hits.put(buildField + " vs " + propKey + ": Build=" + buildValue + " prop=" + propValue);
        }
    }

    private static String readBuildField(String field) {
        switch (field) {
            case "TAGS":
                return normalize(Build.TAGS);
            case "FINGERPRINT":
                return normalize(Build.FINGERPRINT);
            case "TYPE":
                return normalize(Build.TYPE);
            case "MODEL":
                return normalize(Build.MODEL);
            default:
                return "";
        }
    }

    private static void appendPathReasons(JSONArray reasons, String label, JSONArray paths)
            throws JSONException {
        for (int i = 0; i < paths.length(); i++) {
            reasons.put(label + com.android.device.i18n.AppLocale.tr(" 路径存在: ", " path exists: ") + paths.optString(i));
        }
    }

    private static void appendArrayReasons(JSONArray reasons, JSONArray items, String prefix)
            throws JSONException {
        for (int i = 0; i < items.length(); i++) {
            reasons.put(prefix + ": " + items.optString(i));
        }
    }

    private static void mergeReasons(JSONArray target, JSONArray source) throws JSONException {
        if (source == null) {
            return;
        }
        for (int i = 0; i < source.length(); i++) {
            target.put(source.optString(i));
        }
    }

    private static boolean containsString(JSONArray array, String value) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (value.equals(array.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private static JSONArray mergeJsonArrays(JSONArray... arrays) throws JSONException {
        JSONArray merged = new JSONArray();
        for (JSONArray array : arrays) {
            for (int i = 0; i < array.length(); i++) {
                merged.put(array.optString(i));
            }
        }
        return merged;
    }

    private static String[] concatAll(String[]... groups) {
        int total = 0;
        for (String[] group : groups) {
            total += group.length;
        }
        String[] result = new String[total];
        int index = 0;
        for (String[] group : groups) {
            for (String item : group) {
                result[index++] = item;
            }
        }
        return result;
    }

    private static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
