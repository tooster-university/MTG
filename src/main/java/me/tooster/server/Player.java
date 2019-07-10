package me.tooster.server;

import me.tooster.common.Parser;
import me.tooster.server.MTG.Deck;
import me.tooster.server.MTG.MTGCommand;
import me.tooster.server.MTG.Mana;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.EnumSet;

import static me.tooster.server.ServerCommand.*;

public class Player implements AutoCloseable {

    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    private final String nick;
    private final int tag;                // unique tag per server
    private final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

    public final Parser<ServerCommand> scParser = new Parser<>(ServerCommand.class);
    public final Parser<MTGCommand>mtgParser = new Parser<>(MTGCommand.class);

    private Hub hub;                // connected Hub
    private Deck deck;


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

        scParser.enableCommands(HELP, PING, CONFIG, DISCONNECT);
        scParser.commandMask.remove(HELP);

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
     * @return player's deck
     */
    public Deck getDeck() { return deck; }

    /**
     * Set's player's deck
     *
     * @param deck deck to set
     */
    public void setDeck(Deck deck) { this.deck = deck; }

    /**
     * @return returns player's flags
     */
    public EnumSet<Flag> getFlags() { return flags; }

    /**
     * Fetches and runs process command from player
     */
    public void listen() throws IOException {
            while (!flags.contains(Flag.AFK)) {

                try {
                    String cmd = in.readLine();
                    if (cmd == null || cmd.isEmpty()) continue;
                    hub.process(this, cmd);
                } catch (SocketTimeoutException e){ // FIXME: Timeout
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

