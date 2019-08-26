package me.tooster.common;

/**
 * Interface for objects that can behave like chat rooms
 * @param <MessengerT> type of personalities able to receive/send message
 */
public interface ChatRoom<MessengerT> {
    /**
     * Broadcast from the chat room itself to all players
     *
     * @param message message to send
     */
    void broadcast(String message);

    /**
     * Shout from messenger to all on chat room
     *
     * @param from    messenger sending the message
     * @param message message to send
     */
    void shout(MessengerT from, String message);

}