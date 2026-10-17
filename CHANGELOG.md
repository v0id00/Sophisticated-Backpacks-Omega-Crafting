# Changelog

## 1.0.1

- Retargeted to newest 1.21.1 builds of dependencies: Sophisticated Core 1.5.x (1.5.13.2404) and Sophisticated Backpacks 3.26.x (3.26.12.2208)
- No source changes required — public API used by the addon is compatible across the 1.4→1.5 / 3.25→3.26 bump
- Updated dependency version ranges in neoforge.mods.toml

## 1.0.0

- Initial release: Omega Tier Crafting Upgrade with eight embedded sub-upgrades
- Virtual sub-upgrade state persisted via data components and CustomData backup
- Conflict rules block duplicate constituent upgrades
- Block removal when sub-inventories contain items
- Lifecycle hooks for equip, place, break, and GUI close
- Live furnace burn/cook UI sync while backpack GUI is open
