package me.tooster.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.stream.Stream;

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

    default boolean matches(String input) {
        return (this.getClass().isAnnotationPresent(Alias.class)) &&
                Arrays.stream(this.getClass().getAnnotation(Alias.class).value()).anyMatch(s -> s.equalsIgnoreCase(input));
    }

    default String[] aliases() {
        return this.getClass().isAnnotationPresent(Alias.class)
               ? this.getClass().getAnnotation(Alias.class).value()
               : new String[0];
    }

    class Controller<CMD extends Enum<CMD> & Command> {
        private final Class<CMD>   enumClass;
        private final EnumSet<CMD> enabledCommands;
        public final  EnumSet<CMD> mask; // mask for enable/disable etc

        enum Status {ENABLED, DISABLED, MASKED, UNMASKED}

        public Controller() { //(Class<CMD> enumClass) {
            enumClass = (Class<CMD>) ((CMD) new Object()).getClass();// FIXME: WTF hack to get enum class
//        this.enumClass = enumClass;
            enabledCommands = EnumSet.noneOf(enumClass);
            mask = EnumSet.allOf(enumClass);

        }


        /**
         * Sets specified commands as enabled and rest as disabled. Affected by mask.
         *
         * @see #set(Status, Enum[])
         */
        public void set(CMD... commands) { set(null, commands);}

        /**
         * Controls the enabled/disabled status of commands for this controller.
         *
         * @param status   If status is specified, change the state of specified commands to
         *                 'status' leaving others intact. If null - disable unspecified and enable specified.
         *                 Affected by mask
         * @param commands Commands to set.
         */
        public void set(Status status, CMD... commands) {
            EnumSet<CMD> switchMask = commands.length == 0 ? EnumSet.allOf(enumClass)
                                                           : EnumSet.copyOf(Arrays.asList(commands));
            if (status == null) { // for null status, set enabled to 'commands' and disable the rest (intersect mask)
                enabledCommands.removeAll(mask);
                status = Status.ENABLED;
            }

            switchMask.retainAll(mask); // affect only masked types
            switch (status) {
                case ENABLED: enabledCommands.addAll(switchMask);
                    break;
                case DISABLED: enabledCommands.removeAll(switchMask);
                    break;
            }
        }

        public EnumSet<CMD> get(@NotNull Status status) {
            return status == Status.ENABLED
                   ? EnumSet.copyOf(enabledCommands)
                   : EnumSet.complementOf(enabledCommands);
        }

        public EnumSet<CMD> getDisabed() {return EnumSet.complementOf(enabledCommands); }

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

        /**
         * @return return
         */
        public Stream<String> help(Status status) {
            return getEnabled()
                    .stream()
                    .filter(c -> c.aliases().length > 0)
                    .map(c -> "[" + c.aliases()[1] + "]\t" + c.aliases()[0]);
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

        public boolean isEnabled() { return controller.enabledCommands.contains(this.cmd); }

        public boolean isMasked() {return controller.mask.contains(this.cmd);}

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
