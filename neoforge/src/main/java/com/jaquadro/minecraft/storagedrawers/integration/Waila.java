package com.jaquadro.minecraft.storagedrawers.integration;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.config.ModClientConfig;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.ui.ItemStackElement;

import java.util.ArrayList;
import java.util.List;

@WailaPlugin(StorageDrawers.MOD_ID)
public class Waila implements IWailaPlugin
{
    @Override
    public void registerClient (IWailaClientRegistration registration) {
        if (!ModCommonConfig.INSTANCE.INTEGRATION.waila.enable.get()
            || !ModClientConfig.INSTANCE.INTEGRATION.enableWaila.get())
            return;

        registration.addConfig(StorageDrawers.rl("display.content"), true);
        registration.addConfig(StorageDrawers.rl("display.stacklimit"), true);
        registration.addConfig(StorageDrawers.rl("display.status"), true);

        WailaDrawer provider = new WailaDrawer();
        registration.registerBlockComponent(provider, BlockDrawers.class);
    }

    @Override
    public void register (IWailaCommonRegistration registration) {
        if (!ModCommonConfig.INSTANCE.INTEGRATION.waila.enable.get())
            return;

        registration.registerItemStorage(new DrawerItemStorageProvider(), BlockEntityDrawers.class);
    }

    public static class DrawerItemStorageProvider implements IServerExtensionProvider<ItemStack>
    {
        @Override
        public List<ViewGroup<ItemStack>> getGroups (Accessor<?> accessor) {
            BlockEntity blockEntity = null;
            if (accessor instanceof BlockAccessor blockAccessor)
                blockEntity = blockAccessor.getBlockEntity();
            else if (accessor.getTarget() instanceof BlockEntity be)
                blockEntity = be;

            if (!(blockEntity instanceof BlockEntityDrawers tile))
                return List.of();

            var group = tile.getGroup();
            if (group == null)
                return List.of();

            var stacks = new ArrayList<ItemStack>();
            for (int i = 0; i < group.getDrawerCount(); i++) {
                var drawer = group.getDrawer(i);
                if (!drawer.isEnabled() || drawer.isEmpty())
                    continue;
                ItemStack proto = drawer.getStoredItemPrototype();
                if (proto.isEmpty())
                    continue;
                stacks.add(proto.copyWithCount(drawer.getStoredItemCount()));
            }
            if (stacks.isEmpty())
                return List.of();
            return List.of(new ViewGroup<>(stacks));
        }

        @Override
        public Identifier getUid () {
            return Identifier.fromNamespaceAndPath(StorageDrawers.MOD_ID, "drawer_contents");
        }
    }

    public static class WailaDrawer implements IBlockComponentProvider
    {
        @Override
        public @Nullable Element getIcon (BlockAccessor accessor, IPluginConfig config, Element currentIcon) {
            return ItemStackElement.of(new ItemStack(accessor.getBlock()));
        }

        @Override
        public void appendTooltip (ITooltip currenttip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof BlockEntityDrawers blockEntityDrawers))
                return;

            boolean showContent = config.get(StorageDrawers.rl("display.content"));
            IDrawerGroup group = blockEntityDrawers.getGroup();
            if (showContent && group != null) {
                boolean first = true;
                for (int i = 0; i < group.getDrawerCount(); i++) {
                    IDrawer drawer = group.getDrawer(i);
                    if (!drawer.isEnabled() || drawer.isEmpty())
                        continue;
                    ItemStack proto = drawer.getStoredItemPrototype();
                    if (proto.isEmpty())
                        continue;
                    ItemStack stack = proto.copyWithCount(drawer.getStoredItemCount());
                    Element itemElement = ItemStackElement.of(stack);
                    if (first) {
                        currenttip.add(itemElement);
                        first = false;
                    } else {
                        currenttip.append(itemElement);
                    }
                }
            }

            DrawerOverlay overlay = new DrawerOverlay();
            overlay.showContent = showContent;
            overlay.showStackLimit = config.get(StorageDrawers.rl("display.stacklimit"));
            overlay.showStatus = config.get(StorageDrawers.rl("display.status"));

            currenttip.addAll(overlay.getOverlay(blockEntityDrawers));
        }

        @Override
        public Identifier getUid () {
            return Identifier.fromNamespaceAndPath(StorageDrawers.MOD_ID, "main");
        }
    }
}