package fi.dy.masa.litematica.schematic.conversion;

import fi.dy.masa.litematica.mixin.IMixinFenceGateBlock;
import fi.dy.masa.litematica.mixin.IMixinRedStoneWireBlock;
import fi.dy.masa.litematica.mixin.IMixinVineBlock;
import fi.dy.masa.malilib.util.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.StairsShape;

import static net.minecraft.world.level.block.StairBlock.HALF;

public class SchematicConversionFixers
{
    private static final BooleanProperty[] HORIZONTAL_CONNECTING_BLOCK_PROPS = new BooleanProperty[] { null, null, CrossCollisionBlock.NORTH, CrossCollisionBlock.SOUTH, CrossCollisionBlock.WEST, CrossCollisionBlock.EAST };
    private static final BlockState REDSTONE_WIRE_DOT_OLD = Blocks.REDSTONE_WIRE.defaultBlockState();
    private static final BlockState REDSTONE_WIRE_DOT = Blocks.REDSTONE_WIRE.defaultBlockState()
                          .setValue(RedStoneWireBlock.POWER, 0)
                          .setValue(RedStoneWireBlock.NORTH, RedstoneSide.NONE)
                          .setValue(RedStoneWireBlock.EAST, RedstoneSide.NONE)
                          .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.NONE)
                          .setValue(RedStoneWireBlock.WEST, RedstoneSide.NONE);
    private static final BlockState REDSTONE_WIRE_CROSS = Blocks.REDSTONE_WIRE.defaultBlockState()
                          .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
                          .setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)
                          .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE)
                          .setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE);

    public static final IStateFixer FIXER_BANNER = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Base", Constants.NBT.TAG_INT))
        {
            DyeColor colorOrig = ((AbstractBannerBlock) state.getBlock()).getColor();
            DyeColor colorFromData = DyeColor.byId(15 - tag.getInt("Base"));

            if (colorOrig != colorFromData)
            {
                Integer rotation = state.getValue(BannerBlock.ROTATION);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_BANNER.defaultBlockState();      break;
                    case ORANGE:        state = Blocks.ORANGE_BANNER.defaultBlockState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_BANNER.defaultBlockState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_BANNER.defaultBlockState(); break;
                    case YELLOW:        state = Blocks.YELLOW_BANNER.defaultBlockState();     break;
                    case LIME:          state = Blocks.LIME_BANNER.defaultBlockState();       break;
                    case PINK:          state = Blocks.PINK_BANNER.defaultBlockState();       break;
                    case GRAY:          state = Blocks.GRAY_BANNER.defaultBlockState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_BANNER.defaultBlockState(); break;
                    case CYAN:          state = Blocks.CYAN_BANNER.defaultBlockState();       break;
                    case PURPLE:        state = Blocks.PURPLE_BANNER.defaultBlockState();     break;
                    case BLUE:          state = Blocks.BLUE_BANNER.defaultBlockState();       break;
                    case BROWN:         state = Blocks.BROWN_BANNER.defaultBlockState();      break;
                    case GREEN:         state = Blocks.GREEN_BANNER.defaultBlockState();      break;
                    case RED:           state = Blocks.RED_BANNER.defaultBlockState();        break;
                    case BLACK:         state = Blocks.BLACK_BANNER.defaultBlockState();      break;
                }

                state = state.setValue(BannerBlock.ROTATION, rotation);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BANNER_WALL = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Base", Constants.NBT.TAG_INT))
        {
            DyeColor colorOrig = ((AbstractBannerBlock) state.getBlock()).getColor();
            DyeColor colorFromData = DyeColor.byId(15 - tag.getInt("Base"));

            if (colorOrig != colorFromData)
            {
                Direction facing = state.getValue(WallBannerBlock.FACING);

                switch (colorFromData)
                {
                    case WHITE:         state = Blocks.WHITE_WALL_BANNER.defaultBlockState();      break;
                    case ORANGE:        state = Blocks.ORANGE_WALL_BANNER.defaultBlockState();     break;
                    case MAGENTA:       state = Blocks.MAGENTA_WALL_BANNER.defaultBlockState();    break;
                    case LIGHT_BLUE:    state = Blocks.LIGHT_BLUE_WALL_BANNER.defaultBlockState(); break;
                    case YELLOW:        state = Blocks.YELLOW_WALL_BANNER.defaultBlockState();     break;
                    case LIME:          state = Blocks.LIME_WALL_BANNER.defaultBlockState();       break;
                    case PINK:          state = Blocks.PINK_WALL_BANNER.defaultBlockState();       break;
                    case GRAY:          state = Blocks.GRAY_WALL_BANNER.defaultBlockState();       break;
                    case LIGHT_GRAY:    state = Blocks.LIGHT_GRAY_WALL_BANNER.defaultBlockState(); break;
                    case CYAN:          state = Blocks.CYAN_WALL_BANNER.defaultBlockState();       break;
                    case PURPLE:        state = Blocks.PURPLE_WALL_BANNER.defaultBlockState();     break;
                    case BLUE:          state = Blocks.BLUE_WALL_BANNER.defaultBlockState();       break;
                    case BROWN:         state = Blocks.BROWN_WALL_BANNER.defaultBlockState();      break;
                    case GREEN:         state = Blocks.GREEN_WALL_BANNER.defaultBlockState();      break;
                    case RED:           state = Blocks.RED_WALL_BANNER.defaultBlockState();        break;
                    case BLACK:         state = Blocks.BLACK_WALL_BANNER.defaultBlockState();      break;
                }

                state = state.setValue(WallBannerBlock.FACING, facing);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_BED = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("color", Constants.NBT.TAG_INT))
        {
            int colorId = tag.getInt("color");
            Direction facing = state.getValue(BedBlock.FACING);
            BedPart part = state.getValue(BedBlock.PART);
            Boolean occupied = state.getValue(BedBlock.OCCUPIED);

            switch (colorId)
            {
                case  0: state = Blocks.WHITE_BED.defaultBlockState(); break;
                case  1: state = Blocks.ORANGE_BED.defaultBlockState(); break;
                case  2: state = Blocks.MAGENTA_BED.defaultBlockState(); break;
                case  3: state = Blocks.LIGHT_BLUE_BED.defaultBlockState(); break;
                case  4: state = Blocks.YELLOW_BED.defaultBlockState(); break;
                case  5: state = Blocks.LIME_BED.defaultBlockState(); break;
                case  6: state = Blocks.PINK_BED.defaultBlockState(); break;
                case  7: state = Blocks.GRAY_BED.defaultBlockState(); break;
                case  8: state = Blocks.LIGHT_GRAY_BED.defaultBlockState(); break;
                case  9: state = Blocks.CYAN_BED.defaultBlockState(); break;
                case 10: state = Blocks.PURPLE_BED.defaultBlockState(); break;
                case 11: state = Blocks.BLUE_BED.defaultBlockState(); break;
                case 12: state = Blocks.BROWN_BED.defaultBlockState(); break;
                case 13: state =  Blocks.GREEN_BED.defaultBlockState(); break;
                case 14: state = Blocks.RED_BED.defaultBlockState(); break;
                case 15: state = Blocks.BLACK_BED.defaultBlockState(); break;
                default: return state;
            }

            state = state.setValue(BedBlock.FACING, facing)
                         .setValue(BedBlock.PART, part)
                         .setValue(BedBlock.OCCUPIED, occupied);
        }

        return state;
    };

    public static final IStateFixer FIXER_CHRORUS_PLANT = (reader, state, pos) -> ChorusPlantBlock.getStateWithConnections(reader, pos, state);

    public static final IStateFixer FIXER_DIRT_SNOWY = (reader, state, pos) -> {
        Block block = reader.getBlockState(pos.above()).getBlock();
        return state.setValue(SnowyDirtBlock.SNOWY, Boolean.valueOf(block == Blocks.SNOW_BLOCK || block == Blocks.SNOW));
    };

    public static final IStateFixer FIXER_DOOR = (reader, state, pos) -> {
        if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            BlockState stateLower = reader.getBlockState(pos.below());

            if (stateLower.getBlock() == state.getBlock())
            {
                state = state.setValue(DoorBlock.FACING, stateLower.getValue(DoorBlock.FACING));
                state = state.setValue(DoorBlock.OPEN,   stateLower.getValue(DoorBlock.OPEN));
            }
        }
        else
        {
            BlockState stateUpper = reader.getBlockState(pos.above());

            if (stateUpper.getBlock() == state.getBlock())
            {
                state = state.setValue(DoorBlock.HINGE,   stateUpper.getValue(DoorBlock.HINGE));
                state = state.setValue(DoorBlock.POWERED, stateUpper.getValue(DoorBlock.POWERED));
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_DOUBLE_PLANT = (reader, state, pos) -> {
        if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER)
        {
            BlockState stateLower = reader.getBlockState(pos.below());

            if (stateLower.getBlock() instanceof DoublePlantBlock)
            {
                state = stateLower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_FENCE = (reader, state, pos) -> {
        FenceBlock fence = (FenceBlock) state.getBlock();

        for (Direction side : fi.dy.masa.malilib.util.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.relative(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Direction sideOpposite = side.getOpposite();
            boolean flag = stateAdj.isFaceSturdy(reader, posAdj, sideOpposite);
            state = state.setValue(HORIZONTAL_CONNECTING_BLOCK_PROPS[side.get3DDataValue()], fence.connectsTo(stateAdj, flag, sideOpposite));
        }

        return state;
    };

    public static final IStateFixer FIXER_FENCE_GATE = (reader, state, pos) -> {
        FenceGateBlock gate = (FenceGateBlock) state.getBlock();
        Direction facing = state.getValue(FenceGateBlock.FACING);
        boolean inWall = false;

        if (facing.getAxis() == Direction.Axis.X)
        {
            inWall = (((IMixinFenceGateBlock) gate).invokeIsWall(reader.getBlockState(pos.relative(Direction.NORTH)))
                   || ((IMixinFenceGateBlock) gate).invokeIsWall(reader.getBlockState(pos.relative(Direction.SOUTH))));
        }
        else
        {
            inWall = (((IMixinFenceGateBlock) gate).invokeIsWall(reader.getBlockState(pos.relative(Direction.WEST)))
                   || ((IMixinFenceGateBlock) gate).invokeIsWall(reader.getBlockState(pos.relative(Direction.EAST))));
        }

        return state.setValue(FenceGateBlock.IN_WALL, inWall);
    };

    public static final IStateFixer FIXER_FIRE = (reader, state, pos) -> {
        return BaseFireBlock.getState(reader, pos);
    };

    public static final IStateFixer FIXER_FLOWER_POT = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Item", 8))
        {
            String itemName = tag.getString("Item");

            if (itemName.length() > 0 && tag.contains("Data"))
            {
                int meta = tag.getInt("Data");

                switch (itemName)
                {
                    case "minecraft:sapling":
                        if (meta == 0)      return Blocks.POTTED_OAK_SAPLING.defaultBlockState();
                        if (meta == 1)      return Blocks.POTTED_SPRUCE_SAPLING.defaultBlockState();
                        if (meta == 2)      return Blocks.POTTED_BIRCH_SAPLING.defaultBlockState();
                        if (meta == 3)      return Blocks.POTTED_JUNGLE_SAPLING.defaultBlockState();
                        if (meta == 4)      return Blocks.POTTED_ACACIA_SAPLING.defaultBlockState();
                        if (meta == 5)      return Blocks.POTTED_DARK_OAK_SAPLING.defaultBlockState();
                        break;
                    case "minecraft:tallgrass":
                        if (meta == 0)      return Blocks.POTTED_DEAD_BUSH.defaultBlockState();
                        if (meta == 2)      return Blocks.POTTED_FERN.defaultBlockState();
                        break;
                    case "minecraft:red_flower":
                        if (meta == 0)      return Blocks.POTTED_POPPY.defaultBlockState();
                        if (meta == 1)      return Blocks.POTTED_BLUE_ORCHID.defaultBlockState();
                        if (meta == 2)      return Blocks.POTTED_ALLIUM.defaultBlockState();
                        if (meta == 3)      return Blocks.POTTED_AZURE_BLUET.defaultBlockState();
                        if (meta == 4)      return Blocks.POTTED_RED_TULIP.defaultBlockState();
                        if (meta == 5)      return Blocks.POTTED_ORANGE_TULIP.defaultBlockState();
                        if (meta == 6)      return Blocks.POTTED_WHITE_TULIP.defaultBlockState();
                        if (meta == 7)      return Blocks.POTTED_PINK_TULIP.defaultBlockState();
                        if (meta == 8)      return Blocks.POTTED_OXEYE_DAISY.defaultBlockState();
                        break;
                    case "minecraft:yellow_flower":     return Blocks.POTTED_DANDELION.defaultBlockState();
                    case "minecraft:brown_mushroom":    return Blocks.POTTED_BROWN_MUSHROOM.defaultBlockState();
                    case "minecraft:red_mushroom":      return Blocks.POTTED_RED_MUSHROOM.defaultBlockState();
                    case "minecraft:deadbush":          return Blocks.POTTED_DEAD_BUSH.defaultBlockState();
                    case "minecraft:cactus":            return Blocks.POTTED_CACTUS.defaultBlockState();
                    default:                            return state;
                }
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_NOTE_BLOCK = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null)
        {
            state = state
                        .setValue(NoteBlock.POWERED, tag.getBoolean("powered"))
                        .setValue(NoteBlock.NOTE, Mth.clamp(tag.getByte("note"), 0, 24))
                        .setValue(NoteBlock.INSTRUMENT, reader.getBlockState(pos.below()).instrument());
        }

        return state;
    };

    public static final IStateFixer FIXER_PANE = (reader, state, pos) -> {
        IronBarsBlock pane = (IronBarsBlock) state.getBlock();

        for (Direction side : fi.dy.masa.malilib.util.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.relative(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Direction sideOpposite = side.getOpposite();
            boolean flag = stateAdj.isFaceSturdy(reader, posAdj, sideOpposite);
            state = state.setValue(HORIZONTAL_CONNECTING_BLOCK_PROPS[side.get3DDataValue()], pane.attachsTo(stateAdj, flag));
        }

        return state;
    };

    public static final IStateFixer FIXER_REDSTONE_REPEATER = (reader, state, pos) -> {
        return state.setValue(RepeaterBlock.LOCKED, Boolean.valueOf(getIsRepeaterPoweredOnSide(reader, pos, state)));
    };

    public static final IStateFixer FIXER_REDSTONE_WIRE = (reader, state, pos) -> {
        RedStoneWireBlock wire = (RedStoneWireBlock) state.getBlock();
        state = ((IMixinRedStoneWireBlock) wire).litematicaGetPlacementState(reader, state, pos);

        // Turn all old dots into crosses, while keeping the power level
        if (state.equals(REDSTONE_WIRE_DOT) == false && state.setValue(RedStoneWireBlock.POWER, 0) == REDSTONE_WIRE_DOT_OLD)
        {
            state = REDSTONE_WIRE_CROSS.setValue(RedStoneWireBlock.POWER, state.getValue(RedStoneWireBlock.POWER));
        }

        return state;
    };

    public static final IStateFixer FIXER_SIGN = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("Text1", Constants.NBT.TAG_STRING))
        {
            ListTag textList = new ListTag();
            textList.add(tag.get("Text1"));
            textList.add(tag.get("Text2"));
            textList.add(tag.get("Text3"));
            textList.add(tag.get("Text4"));

            CompoundTag frontTextTag = new CompoundTag();
            frontTextTag.put("messages", textList);
            frontTextTag.putString("color", tag.getString("Color"));
            frontTextTag.putByte("has_glowing_text", tag.getByte("GlowingText"));

            tag.put("front_text", frontTextTag);

            tag.remove("Color");
            tag.remove("GlowingText");
            tag.remove("Text1");
            tag.remove("Text2");
            tag.remove("Text3");
            tag.remove("Text4");
        }

        return state;
    };

    public static final IStateFixer FIXER_SKULL = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("SkullType"))
        {
            int id = Mth.clamp(tag.getByte("SkullType"), 0, 5);

            // ;_; >_> <_<
            if (id == 2) { id = 3; } else if (id == 3) { id = 2; }

            SkullBlock.Type typeOrig = ((AbstractSkullBlock) state.getBlock()).getType();
            SkullBlock.Type typeFromData = SkullBlock.Types.values()[id];

            if (typeOrig != typeFromData)
            {
                if (typeFromData == SkullBlock.Types.SKELETON)
                {
                    state = Blocks.SKELETON_SKULL.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_SKULL.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.PLAYER)
                {
                    state = Blocks.PLAYER_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.CREEPER)
                {
                    state = Blocks.CREEPER_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.DRAGON)
                {
                    state = Blocks.DRAGON_HEAD.defaultBlockState();
                }
            }

            state = state.setValue(BannerBlock.ROTATION, Mth.clamp(tag.getByte("Rot"), 0, 15));
        }

        return state;
    };

    public static final IStateFixer FIXER_SKULL_WALL = (reader, state, pos) -> {
        CompoundTag tag = reader.getBlockEntityData(pos);

        if (tag != null && tag.contains("SkullType", Constants.NBT.TAG_BYTE))
        {
            int id = Mth.clamp(tag.getByte("SkullType"), 0, 5);

            // ;_; >_> <_<
            if (id == 2) { id = 3; } else if (id == 3) { id = 2; }

            SkullBlock.Type typeOrig = ((AbstractSkullBlock) state.getBlock()).getType();
            SkullBlock.Type typeFromData = SkullBlock.Types.values()[id];

            if (typeOrig != typeFromData)
            {
                Direction facing = state.getValue(WallSkullBlock.FACING);

                if (typeFromData == SkullBlock.Types.SKELETON)
                {
                    state = Blocks.SKELETON_WALL_SKULL.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.WITHER_SKELETON)
                {
                    state = Blocks.WITHER_SKELETON_WALL_SKULL.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.PLAYER)
                {
                    state = Blocks.PLAYER_WALL_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.ZOMBIE)
                {
                    state = Blocks.ZOMBIE_WALL_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.CREEPER)
                {
                    state = Blocks.CREEPER_WALL_HEAD.defaultBlockState();
                }
                else if (typeFromData == SkullBlock.Types.DRAGON)
                {
                    state = Blocks.DRAGON_WALL_HEAD.defaultBlockState();
                }

                state = state.setValue(WallSkullBlock.FACING, facing);
            }
        }

        return state;
    };

    public static final IStateFixer FIXER_STAIRS = (reader, state, pos) -> {
        return state.setValue(StairBlock.SHAPE, getStairShape(state, reader, pos));
    };

    //These are stolen from StairsBlock as they are private, but necessary. Could not use an accessor as it broke Intellij Hotswapping
    private static StairsShape getStairShape(BlockState state, BlockGetter world, BlockPos pos) {
        Direction direction = (Direction)state.getValue(StairBlock.FACING);
        BlockState blockState = world.getBlockState(pos.relative(direction));
        if (StairBlock.isStairs(blockState) && state.getValue(HALF) == blockState.getValue(HALF)) {
            Direction direction2 = (Direction)blockState.getValue(StairBlock.FACING);
            if (direction2.getAxis() != ((Direction)state.getValue(StairBlock.FACING)).getAxis() && isDifferentOrientation(state, world, pos, direction2.getOpposite())) {
                if (direction2 == direction.getCounterClockWise()) {
                    return StairsShape.OUTER_LEFT;
                }

                return StairsShape.OUTER_RIGHT;
            }
        }

        BlockState blockState2 = world.getBlockState(pos.relative(direction.getOpposite()));
        if (StairBlock.isStairs(blockState2) && state.getValue(HALF) == blockState2.getValue(HALF)) {
            Direction direction3 = (Direction)blockState2.getValue(StairBlock.FACING);
            if (direction3.getAxis() != ((Direction)state.getValue(StairBlock.FACING)).getAxis() && isDifferentOrientation(state, world, pos, direction3)) {
                if (direction3 == direction.getCounterClockWise()) {
                    return StairsShape.INNER_LEFT;
                }

                return StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private static boolean isDifferentOrientation(BlockState state, BlockGetter world, BlockPos pos, Direction dir) {
        BlockState blockState = world.getBlockState(pos.relative(dir));
        return !StairBlock.isStairs(blockState) || blockState.getValue(StairBlock.FACING) != state.getValue(StairBlock.FACING) || blockState.getValue(HALF) != state.getValue(HALF);
    }

    public static final IStateFixer FIXER_STEM = (reader, state, pos) -> {
        /* FIXME 1.20.3 - the gourd block and attached stem are now RegistryKey<Block>, plus they are private...
        StemBlock stem = (StemBlock) state.getBlock();
        GourdBlock crop = stem.getGourdBlock();

        for (Direction side : fi.dy.masa.malilib.util.PositionUtils.HORIZONTAL_DIRECTIONS)
        {
            BlockPos posAdj = pos.offset(side);
            BlockState stateAdj = reader.getBlockState(posAdj);
            Block blockAdj = stateAdj.getBlock();

            if (blockAdj == crop || (stem == Blocks.PUMPKIN_STEM && blockAdj == Blocks.CARVED_PUMPKIN))
            {
                return crop.getAttachedStem().getDefaultState().with(AttachedStemBlock.FACING, side);
            }
        }
        */

        return state;
    };

    public static final IStateFixer FIXER_TRIPWIRE = (reader, state, pos) -> {
        TripWireBlock wire = (TripWireBlock) state.getBlock();

        return state
                .setValue(TripWireBlock.NORTH, ((TripWireBlock) wire).shouldConnectTo(reader.getBlockState(pos.north()), Direction.NORTH))
                .setValue(TripWireBlock.SOUTH, ((TripWireBlock) wire).shouldConnectTo(reader.getBlockState(pos.south()), Direction.SOUTH))
                .setValue(TripWireBlock.WEST, ((TripWireBlock) wire).shouldConnectTo(reader.getBlockState(pos.west()), Direction.WEST))
                .setValue(TripWireBlock.EAST, ((TripWireBlock) wire).shouldConnectTo(reader.getBlockState(pos.east()), Direction.EAST));
    };

    public static final IStateFixer FIXER_VINE = (reader, state, pos) -> {
        VineBlock vine = (VineBlock) state.getBlock();
        return state.setValue(VineBlock.UP, ((IMixinVineBlock) vine).invokeShouldConnectUp(reader, pos.above(), Direction.UP));
    };

    private static boolean getIsRepeaterPoweredOnSide(BlockGetter reader, BlockPos pos, BlockState stateRepeater)
    {
        Direction facing = stateRepeater.getValue(RepeaterBlock.FACING);
        Direction sideLeft = facing.getCounterClockWise();
        Direction sideRight = facing.getClockWise();

        return getRepeaterPowerOnSide(reader, pos.relative(sideLeft) , sideLeft ) > 0 ||
               getRepeaterPowerOnSide(reader, pos.relative(sideRight), sideRight) > 0;
    }

    private static int getRepeaterPowerOnSide(BlockGetter reader, BlockPos pos, Direction side)
    {
        BlockState state = reader.getBlockState(pos);
        Block block = state.getBlock();

        if (DiodeBlock.isDiode(state))
        {
            if (block == Blocks.REDSTONE_BLOCK)
            {
                return 15;
            }
            else
            {
                return block == Blocks.REDSTONE_WIRE ? state.getValue(RedStoneWireBlock.POWER) : state.getDirectSignal(reader, pos, side);
            }
        }
        else
        {
            return 0;
        }
    }

    public interface IStateFixer
    {
        BlockState fixState(IBlockReaderWithData reader, BlockState state, BlockPos pos);
    }
}
