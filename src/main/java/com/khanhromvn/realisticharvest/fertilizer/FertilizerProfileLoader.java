package com.khanhromvn.realisticharvest.fertilizer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.khanhromvn.realisticharvest.RealisticHarvest;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * FertilizerProfileLoader:
 * Tải JSON fertilizer profiles từ datapack path:
 *   data/realisticharvest/fertilizers/*.json
 *
 * JSON mẫu:
 * {
 *   "id": "realisticharvest:nitrogen_mix_fertilizer",
 *   "fertility_boost": 0.30,
 *   "organic_boost": 0.05,
 *   "decay_rate": 0.0025,
 *   "ph_shift": 0.0
 * }
 *
 * Sử dụng:
 *   FertilizerProfileLoader.getProfile(new ResourceLocation("realisticharvest", "nitrogen_mix_fertilizer"));
 */
@Mod.EventBusSubscriber(modid = RealisticHarvest.MOD_ID)
public class FertilizerProfileLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<ResourceLocation, FertilizerProfile> PROFILE_MAP = new HashMap<>();

    public static FertilizerProfile getProfile(ResourceLocation id) {
        return PROFILE_MAP.get(id);
    }

    public static Map<ResourceLocation, FertilizerProfile> getAll() {
        return Collections.unmodifiableMap(PROFILE_MAP);
    }

    private static void reload(IResourceManager resourceManager) {
        PROFILE_MAP.clear();
        String folder = "fertilizers";
        int loaded = 0;

        for (ResourceLocation rl : resourceManager.listResources(folder, path -> path.endsWith(".json"))) {
            if (!RealisticHarvest.MOD_ID.equals(rl.getNamespace())) continue;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resourceManager.getResource(rl).getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    LOGGER.warn("[FertilizerProfileLoader] Empty JSON: {}", rl);
                    continue;
                }
                ResourceLocation id;
                if (root.has("id")) {
                    id = new ResourceLocation(root.get("id").getAsString());
                } else {
                    String p = rl.getPath(); // fertilizers/compost_fertilizer.json
                    String name = p.substring(p.lastIndexOf('/') + 1, p.length() - ".json".length());
                    id = new ResourceLocation(RealisticHarvest.MOD_ID, name);
                }
                FertilizerProfile profile = FertilizerProfile.fromJson(id, root);
                PROFILE_MAP.put(id, profile);
                loaded++;
            } catch (Exception e) {
                LOGGER.error("[FertilizerProfileLoader] Failed to parse {} : {}", rl, e.getMessage());
            }
        }
        LOGGER.info("[FertilizerProfileLoader] Loaded {} fertilizer profiles", loaded);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(resourceManager -> {
            LOGGER.info("[FertilizerProfileLoader] Resource reload triggered.");
            reload(resourceManager);
        });
    }
}
