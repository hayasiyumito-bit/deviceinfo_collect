package com.android.device.software;

import android.os.Build;

import com.android.utils.Cmd;

import java.util.HashMap;
import java.util.Map;

public class ServiceList {

    /**
     * 获取服务列表信息
     *
     * @return 服务列表信息字符串
     */
    private static final Map<String, String> BRAND_PATTERNS = new HashMap<String, String>() {{
// 全面合并生态链/关联品牌（按品牌集团维度组织）

// 小米生态链（含子品牌+系统层）
        put("xiaomi", "redmi|xiaomi|miui|blackshark");
        put("redmi", "redmi|xiaomi|miui|blackshark");
        put("miui", "redmi|xiaomi|miui|blackshark");
        put("blackshark", "redmi|xiaomi|miui|blackshark");

// OPPO生态链（含子品牌+系统层+关联品牌）
        put("oppo", "oppo|realme|oneplus|coloros|heytap|iqoo");
        put("realme", "oppo|realme|oneplus|coloros|heytap|iqoo");
        put("oneplus", "oppo|realme|oneplus|coloros|heytap|iqoo");
        put("coloros", "oppo|realme|oneplus|coloros|heytap|iqoo");
        put("heytap", "oppo|realme|oneplus|coloros|heytap|iqoo");
        put("iqoo", "oppo|realme|oneplus|coloros|heytap|iqoo");

// vivo生态链（含子品牌）
        put("vivo", "vivo|iqoo");
        put("iqoo", "vivo|iqoo"); // 已存在于OPPO链，形成双向关联

// 传音生态链（非洲市场品牌矩阵）
        put("tecno", "tecno|infinix|itel");
        put("infinix", "tecno|infinix|itel");
        put("itel", "tecno|infinix|itel");

// 三星生态链（含系统层）
        put("samsung", "samsung|oneui");
        put("oneui", "samsung|oneui");

// 中兴生态链（含子品牌）
        put("zte", "zte|nubia");
        put("nubia", "zte|nubia");

// 华为生态链（含子品牌）
        put("huawei", "huawei|honor");
        put("honor", "huawei|honor");

// 谷歌服务链（系统级关联）
        put("google", "google|android|gms");
        put("android", "google|android|gms");
        put("gms", "google|android|gms");

// 独立品牌（无明确生态关联）
        put("motorola", "motorola");
        put("sony", "sony");
        put("t-mobile", "t-mobile");
        put("lge", "lge");
        put("nokia", "nokia");
        put("tcl", "tcl");
        put("asus", "asus");
        put("cubot", "cubot");
        put("cricket", "cricket");
        put("lenovo", "lenovo");
        put("ulefone", "ulefone");
        put("celero5g", "celero5g");
        put("kddi", "kddi");
        put("dish", "dish");
        put("alps", "alps");
        put("doogee", "doogee");
        put("umidigi", "umidigi");
        put("acer", "acer");
        put("vortex", "vortex");
        put("8849", "8849");
        put("crosscall", "crosscall");
        put("blackview", "blackview");
        put("oscal", "oscal");
        put("kyocera", "kyocera");
        put("teclast", "teclast");
        put("hotwav", "hotwav");
        put("freeyond", "freeyond");
        put("spc", "spc");
        put("sky_devices", "sky_devices");
        put("jala", "jala");
        put("multilaser", "multilaser");
        put("alcatel", "alcatel");
        put("iiif150", "iiif150");
        put("zuum", "zuum");
        put("kxd", "kxd");
        put("hotpepper", "hotpepper");
        put("vios", "vios");
        put("danew", "danew");
        put("oukitel", "oukitel");
    }};

    /**
     * 获取服务列表信息
     *
     * @return 服务列表信息字符串
     */
    public static String getServiceListInfo() {
        try {
/*            String brand = Build.BRAND.toLowerCase();
            String pattern = BRAND_PATTERNS.get(brand);
            if (pattern == null || pattern.isEmpty()) {
                return "";
            } else {
                return Cmd.exe("service list | grep -Ei '" + pattern + "'");
            }*/
            String brands = "Sony|motorola|HUAWEI|asus|TECNO|samsung|Redmi|T-Mobile|Nokia|KYOCERA|Teclast|iQOO|Blackview|ZTE|TCL|Vios|CUBOT|Crosscall|Cricket|KXD|Itel|google|OSCAL|JALA|HONOR|vivo|xiaomi|realme|blackshark|nubia|8849|DOOGEE|BLU|DANEW|Infinix|HOTWAV|OPPO|qti|Celero5G|FreeYond|Alcatel|ZUUM|Realme|Ulefone|KDDI|UMIDIGI|OUKITEL|IIIF150|Acer|HotPepper|Mi|lge|OnePlus|Amazon|Dish|alps|Xiaomi|htc|Lenovo|Vortex|SPC|Multilaser|SPRD|Sky_Devices|POCO";
            return Cmd.exe("service list | grep -Ei '" + brands + "'");
        } catch (Exception e) {
            return "";
        }
    }
}

