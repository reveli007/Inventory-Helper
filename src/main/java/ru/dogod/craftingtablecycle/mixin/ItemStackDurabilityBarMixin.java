package ru.dogod.craftingtablecycle.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ru.dogod.craftingtablecycle.ModConfig;

@Mixin(Item.class)
public abstract class ItemStackDurabilityBarMixin {
    @Inject(method = "getItemBarColor(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void inventoryHelper$markLowDurabilityRed(
            ItemStack stack,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (stack.isDamageable()
                && (stack.getMaxDamage() - stack.getDamage()) * 100 / stack.getMaxDamage()
                        <= ModConfig.armorWarningPercent) {
            callback.setReturnValue(0xFF3333);
        }
    }
}
