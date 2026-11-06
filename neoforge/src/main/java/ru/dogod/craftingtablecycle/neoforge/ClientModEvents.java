package ru.dogod.craftingtablecycle.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientModEvents {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(InventoryHelper.MOD_ID, "controls")
    );
    private static final KeyMapping SETTINGS_KEY = new KeyMapping(
            "key.inventory_helper.settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    );

    private ClientModEvents() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SETTINGS_KEY);
        ClientEvents.setSettingsKey(SETTINGS_KEY);
    }
}
