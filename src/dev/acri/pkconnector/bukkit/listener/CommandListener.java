package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e){
        String command = e.getMessage().replace("/", "").split(" ")[0];
        if(Bukkit.getPluginCommand(command) != null) if(Bukkit.getPluginCommand(command).getPlugin().getName().equals("Essentials")){
            if(Bukkit.getPluginCommand(command).getName().equals("vanish")){
                com.earth2me.essentials.Essentials es = (com.earth2me.essentials.Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                if(!es.getUser(e.getPlayer().getUniqueId()).isVanished()){
                    Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                            e.getPlayer().getName(),
                            "HIDE"
                    });
                    System.out.println("Hidden");
                }else{
                    Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                            e.getPlayer().getName(),
                            "SHOW"
                    });
                    System.out.println("Shown");
                }

            }


        }

    }
}
