package com.khanhromvn.realisticharvest.soil;

/**
 * Enum các thuộc tính đất lõi cho Realistic Harvest.
 * Giá trị runtime sẽ được lưu dưới dạng float chuẩn hóa 0..1 (trừ pH có chuyển đổi).
 *
 * MOISTURE        : Độ ẩm đất ảnh hưởng tốc độ tăng trưởng và stress cây.
 * PH              : Độ pH thực (scale 3..10) được map về 0..1 để tính toán. 6.0-7.0 thường tối ưu nhiều crop.
 * FERTILITY       : Mức dưỡng chất tổng quát (N-P-K + vi lượng) dùng cho multiplier tăng trưởng.
 * TEXTURE         : Liên tục 0..1 (0=cát, ~0.5=đất thịt (loam), 1=sét) quyết định retention & drainage.
 * AERATION        : Độ thoáng khí; cao giúp rễ khỏe & tăng hiệu quả phân bón; thấp gây yếm khí.
 * ORGANIC_MATTER  : Chất hữu cơ; buffer giữ ẩm & giúp phục hồi fertility.
 */
public enum SoilAttribute {
    MOISTURE,
    PH,
    FERTILITY,
    TEXTURE,
    AERATION,
    ORGANIC_MATTER;

    /**
     * Chuyển pH thực (3..10 giả định phạm vi) sang giá trị chuẩn hóa 0..1.
     */
    public static float normalizePh(float phReal) {
        float clamped = Math.max(3f, Math.min(10f, phReal));
        return (clamped - 3f) / (10f - 3f);
    }

    /**
     * Chuyển giá trị chuẩn hóa về pH thực (3..10).
     */
    public static float denormalizePh(float normalized) {
        float clamped = Math.max(0f, Math.min(1f, normalized));
        return 3f + clamped * (10f - 3f);
    }

    /**
     * Clamp chung cho mọi attribute ngoại trừ pH (đã có mapping riêng).
     */
    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }
}
