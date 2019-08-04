package me.tooster.common;

import java.io.Serializable;

// TODO: replace with protoco buffers
public class Message implements Serializable {

    public enum Type {

        CONFIG,     // client -> server is ping, client <- server is pong, hidden from player
        REQUEST,    // client -> server is request, player <- server is prompt
        RESPONSE,   // response sent serverIn reply to request
        BROADCAST,  // asymmetric broadcast. player -> server is shout, client <- server is broadcast
        SAY,        // chat message
        HINT,       // client <- server is tip, client -> server is option for example auto pass
        ERROR,      // client <- server is error, client -> server is ???
        PING,
        PONG,
    }

    private final Type type;
    private final String[] content;

    public Message(Type type, String ... content) {
        this.type = type;
        this.content = content;
    }

}
