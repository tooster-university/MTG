package me.tooster.common;

import me.tooster.MTG.Mana;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Interface for commands with alias. Each command can be compiled with code in format
 * <pre>new ServerCommand.Compiled(ServerCommand.ECHO, data)</pre>
 * Command interface works best when used with enums.
 * All non-internal commands(without alias) should have help annotation
 */
public interface Command {

    // TODO: automatically generate fields from annotations using annotation processor

    // non internal commands should always have help

    /**
     * Used to alias the commands. Command without alias won't be matched. Main alias is the first one
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Alias {
        String[] value() default {};
    }

    /**
     * Returns help for the command
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Help {
        String value();
    }

    /**
     * Returns given annotation for enum or fails with null and prints stack trace
     *
     * @param annotationClass class of annotation to get
     * @return annotation or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    private @Nullable Annotation getAnnotation(Class annotationClass) {
        try {
            return this.getClass().getDeclaredField(((Enum) this).name()).getAnnotation(annotationClass); // null alias
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        } // this should never trigger
    }

    /**
     * @return Returns true if command has alias
     */
    default boolean hasAlias() { return this.getAnnotation(Alias.class) != null; }

    /**
     * @return list of aliases for the command
     */
    default String[] aliases() {
        var aliasAnnotation = (Alias) this.getAnnotation(Alias.class);
        return aliasAnnotation != null ? aliasAnnotation.value() : new String[0];
    }

    /**
     * @return main (first) alias of command or empty string if it doesn't exist
     */
    default @NotNull String mainAlias() {
        String[] aliases = aliases();
        return aliases.length > 0 ? aliases[0] : "";
    }

    /**
     * Checks if input matches on prefix (ignoring case and whitespaces) any alias of this command
     * For example @Alias("/test alias") will match against "/test alias" " /TeSt   AlIaS some other arguments"
     * but not "/test" nor "/test aliases"
     *
     * @param input input string to match against
     * @return true if input equals (ignoring case) to any of the aliases
     */
    default boolean matches(String input) {
        String[] inputParts = Formatter.splitParts(input).toArray(new String[0]);
        for (var alias : aliases()) {
            var aliasParts = Formatter.splitParts(alias).toArray(new String[0]);
            boolean match = aliasParts.length <= inputParts.length;
            // zip two parts
            for (int i = 0; i < aliasParts.length && match; i++) match = aliasParts[i].equalsIgnoreCase(inputParts[i]);

            if (match) return true;
        }
        return false;
    }

    /**
     * @return Returns help message for this command defined in @Help annotation or detais message if @Help not found on command
     */
    default @NotNull String help() {
        var helpAnnotation = (Help) this.getAnnotation(Help.class);
        var aliases = aliases();
        return helpAnnotation == null ? this + "> has no help message."
                                      : aliases.length > 0
                                        ? String.format("%-30s\t-> %s", String.join(", ", aliases),
                                              helpAnnotation.value())
                                        : String.format("%-30s\t-> %s", ((Enum) this).name() + ">",
                                                helpAnnotation.value());
    }

    /**
     * @return Returns true if command has help message. Safe for null arguments
     */
    default boolean hasHelp() { return this.getAnnotation(Help.class) != null; }

    /**
     * Returns help for command.
     *
     * @param cmd command to get help
     * @return Returns message containing help info about command or error message if command is invalid
     */
    static @NotNull String help(Command cmd) {
        if (cmd == null) return "Invalid command - cannot find help.";
        else return cmd.help();
    }

    class Controller<CMD extends Enum<CMD> & Command> {
        public final Class<CMD>   commandEnumClass;
        public final Object       owner;
        public final EnumSet<CMD> enabledCommands; // by default all  disabled
        public final EnumSet<CMD> commandMask; // commandMask for enable/disable etc. mask is clear at start

        /**
         * Creates a controller for managing commands.
         *
         * @param commandClass class of command that controller controls
         */
        public Controller(Class<CMD> commandClass, Object owner) { //(Class<CMD> enumClass) {
            commandEnumClass = commandClass; //(Class<CMD>) ((CMD) new Object()).getClass();// FIXME: WTF may work hack to get
            // enum
            this.owner = owner;
            enabledCommands = EnumSet.noneOf(commandEnumClass);
            commandMask = EnumSet.noneOf(commandEnumClass);
        }

        /**
         * Sets the enabled commands anew with respect to the mask.
         *
         * @param commands commands to set as enabled, rest will be disabled
         */
        public void setEnabled(CMD... commands) {
            enabledCommands.retainAll(commandMask);
            EnumSet<CMD> cmdSwitch = EnumSet.copyOf(Arrays.asList(commands));
            cmdSwitch.removeAll(commandMask);
            enabledCommands.addAll(cmdSwitch);
        }

        /**
         * Enables commands with respect to the mask.
         *
         * @param commands commands to enable
         */
        public void enable(CMD... commands) {
            EnumSet<CMD> cmdSwitch = EnumSet.copyOf(Arrays.asList(commands));
            cmdSwitch.removeAll(commandMask);
            enabledCommands.addAll(cmdSwitch);
        }

        /**
         * Disables commands with respect to the mask.
         *
         * @param commands commands to disable
         */
        public void disable(CMD... commands) {
            EnumSet<CMD> cmdSwitch = EnumSet.copyOf(Arrays.asList(commands));
            cmdSwitch.removeAll(commandMask);
            enabledCommands.removeAll(cmdSwitch);
        }

        /**
         * @param command command to check
         * @return returns true if command is enabled
         */
        public boolean isEnabled(CMD command) { return enabledCommands.contains(command); }

        /**
         * Sets the mask anew. Masked command is not affected by enable/disable/setEnabled commands.
         *
         * @param commands commands to set in the mask, rest will be unset
         */
        public void setMasked(CMD... commands) {
            commandMask.clear();
            commandMask.addAll(Arrays.asList(commands));
        }

        /**
         * Adds command to mask. Masked command is not affected by enable/disable/setEnabled commands.
         *
         * @param commands command to add to the mask
         */
        public void mask(CMD... commands) { commandMask.addAll(Arrays.asList(commands)); }

        /**
         * Removes command from the mask. Masked command is not affected by enable/disable/setEnabled commands.
         *
         * @param commands command to remove from the mask
         */
        public void unmask(CMD... commands) { commandMask.removeAll(Arrays.asList(commands)); }

        /**
         * Checks if command is in mask. Masked command is not affected by enable/disable/setEnabled commands.
         *
         * @param command command to check
         * @return true iff command is set in mask
         */
        public boolean isMasked(CMD command) { return commandMask.contains(command);}

        //TODO: add 'rawInput' field to Compiled

        /**
         * Parses the input and returns compiled command. If command didn't parse, then parsed command is null and
         * arg0 is raw input string. If parsed, args contain raw parts of command. Parsing is done as described in
         * {@link me.tooster.common.Command#matches(String)}.
         *
         * @param input input string to parse
         * @return compiled command with command and string argument list if parse was a success,
         * null and raw input as first argument otherwise
         */
        public @NotNull Compiled<CMD> parse(@NotNull String input) {
            for (CMD c : commandEnumClass.getEnumConstants())
                if (c.matches(input))
                    return compile(c, Formatter.splitParts(input).toArray(new String[0])); // argument list is command split onto parts
            return compile(null, input); // not parsed is null with raw input as arg0
        }

        /**
         * Creates a compiled command object that has it's enum command and notnull string arguments list.
         *
         * @param command command to compile
         * @param args    command arguments
         * @return compiled command
         */
        public Compiled<CMD> compile(@Nullable CMD command, String... args) {
            return new Compiled<>(this, command, args);
        }

    }

    /**
     * Compiled command object.
     *
     * @param <CMD> command type
     */
    class Compiled<CMD extends Enum<CMD> & Command> {
        @NotNull public final  Controller<CMD> controller;  // controller that parsed this command
        @Nullable public final CMD             cmd;         // command for parse success, null otherwise
        @NotNull public final  String[]        args;        // arguments as array of strings. arg 0 is command itself so
        // `/test a b' argument list is {"/test", "a", "b"}

        /**
         * Creates new compiled command with final fields <code>command</code> and <code>args</code>. Those fields
         * provide easy access to the parsed command object,
         *
         * @param controller controller that compiled this command
         * @param command    command enum representing the command, never null
         * @param args       arguments for command, arg0 should be command itself. Always a not null array.
         *                   Always a fresh copy. If args are not specified, then arg0 is defaulted to main alias.
         *                   If main alias doesn't exist or command is null, a String[0] array is args
         */
        Compiled(@NotNull Controller<CMD> controller, @Nullable CMD command, String... args) {
            this.controller = controller;
            this.cmd = command;
            this.args = args == null ?
                        command == null || command.mainAlias().isBlank() ?
                        new String[0] :
                        new String[]{command.mainAlias()} :
                        args.clone();
        }

        /**
         * @return true iff command is enabled in it's controller
         */
        public boolean isEnabled() { return controller.isEnabled(this.cmd); }

        /**
         * @return true iff command is in mask in it's controller
         */
        public boolean isMasked() {return controller.isMasked(this.cmd);}


        /**
         * @param idx index of argument starting at 0
         * @return argument if specified or empty string "" if doesn't exist
         */
        public String arg(int idx) {
            return idx >= 0 ? idx < args.length ? args[idx] : "" : "";
        }

        /**
         * Converts command to readable format as <br>
         * <code>
         * player#0789 > DECK testDeck
         * </code>
         */
        @Override
        public String toString() {
            return cmd + "> " + String.join(" ", args);
        }
    }

}
