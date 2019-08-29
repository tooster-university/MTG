package me.tooster.server;


import me.tooster.MTG.MTGCommand;
import me.tooster.MTG.MTGStateMachine;
import me.tooster.common.ChatRoom;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages;

import java.util.*;

import static me.tooster.MTG.MTGCommand.*;


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
        broadcast(String.format("%s joined the hub. %s", user, Formatter.formatProgress(users.size(), userSlots)));
        synchronized (fsm) {
            if (fsm.getCurrentState() == MTGStateMachine.State.GAME_PREPARE) {
                user.setReady(false);
                user.mtgCommandController.enable(READY);
                user.transmit(Messages.VisualMsg.newBuilder()
                        .setFrom("HUB")
                        .setTo(user.toString())
                        .setMsg("When you are ready, type '" + READY.mainAlias() + "'."));
            } else user.transmit(Messages.VisualMsg.newBuilder()
                    .setVariant(Messages.VisualMsg.Variant.INVALID)
                    .setMsg("Wait for the game to start"));
        }
        return true;
    }

    synchronized void removeUser(User user) {
        users.remove(user.serverTag);
        user.hub = null;
        user.setReady(false);
        user.mtgCommandController.disable(READY);
        broadcast(String.format("%s left the hub. %s", user, Formatter.formatProgress(users.size(), userSlots)));
        // TODO: forfeit
    }

    @Override
    public void broadcast(String message) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(Messages.VisualMsg.newBuilder().setFrom("HUB").setTo("HUB").setMsg(message)));
        }
    }

    @Override
    public void shout(User user, String message) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(Messages.VisualMsg.newBuilder().setFrom(user.toString()).setTo("HUB").setMsg(message)));
        }
    }
}
