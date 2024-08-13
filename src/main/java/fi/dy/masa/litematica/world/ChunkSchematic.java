package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ChunkSchematic extends LevelChunk {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final List<Entity> entityList = new ArrayList<>();
    private final long timeCreated;
    private final int bottomY;
    private final int topY;
    private int entityCount;
    private boolean isEmpty = true;

    public ChunkSchematic(Level worldIn, ChunkPos pos) {
        super(worldIn, pos);

        this.timeCreated = worldIn.getGameTime();
        this.bottomY = worldIn.getMinBuildHeight();
        this.topY = worldIn.getMaxBuildHeight();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX() & 0xF;
        int y = pos.getY();
        int z = pos.getZ() & 0xF;
        int cy = this.getSectionIndex(y);
        y &= 0xF;

        LevelChunkSection[] sections = this.getSections();

        if (cy >= 0 && cy < sections.length) {
            LevelChunkSection chunkSection = sections[cy];

            if (!chunkSection.hasOnlyAir()) {
                return chunkSection.getBlockState(x, y, z);
            }
        }

        return AIR;
    }

    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        BlockState stateOld = this.getBlockState(pos);
        int y = pos.getY();

        if (stateOld == state || y >= this.topY || y < this.bottomY) {
            return null;
        } else {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;
            int cy = this.getSectionIndex(y);

            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            LevelChunkSection section = this.getSections()[cy];

            if (section.hasOnlyAir() && state.isAir()) {
                return null;
            }

            y &= 0xF;

            if (!state.isAir()) {
                this.isEmpty = false;
            }

            section.setBlockState(x, y, z, state);

            if (blockOld != blockNew) {
                this.getLevel().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y, z).getBlock() != blockNew) {
                return null;
            } else {
                if (state.hasBlockEntity() && blockNew instanceof EntityBlock) {
                    BlockEntity te = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                    if (te == null) {
                        te = ((EntityBlock) blockNew).newBlockEntity(pos, state);

                        if (te != null) {
                            this.getLevel().getChunkAt(pos).setBlockEntity(te);
                        }
                    }
                }

                this.isUnsaved();

                return stateOld;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addEntity(Entity entity) {
        this.entityList.add(entity);
        ++this.entityCount;
    }

    public List<Entity> getEntityList() {
        return this.entityList;
    }

    public int getEntityCount() {
        return this.entityCount;
    }

    public long getTimeCreated() {
        return this.timeCreated;
    }

    @Override
    public boolean isEmpty() {
        return this.isEmpty;
    }
}
