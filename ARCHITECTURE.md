# Realistic Harvest - Kiến Trúc & Thiết Kế Hệ Thống

## Mục tiêu
Tạo hệ thống nông nghiệp thực tế mở rộng cho Minecraft:
- Đất với nhiều thuộc tính động: độ ẩm, pH, độ phì (fertility / nutrient), kết cấu (texture), độ tơi xốp (aeration), chất hữu cơ (organic matter)
- Ảnh hưởng biome & thời tiết vào đất và tăng trưởng cây
- Hệ thống nước & tưới (irrigation) ảnh hưởng độ ẩm
- Phân bón nhiều loại (hữu cơ, N-P-K chuyên dụng, điều chỉnh pH) cùng cơ chế suy giảm theo thời gian
- Cơ chế thâm canh & profile riêng cho từng loại cây trồng (crop cultivation profile)
- Bộ công cụ & cảm biến để đo các chỉ số đất / cây
- Data-driven: cho phép thêm cây / đất / phân bón qua JSON & Datapack
- Mở rộng thuật toán tăng trưởng cây trồng dựa trên stress (thiếu ẩm, sai pH, thiếu dưỡng chất)

---

## Tổng Quan Module

| Module | Nhiệm vụ chính | Loại triển khai |
|--------|----------------|-----------------|
| Soil System | Lưu & cập nhật thuộc tính đất | Capability + Chunk/Block Data |
| Weather & Biome Influence | Tác động nhiệt độ, độ ẩm, mưa vào soil & growth | Tick + Event (Weather, Biome read) |
| Irrigation | Khối / mạng nước bổ sung độ ẩm đất | Blocks + Scheduled moisture update |
| Fertilizer System | Items phân bón, công thức, áp dụng & decay | Registry Items + Effect Applier |
| Crop Profiles | Định nghĩa nhu cầu & ngưỡng tối ưu | JSON Datapack + Runtime cache |
| Growth Logic | Tính tốc độ tăng trưởng / stress | Event (Crop tick) / Inject tick handler |
| Tools & Sensors | Cảm biến pH, ẩm, dụng cụ cải tạo đất | Items + Right-click / HUD overlay |
| Config | Server/Client config & sync | ForgeConfigSpec |
| Data Layer | Load / validate JSON (soil presets, crop profiles, fertilizer types) | Resource reload listener |
| Debug & Overlay | Hiển thị thông tin soil/crop tại vị trí | Client overlay + key toggle |

---

## Soil System Chi Tiết

### Thuộc tính
```java
enum SoilAttribute {
  MOISTURE, PH, FERTILITY, TEXTURE, AERATION, ORGANIC_MATTER
}
```

- Giá trị phạm vi chuẩn hóa [0..1] (riêng pH: map = (pH_real - 3) / (10 - 3))
- TEXTURE: encode dạng continuum (0 = cát, 0.5 = thịt, 1 = sét) -> dùng để tính retention & drainage
- ORGANIC_MATTER tác động gián tiếp tới giữ ẩm & fertility bonus

### Lưu trữ
- Giai đoạn đầu: Capability gắn Block (đồng bộ đơn giản)
- Tối ưu sau: Chunk-level grid (ví dụ 16x16 caching) giảm số capability
- Serial hóa NBT: mỗi attribute -> float
- API:
  - get(attr), set(attr)
  - applyMoistureDelta(biomeFactor, rainfallFactor, irrigationFactor)
  - applyFertilizer(FertilizerProfile fp)
  - tickDegrade() (giảm fertility, moisture evaporate, fertilizer decay)

### Cập nhật Tick
- Server tick: mỗi X ticks (config) duyệt soil cells trong phạm vi người chơi / vùng hoạt động
- Moisture giảm theo:
  ```
  moisture -= evaporationRate(temperature, aeration, texture) * dt
  moisture += rainfallContribution(if raining) + irrigationContribution
  ```
- Fertility giảm dần theo hệ số decay và ORGANIC_MATTER hỗ trợ giữ lại

---

## Weather & Biome Influence

- Biome: lấy temperature, downfall (humidity proxy)
- Weather:
  - Mưa: tăng moisture tới giới hạn phụ thuộc texture (cát giữ ít)
  - Nắng gắt / hot biome: tăng evaporation
- Seasonal / daily cycle (tùy chọn tương lai)

---

## Irrigation System

- Block "Irrigation Channel", "Water Emitter"
- Cơ chế:
  - Quét network kênh nước -> phân phối moisture bonus đến soil cells trong bán kính
- Data:
  - Water Pressure (0..1) -> moisture delta per tick
- Sau mở rộng: nước bẩn ảnh hưởng pH / fertility?

---

## Fertilizer System

### Phân loại
| Type | Ảnh hưởng | Suy giảm |
|------|-----------|---------|
| Organic Compost | +OrganicMatter +Fertility nhỏ, kéo dài | Chậm |
| Nitrogen Mix | +Fertility mạnh (N) | Nhanh |
| Phosphate | +Fertility trung bình, boost root dev | TB |
| Potassium | +Stress resistance (giảm penalty khô) | TB |
| pH Up / Down | Điều chỉnh pH về khoảng tối ưu | Nhanh (buffer ngắn) |

### JSON Fertilizer Profile
```json
{
  "type": "nitrogen_mix",
  "fertility_boost": 0.35,
  "organic_matter_boost": 0.05,
  "ph_delta": 0.0,
  "decay_rate": 0.02,
  "tags": ["nitrogen"]
}
```

### Áp dụng
- Right-click soil với item -> cập nhật SoilData
- Lưu timestamp -> tính decay theo tick

---

## Crop Cultivation Profiles

### JSON Định nghĩa
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

### Tính toán tốc độ tăng trưởng
```
base = vanillaGrowthRate
factor(attr) = if in optimal -> 1
             else -> 1 - penalty * distanceToRange
growthRate = base * Π(factor(attr)) * (1 + intensiveBonus)
```

### Stress / Wither
- Nếu moisture < criticalMin -> slowdown mạnh, có thể gây chết (tương lai)
- Sai pH kéo dài -> fertility giảm nhanh hơn

---

## Tools & Sensors

| Item ID | Chức năng |
|---------|-----------|
| soil_ph_sensor | Hiển thị pH & khuyến nghị điều chỉnh |
| moisture_meter | Hiển thị độ ẩm & trạng thái khô / tối ưu |
| basic_farm_hoe | Tăng aeration + thay đổi texture hướng tới "loam" |
| fertilizer_applicator (tương lai) | Phân phối fertilizer đều cho vùng |

- Client overlay: hiển thị panel nhỏ khi cầm sensor (Forge client event + RenderGameOverlayEvent)

---

## Data Layer & Reload

- Resource reload listener: parse folder `data/realisticharvest/crops/*.json`, `fertilizers/*.json`, `soil_presets/*.json`
- Validation: log cảnh báo nếu range invalid / chồng lấn
- Cache: Map<ResourceLocation, CropProfile>

---

## Config

ForgeConfigSpec (server):
- tick_interval_soil_update
- evaporation_base
- rainfall_moisture_gain
- fertilizer_decay_multiplier
- debug_overlay_enabled_default

Client:
- overlay_position / scale
- color_theme

Sync: server values gửi tới client khi join.

---

## Event Flow (Pseudo)

1. Server Tick:
   - SoilUpdateScheduler.run()
     - Moisture evaporate & apply rainfall/irrigation
     - Fertilizer decay
2. CropGrowthEvent (tiêm qua mixin / hoặc lắng sự kiện random tick của block crop):
   - Fetch CropProfile
   - Compute growthMultiplier
   - Điều chỉnh tuổi cây (growth stage) hoặc ngăn tick nếu stress cao
3. PlayerInteractEvent:
   - Dùng sensor -> đọc SoilData tại block
   - Dùng fertilizer item -> apply profile & set decay data

---

## Registry Kế Hoạch

| Registry | Nội dung |
|----------|----------|
| Items | Sensors, hoes, fertilizer items |
| Blocks | Cultivated soil, irrigation channel, water emitter |
| TileEntities (tương lai) | Irrigation controller / soil analyzer |
| Custom Data | CropProfile registry (runtime map) |

---

## Lộ Trình Phát Triển (Roadmap)

1. Skeleton (DONE: main class, basic items, soil capability stub)
2. Soil capability hoàn chỉnh + attach đúng đối tượng (Chunk/Block)
3. Fertilizer item + áp dụng & decay
4. Moisture logic (evaporation + rainfall)
5. CropProfile JSON loader & tích hợp tăng trưởng lúa mì
6. Sensor overlay hiển thị moisture/pH/fertility
7. Irrigation block & moisture bonus
8. Texture + model placeholders
9. Localization (en_us.json)
10. Mở rộng nhiều crop vanilla
11. Balancing & config
12. Debug HUD / command `/rh soilinfo`
13. Tối ưu lưu trữ (chunk grid) & caching

---

## Package Định Hướng (Tạo dần)

```
com.khanhromvn.realisticharvest
├── RealisticHarvest.java
├── soil/
│   ├── SoilAttribute.java
│   ├── SoilData.java
│   ├── SoilCapability.java
│   └── SoilTickHandler.java
├── fertilizer/
│   ├── FertilizerType.java
│   ├── FertilizerProfile.java
│   ├── FertilizerRegistry.java
│   └── FertilizerApplier.java
├── crop/
│   ├── CropProfile.java
│   ├── CropProfileLoader.java
│   ├── CropGrowthLogic.java
│   └── CropStressCalculator.java
├── irrigation/
│   ├── IrrigationBlock.java
│   ├── IrrigationNetwork.java
│   └── IrrigationTickHandler.java
├── tool/
│   ├── SoilPhSensorItem.java
│   ├── MoistureMeterItem.java
│   ├── FarmHoeItem.java
│   └── OverlayRenderer.java
├── config/
│   ├── RHConfig.java
│   └── ConfigSync.java
├── data/
│   ├── DataReloadListener.java
│   ├── crop/
│   └── fertilizer/
├── debug/
│   ├── SoilDebugOverlay.java
│   └── RHCommands.java
└── util/
    ├── Constants.java
    └── MathUtil.java
```

---

## Ghi chú Kỹ thuật

- 1.16.5 Forge: capability attach sử dụng AttachCapabilitiesEvent<Chunk / TileEntity>. Soil gắn cho block dirt có thể cần tile entity custom -> tạm thời chunk-level aggregate cho hiệu suất.
- Random tick crops: Hook vào `net.minecraft.block.CropsBlock#randomTick`.
- Performance: Giới hạn bán kính soil update theo người chơi, batching mỗi tick.
- Serialization: Chỉ lưu attributes cần thiết + fertilizer decay timer.
- Future: Multi-layer soil horizons (topsoil vs subsoil), microbial activity (ảnh hưởng fertility regen).

---

## Next Steps Ngay Sau Tài Liệu
- Implement phân tách các lớp soil (refactor từ lớp lồng trong RealisticHarvest).
- Thiết lập registry items vào class riêng (ItemInit).
- ForgeConfigSpec skeleton.
- Loader JSON cho CropProfile (resource reload listener).
