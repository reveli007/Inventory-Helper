package ru.dogod.craftingtablecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public final class InventorySorter {
    private InventorySorter() {
    }

    public static void sort(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null
                || player.currentScreenHandler != screen.getScreenHandler()) {
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) {
            player.sendMessage(Text.literal("Place the item on your cursor before sorting."), true);
            return;
        }

        List<Integer> slotIds = getSortableSlots(screen, handler, player);
        if (slotIds.size() < 2) {
            return;
        }

        mergeMatchingStacks(client, player, handler, slotIds);
        sortStacks(client, player, handler, slotIds);
    }

    private static List<Integer> getSortableSlots(
            HandledScreen<?> screen,
            ScreenHandler handler,
            PlayerEntity player
    ) {
        List<Integer> slotIds = new ArrayList<>();
        if (screen instanceof InventoryScreen) {
            for (int slotId = 9; slotId < 36; slotId++) {
                slotIds.add(slotId);
            }
            return slotIds;
        }

        if (!(screen instanceof GenericContainerScreen) && !(screen instanceof ShulkerBoxScreen)) {
            return slotIds;
        }

        for (int slotId = 0; slotId < handler.slots.size(); slotId++) {
            Slot slot = handler.getSlot(slotId);
            if (slot.inventory != player.getInventory()) {
                slotIds.add(slotId);
            }
        }
        return slotIds;
    }

    private static void mergeMatchingStacks(
            MinecraftClient client,
            PlayerEntity player,
            ScreenHandler handler,
            List<Integer> slotIds
    ) {
        for (int targetIndex = 0; targetIndex < slotIds.size(); targetIndex++) {
            int targetId = slotIds.get(targetIndex);
            Slot target = handler.getSlot(targetId);
            ItemStack targetStack = target.getStack();
            if (targetStack.isEmpty() || targetStack.getCount() >= target.getMaxItemCount(targetStack)) {
                continue;
            }

            for (int sourceIndex = targetIndex + 1; sourceIndex < slotIds.size(); sourceIndex++) {
                int sourceId = slotIds.get(sourceIndex);
                Slot source = handler.getSlot(sourceId);
                ItemStack sourceStack = source.getStack();
                targetStack = target.getStack();
                if (targetStack.isEmpty()
                        || targetStack.getCount() >= target.getMaxItemCount(targetStack)
                        || sourceStack.isEmpty()
                        || !ItemStack.areItemsAndComponentsEqual(targetStack, sourceStack)
                        || !source.canTakeItems(player)
                        || !target.canInsert(sourceStack)) {
                    continue;
                }

                client.interactionManager.clickSlot(handler.syncId, sourceId, 0, SlotActionType.PICKUP, player);
                client.interactionManager.clickSlot(handler.syncId, targetId, 0, SlotActionType.PICKUP, player);
                if (!handler.getCursorStack().isEmpty()) {
                    client.interactionManager.clickSlot(handler.syncId, sourceId, 0, SlotActionType.PICKUP, player);
                }
            }
        }
    }

    private static void sortStacks(
            MinecraftClient client,
            PlayerEntity player,
            ScreenHandler handler,
            List<Integer> slotIds
    ) {
        List<ItemStack> sortedStacks = new ArrayList<>();
        for (int slotId : slotIds) {
            ItemStack stack = handler.getSlot(slotId).getStack();
            if (!stack.isEmpty()) {
                sortedStacks.add(stack.copy());
            }
        }
        sortedStacks.sort(Comparator
                .comparing((ItemStack stack) -> stack.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(stack -> Registries.ITEM.getId(stack.getItem()).toString()));

        for (int targetIndex = 0; targetIndex < sortedStacks.size(); targetIndex++) {
            int targetId = slotIds.get(targetIndex);
            ItemStack desiredStack = sortedStacks.get(targetIndex);
            if (ItemStack.areItemsAndComponentsEqual(handler.getSlot(targetId).getStack(), desiredStack)
                    && handler.getSlot(targetId).getStack().getCount() == desiredStack.getCount()) {
                continue;
            }

            int sourceId = -1;
            for (int searchIndex = targetIndex + 1; searchIndex < slotIds.size(); searchIndex++) {
                ItemStack candidate = handler.getSlot(slotIds.get(searchIndex)).getStack();
                if (ItemStack.areItemsAndComponentsEqual(candidate, desiredStack)
                        && candidate.getCount() == desiredStack.getCount()) {
                    sourceId = slotIds.get(searchIndex);
                    break;
                }
            }
            if (sourceId < 0) {
                continue;
            }

            client.interactionManager.clickSlot(handler.syncId, targetId, 0, SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(handler.syncId, sourceId, 0, SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(handler.syncId, targetId, 0, SlotActionType.PICKUP, player);
        }
    }
}
