package com.khanhromvn.realisticharvest.crop;

import com.khanhromvn.realisticharvest.RealisticHarvest;
import com.khanhromvn.realisticharvest.soil.SoilCapability;
import com.khanhromvn.realisticharvest.soil.SoilData;
import com.khanhromvn.realisticharvest.soil.SoilAttribute;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CropGrowthHandler:
 * Lắng sự kiện tăng trưởng crop và điều chỉnh dựa trên SoilData + CropProfile.
 *
 * Logic:
 *  - Pre: tính growthMultiplier từ profile & soil.
 *    Nếu multiplier <= 0.25 => hủy tăng trưởng (stress cao).
 *    Nếu multiplier giữa (0.25..1) => cho tăng trưởng nhưng có xác suất scale (random).
 *    Nếu >1 => cho tăng trưởng và có cơ hội tăng thêm 1 stage (bonus nhỏ).
 *  - Post: (tạm thời) không làm gì, mở rộng sau cho phân bố fertilizer consume...
 *
 * Stress tính qua computeGrowth của CropProfile; profile được load từ JSON.
 * Nếu không có profile => mặc định vanilla.
 *
 * TODO:
 *  - Intensive farming bonus ứng với thao tác người chơi (hoe cải tạo -> tăng aeration)
 *  - Fertility consumption mỗi growth
 */
@Mod.EventBusSubscriber(modid = RealisticHarvest.MOD_ID)
public class CropGrowthHandler {

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        ServerWorld world = (ServerWorld) event.getWorld();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        Block block = state.getBlock();

        if (!(block instanceof CropsBlock)) {
            return;
        }

        // Fetch soil data
        SoilData soil = SoilCapability.getIfExists(world, pos.below()) == null
                ? SoilCapability.getOrCreate(world, pos.below())
                : SoilCapability.getIfExists(world, pos.below());

        // Basic soil values
        float moisture = soil.get(SoilAttribute.MOISTURE);
        float phReal = soil.getPhReal();
        float fertility = soil.get(SoilAttribute.FERTILITY);
        float aeration = soil.get(SoilAttribute.AERATION);
        float organic = soil.get(SoilAttribute.ORGANIC_MATTER);

        // Resolve crop id; mapping: try registry name
        ResourceLocation cropId = block.getRegistryName();
        if (cropId == null) {
            return;
        }

        CropProfile profile = CropProfileLoader.getProfile(cropId);
        if (profile == null) {
            // Không có profile => vanilla
            return;
        }

        // Intensive bonus & stress penalty integration
        float hoeBonus = soil.getHoeBonus();              // 0..0.10 thêm trực tiếp
        float stressPenaltyFactor = 1f - soil.getStressScore() * 0.30f; // stress giảm tối đa 30%
        float baseGrowth = profile.computeGrowth(moisture, phReal, fertility, aeration, organic);
        float growthMultiplier = baseGrowth * (1f + hoeBonus) * stressPenaltyFactor;

        // Decide action with stress tracking
        if (growthMultiplier <= 0.25f) {
            soil.registerStress(0.02f); // tăng stress vì điều kiện quá kém
            event.setResult(BlockEvent.Result.DENY);
            return;
        }

        // Nếu multiplier giữa 0.25 - 1 => xác suất
        if (growthMultiplier < 1f) {
            float chance = growthMultiplier; // trực tiếp dùng multiplier làm xác suất
            if (world.getRandom().nextFloat() > chance) {
                soil.registerStress(0.01f); // tăng nhẹ stress do tăng trưởng bị từ chối ngẫu nhiên
                event.setResult(BlockEvent.Result.DENY);
            } else {
                // phục hồi nhẹ nếu thành công trong vùng suboptimal
                soil.passiveStressRecovery();
            }
            return;
        }

        // multiplier >= 1: cho phép tăng trưởng bình thường; bonus tăng thêm stage nếu multiplier cao
        if (growthMultiplier >= 1.2f) {
            float bonusChance = Math.min((growthMultiplier - 1.0f) * 0.25f, 0.35f);
            if (world.getRandom().nextFloat() < bonusChance) {
                if (block instanceof CropsBlock) {
                    CropsBlock crops = (CropsBlock) block;
                    int age = crops.getAge(state);
                    int maxAge = crops.getMaxAge();
                    if (age < maxAge) {
                        int newAge = Math.min(maxAge, age + 1);
                        world.setBlock(pos, crops.getStateForAge(newAge), 2);
                    }
                }
            }
            soil.passiveStressRecovery(); // điều kiện tốt -> giảm stress
        } else {
            // optimal nhưng không đủ cao cho bonus vẫn hồi nhẹ
            soil.passiveStressRecovery();
        }
    }

    @SubscribeEvent
    public static void onCropGrowPost(BlockEvent.CropGrowEvent.Post event) {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.getWorld();
        BlockPos pos = event.getPos();
        SoilData soil = SoilCapability.getIfExists(world, pos.below());
        if (soil != null) {
            // Tiêu hao tài nguyên sau tăng trưởng thành công
            soil.consumeAfterGrowth();
            // Passive recovery thêm (nhẹ) để stress có thể giảm dần theo thời gian khi tăng trưởng diễn ra
            soil.passiveStressRecovery();
        }
    }
}
