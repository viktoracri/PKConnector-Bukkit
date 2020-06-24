package dev.acri.pkconnector.bukkit.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NormalChatCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {


        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }


        if(args.length == 0){
            sender.sendMessage("§cUsage: /" + label + " <message>");
            sender.sendMessage("§cSends a normal chat message.");
        }else{
            StringBuilder builder = new StringBuilder();
            for(String arg : args)
                builder.append(arg).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            ((Player) sender).chat("§3§4" + message);
        }



        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
