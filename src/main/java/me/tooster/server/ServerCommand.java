package me.tooster.server;

import me.tooster.common.Command;

public enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"?", "HELP"}) HELP,                             // returns list of enabled commands
    @Alias({"PING"}) PING,                                  // player pings the server to check alive
    @Alias({"@", "CONFIG", "SETUP"}) CONFIG,                // sets up a name for example
    @Alias(("DC")) DISCONNECT,                              // player peaceful disconnect request

    // content commands
    // -----------------------------------------------------
    @Alias({"UPLOAD"}) UPLOAD,                              // upload deck

    // prep phase
    // -----------------------------------------------------

    // fixme: "select" without parameters returns "null not imported"
    @Alias({"S", "SELECT"}) SELECT_DECK
    //((player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0)))
    ,
    @Alias({"L", "DECKS"}) LIST_DECKS,
    @Alias({"D", "SHOW"}) SHOW_DECK,
    @Alias({"K", "OK", "READY"}) READY,                    // marks player as ready in deck select gamePhase

    // internals
    // ----------------------------------------------------

    END_GAME,                                                 // thrown from GameStateFSM when game ends
    TIMEOUT,                                                  // executed if a player timeouted

    ;

    private static final ServerCommand[] _cachedValues = ServerCommand.values();

    @Override
    public ServerCommand[] cachedValues() { return _cachedValues; }

    public class Compiled extends Command.Compiled<ServerCommand> {
        private Player player;

        public Compiled(ServerCommand cmd, String... args) { super(cmd, args); }

        public Player getPlayer() { return player; }

        /**
         * Adds info about player issuing the command to the compiled command
         *
         * @param player player issuing the command
         * @return Returns this compiled command but with setup player. Modifies original value.
         */
        public Compiled withPlayer(Player player) {
            this.player = player;
            return this;
        }
    }
}
