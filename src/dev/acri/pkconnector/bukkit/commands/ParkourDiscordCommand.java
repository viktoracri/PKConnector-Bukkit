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

public class ParkourDiscordCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        sender.sendMessage("§6PK Community Discord: §e§nhttps://discord.gg/v8R34hR");


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
       return new ArrayList<>();
    }

}
