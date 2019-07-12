package me.tooster.server;

import me.tooster.common.Command;
import me.tooster.common.CommandException;
import org.jetbrains.annotations.NotNull;

public enum ServerCommand implements Command {

    // generic - always enabled
    // -----------------------------------------------------

    @Alias({"?", "HELP"}) HELP,                             // returns list of enabled commands
    @Alias({"PING"}) PING,                                  // player pings the server to check alive
    @Alias({"@"}) CONFIG,                                   // sets up a name for example
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

    // -----------------------------------------------------
    // COPY PASTE TEMPLATE
    // -----------------------------------------------------
    public static final ServerCommand[] cachedValues = ServerCommand.values();

    public class Compiled extends Command.Compiled<ServerCommand> {
        private User player;

        public Compiled(ServerCommand cmd, String... args) { super(cmd, args); }

        public User getPlayer() { return player; }

        /**
         * Adds info about player issuing the command to the compiled command
         *
         * @param player player issuing the command
         * @return Returns this compiled command but with setup player. Modifies original value.
         */
        public Compiled withPlayer(User player) {
            this.player = player;
            return this;
        }
    }

    public static Compiled parse(@NotNull String input) throws CommandException {
        return (Compiled) Command._parse(ServerCommand.class, input);
    }
}
