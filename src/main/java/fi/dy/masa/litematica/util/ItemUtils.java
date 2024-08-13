package fi.dy.masa.litematica.util;

import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

public class ItemUtils {
    private static final IdentityHashMap<BlockState, ItemStack> ITEMS_FOR_STATES = new IdentityHashMap<>();

    public static boolean areTagsEqualIgnoreDamage(ItemStack stackReference, ItemStack stackToCheck) {
        ItemStack ref = stackReference.copy();
        ItemStack check = stackToCheck.copy();

        if (ref.isDamageableItem() && ref.isDamaged()) {
            ref.setDamageValue(0);
        }
        if (check.isDamageableItem() && check.isDamaged()) {
            check.setDamageValue(0);
        }

        return ItemStack.isSameItemSameComponents(ref, check);
    }

    public static ItemStack getItemForState(BlockState state) {
        ItemStack stack = ITEMS_FOR_STATES.get(state);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void setItemForBlock(Level world, BlockPos pos, BlockState state) {
        if (!ITEMS_FOR_STATES.containsKey(state)) {
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state, false));
        }
    }

    public static ItemStack getItemForBlock(Level world, BlockPos pos, BlockState state, boolean checkCache) {
        if (checkCache) {
            ItemStack stack = ITEMS_FOR_STATES.get(state);

            if (stack != null) {
                return stack;
            }
        }

        if (state.isAir()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getStateToItemOverride(state);

        if (stack.isEmpty()) {
            stack = state.getBlock().getCloneItemStack(world, pos, state);
        }

        if (stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        } else {
            overrideStackSize(state, stack);
        }

        ITEMS_FOR_STATES.put(state, stack);

        return stack;
    }

    public static ItemStack getStateToItemOverride(BlockState state) {
        if (state.getBlock() == Blocks.LAVA) {
            return new ItemStack(Items.LAVA_BUCKET);
        } else if (state.getBlock() == Blocks.WATER) {
            return new ItemStack(Items.WATER_BUCKET);
        }

        return ItemStack.EMPTY;
    }

    private static void overrideStackSize(BlockState state, ItemStack stack) {
        if (state.getBlock() instanceof SlabBlock && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            stack.setCount(2);
        }
    }

    public static String getStackString(ItemStack stack) {
        if (!stack.isEmpty()) {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                    rl != null ? rl.toString() : "null", stack.getHoverName().getString(),
                    stack.getComponents() != null ? stack.getComponents().toString() : "<no NBT>", stack);
        }

        return "<empty>";
    }
}
