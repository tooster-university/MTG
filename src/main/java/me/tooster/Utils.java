package me.tooster;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


/**
 * Utility functions for generic purpose
 */
class Utils {
    private static final String responseDecorator = "================================================================";
    private static final String announceDecorator = "><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><><";
    private static final String errorDecorator = "[ERR]\t";

    /**
     * Formats single server response as:
     *
     * <pre>
     * ================================================================
     * first line of content
     * second line of content
     * ================================================================
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    static String formatResponse(@NotNull String msg) {
        return responseDecorator + "\n" + msg + "\n" + responseDecorator;
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
    static String formatList(@NotNull Object[] elements) {
        String[] t = new String[elements.length];
        for (int i = 0; i < elements.length; i++)
            t[i] = elements[i].toString();

        return ">\t" + String.join("\n>\t", t);
    }

    static String formatAnnouncement(@NotNull String msg) {
        return announceDecorator + "\n" + msg + "\n" + announceDecorator;
    }

    static String formatError(@NotNull String msg) {
        String[] lines = msg.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(errorDecorator + line);
        return sb.toString();

    }
}
