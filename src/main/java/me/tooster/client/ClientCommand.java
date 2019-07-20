package me.tooster.client;

import me.tooster.common.Command;

public enum ClientCommand implements Command {

    @Alias("help") HELP, // displays help for client program
    @Alias("connect") CONNECT, // connects to server - with no arguments, connects do previous server
    @Alias({"nick", "name"}) CHANGE_NAME, // changes nick
    @Alias("shutdown") SHUTDOWN; // shuts down the client gracefully


    public static final ClientCommand[] cachedValues = ClientCommand.values();
}
