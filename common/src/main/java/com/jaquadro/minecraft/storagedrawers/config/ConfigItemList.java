package com.jaquadro.minecraft.storagedrawers.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigItemList
{
    private final List<String> listedNamespaces = new ArrayList<>();
    private final List<Item> listedItems = new ArrayList<>();
    // Every rule ever registered, kept for the lifetime of the game. The listed items are
    // rebuilt from scratch each time initialize() runs, so the rules have to outlive them.
    private final List<String> knownRules = new ArrayList<>();
    // Non-null only while initialize() is running. innerInitialize() applies the config rules
    // and the replay loop would then apply the same rules a second time; this makes each rule
    // take effect once per rebuild instead of logging and resolving twice.
    private Set<String> appliedThisPass;
    private boolean initialized;

    public ConfigItemList () { }

    /**
     * Builds the listed items from config, and rebuilds them on every later call.
     *
     * This cannot run at mod init on 26.2. Resolving a rule to an item ends in
     * {@code new ItemStack(item)}, and the ItemStack constructor reads the item's data
     * components eagerly — components are bound during datapack load, so any earlier call
     * throws {@code NullPointerException: Components not bound yet}. The caller drives this
     * from a datapack-load hook, which also fires again on every reload; hence the rebuild
     * rather than a one-shot guard.
     */
    public void initialize () {
        listedNamespaces.clear();
        listedItems.clear();
        initialized = true;
        appliedThisPass = new HashSet<>();

        try {
            innerInitialize();

            for (String rule : List.copyOf(knownRules)) {
                register(rule);
            }
        }
        finally {
            appliedThisPass = null;
        }
    }

    protected void innerInitialize () { }

    public boolean isListed (ItemStack stack) {
        Item item = stack.getItem();

        if (listedItems.contains(item))
            return true;

        if(!listedNamespaces.isEmpty()) {
            ResourceKey<Item> resourceKey = BuiltInRegistries.ITEM.getResourceKey(item).orElse(null);
            if (resourceKey != null) {
                String namespace = resourceKey.identifier().getNamespace();
                if (listedNamespaces.contains(namespace))
                    return true;
            }
        }

        return false;
    }

    public boolean registerNamespace (@NotNull String namespace) {
        if (namespace.isEmpty())
            return false;

        unregisterNamespace(namespace);
        listedNamespaces.add(namespace);

        logRegisterNamespace(namespace);

        return true;
    }

    protected void logRegisterNamespace (@NotNull String namespace) { }

    public boolean registerItem (@NotNull ItemStack item) {
        if (item.isEmpty())
            return false;

        unregisterItem(item);
        listedItems.add(item.getItem());

        logRegisterItem(item);

        return true;
    }

    protected void logRegisterItem (@NotNull ItemStack item) { }

    public void register (List<String> entries) {
        entries.forEach(this::register);
    }

    public boolean register (String entry) {
        if (!knownRules.contains(entry))
            knownRules.add(entry);

        if (!initialized)
            return true;

        if (appliedThisPass != null && !appliedThisPass.add(entry))
            return true;

        String[] parts = entry.split("\\s*:\\s*");
        if (parts.length == 1)
            return registerNamespace(parts[0]);

        Identifier resource = Identifier.parse(entry);
        Item item = BuiltInRegistries.ITEM.getValue(resource);

        return registerItem(new ItemStack(item));
    }

    public boolean unregisterNamespace (@NotNull String namespace) {
        if (namespace.isEmpty())
            return false;

        return listedNamespaces.remove(namespace);
    }

    public boolean unregisterItem (@NotNull ItemStack stack) {
        if (stack.isEmpty())
            return false;

        return listedItems.remove(stack.getItem());
    }

}
