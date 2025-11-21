package com.android.device.software;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InputLanguage {
    /**
     * 获取系统支持的所有输入语言列表
     *
     * @param context 上下文对象
     * @return 返回包含所有输入语言的JSONArray对象，如果发生异常则返回null
     */
    public static JSONArray getInputLanguageList(Context context) {
        try {
            List<String> arrayList0 = new ArrayList();
            InputMethodManager inputMethodManager0 = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> list = inputMethodManager0.getEnabledInputMethodList();
            for (int i = 0; i < list.size(); ++i) {
                InputMethodInfo inputMethodInfo = list.get(i);
                List<InputMethodSubtype> inputMethodSubtypeList = inputMethodManager0.getEnabledInputMethodSubtypeList(inputMethodInfo, true);
                for (int j = 0; j < inputMethodSubtypeList.size(); ++j) {
                    InputMethodSubtype inputMethodSubtype = inputMethodSubtypeList.get(j);
                    String s = Build.VERSION.SDK_INT >= 24 && !TextUtils.isEmpty(inputMethodSubtype.getLanguageTag()) ? Locale.forLanguageTag(inputMethodSubtype.getLanguageTag()).getLanguage() : (new Locale(inputMethodSubtype.getLocale())).getLanguage();
                    arrayList0.add(s);
                }
            }
            return new JSONArray(arrayList0);
        } catch (Exception var10) {
            return null;
        }
    }
}
