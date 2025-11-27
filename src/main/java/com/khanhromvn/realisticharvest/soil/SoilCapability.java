package com.khanhromvn.realisticharvest.soil;

import com.khanhromvn.realisticharvest.RealisticHarvest;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import com.khanhromvn.realisticharvest.init.ModBlocks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * SoilCapability quản lý SoilData ở cấp độ CHUNK để giảm số lượng capability
 * per-block.
 * Lưu map<BlockPos, SoilData> cho các vị trí đất được truy cập / thay đổi.
 *
 * Thiết kế:
 * - Khi người chơi tương tác (bón phân / đo) mới khởi tạo SoilData tại pos.
 * - Scheduler tick sẽ duyệt các entry đã khởi tạo.
 * - Tối ưu sau: giới hạn số vị trí / aging để xóa entry cũ.
 * - Biome temperature + raining ảnh hưởng moisture; irrigation blocks cấp bonus
 * ẩm.
 */
@Mod.EventBusSubscriber(modid = RealisticHarvest.MOD_ID)
public final class SoilCapability {

    public static final ResourceLocation KEY = new ResourceLocation(RealisticHarvest.MOD_ID, "soil_chunk");
    @CapabilityInject(ChunkSoilStore.class)
    public static Capability<ChunkSoilStore> CHUNK_SOIL_CAP = null;

    /**
     * Store capability: map các vị trí -> SoilData
     */
    public static class ChunkSoilStore implements INBTSerializable<CompoundNBT> {
        private final Map<BlockPos, SoilData> soilMap = new HashMap<>();

        public SoilData getOrCreate(BlockPos pos) {
            return soilMap.computeIfAbsent(pos, p -> new SoilData());
        }

        public SoilData get(BlockPos pos) {
            return soilMap.get(pos);
        }

        public boolean has(BlockPos pos) {
            return soilMap.containsKey(pos);
        }

        public void tickScheduled(World world) {
            boolean raining = world.isRaining();
            for (Map.Entry<BlockPos, SoilData> e : soilMap.entrySet()) {
                BlockPos pos = e.getKey();

                // Biome temperature -> normalized (approx): vanilla temp ~ -0.5..2.0 (1.16)
                float biomeTemp = world.getBiome(pos).getTemperature(pos);
                // Clamp & normalize to 0..1 (assume range -0.5 .. 2.0)
                float tempNorm = (biomeTemp + 0.5f) / 2.5f;
                if (tempNorm < 0f)
                    tempNorm = 0f;
                if (tempNorm > 1f)
                    tempNorm = 1f;

                // Simple irrigation scan:
                // - WATER_EMITTER within radius 4 -> bonus 0.004
                // - IRRIGATION_CHANNEL within radius 3 -> bonus 0.002
                // Priority: emitter overrides channel.
                float irrigationBonus = 0f;
                final int emitterRadius = 4;
                boolean foundEmitter = false;
                BlockPos.Mutable mutable = new BlockPos.Mutable();
                for (int dx = -emitterRadius; dx <= emitterRadius && !foundEmitter; dx++) {
                    for (int dz = -emitterRadius; dz <= emitterRadius && !foundEmitter; dz++) {
                        mutable.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                        if (world.getBlockState(mutable).getBlock() == ModBlocks.WATER_EMITTER.get()) {
                            irrigationBonus = 0.004f;
                            foundEmitter = true;
                        }
                    }
                }
                if (!foundEmitter) {
                    final int channelRadius = 3;
                    for (int dx = -channelRadius; dx <= channelRadius && irrigationBonus == 0f; dx++) {
                        for (int dz = -channelRadius; dz <= channelRadius && irrigationBonus == 0f; dz++) {
                            mutable.set(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
                            if (world.getBlockState(mutable).getBlock() == ModBlocks.IRRIGATION_CHANNEL.get()) {
                                irrigationBonus = 0.002f;
                            }
                        }
                    }
                }

                e.getValue().scheduledUpdate(tempNorm, raining, irrigationBonus);
            }
        }

        @Override
        public CompoundNBT serializeNBT() {
            CompoundNBT root = new CompoundNBT();
            int idx = 0;
            for (Map.Entry<BlockPos, SoilData> entry : soilMap.entrySet()) {
                CompoundNBT soilTag = (CompoundNBT) entry.getValue().serialize();
                soilTag.putInt("x", entry.getKey().getX());
                soilTag.putInt("y", entry.getKey().getY());
                soilTag.putInt("z", entry.getKey().getZ());
                root.put("soil_" + idx, soilTag);
                idx++;
            }
            root.putInt("count", idx);
            return root;
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            soilMap.clear();
            int count = nbt.getInt("count");
            for (int i = 0; i < count; i++) {
                String key = "soil_" + i;
                if (nbt.contains(key)) {
                    CompoundNBT soilTag = nbt.getCompound(key);
                    BlockPos pos = new BlockPos(soilTag.getInt("x"), soilTag.getInt("y"), soilTag.getInt("z"));
                    SoilData data = new SoilData();
                    data.deserialize(soilTag);
                    soilMap.put(pos, data);
                }
            }
        }
    }

    /**
     * Provider gắn vào chunk.
     */
    public static class Provider implements ICapabilityProvider, INBTSerializable<CompoundNBT> {
        private final ChunkSoilStore store = new ChunkSoilStore();
        private final LazyOptional<ChunkSoilStore> opt = LazyOptional.of(() -> store);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == CHUNK_SOIL_CAP) {
                return opt.cast();
            }
            return LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return store.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            store.deserializeNBT(nbt);
        }
    }

    @SubscribeEvent
    public static void attachChunk(AttachCapabilitiesEvent<Chunk> event) {
        if (!event.getObject().getLevel().isClientSide) {
            event.addCapability(KEY, new Provider());
        }
    }

    /**
     * Tick scheduler: mỗi server tick kiểm tra interval; cập nhật soil.
     * (Tạm thời: update mỗi 40 ticks; sẽ lấy từ config
     * RHConfig.SERVER.soilUpdateInterval)
     */
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        tickCounter++;
        int interval = com.khanhromvn.realisticharvest.config.RHConfig.SERVER.soilUpdateInterval.get();
        if (tickCounter < interval)
            return;
        tickCounter = 0;

        // Temporary simplified approach - skip chunk iteration for now
        // TODO: Implement proper chunk iteration for current Minecraft version
        // This will be implemented once the basic compilation issues are resolved
    }

    /**
     * Helper public API để lấy hoặc tạo SoilData tại vị trí.
     */
    public static SoilData getOrCreate(World world, BlockPos pos) {
        Chunk chunk = (Chunk) world.getChunk(pos);
        LazyOptional<ChunkSoilStore> capability = chunk.getCapability(CHUNK_SOIL_CAP);
        if (capability.isPresent()) {
            return capability.map(store -> store.getOrCreate(pos)).orElse(new SoilData());
        }
        return new SoilData();
    }

    public static SoilData getIfExists(World world, BlockPos pos) {
        Chunk chunk = (Chunk) world.getChunk(pos);
        LazyOptional<ChunkSoilStore> capability = chunk.getCapability(CHUNK_SOIL_CAP);
        if (capability.isPresent()) {
            SoilData data = capability.orElse(new ChunkSoilStore()).get(pos);
            return data != null ? data : new SoilData();
        }
        return new SoilData();
    }
}
