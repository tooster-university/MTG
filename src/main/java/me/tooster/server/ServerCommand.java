package me.tooster.server;

import me.tooster.common.Command;

public enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"?", "help"}) HELP,                             // returns list of enabled commands
    @Alias({"ping"}) PING,                                  // player pings the server to check alive
    @Alias({"@"}) CONFIG,                                   // sets up a name for example
    @Alias(("dc")) DISCONNECT,                              // player peaceful disconnect request
    @Alias({"!", "shout"}) SHOUT,
    @Alias({".", "say", "whisper", "w"}) WHISPER,

    // content commands
    // -----------------------------------------------------
    @Alias({"upload"}) UPLOAD,                              // upload deck

    // prep phase
    // -----------------------------------------------------

    // fixme: "select" without parameters returns "null not imported"
    @Alias({"S", "select"}) SELECT_DECK
    //((player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0)))
    ,
    @Alias({"L", "decks"}) LIST_DECKS,
    @Alias({"D", "show"}) SHOW_DECK,
    @Alias({"K", "ok", "ready"}) READY,                    // marks player as ready in deck select gamePhase

    // internals
    // ----------------------------------------------------

    END_GAME,                                                 // thrown from GameStateFSM when game ends
    TIMEOUT,                                                  // executed if a player timeouted
    ;

    public static final ServerCommand[] cachedValues = ServerCommand.values();

    @Override
    public Command[] list() { return cachedValues; }

    public class Parsed extends Command.Parsed<ServerCommand> {
        private User user;

        public Parsed(ServerCommand cmd, String... args) { super(cmd, args); }

        /**
         * @return Returns stored user assigned to compiled command.
         */
        public User getUser() { return user; }

        /**
         * Assigns player to command
         *
         * @param user player issuing the command
         * @return Returns this compiled command but with setup player. Modifies original value.
         */
        public Parsed withUser(User user) {
            this.user = user;
            return this;
        }
    }
}
