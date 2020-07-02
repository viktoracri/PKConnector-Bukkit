package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.commands.*;
import dev.acri.pkconnector.bukkit.listener.PlayerChatListener;
import dev.acri.pkconnector.bukkit.listener.PlayerJoinListener;
import dev.acri.pkconnector.bukkit.listener.PlayerQuitListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends JavaPlugin {

    private static Main instance;
    private Socket socket;

    private final double CHAT_DELAY = 2.0;

    private PKConnector pkConnector;

    public String NAME = "Unknown Name";
    public String IDENTIFIER = "????";

    private Thread connectionListenerThread;

    private List<User> userList = new ArrayList<>();

    DecimalFormat numberFormat = new DecimalFormat("0.0");



    @Override
    public void onEnable() {
        instance = this;
        /*
        try{
            wrapper = new VersionMatcher().match();
        }catch(RuntimeException e){e.printStackTrace();}

         */


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
        registerCommand("pkconnector", PKConnectorCommand.class);
        registerCommand("chat", ChatCommand.class);
        registerCommand("pklist", PKListCommand.class);
        registerCommand("pkservers", ConnectedServersCommand.class);

        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);


        for(Player all : Bukkit.getOnlinePlayers()) {
            userList.add(new User(all));
        }

        ExecutorService ex = Executors.newCachedThreadPool();
        ex.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pkConnector.connect();


        });
        ex.shutdown();




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
        getConfig().addDefault("ChatFormat.Global", "&8[&7{server}&8] &7{player} &8» &f{message}");
        getConfig().addDefault("ChatFormat.Staff", "&c&lS&4: &7[&c{server}&7] &c{player} &4» &f{message}");
        getConfig().addDefault("ChatFormat.Veteran", "&b&lV&3: &7[&3{server}&7] &b{player} &3» &f{message}");
        getConfig().addDefault("Message.From", "&6From &8[&7{server}&8] &e{player}&6: &7{message}");
        getConfig().addDefault("Message.To", "&6To &8[&7{server}&8] &e{player}&6: &7{message}");
        getConfig().addDefault("global-chat-enabled", true);
        getConfig().addDefault("new-users-disable-global-chat", false);
        getConfig().addDefault("bad-word-filter", Arrays.asList(
                "fuck", "nigger", "n1gger", "fucking"));


        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public void sendGlobalChat(Player player, String message){
        if(socket == null){
            player.sendMessage("§cThis server is not connected to PKConnector. Contact a server admin if you think this is in error.");
            return;
        }

        if(!Main.getInstance().getConfig().getBoolean("global-chat-enabled")){
            player.sendMessage("§cThis server has disabled global chat.");
            return;
        }
        User u = getUser(player);
        if(u.isGlobalChatSendBanned()){
            player.sendMessage("§cYou are banned from sending global chat messages.");
            return;
        }
        if(u.getLastMessageTime() != -1) if(System.currentTimeMillis() - u.getLastMessageTime() < CHAT_DELAY*1000){
            double seconds = (CHAT_DELAY*1000 - (System.currentTimeMillis() - u.getLastMessageTime()))/1000;
            player.sendMessage("§cPlease wait " + numberFormat.format(seconds) + " second" + (seconds >1.0 ? "s" : "") + " before typing again.");
            return;
        }
        if(u.getLastMessage().equalsIgnoreCase(message)){
            player.sendMessage("§cYou cannot say the same thing twice in a row.");
            return;
        }



        u.setLastMessageTime(System.currentTimeMillis());

        u.setLastMessage(message);


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


    public YamlConfiguration getConfiguration() {
        return (YamlConfiguration) getConfig();
    }

}
