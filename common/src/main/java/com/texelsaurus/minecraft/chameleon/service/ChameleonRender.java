package com.texelsaurus.minecraft.chameleon.service;

import com.texelsaurus.minecraft.chameleon.render.ChameleonBlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.sprite.Material;
import org.jetbrains.annotations.Nullable;

public interface ChameleonRender
{
    /**
     * @param layer the chunk section layer the returned part renders in, or null to let each quad's
     *     own alpha content decide. It must be supplied here because the layer is baked into every
     *     quad and cannot be changed afterwards.
     */
    ChameleonBlockModelPart createReplacementPart (BlockStateModelPart part, Material.Baked material, @Nullable ChunkSectionLayer layer);
}
