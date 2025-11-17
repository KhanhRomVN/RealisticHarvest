package com.khanhromvn.realisticharvest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.khanhromvn.realisticharvest.RealisticHarvest;
import com.khanhromvn.realisticharvest.soil.SoilCapability;
import com.khanhromvn.realisticharvest.soil.SoilData;
import com.khanhromvn.realisticharvest.soil.SoilAttribute;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * RHCommands:
 * Lệnh gốc /rh với subcommand:
 *
 * /rh soilinfo                 -> lấy thông tin soil tại block dưới chân người chơi
 * /rh soilinfo <x> <y> <z>     -> lấy soil tại tọa độ chỉ định
 *
 * FUTURE:
 *  - /rh set <attr> <value>
 *  - /rh stress reset
 *
 * Ghi chú: SoilData lưu ở chunk-level và tạo lazy khi truy cập.
 */
@Mod.EventBusSubscriber(modid = RealisticHarvest.MOD_ID)
public class RHCommands {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rh")
                        .then(buildSoilInfo())
        );
    }

    private static ArgumentBuilder<CommandSource, ?> buildSoilInfo() {
        return Commands.literal("soilinfo")
                .executes(ctx -> executeSoilInfoPlayer(ctx))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> executeSoilInfoCoords(ctx,
                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                IntegerArgumentType.getInteger(ctx, "z")
                                        )))));
    }

    private static int executeSoilInfoPlayer(CommandContext<CommandSource> ctx) {
        CommandSource source = ctx.getSource();
        if (!source.getEntity().getCommandSenderWorld().isClientSide) {
            BlockPos pos = new BlockPos(source.getPosition());
            // Kiểm tra block dưới chân
            BlockPos soilPos = pos.below();
            sendSoilInfo(source, soilPos);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSoilInfoCoords(CommandContext<CommandSource> ctx, int x, int y, int z) {
        CommandSource source = ctx.getSource();
        if (!source.getEntity().getCommandSenderWorld().isClientSide) {
            BlockPos soilPos = new BlockPos(x, y, z);
            sendSoilInfo(source, soilPos);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void sendSoilInfo(CommandSource source, BlockPos pos) {
        SoilData soil = SoilCapability.getOrCreate(source.getLevel(), pos);
        float moisture = soil.get(SoilAttribute.MOISTURE);
        float ph = soil.getPhReal();
        float fertility = soil.get(SoilAttribute.FERTILITY);
        float aer = soil.get(SoilAttribute.AERATION);
        float org = soil.get(SoilAttribute.ORGANIC_MATTER);
        float stress = soil.getStressScore();
        float hoeBonus = soil.getHoeBonus();

        source.sendSuccess(new StringTextComponent(String.format("[Soil @ %d %d %d]", pos.getX(), pos.getY(), pos.getZ())), false);
        source.sendSuccess(new StringTextComponent(String.format("Moisture=%.3f pH=%.2f Fertility=%.3f Aeration=%.3f Organic=%.3f", moisture, ph, fertility, aer, org)), false);
        source.sendSuccess(new StringTextComponent(String.format("Stress=%.3f HoeBonus=%.3f", stress, hoeBonus)), false);
    }
}
