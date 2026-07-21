package com.jaquadro.minecraft.storagedrawers.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.render.ChameleonBlockModelPart;
import com.texelsaurus.minecraft.chameleon.render.ReplacementBlockPart;
import com.texelsaurus.minecraft.chameleon.service.ChameleonRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpriteReplacementModel extends ParentModel
{
    private TextureAtlasSprite sprite;
    private ChunkSectionLayer layer;
    private Map<BlockModelPart, ChameleonBlockModelPart> cache = new HashMap<>();

    public SpriteReplacementModel (@NotNull BlockStateModel parent, TextureAtlasSprite sprite) {
        super(parent);
        this.sprite = sprite;
    }

    public SpriteReplacementModel (@NotNull BlockStateModel parent, BlockStateModel replacement, ChunkSectionLayer renderLayer) {
        super(parent);
        this.sprite = replacement.particleIcon();
        this.layer = renderLayer;
    }

    public SpriteReplacementModel (@NotNull BlockStateModel parent, ItemStack stack, ChunkSectionLayer renderLayer) {
        super(parent);

        if (stack != null && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockRenderDispatcher disp = Minecraft.getInstance().getBlockRenderer();
            BlockStateModel model = disp.getBlockModel(block.defaultBlockState());
            sprite = model.particleIcon();
        }

        layer = renderLayer;
    }

    public SpriteReplacementModel (@NotNull BlockStateModel parent, ItemStack stack) {
        this(parent, stack, null);
    }

    @Override
    public void collectParts (RandomSource randomSource, List<BlockModelPart> list) {
        if (sprite == null) {
            super.collectParts(randomSource, list);
            return;
        }

        parent.collectParts(randomSource).forEach(part -> {
            if (cache.containsKey(part))
                list.add(cache.get(part));
            else {
                ChameleonBlockModelPart replacement = ChameleonServices.RENDER.createReplacementPart(part, sprite);
                replacement.setRenderType(layer);

                //if (cache.size() < 10)
                //    cache.put(part, replacement);

                list.add(replacement);
            }
        });
    }

    @Override
    public TextureAtlasSprite particleIcon () {
        if (sprite == null)
            return super.particleIcon();

        return sprite;
    }

    /*private static class ReplacementBlockPart implements BlockModelPart
    {
        private BlockModelPart parent;
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
    }*/
}