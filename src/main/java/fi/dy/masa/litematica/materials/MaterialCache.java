package fi.dy.masa.litematica.materials;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;

public class MaterialCache {
    private static final MaterialCache INSTANCE = new MaterialCache();

    protected final IdentityHashMap<BlockState, ItemStack> buildItemsForStates = new IdentityHashMap<>();
    protected final IdentityHashMap<BlockState, ItemStack> displayItemsForStates = new IdentityHashMap<>();
    protected final WorldSchematic tempWorld;
    protected final BlockPos checkPos;

    private MaterialCache() {
        this.tempWorld = SchematicWorldHandler.createSchematicWorld(null);
        this.checkPos = new BlockPos(8, 0, 8);

        WorldUtils.loadChunksSchematicWorld(this.tempWorld, this.checkPos, new Vec3i(1, 1, 1));
    }

    public static MaterialCache getInstance() {
        return INSTANCE;
    }

    public void clearCache() {
        this.buildItemsForStates.clear();
        this.displayItemsForStates.clear();
    }

    public ItemStack getRequiredBuildItemForState(BlockState state) {
        return this.getRequiredBuildItemForState(state, this.tempWorld, this.checkPos);
    }

    public ItemStack getRequiredBuildItemForState(BlockState state, Level world, BlockPos pos) {
        ItemStack stack = this.buildItemsForStates.get(state);

        if (stack == null) {
            stack = this.getItemForStateFromWorld(state, world, pos, true);
        }

        return stack;
    }

    public ItemStack getItemForDisplayNameForState(BlockState state) {
        ItemStack stack = this.displayItemsForStates.get(state);

        if (stack == null) {
            stack = this.getItemForStateFromWorld(state, this.tempWorld, this.checkPos, false);
        }

        return stack;
    }

    protected ItemStack getItemForStateFromWorld(BlockState state, Level world, BlockPos pos, boolean isBuildItem) {
        ItemStack stack = isBuildItem ? this.getStateToItemOverride(state) : null;

        if (stack == null) {
            world.setBlock(pos, state, 0x14);
            stack = state.getBlock().getCloneItemStack(world, pos, state);
        }

        if (stack == null || stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        } else {
            this.overrideStackSize(state, stack);
        }

        if (isBuildItem) {
            this.buildItemsForStates.put(state, stack);
        } else {
            this.displayItemsForStates.put(state, stack);
        }

        return stack;
    }

    public boolean requiresMultipleItems(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT;
    }

    public ImmutableList<ItemStack> getItems(BlockState state) {
        return this.getItems(state, this.tempWorld, this.checkPos);
    }

    public ImmutableList<ItemStack> getItems(BlockState state, Level world, BlockPos pos) {
        Block block = state.getBlock();

        if (block instanceof FlowerPotBlock && block != Blocks.FLOWER_POT) {
            return ImmutableList.of(new ItemStack(Blocks.FLOWER_POT), block.getCloneItemStack(world, pos, state));
        }

        return ImmutableList.of(this.getRequiredBuildItemForState(state, world, pos));
    }

    @Nullable
    protected ItemStack getStateToItemOverride(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.PISTON_HEAD ||
                block == Blocks.MOVING_PISTON ||
                block == Blocks.NETHER_PORTAL ||
                block == Blocks.END_PORTAL ||
                block == Blocks.END_GATEWAY) {
            return ItemStack.EMPTY;
        } else if (block == Blocks.FARMLAND) {
            return new ItemStack(Blocks.DIRT);
        } else if (block == Blocks.BROWN_MUSHROOM_BLOCK) {
            return new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK);
        } else if (block == Blocks.RED_MUSHROOM_BLOCK) {
            return new ItemStack(Blocks.RED_MUSHROOM_BLOCK);
        } else if (block == Blocks.LAVA) {
            if (state.getValue(LiquidBlock.LEVEL) == 0) {
                return new ItemStack(Items.LAVA_BUCKET);
            } else {
                return ItemStack.EMPTY;
            }
        } else if (block == Blocks.WATER) {
            if (state.getValue(LiquidBlock.LEVEL) == 0) {
                return new ItemStack(Items.WATER_BUCKET);
            } else {
                return ItemStack.EMPTY;
            }
        } else if (block instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return ItemStack.EMPTY;
        } else if (block instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return ItemStack.EMPTY;
        } else if (block instanceof DoublePlantBlock && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
            return ItemStack.EMPTY;
        }

        return null;
    }

    protected void overrideStackSize(BlockState state, ItemStack stack) {
        Block block = state.getBlock();

        if (block instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            stack.setCount(2);
        } else if (block == Blocks.SNOW) {
            stack.setCount(state.getValue(SnowLayerBlock.LAYERS));
        } else if (block instanceof TurtleEggBlock) {
            stack.setCount(state.getValue(TurtleEggBlock.EGGS));
        } else if (block instanceof SeaPickleBlock) {
            stack.setCount(state.getValue(SeaPickleBlock.PICKLES));
        } else if (block instanceof CandleBlock) {
            stack.setCount(state.getValue(CandleBlock.CANDLES));
        } else if (block instanceof MultifaceBlock) {
            stack.setCount(MultifaceBlock.availableFaces(state).size());
        }
    }
}
