package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        User u = new User(e.getPlayer());
        Main.getInstance().getUserList().add(u);

        if(Main.getInstance().getConfig().getBoolean("new-users-disable-global-chat"))
            u.setGlobalChatEnabled(false);


        Main.getInstance().getPkConnector().sendData(0x12, new String[]{
                e.getPlayer().getUniqueId().toString(),
                e.getPlayer().getName()
        });
    }
}
