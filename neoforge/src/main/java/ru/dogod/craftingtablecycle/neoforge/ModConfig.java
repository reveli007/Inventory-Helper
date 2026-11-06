package ru.dogod.craftingtablecycle.neoforge;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

final class ModConfig {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("inventory_helper.properties");

    static boolean armorHud = true;
    static boolean armorWarningSound = true;
    static boolean totalItemCount = true;
    static boolean anvilCostHud = true;
    static boolean effectDurationHud = true;
    static boolean effectDurationOnRight = true;
    static boolean armorQuickSwap = true;
    static boolean autoRefill = true;
    static boolean refillOnPlace = true;
    static boolean refillOnUse = true;
    static boolean refillOnDrop = true;
    static int armorWarningPercent = 40;
    static String excludedItems = "";

    private ModConfig() {
    }

    static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            armorHud = bool(properties, "armorHud", armorHud);
            armorWarningSound = bool(properties, "armorWarningSound", armorWarningSound);
            totalItemCount = bool(properties, "totalItemCount", totalItemCount);
            anvilCostHud = bool(properties, "anvilCostHud", anvilCostHud);
            effectDurationHud = bool(properties, "effectDurationHud", effectDurationHud);
            effectDurationOnRight = bool(properties, "effectDurationOnRight", effectDurationOnRight);
            armorQuickSwap = bool(properties, "armorQuickSwap", armorQuickSwap);
            autoRefill = bool(properties, "autoRefill", autoRefill);
            refillOnPlace = bool(properties, "refillOnPlace", refillOnPlace);
            refillOnUse = bool(properties, "refillOnUse", refillOnUse);
            refillOnDrop = bool(properties, "refillOnDrop", refillOnDrop);
            armorWarningPercent = Math.clamp(
                    Integer.parseInt(properties.getProperty("armorWarningPercent", "40")), 1, 100);
            excludedItems = properties.getProperty("excludedItems", "");
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load Inventory Helper settings from " + FILE, exception);
        }
    }

    static void save() {
        Properties properties = new Properties();
        properties.setProperty("armorHud", Boolean.toString(armorHud));
        properties.setProperty("armorWarningSound", Boolean.toString(armorWarningSound));
        properties.setProperty("totalItemCount", Boolean.toString(totalItemCount));
        properties.setProperty("anvilCostHud", Boolean.toString(anvilCostHud));
        properties.setProperty("effectDurationHud", Boolean.toString(effectDurationHud));
        properties.setProperty("effectDurationOnRight", Boolean.toString(effectDurationOnRight));
        properties.setProperty("armorQuickSwap", Boolean.toString(armorQuickSwap));
        properties.setProperty("autoRefill", Boolean.toString(autoRefill));
        properties.setProperty("refillOnPlace", Boolean.toString(refillOnPlace));
        properties.setProperty("refillOnUse", Boolean.toString(refillOnUse));
        properties.setProperty("refillOnDrop", Boolean.toString(refillOnDrop));
        properties.setProperty("armorWarningPercent", Integer.toString(armorWarningPercent));
        properties.setProperty("excludedItems", excludedItems);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "Inventory Helper settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save Inventory Helper settings to " + FILE, exception);
        }
    }

    static boolean isExcluded(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Set<String> excluded = Arrays.stream(excludedItems.split("[,;\\s]+"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        return excluded.contains(id.toString());
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
