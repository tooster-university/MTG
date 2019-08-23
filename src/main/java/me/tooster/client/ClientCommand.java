package me.tooster.client;

import me.tooster.common.Command;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

enum ClientCommand implements Command {

    @Alias({"/?", "/help"}) @Help("It, like, displays help... What did you think...") HELP,
    @Alias("/connect") @Help("'connect IP Port' to connect to given server and 'connect' to connect to the last.") CONNECT,
    @Alias("/disconnect") @Help("Disconnects from the server. Both internal and external.") DISCONNECT,
    @Alias("/config") @Help("Changes player config. Use as `/config key value'.") CONFIG,
    @Alias("/shutdown") @Help("Shuts down the client...") SHUTDOWN,

    SERVER_HELLO, CONNECTION_CLOSE,

    ;

    public static final EnumSet<ClientCommand> commands = EnumSet.allOf(ClientCommand.class);
}
