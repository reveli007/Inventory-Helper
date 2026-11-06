package ru.dogod.craftingtablecycle.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import ru.dogod.craftingtablecycle.CraftingTableCycleClient;

@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer",
        remap = false
)
public abstract class SodiumBlockRendererMixin {
    @ModifyVariable(
            method = "renderModel",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private BlockStateModel craftingTableCycle$replaceModel(
            BlockStateModel model,
            @Local(argsOnly = true) BlockState state
    ) {
        if (!CraftingTableCycleClient.shouldRenderAsCraftingTable(state)) {
            return model;
        }
        return MinecraftClient.getInstance()
                .getBlockRenderManager()
                .getModel(Blocks.CRAFTING_TABLE.getDefaultState());
    }

    @ModifyVariable(
            method = "renderModel",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private BlockState craftingTableCycle$replaceState(BlockState state) {
        return CraftingTableCycleClient.shouldRenderAsCraftingTable(state)
                ? Blocks.CRAFTING_TABLE.getDefaultState()
                : state;
    }
}
