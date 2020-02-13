package com.gmail.andrewandy.customoregen.generator;

import com.gmail.andrewandy.customoregen.util.Region;
import com.gmail.andrewandy.customoregen.util.ItemWrapper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class IslandRegionGenerator extends AbstractGenerator {

    private final String islandID;

    public IslandRegionGenerator(UUID generatorID) {
        super(generatorID);
        ConfigurationSection section = getDataSection();
        islandID = section.getString("IslandID");
    }

    public IslandRegionGenerator(ItemStack itemStack) {
        this(Objects.requireNonNull(itemStack).getItemMeta());
    }

    public IslandRegionGenerator(ItemMeta meta) {
        super(meta);
        this.islandID = ItemWrapper.wrap(meta).getString("islandID");
    }

    public IslandRegionGenerator(Island island, int maxLevel, int level) {
        this(Objects.requireNonNull(island).getUniqueId(), maxLevel, level, Priority.NORMAL);
    }

    public IslandRegionGenerator(Island island, int maxLevel, int level, Priority priority) {
        this(Objects.requireNonNull(island).getUniqueId(), maxLevel, level, priority);
    }

    public IslandRegionGenerator(String islandID, int maxLevel, int level) {
        this(islandID, maxLevel, level, Priority.NORMAL);
    }

    public IslandRegionGenerator(String islandID, int maxLevel, int level, Priority priority) {
        super(maxLevel, level, priority);
        this.islandID = Objects.requireNonNull(islandID);
        if (!BentoBox.getInstance().getIslands().getIslandById(islandID).isPresent()) {
            throw new IllegalArgumentException("Invalid IslandID Specified!");
        }
    }

    public boolean withinRegion(Location location) {
        Optional<Island> optionalIsland = BentoBox.getInstance().getIslands().getIslandById(islandID);
        if (!optionalIsland.isPresent()) {
            throw new IllegalStateException("IslandID is invalid, no island was found!");
        }
        Island island = optionalIsland.get();
        return island.inIslandSpace(location);
    }

    public boolean allWithinRegion(Region region) {
        Optional<Island> optionalIsland = BentoBox.getInstance().getIslands().getIslandById(islandID);
        if (!optionalIsland.isPresent()) {
            throw new IllegalStateException("IslandID is invalid, no island was found!");
        }
        Island island = optionalIsland.get();
        return new Region(island.getBoundingBox(), island.getWorld()).contains(region);
    }

    @Override
    public void save() {
        super.save();
        getDataSection().set("IslandID", islandID);
    }
}
