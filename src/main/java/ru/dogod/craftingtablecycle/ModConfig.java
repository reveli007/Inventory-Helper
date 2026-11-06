package ru.dogod.craftingtablecycle;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ModConfig {
    private static final int CONFIG_VERSION = 6;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("crafting_table_cycle.properties");

    public static boolean armorHud = true;
    public static boolean armorWarningSound = true;
    public static boolean totalItemCount = true;
    public static boolean anvilCostHud = true;
    public static boolean effectDurationHud = true;
    public static boolean effectDurationOnRight = true;
    public static boolean pickupHudOnRight = true;
    public static boolean blockInfoHud = true;
    public static boolean armorQuickSwap = true;
    public static boolean autoRefill = true;
    public static boolean refillOnPlace = true;
    public static boolean refillOnUse = true;
    public static boolean refillOnDrop = true;
    public static boolean craftingTableCycle = false;
    public static boolean inventoryBackgroundDim = true;
    public static int armorWarningPercent = 40;
    public static int pickupHudMaxEntries = 5;
    public static String excludedItems = "";
    public static String selectedArrow = "";

    private ModConfig() {
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }

        java.util.Properties properties = new java.util.Properties();
        boolean needsMigration;
        try (Reader reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            int savedConfigVersion = Integer.parseInt(properties.getProperty("configVersion", "0"));
            needsMigration = savedConfigVersion < CONFIG_VERSION;
            armorHud = getBoolean(properties, "armorHud", armorHud);
            armorWarningSound = getBoolean(properties, "armorWarningSound", armorWarningSound);
            totalItemCount = getBoolean(properties, "totalItemCount", totalItemCount);
            anvilCostHud = getBoolean(properties, "anvilCostHud", anvilCostHud);
            effectDurationHud = getBoolean(properties, "effectDurationHud", effectDurationHud);
            effectDurationOnRight = getBoolean(properties, "effectDurationOnRight", effectDurationOnRight);
            pickupHudOnRight = getBoolean(properties, "pickupHudOnRight", pickupHudOnRight);
            blockInfoHud = getBoolean(properties, "blockInfoHud", blockInfoHud);
            armorQuickSwap = getBoolean(properties, "armorQuickSwap", armorQuickSwap);
            autoRefill = getBoolean(properties, "autoRefill", autoRefill);
            refillOnPlace = getBoolean(properties, "refillOnPlace", refillOnPlace);
            refillOnUse = getBoolean(properties, "refillOnUse", refillOnUse);
            refillOnDrop = getBoolean(properties, "refillOnDrop", refillOnDrop);
            craftingTableCycle = getBoolean(properties, "craftingTableCycle", craftingTableCycle);
            inventoryBackgroundDim = getBoolean(properties, "inventoryBackgroundDim", inventoryBackgroundDim);
            armorWarningPercent = Math.clamp(
                    Integer.parseInt(properties.getProperty("armorWarningPercent", "40")), 1, 100
            );
            pickupHudMaxEntries = Math.clamp(
                    Integer.parseInt(properties.getProperty("pickupHudMaxEntries", "5")), 1, 10
            );
            excludedItems = properties.getProperty("excludedItems", "");
            selectedArrow = properties.getProperty("selectedArrow", "");
            if (savedConfigVersion == 0) {
                craftingTableCycle = false;
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Could not load mod settings from " + FILE, exception);
        }
        if (needsMigration) {
            save();
        }
    }

    public static void save() {
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("configVersion", Integer.toString(CONFIG_VERSION));
        properties.setProperty("armorHud", Boolean.toString(armorHud));
        properties.setProperty("armorWarningSound", Boolean.toString(armorWarningSound));
        properties.setProperty("totalItemCount", Boolean.toString(totalItemCount));
        properties.setProperty("anvilCostHud", Boolean.toString(anvilCostHud));
        properties.setProperty("effectDurationHud", Boolean.toString(effectDurationHud));
        properties.setProperty("effectDurationOnRight", Boolean.toString(effectDurationOnRight));
        properties.setProperty("pickupHudOnRight", Boolean.toString(pickupHudOnRight));
        properties.setProperty("blockInfoHud", Boolean.toString(blockInfoHud));
        properties.setProperty("armorQuickSwap", Boolean.toString(armorQuickSwap));
        properties.setProperty("autoRefill", Boolean.toString(autoRefill));
        properties.setProperty("refillOnPlace", Boolean.toString(refillOnPlace));
        properties.setProperty("refillOnUse", Boolean.toString(refillOnUse));
        properties.setProperty("refillOnDrop", Boolean.toString(refillOnDrop));
        properties.setProperty("craftingTableCycle", Boolean.toString(craftingTableCycle));
        properties.setProperty("inventoryBackgroundDim", Boolean.toString(inventoryBackgroundDim));
        properties.setProperty("armorWarningPercent", Integer.toString(armorWarningPercent));
        properties.setProperty("pickupHudMaxEntries", Integer.toString(pickupHudMaxEntries));
        properties.setProperty("excludedItems", excludedItems);
        properties.setProperty("selectedArrow", selectedArrow);

        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "Crafting Table Cycle settings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save mod settings to " + FILE, exception);
        }
    }

    public static boolean isExcluded(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Identifier id = Registries.ITEM.getId(stack.getItem());
        Set<String> excluded = Arrays.stream(excludedItems.split("[,;\\s]+"))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        return excluded.contains(id.toString());
    }

    private static boolean getBoolean(java.util.Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

}
