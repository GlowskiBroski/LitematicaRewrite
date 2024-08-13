package fi.dy.masa.litematica.world;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class WorldSchematic extends Level {
    protected static final ResourceKey<Level> REGISTRY_KEY = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Reference.MOD_ID, "schematic_world"));

    protected final Minecraft mc;
    protected final ChunkManagerSchematic chunkManagerSchematic;
    protected final Holder<Biome> biome;
    @Nullable
    protected final WorldRendererSchematic worldRenderer;
    private final TickRateManager tickManager;
    protected int nextEntityId;
    protected int entityCount;

    public WorldSchematic(WritableLevelData properties, @Nonnull RegistryAccess registryManager, Holder<DimensionType> dimension, Supplier<ProfilerFiller> supplier, @Nullable WorldRendererSchematic worldRenderer) {
        super(properties, REGISTRY_KEY, !registryManager.equals(RegistryAccess.EMPTY) ? registryManager : MC.level.registryAccess(), dimension, supplier, true, false, 0L, 0);

        this.mc = MC;
        if (this.mc == null || this.mc.level == null) {
            throw new RuntimeException("WorldSchematic invoked when MC or mc.world is null");
        }
        this.worldRenderer = worldRenderer;
        this.chunkManagerSchematic = new ChunkManagerSchematic(this);
        if (!registryManager.equals(RegistryAccess.EMPTY)) {
            this.biome = registryManager.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        } else {
            this.biome = this.mc.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        }
        this.tickManager = new TickRateManager();
    }

    public ChunkManagerSchematic getChunkProvider() {
        return this.chunkManagerSchematic;
    }

    @Override
    public ChunkManagerSchematic getChunkSource() {
        return this.chunkManagerSchematic;
    }

    @Override
    public TickRateManager tickRateManager() {
        return this.tickManager;
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(MapId id) {
        return null;
    }

    @Override
    public void setMapData(MapId id, MapItemSavedData state) {
    }

    @Override
    public MapId getFreeMapId() {
        return null;
    }

    @Override
    public LevelTickAccess<Block> getBlockTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public LevelTickAccess<Fluid> getFluidTicks() {
        return BlackholeTickAccess.emptyLevelList();
    }

    public int getRegularEntityCount() {
        return this.entityCount;
    }

    @Override
    public LevelChunk getChunkAt(BlockPos pos) {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public ChunkSchematic getChunk(int chunkX, int chunkZ) {
        return this.chunkManagerSchematic.getChunkForLighting(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean required) {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.biome;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
        if (pos.getY() < this.getMinBuildHeight() || pos.getY() >= this.getMaxBuildHeight()) {
            return false;
        } else {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, false) != null;
        }
    }

    @Override
    public boolean addFreshEntity(Entity entity) {
        int chunkX = Mth.floor(entity.getX() / 16.0D);
        int chunkZ = Mth.floor(entity.getZ() / 16.0D);

        if (!this.chunkManagerSchematic.hasChunk(chunkX, chunkZ)) {
            return false;
        } else {
            entity.setId(this.nextEntityId++);
            this.chunkManagerSchematic.getChunkForLighting(chunkX, chunkZ).addEntity(entity);
            ++this.entityCount;

            return true;
        }
    }

    public void unloadedEntities(int count) {
        this.entityCount -= count;
    }

    @Nullable
    @Override
    public Entity getEntity(int id) {
        // This shouldn't be used for anything in the mod, so just return null here
        return null;
    }

    @Override
    public List<? extends Player> players() {
        return ImmutableList.of();
    }

    @Override
    public long getGameTime() {
        return this.mc.level != null ? this.mc.level.getGameTime() : 0;
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.mc.level != null ? this.mc.level.getScoreboard() : null;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return this.mc.level != null ? this.mc.level.getRecipeManager() : null;
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        // This is not used in the mod
        return null;
    }

    @Override
    public List<Entity> getEntities(@Nullable final Entity except, final AABB box, Predicate<? super Entity> predicate) {
        final List<Entity> entities = new ArrayList<>();
        List<ChunkSchematic> chunks = this.getChunksWithinBox(box);

        for (ChunkSchematic chunk : chunks) {
            chunk.getEntityList().forEach((e) -> {
                if (e != except && box.intersects(e.getBoundingBox()) && predicate.test(e)) {
                    entities.add(e);
                }
            });
        }

        return entities;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> arg, AABB box, Predicate<? super T> predicate) {
        ArrayList<T> list = new ArrayList<>();

        //for (Entity e : this.getEntities(null, box, e -> true)) {
        for (Entity e : this.getEntities(MC.player, box, e -> true)) {
            T t = arg.tryCast(e);

            if (t != null && predicate.test(t)) {
                list.add(t);
            }
        }

        return list;
    }

    public List<ChunkSchematic> getChunksWithinBox(AABB box) {
        final int minX = Mth.floor(box.minX / 16.0);
        final int minZ = Mth.floor(box.minZ / 16.0);
        final int maxX = Mth.floor(box.maxX / 16.0);
        final int maxZ = Mth.floor(box.maxZ / 16.0);

        List<ChunkSchematic> chunks = new ArrayList<>();

        for (int cx = minX; cx <= maxX; ++cx) {
            for (int cz = minZ; cz <= maxZ; ++cz) {
                ChunkSchematic chunk = this.chunkManagerSchematic.getChunkIfExists(cx, cz);

                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    @Override
    public void setBlocksDirty(BlockPos pos, BlockState stateOld, BlockState stateNew) {
        if (stateNew != stateOld) {
            this.scheduleChunkRenders(pos.getX() >> 4, pos.getZ() >> 4);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ) {
        if (this.worldRenderer != null) {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkZ);
        }
    }

    @Override
    public int getMinBuildHeight() {
        return this.mc.level != null ? this.mc.level.getMinBuildHeight() : -64;
    }

    @Override
    public int getHeight() {
        return this.mc.level != null ? this.mc.level.getHeight() : 384;
    }

    // The following HeightLimitView overrides are to work around an incompatibility with Lithium 0.7.4+

    @Override
    public int getMaxBuildHeight() {
        return this.getMinBuildHeight() + this.getHeight();
    }

    @Override
    public int getMinSection() {
        return this.getMinBuildHeight() >> 4;
    }

    @Override
    public int getMaxSection() {
        return this.getMaxBuildHeight() >> 4;
    }

    @Override
    public int getSectionsCount() {
        return this.getMaxSection() - this.getMinSection();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return this.isOutsideBuildHeight(pos.getY());
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return (y < this.getMinBuildHeight()) || (y >= this.getMaxBuildHeight());
    }

    @Override
    public int getSectionIndex(int y) {
        return (y >> 4) - (this.getMinBuildHeight() >> 4);
    }

    @Override
    public int getSectionIndexFromSectionY(int coord) {
        return coord - (this.getMinBuildHeight() >> 4);
    }

    @Override
    public int getSectionYFromSectionIndex(int index) {
        return index + (this.getMinBuildHeight() >> 4);
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 0;
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int defaultValue) {
        return 15;
    }

    @Override
    public void sendBlockUpdated(BlockPos blockPos_1, BlockState blockState_1, BlockState blockState_2, int flags) {
        // NO-OP
    }

    @Override
    public void destroyBlockProgress(int entityId, BlockPos pos, int progress) {
        // NO-OP
    }

    @Override
    public void globalLevelEvent(int eventId, BlockPos pos, int data) {
        // NO-OP
    }

    @Override
    public void levelEvent(@Nullable Player entity, int id, BlockPos pos, int data) {
        // NO-OP
    }

    @Override
    public void gameEvent(Holder<GameEvent> event, Vec3 emitterPos, GameEvent.Context emitter) {
        // NO-OP
    }

    @Override
    public void playSeededSound(@Nullable Player except, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, long seed) {
        // NO-OP
    }

    @Override
    public void playSeededSound(@javax.annotation.Nullable Player except, Entity entity, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {
        // NO-OP
    }

    @Override
    public void addParticle(ParticleOptions particleParameters_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        // NO-OP
    }

    @Override
    public void addParticle(ParticleOptions particleParameters_1, boolean boolean_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        // NO-OP
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleParameters_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        // NO-OP
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions particleParameters_1, boolean boolean_1, double double_1, double double_2, double double_3, double double_4, double double_5, double double_6) {
        // NO-OP
    }

    @Override
    public void playLocalSound(double x, double y, double z, SoundEvent soundIn, SoundSource category, float volume, float pitch, boolean distanceDelay) {
        // NO-OP
    }

    @Override
    public void playSound(Player player, BlockPos pos, SoundEvent soundIn, SoundSource category, float volume, float pitch) {
        // NO-OP
    }

    @Override
    public void playSeededSound(@javax.annotation.Nullable Player except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource category, float volume, float pitch, long seed) {
        // NO-OP
    }

    @Override
    public void playSound(Player player, double x, double y, double z, SoundEvent soundIn, SoundSource category, float volume, float pitch) {
        // NO-OP
    }

    @Override
    public void playSound(@Nullable Player player, Entity entity, SoundEvent sound, SoundSource category, float volume, float pitch) {
        // NO-OP
    }

    @Override
    public RegistryAccess registryAccess() {
        if (this.mc != null && this.mc.level != null) {
            return this.mc.level.registryAccess();
        } else {
            return RegistryAccess.EMPTY;
        }
    }

    @Override
    public PotionBrewing potionBrewing() {
        if (this.mc != null && this.mc.level != null) {
            return this.mc.level.potionBrewing();
        } else {
            return PotionBrewing.EMPTY;
        }
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        if (this.mc != null && this.mc.level != null) {
            return this.mc.level.enabledFeatures();
        } else {
            return FeatureFlagSet.of();
        }
    }

    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[SCH] W: " + this.getChunkSource().gatherStats() + " E: " + this.getRegularEntityCount();
    }

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, Vec3 pos) {
        // NO-OP
    }

    @Override
    public void gameEvent(@Nullable Entity entity, Holder<GameEvent> event, BlockPos pos) {
        // NO-OP
    }

    @Override
    public void gameEvent(ResourceKey<GameEvent> event, BlockPos pos, @Nullable GameEvent.Context emitter) {
        // NO-OP
    }
}
