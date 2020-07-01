package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class GlobalReplyCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length == 0){
            sender.sendMessage("§cUsage: §7/" + label + " <message>");
            sender.sendMessage("§cReply to a player globally.");
        }else if(!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can execute this command");
        }else{
            User u = Main.getInstance().getUser((Player) sender);
            if(u.getLastMessaged().equals("")){
                sender.sendMessage("§cYou don't have any players to reply to.");
                return true;
            }

            StringBuilder builder = new StringBuilder();
            for(String arg : args)
                builder.append(arg).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            Main.getInstance().getPkConnector().sendData(0xb, new String[]{
                    sender.getName(),
                    u.getLastMessaged(),
                    message
            });

        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
