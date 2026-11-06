package ru.dogod.craftingtablecycle.neoforge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class InventoryHelperSettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private final Screen parent;
    private EditBox excludedItems;
    private int excludedItemsLabelY;

    public InventoryHelperSettingsScreen(Screen parent) {
        super(Component.literal("Inventory Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - 154;
        int right = left + BUTTON_WIDTH + 8;
        int y = Math.max(40, (height - 238) / 2);
        toggle(left, y, "Armor HUD", () -> ModConfig.armorHud, value -> ModConfig.armorHud = value);
        toggle(right, y, "Armor warning sound", () -> ModConfig.armorWarningSound,
                value -> ModConfig.armorWarningSound = value);
        toggle(left, y += 24, "Total item count", () -> ModConfig.totalItemCount,
                value -> ModConfig.totalItemCount = value);
        toggle(right, y, "Anvil cost", () -> ModConfig.anvilCostHud,
                value -> ModConfig.anvilCostHud = value);
        toggle(left, y += 24, "Effect timers", () -> ModConfig.effectDurationHud,
                value -> ModConfig.effectDurationHud = value);
        addRenderableWidget(Button.builder(timerSideText(), button -> {
            saveExcludedItems();
            ModConfig.effectDurationOnRight = !ModConfig.effectDurationOnRight;
            ModConfig.save();
            clearWidgets();
            init();
        }).bounds(right, y, BUTTON_WIDTH, 20).build());
        toggle(left, y += 30, "Auto-refill", () -> ModConfig.autoRefill,
                value -> ModConfig.autoRefill = value);
        toggle(right, y, "Right-click armor swap", () -> ModConfig.armorQuickSwap,
                value -> ModConfig.armorQuickSwap = value);
        toggle(left, y += 24, "Refill on block place", () -> ModConfig.refillOnPlace,
                value -> ModConfig.refillOnPlace = value);
        toggle(right, y, "Refill on item use", () -> ModConfig.refillOnUse,
                value -> ModConfig.refillOnUse = value);
        toggle(left, y += 24, "Refill on drop", () -> ModConfig.refillOnDrop,
                value -> ModConfig.refillOnDrop = value);
        addRenderableWidget(Button.builder(warningText(), button -> {
            saveExcludedItems();
            ModConfig.armorWarningPercent = ModConfig.armorWarningPercent >= 100
                    ? 10 : ModConfig.armorWarningPercent + 10;
            ModConfig.save();
            clearWidgets();
            init();
        }).bounds(left, y += 30, BUTTON_WIDTH, 20).build());
        excludedItemsLabelY = y + 7;
        excludedItems = new EditBox(font, left, y + 20, 308, 20, Component.literal("Excluded item IDs"));
        excludedItems.setMaxLength(2048);
        excludedItems.setValue(ModConfig.excludedItems);
        excludedItems.setHint(Component.literal("minecraft:torch, minecraft:dirt"));
        addRenderableWidget(excludedItems);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(width / 2 - 75, height - 30, 150, 20).build());
    }

    private void toggle(int x, int y, String label, java.util.function.BooleanSupplier getter,
                        java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(toggleText(label, getter.getAsBoolean()), button -> {
            saveExcludedItems();
            setter.accept(!getter.getAsBoolean());
            ModConfig.save();
            clearWidgets();
            init();
        }).bounds(x, y, BUTTON_WIDTH, 20).build());
    }

    private static Component toggleText(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "ON" : "OFF"));
    }

    private static Component timerSideText() {
        return Component.literal("Timer side: " + (ModConfig.effectDurationOnRight ? "Right" : "Left"));
    }

    private static Component warningText() {
        return Component.literal("Durability warning: " + ModConfig.armorWarningPercent + "%");
    }

    private void saveExcludedItems() {
        if (excludedItems != null) {
            ModConfig.excludedItems = excludedItems.getValue().trim();
        }
    }

    @Override
    public void onClose() {
        saveExcludedItems();
        ModConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Configure your HUD and inventory helpers", width / 2, 28, 0xFFB8C0CC);
        graphics.drawString(font, "Excluded item IDs (comma-separated)",
                width / 2 - 154, excludedItemsLabelY, 0xFFD4D9E0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
