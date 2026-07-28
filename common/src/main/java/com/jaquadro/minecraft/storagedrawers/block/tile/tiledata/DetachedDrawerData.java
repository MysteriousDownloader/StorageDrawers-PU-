package com.jaquadro.minecraft.storagedrawers.block.tile.tiledata;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.config.ModCommonConfig;
import com.jaquadro.minecraft.storagedrawers.inventory.ItemStackHelper;
import com.jaquadro.minecraft.storagedrawers.util.LegacyStackCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class DetachedDrawerData implements IDrawer
{
    private ItemStack protoStack;
    private int count;
    private int storageMult;
    private boolean heavy;

    public DetachedDrawerData () {
        protoStack = ItemStack.EMPTY;
        count = 0;
        storageMult = 1;
        heavy = false;
    }

    public DetachedDrawerData (IDrawer sourceDrawer) {
        this(sourceDrawer, 1);
    }

    public DetachedDrawerData (IDrawer sourceDrawer, int storageMult) {
        protoStack = sourceDrawer.getStoredItemPrototype();
        count = sourceDrawer.getStoredItemCount();
        this.storageMult = storageMult;
    }

    public DetachedDrawerData (ValueInput input) {
        deserializeNBT(input);
    }

    protected DetachedDrawerData (DetachedDrawerData data) {
        protoStack = data.protoStack;
        count = data.count;
        storageMult = data.storageMult;
        heavy = data.heavy;
    }

    public int getStorageMultiplier () {
        return storageMult;
    }

    public void setStorageMultiplier (int storageMult) {
        this.storageMult = storageMult;
    }

    public boolean isHeavy () {
        return heavy;
    }

    public void setIsHeavy (boolean state) {
        heavy = state;
    }

    @Override
    public @NotNull ItemStack getStoredItemPrototype () {
        return protoStack;
    }

    @Override
    public @NotNull IDrawer setStoredItem (@NotNull ItemStack itemPrototype) {
        return this;
    }

    protected IDrawer setStoredItemRaw (@NotNull ItemStack itemPrototype) {
        itemPrototype = ItemStackHelper.getItemPrototype(itemPrototype);
        protoStack = itemPrototype;
        protoStack.setCount(1);
        count = 0;

        return this;
    }

    @Override
    public int getStoredItemCount () {
        return count;
    }

    @Override
    public void setStoredItemCount (int amount) {

    }

    protected void setStoredItemCountRaw (int amount) {
        count = amount;
    }

    @Override
    public int getMaxCapacity (@NotNull ItemStack itemPrototype) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getRemainingCapacity () {
        return getMaxCapacity() - getStoredItemCount();
    }

    @Override
    public boolean canItemBeStored (@NotNull ItemStack itemPrototype, Predicate<ItemStack> matchPredicate) {
        return false;
    }

    @Override
    public boolean canItemBeExtracted (@NotNull ItemStack itemPrototype, Predicate<ItemStack> matchPredicate) {
        return false;
    }

    @Override
    public boolean isEmpty () {
        return protoStack.isEmpty();
    }

    @Override
    public IDrawer copy () {
        return new DetachedDrawerData(this);
    }

    public void serializeNBT (ValueOutput output) {
        if (storageMult > 1)
            output.putInt("StorageMult", storageMult);

        if (protoStack.isEmpty())
            return;

        output.store("Item", ItemStack.CODEC, protoStack);
        output.putInt("Count", count);

        if (heavy)
            output.putBoolean("Heavy", true);
    }

    public void deserializeNBT (ValueInput input) {
        if (input == null)
            return;

        storageMult = input.getIntOr("StorageMult", ModCommonConfig.INSTANCE.DRAWERS.baseStackStorage.get() * 8);

        setIsHeavy(input.getBooleanOr("Heavy", false));
        setStoredItemRaw(input.read("Item", LegacyStackCodec.CODEC).orElse(ItemStack.EMPTY));
        setStoredItemCountRaw(input.getIntOr("Count", 0));
    }
}
