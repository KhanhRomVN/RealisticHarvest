package com.khanhromvn.realisticharvest;

import com.khanhromvn.realisticharvest.soil.SoilAttribute;
import com.khanhromvn.realisticharvest.soil.SoilData;
import com.khanhromvn.realisticharvest.config.RHConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

/**
 * Entry point Realistic Harvest.
 * Hệ thống chính: Soil capability, Items/Blocks ban đầu (sensors, hoe, cultivated soil).
 * TODO:
 *  - Tách registry sang package init/ (DONE)
 *  - Thêm ForgeConfigSpec (DONE skeleton)
 *  - Soil tick scheduler & attach capability (chunk/grid)
 *  - Data loaders (crop profiles, fertilizer profiles)
 */
@Mod(RealisticHarvest.MOD_ID)
public class RealisticHarvest {

    public static final String MOD_ID = "realisticharvest";
    private static final Logger LOGGER = LogManager.getLogger();

    // Registry chuyển sang init package (ModItems, ModBlocks).
    // TODO: Thêm ModFertilizers, ModIrrigationBlocks, ModTools khi mở rộng.

    // Soil capability reference
    public static Capability<SoilData> SOIL_DATA_CAPABILITY;

    public RealisticHarvest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        com.khanhromvn.realisticharvest.init.ModItems.register(modBus);
        com.khanhromvn.realisticharvest.init.ModBlocks.register(modBus);

        // Đăng ký config specs
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RHConfig.SERVER_SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RHConfig.CLIENT_SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::enqueueIMC);
        modBus.addListener(this::processIMC);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[RealisticHarvest] Common setup - registering capabilities & systems");
        event.enqueueWork(() ->
            CapabilityManager.INSTANCE.register(SoilData.class, new net.minecraftforge.common.capabilities.Capability.IStorage<SoilData>() {
                @Override
                public net.minecraft.nbt.INBT writeNBT(Capability<SoilData> capability, SoilData instance, net.minecraft.util.Direction side) {
                    return instance.serialize();
                }

                @Override
                public void readNBT(Capability<SoilData> capability, SoilData instance, net.minecraft.util.Direction side, net.minecraft.nbt.INBT nbt) {
                    instance.deserialize(nbt);
                }
            }, SoilData::new)
        );
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("[RealisticHarvest] Client setup - future: overlays hiển thị thông tin đất & cây");
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo(MOD_ID, "bootstrap", () -> {
            LOGGER.info("[RealisticHarvest] IMC enqueue bootstrap");
            return "bootstrap";
        });
    }

    private void processIMC(final InterModProcessEvent event) {
        LOGGER.info("[RealisticHarvest] Processing IMC messages: {}",
                event.getIMCStream().map(m -> m.getMessageSupplier().get()).collect(Collectors.toList()));
    }

    /**
     * Helper stub: lấy SoilData theo context (sẽ triển khai sau khi quyết định attach kiểu nào).
     */
    public static LazyOptional<SoilData> getSoilData(/* world, pos, chunk, etc */) {
        return LazyOptional.empty();
    }

    /**
     * DEBUG METHOD tạm thời: in thông tin soil attribute mapping.
     */
    public static void debugSoilMapping() {
        float phExample = 6.8f;
        float normalized = SoilAttribute.normalizePh(phExample);
        float back = SoilAttribute.denormalizePh(normalized);
        LOGGER.debug("[RealisticHarvest] pH mapping example real={} normalized={} back={}", phExample, normalized, back);
    }
}
