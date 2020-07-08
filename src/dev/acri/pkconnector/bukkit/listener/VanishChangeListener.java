package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VanishChangeListener implements Listener {

    @EventHandler
    public void onVanishChange(net.ess3.api.events.VanishStatusChangeEvent e){
        if(e.getValue()){
            Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                    e.getAffected().getName(),
                    "HIDE"
            });
        }else{
            Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                    e.getAffected().getName(),
                    "SHOW"
            });
        }
    }
}
