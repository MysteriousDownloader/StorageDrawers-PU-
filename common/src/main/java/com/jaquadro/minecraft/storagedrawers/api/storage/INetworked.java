package com.jaquadro.minecraft.storagedrawers.api.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

public interface INetworked
{
    default boolean supportsDirectControllerLink () {
        return false;
    }

    default IControlGroup getBoundControlGroup () {
        return null;
    }

    default Set<IControlGroup> getSoftBoundControlGroups () { return Set.of(); }

    default void softBindControlGroup (IControlGroup group) { }

    default boolean canRecurseSearch () {
        return true;
    }

    default void unbindControlGroup () { }

    default void scheduleValidation () { }

    default boolean isBoundToActiveController () {
        IControlGroup bound = getBoundControlGroup();
        if (bound != null) {
            IDrawerGroup dg = bound.getDrawerGroup();
            if (dg != null && dg.isGroupValid())
                return true;
        }
        for (IControlGroup group : getSoftBoundControlGroups()) {
            if (group != null) {
                IDrawerGroup dg = group.getDrawerGroup();
                if (dg != null && dg.isGroupValid())
                    return true;
            }
        }
        return false;
    }
}