package com.texelsaurus.minecraft.chameleon.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ReplacementBlockPart implements ChameleonBlockModelPart
{
    protected BlockModelPart parent;
    private TextureAtlasSprite sprite;
    private List<BakedQuad> quads = new ArrayList<>();

    public ReplacementBlockPart(BlockModelPart part, TextureAtlasSprite sprite) {
        parent = part;
        this.sprite = sprite;

        part.getQuads(null).forEach(quad -> quads.add(remapQuad(quad, sprite)));
        for (Direction dir : Direction.values()) {
            part.getQuads(dir).forEach(quad -> quads.add(remapQuad(quad, sprite)));
        }
    }

    public ReplacementBlockPart (BlockModelPart parent, BlockModelPart replacement) {
        this(parent, replacement.particleIcon());
    }

    @Override
    public List<BakedQuad> getQuads (@Nullable Direction direction) {
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion () {
        return parent.useAmbientOcclusion();
    }

    @Override
    public TextureAtlasSprite particleIcon () {
        if (sprite == null)
            return parent.particleIcon();

        return sprite;
    }

    BakedQuad remapQuad (BakedQuad quad, TextureAtlasSprite sprite) {
        int[] vertices = quad.vertices().clone();

        for(int i = 0; i < 4; ++i) {
            int blk = DefaultVertexFormat.BLOCK.getVertexSize() / 4 * i;
            int offset = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.UV) / 4;
            vertices[blk + offset] = Float.floatToRawIntBits(sprite.getU(getUnInterpolatedU(quad.sprite(), Float.intBitsToFloat(vertices[blk + offset]))));
            vertices[blk + offset + 1] = Float.floatToRawIntBits(sprite.getV(getUnInterpolatedV(quad.sprite(), Float.intBitsToFloat(vertices[blk + offset + 1]))));
        }

        return new BakedQuad(vertices, quad.tintIndex(), quad.direction(), sprite, quad.shade(), quad.lightEmission());
    }

    private float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
        float diff = sprite.getU1() - sprite.getU0();
        return (u - sprite.getU0()) / diff;
    }

    private float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
        float diff = sprite.getV1() - sprite.getV0();
        return (v - sprite.getV0()) / diff;
    }
}
