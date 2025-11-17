package com.khanhromvn.realisticharvest.init;

import com.khanhromvn.realisticharvest.RealisticHarvest;
import com.khanhromvn.realisticharvest.tool.SoilPhSensorItem;
import com.khanhromvn.realisticharvest.tool.MoistureMeterItem;
import com.khanhromvn.realisticharvest.fertilizer.FertilizerItem;
import com.khanhromvn.realisticharvest.tool.FarmHoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Đăng ký Items cho Realistic Harvest.
 * Bao gồm: sensors, hoe, fertilizer items (compost, nitrogen, potassium, pH up/down).
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RealisticHarvest.MOD_ID);

    // Sensors
    public static final RegistryObject<Item> SOIL_PH_SENSOR = ITEMS.register("soil_ph_sensor",
            () -> new SoilPhSensorItem(new Item.Properties().tab(ItemGroup.TAB_TOOLS)));

    public static final RegistryObject<Item> MOISTURE_METER = ITEMS.register("moisture_meter",
            () -> new MoistureMeterItem(new Item.Properties().tab(ItemGroup.TAB_TOOLS)));

    // Basic hoe cải tạo đất (placeholder)
    public static final RegistryObject<Item> BASIC_HOE = ITEMS.register("basic_farm_hoe",
            () -> new FarmHoeItem(new Item.Properties().tab(ItemGroup.TAB_TOOLS).durability(350)));

    // Fertilizer items (placeholder logic – sẽ thêm subclass để xử lý riêng)
    public static final RegistryObject<Item> COMPOST = ITEMS.register("compost_fertilizer",
            () -> new FertilizerItem(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> NITROGEN_MIX = ITEMS.register("nitrogen_mix_fertilizer",
            () -> new FertilizerItem(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> POTASSIUM_MIX = ITEMS.register("potassium_mix_fertilizer",
            () -> new FertilizerItem(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> PH_UP = ITEMS.register("ph_up_fertilizer",
            () -> new FertilizerItem(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    public static final RegistryObject<Item> PH_DOWN = ITEMS.register("ph_down_fertilizer",
            () -> new FertilizerItem(new Item.Properties().tab(ItemGroup.TAB_MISC)));

    /**
     * Gọi trong constructor mod để gắn vào event bus.
     */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
