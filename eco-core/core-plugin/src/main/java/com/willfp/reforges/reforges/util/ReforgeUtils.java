package com.willfp.reforges.reforges.util;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.reforges.ReforgesPlugin;
import com.willfp.reforges.reforges.Reforge;
import com.willfp.reforges.reforges.Reforges;
import com.willfp.reforges.reforges.meta.ReforgeTarget;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class ReforgeUtils {
    /**
     * Instance of StatTrackers.
     */
    private static final EcoPlugin PLUGIN = ReforgesPlugin.getInstance();

    /**
     * The key for storing reforges.
     */
    private static final NamespacedKey REFORGE_KEY = PLUGIN.getNamespacedKeyFactory().create("reforge");

    /**
     * The key for storing reforge amounts.
     */
    private static final NamespacedKey REFORGE_AMOUNT = PLUGIN.getNamespacedKeyFactory().create("reforge_amount");

    /**
     * The key for storing reforge stones.
     */
    private static final NamespacedKey REFORGE_STONE_KEY = PLUGIN.getNamespacedKeyFactory().create("reforge_stone");

    /**
     * Get a random reforge for a target.
     *
     * @param targets    The targets.
     */
    @Nullable
    public static Reforge getRandomReforge(@NotNull final Collection<ReforgeTarget> targets) {
        return getRandomReforge(targets, Collections.emptyList());
    }

    /**
     * Get a random reforge for a target.
     *
     * @param targets    The targets.
     * @param disallowed The disallowed reforges.
     */
    @Nullable
    public static Reforge getRandomReforge(@NotNull final Collection<ReforgeTarget> targets,
                                           @NotNull final Collection<Reforge> disallowed) {
        List<Reforge> applicable = new ArrayList<>();

        for (Reforge reforge : Reforges.values()) {
            for (ReforgeTarget target : targets) {
                if (reforge.getTargets().contains(target) && !reforge.getRequiresStone()) {
                    applicable.add(reforge);
                }
            }
        }

        Collections.shuffle(applicable);

        applicable.removeAll(disallowed);

        if (applicable.isEmpty()) {
            return null;
        }

        return applicable.get(0);
    }

    public static MetadatedReforgeStatus getStatus(@NotNull final List<ItemStack> captive) {
        ItemStack toReforge = captive.isEmpty() ? null : captive.get(0);
        ItemStack stone = captive.size() == 2 ? captive.get(1) : null;
        ReforgeStatus status = null;

        List<ReforgeTarget> target = new ArrayList<>();

        if (toReforge == null || toReforge.getType() == Material.AIR) {
            status = ReforgeStatus.NO_ITEM;
        } else {
            target.addAll(ReforgeTarget.getForItem(toReforge));
            if (target.isEmpty()) {
                status = ReforgeStatus.INVALID_ITEM;
            }
        }

        if (!target.isEmpty()) {
            status = ReforgeStatus.ALLOW;
        }

        double cost = 0;
        if (status == ReforgeStatus.ALLOW) {
            Reforge reforgeStone = getReforgeStone(stone);
            if (reforgeStone != null && reforgeStone.getTargets().stream()
                    .anyMatch(reforgeTarget -> reforgeTarget.getItems().stream()
                            .anyMatch(item -> item.matches(toReforge)))) {
                cost = reforgeStone.getStonePrice();
                status = ReforgeStatus.ALLOW_STONE;
            }
        }

        return new MetadatedReforgeStatus(status, cost);
    }

    /**
     * Get reforge on an item.
     *
     * @param item The item to query.
     * @return The found reforge, or null.
     */
    public static Reforge getReforge(@Nullable final ItemStack item) {
        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return getReforge(meta);
    }

    /**
     * Get reforge on an item.
     *
     * @param meta The item to query.
     * @return The found reforge, or null.
     */
    public static Reforge getReforge(@Nullable final ItemMeta meta) {
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(REFORGE_KEY, PersistentDataType.STRING)) {
            return null;
        }

        String active = container.get(REFORGE_KEY, PersistentDataType.STRING);

        return Reforges.getByKey(active);
    }

    /**
     * Set reforge on an item.
     *
     * @param item    The item.
     * @param reforge The reforge.
     */
    public static void setReforge(@NotNull final ItemStack item,
                                  @NotNull final Reforge reforge) {

        if (item.getItemMeta() == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        setReforge(meta, reforge);

        item.setItemMeta(meta);
    }

    /**
     * Set reforge on an item.
     *
     * @param meta    The meta.
     * @param reforge The reforge.
     */
    public static void setReforge(@NotNull final ItemMeta meta,
                                  @NotNull final Reforge reforge) {
        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(REFORGE_KEY, PersistentDataType.STRING, reforge.getId());
    }

    /**
     * Get reforge stone on an item.
     *
     * @param item The item to query.
     * @return The found reforge, or null.
     */
    public static Reforge getReforgeStone(@Nullable final ItemStack item) {
        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return getReforgeStone(meta);
    }

    /**
     * Get reforge stone on an item.
     *
     * @param meta The item to query.
     * @return The found reforge, or null.
     */
    public static Reforge getReforgeStone(@Nullable final ItemMeta meta) {
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(REFORGE_STONE_KEY, PersistentDataType.STRING)) {
            return null;
        }

        String active = container.get(REFORGE_STONE_KEY, PersistentDataType.STRING);

        return Reforges.getByKey(active);
    }

    /**
     * Set an item to be a reforge stone.
     *
     * @param item    The item.
     * @param reforge The reforge.
     */
    public static void setReforgeStone(@NotNull final ItemStack item,
                                       @NotNull final Reforge reforge) {
        if (item.getItemMeta() == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer container = meta.getPersistentDataContainer();

        container.set(REFORGE_STONE_KEY, PersistentDataType.STRING, reforge.getId());

        item.setItemMeta(meta);
    }

    /**
     * Get the amount of reforges done to an item.
     *
     * @param item The item.
     */
    public static int getReforges(@NotNull final ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return 0;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (!container.has(REFORGE_AMOUNT, PersistentDataType.INTEGER)) {
            container.set(REFORGE_AMOUNT, PersistentDataType.INTEGER, 0);
            item.setItemMeta(meta);
        }

        Integer amount = container.get(REFORGE_AMOUNT, PersistentDataType.INTEGER);

        return amount == null ? 0 : amount;
    }

    /**
     * Get the amount of reforges done to an item.
     *
     * @param item The item.
     */
    public static void incrementReforges(@NotNull final ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        int amount = getReforges(item);
        amount++;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(REFORGE_AMOUNT, PersistentDataType.INTEGER, amount);
        item.setItemMeta(meta);
    }
}
