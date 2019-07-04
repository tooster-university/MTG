package me.tooster.MTG;

import me.tooster.Hub;
import me.tooster.Parser;
import me.tooster.exceptions.CommandException;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

public class Player implements AutoCloseable {

    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    private String nick;
    private int tag;                // unique tag per server
    private Hub hub;                // connected Hub
    private Deck deck;
    private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);


    private final EnumSet<Parser.Command> enabledCommands = EnumSet.of(Parser.Command.HELP);


    // MTG specific things
    private int HP;
    private Mana manaPool;


    public enum Flag {
        // global flags
        AFK,            // player unresponsive or inactive for since 3 minutes
        SURRENDERED,    // player surrendered
        READY,          // player ady to play a game

        // MTG flags
        HEXPROOF,
        PREVENT_DAMAGE;
    }

    /**
     * Creates new player on server with given socket he's connected to and unique server tag.
     *
     * @param socket players connection
     * @param tag    tag assigned by the server
     * @throws IOException for socket errors
     */
    public Player(Socket socket, int tag) throws IOException {
        socket.setSoTimeout(5000); // 5 second timeout for config sending
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        this.tag = tag;

        String[] config;
        try {
            config = in.readLine().split("\\s+"); // read config with nick
        } catch (SocketTimeoutException e) {
            throw new IOException("Player didn't send config data in the first 5 seconds. Assuming he is dead.");
        }

        this.nick = config[0];
        HP = 20;
        manaPool = new Mana(null);
    }

    /**
     * Returns nick#tag of a player, that is a unique identifier on the server
     *
     * @return nick#tag string
     */
    public String getNick() { return nick + "#" + String.format("%04d", tag); }

    /**
     * Adds reference to player's hub
     *
     * @param hub hub to set
     */
    public void setHub(Hub hub) {
        this.hub = hub;
    }

    /**
     * Returns hub that player connected to.
     *
     * @return hub that the player connected to
     */
    public Hub getHub() { return hub; }

    /**
     * Specifies, which commands are enabled. HELP is always added as side effect
     *
     * @param commands commands to enable, and nothing besides them
     */
    public void setCommands(Parser.Command... commands) {
        enabledCommands.clear();
        enabledCommands.add(Parser.Command.HELP);
        enabledCommands.addAll(Arrays.asList(commands));
    }

    /**
     * Enables commands. HELP can be enabled again with it, but why would it even be disabled...
     *
     * @param commands commands to enable
     */
    public void enableCommands(Parser.Command... commands) { enabledCommands.addAll(Arrays.asList(commands)); }

    /**
     * Disables commands. HELP can be disabled only here, but seriously tho, why??
     *
     * @param commands commands to disable
     */
    public void disableCommands(Parser.Command... commands) { enabledCommands.removeAll(Arrays.asList(commands)); }

    /**
     * @return set of enabled commands for player at given moment
     */
    public EnumSet<Parser.Command> getEnabledCommands() { return enabledCommands; }

    /**
     * @return player's deck
     */
    public Deck getDeck() { return deck; }

    /**
     * Set's player's deck
     *
     * @param deck deck to set
     */
    void setDeck(Deck deck) { this.deck = deck; }

    /**
     * @return returns player's flags
     */
    public EnumSet<Flag> getFlags() { return flags; }

    /**
     * Fetches and runs next command from player
     */
    public void listen() throws IOException, CommandException {
            while (!flags.contains(Flag.AFK)) {

                try {
                    String s = in.readLine();
                    if (s == null || s.isEmpty()) continue;
                    ArrayList<String> command = new ArrayList<>(Arrays.asList(s.split("\\s+")));
                    Parser.CompiledCommand cc = Parser.parse(this, command);
                    hub.issueCommand(cc);
                } catch (CommandException e) {// invalid command
                    transmit(e.getMessage());
                } catch (SocketTimeoutException e){
                    hub.issueCommand(new Parser.CompiledCommand(Parser.Command.TIMEOUT));
                    throw e;
                }
            }
    }

    /**
     * Send data to player
     *
     * @param data data to be sent
     */
    public synchronized void transmit(String data) {
        out.println(data);
    }

    /**
     * Sends a prompt requesting player action.
     *
     * @param msg description of requested action to send to the player
     */
    public synchronized void prompt(String msg) { transmit(String.format("@%s ! %s", nick, msg)); }

    /**
     * Checks if player can cast a card with given ID
     *
     * @param ID ID of card to cast
     * @return true if card with given ID can be cast, false otherwise or if card doesn't exist
     */
    public boolean canCast(String ID) {
        return true;
    }

    //----------------------------------------------
    public void reset() {
        flags.clear();
        deck.reset();
    }


    @Override
    public void close() throws Exception {
        in.close();
        out.close();
        socket.close();
        flags.add(Flag.AFK);
    }
}

