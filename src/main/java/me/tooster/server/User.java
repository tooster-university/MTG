package me.tooster.server;

import me.tooster.common.Command;
import me.tooster.common.CommandException;
import me.tooster.common.Formatter;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static me.tooster.server.ServerCommand.*;

/**
 * High level class representing user connected to the server
 */
public class User implements AutoCloseable, Runnable {

    private       Hub            hub;       // connected Hub
    private final Socket         socket;
    private final BufferedReader in;
    private final PrintWriter    out;

    private       String nick;
    private final int    tag;               // unique tag per server

    public final Command.Controller<ServerCommand> cmdController = new Command.Controller<>();


    /**
     * Creates new player on server with given socket he's connected to and unique server tag.
     *
     * @param socket players connection
     * @param tag    tag assigned by the server
     * @throws IOException for socket errors
     */
    public User(Socket socket, int tag) throws IOException {

        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

        this.tag = tag;

        cmdController.enable(HELP, PING, CONFIG, DISCONNECT, SHOUT, WHISPER);
        cmdController.unmask(HELP);

        this.nick = "thelegend27"; // falback nick


    }

    @Override
    public void run() {
        // listening loop

        try {
            socket.setSoTimeout(20 * 1000);// 20 sec timeout, if client doesn't send the data serverIn 20 sec, he is assumed to be dead
            String s;
            while ((s = in.readLine()) != null) {
                Command.Compiled<ServerCommand> cc = cmdController.parse(s);
                if(cc == null)
                    transmit("");
                if(cc.cmd == PING)
                    transmit(PONG.toString());
                hub.hubFSM.process(cc);
            }
        } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
            hub.hubFSM.process(cmdController.compile(DISCONNECT), User.this);
        } catch (IOException e) {
            e.printStackTrace();
        }

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


    @Override
    public void close() throws Exception {
        in.close();
        out.close();
        socket.close();
    }

    @Override
    public String toString() {
        return getNick();
    }
}

