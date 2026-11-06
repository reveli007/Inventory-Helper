package ru.dogod.craftingtablecycle;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class CraftingTableCycleSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 328;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_GAP = 8;

    private final Screen parent;
    private TextFieldWidget excludedItemsField;

    public CraftingTableCycleSettingsScreen(Screen parent) {
        super(Text.literal("Inventory Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int panelLeft = center - PANEL_WIDTH / 2;
        int left = panelLeft + 12;
        int right = left + BUTTON_WIDTH + BUTTON_GAP;

        addToggle(left, 54, "Armor HUD", () -> ModConfig.armorHud,
                value -> ModConfig.armorHud = value);
        addToggle(right, 54, "Durability sound", () -> ModConfig.armorWarningSound,
                value -> ModConfig.armorWarningSound = value);
        addToggle(left, 78, "Item count", () -> ModConfig.totalItemCount,
                value -> ModConfig.totalItemCount = value);
        addToggle(right, 78, "Anvil cost", () -> ModConfig.anvilCostHud,
                value -> ModConfig.anvilCostHud = value);
        addToggle(left, 102, "Effect timers", () -> ModConfig.effectDurationHud,
                value -> ModConfig.effectDurationHud = value);
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Timer side: " + (ModConfig.effectDurationOnRight ? "Right" : "Left")),
                button -> {
                    saveExcludedItems();
                    ModConfig.effectDurationOnRight = !ModConfig.effectDurationOnRight;
                    ModConfig.save();
                    clearAndInit();
                }).dimensions(right, 102, BUTTON_WIDTH, 20).build());

        addToggle(left, 126, "Block info", () -> ModConfig.blockInfoHud,
                value -> ModConfig.blockInfoHud = value);
        addToggle(right, 126, "Dim behind inventory", () -> ModConfig.inventoryBackgroundDim,
                value -> ModConfig.inventoryBackgroundDim = value);

        addToggle(left, 175, "Auto-refill", () -> ModConfig.autoRefill,
                value -> ModConfig.autoRefill = value);
        addToggle(right, 175, "Armor swap", () -> ModConfig.armorQuickSwap,
                value -> ModConfig.armorQuickSwap = value);
        addToggle(left, 199, "Refill on place", () -> ModConfig.refillOnPlace,
                value -> ModConfig.refillOnPlace = value);
        addToggle(right, 199, "Refill on use", () -> ModConfig.refillOnUse,
                value -> ModConfig.refillOnUse = value);
        addToggle(left, 223, "Refill on drop", () -> ModConfig.refillOnDrop,
                value -> ModConfig.refillOnDrop = value);

        addDrawableChild(new SliderWidget(
                left,
                266,
                304,
                20,
                Text.empty(),
                (ModConfig.armorWarningPercent - 1) / 99.0
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Low durability threshold: "
                        + Math.clamp((int) Math.round(value * 99) + 1, 1, 100) + "%"));
            }

            @Override
            protected void applyValue() {
                ModConfig.armorWarningPercent = Math.clamp((int) Math.round(value * 99) + 1, 1, 100);
            }
        });
        addDrawableChild(new SliderWidget(
                left,
                291,
                BUTTON_WIDTH,
                20,
                Text.empty(),
                (ModConfig.pickupHudMaxEntries - 1) / 9.0
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Pickups: "
                        + (Math.clamp((int) Math.round(value * 9) + 1, 1, 10))));
            }

            @Override
            protected void applyValue() {
                ModConfig.pickupHudMaxEntries = Math.clamp((int) Math.round(value * 9) + 1, 1, 10);
            }
        });
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Pickup side: " + (ModConfig.pickupHudOnRight ? "Right" : "Left")),
                button -> {
                    saveExcludedItems();
                    ModConfig.pickupHudOnRight = !ModConfig.pickupHudOnRight;
                    ModConfig.save();
                    clearAndInit();
                }).dimensions(right, 267, BUTTON_WIDTH, 20).build());

        excludedItemsField = new TextFieldWidget(this.textRenderer, left, 334, 304, 20,
                Text.literal("minecraft:torch, minecraft:dirt"));
        excludedItemsField.setMaxLength(2048);
        excludedItemsField.setText(ModConfig.excludedItems);
        excludedItemsField.setPlaceholder(Text.literal("e.g. minecraft:torch, minecraft:dirt"));
        addDrawableChild(excludedItemsField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(center - 75, this.height - 32, 150, 20).build());
    }

    private void addToggle(
            int x,
            int y,
            String label,
            java.util.function.BooleanSupplier getter,
            java.util.function.Consumer<Boolean> setter
    ) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), button -> {
            saveExcludedItems();
            setter.accept(!getter.getAsBoolean());
            ModConfig.save();
            clearAndInit();
        }).dimensions(x, y, 150, 20).build());
    }

    private static Text toggleText(String label, boolean enabled) {
        return Text.literal(label + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void close() {
        saveExcludedItems();
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private void saveExcludedItems() {
        if (excludedItemsField != null) {
            ModConfig.excludedItems = excludedItemsField.getText().trim();
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        int panelLeft = this.width / 2 - PANEL_WIDTH / 2;
        drawPanel(context, panelLeft, 35, PANEL_WIDTH, 116);
        drawPanel(context, panelLeft, 156, PANEL_WIDTH, 88);
        drawPanel(context, panelLeft, 244, PANEL_WIDTH, 118);
    }

    private static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x50000000);
        context.fill(x, y, x + width, y + height, 0xFF3A424C);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xE51A1E24);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF45D483);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int center = this.width / 2;
        int panelLeft = center - PANEL_WIDTH / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Configure your HUD and inventory helpers"),
                center,
                24,
                0xFFB8C0CC
        );
        drawSectionTitle(context, panelLeft + 12, 41, "DISPLAY & HUD");
        drawSectionTitle(context, panelLeft + 12, 166, "INVENTORY");
        drawSectionTitle(context, panelLeft + 12, 250, "ALERTS & EXCLUSIONS");
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Excluded item IDs (comma-separated)"),
                panelLeft + 12,
                320,
                0xFFD4D9E0
        );
    }

    private void drawSectionTitle(DrawContext context, int x, int y, String label) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), x, y, 0xFF7FE0AA);
    }
}
