package com.khanhromvn.realisticharvest.fertilizer;

import com.khanhromvn.realisticharvest.soil.SoilCapability;
import com.khanhromvn.realisticharvest.soil.SoilData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import com.khanhromvn.realisticharvest.fertilizer.FertilizerProfile;
import com.khanhromvn.realisticharvest.fertilizer.FertilizerProfileLoader;

/**
 * FertilizerItem: áp dụng hiệu ứng phân bón lên SoilData tại vị trí block được right-click.
 * Data-driven:
 *  - Nếu tìm thấy FertilizerProfile (fertilizers/*.json) theo registry name -> dùng giá trị profile.
 *  - Nếu không thấy -> fallback mapping legacy (basicCompost(), nitrogenMix(), potassium()).
 *  - pH Up/Down vẫn xử lý đặc biệt (hoặc có thể có profile riêng với ph_shift).
 *
 * TODO:
 *  - Cooldown / hạn chế spam
 *  - Config tùy chỉnh mức tiêu hao và multipliers
 */
public class FertilizerItem extends Item {

    public FertilizerItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null || ctx.getLevel().isClientSide) {
            return ActionResultType.SUCCESS;
        }

        SoilData data = SoilCapability.getOrCreate(ctx.getLevel(), ctx.getClickedPos());
        String path = this.getRegistryName() != null ? this.getRegistryName().getPath() : "";
        ResourceLocation rid = this.getRegistryName();

        // Đặc biệt cho pH Up/Down nếu chưa có profile
        if ("ph_up_fertilizer".equals(path)) {
            data.setPhReal(data.getPhReal() + 0.3f);
            consume(player, ctx.getItemInHand());
            player.displayClientMessage(new StringTextComponent("[Fertilizer] pH Up áp dụng (+0.30)."), false);
            return ActionResultType.SUCCESS;
        }
        if ("ph_down_fertilizer".equals(path)) {
            data.setPhReal(data.getPhReal() - 0.3f);
            consume(player, ctx.getItemInHand());
            player.displayClientMessage(new StringTextComponent("[Fertilizer] pH Down áp dụng (-0.30)."), false);
            return ActionResultType.SUCCESS;
        }

        // Data-driven profile
        FertilizerProfile profile = rid != null ? FertilizerProfileLoader.getProfile(rid) : null;

        SoilData.FertilizerEffect effect;
        float phShift = 0f;
        if (profile != null) {
            effect = new SoilData.FertilizerEffect(profile.fertilityBoost, profile.organicBoost, profile.decayRate);
            phShift = profile.phShift;
        } else {
            // Fallback legacy mapping
            switch (path) {
                case "compost_fertilizer":
                    effect = SoilData.FertilizerEffect.basicCompost();
                    break;
                case "nitrogen_mix_fertilizer":
                    effect = SoilData.FertilizerEffect.nitrogenMix();
                    break;
                case "potassium_mix_fertilizer":
                    effect = SoilData.FertilizerEffect.potassium();
                    break;
                default:
                    player.displayClientMessage(new StringTextComponent("[Fertilizer] Không xác định hoặc chưa có profile."), false);
                    return ActionResultType.SUCCESS;
            }
        }

        // Áp dụng hiệu ứng
        data.applyFertilizer(effect);
        if (phShift != 0f) {
            data.setPhReal(data.getPhReal() + phShift);
        }

        consume(player, ctx.getItemInHand());
        player.displayClientMessage(new StringTextComponent(String.format(
                "[Fertilizer] %s fertility+%.2f organic+%.2f decay=%.4f%s",
                path, effect.fertilityBoost, effect.organicMatterBoost, effect.decayRate,
                phShift != 0f ? String.format(" pHshift=%.2f", phShift) : "")), false);

        return ActionResultType.SUCCESS;
    }

    private void consume(PlayerEntity player, ItemStack stack) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }
}
