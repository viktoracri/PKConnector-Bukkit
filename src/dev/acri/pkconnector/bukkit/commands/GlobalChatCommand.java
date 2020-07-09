package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GlobalChatCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!Main.getInstance().getConfig().getBoolean("global-chat-enabled")){
            sender.sendMessage("§cThis server has disabled global chat.");
            return true;
        }

        if(Main.getInstance().getPkConnector().isDisconnected()){
            sender.sendMessage("§cThis server is not currently connected to PKConnector.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: §7/" + label + " <message>");
            sender.sendMessage("§cSends a chat message globally, across all parkour servers.");
        } else if (!(sender instanceof Player)) {

            sender.sendMessage("§cOnly players can execute this command");
        } else if (!Main.getInstance().getUser((Player) sender).isGlobalChatEnabled()) {
            sender.sendMessage("§cYou have disabled global chat. Enable it with /togglechatg");
        } else if (Main.getInstance().getUser((Player) sender).isGlobalChatSendBanned()) {
            sender.sendMessage("§cYou are banned from sending global chat messages.");
        }else{
            StringBuilder builder = new StringBuilder();
            for(String arg : args)
                builder.append(arg).append(" ");

            String message = builder.toString().substring(0, builder.toString().length() - 1);

            Main.getInstance().getUser((Player) sender).setNextMessageGlobalChat(true);
            ((Player) sender).chat(message);
        }



        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return new ArrayList<>();
    }
}
