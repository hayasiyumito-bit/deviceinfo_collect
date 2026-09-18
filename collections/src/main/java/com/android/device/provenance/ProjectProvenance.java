/*
 * Copyright (c) 2026 Yumito. All rights reserved.
 *
 * Public license: CC BY-NC 4.0 — non-commercial use only.
 * Commercial rights: reserved exclusively to copyright holder Yumito.
 *
 * Fingerprint: YDC-7F3A9C2E-202607 — do not remove (traceability watermark).
 */
package com.android.device.provenance;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 项目溯源标识：嵌入源码、构建产物与导出 JSON，用于追踪未经授权的商用分发。
 */
public final class ProjectProvenance {

    /** 全局追踪指纹，修改需同步 NOTICE / native / manifest / BuildConfig。 */
    public static final String FINGERPRINT = "YDC-7F3A9C2E-202607";

    public static final String PROJECT_ID = "device-collection";
    public static final String COPYRIGHT_HOLDER = "Yumito";
    public static final String LICENSE_PUBLIC = "CC-BY-NC-4.0";
    public static final String COMMERCIAL_RIGHTS = "Yumito exclusive — commercial use reserved";
    public static final String REPOSITORY = "https://gitee.com/Yumito/device-collection";
    public static final String TRACE_TAG = "YDC-YUMITO-DEVICE-COLLECTION";

    private ProjectProvenance() {
    }

    public static JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("fingerprint", FINGERPRINT);
            json.put("projectId", PROJECT_ID);
            json.put("copyrightHolder", COPYRIGHT_HOLDER);
            json.put("licensePublic", LICENSE_PUBLIC);
            json.put("commercialRights", COMMERCIAL_RIGHTS);
            json.put("repository", REPOSITORY);
            json.put("traceTag", TRACE_TAG);
            json.put("notice", "Unauthorized commercial use prohibited. Fingerprint must not be removed.");
        } catch (JSONException e) {
            // unreachable for fixed keys
        }
        return json;
    }

    public static String compactWatermark() {
        return FINGERPRINT + "|" + PROJECT_ID + "|" + COPYRIGHT_HOLDER + "|" + LICENSE_PUBLIC;
    }
}
