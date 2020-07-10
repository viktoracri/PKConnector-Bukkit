package dev.acri.pkconnector.bukkit;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class User {

    private Player player;
    private boolean globalChatEnabled = true;
    private String lastMessaged = "";
    private ChatChannel chatChannel = ChatChannel.NORMAL;
    private boolean accessStaffChat = false;
    private boolean accessVeteranChat = false;
    private boolean globalChatSendBanned = false;

    private boolean privateMessagesEnabled = true;

    private boolean nextMessageGlobalChat = false;
    private long lastMessageTime = -1;
    private String lastMessage = "";


    public User(Player player){
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void save(){

        List<Object> data = new ArrayList<>();
        data.add(player.getUniqueId().toString());
        data.add(player.getName());
        data.add(globalChatEnabled);
        data.add(chatChannel.name());

        Main.getInstance().getPkConnector().sendData(0x8, data);

    }

    public boolean isGlobalChatEnabled() {
        return globalChatEnabled;
    }

    public void setGlobalChatEnabled(boolean globalChatEnabled) {
        this.globalChatEnabled = globalChatEnabled;
    }

    public String getLastMessaged() {
        return lastMessaged;
    }

    public void setLastMessaged(String lastMessaged) {
        this.lastMessaged = lastMessaged;
    }


    public boolean isAccessStaffChat() {
        return accessStaffChat;
    }

    public void setAccessStaffChat(boolean accessStaffChat) {
        this.accessStaffChat = accessStaffChat;
    }

    public boolean isAccessVeteranChat() {
        return accessVeteranChat;
    }

    public void setAccessVeteranChat(boolean accessVeteranChat) {
        this.accessVeteranChat = accessVeteranChat;
    }

    public ChatChannel getChatChannel() {
        return chatChannel;
    }

    public void setChatChannel(ChatChannel chatChannel) {
        if(chatChannel == ChatChannel.STAFF && !isAccessStaffChat()) {
            this.chatChannel = ChatChannel.NORMAL;
            return;
        }
        else if(chatChannel == ChatChannel.VETERAN && !isAccessVeteranChat()) {
            this.chatChannel = ChatChannel.NORMAL;
            return;
        }
        this.chatChannel = chatChannel;
    }

    public boolean isGlobalChatSendBanned() {
        return globalChatSendBanned;
    }

    public void setGlobalChatSendBanned(boolean globalChatSendBanned) {
        this.globalChatSendBanned = globalChatSendBanned;
    }

    public boolean isNextMessageGlobalChat() {
        return nextMessageGlobalChat;
    }

    public void setNextMessageGlobalChat(boolean nextMessageGlobalChat) {
        this.nextMessageGlobalChat = nextMessageGlobalChat;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isPrivateMessagesEnabled() {
        return privateMessagesEnabled;
    }

    public void setPrivateMessagesEnabled(boolean privateMessagesEnabled) {
        this.privateMessagesEnabled = privateMessagesEnabled;
    }
}
