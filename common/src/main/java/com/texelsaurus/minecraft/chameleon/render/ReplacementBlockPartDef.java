package com.texelsaurus.minecraft.chameleon.render;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.Material;

public class ReplacementBlockPartDef
{
    private BlockStateModelPart part;
    private Material.Baked material;

    public ReplacementBlockPartDef (BlockStateModelPart part, Material.Baked material) {
        this.part = part;
        this.material = material;
    }

    public BlockStateModelPart getPart () {
        return part;
    }

    public Material.Baked getMaterial () {
        return material;
    }
}
