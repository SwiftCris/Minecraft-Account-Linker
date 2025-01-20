package com.example.accountlinker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class AccountLinker extends JavaPlugin implements Listener {

    private Map<String, String> accountMappings = new HashMap<>();
    private File mappingFile;
    private File dataFolder;

    @Override
    public void onEnable() {
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Initialize the mapping file
        mappingFile = new File(getDataFolder(), "accounts.json");
        dataFolder = new File(getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!mappingFile.exists()) {
            try {
                getDataFolder().mkdirs();
                mappingFile.createNewFile();
                saveMappings();
            } catch (Exception e) {
                getLogger().warning("Failed to create accounts.json: " + e.getMessage());
            }
        }

        // Load existing mappings
        loadMappings();
        getLogger().info("AccountLinker has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save mappings on shutdown
        saveMappings();
        getLogger().info("AccountLinker has been disabled!");
    }

    private void loadMappings() {
        try (FileReader reader = new FileReader(mappingFile)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            accountMappings = new Gson().fromJson(reader, type);
            if (accountMappings == null) {
                accountMappings = new HashMap<>();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load account mappings: " + e.getMessage());
        }
    }

    private void saveMappings() {
        try (FileWriter writer = new FileWriter(mappingFile)) {
            new Gson().toJson(accountMappings, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save account mappings: " + e.getMessage());
        }
    }

    private void saveInventory(Player player) {
        try {
            File playerFile = new File(dataFolder, player.getName() + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            config.set("inventory", player.getInventory().getContents());
            config.save(playerFile);
        } catch (Exception e) {
            getLogger().warning("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void loadInventory(Player player) {
        try {
            File playerFile = new File(dataFolder, player.getName() + ".yml");
            if (!playerFile.exists()) {
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            ItemStack[] inventory = ((ItemStack[]) config.get("inventory"));
            player.getInventory().setContents(inventory);
        } catch (Exception e) {
            getLogger().warning("Failed to load inventory for " + player.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        if (accountMappings.containsKey(playerName)) {
            String linkedAccount = accountMappings.get(playerName);
            player.sendMessage("Your account is linked to: " + linkedAccount);
            loadInventory(player);
        } else {
            player.sendMessage("No linked account found. Use /link <JavaUsername> <BedrockUsername> to link accounts.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        saveInventory(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("link")) {
            if (args.length != 2) {
                sender.sendMessage("Usage: /link <JavaUsername> <BedrockUsername>");
                return true;
            }

            String javaName = args[0];
            String bedrockName = args[1];

            accountMappings.put(javaName, bedrockName);
            accountMappings.put(bedrockName, javaName);
            saveMappings();

            sender.sendMessage("Successfully linked Java account " + javaName + " to Bedrock account " + bedrockName);
            return true;
        }
        return false;
    }
}

