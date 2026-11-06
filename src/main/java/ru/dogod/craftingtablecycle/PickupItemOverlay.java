package ru.dogod.craftingtablecycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class PickupItemOverlay {
    private static final int DISPLAY_TICKS = 70;
    private static final List<Notification> notifications = new ArrayList<>();

    private PickupItemOverlay() {
    }

    public static void reset() {
        notifications.clear();
    }

    public static void recordPickup(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }
        for (int index = notifications.size() - 1; index >= 0; index--) {
            Notification notification = notifications.get(index);
            if (ItemStack.areItemsAndComponentsEqual(notification.stack, stack)) {
                notification.count += count;
                notification.ticksRemaining = DISPLAY_TICKS;
                notifications.add(notifications.remove(index));
                trimNotifications();
                return;
            }
        }
        notifications.add(new Notification(stack, count));
        trimNotifications();
    }

    public static void tick() {
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            if (--iterator.next().ticksRemaining <= 0) {
                iterator.remove();
            }
        }
        trimNotifications();
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        boolean onRight = ModConfig.pickupHudOnRight;
        int screenWidth = context.getScaledWindowWidth();
        int y = context.getScaledWindowHeight() - 10;
        int visibleCount = Math.min(notifications.size(), ModConfig.pickupHudMaxEntries);
        for (int index = notifications.size() - 1; index >= notifications.size() - visibleCount; index--) {
            Notification notification = notifications.get(index);
            String countText = "x" + notification.count;
            int textWidth = client.textRenderer.getWidth(countText);
            int width = 22 + textWidth + 6;
            int height = 20;
            int x;
            int right;
            if (onRight) {
                right = screenWidth - 6;
                x = right - width;
            } else {
                x = 6;
                right = x + width;
            }
            int top = y - height;
            int rarityColor = rarityColor(notification.stack);
            context.fill(x, top, right, y, 0x990B0D10);
            context.fill(x, top, right, y, rarityTint(notification.stack));
            if (onRight) {
                context.fill(right - 2, top, right, y, rarityColor);
            } else {
                context.fill(x, top, x + 2, y, rarityColor);
            }
            context.fill(x, y - 1, right, y, rarityColor);
            context.drawItem(notification.stack, x + 3, top + 2);
            context.drawTextWithShadow(
                    client.textRenderer,
                    countText,
                    x + 22,
                    top + 6,
                    rarityColor
            );
            y -= height + 2;
        }
    }

    private static int rarityColor(ItemStack stack) {
        return switch (stack.getRarity()) {
            case UNCOMMON -> 0xFFFFFF55;
            case RARE -> 0xFF55FFFF;
            case EPIC -> 0xFFFF55FF;
            default -> 0xFFFFFFFF;
        };
    }

    private static int rarityTint(ItemStack stack) {
        return switch (stack.getRarity()) {
            case UNCOMMON -> 0x22FFFF55;
            case RARE -> 0x2255FFFF;
            case EPIC -> 0x22FF55FF;
            default -> 0x00000000;
        };
    }

    private static void trimNotifications() {
        while (notifications.size() > ModConfig.pickupHudMaxEntries) {
            notifications.remove(0);
        }
    }

    private static final class Notification {
        private final ItemStack stack;
        private int count;
        private int ticksRemaining = DISPLAY_TICKS;

        private Notification(ItemStack stack, int count) {
            this.stack = stack.copyWithCount(1);
            this.count = count;
        }
    }
}
