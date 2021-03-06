package com.gmail.andrewandy.customoregen.addon;

import com.gmail.andrewandy.corelib.util.Common;
import com.gmail.andrewandy.corelib.util.DeregisterableListener;
import com.gmail.andrewandy.customoregen.addon.generators.IslandOreGenerator;
import com.gmail.andrewandy.customoregen.addon.leveling.IslandLevelingManager;
import com.gmail.andrewandy.customoregen.addon.levels.IslandTemplateMapper;
import com.gmail.andrewandy.customoregen.addon.listener.IslandDataHandler;
import com.gmail.andrewandy.customoregen.addon.util.IslandTracker;
import com.gmail.andrewandy.customoregen.addon.util.IslandTrackingManager;
import com.gmail.andrewandy.customoregen.generator.Priority;
import com.gmail.andrewandy.customoregen.generator.builtins.GenerationChanceHelper;
import com.gmail.andrewandy.customoregen.generator.builtins.OverworldGenerator;
import com.gmail.andrewandy.customoregen.hooks.Hook;
import com.gmail.andrewandy.customoregen.util.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Hooks into BSkyblock and enables
 */
public final class CustomOreGenAddon extends Addon implements Hook {

    private static final Callable<?> RELOAD_TASK = () -> {
        CustomOreGenAddon addon = getInstance();
        if (addon == null) {
            return null;
        }
        addon.saveTrackerManager();
        addon.loadSettings();
        return null;
    };

    private static final List<String> SPECIAL_KEYS = Arrays.asList("COST");
    public static IslandOreGenerator defaultGenerator;
    private static CustomOreGenAddon instance;
    private static JavaPlugin bukkitPlugin;
    private static YamlConfiguration defaults;
    private IslandTrackingManager trackingManager = new IslandTrackingManager();
    private DeregisterableListener islandDataHandler = new IslandDataHandler();

    private Collection<String> addonNames = Arrays.asList("BSkyblock", "AcidIsland", "CaveBlock", "SkyGrid");

    public CustomOreGenAddon() {
    }

    public static void setInstance(CustomOreGenAddon instance) {
        CustomOreGenAddon.instance = instance;
    }

    @Override
    public String getTargetPluginName() {
        return "BentoBox";
    }

    public static YamlConfiguration getDefaults() {
        return defaults;
    }

    public static com.gmail.andrewandy.customoregen.CustomOreGen getBukkitPlugin() {
        return (com.gmail.andrewandy.customoregen.CustomOreGen) bukkitPlugin;
    }

    public static CustomOreGenAddon getInstance() {
        if (instance == null) {
            new CustomOreGenAddon();
        }
        return instance;
    }

    public static IslandOreGenerator getDefaultIslandGenerator(String islandID) {
        if (defaultGenerator == null) {
            return null;
        }
        return new IslandOreGenerator(islandID, defaultGenerator.getLevel(), defaultGenerator.getMaxLevel(), defaultGenerator.getPriority());
    }

    public IslandTrackingManager getTrackingManager() {
        return trackingManager;
    }

    public IslandTracker getIslandTracker(String islandID) {
        return trackingManager.getTracker(islandID);
    }

    private void setupListeners() {
        Bukkit.getPluginManager().registerEvents(islandDataHandler, getBukkitPlugin());
    }

    private void loadFiles() throws IOException {
        Common.log(Level.INFO, "&bLoading files.");
        File file = new File(getDataFolder().getAbsolutePath(), "defaults.yml");
        if (!file.isFile()) {
            Common.log(Level.INFO, "&aDefaults file not found, creating one now.");
            try (InputStream defaultsStream = CustomOreGenAddon.class.getClassLoader().getResourceAsStream("defaults.yml")) {
                if (defaultsStream == null) {
                    Common.log(Level.SEVERE, "&cUnable to load defaults from jar!");
                    throw new IllegalStateException();
                }
                if (!file.createNewFile()) {
                    Common.log(Level.SEVERE, "&cUnable to create new file!");
                    throw new IllegalStateException();
                }
                FileUtil.copy(defaultsStream, file);
            }
        }
        defaults = YamlConfiguration.loadConfiguration(file);
        Common.log(Level.INFO, "&aFile Loading complete!");
    }


    private void disableListeners() {
        islandDataHandler.disable();
    }

    private void registerConfigurationSerialisation() {
        ConfigurationSerialization.registerClass(IslandTracker.class);
        ConfigurationSerialization.registerClass(IslandTemplateMapper.class);
    }

    private void unregisterConfigurationSerialisation() {
        ConfigurationSerialization.unregisterClass(IslandTracker.class);
        ConfigurationSerialization.unregisterClass(IslandTemplateMapper.class);
    }

    private Optional<IslandTrackingManager> loadManager(File data) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(data);
        String json = configuration.getString("ISLAND_LEVEL_MANAGER");
        return json == null ? Optional.empty() : IslandTrackingManager.fromJson(json);
    }

    private void loadIslandLevellingManager() {
        File file = CustomOreGenAddon.getInstance().getDataFolder();
        File data = new File(file.getAbsolutePath(), "IslandLevelData.yml");
        try {
            if (!data.isFile()) {
                data.createNewFile();
            }
            IslandLevelingManager.getInstance().setTrackerManager(trackingManager);
        } catch (IOException ex) {
            Common.log(Level.SEVERE, ex.getMessage());
            Common.log(Level.SEVERE, "&cUnable to load Island Leveling Data!");
        }
    }

    private void saveTrackerManager() throws IOException {
        File file = CustomOreGenAddon.getInstance().getDataFolder();
        File data = new File(file.getAbsolutePath(), "IslandLevelData.yml");
        data.createNewFile();
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("ISLAND_LEVEL_MANAGER", trackingManager.toJson());
        configuration.save(data);
    }

    private void loadDefaultGenerator() {
        ConfigurationSection section = CustomOreGenAddon.getDefaults().getConfigurationSection("IslandSettings");
        assert section != null;
        section = section.getConfigurationSection("DEFAULT");
        if (section == null) {
            Common.log(Level.WARNING, "&eNo default generators found!");
            //TODO load all generators in defaults.
            return;
        }
        Priority priority;
        OverworldGenerator currentInstance = OverworldGenerator.getInstance();
        priority = currentInstance == null ? Priority.NORMAL : currentInstance.getPriority().getNext();
        int maxLevel = section.getInt("MaxLevel");
        int defaultLevel = section.getInt("DefaultLevel");
        IslandOreGenerator islandGenerator = new IslandOreGenerator("null", maxLevel, defaultLevel, priority);
        ConfigurationSection levelSection = section.getConfigurationSection("Levels");
        if (levelSection == null) {
            Common.log(Level.WARNING, "[&aHooks] &eEmpty level section found! Skipping this generator.");
            return;
        }
        for (int index = 1; index <= maxLevel; index++) {
            ConfigurationSection level = levelSection.getConfigurationSection(Integer.toString(index));
            if (level == null) {
                Common.log(Level.WARNING, "&a[Hooks] &cEmpty Level section found! Skipping...");
                continue;
            }
            GenerationChanceHelper spawnChances = islandGenerator.getSpawnChances(index);
            for (String key : level.getKeys(false)) {
                if (SPECIAL_KEYS.contains(key.toUpperCase())) {
                    continue;
                }
                int chance = level.getInt(key);
                Material material = Material.getMaterial(key);
                if (material == null || !material.isBlock() || chance < 1) {
                    Common.log(Level.WARNING, "&a[Hooks] &cInvalid Material found! Skipping...");
                    continue;
                }
                spawnChances.addBlockChance(material.createBlockData(), chance);
            }
        }
        defaultGenerator = islandGenerator;
    }

    private void loadSettings() {
        File file = new File(getDataFolder().getAbsolutePath(), "TrackingManager.yml");
        try {
            if (!file.isFile()) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            Common.log(Level.SEVERE, "&cUnable to load tracking data!");
            throw new IllegalStateException(ex);
        }
        this.trackingManager = loadManager(file).orElse(new IslandTrackingManager());
        loadIslandLevellingManager();
        try {
            loadFiles();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        loadDefaultGenerator();
        Common.log(Level.INFO, "&a[Hooks] &bSkyblock features enabled!");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Common.setPrefix("&3[CustomOreGen] &d[Addon] &b");
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CustomOreGen");
        try {
            Class<?> clazz = com.gmail.andrewandy.customoregen.CustomOreGen.class;
            if (!clazz.isInstance(plugin)) {
                Common.log(Level.SEVERE, "&cCustomOreGen main plugin not found!");
                throw new IllegalStateException();
            }
            bukkitPlugin = (JavaPlugin) plugin;
        } catch (NoClassDefFoundError ex) {
            Common.log(Level.SEVERE, "&cCustomOreGen main plugin not found!");
            throw new IllegalStateException();
        }
    }

    @Override
    public void onEnable() {
        Addon found = null;
        for (String addon : addonNames) {
            Optional<Addon> optionalAddon = BentoBox.getInstance().getAddonsManager().getAddonByName(addon);
            if (optionalAddon.isPresent()) {
                found = optionalAddon.get();
                break;
            }
        }
        if (found == null) {
            Common.log(Level.INFO, "&a[Hooks] &eNo Skyblock addon was not found.");
            return;
        }
        if (instance == null) {
            registerConfigurationSerialisation();
        }
        instance = this;
        loadSettings();
        setupListeners();
        Common.log(Level.INFO, "&a[Hooks] &bSkyblock features enabled!");
        com.gmail.andrewandy.customoregen.commands.ReloadCommand.getInstance().registerReloadTask(RELOAD_TASK);
    }


    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    @Override
    public void onDisable() {
        unregisterConfigurationSerialisation();
        disableListeners();
        try {
            saveTrackerManager();
        } catch (IOException ex) {
            Common.log(Level.SEVERE, "&cUnable to save tracking manager!");
            ex.printStackTrace();
        }
        Common.log(Level.INFO, "&aCustomOreGenAddon has been disabled.");
        com.gmail.andrewandy.customoregen.commands.ReloadCommand.getInstance().registerReloadTask(RELOAD_TASK);
    }

    @Override
    public boolean isEnabled() {
        return instance != null;
    }
}
