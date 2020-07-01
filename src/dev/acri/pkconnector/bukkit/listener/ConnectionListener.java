package dev.acri.pkconnector.bukkit.listener;


import dev.acri.pkconnector.bukkit.ChatChannel;
import dev.acri.pkconnector.bukkit.Main;
import dev.acri.pkconnector.bukkit.MojangAPI;
import dev.acri.pkconnector.bukkit.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionListener implements Runnable {
    ExecutorService executorService;

    @Override
    public void run() {
        executorService = Executors.newSingleThreadExecutor();
        while (Main.getInstance().isEnabled()) {
            readInputStream();
        }
    }

    public void readInputStream(){


        try {
            DataInputStream dis = new DataInputStream(Main.getInstance().getSocket().getInputStream());

            int initialByte = dis.readByte();
           // System.out.println(Main.getInstance().getSocket().getInetAddress().getHostAddress());
            if(initialByte != 0x05)System.out.println("Initialbyte: 0x" + Integer.toHexString(initialByte));

            if(initialByte == 0x05 || initialByte == -1 || initialByte == 0x00) return;

            if(dis.available() < 2){
                System.out.println("Available less than 2");
                return;
            }
            short length = dis.readShort();
            System.out.println("Length: " + length);
            if(length > 1000) return;

            byte[] b = new byte[length];
            while(dis.available() < length){
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Not available");
            }
            dis.readFully(b);

            System.out.println("hi");
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));

            /*executorService.execute(() -> {


                try {


                }catch(IOException e){
                    e.printStackTrace();
                }
            });*/
            switch (initialByte) {
                case 0x2: // Authenticated successfully
                    System.out.println("0x02: 1");
                    Main.getInstance().NAME = in.readUTF();
                    System.out.println("0x02: 3");
                    Main.getInstance().IDENTIFIER = in.readUTF();
                    System.out.println("0x02: 4");
                    Main.getInstance().getPkConnector().setSessionID(in.readUTF());
                    System.out.println("0x02: 5");
                    Main.getInstance().getPkConnector().startThread();
                    System.out.println("0x02: 6");
                    Main.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[PKConnector] Authenticated as " + Main.getInstance().NAME + " with identifier " + Main.getInstance().IDENTIFIER);
                    break;

                case 0x03: // Authentication denied
                    Bukkit.getLogger().info("Could not connect. Reason: " + in.readUTF());
                    Main.getInstance().getSocket().close();
                    Main.getInstance().getConnectionListenerThread().stop();
                    break;
                case 0x04: // System message
                    Bukkit.broadcastMessage("System message received: " + in.readUTF());
                    break;
                case 0x6:
                    String identifier = in.readUTF();
                    String player = in.readUTF();
                    String message = in.readUTF();
                    String finalMessage = Main.getInstance().getConfiguration().getString("ChatFormat.Global").replaceAll("&", "§")
                            .replaceAll("\\{server}", identifier)
                            .replaceAll("\\{player}", player)
                            .replaceAll("\\{message}", message);
                    for (User u : Main.getInstance().getUserList())
                        if (u.isGlobalChatEnabled()) u.getPlayer().sendMessage(finalMessage);
                    Bukkit.getConsoleSender().sendMessage("[Global Chat] " + finalMessage);

                    break;
                case 0x7:
                    String uuid = in.readUTF();
                    if (Bukkit.getPlayer(UUID.fromString(uuid)) != null) {
                        User u = Main.getInstance().getUser(UUID.fromString(uuid));
                        u.setGlobalChatEnabled(in.readBoolean());
                        u.setAccessStaffChat(in.readBoolean());
                        u.setAccessVeteranChat(in.readBoolean());
                        u.setChatChannel(ChatChannel.get(in.readUTF()));
                        u.setGlobalChatSendBanned(in.readBoolean());
                    }
                    break;
                case 0x9:
                    uuid = in.readUTF();
                    String target = in.readUTF();
                    if (Bukkit.getPlayer(target) != null) {
                        Main.getInstance().getPkConnector().sendData(0xa, new String[]{
                                uuid,
                                Bukkit.getPlayer(target).getName()
                        });


                    }
                    break;
                case 0xa:
                    uuid = in.readUTF();
                    target = in.readUTF();
                    String answer = in.readUTF();
                    String str;
                    if (answer.equals("")) {
                        Executors.newCachedThreadPool().execute(() -> {
                            UUID u = MojangAPI.getUUID(target);
                            if (u != null) {
                                MojangAPI.HypixelStatus hs = MojangAPI.getHypixelStatus(u);
                                if (hs != null) {
                                    if (hs.isOnline()) {
                                        String s = "§a" + MojangAPI.getFixedName(target) + " §6is online on §aHypixel: " + hs.getStatus() + "§6.";
                                        Bukkit.getPlayer(UUID.fromString(uuid)).sendMessage(s);
                                        return;
                                    }
                                }
                            }
                            Bukkit.getPlayer(UUID.fromString(uuid)).sendMessage("§cCouldn't find player '" + target + "'");


                        });


                    } else {
                        str = "§a" + target + " §6is online on §a" + answer + "§6.";
                        Bukkit.getPlayer(UUID.fromString(uuid)).sendMessage(str);
                    }

                    break;
                case 0xb:
                    String from = in.readUTF();
                    String to = in.readUTF();
                    String msg = in.readUTF();
                    //String server = in.readUTF();
                    if (Bukkit.getPlayer(to) != null) {
                        Main.getInstance().getPkConnector().sendData(0xc, new String[]{
                                from,
                                Bukkit.getPlayer(to).getName(),
                                msg
                        });
                        Main.getInstance().getUser(Bukkit.getPlayer(to)).setLastMessaged(from);
                    }

                    break;
                case 0xc:
                    from = in.readUTF();
                    to = in.readUTF();
                    msg = in.readUTF();
                    String server = in.readUTF();
                    String type = in.readUTF();
                    if (type.equals("FROM")) {
                        Bukkit.getPlayer(to).sendMessage(Main.getInstance().getConfiguration().getString("Message.From").replaceAll("&", "§")
                                .replace("{player}", from).replace("{message}", msg).replace("{server}", server));
                        Bukkit.getLogger().info(Main.getInstance().getConfiguration().getString("Message.From"));
                        Main.getInstance().getUser(Bukkit.getPlayer(to)).setLastMessaged(from);
                    } else if (type.equals("TO")) {
                        if (msg.equals("")) {
                            Bukkit.getPlayer(from).sendMessage("§cCouldn't find player '" + to + "'");
                        } else {
                            Bukkit.getPlayer(from).sendMessage(Main.getInstance().getConfiguration().getString("Message.To").replaceAll("&", "§")
                                    .replace("{player}", to).replace("{message}", msg).replace("{server}", server));
                        }
                        Main.getInstance().getUser(Bukkit.getPlayer(from)).setLastMessaged(to);

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
                    for (User u2 : Main.getInstance().getUserList())
                        if (u2.isAccessStaffChat()) u2.getPlayer().sendMessage(finalMessage);
                    break;
                case 0xe:
                    identifier = in.readUTF();
                    player = in.readUTF();
                    message = in.readUTF();
                    finalMessage = Main.getInstance().getConfiguration().getString("ChatFormat.Veteran").replaceAll("&", "§")
                            .replaceAll("\\{server}", identifier)
                            .replaceAll("\\{player}", player)
                            .replaceAll("\\{message}", message);
                    for (User u1 : Main.getInstance().getUserList())
                        if (u1.isAccessVeteranChat()) u1.getPlayer().sendMessage(finalMessage);
                    break;
                case 0xf:
                    String user = in.readUTF();
                    msg = in.readUTF();
                    Player p = null;
                    if (Bukkit.getPlayerExact(user) != null) p = Bukkit.getPlayerExact(user);
                    else {
                        try {
                            p = Bukkit.getPlayer(UUID.fromString(user));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }

                    if (p != null) p.sendMessage(msg.replace("&", "§"));
                    break;
                case 0x10:
                    msg = in.readUTF();
                    Bukkit.broadcastMessage(msg);
                    break;
                case 0x11:
                    msg = in.readUTF().replaceAll("&", "§");
                    String a = Main.getInstance().getServer().getClass().getPackage().getName();
                    String version = a.substring(a.lastIndexOf('.') + 1);
                    if (version.equals("v1_8_R3")) {
                        Main.getInstance().getWrapper().sendTitleToAll(msg);
                    }
                    break;

            }





        } catch (IOException ignored) {
        }
    }


}