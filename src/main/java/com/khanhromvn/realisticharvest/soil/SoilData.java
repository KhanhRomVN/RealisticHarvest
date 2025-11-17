package com.khanhromvn.realisticharvest.soil;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumMap;
import java.util.Map;

/**
 * SoilData lưu trữ trạng thái động của đất tại vị trí / ô (design hiện tại đơn giản).
 * Sau có thể chuyển sang grid chunk-level để tối ưu.
 *
 * Các thuộc tính:
 *  - Mọi attribute (trừ pH) lưu dạng normalized 0..1
 *  - pH lưu dạng normalized (mapping thực tế dùng SoilAttribute.normalizePh / denormalizePh)
 *
 * Thêm: quản lý phân bón (fertility & organic matter) và thời gian decay.
 */
public class SoilData implements INBTSerializable<INBT> {

    private final Map<SoilAttribute, Float> values = new EnumMap<>(SoilAttribute.class);

    // Trạng thái phân bón
    private float fertilizerIntensity = 0f;      // ảnh hưởng vào fertility
    private float organicMatterBonus = 0f;       // ảnh hưởng giữ ẩm + giảm decay fertility
    private int ticksSinceFertilized = 0;
    private float fertilizerDecayRate = 0.001f;  // default; sẽ override bởi profile

    // Phase 2: stress & intensive tracking
    private float stressScore = 0f;              // tích lũy stress (0..1) ảnh hưởng tăng trưởng về sau
    private int hoeCount = 0;                    // số lần cải tạo (dùng hoe) -> bonus intensive

    public SoilData() {
        // Giá trị khởi tạo
        values.put(SoilAttribute.MOISTURE, 0.5f);
        values.put(SoilAttribute.PH, SoilAttribute.normalizePh(6.5f));
        values.put(SoilAttribute.FERTILITY, 0.4f);
        values.put(SoilAttribute.TEXTURE, 0.5f);      // loam mặc định
        values.put(SoilAttribute.AERATION, 0.5f);
        values.put(SoilAttribute.ORGANIC_MATTER, 0.3f);
    }

    /* ------------------- Basic Accessors ------------------- */

    public float get(SoilAttribute attr) {
        return values.getOrDefault(attr, 0f);
    }

    public void set(SoilAttribute attr, float v) {
        if (attr == SoilAttribute.PH) {
            values.put(SoilAttribute.PH, SoilAttribute.clamp01(v));
        } else {
            values.put(attr, SoilAttribute.clamp01(v));
        }
    }

    public float getPhReal() {
        return SoilAttribute.denormalizePh(get(SoilAttribute.PH));
    }

    public void setPhReal(float phReal) {
        set(SoilAttribute.PH, SoilAttribute.normalizePh(phReal));
    }

    /* ------------------- Moisture Dynamics ------------------- */

    /**
     * Áp dụng delta độ ẩm (ví dụ từ mưa, tưới). Clamp sau tính toán.
     */
    public void applyMoistureDelta(float delta) {
        float current = get(SoilAttribute.MOISTURE);
        set(SoilAttribute.MOISTURE, current + delta);
    }

    /**
     * Tính evaporation rate dựa trên texture (cát thoát nhanh), aeration, nhiệt độ & organic matter.
     * temperatureNormalized: 0..1 (mapping custom)
     */
    public float computeEvaporation(float temperatureNormalized, boolean isSunny) {
        float texture = get(SoilAttribute.TEXTURE);      // cát=0, sét=1
        float aeration = get(SoilAttribute.AERATION);
        float organic = get(SoilAttribute.ORGANIC_MATTER);

        // Cát: retention thấp => bốc hơi nhanh hơn => scaleTexture > 1 khi gần 0
        float textureFactor = 1.0f + (0.5f - texture) * 0.8f; // nếu texture=0 -> +0.4; texture=1 -> -0.4
        float aerationFactor = 1.0f + (aeration - 0.5f) * 0.3f; // thoáng khí cao => bốc hơi thêm
        float organicFactor = 1.0f - organic * 0.25f;          // hữu cơ cao giảm bốc hơi

        float base = 0.0008f; // cơ bản mỗi tick (adjust qua config)
        float sunBonus = isSunny ? 0.0005f : 0f;

        return base * textureFactor * aerationFactor * organicFactor * (0.5f + temperatureNormalized) + sunBonus;
    }

    /* ------------------- Fertilizer Handling ------------------- */

    public void applyFertilizer(FertilizerEffect effect) {
        fertilizerIntensity += effect.fertilityBoost;
        organicMatterBonus += effect.organicMatterBoost;
        fertilizerDecayRate = effect.decayRate;
        // Áp dụng ngay vào fertility & organic matter
        set(SoilAttribute.FERTILITY, get(SoilAttribute.FERTILITY) + effect.fertilityBoost);
        set(SoilAttribute.ORGANIC_MATTER, get(SoilAttribute.ORGANIC_MATTER) + effect.organicMatterBoost);
        ticksSinceFertilized = 0;
    }

    public void tickFertilizerDecay() {
        if (fertilizerIntensity > 0f) {
            fertilizerIntensity -= fertilizerDecayRate;
            if (fertilizerIntensity < 0f) fertilizerIntensity = 0f;
            // Fertility giảm nhẹ theo cường độ decay, organic matter giảm chậm hơn
            float fert = get(SoilAttribute.FERTILITY);
            set(SoilAttribute.FERTILITY, fert - fertilizerDecayRate * 0.5f);

            float om = get(SoilAttribute.ORGANIC_MATTER);
            set(SoilAttribute.ORGANIC_MATTER, om - fertilizerDecayRate * 0.2f);
        }
        ticksSinceFertilized++;
    }

    /* ------------------- Global Tick Update ------------------- */

    /**
     * Cập nhật soil mỗi tick schedule (gọi không phải mỗi tick game để tránh nặng).
     * @param temperatureNormalized 0..1
     * @param raining có mưa?
     * @param irrigationBonus bổ sung moisture từ hệ thống tưới (0..1 delta nhỏ)
     */
    public void scheduledUpdate(float temperatureNormalized, boolean raining, float irrigationBonus) {
        // Evaporation
        float evap = computeEvaporation(temperatureNormalized, !raining);
        applyMoistureDelta(-evap);

        // Rainfall
        if (raining) {
            // texture ảnh hưởng hấp thụ
            float texture = get(SoilAttribute.TEXTURE);
            float absorption = 0.004f * (0.7f + (0.5f - Math.abs(texture - 0.5f))); // loam hấp thụ tốt
            applyMoistureDelta(absorption);
        }

        // Irrigation
        if (irrigationBonus > 0f) {
            applyMoistureDelta(irrigationBonus);
        }

        // Clamp moisture cuối
        set(SoilAttribute.MOISTURE, get(SoilAttribute.MOISTURE));

        // Decay fertilizer
        tickFertilizerDecay();
    }

    /* ------------------- Serialization ------------------- */

    @Override
    public INBT serializeNBT() {
        return serialize();
    }

    @Override
    public void deserializeNBT(INBT nbt) {
        if (nbt instanceof CompoundNBT) {
            deserialize(nbt);
        }
    }

    public INBT serialize() {
        CompoundNBT tag = new CompoundNBT();
        values.forEach((a, v) -> tag.putFloat(a.name(), v));
        tag.putFloat("fertilizerIntensity", fertilizerIntensity);
        tag.putFloat("organicMatterBonus", organicMatterBonus);
        tag.putInt("ticksSinceFertilized", ticksSinceFertilized);
        tag.putFloat("fertilizerDecayRate", fertilizerDecayRate);
        tag.putFloat("stressScore", stressScore);
        tag.putInt("hoeCount", hoeCount);
        return tag;
    }

    public void deserialize(INBT inbt) {
        if (inbt instanceof CompoundNBT) {
            CompoundNBT tag = (CompoundNBT) inbt;
            for (SoilAttribute a : SoilAttribute.values()) {
                if (tag.contains(a.name())) {
                    set(a, tag.getFloat(a.name()));
                }
            }
            fertilizerIntensity = tag.getFloat("fertilizerIntensity");
            organicMatterBonus = tag.getFloat("organicMatterBonus");
            ticksSinceFertilized = tag.getInt("ticksSinceFertilized");
            fertilizerDecayRate = tag.getFloat("fertilizerDecayRate");
            if (tag.contains("stressScore")) stressScore = tag.getFloat("stressScore");
            if (tag.contains("hoeCount")) hoeCount = tag.getInt("hoeCount");
        }
    }

    /* ------------------- Growth & Stress / Intensive Farming ------------------- */

    /**
     * Gọi khi tăng trưởng thành công để tiêu hao fertility nhẹ và organic cực nhỏ.
     * Có thể scale theo pH / moisture stress để tăng tiêu hao.
     */
    public void consumeAfterGrowth() {
        float fertility = get(SoilAttribute.FERTILITY);
        float organic = get(SoilAttribute.ORGANIC_MATTER);

        // tiêu hao cơ bản
        fertility -= 0.0025f;
        organic -= 0.0008f;

        // Nếu stress cao (>0.5) tiêu hao thêm
        if (stressScore > 0.5f) {
            fertility -= 0.0015f;
        }

        set(SoilAttribute.FERTILITY, fertility);
        set(SoilAttribute.ORGANIC_MATTER, organic);
    }

    /**
     * Đăng ký stress khi tăng trưởng bị từ chối hoặc thông số ngoài range.
     * amount: 0..1 (nhỏ) sẽ cộng dồn và clamp.
     */
    public void registerStress(float amount) {
        stressScore += amount;
        if (stressScore > 1f) stressScore = 1f;
    }

    /**
     * Giảm stress chậm khi điều kiện tốt (có thể được gọi bởi routine bên ngoài).
     */
    public void passiveStressRecovery() {
        if (stressScore > 0f) {
            stressScore -= 0.001f;
            if (stressScore < 0f) stressScore = 0f;
        }
    }

    public float getStressScore() {
        return stressScore;
    }

    /**
     * Gọi khi dùng hoe cải tạo đất (FarmHoeItem).
     */
    public void incrementHoeCount() {
        hoeCount++;
    }

    /**
     * Intensive bonus: chuyển hoeCount thành multiplier add-on cho growth.
     * Diminishing returns: bonus = min(hoeCount * 0.01, 0.10)
     */
    public float getHoeBonus() {
        float bonus = hoeCount * 0.01f;
        if (bonus > 0.10f) bonus = 0.10f;
        return bonus;
    }

    /* ------------------- Inner Fertilizer Effect Placeholder ------------------- */

    public static class FertilizerEffect {
        public final float fertilityBoost;
        public final float organicMatterBoost;
        public final float decayRate; // per scheduled tick

        public FertilizerEffect(float fertilityBoost, float organicMatterBoost, float decayRate) {
            this.fertilityBoost = fertilityBoost;
            this.organicMatterBoost = organicMatterBoost;
            this.decayRate = decayRate;
        }

        public static FertilizerEffect basicCompost() {
            return new FertilizerEffect(0.05f, 0.15f, 0.0005f);
        }

        public static FertilizerEffect nitrogenMix() {
            return new FertilizerEffect(0.30f, 0.05f, 0.0025f);
        }

        public static FertilizerEffect potassium() {
            return new FertilizerEffect(0.15f, 0.02f, 0.0015f);
        }
    }
}
