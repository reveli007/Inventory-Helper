package ru.dogod.craftingtablecycle.neoforge;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = InventoryHelper.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int[] LAST_ARMOR_DAMAGE = {-1, -1, -1, -1};
    private static final Identifier[] LAST_ARMOR_IDS = new Identifier[4];
    private static KeyMapping settingsKey;
    private static ItemStack pendingStack = ItemStack.EMPTY;
    private static int pendingSlot = -1;
    private static int pendingExpiry;
    private static Action pendingAction;
    private static boolean wasDropDown;
    private static int clientTick;

    private ClientEvents() {
    }

    static void setSettingsKey(KeyMapping key) {
        settingsKey = key;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND
                && event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem) {
            recordAction(event.getEntity(), Action.PLACE);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND
                && !(event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem)) {
            recordAction(event.getEntity(), Action.USE);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        clientTick++;
        if (settingsKey != null) {
            while (settingsKey.consumeClick()) {
                minecraft.setScreen(new InventoryHelperSettingsScreen(minecraft.screen));
            }
        }

        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            Arrays.fill(LAST_ARMOR_DAMAGE, -1);
            Arrays.fill(LAST_ARMOR_IDS, null);
            pendingAction = null;
            pendingSlot = -1;
            wasDropDown = false;
            return;
        }

        boolean dropDown = minecraft.options.keyDrop.isDown();
        if (dropDown && !wasDropDown) {
            recordAction(player, Action.DROP);
        }
        wasDropDown = dropDown;

        updateArmorWarning(minecraft, player);
        processRefill(minecraft, player);
    }

    private static void recordAction(Player player, Action action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player || minecraft.screen != null || !ModConfig.autoRefill || !action.enabled()) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        ItemStack held = player.getInventory().getItem(selected);
        if (ModConfig.isExcluded(held)) {
            return;
        }
        pendingAction = action;
        pendingSlot = selected;
        pendingStack = held.copy();
        pendingExpiry = clientTick + 40;
    }

    private static void processRefill(Minecraft minecraft, Player player) {
        if (pendingAction == null) {
            return;
        }
        if (clientTick > pendingExpiry) {
            clearPending();
            return;
        }
        if (minecraft.gameMode == null || minecraft.screen != null || player.containerMenu != player.inventoryMenu
                || !player.getInventory().getItem(pendingSlot).isEmpty()) {
            return;
        }

        int targetMenuSlot = 36 + pendingSlot;
        for (int inventorySlot = 9; inventorySlot < 36; inventorySlot++) {
            ItemStack candidate = player.getInventory().getItem(inventorySlot);
            if (ItemStack.isSameItemSameComponents(candidate, pendingStack)
                    && !ModConfig.isExcluded(candidate)) {
                minecraft.gameMode.handleInventoryMouseClick(
                        player.inventoryMenu.containerId, inventorySlot, 0, ClickType.PICKUP, player);
                minecraft.gameMode.handleInventoryMouseClick(
                        player.inventoryMenu.containerId, targetMenuSlot, 0, ClickType.PICKUP, player);
                if (!player.inventoryMenu.getCarried().isEmpty()) {
                    minecraft.gameMode.handleInventoryMouseClick(
                            player.inventoryMenu.containerId, inventorySlot, 0, ClickType.PICKUP, player);
                }
                clearPending();
                return;
            }
        }
        clearPending();
    }

    private static void clearPending() {
        pendingAction = null;
        pendingSlot = -1;
        pendingStack = ItemStack.EMPTY;
    }

    private static void updateArmorWarning(Minecraft minecraft, Player player) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                LAST_ARMOR_DAMAGE[i] = -1;
                LAST_ARMOR_IDS[i] = null;
                continue;
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int damage = stack.getDamageValue();
            int remaining = stack.getMaxDamage() - damage;
            if (ModConfig.armorWarningSound && LAST_ARMOR_DAMAGE[i] >= 0
                    && itemId.equals(LAST_ARMOR_IDS[i])
                    && damage > LAST_ARMOR_DAMAGE[i]
                    && remaining * 100 / stack.getMaxDamage() <= ModConfig.armorWarningPercent) {
                player.playSound(SoundEvents.ITEM_BREAK.value(), 0.8F, 1.0F);
            }
            LAST_ARMOR_DAMAGE[i] = damage;
            LAST_ARMOR_IDS[i] = itemId;
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        if (ModConfig.totalItemCount) {
            renderTotalCount(graphics, minecraft, player);
        }
        if (ModConfig.armorHud) {
            renderArmor(graphics, minecraft, player);
        }
        if (ModConfig.effectDurationHud) {
            renderEffects(graphics, minecraft, player);
        }
    }

    private static void renderTotalCount(GuiGraphics graphics, Minecraft minecraft, Player player) {
        ItemStack selected = player.getMainHandItem();
        if (selected.isEmpty() || selected.getMaxStackSize() <= 1) {
            return;
        }
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, selected)) {
                count += stack.getCount();
            }
        }
        String value = Integer.toString(count);
        int width = minecraft.font.width(value);
        int hotbarLeft = graphics.guiWidth() / 2 - 91;
        int firstArmorX = hotbarLeft - 29 - ARMOR_SLOTS.length * 20;
        int right = firstArmorX - 4;
        int x = right - width - 6;
        int y = graphics.guiHeight() - 11;
        graphics.fill(x - 2, y - 1, right - 1, y + 10, 0xB0000000);
        graphics.drawString(minecraft.font, value, x, y, 0xFFFFFFFF, true);
    }

    private static void renderArmor(GuiGraphics graphics, Minecraft minecraft, Player player) {
        int hotbarLeft = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 22;
        int firstX = hotbarLeft - 29 - ARMOR_SLOTS.length * 20;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            int x = firstX + i * 20;
            graphics.fill(x, top, x + 20, top + 22, 0xAA101010);
            graphics.fill(x, top, x + 20, top + 1, 0xFF777777);
            ItemStack stack = player.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty()) {
                continue;
            }
            graphics.renderItem(stack, x + 2, top + 3);
            graphics.renderItemDecorations(minecraft.font, stack, x + 2, top + 3);
            if (stack.isDamageableItem()) {
                int barLeft = x + 2;
                int barTop = top + 19;
                int barWidth = 13;
                float remaining = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                int fill = Math.round(barWidth * remaining);
                int color = Math.round((1.0F - remaining) * 255.0F) << 16 | Math.round(remaining * 255.0F) << 8;
                graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 2, 0xFF000000);
                if (fill > 0) {
                    graphics.fill(barLeft, barTop, barLeft + fill, barTop + 1, 0xFF000000 | color);
                }
            }
        }
    }

    private static void renderEffects(GuiGraphics graphics, Minecraft minecraft, Player player) {
        int y = 34;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (!effect.showIcon()) {
                continue;
            }
            int ticks = effect.getDuration();
            String timer = effect.isInfiniteDuration() ? "∞" : formatDuration(ticks);
            int textWidth = minecraft.font.width(timer);
            int rowWidth = 24 + 6 + textWidth + 8;
            int x = ModConfig.effectDurationOnRight ? graphics.guiWidth() - rowWidth - 8 : 8;
            int iconX = ModConfig.effectDurationOnRight ? x + rowWidth - 24 : x;
            int textX = ModConfig.effectDurationOnRight ? x + 4 : iconX + 30;
            boolean flashing = !effect.isInfiniteDuration() && ticks <= 100
                    && (System.currentTimeMillis() / 300L & 1L) == 0L;
            int color = flashing ? 0xFFFF3030 : 0xFFFFFFFF;
            graphics.fill(x, y, x + rowWidth, y + 24, 0x99000000);
            Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            Identifier icon = Identifier.fromNamespaceAndPath(
                    effectId.getNamespace(), "textures/mob_effect/" + effectId.getPath() + ".png");
            graphics.blit(icon, iconX + 3, y + 3, 0, 0, 18, 18, 18, 18);
            graphics.drawString(minecraft.font, timer, textX, y + 7, color, true);
            y += 24;
        }
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        int minutes = seconds / 60;
        return minutes > 0 ? String.format("%d:%02d", minutes, seconds % 60) : seconds + "s";
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!ModConfig.anvilCostHud || !(event.getScreen() instanceof AnvilScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.containerMenu instanceof AnvilMenu menu)) {
            return;
        }
        int cost = menu.getCost();
        int x = event.getGuiGraphics().guiWidth() / 2 - 86;
        int y = event.getGuiGraphics().guiHeight() / 2 - 106;
        event.getGuiGraphics().fill(x, y, x + 172, y + 25, 0xD0101010);
        event.getGuiGraphics().drawString(minecraft.font, "Anvil cost: " + cost + " levels",
                x + 4, y + 3, 0xFFFFFFFF, true);
        boolean expensive = cost >= 40;
        String text = expensive ? "Too Expensive!" : "Too Expensive in: " + (40 - cost) + " levels";
        event.getGuiGraphics().drawString(minecraft.font, text, x + 4, y + 14,
                expensive ? 0xFFFF5555 : 0xFFFFD060, true);
    }

    @SubscribeEvent
    public static void onInventoryMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ModConfig.armorQuickSwap || event.getButton() != 1
                || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null || player.containerMenu != player.inventoryMenu
                || !player.inventoryMenu.getCarried().isEmpty()) {
            return;
        }
        int guiLeft = screen.getGuiLeft();
        int guiTop = screen.getGuiTop();
        Slot source = null;
        int mouseX = (int) event.getMouseX() - guiLeft;
        int mouseY = (int) event.getMouseY() - guiTop;
        for (Slot slot : player.inventoryMenu.slots) {
            if (slot.index >= 9 && slot.index < 36 && mouseX >= slot.x && mouseX < slot.x + 16
                    && mouseY >= slot.y && mouseY < slot.y + 16) {
                source = slot;
                break;
            }
        }
        if (source == null || source.getItem().isEmpty()) {
            return;
        }
        Equippable equippable = source.getItem().get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return;
        }
        int armorSlot = switch (equippable.slot()) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
        if (armorSlot < 0) {
            return;
        }
        Slot target = player.inventoryMenu.getSlot(armorSlot);
        if (!source.mayPickup(player) || !target.mayPickup(player) || !target.mayPlace(source.getItem())) {
            return;
        }
        minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                source.index, 0, ClickType.PICKUP, player);
        minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                armorSlot, 0, ClickType.PICKUP, player);
        if (!player.inventoryMenu.getCarried().isEmpty()) {
            minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                    source.index, 0, ClickType.PICKUP, player);
        }
        event.setCanceled(true);
    }

    private enum Action {
        PLACE, USE, DROP;

        boolean enabled() {
            return switch (this) {
                case PLACE -> ModConfig.refillOnPlace;
                case USE -> ModConfig.refillOnUse;
                case DROP -> ModConfig.refillOnDrop;
            };
        }
    }
}
