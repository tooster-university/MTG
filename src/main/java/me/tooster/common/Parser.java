package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
     *
     * @param commands
     */
    public void disableCommands(CMD... commands) {
        EnumSet<CMD> disable = EnumSet.copyOf(Arrays.asList(commands));
        disable.retainAll(commandMask);
        enabledCommands.removeAll(disable);
    }

    /**
     * Sets up the commands aka disables all and enables specified according to args and commandMask.
     *
     * @param commands
     */
    public void setCommands(CMD... commands) {
        EnumSet<CMD> disable = EnumSet.allOf(enumClass);
        disable.retainAll(commandMask);
        enabledCommands.removeAll(disable);
        enableCommands(commands);
    }

    /**
     * Returns tru if command is enabled. Doesn't use mask.
     *
     * @param command command to check
     * @return true iff command is enabled
     */
    public boolean isEnabled(CMD command) { return enabledCommands.contains(command); }

}
