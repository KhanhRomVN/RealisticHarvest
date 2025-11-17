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
 * Soil pH Sensor: Right-click lên block để đọc pH & gợi ý điều chỉnh.
 * FUTURE: Hiển thị overlay HUD thay vì chat.
 */
public class SoilPhSensorItem extends Item {

    public SoilPhSensorItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null || ctx.getLevel().isClientSide) {
            return ActionResultType.SUCCESS;
        }

        SoilData data = SoilCapability.getIfExists(ctx.getLevel(), ctx.getClickedPos());
        if (data == null) {
            // Tạo mới (lazy init) để người chơi có dữ liệu đọc
            data = SoilCapability.getOrCreate(ctx.getLevel(), ctx.getClickedPos());
        }

        float ph = data.getPhReal();
        String status;
        if (ph < 5.5f) status = "Quá acid, dùng pH Up.";
        else if (ph > 7.5f) status = "Quá kiềm, dùng pH Down.";
        else status = "pH tối ưu.";

        player.displayClientMessage(new StringTextComponent(String.format("[Sensor pH] pH=%.2f => %s", ph, status)), false);
        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // hiệu ứng lấp lánh cho cảm biến
    }
}
