package com.gmail.andrewandy.customoregen.addon.levels;

import com.gmail.andrewandy.corelib.util.Common;
import com.gmail.andrewandy.customoregen.addon.CustomOreGenAddon;
import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.request.AddonRequestBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

public class IslandLevelsHook {

    private static IslandLevelsHook instance;


    private IslandLevelsHook() {
        Common.log(Level.INFO, "&a[Hooks] &bAttempting to hook into Island Level...");
        Optional<Addon> optional = BentoBox.getInstance().getAddonsManager().getAddonByName("Level");
        if (!optional.isPresent()) {
            Common.log(Level.INFO, "&a[Hooks] Level addon not found. Skipping...");
        }
        if (CustomOreGenAddon.getInstance() == null) {
            Common.log(Level.WARNING, "&a[Hooks] &cNo Skyblock addon was found! Disabling.");
            instance = null;
            return;
        }
        instance = this;
        IslandLevelOreGenerator.registerListener();
        try {
            loadIslandTemplates();
        } catch (IOException ex) {
            Common.log(Level.SEVERE, "&a[Hooks] &cUnable to setup island templates mapper! Disabling.");
        }
        Common.log(Level.INFO, "&a[Hooks] &e Sucessfully Hooked into Island Level.");
    }

    public static IslandLevelsHook getInstance() {
        if (instance == null) {
            new IslandLevelsHook();
        }
        return instance;
    }

    private void loadIslandTemplates() throws IOException {
        Common.log(Level.INFO, "&aLoading island templates mapper from disk.");
        long millis = System.currentTimeMillis();
        File file = CustomOreGenAddon.getBukkitPlugin().getDataFolder();
        file = new File(file.getAbsolutePath(), "IslandTemplateMapper.yml");
        if (!file.isFile()) {
            Common.log(Level.INFO, "&aTemplate Mapper data file not found, creating one now.");
            file.createNewFile();
        }
        IslandTemplateMapper.getInstance().load(file);
        Common.log(Level.INFO, "&aLoad complete! Took " + (System.currentTimeMillis() - millis) + "ms");
    }

    public boolean isEnabled() {
        return instance != null;
    }

    public void onEnable() {
        new IslandLevelsHook();
    }

    public void onDisable() {
        IslandLevelOreGenerator.unregisterListener();
        instance = null;
    }

    public long getIslandLevel(World world, String playerUUID) {
        return (Long) new AddonRequestBuilder()
                .addon("Level")
                .label("island-level")
                .addMetaData("world-name", world.getName())
                .addMetaData("player", playerUUID)
                .request();
    }
}
