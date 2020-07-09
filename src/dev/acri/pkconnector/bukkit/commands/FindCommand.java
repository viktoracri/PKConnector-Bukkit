package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FindCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(Main.getInstance().getPkConnector().isDisconnected()){
            sender.sendMessage("§cThis server is not currently connected to PKConnector.");
            return true;
        }

        if(args.length == 0){
            sender.sendMessage("§cUsage: §7/findplayer <player>");
            sender.sendMessage("§cFind what parkour server a player is playing on");
        }
        else{

            List<Object> data = new ArrayList<>();
            data.add((sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : "CONSOLE");
            data.add(args[0]);
            Main.getInstance().getPkConnector().sendData(0x9, data);
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
