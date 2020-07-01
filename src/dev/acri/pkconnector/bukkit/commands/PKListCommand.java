package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PKListCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser(((Player) sender).getUniqueId());


        if(args.length == 0){
            sender.sendMessage("§cUsage: §7/pklist <server" + (u.isAccessStaffChat() ? "/staff" : "") + (u.isAccessVeteranChat() ? "/veteran" : "") + ">");
            sender.sendMessage("§cList players online on a server" + (u.isAccessStaffChat() || u.isAccessVeteranChat() ? " or a specific chat channel." : "."));
        }else if(args[0].equalsIgnoreCase("staff") && !u.isAccessStaffChat()){
            sender.sendMessage("§cYou don't have access to list staff players");
        }else if(args[0].equalsIgnoreCase("veteran") && !u.isAccessVeteranChat()){
            sender.sendMessage("§cYou don't have access to list veteran players");
        }else{

            String target = args[0];
            if(target.equalsIgnoreCase("staff")) target = "STAFF";
            else if(target.equalsIgnoreCase("veteran")) target = "VETERAN";

            Main.getInstance().getPkConnector().sendData(0xa, new String[]{
                    ((Player) sender).getUniqueId().toString(),
                    target
            });
        }


        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
