package ru.dogod.craftingtablecycle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.Identifier;

public final class CraftingTableCycleHud {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");

    private CraftingTableCycleHud() {
    }

    public static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        int hotbarLeft = context.getScaledWindowWidth() / 2 - 91;
        int hotbarTop = context.getScaledWindowHeight() - 22;
        if (ModConfig.totalItemCount) {
            renderTotalItemCount(context, client, hotbarLeft, hotbarTop);
        }
        if (ModConfig.armorHud) {
            renderArmor(context, client);
        }
        BlockInfoOverlay.render(context, client);
        PickupItemOverlay.render(context, client);
    }

    public static void renderEffectOverlay(
            DrawContext context,
            net.minecraft.client.render.RenderTickCounter tickCounter
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModConfig.effectDurationHud
                || client.player == null
                || client.options.hudHidden
                || (client.currentScreen != null && client.currentScreen.showsStatusEffects())) {
            return;
        }
        renderEffectDurations(context, client);
    }

    private static void renderEffectDurations(DrawContext context, MinecraftClient client) {
        int screenWidth = context.getScaledWindowWidth();
        int y = 8;
        boolean rightAligned = ModConfig.effectDurationOnRight;

        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (!effect.shouldShowIcon()) {
                continue;
            }

            String duration = effect.isInfinite() ? "∞" : formatDuration(effect.getDuration());
            int textWidth = client.textRenderer.getWidth(duration);
            int rowWidth = 24 + 6 + textWidth + 8;
            int x = rightAligned ? screenWidth - rowWidth - 8 : 8;
            int iconX = rightAligned ? x + rowWidth - 24 : x;
            int textX = rightAligned ? x + 4 : iconX + 30;
            boolean expiring = !effect.isInfinite() && effect.getDuration() <= 100;
            boolean blinkOn = ((System.currentTimeMillis() / 300L) & 1L) == 0L;
            int textColor = expiring && blinkOn ? 0xFFFF3030 : 0xFFFFFFFF;

            context.fill(x, y, x + rowWidth, y + 24, 0x99000000);
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    InGameHud.getEffectTexture(effect.getEffectType()),
                    iconX,
                    y + 3,
                    18,
                    18
            );
            context.drawTextWithShadow(
                    client.textRenderer,
                    duration,
                    textX,
                    y + 7,
                    textColor
            );
            y += 24;
        }
    }

    private static String formatDuration(int durationTicks) {
        int totalSeconds = Math.max(0, (durationTicks + 19) / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes > 0 ? String.format("%d:%02d", minutes, seconds) : seconds + "s";
    }

    private static void renderTotalItemCount(
            DrawContext context,
            MinecraftClient client,
            int hotbarLeft,
            int hotbarTop
    ) {
        ItemStack heldStack = client.player.getMainHandStack();
        if (heldStack.getItem() instanceof RangedWeaponItem) {
            renderArrowCount(context, client, heldStack, hotbarLeft, hotbarTop);
            return;
        }

        ItemStack selectedStack = heldStack;
        if (selectedStack.isEmpty() || selectedStack.getMaxCount() <= 1) {
            return;
        }

        int totalCount = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (ItemStack.areItemsAndComponentsEqual(stack, selectedStack)) {
                totalCount += stack.getCount();
            }
        }

        String countText = Integer.toString(totalCount);
        int textWidth = client.textRenderer.getWidth(countText);
        int firstArmorSlotX = hotbarLeft - 29 - ARMOR_SLOTS.length * 20;
        int right = firstArmorSlotX - 4;
        int x = right - textWidth - 6;
        int y = hotbarTop + 11;
        context.fill(x - 2, y - 1, right - 1, y + 10, 0xB0000000);
        context.drawTextWithShadow(client.textRenderer, countText, x, y, 0xFFFFFFFF);
    }

    private static void renderArrowCount(
            DrawContext context,
            MinecraftClient client,
            ItemStack weapon,
            int hotbarLeft,
            int hotbarTop
    ) {
        ItemStack arrow = CraftingTableCycleClient.getSelectedArrow(client.player, weapon);
        if (arrow.isEmpty()) {
            java.util.List<ItemStack> availableArrows = CraftingTableCycleClient.getAvailableArrows(client.player);
            if (availableArrows.isEmpty()) {
                return;
            }
            arrow = availableArrows.getFirst();
        }

        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (ItemStack.areItemsAndComponentsEqual(stack, arrow)) {
                count += stack.getCount();
            }
        }
        ItemStack offHand = client.player.getOffHandStack();
        if (ItemStack.areItemsAndComponentsEqual(offHand, arrow)) {
            count += offHand.getCount();
        }

        String countText = Integer.toString(count);
        int textWidth = client.textRenderer.getWidth(countText);
        int width = 18 + textWidth + 8;
        int firstArmorSlotX = hotbarLeft - 29 - ARMOR_SLOTS.length * 20;
        int right = firstArmorSlotX - 4;
        int x = right - width;
        int y = hotbarTop + 11;
        context.fill(x - 2, y - 1, right - 1, y + 10, 0xB0000000);
        context.drawItem(arrow.copyWithCount(1), x, y - 4);
        context.drawTextWithShadow(client.textRenderer, countText, x + 19, y, 0xFFFFFFFF);
    }

    private static void renderArmor(DrawContext context, MinecraftClient client) {
        int hotbarLeft = context.getScaledWindowWidth() / 2 - 91;
        int hotbarTop = context.getScaledWindowHeight() - 22;
        int firstSlotX = hotbarLeft - 29 - ARMOR_SLOTS.length * 20;
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            int x = firstSlotX + index * 20;
            ItemStack stack = client.player.getEquippedStack(ARMOR_SLOTS[index]);
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED,
                    HOTBAR_TEXTURE,
                    182,
                    22,
                    1 + index * 20,
                    0,
                    x,
                    hotbarTop,
                    20,
                    22
            );
            if (stack.isEmpty()) {
                continue;
            }

            context.drawItem(stack, x + 2, hotbarTop + 3);
            context.drawStackOverlay(client.textRenderer, stack, x + 2, hotbarTop + 3);
        }
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_TEXTURE,
                182,
                22,
                0,
                0,
                firstSlotX - 1,
                hotbarTop,
                1,
                22
        );
        context.drawGuiTexture(
                RenderPipelines.GUI_TEXTURED,
                HOTBAR_TEXTURE,
                182,
                22,
                ARMOR_SLOTS.length * 20 + 1,
                0,
                firstSlotX + ARMOR_SLOTS.length * 20,
                hotbarTop,
                1,
                22
        );
    }
}
