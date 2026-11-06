package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.ModConfig;

@Mixin(Screen.class)
public abstract class AnvilCostIndicatorMixin {
    private static final int VANILLA_TOO_EXPENSIVE_LEVELS = 40;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void inventoryHelper$renderAnvilCost(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo callback
    ) {
        if (!ModConfig.anvilCostHud) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof AnvilScreen)
                || client.player == null
                || !(client.player.currentScreenHandler instanceof AnvilScreenHandler handler)) {
            return;
        }

        int cost = handler.getLevelCost();
        int remaining = Math.max(0, VANILLA_TOO_EXPENSIVE_LEVELS - cost);
        int panelX = context.getScaledWindowWidth() / 2 - 86;
        int panelY = context.getScaledWindowHeight() / 2 - 106;
        int panelWidth = 172;
        int panelHeight = 25;
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD0101010);
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("Anvil cost: " + cost + " levels"),
                panelX + 4,
                panelY + 3,
                0xFFFFFFFF
        );

        boolean tooExpensive = cost >= VANILLA_TOO_EXPENSIVE_LEVELS;
        Text remainingText = tooExpensive
                ? Text.literal("Too Expensive!")
                : Text.literal("Too Expensive in: " + remaining + " levels");
        context.drawTextWithShadow(
                client.textRenderer,
                remainingText,
                panelX + 4,
                panelY + 14,
                tooExpensive ? 0xFFFF5555 : 0xFFFFD060
        );
    }
}
