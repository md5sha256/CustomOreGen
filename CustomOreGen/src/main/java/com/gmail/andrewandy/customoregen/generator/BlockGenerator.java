package com.gmail.andrewandy.customoregen.generator;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface BlockGenerator {

    BlockData generateBlockAt(Location location);

    Priority getPriority();

    void setPriority(Priority priority);

    boolean isActiveAtLocation(Location location);

    boolean isGlobal();

    int getLevel();

    void setLevel(int newLevel);

    int maxLevel();

    default boolean isMaxed() {
        if (maxLevel() == -1) {
            return false;
        }
        return getLevel() < maxLevel();
    }

    default void incrementLevel() {
        if (!isMaxed()) {
            setLevel(getLevel() + 1);
        }
    }

    default void decrementLevel() {
        if (getLevel() > 0) {
            setLevel(getLevel() - 1);
        }
    }

    ItemStack toItemStack();

    void writeToMeta(ItemMeta target);
}
