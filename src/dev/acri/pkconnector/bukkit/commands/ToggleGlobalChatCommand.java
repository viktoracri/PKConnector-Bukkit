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

public class ToggleGlobalChatCommand implements TabCompleter, CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(!(sender instanceof Player)){
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser((Player) sender);

        u.setGlobalChatEnabled(!u.isGlobalChatEnabled());

        if(u.isGlobalChatEnabled()){
            sender.sendMessage("§aGlobal chat enabled");
        }else sender.sendMessage("§cGlobal chat disabled");


        return true;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
