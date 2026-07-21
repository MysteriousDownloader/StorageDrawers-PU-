package com.jaquadro.minecraft.storagedrawers.client.model.decorator;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * The mod's own multi-pass key. In 26.2 a model can no longer choose a render type -- the chunk
 * section layer is derived per quad from the sprite's alpha content -- so this enum survives only
 * as a pass selector. {@link #toChunkType} is what now carries the intent: it is threaded through
 * SpriteReplacementModel into each replacement quad's material info.
 */
public enum DecoratorRenderType
{
    SOLID,
    CUTOUT,
    TRANSLUCENT;

    public static DecoratorRenderType fromItemType (ChunkSectionLayer renderType) {
        if (renderType == null)
            return null;

        return switch (renderType) {
            case SOLID -> DecoratorRenderType.SOLID;
            case CUTOUT -> DecoratorRenderType.CUTOUT;
            case TRANSLUCENT -> DecoratorRenderType.TRANSLUCENT;
        };
    }

    // 26.2 has no solid item sheet: vanilla routes opaque block-atlas quads through the cutout
    // block item sheet, so SOLID is no longer distinguishable from CUTOUT here and is never
    // returned.
    public static DecoratorRenderType fromItemType (RenderType renderType) {
        if (renderType == Sheets.cutoutBlockItemSheet() || renderType == Sheets.cutoutItemSheet())
            return DecoratorRenderType.CUTOUT;
        if (renderType == Sheets.translucentBlockItemSheet() || renderType == Sheets.translucentItemSheet())
            return DecoratorRenderType.TRANSLUCENT;
        return null;
    }

    public static ChunkSectionLayer toChunkType (DecoratorRenderType renderType) {
        if (renderType == null)
            return null;

        return switch (renderType) {
            case SOLID -> ChunkSectionLayer.SOLID;
            case CUTOUT -> ChunkSectionLayer.CUTOUT;
            case TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
        };
    }

    // These are block models painted with sprites from the block atlas, so they take the *Block*
    // item sheets -- the same choice BakedQuad.MaterialInfo.of makes for any sprite whose atlas is
    // TextureAtlas.LOCATION_BLOCKS. SOLID and CUTOUT deliberately collapse to one sheet.
    public static RenderType toItemType (DecoratorRenderType renderType) {
        if (renderType == null)
            return null;

        return switch (renderType) {
            case SOLID -> Sheets.cutoutBlockItemSheet();
            case CUTOUT -> Sheets.cutoutBlockItemSheet();
            case TRANSLUCENT -> Sheets.translucentBlockItemSheet();
        };
    }
}
