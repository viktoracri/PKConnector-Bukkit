package dev.acri.pkconnector.bukkit.listener;


import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

public class ConnectionListener implements Runnable {

    private long disconnected = -1;
    @Override
    public void run() {
        while (Main.getInstance().isEnabled()) {
            readInputStream();
        }
    }

    public void readInputStream(){
        try {

            DataInputStream in = new DataInputStream(Main.getInstance().getSocket().getInputStream());


            int initialByte = in.readByte();

            if(disconnected != -1) disconnected = -1;

            if(initialByte == 0x05 || initialByte == -1 || initialByte == 0x00) return;


            short length = in.readShort();
            byte[] b = new byte[length];
            while(in.available() < length){

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            in.readFully(b);




            in = new DataInputStream(new ByteArrayInputStream(b));

            switch(initialByte) {
                case 0x02: // Authenticated successfully
                    Main.getInstance().NAME = in.readUTF();
                    Main.getInstance().IDENTIFIER = in.readUTF();
                    Main.getInstance().getPkConnector().setSessionID(in.readUTF());
                    //Main.getInstance().getPkConnector().startThread();
                    Main.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[PKConnector] Authenticated as " + Main.getInstance().NAME + " with identifier " + Main.getInstance().IDENTIFIER);
                    Main.getInstance().getPkConnector().setDisconnected(false);

                    for(Player all : Bukkit.getOnlinePlayers())
                        Main.getInstance().getPkConnector().sendData(0x12, new String[]{
                                all.getUniqueId().toString(),
                                all.getName()
                        });
                    break;

                case 0x03: // Authentication denied
                    Bukkit.getLogger().info("Could not connect. Reason: " + in.readUTF());
                    Main.getInstance().getSocket().close();
                    Main.getInstance().getConnectionListenerThread().stop();
                    break;
                case 0x04: // System message
                    Bukkit.broadcastMessage("System message received: " + in.readUTF());
                    break;
                case 0x6: // Global message
                    if(!Main.getInstance().getConfig().getBoolean("global-chat-enabled")){
                        return;
                    }
                    String identifier = in.readUTF();
                    String player = in.readUTF();
                    String message = in.readUTF();
                    String finalMessage = Main.getInstance().getConfiguration().getString("ChatFormat.Global").replaceAll("&", "§")
                            .replaceAll("\\{server}", identifier)
                            .replaceAll("\\{player}", player);


                     /*
                            Filter out bad words
                     */
                    StringBuilder badwordBuilder = new StringBuilder();
                    for(String word : message.split(" "))
                        if(Main.getInstance().getConfig().getStringList("bad-word-filter").contains(word.toLowerCase())) badwordBuilder.append(new String(new char[word.length()]).replace("\0", "*")).append(" ");
                        else badwordBuilder.append(word).append(" ");

                    for (User u : Main.getInstance().getUserList())
                        if (u.isGlobalChatEnabled()) {

                            /*
                                    Highlight names
                             */
                            StringBuilder builder = new StringBuilder();
                            boolean containsName = false;
                            for(String word : badwordBuilder.toString().split(" "))
                                if(word.equalsIgnoreCase(u.getPlayer().getName())) {
                                    builder.append("§e").append(word).append("§r ");
                                    containsName = true;
                                }
                                else builder.append(word).append(" ");


                            u.getPlayer().sendMessage(
                                    finalMessage.replace("{message}",
                                            builder.toString().substring(0, builder.toString().length() - 1)));


                        }


                    Bukkit.getConsoleSender().sendMessage("[Global Chat] " + finalMessage.replace("{message}", message));

                    break;
                case 0x7:
                    String uuid = in.readUTF();
                    if (Bukkit.getPlayer(UUID.fromString(uuid)) != null) {
                        User u = Main.getInstance().getUser(UUID.fromString(uuid));
                        if(Main.getInstance().getConfig().getBoolean("global-chat-enabled")) u.setGlobalChatEnabled(in.readBoolean());
                        else in.readBoolean();
                        u.setAccessStaffChat(in.readBoolean());
                        u.setAccessVeteranChat(in.readBoolean());
                        u.setChatChannel(ChatChannel.get(in.readUTF()));
                        u.setGlobalChatSendBanned(in.readBoolean());
                    }
                    break;
                case 0x9:
                    uuid = in.readUTF();
                    String target = in.readUTF();
                    String answer = in.readUTF();


                    String format = "§a{target} §6is{P} §a{answer}§6.";

                    if(answer.contains("{NONE}")) {
                        format = format.replace("{P}", "");
                        answer = answer.replace("{NONE}", "");
                    }
                    else format = format.replace("{P}", " online on");

                    if(answer.equals("")){
                        Bukkit.getPlayer(UUID.fromString(uuid)).sendMessage("§cCouldn't find player '" + target + "'");
                    }else{
                        Bukkit.getPlayer(UUID.fromString(uuid)).sendMessage(
                                format.replace("{target}", target).replace("{answer}", answer)
                        );
                    }
                        break;
                case 0xa:

                    uuid = in.readUTF();
                    String server = in.readUTF();
                    String result = in.readUTF();

                    Player p = Bukkit.getPlayer(UUID.fromString(uuid));
                    if(p != null){

                        if(server.equals("STAFF")){
                            p.sendMessage("§6Staff list: " + result);
                        }else if(server.equals("VETERAN")){
                            p.sendMessage("§6Veteran list: " + result);
                        }else if(result.equals("UNKNOWN_TARGET")){
                            p.sendMessage("§cCan't find server '" + server + "'");
                        }else if(result.equals("")){
                            p.sendMessage("§c" + server + " is empty.");
                        }else{
                            p.sendMessage("§6Players online on §e" + server + "§6:");
                            p.sendMessage(result);
                        }
                    }





                    break;
                case 0xb:
                    String from = in.readUTF();
                    String to = in.readUTF();
                    String msg = in.readUTF();
                    server = in.readUTF();
                    String type = in.readUTF();
                    if(type.equals("FROM")){
                        Bukkit.getPlayer(to).sendMessage(Main.getInstance().getConfiguration().getString("Message.From").replaceAll("&", "§")
                                .replace("{player}", from).replace("{message}", msg).replace("{server}", server));
                        Main.getInstance().getUser(Bukkit.getPlayer(to)).setLastMessaged(from);
                    }else if(type.equals("TO")){
                        if(msg.equals("")){
                            Bukkit.getPlayer(from).sendMessage("§cCouldn't find player '" + to + "'");
                        }else{
                            Bukkit.getPlayer(from).sendMessage(Main.getInstance().getConfiguration().getString("Message.To").replaceAll("&", "§")
                                    .replace("{player}", to).replace("{message}", msg).replace("{server}", server));
                        }
                        Main.getInstance().getUser(Bukkit.getPlayer(from)).setLastMessaged(to);

                    }

                    break;
                case 0xc:

                    player = in.readUTF();
                    result = in.readUTF();
                    if(Bukkit.getPlayer(UUID.fromString(player)) != null){
                        Bukkit.getPlayer(UUID.fromString(player)).sendMessage("§6List of servers connected to PKConnector:");
                        for(String section : result.split("\n"))
                            Bukkit.getPlayer(UUID.fromString(player)).sendMessage(section);
                    }


                    break;
                case 0xd:
                    identifier = in.readUTF();
                    player = in.readUTF();
                    message = in.readUTF();
                    finalMessage = Main.getInstance().getConfiguration().getString("ChatFormat.Staff").replaceAll("&", "§")
                            .replaceAll("\\{server}", identifier)
                            .replaceAll("\\{player}", player)
                            .replaceAll("\\{message}", message);
                    for(User u2 : Main.getInstance().getUserList())
                        if(u2.isAccessStaffChat()) u2.getPlayer().sendMessage(finalMessage);
                    break;
                case 0xe:
                    identifier = in.readUTF();
                    player = in.readUTF();
                    message = in.readUTF();
                    finalMessage = Main.getInstance().getConfiguration().getString("ChatFormat.Veteran").replaceAll("&", "§")
                            .replaceAll("\\{server}", identifier)
                            .replaceAll("\\{player}", player)
                            .replaceAll("\\{message}", message);
                    for(User u1 : Main.getInstance().getUserList())
                        if(u1.isAccessVeteranChat()) u1.getPlayer().sendMessage(finalMessage);
                    break;
                case 0xf:
                    String user = in.readUTF();
                    msg = in.readUTF();
                    p = null;
                    if(Bukkit.getPlayerExact(user) != null) p = Bukkit.getPlayerExact(user);
                    else{
                        try{
                            p = Bukkit.getPlayer(UUID.fromString(user));
                        }catch(IllegalArgumentException ignored){}
                    }

                    if(p != null) p.sendMessage(msg.replace("&", "§"));
                    break;
                case 0x10:
                    msg = in.readUTF();
                    Bukkit.broadcastMessage(msg);
                    break;

            }



        } catch (IOException ignored) {
            if(disconnected == -1) disconnected = System.currentTimeMillis();
            if(System.currentTimeMillis() - disconnected > 3000) Main.getInstance().getPkConnector().setDisconnected(true);
        }
    }


}