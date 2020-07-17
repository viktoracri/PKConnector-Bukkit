package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ParkourAddressCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser((Player) sender);

        if(args.length == 0){
            sender.sendMessage("§cUsage: §7/" + label + " <server>");
            sender.sendMessage("§cGet the public IP address of a parkour server");
        }else {

            StringBuilder server = new StringBuilder(args[0] + " ");
            for(int i = 1; i < args.length; i++)
                server.append(args[i]).append(" ");

            Main.getInstance().getPkConnector().sendData(0x1a, new String[]{
                    ((Player) sender).getUniqueId().toString(),
                    server.toString().substring(0, server.length() - 1)
            });
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
       return new ArrayList<>();
    }

}
