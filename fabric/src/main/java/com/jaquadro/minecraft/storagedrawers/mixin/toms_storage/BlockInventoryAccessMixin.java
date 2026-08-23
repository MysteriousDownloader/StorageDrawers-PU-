package com.jaquadro.minecraft.storagedrawers.mixin.toms_storage;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawerGroup;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityControllerIO;
import com.jaquadro.minecraft.storagedrawers.inventory.DrawerStorageImpl;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.tom.storagemod.inventory.PlatformInventoryAccess", remap = false)
public abstract class BlockInventoryAccessMixin {
    @Shadow
    public abstract Object get();

    @Dynamic
    public Object getPriority() {
        Object storage = get();
        if (storage instanceof DrawerStorageImpl dsi) {
            try {
                Class<?> priorityClass = Class.forName("com.tom.storagemod.util.Priority");
                IDrawerGroup group = dsi.getGroup();
                if (group instanceof BlockEntityController || group instanceof BlockEntityControllerIO) {
                    return priorityClass.getField("HIGHEST").get(null);
                }
                return priorityClass.getField("HIGH").get(null);
            } catch (Throwable ignored) {}
        }
        try {
            return Class.forName("com.tom.storagemod.util.Priority").getField("NORMAL").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
