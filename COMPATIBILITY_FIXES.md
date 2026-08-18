# Storage Drawers 26.2 Compatibility Fixes & Port Architecture

This document records the compatibility issues identified, their root causes, and the architectural solutions implemented in this fork for **Minecraft 26.2 (Fabric & NeoForge)**.

---

## Table of Contents
1. [Issue 1: Double Item Counting in External Storage Networks](#issue-1-double-item-counting-in-external-storage-networks)
2. [Issue 2: Random Slot Insertion When Depositing Items Back](#issue-2-random-slot-insertion-when-depositing-items-back)
3. [Issue 3: External Cable Traversal & Network Continuity (Trims Tag)](#issue-3-external-cable-traversal--network-continuity-trims-tag)
4. [Issue 4: Jade / Waila HUD Tooltip Overlay Compatibility](#issue-4-jade--waila-hud-tooltip-overlay-compatibility)
5. [Summary of Modified and Created Files](#summary-of-modified-and-created-files)

---

## Issue 1: Double Item Counting in External Storage Networks

### Problem Description
When connecting storage network mods (such as **Tom's Storage**, **Applied Energistics 2**, or **Refined Storage**) to a drawer network touching both a `Drawer Controller` and individual `Drawers`, the crafting terminal displayed **double the actual item quantity** (e.g. 100 Lapis Lazuli appeared as 200).

### Root Cause
1. `BlockEntityController` exposes all drawers in its cluster via `ItemStorage.SIDED` (Fabric) / `Capabilities.Item.BLOCK` (NeoForge).
2. Every individual `BlockEntityDrawers` also unconditionally registered and returned its own `DrawerStorageImpl` for `ItemStorage.SIDED`.
3. When network scanners performed a BFS/flood-fill search across adjacent blocks, they discovered both the Controller **and** each individual Drawer block, treating each as a distinct storage container and reading the item inventory twice.

### How the Fix Was Made
* **Controller-Aware Capability Filtering**:
  * In `INetworked.java`, added `default boolean isBoundToActiveController()`, which checks whether `getBoundControlGroup()` or `getSoftBoundControlGroups()` points to a valid, active `IControlGroup`.
  * In `PlatformCapabilities.java` (Fabric & NeoForge), modified `ItemStorage.SIDED` and `Capabilities.Item.BLOCK` registration for all drawer types:
    ```java
    ItemStorage.SIDED.registerForBlockEntity((entity, dir) -> 
        entity.isBoundToActiveController() ? null : DrawerStorageImpl.of(entity), 
        ModBlockEntities.STANDARD_DRAWERS_1.get()
    );
    ```
  * When a drawer is managed by a Controller, querying `ItemStorage.SIDED` on the individual drawer returns `null`, establishing the **Controller as the sole master entry point** for external automation.
  * Standalone drawers (not connected to any controller) continue to expose their own storage as normal.

---

## Issue 2: Random Slot Insertion When Depositing Items Back

### Problem Description
When storing items back into a connected drawer network via an external terminal (e.g. Tom's Storage Crafting Terminal), items did not return to their existing drawer (which already held that item prototype). Instead, items were placed into an empty drawer at random.

### Root Cause
1. In unorganized multi-inventory loops, external mods query each connected storage block in list iteration order.
2. If an empty unlocked drawer appeared before the matching populated drawer or Controller in the list, its `canItemBeStored()` returned `true` (since empty drawers accept any item).
3. The empty drawer accepted the transaction before the loop ever evaluated the drawer that actually held the item.

### How the Fix Was Made
* By directing all external inventory access through the `BlockEntityController` (as established in Issue 1), external insertions always invoke `BlockEntityController`'s `insert()` method.
* `BlockEntityController` maintains an internal priority queue of `drawerSlots` (`sortSlotRecords`):
  1. **Highest Priority**: Drawers that already contain a matching item prototype with available capacity.
  2. **Medium Priority**: Locked empty drawers assigned to that prototype.
  3. **Lowest Priority**: Unlocked empty drawers.
* As a result, items are guaranteed to route back to existing matching drawers first.

---

## Issue 3: External Cable Traversal & Network Continuity (Trims Tag)

### Problem Description
When connecting an external network connector (like Tom's Storage Inventory Connector) to a Drawer block rather than directly to the Controller block, the connector needed to traverse through drawer clusters to find the Controller.

### Root Cause
Tom's Storage checks whether adjacent blocks belong to `#toms_storage:trims` or `#c:storage_trims` to determine if a block acts as a conduit cable.

### How the Fix Was Made
Added the following tag files under `common/src/main/resources/data/`:
* `data/toms_storage/tags/block/trims.json`
* `data/c/tags/block/storage_trims.json`
* `data/c/tags/block/trims.json`

Each tag includes:
```json
{
  "replace": false,
  "values": [
    "#storagedrawers:drawers",
    "#storagedrawers:trim"
  ]
}
```
This enables full network traversal through any drawer or trim block in the cluster.

---

## Issue 4: Jade / Waila HUD Tooltip Overlay Compatibility

### Problem Description
After disabling `ItemStorage.SIDED` on controlled individual drawers, Jade's HUD tooltip overlay stopped displaying drawer contents on Fabric.

### Root Cause
1. Fabric lacked a native `IWailaPlugin` implementation for Storage Drawers (`fabric.mod.json` had no `jade` entrypoint, and `Waila.java` was only compiled for NeoForge).
2. Jade was relying on its generic `ItemStorageProvider` fallback to read `ItemStorage.SIDED`. When `ItemStorage.SIDED` returned `null` for controlled drawers, Jade had no fallback data source.
3. In `DrawerOverlay.java`, the item content rendering method (`addContent`) was commented out.

### How the Fix Was Made
1. **Implemented Dedicated Fabric Jade Plugin**:
   * Created `fabric/src/main/java/com/jaquadro/minecraft/storagedrawers/integration/Waila.java` implementing `IWailaPlugin` and `IBlockComponentProvider`.
   * Created `fabric/src/main/java/com/jaquadro/minecraft/storagedrawers/integration/DrawerOverlay.java` with `addContent()` enabled.
2. **Registered `jade` Entrypoint**:
   * Added `"jade": ["com.jaquadro.minecraft.storagedrawers.integration.Waila"]` in `fabric/src/main/resources/fabric.mod.json`.
3. **Direct Group Access**:
   * `WailaDrawer` directly queries `blockEntityDrawers.getGroup()` and `drawer.getStoredItemPrototype()` / `drawer.getStoredItemCount()`, rendering accurate item counts, capacity limits, and status badges independently of external Transfer API capabilities.
4. **Compile Dependencies**:
   * Added `compileOnly("maven.modrinth:jade:26.2.8+fabric")` to `fabric/build.gradle.kts`.

---

## Summary of Modified and Created Files

| File | Module | Type | Description |
| :--- | :---: | :---: | :--- |
| `common/.../api/storage/INetworked.java` | Common | Modified | Added `isBoundToActiveController()` helper method |
| `common/.../data/toms_storage/tags/block/trims.json` | Common | Created | Tom's Storage trim tag compatibility |
| `common/.../data/c/tags/block/storage_trims.json` | Common | Created | Conventional storage trim tag compatibility |
| `fabric/.../capabilities/PlatformCapabilities.java` | Fabric | Modified | Controller-aware `ItemStorage.SIDED` registration |
| `fabric/.../integration/Waila.java` | Fabric | Created | Native Jade `IWailaPlugin` for Fabric |
| `fabric/.../integration/DrawerOverlay.java` | Fabric | Created | Custom tooltip overlay renderer for Jade |
| `fabric/src/main/resources/fabric.mod.json` | Fabric | Modified | Added `"jade"` plugin entrypoint |
| `fabric/build.gradle.kts` | Fabric | Modified | Added Jade Fabric dependency |
| `neoforge/.../capabilities/PlatformCapabilities.java` | NeoForge | Modified | Controller-aware `Capabilities.Item.BLOCK` registration |
| `neoforge/.../integration/DrawerOverlay.java` | NeoForge | Modified | Enabled `addContent()` for Jade overlay |