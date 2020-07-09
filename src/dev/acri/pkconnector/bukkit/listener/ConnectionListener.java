package dev.acri.pkconnector.bukkit.listener;


import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
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

            DataInputStream in = new DataInputStream(Main.getInstance().getSocket().getInputStream());

            initialByte = in.readByte();
            if(disconnected != -1) disconnected = -1;

            if(initialByte == 0x05 || initialByte == -1 || initialByte == 0x00) return;


            length = in.readShort();
//            System.out.println("Received 0x" + Integer.toHexString(initialByte) + ", length: " + length);


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

            if(initialByte != 0x03) {

                byte[] encryptedKey = new byte[256];
                for(int i = 0; i < 256; i++){
                    encryptedKey[i] = (byte) in.read();
                }

                int informationSize = in.readShort();
//                System.out.println("byte: 0x" + Integer.toHexString(initialByte) + ", infoSize: " + informationSize);
                byte[] information = new byte[informationSize];
                for(int i = 0; i < informationSize; i++){
                    information[i] = (byte) in.read();
                }

                cipher.init(Cipher.PRIVATE_KEY, Main.getInstance().getPkConnector().getPrivateKey());
                byte[] decryptedKey = cipher.doFinal(encryptedKey);

                SecretKey originalKey = new SecretKeySpec(decryptedKey , 0, decryptedKey .length, "AES");
                Cipher aesCipher = Cipher.getInstance("AES");
                aesCipher.init(Cipher.DECRYPT_MODE, originalKey);
                b = aesCipher.doFinal(information);


            }


            in = new DataInputStream(new ByteArrayInputStream(b));


            switch(initialByte) {
                case 0x02: // Authenticated successfully
                    Main.getInstance().NAME = in.readUTF();
                    Main.getInstance().IDENTIFIER = in.readUTF();
                    Main.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[PKConnector] Authenticated as " + Main.getInstance().NAME + " with identifier " + Main.getInstance().IDENTIFIER);
                    Main.getInstance().getPkConnector().setDisconnected(false);
                    ExecutorService ex = Executors.newSingleThreadExecutor();

                    for(Player all : Bukkit.getOnlinePlayers())
                        Main.getInstance().getPkConnector().sendData(0x12, new String[]{
                                all.getUniqueId().toString(),
                                all.getName()
                        });

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
                    ex.shutdown();
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
                case 0x7: // Player information
                    String uuid = in.readUTF();
                    if (Bukkit.getPlayer(UUID.fromString(uuid)) != null) {
                        User u = Main.getInstance().getUser(UUID.fromString(uuid));
                        if(Main.getInstance().getConfig().getBoolean("auto-disable-global-chat-on-join")) {
                            u.setGlobalChatEnabled(false);
                            in.readBoolean();
                        }
                        else u.setGlobalChatEnabled(in.readBoolean());
                        u.setAccessStaffChat(in.readBoolean());
                        u.setAccessVeteranChat(in.readBoolean());
                        u.setChatChannel(ChatChannel.get(in.readUTF()));
                        u.setGlobalChatSendBanned(in.readBoolean());
                        u.setPrivateMessagesEnabled(in.readBoolean());
                        u.setAdminAccess(in.readBoolean());
                    }
                    break;
                case 0x9: // Find results
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

                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(" §8§l» §6§lGlobal Announcement §8§l» §e" + message.replaceAll("&", "§"));
                    Bukkit.broadcastMessage("");

                    break;
                case 0x18: // PKAdmin results
                    target = in.readUTF();
                    String reply = in.readUTF();
                    Player player1 = Bukkit.getPlayer(target);
                    if(player1 != null){
                        if(reply.equals("INVALID_PLAYER")){
                            player1.sendMessage("§cCouldn't find player " + in.readUTF());
                        }else if(reply.equals("BAN")){
                            player1.sendMessage("§aPlayer " + in.readUTF() + " was " + (in.readBoolean() ? "" : "un") + "banned from global chat.");
                        }else if(reply.equals("VETERAN")){
                            String player2 = in.readUTF();
                            player1.sendMessage(in.readBoolean() ? "§aPlayer " + player2 +" was granted access to Veteran chat!" :
                                    "§aPlayer " + player2 +" no longer has access to Veteran chat.");
                        }else if(reply.equals("STAFF")){
                            String player2 = in.readUTF();
                            player1.sendMessage(in.readBoolean() ? "§aPlayer " + player2 +" was granted access to Staff chat!" :
                                    "§aPlayer " + player2 +" no longer has access to Staff chat.");
                        }else if(reply.equals("INFO")){
                            String player2 = in.readUTF();
                            boolean veteranChat = in.readBoolean();
                            boolean staffChat = in.readBoolean();
                            boolean admin = in.readBoolean();
                            boolean banned = in.readBoolean();

                            player1.sendMessage("§6Information about §e" + player2 + "§6:");
                            player1.sendMessage("§7- §eBanned from global chat: " + (banned ? "§atrue" : "§cfalse"));
                            player1.sendMessage("§7- §eStaff: " + (staffChat ? "§atrue" : "§cfalse"));
                            player1.sendMessage("§7- §eVeteran: " + (veteranChat ? "§atrue" : "§cfalse"));
                            player1.sendMessage("§7- §eAdmin: " + (admin ? "§atrue" : "§cfalse"));
                        }
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
                    }else if(type.equals("ADMIN")){ // Admin
                        u.setAdminAccess(value);

                        targetPlayer.sendMessage(value ?
                                "§aYou are now an admin."
                                : "§aYou are no longer an admin");
                    }


                    break;
            }



        } catch (IOException ignored) {
            if(disconnected == -1) disconnected = System.currentTimeMillis();
            if(System.currentTimeMillis() - disconnected > 3000) Main.getInstance().getPkConnector().setDisconnected(true);
        } catch (NoSuchPaddingException | InvalidKeyException | BadPaddingException | NoSuchAlgorithmException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }


}