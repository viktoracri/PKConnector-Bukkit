package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.Bukkit;
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

        if(args.length == 0){
            sender.sendMessage("§cUsage: /findplayer <player>");
            sender.sendMessage("§cFind what parkour server a player is playing on");
        }else if(Bukkit.getPlayer(args[0]) != null){
            sender.sendMessage("§a" + Bukkit.getPlayer(args[0]).getName() + "§6 is online on §a" + Main.getInstance().NAME);
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
