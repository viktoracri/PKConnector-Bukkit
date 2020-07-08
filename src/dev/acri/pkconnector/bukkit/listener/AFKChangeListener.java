package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AFKChangeListener implements Listener {

    @EventHandler
    public void onVanishChange(net.ess3.api.events.AfkStatusChangeEvent e){
        if(e.getValue()){
            Main.getInstance().getPkConnector().sendData(0x15, new String[]{
                    e.getAffected().getName(),
                    "AFK"
            });
        }else{
            Main.getInstance().getPkConnector().sendData(0x15, new String[]{
                    e.getAffected().getName(),
                    "BACK"
            });
        }
    }
}
