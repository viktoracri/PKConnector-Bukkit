package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.listener.ConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PKConnector {

    private UUID sessionID;



    private boolean disconnected = true;

    public Thread hostWatcherThread;
    public void connect(){
        String authentication_code = Main.getInstance().getConfig().getString("AuthenticationCode");

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
            out.writeShort(authentication_code.length()+2 + Main.getInstance().getDescription().getVersion().length() + 2);
            out.writeUTF(authentication_code);
            out.writeUTF(Main.getInstance().getDescription().getVersion());
            Bukkit.getConsoleSender().sendMessage("§a[PKConnector] Authenticating...");

            Main.getInstance().setConnectionListenerThread(new Thread(new ConnectionListener(), "Thread-ConnectionListener"));
            Main.getInstance().getConnectionListenerThread().start();
            if(hostWatcherThread != null) if(hostWatcherThread.isAlive())hostWatcherThread.stop();


        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Host is not running. Contact Viktoracri and run /pkconnector reconnect when host is available again.");
        }
    }

    public void startThread(){
        hostWatcherThread = new Thread(new HostWatcher(), "Thread-HostWatcher");
        hostWatcherThread.start();
    }

    public void disconnect(){
        try {
            Main.getInstance().getSocket().close();
        } catch (IOException | NullPointerException ignored) {
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
                if(obj instanceof String) length += (((String) obj).getBytes().length + 2);
                else if(obj instanceof Byte) length += 1;
                else if(obj instanceof Short) length += 2;
                else if(obj instanceof Integer) length += 4;
                else if(obj instanceof Long) length += 8;
                else if(obj instanceof Float) length += 4;
                else if(obj instanceof Double) length += 8;
                else if(obj instanceof Character) length += 2;
                else if(obj instanceof Boolean) length += 1;

            out.writeShort(length);
            out.writeUTF(sessionID.toString());
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
                Main.getInstance().getPkConnector().setDisconnected(true);
                //System.out.println("Nulled socket");
            }

        }
    }

    public class HostWatcher implements Runnable{

        @Override
        public void run() {

            while(Main.getInstance().isEnabled()) {
                try {

                    if(disconnected){
                        try {
                            Socket socket = new Socket("honeyfrost.net", 6006);
                            socket.close();
                            connect();
                        } catch (IOException e) {
                        }


                    }

                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }


    }

    public UUID getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = UUID.fromString(sessionID);
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
        if(disconnected && Main.getInstance().isEnabled()){
            Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Lost connection to host. Trying to reconnect..");
            startThread();
            Main.getInstance().getConnectionListenerThread().stop();
        }
    }
}
