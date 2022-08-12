package org.openredstone.networkunifier.handlers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class JoinQuitEventHandler {

    Random rand = new Random();
    Channel gameChannel;
    Logger logger;
    CommentedConfigurationNode config;

    String greeting;
    String farewell;
    List<String> greetings;
    List<String> farewells;

    String ircChannel;

    boolean quirkyMessages;
    boolean specialFarewellGreetings;
    int quirkyMessageFrequency;
    String quirkyGreeting;
    String quirkyFarewell;

    public JoinQuitEventHandler(CommentedConfigurationNode config, Logger logger, Channel gameChannel) {
        super();
        this.config = config;
        this.gameChannel = gameChannel;
        this.logger = logger;

        greeting = config.getString("greeting_message");
        farewell = config.getString("farewell_message");

        if (specialFarewellGreetings = config.node("enable_special_farewells_and_greetings").getBoolean()) {
            try {
                farewells = config.node("message_farewells").getList(String.class);
                greetings = config.node("message_greetings").getList(String.class);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            logger.info("Loaded Farewells: " + farewells.toString());
            logger.info("Loaded Greetings: " + greetings.toString());
        }
        if (quirkyMessages = config.node("enable_special_quirky_message").getBoolean()) {
            quirkyMessageFrequency = config.node("quirky_message_frequency").getInt();
            quirkyGreeting = config.node("quirky_greeting_message").getString();
            quirkyFarewell = config.node("quirky_farewell_message").getString();
        }
        ircChannel = config.getString("irc_channel");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Thread postLoginThread = new Thread(() -> {
            sendJoins(event.getPlayer().getUsername());
        });
        postLoginThread.start();
    }

    @Subscribe
    public void onQuitEvent(DisconnectEvent event) {
        Thread postQuitThread = new Thread(() -> {
            sendQuits(event.getPlayer().getUsername());
        });
        postQuitThread.start();
    }

    private void sendJoins(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = getQuirkyGreeting(name);
        } else {
            message = getGreeting(name);
        }
        if (config.node("discord_enabled").getBoolean()) sendToDiscord(message);
    }

    private void sendQuits(String name) {
        String message;
        if (quirkyMessages && (rand.nextInt(quirkyMessageFrequency) == 1)) {
            message = getQuirkyFarewell(name);
        } else {
            message = getFarewell(name);
        }
        if (config.node("discord_enabled").getBoolean()) sendToDiscord(message);
    }

    private String getQuirkyGreeting(String name) {
        return quirkyGreeting.replace("%USER%", name);
    }

    private String getQuirkyFarewell(String name) {
        return quirkyFarewell.replace("%USER%", name);
    }

    private String getFarewell(String name) {
        if (specialFarewellGreetings) {
            return farewell.replace("%USER%", name).replace("%QUIRKY%", getRandomFarewell());
        } else {
            return farewell.replace("%USER%", name);
        }
    }

    private String getGreeting(String name) {
        if (specialFarewellGreetings) {
            return greeting.replace("%USER%", name).replace("%QUIRKY%", getRandomGreeting());
        } else {
            return greeting.replace("%USER%", name);
        }
    }

    private void sendToDiscord(String message) {
        ((ServerTextChannel) gameChannel).sendMessage("**" + message + "**");
    }

    public String getRandomFarewell() {
        return farewells.get(rand.nextInt(farewells.size() - 1 ));
    }

    public String getRandomGreeting() {
        return greetings.get(rand.nextInt(greetings.size() - 1 ));
    }
}
