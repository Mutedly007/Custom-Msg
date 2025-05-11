package me.mutedly.custommsg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class Custommsg extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private MessageManager messageManager;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder().character('&').build();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        messageManager = new MessageManager(this);
        
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("customjoin").setExecutor(this);
        getCommand("customleave").setExecutor(this);
        getCommand("custommessage").setExecutor(this);
        
        getLogger().info("CustomMsg has been enabled!");
    }

    @Override
    public void onDisable() {
        messageManager.saveMessages();
        saveConfig();
        getLogger().info("CustomMsg has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getConfigMessage("admin-messages.no-permission"));
            return true;
        }

        Player player = (Player) sender;

        switch (command.getName().toLowerCase()) {
            case "customjoin":
            case "customleave":
                if (!player.hasPermission("custommsg.use")) {
                    player.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
                
                if (args.length == 0) {
                    player.sendMessage(Component.text("Usage: /" + command.getName() + " <message>", NamedTextColor.RED));
                    return true;
                }

                String message = String.join(" ", args);
                if (validateMessage(message)) {
                    String type = command.getName().equals("customjoin") ? "join" : "leave";
                    messageManager.setMessage(player.getUniqueId(), type, message);
                    player.sendMessage(Component.text("Your " + type + " message has been set!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Invalid message! Message is too long or contains inappropriate content.", NamedTextColor.RED));
                }
                return true;

            case "custommessage":
                if (args.length == 0) {
                    showHelp(player);
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (!player.hasPermission("custommsg.admin.reload")) {
                            player.sendMessage(Component.text("You don't have permission to reload the configuration!", NamedTextColor.RED));
                            return true;
                        }
                        reloadConfig();
                        config = getConfig();
                        messageManager.reload();
                        player.sendMessage(getConfigMessage("admin-messages.config-reloaded"));
                        return true;

                    case "view":
                        if (!player.hasPermission("custommsg.admin.view")) {
                            player.sendMessage(Component.text("You don't have permission to view other players' messages!", NamedTextColor.RED));
                            return true;
                        }
                        if (args.length < 2) {
                            player.sendMessage(Component.text("Usage: /custommessage view <player>", NamedTextColor.RED));
                            return true;
                        }
                        handleViewMessages(player, args[1]);
                        return true;

                    case "set":
                        if (!player.hasPermission("custommsg.admin.set")) {
                            player.sendMessage(Component.text("You don't have permission to set other players' messages!", NamedTextColor.RED));
                            return true;
                        }
                        if (args.length < 4) {
                            player.sendMessage(Component.text("Usage: /custommessage set <player> <join/leave> <message>", NamedTextColor.RED));
                            return true;
                        }
                        handleAdminSet(player, args);
                        return true;

                    case "reset":
                        if (!player.hasPermission("custommsg.admin.reset")) {
                            player.sendMessage(Component.text("You don't have permission to reset other players' messages!", NamedTextColor.RED));
                            return true;
                        }
                        if (args.length < 3) {
                            player.sendMessage(Component.text("Usage: /custommessage reset <player> <join/leave>", NamedTextColor.RED));
                            return true;
                        }
                        handleAdminReset(player, args);
                        return true;

                    case "help":
                    default:
                        showHelp(player);
                        return true;
                }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String defaultMessage = config.getString("default-messages.join");
        String message = messageManager.getMessage(player.getUniqueId(), "join", defaultMessage);
        event.joinMessage(legacySerializer.deserialize(message.replace("%player%", player.getName())));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String defaultMessage = config.getString("default-messages.leave");
        String message = messageManager.getMessage(player.getUniqueId(), "leave", defaultMessage);
        event.quitMessage(legacySerializer.deserialize(message.replace("%player%", player.getName())));
    }

    private void handleAdminSet(Player admin, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        String type = args[2].toLowerCase();
        if (!type.equals("join") && !type.equals("leave")) {
            admin.sendMessage(Component.text("Type must be either 'join' or 'leave'", NamedTextColor.RED));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        messageManager.setMessage(target.getUniqueId(), type, message);
        
        String successMessage = getConfigMessage("admin-messages.message-set");
        if (successMessage.startsWith("&cMessage not found")) {
            successMessage = "&aSuccessfully set &e" + target.getName() + "&a's &e" + type + "&a message!";
        }
        admin.sendMessage(legacySerializer.deserialize(successMessage
            .replace("%player%", target.getName())
            .replace("%type%", type)));
    }

    private void handleAdminReset(Player admin, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return;
        }

        String type = args[2].toLowerCase();
        if (!type.equals("join") && !type.equals("leave")) {
            admin.sendMessage(Component.text("Type must be either 'join' or 'leave'", NamedTextColor.RED));
            return;
        }

        messageManager.resetMessage(target.getUniqueId(), type);
        
        String resetMessage = getConfigMessage("admin-messages.message-reset");
        if (resetMessage.startsWith("&cMessage not found")) {
            resetMessage = "&aSuccessfully reset &e" + target.getName() + "&a's &e" + type + "&a message!";
        }
        admin.sendMessage(legacySerializer.deserialize(resetMessage
            .replace("%player%", target.getName())
            .replace("%type%", type)));
    }

    private void handleViewMessages(Player admin, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            admin.sendMessage(legacySerializer.deserialize(getConfigMessage("admin-messages.player-not-found")));
            return;
        }

        String defaultJoinMessage = getConfigMessage("default-messages.join");
        String defaultLeaveMessage = getConfigMessage("default-messages.leave");
        String joinMessage = messageManager.getMessage(target.getUniqueId(), "join", defaultJoinMessage);
        String leaveMessage = messageManager.getMessage(target.getUniqueId(), "leave", defaultLeaveMessage);

        admin.sendMessage(Component.empty());
        admin.sendMessage(Component.text("Messages for player: ", NamedTextColor.GOLD)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW)));
        admin.sendMessage(Component.text("Join message: ", NamedTextColor.AQUA)
                .append(legacySerializer.deserialize(joinMessage.replace("%player%", target.getName()))));
        admin.sendMessage(Component.text("Leave message: ", NamedTextColor.AQUA)
                .append(legacySerializer.deserialize(leaveMessage.replace("%player%", target.getName()))));
        admin.sendMessage(Component.empty());
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("=== CustomMsg Help ===", NamedTextColor.GOLD));
        player.sendMessage(Component.empty());
        
        // Basic Commands
        player.sendMessage(Component.text("Basic Commands:", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/customjoin <message> ", NamedTextColor.AQUA)
                .append(Component.text("- Set your join message", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  • Use & for color codes", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • Use %player% for your name", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/customleave <message> ", NamedTextColor.AQUA)
                .append(Component.text("- Set your leave message", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  • Use & for color codes", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • Use %player% for your name", NamedTextColor.GRAY));
        
        // Admin Commands
        if (player.hasPermission("custommsg.admin")) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Admin Commands:", NamedTextColor.YELLOW));
            
            if (player.hasPermission("custommsg.admin.set")) {
                player.sendMessage(Component.text("/custommessage set <player> <join/leave> <message> ", NamedTextColor.AQUA)
                        .append(Component.text("- Set a player's message", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("  • Example: /custommessage set Player123 join &aWelcome %player%!", NamedTextColor.GRAY));
            }
            
            if (player.hasPermission("custommsg.admin.reset")) {
                player.sendMessage(Component.text("/custommessage reset <player> <join/leave> ", NamedTextColor.AQUA)
                        .append(Component.text("- Reset a player's message to default", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("  • Example: /custommessage reset Player123 join", NamedTextColor.GRAY));
            }
            
            if (player.hasPermission("custommsg.admin.view")) {
                player.sendMessage(Component.text("/custommessage view <player> ", NamedTextColor.AQUA)
                        .append(Component.text("- View a player's current messages", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("  • Shows both join and leave messages", NamedTextColor.GRAY));
            }
            
            if (player.hasPermission("custommsg.admin.reload")) {
                player.sendMessage(Component.text("/custommessage reload ", NamedTextColor.AQUA)
                        .append(Component.text("- Reload the plugin configuration", NamedTextColor.GRAY)));
                player.sendMessage(Component.text("  • Reloads config.yml and messages.yml", NamedTextColor.GRAY));
            }
        }
        
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Note: Messages support color codes (&) and the %player% placeholder", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
    }

    private String getConfigMessage(String path) {
        return config.getString(path, "&cMessage not found: " + path);
    }

    private boolean validateMessage(String message) {
        if (message.length() > config.getInt("settings.max-length", 100)) {
            return false;
        }

        String lowercaseMsg = message.toLowerCase();
        for (String blockedWord : config.getStringList("blocked.words")) {
            if (lowercaseMsg.contains(blockedWord.toLowerCase())) {
                return false;
            }
        }

        return !message.contains("§") && 
               message.matches("^[a-zA-Z0-9\\s&_\\-!@#$%^*()+=\\[\\]{}|:;\"'<>,.?/~`]*$");
    }
}
