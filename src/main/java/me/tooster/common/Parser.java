package me.tooster.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This class manages the command parsing with context type C and arguments type A[]
 */
public class Parser<CMD extends Enum<CMD> & Command> {

    private final Class<CMD> enumClass;
    private final EnumSet<CMD> enabledCommands;

    public Parser(Class<CMD> enumClass) {
        this.enumClass = enumClass;
        enabledCommands = EnumSet.noneOf(enumClass);
    }

    public void enableCommands(CMD... commands) { enabledCommands.addAll(Arrays.asList(commands)); }
    public void disableCommands(CMD... commands) {enabledCommands.removeAll(Arrays.asList(commands));}
    public void setCommands(CMD... commands) {enabledCommands.clear(); enableCommands(commands);}

    /**
     * Compiled command consists of Command enum and arguments as ArrayList&lt;String&gt;
     */
    public class CompiledCommand {
        @NotNull
        public final CMD command;
        @NotNull
        public final String[] args; // arguments as array of strings

        /**
         * Creates new compiled command with player as invoker.
         *
         * @param command command enum representing the command
         * @param args    arguments for command
         */
        CompiledCommand(@NotNull CMD command, @Nullable String... args) {
            this.command = command;
            this.args = args == null ? new String[0] : args;
        }

        /**
         * Converts command to readable format as <br>
         * <code>
         * player#0789 > DECK testdeck`
         * </code>
         */
        @Override
        public String toString() {
            return command + " " + Arrays.toString(args);
        }
    }

    /**
     * Parses input line into command object with argument list.
     * Only player's inputted commands are checked against predicate and availability.
     *
     * @param input input to parse
     * @return returns pair &lt;Command, args&gt; for parsed input or nil if input was empty
     * @throws CommandException if argument's don't fulfill predicate or command is not enabled right now
     */
    public final CompiledCommand parse(@NotNull String input) throws CommandException {
        List<String> parts = Arrays.asList(input.split("\\s+"));
        String cname = parts.get(0);
        parts.remove(0);

        for (CMD c : enumClass.getEnumConstants()) {
            if (c.getClass().getAnnotation(Command.Alias.class) != null)
                for (String alias : c.getClass().getAnnotation(Command.Alias.class).value()) // check all aliases
                    // matching alias
                    if (cname.toUpperCase().equals(alias))  // first arg is command
                        return new CompiledCommand(c, (String[]) parts.toArray());
        }

        throw new CommandException("No such command or command disabled right now.");
    }

}
