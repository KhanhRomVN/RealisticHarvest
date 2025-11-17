package com.khanhromvn.realisticharvest.tool;

import com.khanhromvn.realisticharvest.soil.SoilCapability;
import com.khanhromvn.realisticharvest.soil.SoilData;
import com.khanhromvn.realisticharvest.soil.SoilAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.StringTextComponent;

/**
 * FarmHoeItem:
 *  - Right-click lên đất (block top) để cải tạo tăng Aeration & Organic Matter nhẹ.
 *  - Giảm nhẹ moisture (đất xới dễ thoát nước) và chuẩn hóa texture về loam (0.5) từng bước.
 *  - FUTURE: chỉ hoạt động trên các block cụ thể (farmland / cultivated_soil).
 *
 * Công thức:
 *  aeration += 0.05 (clamp 0..1)
 *  organic_matter += 0.02
 *  moisture -= 0.01
 *  texture tiến về 0.5 (loam): texture = texture + (0.5 - texture)*0.3
 *
 * Có độ bền (durability) giảm mỗi lần dùng nếu không ở creative.
 */
public class FarmHoeItem extends Item {

    public FarmHoeItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null || ctx.getLevel().isClientSide) {
            return ActionResultType.SUCCESS;
        }

        SoilData data = SoilCapability.getOrCreate(ctx.getLevel(), ctx.getClickedPos());
        float aer = data.get(SoilAttribute.AERATION);
        float org = data.get(SoilAttribute.ORGANIC_MATTER);
        float moisture = data.get(SoilAttribute.MOISTURE);
        float texture = data.get(SoilAttribute.TEXTURE);

        aer = Math.min(1f, aer + 0.05f);
        org = Math.min(1f, org + 0.02f);
        moisture = Math.max(0f, moisture - 0.01f);
        texture = texture + (0.5f - texture) * 0.3f;

        data.set(SoilAttribute.AERATION, aer);
        data.set(SoilAttribute.ORGANIC_MATTER, org);
        data.set(SoilAttribute.MOISTURE, moisture);
        data.set(SoilAttribute.TEXTURE, texture);
        // Intensive farming tracking
        data.incrementHoeCount();

        player.displayClientMessage(new StringTextComponent(String.format(
                "[Hoe] Aer=%.2f Org=%.2f Moist=%.2f Tex=%.2f IntensiveCount=%d Bonus=%.2f",
                aer, org, moisture, texture, data.getHoeBonus() > 0 ? (int)(data.getHoeBonus()/0.01f) : 0, data.getHoeBonus())), false);

        // Giảm durability
        ItemStack stack = ctx.getItemInHand();
        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(ctx.getHand()));
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
