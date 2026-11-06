package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.CraftingTableCycleHud;
import ru.dogod.craftingtablecycle.ModConfig;

@Mixin(InGameHud.class)
public abstract class StatusEffectHudMixin {
    @Inject(
            method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inventoryHelper$replaceVanillaEffectIcons(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo callback
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (ModConfig.effectDurationHud
                && (client.currentScreen == null || !client.currentScreen.showsStatusEffects())) {
            CraftingTableCycleHud.renderEffectOverlay(context, tickCounter);
            callback.cancel();
        }
    }
}
