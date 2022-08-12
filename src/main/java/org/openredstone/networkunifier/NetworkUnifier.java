package org.openredstone.networkunifier;

import co.aikar.commands.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.openredstone.networkunifier.commands.minecraft.DiscordCommand;
import org.openredstone.networkunifier.handlers.JoinQuitEventHandler;
import org.openredstone.networkunifier.handlers.OnJoinHandler;
import org.openredstone.networkunifier.listeners.UserUpdateListener;
import org.openredstone.networkunifier.managers.*;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


@Plugin(
        id = "networkunifier",
        name = "NetworkUnifier",
        version = NetworkUnifier.VERSION,
        url = "https://openredstone.org",
        authors = {"Nickster258", "PaukkuPalikka", "StackDoubleFlow"},
        dependencies = {
                @Dependency(id = "luckperms")
        }
)
public class NetworkUnifier {

    public static final String VERSION = "3.0";

    CommentedConfigurationNode config;
    Logger logger;
    File dataFolder;
    ProxyServer proxy;

    DiscordApi discordNetworkBot;
    Channel gameChannel;

    LuckPerms luckPerms;

    AccountManager accountManager;
    DiscordCommandManager discordCommandManager;
    NicknameManager nicknameManager;
    QueryManager queryManager;
    RoleManager roleManager;
    StatusManager statusManager;

    VelocityCommandManager commandManager;

    @Inject
    public NetworkUnifier(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = server;
        this.logger = logger;
        this.dataFolder = dataDirectory.toFile();
    }

    public static void sendMessage(Player sender, String message) {
        Component text = Component.text()
                .content("[").color(NamedTextColor.DARK_GRAY)
                .append(Component.text().content("NetworkUnifier").color(NamedTextColor.GRAY))
                .append(Component.text().content("] ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text().content(message).color(NamedTextColor.GRAY))
                .build();
        sender.sendMessage(text);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        commandManager = new VelocityCommandManager(proxy, this);
        load();
    }

    public void load() {
        loadConfig();

        luckPerms = LuckPermsProvider.get();
        try {
            queryManager = new QueryManager(
                    config.node("database_host").getString(),
                    config.node("database_port").getInt(),
                    config.node("database_name").getString(),
                    config.node("database_user").getString(),
                    config.node("database_pass").getString()
            );
            accountManager = new AccountManager(queryManager, config.node("discord_token_size").getInt(), config.node("discord_token_lifespan").getInt());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        if (config.node("discord_enabled").getBoolean()) {
            discordNetworkBot = new DiscordApiBuilder().setToken(config.getString("discord_network_bot_token")).login().join();
            discordNetworkBot.updateActivity(config.getString("discord_network_bot_playing_message"));
            gameChannel = discordNetworkBot.getServerTextChannelById(config.getString("discord_channel_id")).get();
            statusManager = new StatusManager(config, discordNetworkBot, this, proxy);
            nicknameManager = new NicknameManager(discordNetworkBot, accountManager, config.getString("discord_server_id"));
            try {
                roleManager = new RoleManager(accountManager, discordNetworkBot, luckPerms, config.getString("discord_server_id"), config.node("discord_tracked_tracks").getList(String.class));
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            if (!roleManager.groupsExistInTrackOnDiscordAlsoThisMethodIsReallyLongButIAmKeepingItToAnnoyPeople()) {
                logger.log(Level.SEVERE, "Cannot validate that the roles from the specified tracks exist on Discord or LuckPerms!");
                return;
            } else {
                logger.log(Level.INFO, "Validated that all listened tracks have related groups on discord.");
            }
            discordNetworkBot.addServerMemberJoinListener(event -> {
                String id = event.getUser().getIdAsString();
                if (!accountManager.userIsLinkedByDiscordId(id)) {
                    return;
                }
                String uuid = accountManager.getUserIdByDiscordId(id);
                nicknameManager.setNickname(uuid, accountManager.getSavedIgnFromDiscordId(id));
                User user = luckPerms.getUserManager().getUser(uuid);
                if (user == null) {
                    return;
                }
                String primaryGroup = user.getPrimaryGroup();
                if (!roleManager.isGroupTracked(primaryGroup)) {
                    return;
                }
                Group luckPrimaryGroup = luckPerms.getGroupManager().getGroup(primaryGroup);
                if (luckPrimaryGroup == null) {
                    return;
                }
                roleManager.setTrackedDiscordGroup(id, luckPrimaryGroup.getDisplayName());
            });
            discordCommandManager = new DiscordCommandManager(discordNetworkBot, accountManager, roleManager, luckPerms, config.getString("discord_command_character").charAt(0));
            new UserUpdateListener(roleManager, accountManager, luckPerms);
            proxy.getEventManager().register(this, new OnJoinHandler(accountManager, nicknameManager));
            commandManager.registerCommand(new DiscordCommand(accountManager));

        }

        proxy.getEventManager().register(this, new JoinQuitEventHandler(config, logger, gameChannel));

    }

    private void loadConfig() {
        try {
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File file = new File(dataFolder, "config.yml");
            if (!file.exists()) {
                file.createNewFile();
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(file).build();
            config = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}