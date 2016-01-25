/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.cc007.cip;

import com.earth2me.essentials.Essentials;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Rik Schaaf aka CC007 (http://coolcat007.nl/)
 */
public class CIP extends JavaPlugin {

    private Logger log;
    private Plugin vault = null;
    private Permission permission = null;
    private FileConfiguration config = null;
    private File configFile = null;
    private Essentials ess;
    private CIPEventListener cipListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        /* Setup the logger */
        log = getLogger();

        /* Setup permissions */
        vault = getPlugin("Vault");
        if (vault != null) {
            setupPermissions();
        }

        getCommand("cip").setExecutor(new CIPCommand(this));
        ess = (Essentials) getPlugin("Essentials");
        cipListener = new CIPEventListener(this);

    }

    @Override
    public void onDisable() {
        super.onDisable(); //To change body of generated methods, choose Tools | Templates.
    }

    public Essentials getEssentials() {
        return ess;
    }

    /**
     * Gets a plugin
     *
     * @param pluginName Name of the plugin to get
     * @return The plugin from name
     */
    protected Plugin getPlugin(String pluginName) {
        if (getServer().getPluginManager().getPlugin(pluginName) != null && getServer().getPluginManager().getPlugin(pluginName).isEnabled()) {
            return getServer().getPluginManager().getPlugin(pluginName);
        } else {
            getLogger().log(Level.WARNING, "Could not find plugin \"{0}\"!", pluginName);
            return null;
        }
    }

    /**
     * Setup permissions
     *
     * @return True: Setup correctly, Didn't setup correctly
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        if (permission == null) {
            getLogger().log(Level.WARNING, "Could not hook Vault!");
        } else {
            getLogger().log(Level.WARNING, "Hooked Vault!");
        }

        return (permission != null);
    }

    /**
     * Get the vault
     *
     * @return the vault
     */
    public Plugin getVault() {
        return vault;
    }

    /**
     * Get the permissions
     *
     * @return the permissions
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Method to reload the config.yml config file
     */
    @Override
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        Reader defConfigStream = null;
        try {
            defConfigStream = new InputStreamReader(this.getResource("config.yml"), "UTF8");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }

    /**
     * Method to get YML content of the config.yml config file
     *
     * @return YML content of the catagories.yml config file
     */
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Method to save the config.yml config file
     */
    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }
}
