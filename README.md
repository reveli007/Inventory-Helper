# Inventory Helper

Client-side Fabric mod for Minecraft 1.21.11. Press **O** (rebindable under
Controls → Inventory Helper) to open the in-game settings.

Features:

- Four armor slots extending left from the offhand slot, beside the hotbar.
  Durability uses Minecraft's standard item durability bar, with an adjustable
  low-durability warning sound.
- Right-click an armor piece in your player inventory to equip it and swap the
  currently worn piece back into that inventory slot. This can be disabled in
  settings; the cursor must be empty and binding-curse armor cannot be removed.
- Configurable automatic refill when a hotbar stack is fully consumed by
  placing blocks, using items, or dropping them. Matching items move from the
  main inventory into the same hotbar slot.
- A configurable list of item IDs excluded from automatic refill.
- A total-count indicator for the held stackable item, summing matching item
  stacks with the same components across the hotbar and main inventory. It
  appears in the hotbar's lower-right corner and can be toggled in settings.
- A **Sort** button in the player inventory, chest, and shulker screens.
  Sorting merges matching stacks and orders items by name. It only changes the
  inventory shown by that screen: the player inventory and hotbar are untouched
  while sorting a container, and the hotbar is excluded when sorting the player
  inventory.
- Configurable low-durability sound warnings for equipped armor and held
  damageable items, with low-durability bars highlighted red.
- Recent item pickups appear as item icons and counts on the right side of the
  HUD. The settings screen controls how many notifications are shown at once.
- Looking at a block shows a top-center tooltip with its item icon, name, and
  the mod it comes from. It can be toggled in settings.
- Press **R** while holding a bow or crossbow to open the arrow picker. Click an
  arrow type to select it; its icon and remaining count are shown by the hotbar.
  The selected arrow stack is moved ahead of other arrow stacks in the inventory
  so vanilla ammo selection uses it; if another arrow is in the offhand, the
  selected stack swaps with that arrow. Non-arrow inventory items are not moved.
- An anvil indicator showing the current level cost and levels remaining
  before the vanilla 40-level "Too Expensive!" limit. It can be toggled in
  settings; modified server limits may differ.
- Custom potion and status-effect icons with remaining durations and no
  background frame, replacing the vanilla HUD icons in the upper left or upper
  right corner. Timers blink red during their last five seconds. The display
  and position can be changed in settings.
- A hidden crafting-table texture-cycle easter egg is disabled by default and
  has no in-game toggle. To enable it, set `craftingTableCycle=true` in
  `config/crafting_table_cycle.properties`. Once enabled, every 60 seconds the
  block under the player is added to the session list and all blocks of that
  type are rendered as crafting tables. Sodium chunk rendering is supported.

Settings are saved to `config/crafting_table_cycle.properties`.

The mod is client-side and does not change world blocks or require server-side
installation.
