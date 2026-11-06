package ru.dogod.craftingtablecycle;

import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class ArrowSelectionScreen extends Screen {
    private final Screen parent;
    private List<ItemStack> arrows = List.of();
    private int firstVisible;

    public ArrowSelectionScreen(Screen parent) {
        super(Text.literal("Select Arrow"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.client == null || this.client.player == null) {
            return;
        }

        arrows = CraftingTableCycleClient.getAvailableArrows(this.client.player);
        int rowHeight = 26;
        int panelWidth = 224;
        int panelHeight = panelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        int visibleRows = visibleRows();
        for (int index = firstVisible; index < Math.min(arrows.size(), firstVisible + visibleRows); index++) {
            ItemStack arrow = arrows.get(index);
            int buttonX = left + 12;
            int buttonY = top + 34 + (index - firstVisible) * rowHeight;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(arrow.getName().getString() + "  x" + countArrow(arrow)),
                    button -> {
                        CraftingTableCycleClient.selectArrow(arrow);
                        close();
                    }).dimensions(buttonX + 24, buttonY, panelWidth - 48, 22).build());
        }
    }

    private int countArrow(ItemStack arrow) {
        if (this.client == null || this.client.player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = this.client.player.getInventory().getStack(slot);
            if (ItemStack.areItemsAndComponentsEqual(stack, arrow)) {
                count += stack.getCount();
            }
        }
        ItemStack offHand = this.client.player.getOffHandStack();
        if (ItemStack.areItemsAndComponentsEqual(offHand, arrow)) {
            count += offHand.getCount();
        }
        return count;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxFirstVisible = Math.max(0, arrows.size() - visibleRows());
        if (verticalAmount < 0 && firstVisible < maxFirstVisible) {
            firstVisible++;
            clearAndInit();
            return true;
        }
        if (verticalAmount > 0 && firstVisible > 0) {
            firstVisible--;
            clearAndInit();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = 224;
        int panelHeight = panelHeight();
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        context.fill(left - 3, top - 3, left + panelWidth + 3, top + panelHeight + 3, 0x99000000);
        context.fill(left, top, left + panelWidth, top + panelHeight, 0xB0181B20);
        context.fill(left, top, left + panelWidth, top + 2, 0xCC45D483);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int panelHeight = panelHeight();
        int top = (this.height - panelHeight) / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top + 9, 0xFFFFFFFF);
        if (arrows.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("No arrows in your inventory"),
                    this.width / 2,
                    top + 32,
                    0xFFB8C0CC
            );
            return;
        }

        int left = (this.width - 224) / 2;
        int visibleRows = visibleRows();
        for (int index = firstVisible; index < Math.min(arrows.size(), firstVisible + visibleRows); index++) {
            context.drawItem(
                    arrows.get(index).copyWithCount(1),
                    left + 20,
                    top + 37 + (index - firstVisible) * 26
            );
        }
        if (arrows.size() > visibleRows) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal((firstVisible + 1) + "-" + Math.min(arrows.size(), firstVisible + visibleRows)
                            + " / " + arrows.size() + "  (scroll)"),
                    this.width / 2,
                    top + panelHeight - 12,
                    0xFFB8C0CC
            );
        }
    }

    private int visibleRows() {
        return Math.max(1, Math.min(8, (this.height - 90) / 26));
    }

    private int panelHeight() {
        return 50 + Math.max(1, Math.min(arrows.size(), visibleRows())) * 26;
    }
}
