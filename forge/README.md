# Inventory Helper - Forge

A client-side Forge port of Inventory Helper for Minecraft 1.21.11. It requires Forge 61.2.1 or newer in the 61.x line and Java 21. The Fabric project is not changed by this subproject.

## Features

- Open the English settings screen with O.
- Armor HUD and an armor-break sound warning at a configurable remaining-durability threshold.
- Total inventory count for the matching stackable item in the main hand.
- Status-effect duration list with an expiring-effect flash and selectable left/right placement.
- Anvil level-cost and "Too Expensive" threshold indicator.
- Automatic hotbar refill after block placement, item use, or dropping the selected item, with comma/semicolon/space-separated item-ID exclusions.
- Right-click armor swapping in the player inventory.

Settings are saved to `config/inventory_helper.properties`.

## Platform-specific omission

The optional hidden crafting-table texture-cycle feature is intentionally omitted. The Fabric implementation relies on Sodium-specific rendering mixins and Fabric's chunk-render scheduling, neither of which has a reliable equivalent in this Forge-only port. No dormant setting or mixin is shipped for it.

## Build

Run `gradle clean build` (or `gradlew.bat clean build`) with a Java 21 toolchain. The production Forge jar is emitted under `build/libs/`.