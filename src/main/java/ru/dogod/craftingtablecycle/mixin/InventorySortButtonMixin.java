package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ru.dogod.craftingtablecycle.InventorySorter;

@Mixin(HandledScreen.class)
public abstract class InventorySortButtonMixin {
    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Inject(method = "init", at = @At("TAIL"))
    private void inventoryHelper$addSortButton(CallbackInfo callback) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (!(screen instanceof InventoryScreen)
                && !(screen instanceof GenericContainerScreen)
                && !(screen instanceof ShulkerBoxScreen)) {
            return;
        }

        ((ScreenAccessorMixin) (Object) screen).inventoryHelper$addDrawableChild(ButtonWidget.builder(
                Text.literal("S"),
                button -> InventorySorter.sort(screen)
        ).dimensions(x + backgroundWidth - 15, y + 4, 11, 11).build());
    }
}
