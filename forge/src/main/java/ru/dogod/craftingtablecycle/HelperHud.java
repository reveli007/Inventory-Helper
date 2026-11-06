package ru.dogod.craftingtablecycle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

public final class HelperHud {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private HelperHud() {
    }

    public static void render(GuiGraphics graphics, Minecraft minecraft) {
        if (ModConfig.armorHud) renderArmor(graphics, minecraft);
        if (ModConfig.totalItemCount) renderTotalCount(graphics, minecraft);
        if (ModConfig.effectDurationHud) renderEffects(graphics, minecraft);
    }

    private static void renderArmor(GuiGraphics graphics, Minecraft minecraft) {
        int left = graphics.guiWidth() / 2 - 91 - 29 - ARMOR.length * 20;
        int top = graphics.guiHeight() - 22;
        for (int i = 0; i < ARMOR.length; i++) {
            int x = left + i * 20;
            graphics.fill(x, top, x + 20, top + 22, 0x99000000);
            graphics.fill(x, top, x + 20, top + 1, 0xAAFFFFFF);
            ItemStack item = minecraft.player.getItemBySlot(ARMOR[i]);
            if (!item.isEmpty()) {
                graphics.renderItem(item, x + 2, top + 3);
                graphics.renderItemDecorations(minecraft.font, item, x + 2, top + 3);
            }
        }
    }

    private static void renderTotalCount(GuiGraphics graphics, Minecraft minecraft) {
        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty() || held.getMaxStackSize() <= 1) return;
        int total = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, held)) total += stack.getCount();
        }
        String text = Integer.toString(total);
        int right = graphics.guiWidth() / 2 - 91 - 29 - ARMOR.length * 20 - 4;
        int x = right - minecraft.font.width(text) - 6;
        int y = graphics.guiHeight() - 11;
        graphics.fill(x - 2, y - 1, right - 1, y + 10, 0xB0000000);
        graphics.drawString(minecraft.font, text, x, y, 0xFFFFFFFF, true);
    }

    private static void renderEffects(GuiGraphics graphics, Minecraft minecraft) {
        int y = 8;
        int screenWidth = graphics.guiWidth();
        boolean right = ModConfig.effectDurationOnRight;
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (!effect.showIcon()) continue;
            int ticks = effect.getDuration();
            String duration = effect.isInfiniteDuration() ? "∞" : formatDuration(ticks);
            String name = effect.getEffect().value().getDisplayName().getString();
            int durationWidth = minecraft.font.width(duration);
            int nameWidth = minecraft.font.width(name);
            int rowWidth = durationWidth + nameWidth + 16;
            int x = right ? screenWidth - rowWidth - 8 : 8;
            int nameX = right ? x + durationWidth + 12 : x + 6;
            int durationX = right ? x + 6 : x + nameWidth + 10;
            boolean flash = !effect.isInfiniteDuration() && ticks <= 100
                    && (System.currentTimeMillis() / 300L & 1L) == 0;
            graphics.fill(x, y, x + rowWidth, y + 24, 0x99000000);
            graphics.drawString(minecraft.font, name, nameX, y + 7, 0xFFFFFFFF, true);
            graphics.drawString(minecraft.font, duration, durationX, y + 7,
                    flash ? 0xFFFF3030 : 0xFFFFFFFF, true);
            y += 24;
        }
    }
    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        int minutes = seconds / 60;
        return minutes > 0 ? String.format("%d:%02d", minutes, seconds % 60) : seconds + "s";
    }

    public static void renderAnvilCost(GuiGraphics graphics, Minecraft minecraft, int cost) {
        int x = graphics.guiWidth() / 2 - 86;
        int y = graphics.guiHeight() / 2 - 106;
        graphics.fill(x, y, x + 172, y + 25, 0xD0101010);
        graphics.drawString(minecraft.font, "Anvil cost: " + cost + " levels", x + 4, y + 3, 0xFFFFFFFF, true);
        boolean tooExpensive = cost >= 40;
        String text = tooExpensive ? "Too Expensive!" : "Too Expensive in: " + (40 - cost) + " levels";
        graphics.drawString(minecraft.font, text, x + 4, y + 14, tooExpensive ? 0xFFFF5555 : 0xFFFFD060, true);
    }
}