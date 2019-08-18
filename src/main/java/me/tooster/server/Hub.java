package me.tooster.server;


import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.tooster.server.ServerCommand.*;


/**
 * Hub manages connected users and
 */
public class Hub {

    public final HubStateMachine hubFSM = new HubStateMachine();

    public final Map<Long, User> users     = Collections.synchronizedMap(new HashMap<>(2)); // users connected to session
    public       Integer         userSlots = 2;


    //------------------------------------------------------------------------------------------------------------------

    /**
     * Adds user to Hub.
     * Sets up user's hub reference and his enabled commands.
     * Broadcasts info about joining to others.
     * Sends welcome message to user
     *
     * @param user user to add
     */
    void addUser(User user) {
        assert (!users.containsKey(user.serverTag));
        users.put(user.serverTag, user);
        user.hub = this;
        broadcast(String.format("User %s joined the hub. %s", user, Formatter.formatProgress(users.size(), userSlots)));

        hubFSM.process(user.serverCommandController.compile(HUB_ADD_USER), this);
    }

    void removeUser(User user) {
        assert (users.containsValue(user));
        users.remove(user.serverTag);
        user.hub = null;
        user.setReady(false);
        broadcast(String.format("User %s left the hub. %s", user, Formatter.formatProgress(users.size(), userSlots)));

        hubFSM.process(user.serverCommandController.compile(HUB_REMOVE_USER), this);
    }

    /**
     * Sends message to all users
     *
     * @param msg message to send
     */
    void broadcast(String msg) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(Messages.ChatMsg.newBuilder().setFrom("HUB").setMsg(msg)));
        }
    }
}
