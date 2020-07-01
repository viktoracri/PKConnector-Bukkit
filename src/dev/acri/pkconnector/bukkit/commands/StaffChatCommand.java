package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StaffChatCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {


        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser((Player) sender);

        if(!u.isAccessStaffChat()){
            sender.sendMessage("§cYou do not have access to this command");
            return true;
        }

        if(args.length == 0){
            sender.sendMessage("§cUsage: §7/" + label + " <message>");
            sender.sendMessage("§cSends a staff chat message globally, across all parkour servers.");
        }else{
            StringBuilder builder = new StringBuilder();
            for(String arg : args)
                builder.append(arg).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            Main.getInstance().sendStaffChat((Player) sender, message);
        }



        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
