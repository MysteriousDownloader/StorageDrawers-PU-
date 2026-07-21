package com.jaquadro.minecraft.storagedrawers.core;

import com.jaquadro.minecraft.storagedrawers.ModConstants;
import com.jaquadro.minecraft.storagedrawers.core.recipe.*;
import com.texelsaurus.minecraft.chameleon.ChameleonServices;
import com.texelsaurus.minecraft.chameleon.api.ChameleonInit;
import com.texelsaurus.minecraft.chameleon.registry.ChameleonRegistry;
import com.texelsaurus.minecraft.chameleon.registry.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class ModRecipes
{
    private static final ChameleonRegistry<RecipeSerializer<?>> RECIPES = ChameleonServices.REGISTRY.create(BuiltInRegistries.RECIPE_SERIALIZER, ModConstants.MOD_ID);

    public static final RegistryEntry<RecipeSerializer<AddUpgradeRecipe>> UPGRADE_RECIPE_SERIALIZER = RECIPES.register("add_upgrade", AddUpgradeRecipe::makeSerializer);
    public static final RegistryEntry<RecipeSerializer<ShapedRecipe>> KEYRING_RECIPE_SERIALIZER = RECIPES.register("keyring", KeyringRecipe::makeSerializer);
    public static final RegistryEntry<RecipeSerializer<ShapedRecipe>> REMOTE_GROUP_UPGRADE_SERIALIZER = RECIPES.register("remote_group_upgrade", RemoteGroupUpgradeRecipe::makeSerializer);
    public static final RegistryEntry<RecipeSerializer<UpgradeDetachedDrawerRecipe>> DETACHED_UPGRADE_RECIPE_SERIALIZER = RECIPES.register("add_detached_upgrade", UpgradeDetachedDrawerRecipe::makeSerializer);
    public static final RegistryEntry<RecipeSerializer<PersonalKeyRecipe>> PERSONAL_KEY_RECIPE_SERIALIZER = RECIPES.register("personal_key_cycle", PersonalKeyRecipe::makeSerializer);

    public static void init (ChameleonInit.InitContext context) {
        RECIPES.init(context);
    }
}
