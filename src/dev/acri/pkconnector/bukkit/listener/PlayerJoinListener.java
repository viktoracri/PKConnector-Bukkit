package dev.acri.pkconnector.bukkit.listener;


import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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


        if(Bukkit.getPluginManager().getPlugin("Essentials") != null){
            com.earth2me.essentials.Essentials es = (com.earth2me.essentials.Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
            if(es.getUser(e.getPlayer().getUniqueId()).isVanished()){

                Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                        e.getPlayer().getName(),
                        "HIDE"
                });

            }

        }


    }
}
