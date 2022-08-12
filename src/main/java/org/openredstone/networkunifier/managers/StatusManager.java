package org.openredstone.networkunifier.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import org.openredstone.networkunifier.NetworkUnifier;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class StatusManager {

    private Channel channel;
    private NetworkUnifier plugin;
    private ProxyServer proxy;

    private final HashMap<String, Boolean> serversOnline = new HashMap<>();

    public StatusManager(CommentedConfigurationNode config, DiscordApi api, NetworkUnifier plugin, ProxyServer proxy) {
        this.channel = api.getServerTextChannelById(config.node("discord_status_channel_id").getString()).get();
        this.plugin = plugin;
        this.proxy = proxy;

        proxy.getScheduler().buildTask(plugin, this::checkServers).repeat(config.node("status_update_frequency").getInt(), TimeUnit.SECONDS).schedule();
        proxy.getScheduler().buildTask(plugin, () -> updateStatus(generateStatusMessage())).repeat(config.node("status_update_frequency").getInt(), TimeUnit.SECONDS).schedule();
    }

    private void checkServers() {

        for (RegisteredServer server : proxy.getAllServers()) {
            server.ping().handle((ServerPing result, Throwable error) -> {
                serversOnline.put(server.getServerInfo().getName(), error == null);
                return result;
            });
        }
    }

    private EmbedBuilder generateStatusMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setColor(Color.decode("#cd2d0a"))
                .setTitle("Status")
                .addField("**Players Online**", String.valueOf(proxy.getAllPlayers().size()))
                .setThumbnail("https://openredstone.org/wp-content/uploads/2018/07/icon-mini.png")
                .setTimestamp(Instant.now());

        for (RegisteredServer server : proxy.getAllServers()) {
            ServerInfo serverInfo = server.getServerInfo();
            if (serversOnline.containsKey(serverInfo.getName()) && serversOnline.get(serverInfo.getName())) {
                Collection<Player> players = server.getPlayersConnected();
                if (players.isEmpty()) {
                    embedBuilder.addField(serverInfo.getName() + " (**0**)", "☹");
                } else {
                    StringBuilder message = new StringBuilder();
                    message.append("`");
                    String prefix = "";
                    for (Player player : players) {
                        message.append(prefix);
                        prefix = ", ";
                        message.append(player.getUsername());
                    }
                    message.append("`");
                    embedBuilder.addField(serverInfo.getName() + " (**" + players.size() + "**)", message.toString());
                }
            } else {
                embedBuilder.addField(serverInfo.getName() + " is **offline**",  "☠");
            }
        }
        return embedBuilder;
    }

    private void updateStatus(EmbedBuilder embedBuilder) {
        try {
            MessageSet messages = ((ServerTextChannel) this.channel).getMessages(1).get();
            if (messages.isEmpty()) {
                ((ServerTextChannel) this.channel).sendMessage(embedBuilder).exceptionally(ExceptionLogger.get());
            } else {
                if (messages.getNewestMessage().isPresent()) {
                    messages.getNewestMessage().get().edit("");
                    messages.getNewestMessage().get().edit(embedBuilder);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
