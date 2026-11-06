package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.ModConfig;

@Mixin(HandledScreen.class)
public abstract class InventoryBackgroundDimMixin {
    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void inventoryHelper$dimInventoryBackground(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo callback
    ) {
        if (ModConfig.inventoryBackgroundDim && (Object) this instanceof InventoryScreen) {
            context.fill(
                    0,
                    0,
                    context.getScaledWindowWidth(),
                    context.getScaledWindowHeight(),
                    0x66000000
            );
        }
    }
}
