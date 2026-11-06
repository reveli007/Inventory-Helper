package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.ModConfig;

@Mixin(HandledScreen.class)
public abstract class RightClickArmorSwapMixin {
    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inventoryHelper$swapArmorOnRightClick(
            Slot clickedSlot,
            int slotId,
            int button,
            SlotActionType actionType,
            CallbackInfo callback
    ) {
        if (!ModConfig.armorQuickSwap
                || button != 1
                || actionType != SlotActionType.PICKUP
                || !((Object) this instanceof InventoryScreen)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null
                || client.interactionManager == null
                || player.currentScreenHandler != player.playerScreenHandler
                || slotId < 9
                || slotId >= player.playerScreenHandler.slots.size()
                || !player.playerScreenHandler.getCursorStack().isEmpty()) {
            return;
        }

        PlayerScreenHandler handler = player.playerScreenHandler;
        Slot sourceSlot = handler.getSlot(slotId);
        ItemStack replacement = sourceSlot.getStack();
        EquippableComponent equippable = replacement.get(DataComponentTypes.EQUIPPABLE);
        if (replacement.isEmpty() || equippable == null) {
            return;
        }

        int armorSlotId = getArmorSlotId(equippable.slot());
        if (armorSlotId < 0) {
            return;
        }

        Slot armorSlot = handler.getSlot(armorSlotId);
        if (!sourceSlot.canTakeItems(player)
                || !armorSlot.canTakeItems(player)
                || !armorSlot.canInsert(replacement)) {
            return;
        }

        client.interactionManager.clickSlot(
                handler.syncId, slotId, 0, SlotActionType.PICKUP, player
        );
        client.interactionManager.clickSlot(
                handler.syncId, armorSlotId, 0, SlotActionType.PICKUP, player
        );
        if (!handler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(
                    handler.syncId, slotId, 0, SlotActionType.PICKUP, player
            );
        }
        callback.cancel();
    }

    private static int getArmorSlotId(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }
}
