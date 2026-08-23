package com.jaquadro.minecraft.storagedrawers.mixin.toms_storage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityControllerIO;
import com.jaquadro.minecraft.storagedrawers.inventory.DrawerStorageImpl;
import com.tom.storagemod.util.Priority;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.tom.storagemod.inventory.PlatformInventoryAccess$BlockInventoryAccess", remap = false)
public abstract class BlockInventoryAccessMixin implements Priority.IPriority {
    @Shadow
    public abstract Object get();

    @Override
    public Priority getPriority() {
        Object storage = get();
        if (storage instanceof DrawerStorageImpl dsi) {
            IDrawerGroup group = dsi.getGroup();
            if (group instanceof BlockEntityController || group instanceof BlockEntityControllerIO) {
                return Priority.HIGHEST;
            }
            return Priority.HIGH;
        }
        return Priority.NORMAL;
    }
}