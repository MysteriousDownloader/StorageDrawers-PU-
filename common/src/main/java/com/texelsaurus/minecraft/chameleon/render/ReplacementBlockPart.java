package com.texelsaurus.minecraft.chameleon.render;

import com.mojang.blaze3d.platform.Transparency;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a baked model part and re-textures every one of its quads onto a different sprite,
 * preserving the original geometry and the relative (0..1) position of each vertex within the
 * source sprite.
 */
public class ReplacementBlockPart implements ChameleonBlockModelPart
{
    protected final BlockStateModelPart parent;

    private final Material.Baked material;
    private final ChunkSectionLayer layer;
    private final Map<Direction, List<BakedQuad>> faceQuads = new EnumMap<>(Direction.class);
    private final List<BakedQuad> unculledQuads = new ArrayList<>();
    private final int materialFlags;

    /**
     * @param part the part whose geometry is reused
     * @param material the sprite every quad is remapped onto
     * @param layer the chunk section layer to render in, or null to let each quad's own alpha
     *     content decide. See {@link #resolveTransparency}.
     */
    public ReplacementBlockPart (BlockStateModelPart part, Material.Baked material, @Nullable ChunkSectionLayer layer) {
        this.parent = part;
        this.material = material;
        this.layer = layer;

        int flags = 0;

        for (BakedQuad quad : part.getQuads(null)) {
            BakedQuad remapped = remapQuad(quad);
            unculledQuads.add(remapped);
            flags |= remapped.materialInfo().flags();
        }

        // Quads must stay in the bucket the parent put them in: ModelBlockRenderer asks for each of
        // the six directions and then for the unculled bucket, so a part that answers every call
        // with one merged list emits each quad up to seven times.
        for (Direction dir : Direction.values()) {
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedQuad quad : part.getQuads(dir)) {
                BakedQuad remapped = remapQuad(quad);
                quads.add(remapped);
                flags |= remapped.materialInfo().flags();
            }

            faceQuads.put(dir, quads);
        }

        this.materialFlags = flags;
    }

    public ReplacementBlockPart (BlockStateModelPart parent, BlockStateModelPart replacement, @Nullable ChunkSectionLayer layer) {
        this(parent, replacement.particleMaterial(), layer);
    }

    @Override
    public List<BakedQuad> getQuads (@Nullable Direction direction) {
        if (direction == null)
            return unculledQuads;

        return faceQuads.getOrDefault(direction, List.of());
    }

    @Override
    public boolean useAmbientOcclusion () {
        return parent.useAmbientOcclusion();
    }

    @Override
    public Material.Baked particleMaterial () {
        if (material == null)
            return parent.particleMaterial();

        return material;
    }

    @Override
    public int materialFlags () {
        return materialFlags;
    }

    private BakedQuad remapQuad (BakedQuad quad) {
        if (material == null)
            return quad;

        BakedQuad.MaterialInfo source = quad.materialInfo();
        TextureAtlasSprite from = source.sprite();
        TextureAtlasSprite to = material.sprite();

        long[] packedUV = new long[BakedQuad.VERTEX_COUNT];
        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;

        for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
            long uv = quad.packedUV(i);
            float u = unlerp(from.getU0(), from.getU1(), UVPair.unpackU(uv));
            float v = unlerp(from.getV0(), from.getV1(), UVPair.unpackV(uv));

            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);

            packedUV[i] = UVPair.pack(to.getU(u), to.getV(v));
        }

        Transparency transparency;
        if (material.forceTranslucent())
            transparency = Transparency.TRANSLUCENT;
        else {
            transparency = requestedTransparency(layer);

            // Only classify from pixels when nothing else has decided: computeTransparency walks
            // the sprite image, and replacement parts are rebuilt on every collectParts call. This
            // is the same rule FaceBakery applies at bake time, over this quad's own sub-rect.
            if (transparency == null)
                transparency = to.contents().computeTransparency(clamp01(minU), clamp01(minV), clamp01(maxU), clamp01(maxV));
        }

        BakedQuad.MaterialInfo info = BakedQuad.MaterialInfo.of(material, transparency,
            source.tintIndex(), source.shade(), source.lightEmission());

        // Positions are reused by reference: Vector3fc is a read-only view and only the UVs change.
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
            packedUV[0], packedUV[1], packedUV[2], packedUV[3], quad.direction(), info);
    }

    /**
     * Picks the transparency a replaced quad is stamped with, in the same precedence order
     * {@link #remapQuad} uses.
     *
     * A material flagged forceTranslucent wins outright, as it does in vanilla's own FaceBakery.
     * Failing that a requested layer wins, as it did before 26.2: the mod already decides which
     * pass a framed material renders in, from its own opacity data and the
     * renderTranslucentMaterials config, and classifying from pixel data instead would silently
     * override that config. SOLID is the exception -- it is read as "no opinion" and lets the sprite
     * classify itself, because forcing a sprite that has holes into the solid buffer renders the
     * holes opaque. Nothing passes SOLID today; the guard exists because the constructor is public.
     *
     * Expressing the choice as a transparency, rather than patching MaterialInfo.layer afterwards,
     * keeps the chunk layer and the item render type consistent: MaterialInfo.of derives both from
     * this one value, and the item sheet it picks then agrees with the one
     * DecoratorRenderType.toItemType binds for the same pass.
     *
     * @param spriteTransparency the transparency the replacement sprite's own pixels imply
     */
    public static Transparency resolveTransparency (Material.Baked material, Transparency spriteTransparency, @Nullable ChunkSectionLayer requested) {
        if (material.forceTranslucent())
            return Transparency.TRANSLUCENT;

        Transparency requestedTransparency = requestedTransparency(requested);
        return (requestedTransparency != null) ? requestedTransparency : spriteTransparency;
    }

    /**
     * The transparency a requested chunk section layer implies, or null if the request carries no
     * opinion and the sprite should classify itself.
     */
    @Nullable
    private static Transparency requestedTransparency (@Nullable ChunkSectionLayer requested) {
        if (requested == null)
            return null;

        return switch (requested) {
            case SOLID -> null;
            case CUTOUT -> Transparency.TRANSPARENT;
            case TRANSLUCENT -> Transparency.TRANSLUCENT;
        };
    }

    private static float unlerp (float lo, float hi, float x) {
        float diff = hi - lo;
        if (diff == 0f)
            return 0f;

        return (x - lo) / diff;
    }

    private static float clamp01 (float x) {
        if (x < 0f)
            return 0f;
        if (x > 1f)
            return 1f;

        return x;
    }
}
