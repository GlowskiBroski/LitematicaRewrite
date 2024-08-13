package fi.dy.masa.litematica.util;

import com.google.common.base.Splitter;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import java.util.Iterator;
import java.util.Optional;

public class BlockUtils {
    private static final Splitter COMMA_SPLITTER = Splitter.on(',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

    public static BlockState fixMirrorDoubleChest(BlockState state, Mirror mirror, ChestType type) {
        Direction facing = state.getValue(ChestBlock.FACING);
        Direction.Axis axis = facing.getAxis();

        if (mirror == Mirror.FRONT_BACK) // x
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.X) {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        } else if (mirror == Mirror.LEFT_RIGHT) // z
        {
            state = state.setValue(ChestBlock.TYPE, type.getOpposite());

            if (axis == Direction.Axis.Z) {
                state = state.setValue(ChestBlock.FACING, facing.getOpposite());
            }
        }

        return state;
    }

    /**
     * Parses the provided string into the full block state.<br>
     * The string should be in either one of the following formats:<br>
     * 'minecraft:stone' or 'minecraft:smooth_stone_slab[half=top,waterlogged=false]'
     */
    public static Optional<BlockState> getBlockStateFromString(String str) {
        int index = str.indexOf("["); // [f=b]
        String blockName = index != -1 ? str.substring(0, index) : str;

        try {
            ResourceLocation id = new ResourceLocation(blockName);

            if (BuiltInRegistries.BLOCK.containsKey(id)) {
                Block block = BuiltInRegistries.BLOCK.get(id);
                BlockState state = block.defaultBlockState();

                if (index != -1 && str.length() > (index + 4) && str.charAt(str.length() - 1) == ']') {
                    StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();
                    String propStr = str.substring(index + 1, str.length() - 1);

                    for (String propAndVal : COMMA_SPLITTER.split(propStr)) {
                        Iterator<String> valIter = EQUAL_SPLITTER.split(propAndVal).iterator();

                        if (!valIter.hasNext()) {
                            continue;
                        }

                        Property<?> prop = stateManager.getProperty(valIter.next());

                        if (prop == null || !valIter.hasNext()) {
                            continue;
                        }

                        Comparable<?> val = getPropertyValueByName(prop, valIter.next());

                        if (val != null) {
                            state = getBlockStateWithProperty(state, prop, val);
                        }
                    }
                }

                return Optional.of(state);
            }
        } catch (Exception e) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> BlockState getBlockStateWithProperty(BlockState state, Property<T> prop, Comparable<?> value) {
        return state.setValue(prop, (T) value);
    }

    @Nullable
    public static <T extends Comparable<T>> T getPropertyValueByName(Property<T> prop, String valStr) {
        return prop.getValue(valStr).orElse(null);
    }

    public static boolean blocksHaveSameProperties(BlockState state1, BlockState state2) {
        StateDefinition<Block, BlockState> stateManager1 = state1.getBlock().getStateDefinition();
        StateDefinition<Block, BlockState> stateManager2 = state2.getBlock().getStateDefinition();
        return stateManager1.getProperties().equals(stateManager2.getProperties());
    }
}
