package com.khanhromvn.realisticharvest.tool;

import com.khanhromvn.realisticharvest.soil.SoilCapability;
import com.khanhromvn.realisticharvest.soil.SoilData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.StringTextComponent;

/**
 * MoistureMeterItem: đo độ ẩm đất & đưa ra đánh giá trạng thái thủy phần.
 * Right-click vào block đất bất kỳ để hiển thị thông tin.
 *
 * Phân loại trạng thái:
 *  - <0.25 : Rất khô (Dry)
 *  - 0.25-0.45 : Thiếu ẩm nhẹ
 *  - 0.45-0.75 : Tối ưu
 *  - 0.75-0.90 : Ẩm cao (Risk root rot trong tương lai)
 *  - >0.90 : Quá dư nước
 *
 * FUTURE:
 *  - Overlay thay vì chat message
 *  - Hiển thị lịch sử / xu hướng
 */
public class MoistureMeterItem extends Item {

    public MoistureMeterItem(Properties props) {
        super(props);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null || ctx.getLevel().isClientSide) {
            return ActionResultType.SUCCESS;
        }

        SoilData data = SoilCapability.getOrCreate(ctx.getLevel(), ctx.getClickedPos());
        float moisture = data.get(com.khanhromvn.realisticharvest.soil.SoilAttribute.MOISTURE);

        String status;
        if (moisture < 0.25f) status = "Rất khô";
        else if (moisture < 0.45f) status = "Thiếu ẩm nhẹ";
        else if (moisture < 0.75f) status = "Tối ưu";
        else if (moisture < 0.90f) status = "Ẩm cao";
        else status = "Quá dư nước";

        player.displayClientMessage(new StringTextComponent(String.format("[Moisture] %.2f => %s", moisture, status)), false);
        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
