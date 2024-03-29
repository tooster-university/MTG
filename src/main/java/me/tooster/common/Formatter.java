package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility functions for generic purpose
 */
public enum Formatter {
    RESET("\033[0m"),
    INVERT("\33[7m"),
    UNDERLINE("\33[4m"),
    STRIKETHROUGH("\33[9m"),

    RED("\033[31m"),
    GREEN("\33[32m"),
    YELLOW("\33[33m"),
    BLUE("\33[34m"),
    PURPLE("\33[35m"),
    CYAN("\33[36m"),
    GRAY("\33[90m");

    private final String value;

    Formatter(String value) { this.value = value;}

    @Override
    public String toString() {return this.value;}

    /**
     * Formats announcement as:
     * <pre>
     * [HUB] hub announcement in purple underline
     * [SERVER] server announcement in cyan underline
     * DM from server in purple
     * DM from HUB in cyan
     * [!] [Player#725] server chat in yellow underlined
     * [Player$725] hub chat
     * -->[Boss] outgoing message in yellow inverted prefix
     * <--[Boss] incoming message in yellow inverted prefix
     * </pre>
     *
     * @param from     identity sending message
     * @param to       identity to send message to
     * @param incoming if message is incoming or outgoing
     * @param msg      string to format
     * @return formatted message as described above
     */
    public static String chat(@NotNull String from, @NotNull String to, boolean incoming, @NotNull String msg) {
        String prefix;
        // from server or hub
        if (from.equals("SERVER"))
            prefix = to.equals("SERVER") ? String.format("%s[SERVER] %s%s", PURPLE, UNDERLINE, PURPLE) : PURPLE.value;
        else if (from.equals("HUB")) prefix = to.equals("HUB") ? String.format("%s[HUB] %s%s", CYAN, UNDERLINE, CYAN) : CYAN.value;
            // from user
        else if (to.equals("SERVER")) prefix = String.format("%s[!] [%s] %s%s", YELLOW, from, UNDERLINE, YELLOW); // server shout
        else if (to.equals("HUB")) prefix = String.format("[%s] ", from); // hub shout
            // dm
        else prefix = String.format("%s%s%s[%s]%s%s ", YELLOW, INVERT, incoming ? "<--" : "-->", incoming ? from : to, INVERT, YELLOW);

        return String.format("%s%s%s", prefix, msg, RESET);
        //        return formatAllLines(CYAN + "" + INVERT + "[~] %-100s [~]" + RESET, msg);
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
    public static String response(@NotNull String msg) { // FIXME
        return formatAllLines(INVERT + " %-40s\t" + RESET + "\n", msg);
    }

    /**
     * Formats error as:
     *
     * <pre>
     * [ERR] some error message in red inverted
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String error(@NotNull String msg) {
        return String.format("%s%s[ERROR] %s%s", RED, INVERT, msg, RESET);
    }

    /**
     * Formats invalid as:
     *
     * <pre>
     * some invalid message in red
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String invalid(@NotNull String msg) {
        return String.format("%s%s%s", RED, msg, RESET);
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
        return String.format("%s(*) %s%s", GREEN, msg, RESET);
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
        return String.format("%s%s[$]> %s%s", PURPLE, INVERT, msg, RESET);
    }

    /**
     * Formats the warning as:
     * <pre>
     * Warning in yellow inverted
     * </pre>
     *
     * @param msg string to format
     * @return formatted message
     */
    public static String warning(@NotNull String msg) {
        return String.format("%s%s%s%s", Formatter.YELLOW, Formatter.INVERT, msg, Formatter.RESET);
    }

    /**
     * Formats the list of elements as:
     * <pre>
     * - element 1
     * - element 2
     * ...
     * - element n
     * </pre>
     *
     * @param elements array of string elements
     * @return single formatted string representing array of elements as in the order as in <b>elements</b>
     */
    public static String list(@NotNull Object[] elements) {
        String[] t = new String[elements.length];
        for (int i = 0; i < elements.length; i++)
            t[i] = elements[i].toString();

        return elements.length == 0 ? "" : "- " + String.join("\n- ", t);
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
        var formatArgs = new ArrayList<String>();
        formatArgs.add(""); // add dummy to be replaced by line
        formatArgs.addAll(Arrays.asList(args)); // append other args

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            formatArgs.set(0, line);
            sb.append(String.format(format, formatArgs.toArray()));
        }

        return sb.toString();
    }

    /**
     * Splits the single input line into parts separated by whitespace, where a part is any string that doesn't have whitespaces
     * or a string inside normal quotes ("this for example") with whitespaces.
     * For example:
     * <pre>test this123 !@#.asd "and !@3 this" "tes't ~"</pre>
     * will be split into
     * <pre>(test) (this123) (!@#.asd) (and !@3 this) (tes't ~)</pre>
     *
     * @param input input to split into parts
     * @return list of parts as described above.
     */
    public static List<String> splitParts(String input) {
        List<String> matchList = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"]+|\"([^\"]*)\""); // matches: (abcd) "(xy zv)" (x)
        Matcher regexMatcher = regex.matcher(input);
        while (regexMatcher.find())
            matchList.add(regexMatcher.group(1) != null ? regexMatcher.group(1) : regexMatcher.group());
        return matchList;
    }

    /**
     * Removes given part from the string
     *
     * @param idx   index of part to remove starting from 0
     * @param input string consisting of words and doubly quoted phrases: this "for example consists" 'of 5' parts:
     *              `this` `for example consists` `'of` `5'` `parts. Only double quotes make parts
     * @return input without part and it's trailing whitespaces: `remove  the      idx=1  part` -> `remove  idx=1 part`
     */
    public static String removePart(Integer idx, String input) {
        Pattern regex = Pattern.compile("[^\\s\"]+\\s*|\"[^\"]*\"\\s*"); // matches: (abcd) "(xy zv)" (x)
        Matcher regexMatcher = regex.matcher(input);
        int i = 0;
        int start = 0, end = 0;
        while (regexMatcher.find())
            if (i++ == idx) {
                start = regexMatcher.start();
                end = regexMatcher.end();
            }
        return input.substring(0, start) + input.substring(end);
    }

    /**
     * Returns progress in form [a/b] for numeric values or '-' for nulls
     *
     * @param a first stat
     * @param b second stat
     * @return
     */
    public static String formatProgress(Integer a, Integer b) {
        return String.format("[%s/%s]", a == null ? "-" : a, b == null ? "-" : b);
    }
}

