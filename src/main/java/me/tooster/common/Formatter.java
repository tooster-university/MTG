package me.tooster.common;

import org.jetbrains.annotations.NotNull;


/**
 * Utility functions for generic purpose
 */
public enum Formatter {
    RESET("\033[0m"),
    INVERT("\33[7m"),
    UNDERLINE("\33[4m"),

    RED("\033[31m"),
    GREEN("\33[32m"),
    YELLOW("\33[33m"),
    BLUE("\33[34m"),
    PURPLE("\33[35m"),
    CYAN("\33[36m");

    private final String value;

    Formatter(String value) { this.value = value;}

    @Override
    public String toString() {return this.value;}

    /**
     * Formats announcement as:
     * <pre>
     * [~] something     [~]
     * [~] something2    [~]
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String broadcast(@NotNull String msg) {
        return formatAllLines(CYAN + "" + INVERT + "[~] %-100s [~]" + RESET, msg);
    }

    /**
     * Formats single server response as:
     *
     * <pre>
     * #==============================================================================#
     * #= first line of content                                                      =#
     * #= second line of content                                                     =#
     * #==============================================================================#
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String response(@NotNull String msg) {
        return BLUE +
                "#==============================================================================#\n" +
                formatAllLines("#= %-74s =#", msg) + "\n" +
                "#==============================================================================#" +
                RESET;
    }

    /**
     * Formats error as:
     *
     * <pre>
     * [ERR] someerror.something
     * [ERR] anotherlineeoferror
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String error(@NotNull String msg) {
        return formatAllLines(RED + "" + INVERT + "[ERR]\t%s" + INVERT, msg);
    }

    /**
     * Formats tip as:
     *
     * <pre>
     * (*) a tip is here
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String tip(@NotNull String msg) {
        return formatAllLines(GREEN + "(*) %s" + INVERT, msg);
    }

    /**
     * Formats the prompt as:
     * <pre>
     * [$]> please, choose a deck
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String prompt(@NotNull String msg) {
        return formatAllLines(PURPLE + "" + INVERT + "[$]> %s" + INVERT, msg);
    }

    /**
     * Formats the say message as:
     * <pre>
     * [somePlayer]-> yeah this is the message
     * </pre>
     *
     * @param nick nick of a sender
     * @param msg  message
     * @return formatted message
     */
    public static String say(@NotNull String nick, @NotNull String msg) {
        return formatAllLines(YELLOW + "" + INVERT + "[%2$s]" + INVERT + INVERT + "-> " +
                "%1$s" + INVERT, msg, nick);
    }

    /**
     * Formats the shout message as:
     * <pre>
     * [!] [somePlayer]: yeah this is the message
     * </pre>
     *
     * @param nick nick of a sender
     * @param msg  message
     * @return formatted message
     */
    public static String shout(@NotNull String nick, @NotNull String msg) {
        return formatAllLines(YELLOW + "[!] [%2$s]" + INVERT + ": %1$s", msg, nick);
    }

    /**
     * Formats the list of elements as:
     *
     * <pre>
     * > element 1
     * > element 2
     * ...
     * > element n
     * </pre>
     *
     * @param elements array of string elements
     * @return single formatted string representing array of elements as serverIn the order as serverIn <b>elements</b>
     */
    public static String list(@NotNull Object[] elements) {
        String[] t = new String[elements.length];
        for (int i = 0; i < elements.length; i++)
            t[i] = elements[i].toString();

        return "- " + String.join("\n- ", t);
    }

    /**
     * Formats all lines of multiline message to give them some format
     *
     * @param format format of the line where %1$s is from msg line
     * @param args   arguments for formatter, referenced from %2$s
     * @return formatted message
     */
    public static String formatAllLines(String format, String msg, String... args) {
        String[] lines = msg.split("\\r?\\n");

        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(String.format(format, line, args));

        return sb.toString();
    }
}

