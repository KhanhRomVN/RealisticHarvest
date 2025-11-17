package com.khanhromvn.realisticharvest.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge config cho Realistic Harvest.
 * Tách server & client:
 *  - Server: ảnh hưởng logic mô phỏng (tick interval, hệ số bay hơi, decay fertilizer...)
 *  - Client: overlay hiển thị & tùy chọn người chơi.
 */
public final class RHConfig {

    // SERVER
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    // CLIENT
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder sb = new ForgeConfigSpec.Builder();
        SERVER = new Server(sb);
        SERVER_SPEC = sb.build();

        ForgeConfigSpec.Builder cb = new ForgeConfigSpec.Builder();
        CLIENT = new Client(cb);
        CLIENT_SPEC = cb.build();
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue soilUpdateInterval;
        public final ForgeConfigSpec.DoubleValue evaporationBase;
        public final ForgeConfigSpec.DoubleValue rainfallMoistureGain;
        public final ForgeConfigSpec.DoubleValue fertilizerDecayMultiplier;
        public final ForgeConfigSpec.BooleanValue debugOverlayDefault;

        Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Realistic Harvest - Server Config").push("server");

            soilUpdateInterval = builder
                    .comment("Số tick giữa mỗi lần cập nhật soil scheduler (giảm tải).")
                    .defineInRange("soilUpdateInterval", 40, 5, 400);

            evaporationBase = builder
                    .comment("Hệ số bay hơi nền (điều chỉnh tốc độ mất ẩm).")
                    .defineInRange("evaporationBase", 0.0008D, 0.0001D, 0.01D);

            rainfallMoistureGain = builder
                    .comment("Delta độ ẩm cơ bản khi mưa (sẽ còn scale theo texture).")
                    .defineInRange("rainfallMoistureGain", 0.004D, 0.0005D, 0.05D);

            fertilizerDecayMultiplier = builder
                    .comment("Multiplier global cho tốc độ suy giảm phân bón.")
                    .defineInRange("fertilizerDecayMultiplier", 1.0D, 0.1D, 10.0D);

            debugOverlayDefault = builder
                    .comment("Bật debug overlay soil mặc định khi join (chỉ dành cho dev).")
                    .define("debugOverlayDefault", false);

            builder.pop();
        }
    }

    public static class Client {
        public final ForgeConfigSpec.IntValue overlayX;
        public final ForgeConfigSpec.IntValue overlayY;
        public final ForgeConfigSpec.DoubleValue overlayScale;
        public final ForgeConfigSpec.BooleanValue showPh;
        public final ForgeConfigSpec.BooleanValue showMoisture;
        public final ForgeConfigSpec.BooleanValue showFertility;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Realistic Harvest - Client Config").push("client");

            overlayX = builder
                    .comment("Vị trí overlay X.")
                    .defineInRange("overlayX", 5, 0, 5000);

            overlayY = builder
                    .comment("Vị trí overlay Y.")
                    .defineInRange("overlayY", 5, 0, 5000);

            overlayScale = builder
                    .comment("Tỷ lệ scale overlay.")
                    .defineInRange("overlayScale", 1.0D, 0.5D, 3.0D);

            showPh = builder
                    .comment("Hiển thị pH trên overlay.")
                    .define("showPh", true);

            showMoisture = builder
                    .comment("Hiển thị moisture trên overlay.")
                    .define("showMoisture", true);

            showFertility = builder
                    .comment("Hiển thị fertility trên overlay.")
                    .define("showFertility", true);

            builder.pop();
        }
    }

    private RHConfig() {}
}
