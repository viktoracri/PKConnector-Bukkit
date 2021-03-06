package dev.acri.pkconnector.bukkit.listener;


import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import dev.acri.pkconnector.bukkit.events.GlobalChatReceivedEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.soap.Text;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionListener implements Runnable {

    Cipher cipher;

    {
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    private long disconnected = -1;
    @Override
    public void run() {
        while (Main.getInstance().isEnabled()) {
            readInputStream();
        }
    }

    public void readInputStream(){
        int initialByte = -1;
        short length = -1;
        try {

            DataInputStream dis = new DataInputStream(Main.getInstance().getSocket().getInputStream());
            initialByte = dis.readByte();
            if(disconnected != -1) disconnected = -1;

            if(initialByte == 0x05 || initialByte == -1 || initialByte == 0x00) return;


            length = dis.readShort();
//            System.out.println("Received 0x" + Integer.toHexString(initialByte) + ", length: " + length);


            byte[] b = new byte[length];
            while(dis.available() < length){

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            dis.readFully(b);



            if(initialByte != 0x03) {

                DataInputStream disEncrypted = new DataInputStream(new ByteArrayInputStream(b));
                byte[] encryptedKey = new byte[256];
                for(int i = 0; i < 256; i++){
                    encryptedKey[i] = (byte) disEncrypted.read();
                }

                int informationSize = disEncrypted.readShort();
//                System.out.println("byte: 0x" + Integer.toHexString(initialByte) + ", infoSize: " + informationSize);
                byte[] information = new byte[informationSize];
                for(int i = 0; i < informationSize; i++){
                    information[i] = (byte) disEncrypted.read();
                }

                cipher.init(Cipher.PRIVATE_KEY, Main.getInstance().getPkConnector().getPrivateKey());
                byte[] decryptedKey = cipher.doFinal(encryptedKey);

                SecretKey originalKey = new SecretKeySpec(decryptedKey , 0, decryptedKey .length, "AES");
                Cipher aesCipher = Cipher.getInstance("AES");
                aesCipher.init(Cipher.DECRYPT_MODE, originalKey);
                b = aesCipher.doFinal(information);

            }


            try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(b))){
                switch(initialByte) {
                    case 0x02: // Authenticated successfully
                        Main.getInstance().NAME = in.readUTF();
                        Main.getInstance().IDENTIFIER = in.readUTF();
                        Main.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[PKConnector] Authenticated as " + Main.getInstance().NAME + " with identifier " + Main.getInstance().IDENTIFIER);
                        Main.getInstance().getPkConnector().setDisconnected(false);

                        for(Player all : Bukkit.getOnlinePlayers())
                            Main.getInstance().getPkConnector().sendData(0x12, new String[]{
                                    all.getUniqueId().toString(),
                                    all.getName()
                            });

                        if(!Main.getInstance().getConfig().getBoolean("global-private-messages-enabled"))
                            Main.getInstance().getPkConnector().sendData(0x1b, Collections.singletonList("DISABLED_PM"));

                        if(Bukkit.getPluginManager().getPlugin("Essentials") != null){
                            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                                com.earth2me.essentials.Essentials es = (com.earth2me.essentials.Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
                                for(Player all : Bukkit.getOnlinePlayers()){
                                    com.earth2me.essentials.User user = es.getUser(all.getUniqueId());
                                    if(user.isVanished()){

                                        Main.getInstance().getPkConnector().sendData(0x14, new String[]{
                                                all.getName(),
                                                "HIDE"
                                        });
                                    }
                                    if(user.isAfk()){
                                        Main.getInstance().getPkConnector().sendData(0x15, new String[]{
                                                all.getName(),
                                                "AFK"
                                        });
                                    }
                                }


                            }, 10);
                        }
                        break;

                    case 0x03: // Authentication denied
                        String reason = in.readUTF();
                        Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Could not connect. Reason: " + reason);
                        if(reason.contains("Unsupported version")){
                            if(Main.getInstance().getConfig().getBoolean("auto-update"))
                                Main.getInstance().updatePlugin();
                            else Bukkit.getConsoleSender().sendMessage("§c[PKConnector] You have disabled automatic updates. Please update the plugin manually to use PKConnector.");
                        }
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
                        String message = in.readUTF().replaceAll("§", "&");
                        String ignoringPlayers = in.readUTF();
                        String uuid = in.readUTF();

                        GlobalChatReceivedEvent event = new GlobalChatReceivedEvent(UUID.fromString(uuid), player, identifier, message);
                        Bukkit.getServer().getPluginManager().callEvent(event);

                        if(event.isCancelled()){
                            break;
                        }

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

                        users:
                        for (User u : Main.getInstance().getUserList())
                            if (u.isGlobalChatEnabled()) {

                                for(String ignoringPlayer : ignoringPlayers.split(";"))
                                    if(ignoringPlayer.equals(u.getPlayer().getName())){
//                                        TextComponent hoverMessage = new TextComponent("[IGNORED]");
//                                        hoverMessage.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
//                                        hoverMessage.setHoverEvent( new HoverEvent( HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(badwordBuilder.toString()).create()));
//                                        TextComponent textComponent = new TextComponent(finalMessage.split("\\{message\\}")[0]);
//                                        textComponent.addExtra(hoverMessage);
//                                        if(finalMessage.contains("{message}"))textComponent.addExtra(new TextComponent(finalMessage.split("\\{message\\}")[1]));
//                                        u.getPlayer().spigot().sendMessage(textComponent);

                                        continue users;
                                    }

                        /*
                                Highlight names
                         */
                                StringBuilder builder = new StringBuilder();
                                for(String word : badwordBuilder.toString().split(" "))
                                    if(word.equalsIgnoreCase(u.getPlayer().getName())) {
                                        builder.append("§e").append(word).append("§r ");
                                    }
                                    else builder.append(word).append(" ");


                                u.getPlayer().sendMessage(
                                        finalMessage.replace("{message}",
                                                builder.toString().substring(0, builder.toString().length() - 1)));


                            }


                        Bukkit.getConsoleSender().sendMessage("[Global Chat] " + finalMessage.replace("{message}", message));

                        break;
                    case 0x7: { // Player information
                        uuid = in.readUTF();
                        if (Bukkit.getPlayer(UUID.fromString(uuid)) != null) {
                            User u = Main.getInstance().getUser(UUID.fromString(uuid));
                            if (Main.getInstance().getConfig().getBoolean("auto-disable-global-chat-on-join")) {
                                u.setGlobalChatEnabled(false);
                                in.readBoolean();
                            } else u.setGlobalChatEnabled(in.readBoolean());
                            u.setAccessStaffChat(in.readBoolean());
                            u.setAccessVeteranChat(in.readBoolean());
                            // u.setChatChannel(ChatChannel.get(in.readUTF()));
                            u.setGlobalChatSendBanned(in.readBoolean());
                            u.setPrivateMessagesEnabled(in.readBoolean());
                            String ignoredPlayers = in.readUTF();
                            if (ignoredPlayers.contains(";")) u.setIgnoredPlayers(ignoredPlayers);
                        }
                        break;
                    }case 0x9: // Find results
                        uuid = in.readUTF();
                        String target = in.readUTF();
                        String answer = in.readUTF();

                        if(target.contains("{AFK}")){
                            target = target.replace("{AFK}", "") + " §7[AFK]";
                        }

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
                    case 0xa: // /pklist result

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
                    case 0xb: // global private messages
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

                        }else if(type.equals("DISABLED_PM")){
                            Bukkit.getPlayer(from).sendMessage("§cPlayer has disabled global private messages.");
                        }else if(type.equals("SERVER_DISABLED_PM")){
                            Bukkit.getPlayer(from).sendMessage("§cThe server the player is on has disabled global private messages.");
                        }else if(type.equals("FROM_IGNORING")){
                            Bukkit.getPlayer(from).sendMessage("§cYou are currently ignoring this player.");
                        }

                        break;
                    case 0xc: // pkservers

                        player = in.readUTF();
                        result = in.readUTF();
                        if(Bukkit.getPlayer(UUID.fromString(player)) != null){
                            Bukkit.getPlayer(UUID.fromString(player)).sendMessage("§6List of servers connected to PKConnector:");
                            for(String section : result.split("\n"))
                                Bukkit.getPlayer(UUID.fromString(player)).sendMessage(section);
                        }


                        break;
                    case 0xd: // staff chat
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
                    case 0xe: // veteran chat
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
                    case 0xf: // send message to individual player
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
                    case 0x10: // broadcast message
                        msg = in.readUTF();
                        Bukkit.broadcastMessage(msg);
                        break;
                    case 0x17: // global announcement
                        message = in.readUTF();
                        if(Main.getInstance().getConfig().getBoolean("community-announcements")){
                            Bukkit.broadcastMessage(" ");
                            Bukkit.broadcastMessage(" §8§l» §6§lAnnouncement §8§l» §e" + message.replaceAll("&", "§"));
                            Bukkit.broadcastMessage("");
                        }


                        break;
                    case 0x19: // Role updates
                        type = in.readUTF();
                        boolean value = in.readBoolean();
                        target = in.readUTF();
                        Player targetPlayer = Bukkit.getPlayer(target);
                        if(targetPlayer == null)return;
                        User u = Main.getInstance().getUser(targetPlayer);

                        if(type.equals("BAN")){ // Ban
                            u.setGlobalChatSendBanned(value);
                            if(u.getChatChannel().equals(ChatChannel.GLOBAL)) u.setChatChannel(ChatChannel.NORMAL);
                            targetPlayer.sendMessage(value ?
                                    "§aYou were banned from global chat!"
                                    : "§aYou were unbanned from global chat.");
                        }else if(type.equals("STAFF")){ // Staff
                            u.setAccessStaffChat(value);
                            if(u.getChatChannel().equals(ChatChannel.STAFF)) u.setChatChannel(ChatChannel.NORMAL);
                            targetPlayer.sendMessage(value ?
                                    "§aYou were given access to staff chat!"
                                    : "§aYou no longer have access to staff chat.");
                        }else if(type.equals("VET")){ // Veteran
                            u.setAccessVeteranChat(value);
                            if(u.getChatChannel().equals(ChatChannel.VETERAN)) u.setChatChannel(ChatChannel.NORMAL);
                            targetPlayer.sendMessage(value ?
                                    "§aYou were given access to veteran chat!"
                                    : "§aYou no longer have access to veteran chat.");
                        }


                        break;
                    case 0x1a:
                        uuid = in.readUTF();
                        server = in.readUTF();


                        p = Bukkit.getPlayer(UUID.fromString(uuid));
                        if(p != null){
                            if(server.contains("{NOT_FOUND}")){
                                p.sendMessage("§cCould not find server '" + server.replace("{NOT_FOUND}", "") + "'");
                            }else{
                                String ip = in.readUTF();
                                p.sendMessage("§6" + server + "§e ip address: §6" + (!ip.equals("") ? ip : "§cNot assigned"));
                            }
                        }
                        break;
                }
            }













        } catch (IOException ignored) {
            if(disconnected == -1) disconnected = System.currentTimeMillis();
            if(System.currentTimeMillis() - disconnected > 3000) Main.getInstance().getPkConnector().setDisconnected(true);
        } catch (NoSuchPaddingException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }


}