package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.PickupItemOverlay;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ItemPickupOverlayMixin {
    @Inject(
            method = "onItemPickupAnimation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;decrement(I)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void inventoryHelper$showPickedUpItem(
            ItemPickupAnimationS2CPacket packet,
            CallbackInfo callback
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null
                || packet.getCollectorEntityId() != client.player.getId()) {
            return;
        }

        if (client.world.getEntityById(packet.getEntityId()) instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            PickupItemOverlay.recordPickup(stack, Math.min(packet.getStackAmount(), stack.getCount()));
        }
    }
}
