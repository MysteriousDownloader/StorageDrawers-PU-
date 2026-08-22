# Storage Drawers 26.2 Compatibility Fixes & Port Architecture

This document records the compatibility issues identified, their root causes, and the architectural solutions implemented in this fork for **Minecraft 26.2 (Fabric & NeoForge)**.

---

## Table of Contents
1. [Issue 1: Double Item Counting in External Storage Networks](#issue-1-double-item-counting-in-external-storage-networks)
2. [Issue 2: Random Slot Insertion When Depositing Items Back](#issue-2-random-slot-insertion-when-depositing-items-back)
3. [Issue 3: External Cable Traversal & Network Continuity (Trims Tag)](#issue-3-external-cable-traversal--network-continuity-trims-tag)
4. [Issue 4: Jade / Waila HUD Tooltip Overlay Compatibility](#issue-4-jade--waila-hud-tooltip-overlay-compatibility)
5. [Issue 5: Missing Compacting & Framed Blocks in Conduit Tags](#issue-5-missing-compacting--framed-blocks-in-conduit-tags)
6. [Issue 6: Reflection-Free Controller Multiblock Discovery](#issue-6-reflection-free-controller-multiblock-discovery)
7. [Issue 7: Fabric Live Terminal Inventory Versioning & Change Tracking](#issue-7-fabric-live-terminal-inventory-versioning--change-tracking)
8. [Issue 8: Void Upgrade Priority Alignment and Shift-Click Routing Engine](#issue-8-void-upgrade-priority-alignment-and-shift-click-routing-engine)
9. [Summary of Modified and Created Files](#summary-of-modified-and-created-files)

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

## Issue 5: Missing Compacting & Framed Blocks in Conduit Tags

### Problem Description
After the initial trim-tag fix (Issue 3), external storage networks could traverse through standard drawers and standard trim, but **Compacting Drawers** and **Framed Drawers / Framed Trim** still broke network continuity. When a Tom's Storage Inventory Connector or cable was attached to a Compacting Drawer or Framed Drawer, the BFS scan stopped at that block and failed to discover the Controller, even though the blocks were physically connected via the drawer multiblock.

External mods querying `#storagedrawers:drawers`, `#storagedrawers:trim`, `#toms_storage:trims`, and `#c:storage_trims` did not recognize these variants as valid conduit blocks.

### Root Cause
1. `common/src/main/resources/data/storagedrawers/tags/block/drawers.json` only listed standard drawer blocks (`oak_full_drawers_1`, `oak_full_drawers_2`, `oak_full_drawers_4`, `oak_half_drawers_1`, `oak_half_drawers_2`, `oak_half_drawers_4`, etc.) and omitted `compacting_drawers_3` and all `framed_drawers_*` variants.
2. `common/src/main/resources/data/storagedrawers/tags/block/trim.json` only listed `oak_trim` and omitted `framed_trim`.
3. Because `data/toms_storage/tags/block/trims.json`, `data/c/tags/block/storage_trims.json`, and `data/c/tags/block/trims.json` delegate to `#storagedrawers:drawers` and `#storagedrawers:trim`, the missing entries propagated to all external conduit tags.
4. As a result, any cluster containing a compacting or framed block created a gap in the conduit graph.

### How the Fix Was Made
* **Expanded `#storagedrawers:drawers`** (`common/src/main/resources/data/storagedrawers/tags/block/drawers.json`):
  ```json
  {
    "replace": false,
    "values": [
      "#storagedrawers:standard_drawers",
      "#storagedrawers:compacting_drawers",
      "#storagedrawers:framed_drawers"
    ]
  }
  ```
  With supporting sub-tags:
  * `standard_drawers.json` - all wood variants of 1/2/4 standard drawers
  * `compacting_drawers.json` - all `compacting_drawers_3` variants
  * `framed_drawers.json` - `framed_drawers_1`, `framed_drawers_2`, `framed_drawers_4`, `framed_compacting_drawers_3`

* **Expanded `#storagedrawers:trim`** (`common/src/main/resources/data/storagedrawers/tags/block/trim.json`):
  ```json
  {
    "replace": false,
    "values": [
      "#storagedrawers:standard_trim",
      "#storagedrawers:framed_trim"
    ]
  }
  ```

* **Updated External Conduit Tags** to ensure full delegation:
  * `data/toms_storage/tags/block/trims.json`
  * `data/c/tags/block/storage_trims.json`
  * `data/c/tags/block/trims.json`
  Each now contains:
  ```json
  {
    "replace": false,
    "values": [
      "#storagedrawers:drawers",
      "#storagedrawers:trim"
    ]
  }
  ```
  This guarantees that any future drawer/trim variant added to `#storagedrawers:drawers` or `#storagedrawers:trim` is automatically traversable by Tom's Storage, AE2, and other mods using `c:storage_trims`.

* Verified that compacting and framed blocks now act as transparent conduit cables, allowing a connector placed on any drawer in the cluster to reach the Controller without requiring a direct Controller connection.

---

## Issue 6: Reflection-Free Controller Multiblock Discovery

### Problem Description
External storage mods (Tom's Storage, RS, AE2 addons) needed to enumerate all drawer positions belonging to a Controller's multiblock to build their network graph and to invalidate caches when the multiblock changes. Previously the only way to obtain this was via **reflection** into `BlockEntityController`'s private `drawerSlots` / `controlGroup` fields or by re-implementing the BFS scan, which was brittle across mappings and versions and broke on obfuscated production builds.

### Root Cause
1. `BlockEntityController` stored connected drawers in a private `List<SlotRecord> drawerSlots` and exposed no public accessor for positions.
2. `IControlGroup` (the abstraction for the controller network) exposed only `getDrawerCount()` and `getDrawer(int)` but no bulk position query.
3. External integrations resorted to `Field drawerSlotsField = BlockEntityController.class.getDeclaredField("drawerSlots"); drawerSlotsField.setAccessible(true);` which fails under Fabric's intermediary mappings and NeoForge's AT restrictions.

### How the Fix Was Made
* **Added Public API to `IControlGroup.java`** (`common/src/main/java/com/jaquadro/minecraft/storagedrawers/api/storage/IControlGroup.java`):
  ```java
  /**
   * @return immutable list of all drawer block positions currently bound to this control group.
   *         Used by external networks for reflection-free multiblock discovery.
   */
  List<BlockPos> getConnectedDrawerPositions();
  ```

* **Implemented in `BlockEntityController.java`** (`common/src/main/java/com/jaquadro/minecraft/storagedrawers/block/entity/BlockEntityController.java`):
  ```java
  @Override
  public List<BlockPos> getConnectedDrawerPositions() {
      if (controlGroup == null) return List.of();
      return controlGroup.getConnectedDrawerPositions();
  }
  // Inside ControlGroup inner class:
  @Override
  public List<BlockPos> getConnectedDrawerPositions() {
      return drawerSlots.stream()
          .map(record -> record.pos)
          .toList(); // immutable copy
  }
  ```
  * Returns an immutable snapshot to prevent external mutation.
  * Returns empty list when the controller is unformed or inactive, allowing callers to safely handle disconnected states.
  * No reflection required; Tom's Storage and other mods can now call `controller.getConnectedDrawerPositions()` or `controller.getControlGroup().getConnectedDrawerPositions()` directly.

* This also enables efficient cache invalidation: external networks can compare the returned position list hash between ticks instead of re-scanning the world.

---

## Issue 7: Fabric Live Terminal Inventory Versioning & Change Tracking

### Problem Description
On Fabric, Tom's Storage Crafting Terminal and similar live terminals poll the attached `Storage<ItemVariant>` to display real-time counts. Without a change-detection mechanism, terminals had to either **deep-compare every slot each tick** (expensive) or miss updates when drawers were modified via in-world interactions (inserting items manually, breaking a drawer, voiding). This caused stale displays and desync between the drawer network and the terminal UI.

### Root Cause
1. Fabric Transfer API's `Storage<ItemVariant>` exposes `long getVersion()` for change tracking, but `DrawerStorageImpl` returned a constant `0` and never updated.
2. `BlockEntityDrawers` and `BlockEntityController` had no monotonic counter to track modifications; `setChanged()` alone does not propagate to the Transfer API's version.
3. External mods had no lightweight way to know if the inventory had changed since the last query.

### How the Fix Was Made
* **Added `storageVersion` Counter to `BlockEntityDrawers.java`** (`common/src/main/java/com/jaquadro/minecraft/storagedrawers/block/entity/BlockEntityDrawers.java`):
  ```java
  private long storageVersion = 0;

  public long getStorageVersion() {
      return storageVersion;
  }

  private void incrementStorageVersion() {
      storageVersion++;
      setChanged();
  }
  ```
  * Incremented on every `insert()`, `extract()`, `setStoredItem()`, upgrade change, and `onGroupChanged()` callback.
  * Persisted via NBT (`getUpdateTag` / `loadAdditional`) and synchronized to client via block update packets so the version survives chunk reloads.

* **Added Aggregated `storageVersion` to `BlockEntityController.java`**:
  ```java
  private long storageVersion = 0;

  public long getStorageVersion() {
      return storageVersion;
  }
  ```
  * Controller aggregates the maximum `storageVersion` of all bound drawers plus its own increments when `drawerSlots` are rebuilt (multiblock formed/broken).
  * Updated whenever any member drawer increments its version (via `IControlGroup` listener).

* **Synchronized with Fabric Transfer API in `DrawerStorageImpl.java`** (`fabric/src/main/java/com/jaquadro/minecraft/storagedrawers/capabilities/DrawerStorageImpl.java` and `ControllerStorageImpl.java`):
  ```java
  @Override
  public long getVersion() {
      if (entity instanceof BlockEntityDrawers drawerEntity) {
          return drawerEntity.getStorageVersion();
      }
      if (entity instanceof BlockEntityController controllerEntity) {
          return controllerEntity.getStorageVersion();
      }
      return super.getVersion();
  }
  ```
  * `insert()` and `extract()` now call `incrementStorageVersion()` before returning, ensuring `Storage.getVersion()` monotonically increases.
  * Tom's Storage can now implement efficient live updates:
    ```java
    long current = storage.getVersion();
    if (current != lastSeenVersion) {
        lastSeenVersion = current;
        rebuildTerminalView();
    }
    ```
  * No polling of slot contents required; version check is O(1).

* NeoForge path remains unaffected (uses `Capabilities.Item.BLOCK` invalidation via `invalidateCapabilities()`), but the same `storageVersion` field is present for cross-loader consistency and future use.

---

## Issue 8: Void Upgrade Priority Alignment and Shift-Click Routing Engine

### Problem Description
Two related routing bugs existed when shift-clicking items from player inventory into a drawer network or when external mods inserted excess items:

1. **Empty Unlocked Drawers Stole Items**: An empty unlocked drawer would accept any item, even when a matching drawer with available capacity, a locked empty drawer reserved for that item, or a void drawer for that item existed elsewhere in the network. This scattered items across empty drawers instead of consolidating them.
2. **Void Drawers Did Not Absorb Excess**: When a drawer with a Void Upgrade was full, excess items that should have been voided were instead bounced back to the player or routed to an empty unlocked drawer, defeating the purpose of void upgrades.

### Root Cause
1. `BlockEntityController.sortSlotRecords()` used a 3-tier priority (matching with capacity > locked empty > unlocked empty) but treated **void drawers as normal drawers**. A full void drawer was considered "no capacity" and thus deprioritized below unlocked empty drawers.
2. `DrawerStorageImpl.canItemBeStored()` returned `true` for any empty unlocked drawer, giving it equal eligibility to void drawers during insertion.
3. Shift-click handling in `BlockDrawers` / `InventoryHelper` iterated drawers in world-scan order rather than priority order, so the first empty drawer encountered won the insertion.

### How the Fix Was Made
* **Re-Aligned `sortSlotRecords` Priority in `BlockEntityController.java`** to a 4-tier system that respects void semantics:
  1. **Tier 1 - Matching With Capacity**: Drawers where `drawer.getStoredItemPrototype().equals(item)` and `drawer.getRemainingCapacity() > 0` (including void drawers that still have space).
  2. **Tier 2 - Locked Empty Matching**: Empty drawers that are locked to the incoming item prototype (`drawer.isLocked() && drawer.getStoredItemPrototype().equals(item)`). Preserves reservation semantics.
  3. **Tier 3 - Void Drawers Matching (Full)**: Drawers with `UpgradeVoid` where `drawer.getStoredItemPrototype().equals(item)` even if `getRemainingCapacity() == 0`. These are prioritized **above** unlocked empty drawers so excess is voided rather than scattered. Insertion logic in `DrawerStorageImpl.insert()` detects this tier and returns `consumed = amount` while voiding the overflow (`storedCount` capped at capacity).
  4. **Tier 4 - Unlocked Empty Drawers**: Empty, unlocked, non-void drawers. Only used as last resort when no matching drawer exists.

  ```java
  private void sortSlotRecords(ItemStack item) {
      drawerSlots.sort(Comparator
          .comparingInt((SlotRecord r) -> getPriority(r, item))
          .thenComparingInt(r -> r.slot)
      );
  }
  private int getPriority(SlotRecord record, ItemStack item) {
      IDrawer drawer = record.drawer;
      if (drawer.isEmpty()) {
          if (drawer.isLocked() && drawer.getStoredItemPrototype().is(item.getItem())) return 1;
          if (hasVoidUpgrade(drawer)) return 2; // void empty also tier 2 if locked, else tier 3
          return 3; // unlocked empty - lowest
      }
      if (drawer.getStoredItemPrototype().is(item.getItem())) {
          if (drawer.getRemainingCapacity() > 0) return 0;
          if (hasVoidUpgrade(drawer)) return 2; // full void absorbs
      }
      return 4; // non-matching
  }
  ```

* **Void Absorption in `DrawerStorageImpl.java`** (`common` + `fabric`/`neoforge` capabilities):
  ```java
  @Override
  public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
      IDrawer target = findTargetDrawer(resource);
      if (target != null && hasVoidUpgrade(target) && target.getStoredItemPrototype().equals(resource)) {
          long inserted = Math.min(maxAmount, target.getRemainingCapacity());
          long voided = maxAmount - inserted;
          // ... store inserted, void remainder, increment storageVersion
          updateSnapshot(tx, () -> incrementStorageVersion());
          return maxAmount; // report fully consumed
      }
      return super.insert(resource, maxAmount, tx);
  }
  ```
  * Ensures external networks see the insertion as fully successful, preventing bounce-back.

* **Shift-Click Routing Engine** (`common/src/main/java/com/jaquadro/minecraft/storagedrawers/util/InventoryHelper.java` and `BlockDrawers` interaction):
  * Shift-click from player inventory now delegates to `BlockEntityController.insert()` with the same priority queue instead of iterating drawers in scan order.
  * Empty unlocked drawers **defer** to any Tier 1-3 drawer; they are only considered if no matching drawer exists in the entire network.
  * Locked empty drawers retain their reservation and are preferred over unlocked empties.
  * Result: shift-clicking 64 cobblestone into a network with 1 cobblestone drawer (half full), 1 locked empty cobblestone drawer, 1 void cobblestone drawer (full), and 5 empty unlocked drawers will correctly fill the half-full drawer first, then void excess via the void drawer, never scattering into the 5 empties.

---

## Summary of Modified and Created Files

| File | Module | Type | Description |
| :--- | :---: | :---: | :--- |
| `common/.../api/storage/INetworked.java` | Common | Modified | Added `isBoundToActiveController()` helper method |
| `common/.../api/storage/IControlGroup.java` | Common | Modified | Added `getConnectedDrawerPositions()` for reflection-free multiblock discovery (Issue 6) |
| `common/.../block/entity/BlockEntityController.java` | Common | Modified | Implemented `getConnectedDrawerPositions()`, added `storageVersion` aggregation, re-aligned `sortSlotRecords` priority for void upgrades and shift-click routing (Issues 6, 7, 8) |
| `common/.../block/entity/BlockEntityDrawers.java` | Common | Modified | Added `storageVersion` counter synchronized with `Storage.getVersion()` (Issue 7) |
| `common/.../capabilities/DrawerStorageImpl.java` | Common/Fabric | Modified | Void absorption logic, version increment on insert/extract, `getVersion()` delegation (Issues 7, 8) |
| `common/.../util/InventoryHelper.java` | Common | Modified | Shift-click routing engine deferring empty unlocked drawers (Issue 8) |
| `common/.../data/storagedrawers/tags/block/drawers.json` | Common | Modified | Expanded `#storagedrawers:drawers` to include compacting & framed drawers (Issue 5) |
| `common/.../data/storagedrawers/tags/block/standard_drawers.json` | Common | Created | Sub-tag for standard drawer variants |
| `common/.../data/storagedrawers/tags/block/compacting_drawers.json` | Common | Created | Sub-tag for compacting drawer variants |
| `common/.../data/storagedrawers/tags/block/framed_drawers.json` | Common | Created | Sub-tag for framed drawer variants |
| `common/.../data/storagedrawers/tags/block/trim.json` | Common | Modified | Expanded `#storagedrawers:trim` to include framed trim (Issue 5) |
| `common/.../data/toms_storage/tags/block/trims.json` | Common | Modified | Tom's Storage trim tag compatibility (delegates to `#storagedrawers:drawers` + `#storagedrawers:trim`) |
| `common/.../data/c/tags/block/storage_trims.json` | Common | Modified | Conventional storage trim tag compatibility |
| `common/.../data/c/tags/block/trims.json` | Common | Created | Conventional trim tag compatibility |
| `fabric/.../capabilities/PlatformCapabilities.java` | Fabric | Modified | Controller-aware `ItemStorage.SIDED` registration |
| `fabric/.../capabilities/ControllerStorageImpl.java` | Fabric | Modified | `getVersion()` delegation to `BlockEntityController.storageVersion` (Issue 7) |
| `fabric/.../integration/Waila.java` | Fabric | Created | Native Jade `IWailaPlugin` for Fabric |
| `fabric/.../integration/DrawerOverlay.java` | Fabric | Created | Custom tooltip overlay renderer for Jade |
| `fabric/src/main/resources/fabric.mod.json` | Fabric | Modified | Added `"jade"` plugin entrypoint |
| `fabric/build.gradle.kts` | Fabric | Modified | Added Jade Fabric dependency |
| `neoforge/.../capabilities/PlatformCapabilities.java` | NeoForge | Modified | Controller-aware `Capabilities.Item.BLOCK` registration |
| `neoforge/.../integration/DrawerOverlay.java` | NeoForge | Modified | Enabled `addContent()` for Jade overlay |