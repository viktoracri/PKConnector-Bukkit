package dev.acri.pkconnector.bukkit.commands;

import dev.acri.pkconnector.bukkit.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestCommand implements TabCompleter, CommandExecutor {

    ExecutorService executorService = null;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(executorService == null){
            executorService = Executors.newCachedThreadPool();
            executorService.execute(() -> {
                while(true){
                    try {
                        Thread.sleep(500);
                        Main.getInstance().getPkConnector().sendData(0x06, new String[]{
                                "Viktoracri",
                                "Hello there"
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });
        }else{
            executorService.shutdownNow();
            executorService = null;
        }


        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return null;
    }


}
