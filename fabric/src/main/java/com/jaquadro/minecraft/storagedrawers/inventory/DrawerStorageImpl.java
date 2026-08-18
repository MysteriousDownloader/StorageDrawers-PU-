package com.jaquadro.minecraft.storagedrawers.inventory;

import com.google.common.collect.MapMaker;
import com.jaquadro.minecraft.storagedrawers.api.storage.Drawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DrawerStorageImpl extends CombinedStorage<ItemVariant, SingleSlotStorage<ItemVariant>> implements SlottedStorage<ItemVariant>
{
    private static final Map<IDrawerGroup, DrawerStorageImpl> WRAPPERS = new MapMaker().weakValues().makeMap();

    public static DrawerStorageImpl of (IDrawerGroup group) {
        DrawerStorageImpl storage = WRAPPERS.computeIfAbsent(group, DrawerStorageImpl::new);
        storage.resizeSlotList();
        return storage;
    }

    final IDrawerGroup group;
    final List<DrawerStackStorage> backingList;

    public DrawerStorageImpl (IDrawerGroup group) {
        super(Collections.emptyList());
        this.group = group;
        backingList = new ArrayList<>();
    }

    @Override
    public @UnmodifiableView List<SingleSlotStorage<ItemVariant>> getSlots () {
        return parts;
    }

    private void resizeSlotList() {
        int[] slots = group.getAccessibleDrawerSlots();

        if (slots.length != parts.size()) {
            while (backingList.size() < slots.length)
                backingList.add(new DrawerStackStorage(this, backingList.size()));

            parts = Collections.unmodifiableList(backingList.subList(0, slots.length));
        }

        for (int i = 0; i < slots.length; i++)
            backingList.get(i).updateSlot(slots[i]);
    }

    @Override
    public int getSlotCount () {
        return getSlots().size();
    }

    @Override
    public SingleSlotStorage<ItemVariant> getSlot (int slot) {
        return getSlots().get(slot);
    }

    IDrawer getDrawer (int slot) {
        if (slot < 0 || slot >= group.getDrawerCount())
            return Drawers.DISABLED;

        return group.getDrawer(slot);
    }

    @Override
    public long insert (ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        if (maxAmount == 0)
            return 0;

        long remaining = maxAmount;
        long insertedTotal = 0;

        // Pass 1: matching populated drawers with remaining capacity
        for (SingleSlotStorage<ItemVariant> slot : parts) {
            if (remaining == 0) break;
            if (!(slot instanceof DrawerStackStorage dss)) continue;
            IDrawer drawer = getDrawer(dss.slot);
            if (drawer.isEmpty()) continue;
            if (drawer.getRemainingCapacity() <= 0) continue;
            if (!drawer.canItemBeStored(resource.toStack())) continue;
            // must be matching item (same as drawer's prototype)
            // canItemBeStored already validates match for non-empty drawers
            long inserted = slot.insert(resource, remaining, transaction);
            insertedTotal += inserted;
            remaining -= inserted;
        }
        if (remaining == 0) return insertedTotal;

        // Pass 2: locked empty drawers that accept this item
        for (SingleSlotStorage<ItemVariant> slot : parts) {
            if (remaining == 0) break;
            if (!(slot instanceof DrawerStackStorage dss)) continue;
            IDrawer drawer = getDrawer(dss.slot);
            if (!drawer.isEmpty()) continue;
            if (drawer.isMissing() || !drawer.isEnabled()) continue;
            // locked empty means it still accepts this specific item but not arbitrary items
            // heuristic: canItemBeStored true but would reject a different item probe
            // Use: locked if getDrawerAttributes indicates LOCK_EMPTY or matcher restricts
            // We approximate by checking that drawer.canItemBeStored succeeds for this resource
            // but the group already has no matching populated slot with room (handled in pass 1),
            // so any locked empty matching this item should be preferred over unlocked empties.
            // Detect locked empty: drawer is empty but canItemBeStored(resource) while
            // canItemBeStored for a probe "barrier" would fail. Use attribute check.
            var attrs = drawer.getAttributes();
            // ponytail: ceiling — if lock semantics change, this check may need updating to
            // consult IDrawerAttributes.isItemLocked(LOCK_EMPTY) on the group's attribute provider.
            boolean isLockedEmpty = attrs != null && attrs.isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY);
            if (!isLockedEmpty) continue;
            if (!drawer.canItemBeStored(resource.toStack())) continue;
            long inserted = slot.insert(resource, remaining, transaction);
            insertedTotal += inserted;
            remaining -= inserted;
        }
        if (remaining == 0) return insertedTotal;

        // Pass 3: unlocked empty drawers (catch-all)
        for (SingleSlotStorage<ItemVariant> slot : parts) {
            if (remaining == 0) break;
            if (!(slot instanceof DrawerStackStorage dss)) continue;
            IDrawer drawer = getDrawer(dss.slot);
            if (!drawer.isEmpty()) continue;
            if (drawer.isMissing() || !drawer.isEnabled()) continue;
            var attrs = drawer.getAttributes();
            boolean isLockedEmpty = attrs != null && attrs.isItemLocked(com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute.LOCK_EMPTY);
            if (isLockedEmpty) continue;
            if (!drawer.canItemBeStored(resource.toStack())) continue;
            long inserted = slot.insert(resource, remaining, transaction);
            insertedTotal += inserted;
            remaining -= inserted;
        }
        return insertedTotal;
    }
}
