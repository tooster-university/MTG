package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * class to enable and disable commands
 *
 * @param <CMD> command type extending Enum implementing {@link me.tooster.common.Command.Compiled}
 */
public class CommandManager<CMD extends Enum<CMD> & Command> {

    private final Class<CMD>   enumClass;
    private final EnumSet<CMD> enabledCommands;
    /**
     * Mask for the command enable/disable/set functions. Only set fields are affected.
     */
    public final  EnumSet<CMD> commandMask;

    /**
     * Creates new command manger that manages the states of given commands
     * @param enumClass enum class
     */
    public CommandManager(Class<CMD> enumClass) {
        enumClass = (Class<CMD>) ((CMD) new Object()).getClass();
        this.enumClass = enumClass; // FIXME: WTF hackto get enum class
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

    public EnumSet<CMD> getEnabled() {return EnumSet.copyOf(enabledCommands); }

    public EnumSet<CMD> getDisabed() {return EnumSet.complementOf(enabledCommands); }

}
