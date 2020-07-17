package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ParkourIgnoreCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        if(Main.getInstance().getPkConnector().isDisconnected()){
            sender.sendMessage("§cThis server is not currently connected to PKConnector.");
            return true;
        }

        if(args.length < 1){
            sender.sendMessage("§cUsage: §7/pkignore <player/list>");
            sender.sendMessage("§cIgnore or unignore a player across all connected parkour servers");
        }else if(args[0].equalsIgnoreCase("list")){
            Main.getInstance().getPkConnector().sendData(0x1c, new String[]{
                    "LIST_IGNORED_PLAYERS",
                    ((Player) sender).getUniqueId().toString()
            });
        }else if(args[0].equalsIgnoreCase(sender.getName())){
            sender.sendMessage("§cYou cannot ignore yourself.");
        }else if(args[0].equalsIgnoreCase("host")){
            sender.sendMessage("§clol");
        }else{

            Main.getInstance().getPkConnector().sendData(0x1c, new String[]{
                    args[0],
                    ((Player) sender).getUniqueId().toString()
            });
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
