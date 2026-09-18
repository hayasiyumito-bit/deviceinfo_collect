package com.android.device.i18n;

import android.content.Context;

import com.android.device.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 列表分区标题的显示层翻译。
 *
 * 解析器（DeviceInfoParser）内部把中文分类名当作分组键使用，属于数据层，不改动。
 * 这里仅在渲染时把已知分类键映射为按界面语言的显示文案；未知键原样返回。
 */
public final class CategoryLabels {

    private static final Map<String, Integer> KEY_TO_RES = new HashMap<>();

    static {
        KEY_TO_RES.put("安全检测", R.string.cat_security);
        KEY_TO_RES.put("系统信息", R.string.cat_system);
        KEY_TO_RES.put("硬件信息", R.string.cat_hardware);
        KEY_TO_RES.put("网络信息", R.string.cat_network);
        KEY_TO_RES.put("软件信息", R.string.cat_software);
        KEY_TO_RES.put("存储信息", R.string.cat_storage);
        KEY_TO_RES.put("传感器信息", R.string.cat_sensor);
        KEY_TO_RES.put("其他信息", R.string.cat_other);
    }

    private CategoryLabels() {
    }

    public static String localize(Context context, String categoryKey) {
        Integer res = KEY_TO_RES.get(categoryKey);
        return res != null ? context.getString(res) : categoryKey;
    }
}
