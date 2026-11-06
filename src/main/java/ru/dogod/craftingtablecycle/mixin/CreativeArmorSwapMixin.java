package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
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

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeArmorSwapMixin {
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
                || clickedSlot == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        PlayerScreenHandler handler = player.playerScreenHandler;
        if (handler == null || !handler.getCursorStack().isEmpty()) {
            return;
        }

        int sourceId = toPlayerSlotId(handler, clickedSlot);
        if (sourceId < 9 || sourceId >= handler.slots.size()) {
            return;
        }

        Slot sourceSlot = handler.getSlot(sourceId);
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

        handler.onSlotClick(sourceId, 0, SlotActionType.PICKUP, player);
        handler.onSlotClick(armorSlotId, 0, SlotActionType.PICKUP, player);
        if (!handler.getCursorStack().isEmpty()) {
            handler.onSlotClick(sourceId, 0, SlotActionType.PICKUP, player);
        }
        handler.sendContentUpdates();
        callback.cancel();
    }

    private static int toPlayerSlotId(PlayerScreenHandler handler, Slot clickedSlot) {
        if (clickedSlot instanceof CreativeSlotAccessor accessor) {
            Slot wrapped = accessor.inventoryHelper$getWrappedSlot();
            if (wrapped != null
                    && wrapped.id >= 0
                    && wrapped.id < handler.slots.size()
                    && handler.getSlot(wrapped.id) == wrapped) {
                return wrapped.id;
            }
            return -1;
        }
        for (Slot candidate : handler.slots) {
            if (candidate.inventory == clickedSlot.inventory
                    && candidate.getIndex() == clickedSlot.getIndex()) {
                return candidate.id;
            }
        }
        return -1;
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
