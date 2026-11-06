package ru.dogod.craftingtablecycle;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public final class CraftingTableCycleClient implements ClientModInitializer {
    private static final long SCAN_INTERVAL_NANOS = 60_000_000_000L;
    private static final Set<Identifier> REPLACED_BLOCKS = ConcurrentHashMap.newKeySet();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static KeyBinding settingsKey;
    private static KeyBinding arrowSelectionKey;
    private static volatile ItemStack selectedArrowStack = ItemStack.EMPTY;
    private static volatile UUID selectedArrowOwner;
    private static long clientTick;
    private static long pendingActionExpiry;
    private static ItemStack pendingActionStack = ItemStack.EMPTY;
    private static int pendingActionSlot = -1;
    private static Action pendingAction;

    private final int[] lastArmorDamage = {-1, -1, -1, -1};
    private final Identifier[] lastArmorIds = new Identifier[4];
    private int lastHeldItemDamage = -1;
    private Identifier lastHeldItemId;
    private int lastHeldItemSlot = -1;
    private World pickupWorld;
    private long lastScanNanos;

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crafting_table_cycle.settings",
                GLFW.GLFW_KEY_O,
                KeyBinding.Category.create(Identifier.of("crafting_table_cycle", "controls"))
        ));
        arrowSelectionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crafting_table_cycle.select_arrow",
                GLFW.GLFW_KEY_R,
                KeyBinding.Category.create(Identifier.of("crafting_table_cycle", "arrow_controls"))
        ));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand == Hand.MAIN_HAND && player.getStackInHand(hand).getItem() instanceof BlockItem) {
                recordAction(player, world, Action.PLACE);
            }
            return net.minecraft.util.ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand == Hand.MAIN_HAND && !(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
                recordAction(player, world, Action.USE);
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        HudElementRegistry.addLast(
                Identifier.of("crafting_table_cycle", "equipment_hud"),
                CraftingTableCycleHud::render
        );
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    public static boolean shouldRenderAsCraftingTable(BlockState state) {
        return REPLACED_BLOCKS.contains(Registries.BLOCK.getId(state.getBlock()));
    }

    public static void recordDrop(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == player && client.world != null) {
            recordAction(player, client.world, Action.DROP);
        }
    }

    private static void recordAction(PlayerEntity player, World world, Action action) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != player || client.currentScreen != null || !ModConfig.autoRefill) {
            return;
        }
        if (!action.isEnabled()) {
            return;
        }

        int slot = player.getInventory().getSelectedSlot();
        ItemStack stack = player.getInventory().getStack(slot);
        if (ModConfig.isExcluded(stack)) {
            return;
        }

        pendingAction = action;
        pendingActionSlot = slot;
        pendingActionStack = stack.copy();
        pendingActionExpiry = clientTick + 40;
    }

    private void onClientTick(MinecraftClient client) {
        clientTick++;
        while (settingsKey.wasPressed()) {
            client.setScreen(new CraftingTableCycleSettingsScreen(client.currentScreen));
        }
        boolean openArrowSelection = arrowSelectionKey.wasPressed();
        if (openArrowSelection
                && client.currentScreen == null
                && client.player != null
                && client.player.getMainHandStack().getItem() instanceof RangedWeaponItem) {
            client.setScreen(new ArrowSelectionScreen(null));
        }

        if (client.player == null || client.world == null) {
            lastScanNanos = 0L;
            pendingAction = null;
            pickupWorld = null;
            PickupItemOverlay.reset();
            clearArmorSnapshot();
            clearHeldItemSnapshot();
            return;
        }

        if (selectedArrowOwner == null
                && !ModConfig.selectedArrow.isBlank()
                && client.player.currentScreenHandler == client.player.playerScreenHandler) {
            selectedArrowOwner = client.player.getUuid();
            Identifier selectedId = Identifier.tryParse(ModConfig.selectedArrow);
            if (selectedId != null) {
                ItemStack selected = findArrow(client.player, candidate ->
                        Registries.ITEM.getId(candidate.getItem()).equals(selectedId));
                if (!selected.isEmpty()) {
                    selectedArrowStack = selected.copy();
                    prioritizeSelectedArrow(client, client.player, selected);
                }
            }
        }
        if (pickupWorld != client.world) {
            pickupWorld = client.world;
            PickupItemOverlay.reset();
        }
        PickupItemOverlay.tick();
        monitorArmor(client);
        monitorHeldItem(client);
        processAutoRefill(client);
        processCraftingTableCycle(client);
    }

    private void processAutoRefill(MinecraftClient client) {
        if (pendingAction == null || !ModConfig.autoRefill) {
            return;
        }
        if (clientTick > pendingActionExpiry) {
            clearPendingAction();
            return;
        }
        if (client.currentScreen != null || client.player.currentScreenHandler != client.player.playerScreenHandler) {
            return;
        }

        ItemStack targetStack = client.player.getInventory().getStack(pendingActionSlot);
        if (!targetStack.isEmpty()) {
            return;
        }

        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            ItemStack candidate = client.player.getInventory().getStack(inventorySlot);
            if (ItemStack.areItemsAndComponentsEqual(candidate, pendingActionStack)
                    && !ModConfig.isExcluded(candidate)) {
                client.interactionManager.clickSlot(
                        client.player.playerScreenHandler.syncId,
                        inventorySlot,
                        pendingActionSlot,
                        SlotActionType.SWAP,
                        client.player
                );
                clearPendingAction();
                return;
            }
        }
        clearPendingAction();
    }

    private void processCraftingTableCycle(MinecraftClient client) {
        if (!ModConfig.craftingTableCycle) {
            lastScanNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        if (lastScanNanos == 0L) {
            lastScanNanos = now;
            return;
        }
        if (now - lastScanNanos < SCAN_INTERVAL_NANOS) {
            return;
        }
        lastScanNanos = now;

        BlockPos supportPos = client.player.getBlockPos().down();
        BlockState supportState = client.world.getBlockState(supportPos);
        if (supportState.isAir()) {
            return;
        }

        Identifier blockId = Registries.BLOCK.getId(supportState.getBlock());
        if (REPLACED_BLOCKS.add(blockId)) {
            int radius = (client.options.getViewDistance().getValue() + 2) * 16;
            int x = supportPos.getX();
            int z = supportPos.getZ();
            client.worldRenderer.scheduleChunkRenders(
                    x - radius, client.world.getBottomY(), z - radius,
                    x + radius, client.world.getBottomY() + client.world.getHeight(), z + radius
            );
            client.player.sendMessage(
                    Text.literal("§6Crafting table: §f" + supportState.getBlock().getName().getString()),
                    true
            );
        }
    }

    private void monitorArmor(MinecraftClient client) {
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            ItemStack stack = client.player.getEquippedStack(ARMOR_SLOTS[index]);
            if (stack.isEmpty() || !stack.isDamageable()) {
                lastArmorDamage[index] = -1;
                lastArmorIds[index] = null;
                continue;
            }

            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            int damage = stack.getDamage();
            int maximumDamage = stack.getMaxDamage();
            if (ModConfig.armorWarningSound && lastArmorIds[index] != null
                    && lastArmorIds[index].equals(itemId)
                    && lastArmorDamage[index] >= 0
                    && damage > lastArmorDamage[index]
                    && (maximumDamage - damage) * 100 / maximumDamage <= ModConfig.armorWarningPercent) {
                playArmorWarning(client);
            }

            lastArmorDamage[index] = damage;
            lastArmorIds[index] = itemId;
        }
    }

    private void monitorHeldItem(MinecraftClient client) {
        ItemStack stack = client.player.getMainHandStack();
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        if (stack.isEmpty() || !stack.isDamageable()) {
            lastHeldItemDamage = -1;
            lastHeldItemId = null;
            lastHeldItemSlot = selectedSlot;
            return;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        int damage = stack.getDamage();
        int maximumDamage = stack.getMaxDamage();
        boolean sameHeldItem = selectedSlot == lastHeldItemSlot && itemId.equals(lastHeldItemId);
        int remainingPercent = (maximumDamage - damage) * 100 / maximumDamage;
        if (ModConfig.armorWarningSound
                && sameHeldItem
                && lastHeldItemDamage >= 0
                && damage > lastHeldItemDamage
                && remainingPercent <= ModConfig.armorWarningPercent) {
            playArmorWarning(client);
        }

        lastHeldItemId = itemId;
        lastHeldItemDamage = damage;
        lastHeldItemSlot = selectedSlot;
    }

    public static java.util.List<ItemStack> getAvailableArrows(PlayerEntity player) {
        java.util.List<ItemStack> arrows = new java.util.ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            addArrowIfNew(arrows, player.getInventory().getStack(slot));
        }
        addArrowIfNew(arrows, player.getOffHandStack());
        return arrows;
    }

    private static void addArrowIfNew(java.util.List<ItemStack> arrows, ItemStack stack) {
        if (stack.isEmpty() || !stack.isIn(ItemTags.ARROWS)) {
            return;
        }
        for (ItemStack existing : arrows) {
            if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                existing.increment(stack.getCount());
                return;
            }
        }
        arrows.add(stack.copy());
    }

    public static void selectArrow(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        selectedArrowStack = stack.copy();
        Item arrowItem = stack.getItem();
        ModConfig.selectedArrow = Registries.ITEM.getId(arrowItem).toString();
        selectedArrowOwner = client.player == null ? null : client.player.getUuid();
        ModConfig.save();
        if (client.player != null) {
            prioritizeSelectedArrow(client, client.player, stack);
        }
    }

    private static void prioritizeSelectedArrow(MinecraftClient client, PlayerEntity player, ItemStack selected) {
        if (client.interactionManager == null
                || player.currentScreenHandler != player.playerScreenHandler
                || !player.playerScreenHandler.getCursorStack().isEmpty()) {
            return;
        }

        PlayerScreenHandler handler = player.playerScreenHandler;
        ItemStack offHand = player.getOffHandStack();
        if (offHand.isIn(ItemTags.ARROWS)) {
            if (!ItemStack.areItemsAndComponentsEqual(offHand, selected)) {
                int selectedSlot = findInventorySlot(player, selected);
                if (selectedSlot >= 0) {
                    swapInventorySlots(client, player, handler, inventorySlotToHandlerSlot(selectedSlot), 45);
                }
            }
            return;
        }

        int firstArrowSlot = -1;
        int selectedSlot = -1;
        for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
            ItemStack candidate = player.getInventory().getStack(inventorySlot);
            if (!candidate.isIn(ItemTags.ARROWS)) {
                continue;
            }
            if (firstArrowSlot < 0) {
                firstArrowSlot = inventorySlot;
            }
            if (ItemStack.areItemsAndComponentsEqual(candidate, selected)) {
                selectedSlot = inventorySlot;
            }
        }
        if (firstArrowSlot >= 0 && selectedSlot >= 0 && firstArrowSlot != selectedSlot) {
            swapInventorySlots(
                    client,
                    player,
                    handler,
                    inventorySlotToHandlerSlot(selectedSlot),
                    inventorySlotToHandlerSlot(firstArrowSlot)
            );
        }
    }

    private static int findInventorySlot(PlayerEntity player, ItemStack selected) {
        for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
            if (ItemStack.areItemsAndComponentsEqual(player.getInventory().getStack(inventorySlot), selected)) {
                return inventorySlot;
            }
        }
        return -1;
    }

    private static int inventorySlotToHandlerSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    private static void swapInventorySlots(
            MinecraftClient client,
            PlayerEntity player,
            PlayerScreenHandler handler,
            int firstSlot,
            int secondSlot
    ) {
        client.interactionManager.clickSlot(handler.syncId, firstSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(handler.syncId, secondSlot, 0, SlotActionType.PICKUP, player);
        client.interactionManager.clickSlot(handler.syncId, firstSlot, 0, SlotActionType.PICKUP, player);
    }

    public static ItemStack getSelectedArrow(PlayerEntity player, ItemStack weapon) {
        if (selectedArrowOwner == null || !selectedArrowOwner.equals(player.getUuid())) {
            return ItemStack.EMPTY;
        }
        if (!(weapon.getItem() instanceof RangedWeaponItem projectileWeapon)) {
            return ItemStack.EMPTY;
        }

        if (!selectedArrowStack.isEmpty()
                && projectileWeapon.getProjectiles().test(selectedArrowStack)) {
            ItemStack available = findArrow(player, candidate ->
                    ItemStack.areItemsAndComponentsEqual(candidate, selectedArrowStack));
            if (!available.isEmpty()) {
                return available;
            }
        }

        if (!ModConfig.selectedArrow.isBlank()) {
            Identifier selectedId = Identifier.tryParse(ModConfig.selectedArrow);
            if (selectedId != null) {
                ItemStack available = findArrow(player, candidate ->
                        Registries.ITEM.getId(candidate.getItem()).equals(selectedId)
                                && projectileWeapon.getProjectiles().test(candidate));
                if (!available.isEmpty()) {
                    selectedArrowStack = available.copy();
                    return available;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack findArrow(PlayerEntity player, java.util.function.Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack candidate = player.getInventory().getStack(slot);
            if (!candidate.isEmpty() && candidate.isIn(ItemTags.ARROWS) && predicate.test(candidate)) {
                return candidate;
            }
        }
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty() && offHand.isIn(ItemTags.ARROWS) && predicate.test(offHand)) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private static void playArmorWarning(MinecraftClient client) {
        client.player.playSound(SoundEvents.ENTITY_ITEM_BREAK.value(), 0.8F, 1.0F);
    }

    private void clearArmorSnapshot() {
        java.util.Arrays.fill(lastArmorDamage, -1);
        java.util.Arrays.fill(lastArmorIds, null);
    }

    private void clearHeldItemSnapshot() {
        lastHeldItemDamage = -1;
        lastHeldItemId = null;
        lastHeldItemSlot = -1;
    }

    private static void clearPendingAction() {
        pendingAction = null;
        pendingActionSlot = -1;
        pendingActionStack = ItemStack.EMPTY;
    }

    private enum Action {
        PLACE,
        USE,
        DROP;

        private boolean isEnabled() {
            return switch (this) {
                case PLACE -> ModConfig.refillOnPlace;
                case USE -> ModConfig.refillOnUse;
                case DROP -> ModConfig.refillOnDrop;
            };
        }
    }
}
