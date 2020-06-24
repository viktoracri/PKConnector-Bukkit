package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChatCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser((Player) sender);

        if(args.length == 0){
            sender.sendMessage("§cUsage: /" + label + " <channel>");
            sender.sendMessage("§cSelect a chat channel");
            sendAvailableChatChannels(u);
        }else {
            ChatChannel channel = ChatChannel.getCanNull(args[0]);

            boolean ok = true;
            if(channel == ChatChannel.STAFF && !u.isAccessStaffChat())
                ok = false;
            else if(channel == ChatChannel.VETERAN && !u.isAccessVeteranChat())
                ok = false;
            else if(channel == ChatChannel.GLOBAL && u.isGlobalChatSendBanned()){
                sender.sendMessage("§cYou are banned from sending global chat messages.");
                return true;
            }

            if(ok && channel != null){
                sender.sendMessage("§aChat channel set to " + channel.name());
                u.setChatChannel(channel);
                return true;
            }

            sender.sendMessage("§cInvalid chat channel!");
            sendAvailableChatChannels(u);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) return null;
        User u = Main.getInstance().getUser((Player) sender);
        List<String> list = new ArrayList<>();
            if(args.length == 1) {
                list.add("NORMAL");
                if(!u.isGlobalChatSendBanned()) list.add("GLOBAL");
                if(u.isAccessStaffChat()) list.add("STAFF");
                if(u.isAccessVeteranChat()) list.add("VETERAN");
            }

        List<String> returnList = new ArrayList<String>();
        for(String str : list) {
            if(str.toLowerCase().startsWith(args[args.length-1].toLowerCase()))
                returnList.add(str);
        }

        return returnList;
    }

    public void sendAvailableChatChannels(User u){


        String msg = "§cAvailable chat channels: NORMAL";

        if(!u.isGlobalChatSendBanned())msg += ", GLOBAL";
        if(u.isAccessStaffChat())msg += ", STAFF";
        if(u.isAccessVeteranChat())msg += ", VETERAN";

        u.getPlayer().sendMessage(msg);
    }
}
