package com.gmail.andrewandy.customoregen.generator;

import com.gmail.andrewandy.customoregen.generator.builtins.GenerationChanceHelper;
import com.gmail.andrewandy.customoregen.util.ItemWrapper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public abstract class ChanceGenerator extends AbstractGenerator {

    private GenerationChanceHelper spawnChances; //TODO remove this
    private List<GenerationChanceHelper> levelChance;

    protected ChanceGenerator(int maxLevel, int level) {
        this(maxLevel, level, Priority.NORMAL);
    }

    protected ChanceGenerator(int maxLevel, int level, Priority priority) {
        super(maxLevel, level, priority);
        setupSpawnChances();
        fillSpawnChances(true);
    }

    protected ChanceGenerator(ItemStack itemStack) {
        this(Objects.requireNonNull(itemStack).getItemMeta());
    }

    protected ChanceGenerator(ItemMeta meta) {
        super(meta);
        setupSpawnChances();
        ItemWrapper wrapper = ItemWrapper.wrap(meta);
        for (int index = 0; index < maxLevel(); index++) {
            String jsonMapped = wrapper.getString("SpawnChanceWrapper:" + index);
            setSpawnChance(index, jsonMapped);
        }
    }

    public ChanceGenerator(UUID fromID) throws IllegalArgumentException {
        super(fromID);
        setupSpawnChances();
        ConfigurationSection section = getDataSection().getConfigurationSection("Levels");
        if (section == null) {
            fillSpawnChances(false);
            return;
        }
        for (int index = 0; index < maxLevel(); index++) {
            String jsonMapped = section.getString(Integer.toString(index));
            setSpawnChance(index, jsonMapped);
        }
    }

    private void setupSpawnChances() {
        levelChance = maxLevel() > 0 ? new ArrayList<>(maxLevel()) : new LinkedList<>();
    }

    @Override
    public void save() {
        super.save();
        spawnChances = spawnChances == null ? new GenerationChanceHelper() : spawnChances;
        ConfigurationSection section = getDataSection().createSection("Levels");
        for (int index = 0; index < maxLevel(); index++) {
            ConfigurationSection levelSection = section.createSection(Integer.toString(index));
            GenerationChanceHelper chances = levelChance.get(index);
            levelSection.set("Data", chances == null ? null : chances.serialise());
        }
    }

    @Override
    public void writeToMeta(ItemMeta original) {
        super.writeToMeta(original);
        ItemWrapper wrapper = ItemWrapper.wrap(original);
        this.spawnChances = this.spawnChances == null ? new GenerationChanceHelper() : this.spawnChances;
        for (int index = 0; index < maxLevel(); index++) {
            GenerationChanceHelper chances = levelChance.get(index);
            wrapper.setString("SpawnChanceWrapper:" + index, chances.serialise());
        }
    }

    public GenerationChanceHelper getSpawnChances() {
        return getSpawnChances(getLevel());
    }

    public GenerationChanceHelper getSpawnChances(int level) {
        return levelChance.get(level - 1);
    }

    public List<GenerationChanceHelper> getAllSpawnChances() {
        return new ArrayList<>(levelChance);
    }

    private void fillSpawnChances(boolean overwrite) {
        for (int index = 0; index < maxLevel(); index++) {
            if (levelChance.size() == index) {
                levelChance.add(index, new GenerationChanceHelper());
                continue;
            }
            if (levelChance.get(index) != null) {
                if (!overwrite) {
                    continue;
                }
                levelChance.add(index, new GenerationChanceHelper());
            } else {
                levelChance.set(index, new GenerationChanceHelper());
            }
        }
    }

    private void setSpawnChance(int level, String serial) {
        levelChance.set(level, new GenerationChanceHelper(serial));
    }
}
