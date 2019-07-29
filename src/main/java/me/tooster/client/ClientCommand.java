package me.tooster.client;

import me.tooster.common.Command;

public enum ClientCommand implements Command {

    @Alias("help") @Help("It, like, displays help... What did you think...") HELP,
    @Alias("connect") @Help("'connect IP Port' to connect to given server and 'connect' to connect to the last.") CONNECT,
    @Alias("disconnect") @Help("Disconnects from the server. Both internal and external.") DISCONNECT,
    @Alias({"nick", "name"}) @Help("'nick newnick' to change nick.") CHANGE_NAME,
    @Alias("shutdown") @Help("Shuts down the client...") SHUTDOWN;


    public static final ClientCommand[] cachedValues = ClientCommand.values();
}
