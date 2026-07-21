package com.texelsaurus.minecraft.chameleon.render;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

/**
 * Marker type for model parts produced by Chameleon.
 *
 * In 26.2 the chunk section layer is a per-quad property baked into
 * {@code BakedQuad.MaterialInfo}, so a part can no longer be told which render type to use after
 * its quads have been built. The old {@code setRenderType}/{@code getRenderType} pair is therefore
 * gone; the desired layer is passed to
 * {@link com.texelsaurus.minecraft.chameleon.service.ChameleonRender#createReplacementPart} instead.
 */
public interface ChameleonBlockModelPart extends BlockStateModelPart
{
}
