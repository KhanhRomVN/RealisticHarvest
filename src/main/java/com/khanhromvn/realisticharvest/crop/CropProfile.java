package com.khanhromvn.realisticharvest.crop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/**
 * CropProfile biểu diễn yêu cầu & tham số tăng trưởng cho một loại crop.
 * Data nguồn từ JSON (datapack):
 * {
 *   "crop": "minecraft:wheat",
 *   "optimal": {
 *     "moisture": { "min": 0.45, "max": 0.75 },
 *     "ph": { "min": 6.0, "max": 7.0 },
 *     "fertility": { "min": 0.50, "max": 0.90 }
 *   },
 *   "stress_penalty": {
 *     "moisture": 0.4,
 *     "ph": 0.3,
 *     "fertility": 0.5
 *   },
 *   "growth_multiplier": 1.0,
 *   "intensive_bonus": {
 *     "aeration": { "threshold": 0.6, "bonus": 0.05 },
 *     "organic_matter": { "threshold": 0.5, "bonus": 0.07 }
 *   }
 * }
 */
public class CropProfile {

    public static class Range {
        public final float min;
        public final float max;

        public Range(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public boolean in(float v) {
            return v >= min && v <= max;
        }

        public float distance(float v) {
            if (in(v)) return 0f;
            if (v < min) return min - v;
            return v - max;
        }
    }

    public final ResourceLocation cropId;

    public final Range moistureRange;
    public final Range phRange;           // ph thực
    public final Range fertilityRange;

    public final float moisturePenalty;
    public final float phPenalty;
    public final float fertilityPenalty;

    public final float baseGrowthMultiplier;

    public final float aerationThreshold;
    public final float aerationBonus;
    public final float organicThreshold;
    public final float organicBonus;

    public CropProfile(ResourceLocation cropId,
                       Range moistureRange,
                       Range phRange,
                       Range fertilityRange,
                       float moisturePenalty,
                       float phPenalty,
                       float fertilityPenalty,
                       float baseGrowthMultiplier,
                       float aerationThreshold,
                       float aerationBonus,
                       float organicThreshold,
                       float organicBonus) {
        this.cropId = cropId;
        this.moistureRange = moistureRange;
        this.phRange = phRange;
        this.fertilityRange = fertilityRange;
        this.moisturePenalty = moisturePenalty;
        this.phPenalty = phPenalty;
        this.fertilityPenalty = fertilityPenalty;
        this.baseGrowthMultiplier = baseGrowthMultiplier;
        this.aerationThreshold = aerationThreshold;
        this.aerationBonus = aerationBonus;
        this.organicThreshold = organicThreshold;
        this.organicBonus = organicBonus;
    }

    public static CropProfile fromJson(ResourceLocation id, JsonObject root) {
        JsonObject optimal = root.getAsJsonObject("optimal");
        Range moisture = toRange(optimal.getAsJsonObject("moisture"));
        Range ph = toRange(optimal.getAsJsonObject("ph"));
        Range fertility = toRange(optimal.getAsJsonObject("fertility"));

        JsonObject stress = root.getAsJsonObject("stress_penalty");
        float moisturePen = getFloat(stress, "moisture", 0.4f);
        float phPen = getFloat(stress, "ph", 0.3f);
        float fertilityPen = getFloat(stress, "fertility", 0.5f);

        float growthMultiplier = getFloat(root, "growth_multiplier", 1.0f);

        JsonObject intensive = root.has("intensive_bonus") ? root.getAsJsonObject("intensive_bonus") : new JsonObject();
        JsonObject aerationObj = intensive.has("aeration") ? intensive.getAsJsonObject("aeration") : new JsonObject();
        JsonObject organicObj = intensive.has("organic_matter") ? intensive.getAsJsonObject("organic_matter") : new JsonObject();

        float aerationThr = getFloat(aerationObj, "threshold", 0.6f);
        float aerationBon = getFloat(aerationObj, "bonus", 0.05f);
        float organicThr = getFloat(organicObj, "threshold", 0.5f);
        float organicBon = getFloat(organicObj, "bonus", 0.07f);

        return new CropProfile(id, moisture, ph, fertility,
                moisturePen, phPen, fertilityPen,
                growthMultiplier,
                aerationThr, aerationBon, organicThr, organicBon);
    }

    private static Range toRange(JsonObject obj) {
        return new Range(getFloat(obj, "min", 0f), getFloat(obj, "max", 1f));
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : def;
    }

    /**
     * Tính growth multiplier dựa trên soil value đã chuẩn hóa (trừ pH dùng real).
     */
    public float computeGrowth(float moistureNorm, float phReal, float fertilityNorm,
                               float aerationNorm, float organicNorm) {

        float mFactor = factor(moistureRange, moistureNorm, moisturePenalty);
        float pFactor = factor(phRange, phReal, phPenalty);
        float fFactor = factor(fertilityRange, fertilityNorm, fertilityPenalty);

        float base = baseGrowthMultiplier * mFactor * pFactor * fFactor;

        float intensiveBonus = 0f;
        if (aerationNorm >= aerationThreshold) intensiveBonus += aerationBonus;
        if (organicNorm >= organicThreshold) intensiveBonus += organicBonus;

        return base * (1f + intensiveBonus);
    }

    private float factor(Range r, float value, float penalty) {
        float dist = r.distance(value);
        if (dist == 0f) return 1f;
        // Dist scale thô: clamp 0..1
        float scaled = Math.min(dist / (r.max - r.min + 0.0001f), 1f);
        return 1f - penalty * scaled;
    }
}
