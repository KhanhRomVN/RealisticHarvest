package com.khanhromvn.realisticharvest.fertilizer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

/**
 * FertilizerProfile mô tả hiệu ứng phân bón data-driven.
 * JSON mẫu (data/realisticharvest/fertilizers/compost.json):
 * {
 *   "id": "realisticharvest:compost_fertilizer",
 *   "fertility_boost": 0.05,
 *   "organic_boost": 0.15,
 *   "decay_rate": 0.0005,
 *   "ph_shift": 0.0
 * }
 *
 * ph_shift: điều chỉnh pH thực (+/-) khi áp dụng.
 */
public class FertilizerProfile {

    public final ResourceLocation id;
    public final float fertilityBoost;
    public final float organicBoost;
    public final float decayRate;
    public final float phShift;

    public FertilizerProfile(ResourceLocation id,
                              float fertilityBoost,
                              float organicBoost,
                              float decayRate,
                              float phShift) {
        this.id = id;
        this.fertilityBoost = fertilityBoost;
        this.organicBoost = organicBoost;
        this.decayRate = decayRate;
        this.phShift = phShift;
    }

    public static FertilizerProfile fromJson(ResourceLocation id, JsonObject root) {
        float fertility = getFloat(root, "fertility_boost", 0f);
        float organic = getFloat(root, "organic_boost", 0f);
        float decay = getFloat(root, "decay_rate", 0.001f);
        float ph = getFloat(root, "ph_shift", 0f);
        return new FertilizerProfile(id, fertility, organic, decay, ph);
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        JsonElement e = obj.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : def;
    }
}
