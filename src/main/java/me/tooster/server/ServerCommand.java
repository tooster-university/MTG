package me.tooster.server;

import me.tooster.common.Command;

public enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"?", "help"}) @Help("Displays server's help") HELP,                             // returns list of enabled commands
    @Alias({"ping"}) PING, PONG,                            // player pings the server to check alive, server replies PONG
    @Alias({"@"}) CONFIG,                                   // sets up a name for example
    @Alias(("dc")) DISCONNECT,                              // player peaceful disconnect request
    @Alias({"!", "shout"}) SHOUT,                           // hub chat
    @Alias({".", "say", "whisper", "w"}) WHISPER,           // personal chat

    ;

    public static final ServerCommand[] cachedValues = ServerCommand.values();
}
