package dev.acri.pkconnector.bukkit.version.wrappers;


import dev.acri.pkconnector.bukkit.version.VersionWrapper;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class Wrapper1_8_R3 implements VersionWrapper {


    @Override
    public void sendTitleToAll(String msg) {
        IChatBaseComponent chatTitle = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + msg + "\",color:" + ChatColor.WHITE.name().toLowerCase() + "}");

        PacketPlayOutTitle title = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, chatTitle);
        PacketPlayOutTitle titleLength = new PacketPlayOutTitle(5, 100, 5);

        for(Player all : Bukkit.getOnlinePlayers()){
            ((CraftPlayer) all).getHandle().playerConnection.sendPacket(title);
            ((CraftPlayer) all).getHandle().playerConnection.sendPacket(titleLength);
        }
    }
}
