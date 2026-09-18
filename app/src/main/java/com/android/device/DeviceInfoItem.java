package com.android.device;

/** 列表展示用的单条设备信息。 */
public final class DeviceInfoItem {

    private final String originalKey;
    private final String translatedKey;
    private final String displayValue;
    private final String category;
    private final String fullValue;

    public DeviceInfoItem(
            String originalKey,
            String translatedKey,
            String displayValue,
            String category,
            String fullValue
    ) {
        this.originalKey = originalKey;
        this.translatedKey = translatedKey;
        this.displayValue = displayValue;
        this.category = category;
        this.fullValue = fullValue;
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public String getTranslatedKey() {
        return translatedKey;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public String getCategory() {
        return category;
    }

    public String getFullValue() {
        return fullValue;
    }
}
