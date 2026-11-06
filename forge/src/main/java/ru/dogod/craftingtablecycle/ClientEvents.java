package ru.dogod.craftingtablecycle;

import java.util.Arrays;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public final class ClientEvents {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int[] lastArmorDamage = {-1, -1, -1, -1};
    private static final Identifier[] lastArmorIds = new Identifier[4];
    private static final Identifier HUD_LAYER = Identifier.fromNamespaceAndPath(InventoryHelperMod.MOD_ID, "hud");
    private static long tick;
    private static long pendingExpiry;
    private static int pendingHotbarSlot = -1;
    private static ItemStack pendingStack = ItemStack.EMPTY;
    private static Action pendingAction;

    private ClientEvents() {}

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientEvents::onClientTick);
        InputEvent.Key.BUS.addListener(ClientEvents::onKey);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(ClientEvents::onRightClickBlock);
        PlayerInteractEvent.RightClickItem.BUS.addListener(ClientEvents::onRightClickItem);
        AddGuiOverlayLayersEvent.BUS.addListener(ClientEvents::onGuiLayers);
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(ClientEvents::onScreenMouse);
        ScreenEvent.Render.Post.BUS.addListener(ClientEvents::onScreenRenderPost);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        tick++;
        if (minecraft.player == null || minecraft.level == null) {
            clearPending();
            Arrays.fill(lastArmorDamage, -1);
            Arrays.fill(lastArmorIds, null);
            return;
        }
        monitorArmor(minecraft);
        refill(minecraft);
    }

    private static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getKey() == GLFW.GLFW_KEY_O && minecraft.player != null
                && (minecraft.screen == null || minecraft.screen instanceof InventoryScreen)) {
            minecraft.setScreen(new SettingsScreen(minecraft.screen));
            return;
        }
        if (minecraft.player != null && event.getKey() == GLFW.GLFW_KEY_Q) {
            recordAction(minecraft.player, Action.DROP);
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND
                && event.getItemStack().getItem() instanceof BlockItem) {
            recordAction(event.getEntity(), Action.PLACE);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() && event.getHand() == InteractionHand.MAIN_HAND
                && !(event.getItemStack().getItem() instanceof BlockItem)) {
            recordAction(event.getEntity(), Action.USE);
        }
    }

    private static void onGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addConditionTo(ForgeLayeredDraw.POTION_EFFECTS,
                () -> !ModConfig.effectDurationHud || Minecraft.getInstance().screen != null);
        event.getLayeredDraw().add(HUD_LAYER, (graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && !minecraft.options.hideGui) HelperHud.render(graphics, minecraft);
        });
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ModConfig.anvilCostHud && event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen
                && minecraft.player != null && minecraft.player.containerMenu instanceof AnvilMenu menu) {
            HelperHud.renderAnvilCost(event.getGuiGraphics(), minecraft, menu.getCost());
        }
    }

    private static boolean onScreenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ModConfig.armorQuickSwap || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || !(event.getScreen() instanceof InventoryScreen screen)) return false;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null || !player.inventoryMenu.getCarried().isEmpty()) return false;
        Slot source = slotUnderMouse(screen, event.getMouseX(), event.getMouseY());
        if (source == null) return false;
        int sourceMenuSlot = screen.getMenu().slots.indexOf(source);
        if (sourceMenuSlot < 9 || sourceMenuSlot >= 45) return false;
        ItemStack replacement = source.getItem();
        Equippable equippable = replacement.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (replacement.isEmpty() || equippable == null) return false;
        int armorSlotId = armorMenuSlot(equippable.slot());
        if (armorSlotId < 0) return false;
        Slot armor = player.inventoryMenu.getSlot(armorSlotId);
        if (!source.mayPickup(player) || !armor.mayPickup(player) || !armor.mayPlace(replacement)) return false;
        minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, armorSlotId, 0, ClickType.PICKUP, player);
        if (!player.inventoryMenu.getCarried().isEmpty()) {
            minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, sourceMenuSlot, 0, ClickType.PICKUP, player);
        }
        return true;
    }
    private static Slot slotUnderMouse(InventoryScreen screen, double mouseX, double mouseY) {
        int x = (int) mouseX - screen.getGuiLeft();
        int y = (int) mouseY - screen.getGuiTop();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && x >= slot.x - 1 && x < slot.x + 17 && y >= slot.y - 1 && y < slot.y + 17) return slot;
        }
        return null;
    }

    private static int armorMenuSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    private static void recordAction(Player player, Action action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player || minecraft.screen != null || !ModConfig.autoRefill || !action.enabled()) return;
        int selected = player.getInventory().getSelectedSlot();
        ItemStack held = player.getInventory().getItem(selected);
        if (ModConfig.isExcluded(held)) return;
        pendingAction = action;
        pendingHotbarSlot = selected;
        pendingStack = held.copy();
        pendingExpiry = tick + 40;
    }

    private static void refill(Minecraft minecraft) {
        if (pendingAction == null || !ModConfig.autoRefill) return;
        Player player = minecraft.player;
        if (tick > pendingExpiry) {
            clearPending();
            return;
        }
        if (minecraft.screen != null || player.containerMenu != player.inventoryMenu) return;
        if (!player.getInventory().getItem(pendingHotbarSlot).isEmpty()) return;
        for (int inventoryIndex = 9; inventoryIndex < 36; inventoryIndex++) {
            ItemStack candidate = player.getInventory().getItem(inventoryIndex);
            if (ItemStack.isSameItemSameComponents(candidate, pendingStack) && !ModConfig.isExcluded(candidate)) {
                minecraft.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId,
                        inventoryIndex, pendingHotbarSlot, ClickType.SWAP, player);
                clearPending();
                return;
            }
        }
        clearPending();
    }

    private static void monitorArmor(Minecraft minecraft) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack stack = minecraft.player.getItemBySlot(ARMOR_SLOTS[i]);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                if (lastArmorIds[i] != null && ModConfig.armorWarningSound) playWarning(minecraft);
                lastArmorDamage[i] = -1;
                lastArmorIds[i] = null;
                continue;
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int damage = stack.getDamageValue();
            int maxDamage = stack.getMaxDamage();
            if (ModConfig.armorWarningSound && itemId.equals(lastArmorIds[i]) && lastArmorDamage[i] >= 0
                    && damage > lastArmorDamage[i]
                    && (maxDamage - damage) * 100 / maxDamage <= ModConfig.armorWarningPercent) {
                playWarning(minecraft);
            }
            lastArmorDamage[i] = damage;
            lastArmorIds[i] = itemId;
        }
    }

    private static void playWarning(Minecraft minecraft) {
        minecraft.player.playSound(SoundEvents.ITEM_BREAK.value(), 0.8F, 1.0F);
    }

    private static void clearPending() {
        pendingAction = null;
        pendingHotbarSlot = -1;
        pendingStack = ItemStack.EMPTY;
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