package ru.dogod.craftingtablecycle.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

@Mod(InventoryHelper.MOD_ID)
public final class InventoryHelper {
    public static final String MOD_ID = "inventory_helper";

    public InventoryHelper(IEventBus modEventBus) {
        ModConfig.load();
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
    }
}
