package ru.dogod.craftingtablecycle;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class BlockInfoOverlay {
    private BlockInfoOverlay() {
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (!ModConfig.blockInfoHud
                || client.player == null
                || client.world == null
                || client.options.hudHidden
                || client.currentScreen != null) {
            return;
        }

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) {
            return;
        }

        Block block = state.getBlock();
        Text blockName = block.getName();
        String namespace = Registries.BLOCK.getId(block).getNamespace();
        String modName = FabricLoader.getInstance().getModContainer(namespace)
                .map(container -> container.getMetadata().getName())
                .orElse(namespace);

        ItemStack icon = new ItemStack(block.asItem());
        boolean hasIcon = !icon.isEmpty();

        int pad = 5;
        int iconSize = 16;
        int nameWidth = client.textRenderer.getWidth(blockName);
        int modWidth = client.textRenderer.getWidth(modName);
        int textWidth = Math.max(nameWidth, modWidth);
        int textLeft = hasIcon ? pad + iconSize + pad : pad;
        int width = textLeft + textWidth + pad;
        int height = 30;

        int x = context.getScaledWindowWidth() / 2 - width / 2;
        int y = 8;

        context.fill(x, y, x + width, y + height, 0x990B0D10);
        if (hasIcon) {
            context.drawItem(icon, x + pad, y + (height - iconSize) / 2);
        }
        context.drawTextWithShadow(
                client.textRenderer,
                blockName,
                x + textLeft,
                y + 5,
                0xFFFFFFFF
        );
        context.drawTextWithShadow(
                client.textRenderer,
                modName,
                x + textLeft,
                y + 17,
                0xFF8FB8E8
        );
    }
}
