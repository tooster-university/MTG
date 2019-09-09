package me.tooster.common;

// TODO: ChatMember interface
/**
 * Interface for objects that can behave like chat rooms
 * @param <MessengerT> type of personalities able to receive/send message
 */
public interface ChatRoom<MessengerT> {
    /**
     * Broadcast from the chat room itself to all players
     *  @param format message to send
     * @param args arguments to format
     */
    void broadcast(String format, Object... args);

    /**
     * Shout from messenger to all on chat room
     * @param from    messenger sending the message
     * @param format message to send
     * @param args arguments to format
     */
    void shout(MessengerT from, String format, Object... args);

}