# Minecraft 1.12.2 Mod Project Structure

```
<mod_name>/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── <mod_name>/
│   │   │               ├── ModMain.java
│   │   │               ├── proxy/
│   │   │               │   ├── CommonProxy.java
│   │   │               │   ├── ClientProxy.java
│   │   │               │   └── ServerProxy.java
│   │   │               ├── config/
│   │   │               │   └── ModConfig.java
│   │   │               ├── handlers/
│   │   │               │   ├── RegistryHandler.java
│   │   │               │   ├── EventHandler.java
│   │   │               │   └── GuiHandler.java
│   │   │               ├── init/
│   │   │               │   ├── ModItems.java
│   │   │               │   ├── ModBlocks.java
│   │   │               │   ├── ModEntities.java
│   │   │               │   └── ModRecipes.java
│   │   │               ├── items/
│   │   │               │   └── ItemBase.java
│   │   │               ├── blocks/
│   │   │               │   └── BlockBase.java
│   │   │               ├── world/
│   │   │               │   ├── ModWorldGen.java
│   │   │               │   └── gen/
│   │   │               │       └── OreGen.java
│   │   │               ├── network/
│   │   │               │   └── PacketHandler.java
│   │   │               └── util/
│   │   │                   └── Reference.java
│   │   └── resources/
│   │       ├── assets/
│   │       │   └── <mod_name>/
│   │       │       ├── lang/
│   │       │       │   └── en_us.lang
│   │       │       ├── models/
│   │       │       │   ├── item/
│   │       │       │   └── block/
│   │       │       ├── textures/
│   │       │       │   ├── items/
│   │       │       │   ├── blocks/
│   │       │       │   └── gui/
│   │       │       ├── blockstates/
│   │       │       └── sounds.json
│   │       ├── mcmod.info
│   │       └── pack.mcmeta
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── <mod_name>/
│                       └── tests/
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle
├── gradlew
├── gradlew.bat
└── LICENSE
```

## Mô tả chi tiết các thư mục và file

### **Thư mục gốc**
- **build.gradle** - File cấu hình Gradle build system, định nghĩa dependencies và build process
- **gradlew / gradlew.bat** - Scripts để chạy Gradle wrapper (Unix/Windows)
- **LICENSE** - File license của mod

### **Thư mục src/main/java**
Chứa toàn bộ mã nguồn Java của mod:

- **com/example/<mod_name>/ModMain.java** - Class chính của mod, điểm khởi đầu
- **proxy/** - Các class proxy để xử lý client/server specific code
  - *CommonProxy.java* - Proxy chung
  - *ClientProxy.java* - Xử lý client-side code
  - *ServerProxy.java* - Xử lý server-side code
- **config/** - Quản lý cấu hình mod
  - *ModConfig.java* - Xử lý config file
- **handlers/** - Các event handler và registry
  - *RegistryHandler.java* - Đăng ký items, blocks, entities
  - *EventHandler.java* - Xử lý game events
  - *GuiHandler.java* - Quản lý GUI
- **init/** - Khởi tạo và đăng ký game objects
  - *ModItems.java* - Đăng ký và khởi tạo items
  - *ModBlocks.java* - Đăng ký và khởi tạo blocks
  - *ModEntities.java* - Đăng ký entities
  - *ModRecipes.java* - Đăng ký crafting recipes
- **items/** - Custom item classes
  - *ItemBase.java* - Base class cho custom items
- **blocks/** - Custom block classes
  - *BlockBase.java* - Base class cho custom blocks
- **world/** - World generation và structures
  - *ModWorldGen.java* - Quản lý world generation
  - *gen/OreGen.java* - Ore generation logic
- **network/** - Network communication
  - *PacketHandler.java* - Xử lý network packets
- **util/** - Utility classes
  - *Reference.java* - Chứa constants và references

### **Thư mục src/main/resources**
Chứa tài nguyên của mod:

- **assets/<mod_name>/** - Tất cả assets của mod
  - **lang/** - Localization files
    - *en_us.lang* - English translations
  - **models/** - JSON models
    - **item/** - Item models
    - **block/** - Block models
  - **textures/** - Texture files
    - **items/** - Item textures
    - **blocks/** - Block textures
    - **gui/** - GUI textures
  - **blockstates/** - Block state definitions
  - *sounds.json* - Sound definitions
- **mcmod.info** - Metadata của mod (tên, mô tả, version...)
- **pack.mcmeta** - Resource pack metadata

### **Thư mục src/test**
Chứa unit tests cho mod

### **Thư mục gradle/**
Chứa Gradle wrapper files để đảm bảo build consistency