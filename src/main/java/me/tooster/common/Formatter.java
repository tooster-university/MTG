package me.tooster.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        if (from.equals("SERVER")) prefix = to.equals("SERVER") ? String.format("%s[SERVER] %s", PURPLE, UNDERLINE) : PURPLE.value;
        else if (from.equals("HUB")) prefix = to.equals("HUB") ? String.format("%s[HUB] %s", CYAN, UNDERLINE) : CYAN.value;
        // from user
        else if (to.equals("SERVER")) prefix = String.format("%s[!] [%s] %s", YELLOW, from, UNDERLINE); // server shout
        else if (to.equals("HUB")) prefix = String.format("[%s] ",from); // hub shout
        // dm
        else prefix = String.format("%s%s%s[%s]%s ", YELLOW, INVERT, incoming ? "<--" : "-->", incoming ? from : to, INVERT);

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
        return String.format("%s%s[ERROR] %s%s", RED, INVERT, msg, RESET);
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
     * Formats the list of elements as:
     * <pre>
     * - element 1
     * - element 2
     * ...
     * - element n
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
     * Returns progress in form [a/b] for numeric values or '-' for nulls
     * @param a first stat
     * @param b second stat
     * @return
     */
    public static String formatProgress(Integer a, Integer b){
        return String.format("[%s/%s]", a == null ? "-" : a, b == null ? "-" : b);
    }
}

