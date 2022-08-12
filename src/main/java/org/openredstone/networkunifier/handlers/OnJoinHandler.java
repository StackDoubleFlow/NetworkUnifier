package org.openredstone.networkunifier.handlers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.openredstone.networkunifier.managers.AccountManager;
import org.openredstone.networkunifier.managers.NicknameManager;

public class OnJoinHandler {

    private AccountManager accountManager;
    private NicknameManager nicknameManager;

    public OnJoinHandler(AccountManager accountManager, NicknameManager nicknameManager) {
        this.accountManager = accountManager;
        this.nicknameManager = nicknameManager;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        String userId = event.getPlayer().getUniqueId().toString();
        if (accountManager.userIsLinkedById(userId)) {
            if (!accountManager.getSavedIgnFromUserId(userId).equals(event.getPlayer().getUsername())) {
                accountManager.updateAccountIgn(userId, event.getPlayer().getUsername());
                nicknameManager.setNickname(userId, event.getPlayer().getUsername());
            }
        }
    }
}
