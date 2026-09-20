package com.android.device.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.core.view.WindowInsetsControllerCompat
import com.android.device.R

/**
 * 多套预设主题配色的存取与应用。
 *
 * 用户选择写入 SharedPreferences 长期缓存；每个 Activity 在 super.onCreate 之前调用
 * [apply] 把缓存的主题 setTheme 上去。切换后由调用方 recreate() 使其立即生效。
 */
object ThemeManager {

    private const val PREFS = "device_ui_prefs"
    private const val KEY_THEME = "app_theme"

    /** 预设主题：id 用于持久化，themeRes 是样式，nameRes 是展示名。 */
    enum class Preset(val id: String, val themeRes: Int, @StringRes val nameRes: Int) {
        CYBER_BLUE("cyber_blue", R.style.Theme_DeviceInfo_CyberBlue, R.string.theme_cyber_blue),
        NEON_GREEN("neon_green", R.style.Theme_DeviceInfo_NeonGreen, R.string.theme_neon_green),
        SAKURA_PINK("sakura_pink", R.style.Theme_DeviceInfo_SakuraPink, R.string.theme_sakura_pink),
        SUNSET_ORANGE("sunset_orange", R.style.Theme_DeviceInfo_SunsetOrange, R.string.theme_sunset_orange),
        GRAPE("grape", R.style.Theme_DeviceInfo_Grape, R.string.theme_grape),
        GRAPHITE("graphite", R.style.Theme_DeviceInfo_Graphite, R.string.theme_graphite);

        companion object {
            fun fromId(id: String?): Preset = entries.firstOrNull { it.id == id } ?: CYBER_BLUE
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(context: Context): Preset =
        Preset.fromId(prefs(context).getString(KEY_THEME, Preset.CYBER_BLUE.id))

    fun setPreset(context: Context, preset: Preset) {
        prefs(context).edit().putString(KEY_THEME, preset.id).apply()
    }

    /** 在 setContentView 之前调用（通常放在 super.onCreate 之前）。 */
    fun apply(activity: Activity) {
        activity.setTheme(current(activity).themeRes)
    }

    /**
     * 适配状态栏 / 导航栏图标明暗：浅色主题用深色图标，深色模式用浅色图标，
     * 避免白底白字看不清。在 setContentView 之后调用。
     */
    fun applySystemBars(activity: Activity) {
        val night = (activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !night
        controller.isAppearanceLightNavigationBars = !night
    }
}
