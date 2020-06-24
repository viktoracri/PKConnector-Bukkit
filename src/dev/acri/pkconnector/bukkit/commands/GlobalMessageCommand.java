package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GlobalMessageCommand implements TabCompleter, CommandExecutor {


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(args.length < 2){
            sender.sendMessage("§cUsage: /" + label + " <player> <message>");
            sender.sendMessage("§cSend a message to any player globally.");
        }else if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
        }else if(args[0].equalsIgnoreCase(sender.getName())) {
            sender.sendMessage("§cYou cannot message yourself. ☹");

        }else if(Bukkit.getPlayer(args[0]) != null){

            /*
                    Local messaging if the target is locally online
             */
            User uSender = Main.getInstance().getUser((Player) sender);
            User uTarget = Main.getInstance().getUser(Bukkit.getPlayer(args[0]));

            uSender.setLastMessaged(uTarget.getPlayer().getName());
            uTarget.setLastMessaged(uSender.getPlayer().getName());

            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++)
                builder.append(args[i]).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            sender.sendMessage(Main.getInstance().getConfiguration().getString("Message.To").replaceAll("&", "§").replace("{player}", uTarget.getPlayer().getName()).replace("{message}", message));
            uTarget.getPlayer().sendMessage(Main.getInstance().getConfiguration().getString("Message.From").replaceAll("&", "§").replace("{player}", sender.getName()).replace("{message}", message));

        }else{

            StringBuilder builder = new StringBuilder();
            for(int i = 1; i < args.length; i++)
                builder.append(args[i]).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            Main.getInstance().getPkConnector().sendData(0xb, new String[]{
                    sender.getName(),
                    args[0],
                    message
            });



        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }

}
