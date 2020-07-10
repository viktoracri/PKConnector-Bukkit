package dev.acri.pkconnector.bukkit;

import dev.acri.pkconnector.bukkit.listener.ConnectionListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.crypto.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PKConnector {

    private PrivateKey privateKey;
    private PublicKey publicKey;



    private boolean disconnected = true;

    public Thread hostWatcherThread;
    public void connect(){

        File authenticationFile = new File(Main.getInstance().getDataFolder() + File.separator + "authentication.yml");
        if(!authenticationFile.exists()){
            Main.getInstance().getServer().getConsoleSender().sendMessage("§cNo authentication file provided. Authentication aborted.");
            return;
        }
        FileConfiguration authConfig = YamlConfiguration.loadConfiguration(authenticationFile);

        String username = authConfig.getString("username");
        String password = authConfig.getString("password");

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(authConfig.getString("private"))
            ));
            publicKey = kf.generatePublic(new X509EncodedKeySpec(
                    Base64.getDecoder().decode(authConfig.getString("public"))
            ));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }


        try {
            Socket socket = new Socket("honeyfrost.net", 6006);
            Main.getInstance().setSocket(socket);

            DataOutputStream out = new DataOutputStream(Main.getInstance().getSocket().getOutputStream());



            out.write(0x01);
            out.writeShort(username.length()+2 + 256+ Main.getInstance().getDescription().getVersion().length() + 2);
            out.writeUTF(Main.version);
            out.writeUTF(username);
            out.write(encrypt(password.getBytes(), publicKey));

            // todo Is it out.flush?

            Bukkit.getConsoleSender().sendMessage("§a[PKConnector] Authenticating...");

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Main.getInstance().setConnectionListenerThread(new Thread(new ConnectionListener(), "Thread-ConnectionListener"));
                Main.getInstance().getConnectionListenerThread().start();
            }, 5);

            if(hostWatcherThread != null) if(hostWatcherThread.isAlive())hostWatcherThread.stop();


        } catch (IOException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            Bukkit.getConsoleSender().sendMessage("§c[PKConnector] Host is not running. Contact Viktoracri and run /pkconnector reconnect when host is available again.");
        }
    }

    public void startThread(){
        try {
            Class.forName("dev.acri.pkconnector.bukkit.PKConnector$HostWatcher");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        hostWatcherThread = new Thread(new HostWatcher(), "Thread-HostWatcher");
        hostWatcherThread.start();
    }

    public void disconnect(){
        try {
            Main.getInstance().getSocket().close();
        } catch (IOException | NullPointerException ignored) {
        }
    }

    public void sendData(int b, Object[] data){
        sendData(b, Arrays.asList(data));

    }
    public void sendData(int b, List<Object> dataInput){
        final List<Object> finalDataInput = dataInput;
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            List<Object> data = finalDataInput;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);
                if (data == null) data = new ArrayList<>();

                out.writeByte(b);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream infoOut = new DataOutputStream(bos);

                for (Object obj : data) {
                    if (obj instanceof String) infoOut.writeUTF((String) obj);
                    else if (obj instanceof Byte) infoOut.writeByte((Byte) obj);
                    else if (obj instanceof Short) infoOut.writeShort((Short) obj);
                    else if (obj instanceof Integer) infoOut.writeInt((Integer) obj);
                    else if (obj instanceof Long) infoOut.writeLong((Long) obj);
                    else if (obj instanceof Float) infoOut.writeFloat((Float) obj);
                    else if (obj instanceof Double) infoOut.writeDouble((Double) obj);
                    else if (obj instanceof Character) infoOut.writeChar((Character) obj);
                    else if (obj instanceof Boolean) infoOut.writeBoolean((Boolean) obj);
                }

                infoOut.flush();

                KeyGenerator generator = KeyGenerator.getInstance("AES");
                generator.init(128); // The AES key size in number of bits
                SecretKey secKey = generator.generateKey();

                Cipher aesCipher = Cipher.getInstance("AES");
                aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
                byte[] information = aesCipher.doFinal(bos.toByteArray());
                byte[] encryptedKey = encrypt(secKey.getEncoded(), publicKey);
                out.writeShort(encryptedKey.length + information.length + 2);
                out.write(encryptedKey);
                out.writeShort(information.length);
                out.write(information);

                out.flush();

                Socket socket = new Socket("honeyfrost.net", 6006);
                DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());

                socketOut.write(baos.toByteArray());

                socketOut.flush();
                socket.close();

//            System.out.println("Sent byte: 0x" + Integer.toHexString(b));
//            System.out.println("Length: " + information.length + ", dataSize: " + data.size());



            } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException | UnknownHostException e) {
                System.out.println("Failed to send data: " + e.getMessage());
            } catch (IOException e) {
                if (e.getMessage().equals("Connection refused: connect")) {
                    Main.getInstance().setSocket(null);
                    Main.getInstance().getPkConnector().setDisconnected(true);
                }
            }
        });
        ex.shutdown();
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

                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }


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


    public byte[] encrypt(byte[] data, PublicKey publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public byte[] decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
