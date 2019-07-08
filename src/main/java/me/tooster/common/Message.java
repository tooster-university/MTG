package me.tooster.common;

import java.io.Serializable;

public class Message implements Serializable {



    public enum Type {
        CONTROL,    // client -> server is ping, client <- server is pong, hidden from player
        REQUEST,    // client -> server is request, player <- server is prompt
        RESPONSE,   // response sent in reply to request
        BROADCAST,  // asymmetric broadcast. player -> server is shout, client <- server is broadcast
        SAY,        // chat message
        HINT,       // client <- server is tip, client -> server is option for example auto pass
        ERROR,      // client <- server is error, client -> server is ???
    }

    private final Type type;
    private final String content;
    private final String[] options;

    public Message(Type type, String content, String[] options) {
        this.type = type;
        this.content = content;
        this.options = options;
    }

}
