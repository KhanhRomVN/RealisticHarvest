package com.khanhromvn.realisticharvest.crop;

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
 * CropProfileLoader: tải JSON profiles từ datapack path:
 * data/realisticharvest/crops/*.json
 *
 * Cơ chế:
 *  - Đăng ký làm reload listener qua AddReloadListenerEvent.
 *  - Khi reload: quét tất cả resources có prefix folder "crops".
 *  - Parse JSON -> CropProfile -> cache vào MAP.
 *
 * Tích hợp sử dụng:
 *  CropProfileLoader.getProfile(new ResourceLocation("minecraft", "wheat"));
 *
 * TODO:
 *  - Validation chi tiết (range logic)
 *  - Log cảnh báo khi thiếu field
 *  - Thêm fertilizer profile loader tương tự
 */
@Mod.EventBusSubscriber(modid = RealisticHarvest.MOD_ID)
public class CropProfileLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<ResourceLocation, CropProfile> PROFILE_MAP = new HashMap<>();

    public static CropProfile getProfile(ResourceLocation id) {
        return PROFILE_MAP.get(id);
    }

    public static Map<ResourceLocation, CropProfile> getAll() {
        return Collections.unmodifiableMap(PROFILE_MAP);
    }

    /**
     * Reload logic thực hiện parse.
     */
    private static void reload(IResourceManager resourceManager) {
        PROFILE_MAP.clear();
        String folder = "crops";

        int loaded = 0;
        for (ResourceLocation rl : resourceManager.listResources(folder, path -> path.endsWith(".json"))) {
            // Expect path: realisticharvest:crops/wheat.json
            if (!"realisticharvest".equals(rl.getNamespace())) continue;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.getResource(rl).getInputStream(), StandardCharsets.UTF_8))) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    LOGGER.warn("[CropProfileLoader] Empty JSON: {}", rl);
                    continue;
                }
                // Field 'crop' nếu có dùng làm override id; nếu không lấy từ filename
                ResourceLocation cropId;
                if (root.has("crop")) {
                    cropId = new ResourceLocation(root.get("crop").getAsString());
                } else {
                    // filename = crops/<name>.json
                    String p = rl.getPath(); // crops/wheat.json
                    String name = p.substring(p.lastIndexOf('/') + 1, p.length() - ".json".length());
                    cropId = new ResourceLocation("minecraft", name); // fallback namespace
                }
                CropProfile profile = CropProfile.fromJson(cropId, root);
                PROFILE_MAP.put(cropId, profile);
                loaded++;
            } catch (Exception e) {
                LOGGER.error("[CropProfileLoader] Failed to parse {} : {}", rl, e.getMessage());
            }
        }
        LOGGER.info("[CropProfileLoader] Loaded {} crop profiles", loaded);
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(resourceManager -> {
            LOGGER.info("[CropProfileLoader] Resource reload triggered.");
            reload(resourceManager);
        });
    }
}
