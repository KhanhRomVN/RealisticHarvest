# Realistic Harvest

Realistic Harvest là một mod mở rộng hệ thống nông nghiệp Minecraft với nhiều cơ chế **mô phỏng thực tế**: đất có thuộc tính động, ảnh hưởng biome & thời tiết, hệ thống tưới, phân bón đa dạng, công cụ cảm biến và profile cây trồng tùy biến qua JSON.

## Mục tiêu Chính
- Soil System: độ ẩm, pH, độ phì (fertility), kết cấu (texture), độ tơi xốp (aeration), chất hữu cơ (organic matter).
- Ảnh hưởng Biome & Weather: mưa, nhiệt độ, ánh nắng tác động bay hơi & giữ ẩm.
- Irrigation: khối & mạng tưới cung cấp bonus độ ẩm.
- Fertilizer: nhiều loại phân (compost, nitrogen, potassium, pH up/down) có cơ chế decay.
- Crop Cultivation Profiles: yêu cầu tối ưu từng cây, stress penalty, intensive bonus.
- Tools & Sensors: cảm biến pH, đo độ ẩm, hoe cải tạo đất; overlay debug.
- Data-driven: thêm crop/fertilizer/soil preset bằng datapack JSON.
- Config: server/client config (ForgeConfigSpec).
- Performance & Optimization: tiến tới chunk-level caching & scheduler tick.

## Trạng Thái Hiện Tại
| Thành phần | Trạng thái |
|------------|-----------|
| Main Mod Class | Đã tách `RealisticHarvest` + registry init |
| Items cơ bản | Sensors + hoe (placeholder) |
| Blocks | `cultivated_soil` (placeholder) |
| Soil Attribute Enum | Hoàn thành |
| SoilData + FertilizerEffect cơ bản | Hoàn thành (logic moisture & decay stub) |
| Config Skeleton | Hoàn thành (server & client) |
| Kiến trúc & thiết kế | ARCHITECTURE.md hoàn thành |
| Data Loader | Chưa triển khai |
| Growth Logic Hook | Chưa |
| Irrigation System | Chưa |
| Overlay / HUD | Chưa |
| Localization | Chưa |
| Command / Debug | Chưa |

## Cấu Trúc Thư Mục (rút gọn)
```
src/main/java/com/realisticharvest/
  RealisticHarvest.java
  config/RHConfig.java
  init/ModItems.java
  init/ModBlocks.java
  soil/SoilAttribute.java
  soil/SoilData.java
ARCHITECTURE.md
gradle.properties
```

Xem thêm: `ARCHITECTURE.md` để biết chi tiết module & roadmap.

## Hệ Thống Soil (Tóm tắt)
- Giá trị chuẩn hóa (0..1) ngoại trừ pH được map từ real (3..10).
- `SoilData` quản lý moisture evaporation, rainfall absorption, fertilizer decay.
- Sau này: scheduler chạy mỗi `soilUpdateInterval` (config) thay vì mỗi tick.

## Fertilizer (Stub)
Các profile ban đầu trong `SoilData.FertilizerEffect` (sẽ thay bằng JSON):
- Compost: tăng organic matter mạnh, decay chậm.
- Nitrogen mix: tăng fertility nhanh, decay nhanh.
- Potassium: cân bằng giữa fertility & hỗ trợ stress.

## Config
File `RHConfig` định nghĩa:
- Server: `soilUpdateInterval`, `evaporationBase`, `rainfallMoistureGain`, `fertilizerDecayMultiplier`, `debugOverlayDefault`
- Client: vị trí & scale overlay, lựa chọn hiển thị pH/moisture/fertility.

## Roadmap (Ngắn hạn)
1. Soil capability attach model (chunk-level vs block-level).
2. Scheduler & evaporation + rainfall mô phỏng chuẩn.
3. Fertilizer item & apply logic + NBT timestamp.
4. CropProfile JSON loader (wheat pilot).
5. Hook tăng trưởng crop (randomTick override / event).
6. Client overlay khi cầm sensor.
7. Irrigation channel block + water emitter cơ bản.
8. Localization `en_us.json`.
9. Debug command `/rh soilinfo`.
10. Tối ưu hóa & balancing.

## Datapack Định Hình (Dự kiến)
Thêm vào:
```
data/realisticharvest/crops/*.json
data/realisticharvest/fertilizers/*.json
data/realisticharvest/soil_presets/*.json
```

Ví dụ CropProfile JSON:
```json
{
  "crop": "minecraft:wheat",
  "optimal": {
    "moisture": { "min": 0.45, "max": 0.75 },
    "ph": { "min": 6.0, "max": 7.0 },
    "fertility": { "min": 0.50, "max": 0.90 }
  },
  "stress_penalty": {
    "moisture": 0.4,
    "ph": 0.3,
    "fertility": 0.5
  },
  "growth_multiplier": 1.0,
  "intensive_bonus": {
    "aeration": { "threshold": 0.6, "bonus": 0.05 },
    "organic_matter": { "threshold": 0.5, "bonus": 0.07 }
  }
}
```

## Build & Phát Triển
Yêu cầu:
- JDK phù hợp Forge 1.16.5 (Java 8 theo thiết lập trong build script hiện tại – xem xét nâng nếu cần).
- Gradle wrapper đi kèm.

Chạy client dev:
```
./gradlew runClient
```

Sinh data (sau khi thêm datagen):
```
./gradlew runData
```

## Đóng Góp
- Mở issue đề xuất thêm loại cây / fertilizer mới.
- Cần kiểm tra hiệu suất khi soil grid mở rộng.
- Tài liệu bên trong code giữ phong cách chú thích ngắn gọn + TODO.

## Giấy Phép
All Rights Reserved (tạm thời) – xem xét chuyển sang giấy phép mở nếu cộng đồng tham gia.

## Ghi Chú Khác
- MDK legacy `ExampleMod` giữ lại như tài liệu tham khảo không còn @Mod.
- Các phần chưa hoàn thiện được đánh dấu TODO trong source.

---
Phát triển hướng tới mô phỏng nông nghiệp cân bằng giữa gameplay & chiều sâu kỹ thuật.
