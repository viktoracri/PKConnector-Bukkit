package dev.acri.pkconnector.bukkit.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class GlobalChatReceivedEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private UUID uuid;
    private String username;
    private String serverIdentifier;
    private String message;

    private boolean cancelled = false;

    public GlobalChatReceivedEvent(UUID uuid, String username, String serverIdentifier, String message) {
        this.uuid = uuid;
        this.username = username;
        this.serverIdentifier = serverIdentifier;
        this.message = message;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getServerIdentifier() {
        return serverIdentifier;
    }

    public String getMessage() {
        return message;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
