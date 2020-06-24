package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        User u = Main.getInstance().getUser(e.getPlayer());
        u.save();
        Main.getInstance().getUserList().remove(u);
    }
}
