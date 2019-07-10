package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Class managing the parsing of a command. It has a list of enabled commands
 *
 * @param <CMD> command type extending Enum implementing {@link me.tooster.common.Command.Compiled}
 */
public class Parser<CMD extends Enum<CMD> & Command> {

    private final Class<CMD>   enumClass;
    private final EnumSet<CMD> enabledCommands;
    /**
     * Mask for the command enable/disable/set functions. Only set fields are affected.
     */
    public final  EnumSet<CMD> commandMask;

    public Parser(Class<CMD> enumClass) {
        this.enumClass = enumClass;
        enabledCommands = EnumSet.noneOf(enumClass);
        commandMask = EnumSet.allOf(enumClass);
    }

    /**
     * Enables commands according to arguments and commandMask.
     *
     * @param commands commands to enable
     */
    public void enableCommands(CMD... commands) {
        EnumSet<CMD> enable = EnumSet.copyOf(Arrays.asList(commands));
        enable.retainAll(commandMask);
        enabledCommands.addAll(enable);
    }

    /**
     * Disables commands according to arguments and commandMask.
     * @param commands
     */
    public void disableCommands(CMD... commands) {
        EnumSet<CMD> disable = EnumSet.copyOf(Arrays.asList(commands));
        disable.retainAll(commandMask);
        enabledCommands.removeAll(disable);
    }

    /**
     * Sets up the commands aka disables all and enables specified according to args and commandMask.
     * @param commands
     */
    public void setCommands(CMD... commands) {
        EnumSet<CMD> disable = EnumSet.allOf(enumClass);
        disable.retainAll(commandMask);
        enabledCommands.removeAll(disable);
        enableCommands(commands);
    }


    /**
     * Parses input line into command object with argument list.
     * Only player's inputted commands are checked against predicate and availability.
     *
     * @param input input to parse
     * @return returns pair &lt;Command, args&gt; for parsed input or nil if input was empty
     * @throws CommandException if argument's don't fulfill predicate or command is not enabled right now
     */
    public final CMD.Compiled parse(@NotNull String input) throws CommandException {
        List<String> parts = Arrays.asList(input.split("\\s+"));
        String cname = parts.get(0);
        parts.remove(0);

        for (CMD c : enumClass.getEnumConstants()) {
            if (enabledCommands.contains(c)) {
                if (c.getClass().getAnnotation(Command.Alias.class) != null)
                    for (String alias : c.getClass().getAnnotation(Command.Alias.class).value()) // check all aliases
                        // matching alias
                        if (cname.toUpperCase().equals(alias))  // first arg is command
                            return new CMD.Compiled<>(c, (String[]) parts.toArray());
            } else throw new CommandException("Command disabled.");
        }
        throw new CommandException("Cannot parse the command.");
    }

}
