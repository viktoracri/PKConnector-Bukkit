package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ParkourAdminCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(Main.getInstance().getPkConnector().isDisconnected()){
            sender.sendMessage("§cThis server is not currently connected to PKConnector.");
            return true;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can execute this command");
            return true;
        }

        User u = Main.getInstance().getUser((Player) sender);

        if(!u.hasAdminAccess()){
            sender.sendMessage("§4You do not have access to this command");
            return true;
        }

        if(args.length < 2){
            sender.sendMessage("§cSubcommands: ");
            sender.sendMessage("§c/pkadmin info <player>");
            sender.sendMessage("§c/pkadmin ban <player>");
            sender.sendMessage("§c/pkadmin staff <player>");
            sender.sendMessage("§c/pkadmin veteran <player>");
        }else if(args[0].equalsIgnoreCase("info")){
            Main.getInstance().getPkConnector().sendData(0x18, new Object[]{
                    sender.getName(), "INFO", args[1]
            });
        }else if(args[0].equalsIgnoreCase("ban")){
            Main.getInstance().getPkConnector().sendData(0x18, new Object[]{
                    sender.getName(), "BAN", args[1]
            });
        }else if(args[0].equalsIgnoreCase("staff")){
            Main.getInstance().getPkConnector().sendData(0x18, new Object[]{
                    sender.getName(), "STAFF", args[1]
            });
        }else if(args[0].equalsIgnoreCase("veteran")){
            Main.getInstance().getPkConnector().sendData(0x18, new Object[]{
                    sender.getName(), "VETERAN", args[1]
            });
        }else{
            sender.sendMessage("§cInvalid subcommand.");
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
