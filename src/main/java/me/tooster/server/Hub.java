package me.tooster.server;


import me.tooster.MTG.MTGStateMachine;
import me.tooster.common.ChatRoom;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages;

import java.util.*;


/**
 * Hub manages connected users and
 */
public class Hub implements ChatRoom<User> {

    public final Map<Long, User> users;// users connected to session
    public       Integer         userSlots;
    public       MTGStateMachine fsm;

    private Hub() {
        users = Collections.synchronizedMap(new HashMap<>(2));
        userSlots = 2;
    }

    /**
     * Static factory method to make new hubs
     *
     * @param slots
     * @return
     */
    public static Hub makeHub(int slots) {
        Hub hub = new Hub();
        hub.userSlots = slots;
        hub.fsm = new MTGStateMachine(hub, slots);
        hub.fsm.start();
        return hub;
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Adds user to Hub.
     * Sets up user's hub reference and his enabled commands.
     * Broadcasts info about joining to others.
     * Sends welcome message to user
     *
     * @param user user to add
     * @return returns true if player was added to the hub and false otherwise
     */
    synchronized boolean addUser(User user) {
        if (users.size() == userSlots || users.containsKey(user.serverTag)) return false;
        users.put(user.serverTag, user);
        user.hub = this;
        broadcast("%s joined the hub. %s", user.toString(), Formatter.formatProgress(users.size(), userSlots));
        fsm.tryAddUser(user); // TODO: add to waiting queue if tryAddUser was a fail
        return true;
    }

    synchronized void removeUser(User user) {
        users.remove(user.serverTag);
        user.hub = null;
        fsm.removeUser(user);
        broadcast("%s left the hub. %s", user.toString(), Formatter.formatProgress(users.size(), userSlots));
    }

    @Override
    public void broadcast(String format, Object... args) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(Messages.VisualMsg.newBuilder()
                    .setFrom("HUB")
                    .setTo("HUB")
                    .setMsg(String.format(format, (Object[]) args))));
        }
    }

    @Override
    public void shout(User user, String format, Object... args) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(Messages.VisualMsg.newBuilder()
                    .setFrom(user.toString())
                    .setTo("HUB")
                    .setMsg(String.format(format, (Object[]) args))));
        }
    }
}
