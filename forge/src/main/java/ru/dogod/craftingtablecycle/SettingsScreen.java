package ru.dogod.craftingtablecycle;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 328;
    private static final int BUTTON_WIDTH = 150;
    private final Screen parent;
    private EditBox excludedItemsField;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Inventory Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int panelLeft = center - PANEL_WIDTH / 2;
        int left = panelLeft + 12;
        int right = left + BUTTON_WIDTH + 8;
        toggle(left, 56, "Armor HUD", () -> ModConfig.armorHud, v -> ModConfig.armorHud = v);
        toggle(right, 56, "Armor warning", () -> ModConfig.armorWarningSound, v -> ModConfig.armorWarningSound = v);
        toggle(left, 80, "Item count", () -> ModConfig.totalItemCount, v -> ModConfig.totalItemCount = v);
        toggle(right, 80, "Anvil cost", () -> ModConfig.anvilCostHud, v -> ModConfig.anvilCostHud = v);
        toggle(left, 104, "Effect timers", () -> ModConfig.effectDurationHud, v -> ModConfig.effectDurationHud = v);
        addRenderableWidget(Button.builder(Component.literal("Timer side: " + (ModConfig.effectDurationOnRight ? "Right" : "Left")), button -> {
            saveExcludedItems();
            ModConfig.effectDurationOnRight = !ModConfig.effectDurationOnRight;
            ModConfig.save();
            rebuildWidgets();
        }).bounds(right, 104, BUTTON_WIDTH, 20).build());
        toggle(left, 151, "Auto-refill", () -> ModConfig.autoRefill, v -> ModConfig.autoRefill = v);
        toggle(right, 151, "Armor swap", () -> ModConfig.armorQuickSwap, v -> ModConfig.armorQuickSwap = v);
        toggle(left, 175, "Refill on place", () -> ModConfig.refillOnPlace, v -> ModConfig.refillOnPlace = v);
        toggle(right, 175, "Refill on use", () -> ModConfig.refillOnUse, v -> ModConfig.refillOnUse = v);
        toggle(left, 199, "Refill on drop", () -> ModConfig.refillOnDrop, v -> ModConfig.refillOnDrop = v);
        addRenderableWidget(Button.builder(Component.literal("Durability warning: " + ModConfig.armorWarningPercent + "%"), button -> {
            saveExcludedItems();
            ModConfig.armorWarningPercent = ModConfig.armorWarningPercent >= 100 ? 10 : ModConfig.armorWarningPercent + 10;
            ModConfig.save();
            rebuildWidgets();
        }).bounds(left, 236, BUTTON_WIDTH, 20).build());
        excludedItemsField = new EditBox(font, left, 274, 304, 20, Component.literal("minecraft:torch, minecraft:dirt"));
        excludedItemsField.setMaxLength(2048);
        excludedItemsField.setValue(ModConfig.excludedItems);
        excludedItemsField.setHint(Component.literal("e.g. minecraft:torch, minecraft:dirt"));
        addRenderableWidget(excludedItemsField);
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(center - 75, height - 32, 150, 20).build());
    }

    private void toggle(int x, int y, String label, java.util.function.BooleanSupplier getter,
                        java.util.function.Consumer<Boolean> setter) {
        addRenderableWidget(Button.builder(toggleText(label, getter.getAsBoolean()), button -> {
            saveExcludedItems();
            setter.accept(!getter.getAsBoolean());
            ModConfig.save();
            rebuildWidgets();
        }).bounds(x, y, BUTTON_WIDTH, 20).build());
    }

    private static Component toggleText(String label, boolean enabled) {
        return Component.literal(label + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void onClose() {
        saveExcludedItems();
        ModConfig.save();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void saveExcludedItems() {
        if (excludedItemsField != null) ModConfig.excludedItems = excludedItemsField.getValue().trim();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.renderBackground(graphics, mouseX, mouseY, delta);
        int panelLeft = width / 2 - PANEL_WIDTH / 2;
        panel(graphics, panelLeft, 37, PANEL_WIDTH, 94);
        panel(graphics, panelLeft, 136, PANEL_WIDTH, 83);
        panel(graphics, panelLeft, 224, PANEL_WIDTH, 76);
    }

    private static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x50000000);
        graphics.fill(x, y, x + w, y + h, 0xFF3A424C);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xE51A1E24);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF45D483);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int center = width / 2;
        int panelLeft = center - PANEL_WIDTH / 2;
        graphics.drawCenteredString(font, title, center, 10, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Configure your HUD and inventory helpers", center, 24, 0xFFB8C0CC);
        graphics.drawString(font, "DISPLAY & HUD", panelLeft + 12, 43, 0xFF7FE0AA);
        graphics.drawString(font, "INVENTORY", panelLeft + 12, 142, 0xFF7FE0AA);
        graphics.drawString(font, "ALERTS & EXCLUSIONS", panelLeft + 12, 230, 0xFF7FE0AA);
        graphics.drawString(font, "Excluded item IDs (comma-separated)", panelLeft + 12, 258, 0xFFD4D9E0);
    }
}