package com.jaquadro.minecraft.storagedrawers.client.model;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ParentModel implements BlockStateModel
{
    @NotNull
    protected final BlockStateModel parent;

    public ParentModel (@NotNull BlockStateModel parent) {
        this.parent = parent;
    }

    @Override
    public void collectParts (RandomSource randomSource, List<BlockModelPart> list) {
        parent.collectParts(randomSource, list);
    }

    @Override
    public TextureAtlasSprite particleIcon () {
        return parent.particleIcon();
    }
}