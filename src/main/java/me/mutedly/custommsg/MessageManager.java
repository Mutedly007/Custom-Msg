package me.mutedly.custommsg;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class MessageManager {
    private final JavaPlugin plugin;
    private final File messagesFile;
    private FileConfiguration messagesConfig;
    private FileConfiguration config;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.config = plugin.getConfig();
        loadMessages();
        startAutoSave();
    }

    private void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        config = plugin.getConfig();
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml!");
            e.printStackTrace();
        }
    }

    private void startAutoSave() {
        if (config.getBoolean("storage.auto-save", true)) {
            int saveInterval = config.getInt("storage.save-interval", 300) * 20; // Convert seconds to ticks
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveMessages, saveInterval, saveInterval);
        }
    }

    public String getMessage(UUID playerId, String type, String defaultMessage) {
        String path = "messages." + type + "." + playerId.toString();
        return messagesConfig.getString(path, defaultMessage);
    }

    public void setMessage(UUID playerId, String type, String message) {
        String path = "messages." + type + "." + playerId.toString();
        messagesConfig.set(path, message);
        if (!config.getBoolean("storage.auto-save", true)) {
            saveMessages();
        }
    }

    public void resetMessage(UUID playerId, String type) {
        String path = "messages." + type + "." + playerId.toString();
        messagesConfig.set(path, null);
        if (!config.getBoolean("storage.auto-save", true)) {
            saveMessages();
        }
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
