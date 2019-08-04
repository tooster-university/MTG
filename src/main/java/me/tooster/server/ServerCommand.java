package me.tooster.server;

import me.tooster.common.Command;

public enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"?", "help"}) HELP,                             // returns list of enabled commands
    @Alias({"ping"}) PING, PONG,                            // player pings the server to check alive, server replies PONG
    @Alias({"@"}) CONFIG,                                   // sets up a name for example
    @Alias(("dc")) DISCONNECT,                              // player peaceful disconnect request
    @Alias({"!", "shout"}) SHOUT,                           // hub chat
    @Alias({".", "say", "whisper", "w"}) WHISPER,           // personal chat

    // prep phase
    // -----------------------------------------------------

    // fixme: "select" without parameters returns "null not imported"
    @Alias({"S", "select"}) SELECT_DECK
    //((player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0)))
    ,
    @Alias({"L", "decks"}) LIST_DECKS,
    @Alias({"D", "show"}) SHOW_DECK,
    @Alias({"K", "ok", "ready"}) READY,                    // marks player as ready serverIn deck select gamePhase

    // internals
    // ----------------------------------------------------

    END_GAME,                                                 // thrown from GameStateFSM when game ends
    TIMEOUT,                                                  // executed if a player timeouted
    ;

    public static final ServerCommand[] cachedValues = ServerCommand.values();
}
