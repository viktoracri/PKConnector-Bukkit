package dev.acri.pkconnector.bukkit.listener;

import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e){

        if(e.isCancelled())return;

        User u = Main.getInstance().getUser(e.getPlayer());

        if(e.getMessage().startsWith("§3§4")){
            e.setMessage(e.getMessage().substring(4));
            return;
        }


        else if(u.getChatChannel() == ChatChannel.GLOBAL || u.isNextMessageGlobalChat()){
            if(u.isNextMessageGlobalChat())u.setNextMessageGlobalChat(false);
            e.setCancelled(true);
            if(!u.isGlobalChatEnabled()){
                e.getPlayer().sendMessage("§cYou have disabled global chat. Enable it with /togglechatg");
            }else Main.getInstance().sendGlobalChat(e.getPlayer(), e.getMessage());
            return;
        }

        if(u.getChatChannel() == ChatChannel.STAFF){
            e.setCancelled(true);
            Main.getInstance().sendStaffChat(e.getPlayer(), e.getMessage());
        }

        if(u.getChatChannel() == ChatChannel.VETERAN){
            e.setCancelled(true);
            Main.getInstance().sendVeteranChat(e.getPlayer(), e.getMessage());
        }




    }
}
