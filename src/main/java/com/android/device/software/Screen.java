package com.android.device.software;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.android.UApplication;
import com.android.utils.ULog;

import org.json.JSONObject;

public final class Screen {

    /**
     * 获得当前屏幕亮度的模式
     * * 0:手动调节屏幕亮度
     *
     * @param context 上下文对象
     * @return 屏幕亮度模式，例如0
     */
    private static int getScreenMode(Context context) {
        int screenMode = 0;
        try {
            /**
             *  SCREEN_BRIGHTNESS_MODE_AUTOMATIC=1 为自动调节屏幕亮度
             *  SCREEN_BRIGHTNESS_MODE_MANUAL=0  为手动调节屏幕亮度
             */
            screenMode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return screenMode;
    }

    /**
     * 获取当前屏幕亮度值，并转换为百分比形式 0--255
     *
     * @param context 上下文对象
     * @return 屏幕亮度值，例如125
     */
    public static int getScreenBrightness(Context context) {
        int screenBrightness = -1;
        try {
            screenBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return screenBrightness;
    }

    /**
     * 获取屏幕宽高和密度
     *
     * @param context 上下文对象
     * @return 宽度*高度*密度，例如"1080*2340*440"
     */
    public static String getScreenWHD(Context context) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.widthPixels + "*" + outMetrics.heightPixels + "*" + outMetrics.densityDpi;
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

    /**
     * 获取真实屏幕宽高
     *
     * @param context 上下文对象
     * @return 宽度*高度，例如"1080*2340"
     */
    public static String getRealScreenWH(Context context) {
        try {
            Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            return point.x + "*" + point.y;
        } catch (Throwable e) {
            // 删除ULog，静默处理异常
        }
        return "";
    }

    /**
     * 获取屏幕刷新率
     *
     * @param activity
     * @return 刷新率值，例如60.0f表示屏幕每秒刷新60次。如果获取失败则返回-1.0f。
     */
    public static float getRefreshRate(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        float refreshRate = display.getRefreshRate();
        return refreshRate;
    }

    /**
     * 获取屏幕详细信息
     *
     * @param context 上下文对象
     * @return JSONObject 包含屏幕详细信息的JSON对象
     * @throws Exception 可能抛出JSONException异常  如果JSON对象创建失败，则抛出异常。
     */
    public static JSONObject getScreenDetailInfo(Context context) {
        try {
            JSONObject jsonObject = new JSONObject();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);

            jsonObject.put("d_dpi", String.valueOf(displayMetrics.densityDpi));
            jsonObject.put("xdp", String.valueOf(displayMetrics.xdpi));
            jsonObject.put("ydp", String.valueOf(displayMetrics.ydpi));
            jsonObject.put("x_px", String.valueOf(displayMetrics.widthPixels));
            jsonObject.put("y_px", String.valueOf(displayMetrics.heightPixels));
            jsonObject.put("size", String.valueOf(context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK));
            jsonObject.put("density", String.valueOf(displayMetrics.density));
            jsonObject.put("scaledDensity", String.valueOf(displayMetrics.scaledDensity));
            jsonObject.put("screenLayout", String.valueOf(context.getResources().getConfiguration().screenLayout));
            jsonObject.put("screen_brightness", getScreenBrightness(context));
            try {
                int statusBarHeight = getSystemBarHeight("status_bar_height", context);
                int navigationBarHeight = getSystemBarHeight("navigation_bar_height", context);
                jsonObject.put("statusBarHeight", statusBarHeight);
                jsonObject.put("navigationBarHeight", navigationBarHeight);
            } catch (Exception e) {
                //do
            }

            return jsonObject;
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取系统状态栏高度和导航栏高度
     *
     * @param resource 资源名称
     * @param context  上下文
     * @return
     */
    private static int getSystemBarHeight(String resource, Context context) {
        int resourceId = context.getResources().getIdentifier(resource, "dimen", "android");
        return (resourceId > 0) ? context.getResources().getDimensionPixelSize(resourceId) : 0;
    }

}


