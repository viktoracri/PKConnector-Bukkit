package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.concurrent.Executors;

public class ConnectCommand implements TabCompleter, CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(sender.isOp() || sender.getName().equals("Viktoracri")){
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
        }else{
            sender.sendMessage("§cNo Permission");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }
}
