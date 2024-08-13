package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class InventoryUtils {
    private static final List<Integer> PICK_BLOCKABLE_SLOTS = new ArrayList<>();
    private static int nextPickSlotIndex;

    public static void setPickBlockableSlots(String configStr) {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");

        for (String str : parts) {
            try {
                int slotNum = Integer.parseInt(str) - 1;

                if (Inventory.isHotbarSlot(slotNum) &&
                        !PICK_BLOCKABLE_SLOTS.contains(slotNum)) {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            } catch (NumberFormatException ignore) {
            }
        }
    }

    public static void setPickedItemToHand(ItemStack stack, Minecraft mc) {
        int slotNum = mc.player.getInventory().findSlotMatchingItem(stack);
        setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setPickedItemToHand(int sourceSlot, ItemStack stack, Minecraft mc) {
        Player player = mc.player;
        Inventory inventory = player.getInventory();

        if (Inventory.isHotbarSlot(sourceSlot)) {
            inventory.selected = sourceSlot;
        } else {
            if (PICK_BLOCKABLE_SLOTS.size() == 0) {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return;
            }

            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || !Inventory.isHotbarSlot(sourceSlot)) {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1) {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1) {
                inventory.selected = hotbarSlot;

                if (EntityUtils.isCreativeMode(player)) {
                    inventory.items.set(hotbarSlot, stack.copy());
                } else {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }

                WorldUtils.setEasyPlaceLastPickBlockTime();
            } else {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }
    }

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               Level schematicWorld, Minecraft mc) {
        if (!stack.isEmpty()) {
            Inventory inv = mc.player.getInventory();
            stack = stack.copy();

            if (EntityUtils.isCreativeMode(mc.player)) {
                BlockEntity te = schematicWorld.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.level.isEmptyBlock(pos)) {
                    te.saveToItem(stack, schematicWorld.registryAccess());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.gameMode.handleCreativeModeItemAdd(mc.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inv.selected);

                //return true;
            } else {
                int slot = inv.findSlotMatchingItem(stack);
                boolean shouldPick = inv.selected != slot;

                if (shouldPick && slot != -1) {
                    setPickedItemToHand(stack, mc);
                } else if (slot == -1 && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue()) {
                    slot = findSlotWithBoxWithItem(mc.player.inventoryMenu, stack, false);

                    if (slot != -1) {
                        ItemStack boxStack = mc.player.inventoryMenu.slots.get(slot).getItem();
                        setPickedItemToHand(boxStack, mc);
                    }
                }

                //return shouldPick == false || canPick;
            }
        }
    }

    private static boolean canPickToSlot(Inventory inventory, int slotNum) {
        if (!PICK_BLOCKABLE_SLOTS.contains(slotNum)) {
            return false;
        }

        ItemStack stack = inventory.getItem(slotNum);

        if (stack.isEmpty()) {
            return true;
        }

        return (!Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() ||
                !stack.isDamageableItem()) &&
                (!Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() ||
                        !(stack.getItem() instanceof TieredItem));
    }

    private static int getPickBlockTargetSlot(Player player) {
        if (PICK_BLOCKABLE_SLOTS.isEmpty()) {
            return -1;
        }

        int slotNum = player.getInventory().selected;

        if (canPickToSlot(player.getInventory(), slotNum)) {
            return slotNum;
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size()) {
            nextPickSlotIndex = 0;
        }

        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i) {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size()) {
                nextPickSlotIndex = 0;
            }

            if (canPickToSlot(player.getInventory(), slotNum)) {
                return slotNum;
            }
        }

        return -1;
    }

    private static int getEmptyPickBlockableHotbarSlot(Inventory inventory) {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i) {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i);

            if (Inventory.isHotbarSlot(slotNum)) {
                ItemStack stack = inventory.getItem(slotNum);

                if (stack.isEmpty()) {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    public static boolean doesShulkerBoxContainItem(ItemStack stack, ItemStack referenceItem) {
        NonNullList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    public static boolean doesBundleContainItem(ItemStack stack, ItemStack referenceItem) {
        NonNullList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getBundleItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    private static boolean doesListContainItem(NonNullList<ItemStack> items, ItemStack referenceItem) {
        if (items.size() > 0) {
            for (ItemStack item : items) {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int findSlotWithBoxWithItem(AbstractContainerMenu container, ItemStack stackReference, boolean reverse) {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof InventoryMenu;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment) {
            Slot slot = container.slots.get(slotNum);

            if ((!isPlayerInv || fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.index, false)) &&
                    doesShulkerBoxContainItem(slot.getItem(), stackReference)) {
                return slot.index;
            }
        }

        return -1;
    }
}
