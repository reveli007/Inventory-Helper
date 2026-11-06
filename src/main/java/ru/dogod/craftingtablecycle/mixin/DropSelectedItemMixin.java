package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.dogod.craftingtablecycle.CraftingTableCycleClient;

@Mixin(ClientPlayerEntity.class)
public abstract class DropSelectedItemMixin {
    @Inject(method = "dropSelectedItem(Z)Z", at = @At("HEAD"))
    private void craftingTableCycle$recordDrop(boolean entireStack, CallbackInfoReturnable<Boolean> callback) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null) {
            CraftingTableCycleClient.recordDrop(player);
        }
    }
}
