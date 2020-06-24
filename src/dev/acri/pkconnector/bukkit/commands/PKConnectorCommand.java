package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
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

        if(sender.isOp() || sender.getName().equals("Viktoracri")){
            if(args.length == 0){
                sendHelp(sender);
            }else if(args[0].equalsIgnoreCase("reconnect")){
                Main.getInstance().getPkConnector().disconnect();
                Executors.newCachedThreadPool().execute(() ->{
                    try {
                        Thread.sleep(1000);
                        Main.getInstance().getPkConnector().connect();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                });
                sender.sendMessage("§6Reconnecting to host...");
            }else if(args[0].equalsIgnoreCase("reloadconfig")){
                try {
                    Main.getInstance().getConfiguration().load(new File(Main.getInstance().getDataFolder() + "/config.yml"));
                } catch (IOException | InvalidConfigurationException e) {
                    e.printStackTrace();
                }
                sender.sendMessage("§6Reloading config...");
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
