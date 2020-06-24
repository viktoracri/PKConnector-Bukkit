package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        Main.getInstance().getUserList().add(new User(e.getPlayer()));


        Main.getInstance().getPkConnector().sendData(0x7, new String[]{
                e.getPlayer().getUniqueId().toString()
        });
    }
}
