package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.commands.*;
import dev.acri.pkconnector.bukkit.listener.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends JavaPlugin {

    public static final String version = "0.11.3";
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
        registerCommand("togglegmsg", GlobalMessageToggle.class);
        registerCommand("pkip", ParkourAddressCommand.class);
        registerCommand("pkdiscord", ParkourDiscordCommand.class);
        registerCommand("pkignore", ParkourIgnoreCommand.class);

        Bukkit.getPluginManager().registerEvents(new PlayerChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(), this);

        if(Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            Bukkit.getPluginManager().registerEvents(new VanishChangeListener(), this);
            Bukkit.getPluginManager().registerEvents(new AFKChangeListener(), this);
        }

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



        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Plugin p = Bukkit.getPluginManager().getPlugin("PKConnectorReloader");
            if(p != null){
                String PLUGIN_FILE = p.getClass().getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .getPath();
                unload(p);

                new File(PLUGIN_FILE).delete();

                Bukkit.getConsoleSender().sendMessage("§e[PKConnector] Successfully updated to PKConnector version " + getDescription().getVersion() + "!");
            }
        }, 40);

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

        getConfig().addDefault("ChatFormat.Global", "&8[&7{server}&8] &7{player} &8» &f{message}");
        getConfig().addDefault("ChatFormat.Staff", "&c&lS&4: &7[&c{server}&7] &c{player} &4» &f{message}");
        getConfig().addDefault("ChatFormat.Veteran", "&b&lV&3: &7[&3{server}&7] &b{player} &3» &f{message}");
        getConfig().addDefault("Message.From", "&6From &8[&7{server}&8] &e{player}&6: &7{message}");
        getConfig().addDefault("Message.To", "&6To &8[&7{server}&8] &e{player}&6: &7{message}");
        getConfig().addDefault("global-chat-enabled", true);
        getConfig().addDefault("auto-disable-global-chat-on-join", false);
        getConfig().addDefault("new-users-disable-global-chat", false);
        getConfig().addDefault("bad-word-filter", Arrays.asList(
                "fuck", "shit", "blimey"));
        getConfig().addDefault("auto-update", true);
        getConfig().addDefault("community-announcements", true);
        getConfig().addDefault("global-private-messages-enabled", true);

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public void sendGlobalChat(Player player, String message){
        if(pkConnector.isDisconnected()){
            player.sendMessage("§cThis server is not currently connected to PKConnector.");
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
        data.add(player.getUniqueId().toString());

        Main.getInstance().getPkConnector().sendData(0x6, data);


    }

    public void sendStaffChat(Player player, String message){
        if(pkConnector.isDisconnected()){
            player.sendMessage("§cThis server is not currently connected to PKConnector.");
            return;
        }
        List<Object> data = new ArrayList<>();
        data.add(player.getName());
        data.add(message);

        Main.getInstance().getPkConnector().sendData(0xd, data);

    }

    public void sendVeteranChat(Player player, String message){
        if(pkConnector.isDisconnected()){
            player.sendMessage("§cThis server is not currently connected to PKConnector.");
            return;
        }
        List<Object> data = new ArrayList<>();
        data.add(player.getName());
        data.add(message);

        Main.getInstance().getPkConnector().sendData(0xe, data);

    }

    public void updatePlugin(){

        for(User u : userList)
            u.save();


        try {
            URL FILE_URL = new URL("https://honeyfrost.net/content/PKConnector/PKConnectorPlugin.jar");
            String DESTINATION = Main.getInstance().getClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            URL EXTERNAL_RELOADER = new URL("https://honeyfrost.net/content/PKConnector/PKConnectorReloader.jar");
            String RELOADER_DESTINATION = Main.getInstance().getDataFolder().getParentFile().getAbsolutePath() + "/PKConnectorReloader.jar";


            Bukkit.getConsoleSender().sendMessage("§e[PKConnector] Downloading update...");

            System.out.println(DESTINATION);
            final String pathname = DESTINATION.substring(0, DESTINATION.lastIndexOf("/")) + "/__NEW__PKConnectorPlugin.jar";
            FileUtils.copyURLToFile(FILE_URL, new File(pathname));

            Thread t = new Thread(() -> { try {
                    InputStream ex_in = null;
                    try{
                        ex_in= EXTERNAL_RELOADER.openStream();
                    }catch(IOException e){
                        Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Could not download reloader. Restart the server to apply the update.");
                        return;
                    }

                    Files.copy(ex_in, Paths.get(RELOADER_DESTINATION), StandardCopyOption.REPLACE_EXISTING);

                    Bukkit.getPluginManager().loadPlugin(new File(RELOADER_DESTINATION));
                    Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("PKConnectorReloader"));

                } catch (IOException | InvalidPluginException | InvalidDescriptionException e) {
                    e.printStackTrace();
                    Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Could not download reloader. Restart the server to apply the update.");
                }
            }, "PluginReloader");
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
        Taken from PlugMan
     */
    public static void unload(Plugin plugin) {

        String name = plugin.getName();

        PluginManager pluginManager = Bukkit.getPluginManager();

        SimpleCommandMap commandMap = null;

        List<Plugin> plugins = null;

        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;

        boolean reloadlisteners = true;

        if (pluginManager != null) {

            pluginManager.disablePlugin(plugin);

            try {

                Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                plugins = (List<Plugin>) pluginsField.get(pluginManager);

                Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

                try {
                    Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                    listenersField.setAccessible(true);
                    listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
                } catch (Exception e) {
                    reloadlisteners = false;
                }

                Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                commands = (Map<String, Command>) knownCommandsField.get(commandMap);

            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null && plugins.contains(plugin))
            plugins.remove(plugin);

        if (names != null && names.containsKey(name))
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                for (Iterator<RegisteredListener> it = set.iterator(); it.hasNext(); ) {
                    RegisteredListener value = it.next();
                    if (value.getPlugin() == plugin) {
                        it.remove();
                    }
                }
            }
        }

        if (commandMap != null) {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {

            try {

                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
            }

            try {

                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
            }

        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();


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
