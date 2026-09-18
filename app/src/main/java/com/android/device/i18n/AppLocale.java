package com.android.device.i18n;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * 应用内界面语言切换。
 *
 * SharedPreferences 长期缓存用户选择（数据源），从未设置过时默认英文（{@link #DEFAULT}）。
 * Android 13+（API 33）走系统 {@link LocaleManager} 的 per-app locales，低版本回落到
 * AppCompat（配合 manifest 的 autoStoreLocales 服务持久化）。启动时 {@link #applyFromCache}
 * 把当前 locale 对齐到缓存，因此设备系统语言即便是中文，首次启动也默认英文。
 */
public final class AppLocale {

    private static final String PREFS = "device_ui_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static final String EN = "en";
    public static final String ZH = "zh-CN";
    /** 默认语言：英文。 */
    public static final String DEFAULT = EN;

    /** 当前界面语言（由 applyFromCache / setLanguage 维护，供无 Context 的内容翻译分派使用）。 */
    private static volatile String currentLanguage = DEFAULT;

    private AppLocale() {
    }

    /** 当前界面语言是否为中文。 */
    public static boolean isChinese() {
        return currentLanguage != null && currentLanguage.toLowerCase().startsWith("zh");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 读取已缓存语言；从未设置过则返回默认英文。 */
    public static String getLanguage(Context context) {
        return prefs(context).getString(KEY_LANGUAGE, DEFAULT);
    }

    /** 保存选择（长期缓存）并立即应用；系统会以新 locale 重建界面。 */
    public static void setLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_LANGUAGE, language).apply();
        currentLanguage = language;
        apply(context, language);
    }

    /** 启动时调用：把当前 locale 对齐到缓存（首启默认英文）。 */
    public static void applyFromCache(Context context) {
        String desired = getLanguage(context);
        currentLanguage = desired;
        if (!desired.equalsIgnoreCase(currentTags(context))) {
            apply(context, desired);
        }
    }

    private static String currentTags(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager lm = context.getSystemService(LocaleManager.class);
            return lm == null ? "" : lm.getApplicationLocales().toLanguageTags();
        }
        return AppCompatDelegate.getApplicationLocales().toLanguageTags();
    }

    private static void apply(Context context, String language) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManager lm = context.getSystemService(LocaleManager.class);
            if (lm != null) {
                lm.setApplicationLocales(LocaleList.forLanguageTags(language));
            }
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
        }
    }
}
