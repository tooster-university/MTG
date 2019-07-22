package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
        String value() default "";
    }

    default boolean matches(String input) {
        return (this.getClass().isAnnotationPresent(Alias.class)) &&
                Arrays.stream(this.getClass().getAnnotation(Alias.class).value()).anyMatch(s -> s.equalsIgnoreCase(input));
    }

    /**
     * @return list of aliases for the command
     */
    default String[] aliases() {
        return this.getClass().isAnnotationPresent(Alias.class)
               ? this.getClass().getAnnotation(Alias.class).value()
               : new String[0];
    }

    /**
     * @return main (first) alias of command or empty string if it doesn't exist
     */
    default String mainAlias() {
        String[] aliases = aliases();
        return aliases.length > 0 ? aliases[0] : "";
    }

    /**
     * @return the acronym of command (second alias) if exists, otherwise returns empty string
     */
    default String acronym() {
        String[] aliases = aliases();
        return aliases.length > 1 ? aliases[1] : "";
    }

    /**
     * @return Returns help message for this command.
     */
    default String help() {
        return this.getClass().isAnnotationPresent(Help.class)
               ? this.getClass().getAnnotation(Help.class).value()
               : "";
    }

    class Controller<CMD extends Enum<CMD> & Command> {
        private final Class<CMD>   enumClass;
        public final  EnumSet<CMD> enabledCommands;
        public final  EnumSet<CMD> commandMask; // commandMask for enable/disable etc

        public Controller() { //(Class<CMD> enumClass) {
            enumClass = (Class<CMD>) ((CMD) new Object()).getClass();// FIXME: WTF may work hack to get enum class???
//        this.enumClass = enumClass;
            enabledCommands = EnumSet.noneOf(enumClass);
            commandMask = EnumSet.allOf(enumClass);

        }

        /**
         * Parses the input and returns compiled command
         *
         * @param input input string to parse
         * @return compiled command with command and string argument list if parse was a success, null otherwise
         */
        public Compiled<CMD> parse(@NotNull String input) {
            List<String> matchList    = new ArrayList<>();
            Pattern      regex        = Pattern.compile("[^\\s\"]+|\"([^\"]*)\""); // matches: (abcd) "(xy zv)" (x)
            Matcher      regexMatcher = regex.matcher(input);
            while (regexMatcher.find())
                matchList.add(regexMatcher.group(1) != null ? regexMatcher.group(1) : regexMatcher.group());

            String cname = matchList.remove(0); // command name
            for (Command c : enumClass.getEnumConstants()) {
                if (c.matches(cname))
                    return compile((CMD) c, (String[]) matchList.toArray()); // rest is argument's list
            }
            return null;
        }

        /**
         * Creates a compiled command object that has it's enum command and notnull string arguments list.
         *
         * @param command command to compile
         * @param args    command arguments
         * @return compiled command
         */
        public Compiled<CMD> compile(@NotNull CMD command, String... args) {
            return new Compiled<>(this, command, args);
        }

    }

    class Compiled<CMD> {
        @NotNull public final  CMD        cmd;
        @NotNull public final  String[]   args; // arguments as array of strings
        @NotNull private final Controller controller;

        /**
         * Creates new compiled command with final fields <code>command</code> and <code>args</code>. Those fields
         * provide easy access to the parsed command object,
         *
         * @param command command enum representing the command
         * @param args    arguments for command
         */
        Compiled(@NotNull Controller controller, @NotNull CMD command, String... args) {
            this.controller = controller;
            this.cmd = command;
            this.args = args == null ? new String[0] : args;
        }

        /**
         * @return true iff command is enabled in it's controller
         */
        public boolean isEnabled() { return controller.enabledCommands.contains(this.cmd); }

        /**
         * @return true iff command is in mask in it's controller
         */
        public boolean isMasked() {return controller.commandMask.contains(this.cmd);}

        /**
         * Converts command to readable format as <br>
         * <code>
         * player#0789 > DECK testDeck
         * </code>
         */
        @Override
        public String toString() {
            return cmd + " " + String.join(" ", args);
        }
    }

}
