# Inventory Helper — NeoForge

This isolated subproject is the NeoForge client-only port of Inventory Helper
for **Minecraft 1.21.11**. It uses NeoForge 21.11.45, ModDevGradle, Java 21,
and Mojang mappings. It does not alter or depend on the Fabric project.

Press **O** to open settings. The port includes the armor HUD and configurable
low-durability warning threshold, matching held-item total count, status-effect
timers with a blinking final-five-second alert and selectable side, anvil cost
indicator, hotbar auto-refill after use/place/drop with item-ID exclusions,
and right-click armor swapping in the player inventory. Settings are stored in
`config/inventory_helper.properties`.

The optional hidden crafting-table texture-cycle feature is deliberately
omitted. Its block-render substitution and Sodium mixin are specific to the
Fabric rendering pipeline; there is no reliable, safe general NeoForge
equivalent to ship as an option.

The production mod jar is the regular `jar` task output in `build/libs/`;
sources jars are not required for installation.
