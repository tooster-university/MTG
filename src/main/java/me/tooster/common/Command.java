package me.tooster.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Interface for commands with alias. Each command can be compiled with code in format
 * <pre>new ServerCommand.Compiled(ServerCommand.ECHO, data)</pre>
 * Command interface works best when used with enums enums. Compiled command
 */
public interface Command{

    /**
     * Used to alias the commands. Command without alias won't be matched.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Alias {
        String[] value() default {};
    }

    /**
     * @return Returns list of commands from cached array, so there is no runtime memory overhead.
     */
    Command[] cachedValues();

    /**
     * Compiled command consists of Command enum and arguments as ArrayList&lt;String&gt;
     */
    class Compiled<CMD> {
        @NotNull
        public final CMD command;
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
}
