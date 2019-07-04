package me.tooster;

import me.tooster.MTG.Card;
import me.tooster.MTG.Player;
import me.tooster.exceptions.CommandException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * This class manages the command parsing from user
 */
public class Parser {

    /**
     * Compiled command consists of Command enum and arguments as ArrayList&lt;String&gt;
     */
    public static class CompiledCommand {
        @Nullable
        private final Player player; // player who issued a command
        @NotNull
        private final Command command; // parsed command
        @NotNull
        private final ArrayList<String> args; // arguments as array of strings

        /**
         * Creates new compiled command with player as invoker.
         *
         * @param player  player invoking the command
         * @param command command enum representing the command
         * @param args    arguments for command
         */
        CompiledCommand(@Nullable Player player, @NotNull Command command, @NotNull ArrayList<String> args) {
            this.player = player;
            this.command = command;
            this.args = args;
        }

        /**
         * Server internal commands are created with this.
         *
         * @see Parser.CompiledCommand#CompiledCommand(Player, Command, ArrayList)
         */
        public CompiledCommand(@NotNull Command command, @NotNull ArrayList<String> args) {
            this(null, command, args);
        }

        /**
         * Internal command without arguments
         *
         * @see Parser.CompiledCommand#CompiledCommand(Command)
         */
        public CompiledCommand(@NotNull Command command) { this(command, new ArrayList<>());}

        /**
         * @return returns player for this command
         */
        @Nullable
        public Player getPlayer() { return player; }

        /**
         * @return command enum for this command
         */
        @NotNull
        public Command getCommand() { return command; }

        /**
         * @return collection of string arguments; can be empty but always not null
         */
        public List<String> getArgs() { return args; }

        /**
         * Returns i-th argument.
         *
         * @param idx index of argument counting from 0
         * @return i-th argument in argument's list or null if list is empty
         */
        @Nullable
        public String getArg(int idx) {return args.size() > 0 ? args.get(idx) : null; }

        /**
         * @throws CommandException if the command doesn't exist or has wrong arguments
         */
        public void test() throws CommandException {
            if (isInternal() ||
                    player.getEnabledCommands().contains(command) &&
                            command.predicate.test(player, args))
                ; // do nothing
            else throw new CommandException("Invalid command or arguments.");
        }

        /**
         * Converts command to readable format as <br>
         * <code>
         * player#0789 > DECK testdeck`
         * </code>
         */
        @Override
        public String toString() {
            return (isInternal() ? "INTERNAL" : this.player.getNick()) +
                    " > " + command.toString() + " " + args.toString();
        }

        /**
         * @return returns true if this is an internal command, not issued by a player
         */
        public boolean isInternal() { return player == null;}
    }

    /**
     * Parses user's input line into command readable by engine.
     * Only player's inputted commands are checked against predicate and availability.
     *
     * @param player player issuing the command
     * @param input  single command line
     * @return returns pair &lt;Command, args&gt; for parsed input or nil if input was empty
     * @throws CommandException if argument's don't fulfill predicate or command is not enabled right now
     */
    public static CompiledCommand parse(Player player, ArrayList<String> input) throws CommandException {
        if (input.size() == 0)
            return null;

        for (Command c : Command._cachedValues) { // reuse cached values of Command enum
            for (String alias : c.aliases) // check al aliases for matching alias
                if (input.get(0).toUpperCase().equals(alias)) {
                    input.remove(0);
                    return new CompiledCommand(player, c, input);
                }
        }

        throw new CommandException("No such command or command disabled right now.");
    }

    /**
     * Command enum represents a command with it's contracts that must evaluate successfully on their arguments, to
     * execute properly the command
     */
    public enum Command {

        // convention: first alias is the most descriptive, next is the shortest among others

        // generic - always enabled
        // ----------------------------------------------------

        HELP(new String[]{"HELP", "?"}),                            // returns list of enabled commands

        // prep phase
        // ----------------------------------------------------

        SELECT_DECK(new String[]{"SELECT", "S"},                    // selects a deck
                (player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0))),

        LIST_DECKS(new String[]{"DECKS", "L"}),                     // lists decks
        SHOW_DECK(new String[]{"DECK", "D", "SHOW"},     // displays the deck
                SELECT_DECK.predicate),

        READY(new String[]{"READY", "K", "OK"}),                    // marks player as ready in deck select gamePhase

        // internals
        // ----------------------------------------------------

        AUTO(),                                                     // command passed by engine in FSM auto mode
        END_GAME(),                                                 // thrown from GameStateFSM when game ends
        TIMEOUT(),                                                  // executed if a player timeouted

        // game phase
        // ----------------------------------------------------

        PASS_PRIORITY(new String[]{"PASS", " ", "P"}),
        DECLARE_ATTACK(new String[]{"ATTACK", "A", "ATK"}),
        DECLARE_DEFEND(new String[]{"DEFEND", "D", "DEF"}),
        TAP(new String[]{"TAP", "T"},
                (player, args) -> args.stream().allMatch(
                        ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
                                .getFlags().contains(Card.Flag.CAN_TAP))

        ),
        UNTAP(new String[]{"UNTAP", "U"},
                (player, args) -> args.stream().allMatch(
                        ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
                                .getFlags().contains(Card.Flag.CAN_UNTAP))
        ),
        DRAW(new String[]{"DRAW", "DR"}),
        MULLIGAN(new String[]{"MULLIGAN", "M"}),
        CAST(new String[]{"CAST", "C"},
                ((player, args) ->
                        player.canCast(args.get(1))));

        BiPredicate<Player, ArrayList<String>> predicate;
        String[] aliases;
        private static final Command[] _cachedValues = Command.values();


        Command(String[] aliases, BiPredicate<Player, ArrayList<String>> predicate) {
            this.aliases = aliases;
            assert aliases.length == 0 || aliases.length >= 2; // either null or alias with acronym
            this.predicate = predicate;
        }

        Command(String[] aliases) { this(aliases, (player, args) -> true); }

        Command() {this(new String[0]);} // for engine-only commands - player cannot cast cuz they don't have
        // alias

        public Command[] cachedValues() {return _cachedValues; }

    }

}
