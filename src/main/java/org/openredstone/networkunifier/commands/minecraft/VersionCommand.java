package org.openredstone.networkunifier.commands.minecraft;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.velocitypowered.api.proxy.Player;
import org.openredstone.networkunifier.NetworkUnifier;

@CommandPermission("networkunifier.discord")
public class VersionCommand extends BaseCommand {
    @Default
    public void execute(Player player) {
        NetworkUnifier.sendMessage(player, "Version " + NetworkUnifier.VERSION);
    }
}
