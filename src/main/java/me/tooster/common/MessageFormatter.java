package me.tooster.common;

import org.jetbrains.annotations.NotNull;


/**
 * Utility functions for generic purpose
 */
public class MessageFormatter {
    private static final String reset = "\033[0m";
    private static final String invert = "\33[7m";
    private static final String underline = "\33[4m";

    private static final String red = "\033[31m";
    private static final String green = "\33[32m";
    private static final String yellow = "\33[33m";
    private static final String blue = "\33[34m";
    private static final String purple = "\33[35m";
    private static final String cyan = "\33[36m";

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
        return formatAllLines(cyan + invert + "[~] %-100s [~]" + reset, msg);
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
        return blue +
                "#==============================================================================#\n" +
                formatAllLines("#= %-74s =#", msg) + "\n" +
                "#==============================================================================#" +
                reset;
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
        return formatAllLines(red + invert + "[ERR]\t%s" + reset, msg);
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
        return formatAllLines(green + "(*) %s" + reset, msg);
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
        return formatAllLines(purple + invert + "[$]> %s" + reset, msg);
    }

    /**
     * Formats the say message as:
     * <pre>
     * [somePlayer]-> yeah this is the message
     * </pre>
     * @param nick nick of a sender
     * @param msg message
     * @return formatted message
     */
    public static String say(@NotNull String nick, @NotNull String msg) {
        return formatAllLines(yellow + invert + "[%2$s]" + reset + invert + "-> %1$s" + reset, msg, nick);
    }

    /**
     * Formats the shout message as:
     * <pre>
     * [!] [somePlayer]: yeah this is the message
     * </pre>
     * @param nick nick of a sender
     * @param msg message
     * @return formatted message
     */
    public static String shout(@NotNull String nick,@NotNull String msg) {
        return formatAllLines(yellow + "[!] [%2$s]" + reset  + ": %1$s", msg, nick);
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
     * @return single formatted string representing array of elements as in the order as in <b>elements</b>
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
     * @param args   arguments
     * @return formatted message
     */
    private static String formatAllLines(String format, String msg, String... args) {
        String[] lines = msg.split("\\r?\\n");

        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(String.format(format, line, args));

        return sb.toString();
    }
}

