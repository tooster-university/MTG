package me.tooster.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface for commands with alias. Each command can be compiled with code in format
 * <pre>new ServerCommand.Compiled(ServerCommand.ECHO, data)</pre>
 * Command interface works best when used with enums enums. Compiled command
 */
public interface Command {

    /**
     * Used to alias the commands. Command without alias won't be matched.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Alias {
        String[] value() default {};
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Enable {
        boolean value() default false;
    }

    /**
     * Compiled command consists of Command enum and arguments as ArrayList&lt;String&gt;
     */
    class Compiled<CMD> {
        @NotNull
        public final CMD      command;
        @NotNull
        public final String[] args; // arguments as array of strings

        /**
         * Creates new compiled command with final fields <code>command</code> and <code>args</code>. Those fields
         * provide easy access to the parsed command object,
         *
         * @param command command enum representing the command
         * @param args    arguments for command
         */
        public Compiled(@NotNull CMD command, @Nullable String... args) {
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
            return command + " " + String.join(" ", args);
        }
    }

    /**
     * @return returns list of all defined commands.
     * FIXME #1: check if this successfully hides enum values() if overridden
     */
    Command[] values();


    /**
     * Parses the input and returns compiled command
     * @param clazz exact class of enum to parse
     * @param input input string to parse
     * @return compiled command with command and string argument list
     * @throws CommandException if command cannot parse aka doesn't match any or error occurs
     */
    static <CMD extends Command> Compiled _parse(Class<CMD> clazz, @NotNull String input) throws CommandException {
        List<String> matchList    = new ArrayList<>();
        Pattern      regex        = Pattern.compile("[^\\s\"]+|\"([^\"]*)\""); // matches: (abcd) "(xy zv)" (x)
        Matcher      regexMatcher = regex.matcher(input);
        while (regexMatcher.find())
            matchList.add(regexMatcher.group(1) != null ? regexMatcher.group(1) : regexMatcher.group());

        String cname = matchList.remove(0); // command name
        for (Command c : clazz.getEnumConstants()) {
            if (c.getClass().getAnnotation(Command.Alias.class) != null) // check for alias annotation
                for (String alias : c.getClass().getAnnotation(Command.Alias.class).value()) // check all aliases
                    // matching alias
                    if (cname.toUpperCase().equals(alias))  // first arg is command
                        return new Compiled<>(c, (String[]) matchList.toArray()); // rest is argument's list
        }
        throw new CommandException("No such command.");
    }
}
