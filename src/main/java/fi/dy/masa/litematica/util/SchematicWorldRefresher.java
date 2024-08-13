package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class SchematicWorldRefresher implements IRangeChangeListener {
    public static final SchematicWorldRefresher INSTANCE = new SchematicWorldRefresher();


    @Override
    public void updateAll() {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            final int minY = world.getMinBuildHeight();
            final int maxY = world.getMaxBuildHeight() - 1;
            this.updateBetweenY(minY, maxY);
        }
    }

    @Override
    public void updateBetweenX(int minX, int maxX) {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            final int cxMin = (Math.min(minX, maxX) >> 4);
            final int cxMax = (Math.max(minX, maxX) >> 4);

            for (ChunkSchematic chunk : schematicChunks.values()) {
                ChunkPos pos = chunk.getPos();

                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.x >= cxMin && pos.x <= cxMax && !chunk.isEmpty() && WorldUtils.isClientChunkLoaded(MC.level, pos.x, pos.z)) {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    @Override
    public void updateBetweenY(int minY, int maxY) {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();

            for (ChunkSchematic chunk : schematicChunks.values()) {
                ChunkPos pos = chunk.getPos();

                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (!chunk.isEmpty() && WorldUtils.isClientChunkLoaded(MC.level, pos.x, pos.z)) {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ) {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkProvider().getLoadedChunks();
            final int czMin = (Math.min(minZ, maxZ) >> 4);
            final int czMax = (Math.max(minZ, maxZ) >> 4);

            for (ChunkSchematic chunk : schematicChunks.values()) {
                ChunkPos pos = chunk.getPos();

                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.z >= czMin && pos.z <= czMax && !chunk.isEmpty() && WorldUtils.isClientChunkLoaded(MC.level, pos.x, pos.z)) {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    public void markSchematicChunksForRenderUpdate(int chunkX, int chunkZ) {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            if (world.getChunkProvider().hasChunk(chunkX, chunkZ) && WorldUtils.isClientChunkLoaded(MC.level, chunkX, chunkZ)) {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }

    public void markSchematicChunkForRenderUpdate(BlockPos pos) {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && MC.level != null) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            //Litematica.debugLog("SchematicWorldRefresher#markSchematicChunkForRenderUpdate({}, {})", chunkX, chunkZ);

            if (world.getChunkProvider().hasChunk(chunkX, chunkZ) && WorldUtils.isClientChunkLoaded(MC.level, chunkX, chunkZ)) {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }
}
