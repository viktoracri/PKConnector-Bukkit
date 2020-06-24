package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.commands.*;
import dev.acri.pkconnector.bukkit.listener.PlayerChatListener;
import dev.acri.pkconnector.bukkit.listener.PlayerJoinListener;
import dev.acri.pkconnector.bukkit.listener.PlayerQuitListener;
import dev.acri.pkconnector.bukkit.version.VersionMatcher;
import dev.acri.pkconnector.bukkit.version.VersionWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class Main extends JavaPlugin {

    private static Main instance;
    private Socket socket;

    private PKConnector pkConnector;

    public String NAME = "Unknown Name";
    public String IDENTIFIER = "????";

    private Thread connectionListenerThread;

    private List<User> userList = new ArrayList<>();

    private VersionWrapper wrapper;




    @Override
    public void onEnable() {
        instance = this;
        try {
            wrapper = new VersionMatcher().match();
        }catch(RuntimeException e){}

        setupDefaultConfig();

        pkConnector = new PKConnector();

        registerCommand("findplayer", FindCommand.class);
        registerCommand("togglechatg", ToggleGlobalChatCommand.class);
        registerCommand("gmsg", GlobalMessageCommand.class);
        registerCommand("gr", GlobalReplyCommand.class);
        registerCommand("cg", GlobalChatCommand.class);
        registerCommand("cn", NormalChatCommand.class);
        registerCommand("cv", VeteranChatCommand.class);
        registerCommand("cs", StaffChatCommand.class);
        registerCommand("pkconnect", ConnectCommand.class);
        registerCommand("chat", ChatCommand.class);

        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);

        Executors.newCachedThreadPool().execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pkConnector.connect();

            for(Player all : Bukkit.getOnlinePlayers()) {
                userList.add(new User(all));
                Main.getInstance().getPkConnector().sendData(0x7, new String[]{
                        all.getUniqueId().toString()
                });

            }
        });




    }

    @Override
    public void onDisable() {

        pkConnector.disconnect();

        for(Player all : Bukkit.getOnlinePlayers()) {
            getUser(all).save();
        }

        if(Main.getInstance().getPkConnector().hostWatcherThread != null)
            if(Main.getInstance().getPkConnector().hostWatcherThread.isAlive())
                Main.getInstance().getPkConnector().hostWatcherThread.stop();

    }


    public void registerCommand(String name, Class<?> cmd_class) {
        Object cmd;
        try {
            cmd = cmd_class.newInstance();

            this.getCommand(name).setExecutor((CommandExecutor) cmd);
            if(cmd instanceof TabCompleter)
                this.getCommand(name).setTabCompleter((TabCompleter) cmd);

        } catch (InstantiationException | IllegalAccessException e) {
            System.out.println("Could not register command '" + name + "'");}
    }

    public void setupDefaultConfig() {
        File file = new File(getDataFolder(), "config.yml");

        if(!file.exists()) {
            try {file.getParentFile().mkdirs();file.createNewFile();} catch (IOException e) {e.printStackTrace();}}
        saveConfig();

        getConfig().addDefault("AuthenticationCode", "insert_code_here");


        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public void sendGlobalChat(Player player, String message){
        if(socket == null){
            player.sendMessage("§cThis server is not connected to PKConnector. Contact a server admin if you think this is in error.");
            return;
        }
        User u = getUser(player);
        if(u.isGlobalChatSendBanned()){
            player.sendMessage("§cYou are banned from sending global chat messages.");
            return;
        }

        List<Object> data = new ArrayList<>();
        data.add(player.getName());
        data.add(message);

        Main.getInstance().getPkConnector().sendData(0x6, data);

    }

    public void sendStaffChat(Player player, String message){
        if(socket == null){
            player.sendMessage("§cThis server is not connected to PKConnector. Contact a server admin if you think this is in error.");
            return;
        }
        List<Object> data = new ArrayList<>();
        data.add(player.getName());
        data.add(message);

        Main.getInstance().getPkConnector().sendData(0xd, data);

    }

    public void sendVeteranChat(Player player, String message){
        if(socket == null){
            player.sendMessage("§cThis server is not connected to PKConnector. Contact a server admin if you think this is in error.");
            return;
        }
        List<Object> data = new ArrayList<>();
        data.add(player.getName());
        data.add(message);

        Main.getInstance().getPkConnector().sendData(0xe, data);

    }

    public User getUser(Player p){
        for(User u : userList)
            if(u.getPlayer() == p)
                return u;
            return null;
    }

    public User getUser(UUID uuid){
        for(User u : userList)
            if(u.getPlayer().getUniqueId().toString().equals(uuid.toString()))
                return u;
        return null;
    }


    public static Main getInstance() {
        return instance;
    }

    public Socket getSocket() {
        return socket;
    }

    public PKConnector getPkConnector() {
        return pkConnector;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Thread getConnectionListenerThread() {
        return connectionListenerThread;
    }

    public void setConnectionListenerThread(Thread connectionListenerThread) {
        this.connectionListenerThread = connectionListenerThread;
    }

    public List<User> getUserList() {
        return userList;
    }

    public VersionWrapper getWrapper() {
        return wrapper;
    }
}
