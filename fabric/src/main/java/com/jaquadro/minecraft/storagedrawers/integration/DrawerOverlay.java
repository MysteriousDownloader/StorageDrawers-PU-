package com.jaquadro.minecraft.storagedrawers.integration;

import com.jaquadro.minecraft.storagedrawers.api.storage.*;
import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.capabilities.Capabilities;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DrawerOverlay {
    public boolean showContent = true;
    public boolean showStackLimit = true;
    public boolean showStatus = true;
    public boolean showStackRemainder;
    public boolean respectQuantifyKey;

    public DrawerOverlay() {
        showStackRemainder = ModCommonConfig.INSTANCE.INTEGRATION.waila.stackRemainder.get();
        respectQuantifyKey = ModCommonConfig.INSTANCE.INTEGRATION.waila.respectQuantifyKey.get();
    }

    public List<Component> getOverlay(final BlockEntityDrawers tile) {
        final List<Component> result = new ArrayList<>();
        if (tile == null)
            return result;

        IDrawerAttributes attr = tile.getCapability(Capabilities.DRAWER_ATTRIBUTES);
        if (attr == null)
            attr = EmptyDrawerAttributes.EMPTY;

        addContent(result, tile, attr);
        addStackLimit(result, tile, attr);
        addStatus(result, tile, attr);

        return result;
    }

    private void addContent(final List<Component> result, final BlockEntityDrawers tile, final IDrawerAttributes attr) {
        // ponytail: Content now rendered as native Jade item icon grid via IServerExtensionProvider<ViewGroup<ItemStack>>
        // (Waila.DrawerItemStorageProvider). Keep text line suppressed to avoid duplicate "#1: [Item]" lines.
        // If that provider is disabled or Jade absent, capacity/status lines below still show.
    }

    private void addStackLimit(List<Component> result, BlockEntityDrawers tile, IDrawerAttributes attr) {
        if (!this.showStackLimit) return;

        if (attr.isUnlimitedStorage() || tile.getDrawerAttributes().isUnlimitedVending())
            result.add(Component.translatable("tooltip.storagedrawers.waila.nolimit"));
        else {
            int multiplier = tile.upgrades().getStorageMultiplier();
            int limit = tile.getEffectiveDrawerCapacity();
            try {
                limit = Math.multiplyExact(limit, multiplier);
            } catch (ArithmeticException e) {
                limit = Integer.MAX_VALUE / 64;
            }

            result.add(Component.translatable("tooltip.storagedrawers.waila.limit", limit, multiplier));
        }
    }

    private void addStatus(List<Component> result, BlockEntityDrawers tile, IDrawerAttributes attr) {
        if (!this.showStatus) return;

        List<MutableComponent> attribs = new ArrayList<>();
        if (attr.isItemLocked(LockAttribute.LOCK_POPULATED))
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.locked"));
        if (attr.isVoid())
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.void"));
        if (attr.isBalancedFill())
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.balanced"));
        if (tile.getOwner() != null)
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.protected"));
        if (attr.isMagnet())
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.magnetic"));
        if (attr.isSuspended())
            attribs.add(Component.translatable("tooltip.storagedrawers.waila.suspended"));

        if (!attribs.isEmpty())
            result.add(attribs.stream().reduce((a, b) ->
                    a.append(Component.literal(", ")).append(b)).get());
    }
}