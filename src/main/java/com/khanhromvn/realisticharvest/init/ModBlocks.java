package com.khanhromvn.realisticharvest.init;

import com.khanhromvn.realisticharvest.RealisticHarvest;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Đăng ký Blocks cho Realistic Harvest.
 * Bao gồm cultivated_soil (đất cải tạo), irrigation_channel, water_emitter (cơ sở cho hệ thống tưới).
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RealisticHarvest.MOD_ID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RealisticHarvest.MOD_ID);

    // Cultivated Soil – placeholder block
    public static final RegistryObject<Block> CULTIVATED_SOIL = BLOCKS.register("cultivated_soil",
            () -> new Block(Block.Properties.of(Material.DIRT).strength(0.6F)));

    // Irrigation Channel – đơn giản: block để đánh dấu mạng tưới
    public static final RegistryObject<Block> IRRIGATION_CHANNEL = BLOCKS.register("irrigation_channel",
            () -> new Block(Block.Properties.of(Material.METAL).strength(1.0F)));

    // Water Emitter – nguồn cấp ẩm bán kính nhỏ
    public static final RegistryObject<Block> WATER_EMITTER = BLOCKS.register("water_emitter",
            () -> new Block(Block.Properties.of(Material.METAL).strength(1.5F)));

    // Item đại diện cho block (BlockItem)
    public static final RegistryObject<Item> CULTIVATED_SOIL_ITEM = BLOCK_ITEMS.register("cultivated_soil",
            () -> new BlockItem(CULTIVATED_SOIL.get(), new Item.Properties().tab(ItemGroup.TAB_BUILDING_BLOCKS)));

    public static final RegistryObject<Item> IRRIGATION_CHANNEL_ITEM = BLOCK_ITEMS.register("irrigation_channel",
            () -> new BlockItem(IRRIGATION_CHANNEL.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    public static final RegistryObject<Item> WATER_EMITTER_ITEM = BLOCK_ITEMS.register("water_emitter",
            () -> new BlockItem(WATER_EMITTER.get(), new Item.Properties().tab(ItemGroup.TAB_REDSTONE)));

    /**
     * Đăng ký với event bus.
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ITEMS.register(bus);
    }
}
