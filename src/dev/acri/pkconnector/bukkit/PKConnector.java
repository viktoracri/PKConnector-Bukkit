package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.listener.ConnectionListener;
import org.apache.commons.lang.SerializationUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.org.ibex.nestedvm.util.Seekable;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PKConnector {

    public String authentication_code;

    public Thread hostWatcherThread;
    public void connect(){
        authentication_code = Main.getInstance().getConfig().getString("AuthenticationCode");

        try{UUID.fromString(authentication_code);
        }catch(IllegalArgumentException e){
            Main.getInstance().getServer().getConsoleSender().sendMessage(ChatColor.RED + "Authentication Code as specified in config.yml is invalid. Authentication aborted.");
            return;
        }

        try {
            Socket socket = new Socket("honeyfrost.net", 6006);
            socket.setKeepAlive(true);
            Main.getInstance().setSocket(socket);

            DataOutputStream out = new DataOutputStream(Main.getInstance().getSocket().getOutputStream());
            out.write(0x01);
            out.writeShort(authentication_code.length()+2 + 8);
            out.writeUTF(authentication_code);
            out.writeDouble(Double.parseDouble(Main.getInstance().getDescription().getVersion()));
            Bukkit.getConsoleSender().sendMessage("§a[PKConnector] Authenticating...");

            Main.getInstance().setConnectionListenerThread(new Thread(new ConnectionListener(), "Thread-ConnectionListener"));
            Main.getInstance().getConnectionListenerThread().start();



        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Host is not running");
        }
    }

    public void startThread(){
        hostWatcherThread = new Thread(new HostWatcher(), "Thread-HostWatcher");
        hostWatcherThread.start();
    }

    public void disconnect(){
        try {
            Main.getInstance().getSocket().close();
        } catch (IOException | NullPointerException e) {
        }
    }

    public void sendByte(int b){
        sendData(b, new String[0]);

    }

    public void sendData(int b, String[] data){
        sendData(b, Arrays.asList(data));

    }
    public void sendData(int b, List<Object> data){
        try {
            Socket socket = new Socket("honeyfrost.net", 6006);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            if(data == null) data = new ArrayList<>();

            out.writeByte(b);

            short length = 38;
            for(Object obj : data)
                if(obj instanceof String) length += (((String) obj).length() + 2);
                else if(obj instanceof Byte) length += 1;
                else if(obj instanceof Short) length += 2;
                else if(obj instanceof Integer) length += 4;
                else if(obj instanceof Long) length += 8;
                else if(obj instanceof Float) length += 4;
                else if(obj instanceof Double) length += 8;
                else if(obj instanceof Character) length += 2;
                else if(obj instanceof Boolean) length += 1;

            out.writeShort(length);
            out.writeUTF(authentication_code);
            for(Object obj : data)
                if(obj instanceof String) out.writeUTF((String)obj);
                else if(obj instanceof Byte) out.writeByte((Byte) obj);
                else if(obj instanceof Short) out.writeShort((Short) obj);
                else if(obj instanceof Integer) out.writeInt((Integer) obj);
                else if(obj instanceof Long) out.writeLong((Long) obj);
                else if(obj instanceof Float) out.writeFloat((Float) obj);
                else if(obj instanceof Double) out.writeDouble((Double) obj);
                else if(obj instanceof Character) out.writeChar((Character) obj);
                else if(obj instanceof Boolean) out.writeBoolean((Boolean)obj);


            out.flush();
            socket.close();

           // System.out.println("Sent byte 0x" + Integer.toHexString(b));

        } catch (IOException e) {
            if(e.getMessage().equals("Connection refused: connect")){
                Main.getInstance().setSocket(null);
                //System.out.println("Nulled socket");
            }

        }
    }

    public class HostWatcher implements Runnable{

        @Override
        public void run() {
            boolean prevDisconnected = false;
            boolean disconnected = false;

            while(Main.getInstance().isEnabled()) {
                try {
                    try {
                        new Socket("honeyfrost.net", 6006);

                        disconnected = false;
                    } catch (IOException e) {
                        disconnected = true;
                    }

                    if(disconnected && prevDisconnected){
                        Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Lost connection to host. Trying to reconnect in 2 seconds...");
                        Main.getInstance().getConnectionListenerThread().stop();
                        Thread.sleep(2000);
                    }

                    if(!disconnected && prevDisconnected){
                        connect();
                        break;
                    }



                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                prevDisconnected = disconnected;
            }

        }


    }
}
