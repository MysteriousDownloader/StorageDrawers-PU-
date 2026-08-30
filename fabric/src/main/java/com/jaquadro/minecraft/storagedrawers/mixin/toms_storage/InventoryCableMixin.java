package com.jaquadro.minecraft.storagedrawers.mixin.toms_storage;

import com.jaquadro.minecraft.storagedrawers.block.BlockController;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.BlockTrim;
import com.tom.storagemod.StorageTags;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.tom.storagemod.block.IInventoryCable", remap = false)
public interface InventoryCableMixin {
    @Inject(method = "canConnect", at = @At("HEAD"), cancellable = true)
    private static void storagedrawers$canConnectToStorage(BlockState state, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (state == null)
            return;

        if (state.is(StorageTags.TRIMS)
            || state.is(BlockTags.SHULKER_BOXES)
            || state.getBlock() instanceof BlockDrawers
            || state.getBlock() instanceof BlockController
            || state.getBlock() instanceof BlockTrim
            || state.getBlock() instanceof ChestBlock
            || state.getBlock() instanceof BarrelBlock
            || state.getBlock() instanceof ShulkerBoxBlock
            || state.getBlock() instanceof HopperBlock
            || state.getBlock() instanceof DispenserBlock
            || state.getBlock() instanceof DropperBlock
            || state.getBlock() instanceof CrafterBlock
            || state.getBlock() instanceof ChiseledBookShelfBlock) {
            cir.setReturnValue(true);
        }
    }
}
