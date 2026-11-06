package ru.dogod.craftingtablecycle;

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
import net.minecraftforge.fml.loading.FMLPaths;

public final class ModConfig {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("inventory_helper.properties");

    public static boolean armorHud = true;
    public static boolean armorWarningSound = true;
    public static boolean totalItemCount = true;
    public static boolean anvilCostHud = true;
    public static boolean effectDurationHud = true;
    public static boolean effectDurationOnRight = true;
    public static boolean armorQuickSwap = true;
    public static boolean autoRefill = true;
    public static boolean refillOnPlace = true;
    public static boolean refillOnUse = true;
    public static boolean refillOnDrop = true;
    public static int armorWarningPercent = 40;
    public static String excludedItems = "";

    private ModConfig() {
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            armorHud = getBoolean(properties, "armorHud", armorHud);
            armorWarningSound = getBoolean(properties, "armorWarningSound", armorWarningSound);
            totalItemCount = getBoolean(properties, "totalItemCount", totalItemCount);
            anvilCostHud = getBoolean(properties, "anvilCostHud", anvilCostHud);
            effectDurationHud = getBoolean(properties, "effectDurationHud", effectDurationHud);
            effectDurationOnRight = getBoolean(properties, "effectDurationOnRight", effectDurationOnRight);
            armorQuickSwap = getBoolean(properties, "armorQuickSwap", armorQuickSwap);
            autoRefill = getBoolean(properties, "autoRefill", autoRefill);
            refillOnPlace = getBoolean(properties, "refillOnPlace", refillOnPlace);
            refillOnUse = getBoolean(properties, "refillOnUse", refillOnUse);
            refillOnDrop = getBoolean(properties, "refillOnDrop", refillOnDrop);
            armorWarningPercent = Math.clamp(Integer.parseInt(properties.getProperty("armorWarningPercent", "40")), 1, 100);
            excludedItems = properties.getProperty("excludedItems", "");
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load Inventory Helper settings from " + FILE, exception);
        }
    }

    public static void save() {
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

    public static boolean isExcluded(ItemStack stack) {
        if (stack.isEmpty()) return true;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Set<String> excluded = Arrays.stream(excludedItems.split("[,;\\s]+"))
                .filter(value -> !value.isBlank()).collect(Collectors.toSet());
        return excluded.contains(id.toString());
    }

    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}