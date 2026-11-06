package ru.dogod.craftingtablecycle;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(InventoryHelperMod.MOD_ID)
public final class InventoryHelperMod {
    public static final String MOD_ID = "inventory_helper";

    public InventoryHelperMod() {
        ModConfig.load();
        MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }
}