package org.openredstone.networkunifier.commands.minecraft;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import com.velocitypowered.api.proxy.Player;
import org.openredstone.networkunifier.NetworkUnifier;
import org.openredstone.networkunifier.managers.AccountManager;

@CommandPermission("networkunifier.discord")
public class DiscordCommand extends BaseCommand {

    private AccountManager accountManager;

    public DiscordCommand(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Default
    public void execute(Player player) {
        if (accountManager.userIsLinkedById(player.getUniqueId().toString())) {
            NetworkUnifier.sendMessage(player, "You are already linked to Discord.");
            return;
        }

        String token = accountManager.createAccount(player.getUniqueId().toString(), player.getUsername());

        if (token != null) {
            NetworkUnifier.sendMessage(player, "Type \"!auth " + token + "\" anywhere on our Discord to finish validating your account.");
        } else {
            NetworkUnifier.sendMessage(player, "There was an issue processing this command.");
        }
    }
}
