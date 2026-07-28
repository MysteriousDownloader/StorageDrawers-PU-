package com.jaquadro.minecraft.storagedrawers.util;

import com.jaquadro.minecraft.storagedrawers.ModServices;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tolerant {@link ItemStack} codecs for data this mod owns on disk.
 *
 * Vanilla's DataFixerUpper has no schema for modded NBT, so it never visits item stacks stored
 * inside our block entities or inside our data components. A world carried across the 1.21.5
 * component flattening — where {@code show_in_tooltip} moved out to {@code tooltip_display} and
 * several components lost their wrapper field — therefore hands us stacks in the old shape that
 * {@code ItemStack.CODEC} rejects outright. Losing that decode costs the whole drawer, not one
 * item: a drawer stores its item identity once and its quantity separately, so a rejected stack
 * takes every item in the slot with it.
 *
 * These codecs decode strictly first and only attempt a rewrite once that has already failed, so
 * valid data never takes this path and genuine corruption still surfaces as the original error.
 * A repaired stack is written back in the current shape on the next save, so the fallback fires
 * at most once per drawer.
 */
public final class LegacyStackCodec
{
    public static final Codec<ItemStack> CODEC = tolerant(ItemStack.CODEC);
    public static final Codec<ItemStack> OPTIONAL_CODEC = tolerant(ItemStack.OPTIONAL_CODEC);

    // Components that were unwrapped in 1.21.5, mapped to the field that used to wrap them.
    // Components that merely dropped `show_in_tooltip` (trim, unbreakable, jukebox_playable) are
    // absent deliberately — a record codec ignores unknown fields, so those still decode as-is.
    private static final Map<String, String> LEGACY_WRAPPERS = Map.of(
        "minecraft:enchantments", "levels",
        "minecraft:stored_enchantments", "levels",
        "minecraft:attribute_modifiers", "modifiers",
        "minecraft:can_place_on", "predicates",
        "minecraft:can_break", "predicates",
        "minecraft:dyed_color", "rgb"
    );

    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private LegacyStackCodec () { }

    private static Codec<ItemStack> tolerant (Codec<ItemStack> base) {
        return new Codec<ItemStack>() {
            @Override
            public <T> DataResult<Pair<ItemStack, T>> decode (DynamicOps<T> ops, T input) {
                DataResult<Pair<ItemStack, T>> strict = base.decode(ops, input);
                if (strict.result().isPresent())
                    return strict;

                T repaired = repair(ops, input);
                if (repaired.equals(input))
                    return strict;

                DataResult<Pair<ItemStack, T>> retry = base.decode(ops, repaired);
                if (retry.result().isEmpty())
                    return strict;

                report(ops, repaired);
                return retry;
            }

            @Override
            public <T> DataResult<T> encode (ItemStack value, DynamicOps<T> ops, T prefix) {
                return base.encode(value, ops, prefix);
            }

            @Override
            public String toString () {
                return "LegacyTolerant[" + base + "]";
            }
        };
    }

    private static <T> T repair (DynamicOps<T> ops, T input) {
        return new Dynamic<>(ops, input)
            .update("components", LegacyStackCodec::repairComponents)
            .getValue();
    }

    private static Dynamic<?> repairComponents (Dynamic<?> components) {
        Dynamic<?> result = components;
        for (Map.Entry<String, String> entry : LEGACY_WRAPPERS.entrySet())
            result = unwrap(result, entry.getKey(), entry.getValue());

        return result;
    }

    // Unwrapping is keyed on the wrapper actually being present, so a component already in the
    // current shape — or one whose legacy field we guessed wrong — is left untouched rather than
    // mangled.
    private static <T> Dynamic<T> unwrap (Dynamic<T> components, String component, String wrapper) {
        return components.update(component, value -> {
            Optional<? extends Dynamic<?>> inner = value.get(wrapper).result();
            return inner.isPresent() ? inner.get() : value;
        });
    }

    private static <T> void report (DynamicOps<T> ops, T repaired) {
        String id = new Dynamic<>(ops, repaired).get("id").asString().result().orElse("<unknown item>");
        if (!reported.add(id))
            return;

        ModServices.log.warn("Repaired pre-1.21.5 component data on {} while loading saved contents. "
            + "Vanilla's data fixer does not reach item stacks held by modded block entities or components, "
            + "so this stack was rewritten on read and will be saved in the current format.", id);
    }
}
