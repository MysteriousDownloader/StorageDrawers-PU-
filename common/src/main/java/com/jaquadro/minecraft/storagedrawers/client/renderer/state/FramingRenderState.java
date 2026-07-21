package com.jaquadro.minecraft.storagedrawers.client.renderer.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.level.block.state.BlockState;

public class FramingRenderState extends BlockEntityRenderState
{
    // BlockEntityRenderState.blockState became private in 26.2 with no accessor, so
    // renderers that need the state must capture it themselves during extraction.
    public BlockState blockState;

    public ItemStackRenderState mainSlotItem;
    public ItemStackRenderState sideSlotItem;
    public ItemStackRenderState frontSlotItem;
    public ItemStackRenderState trimSlotItem;

    public FramingRenderState () { }
}
