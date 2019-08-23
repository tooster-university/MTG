package me.tooster.server;

import me.tooster.MTG.Deck;
import me.tooster.common.Command;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages;
import me.tooster.MTG.MTGCommand;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.tooster.server.ServerCommand.*;
import static me.tooster.common.proto.Messages.*;

/**
 * High level class representing user connected to the server
 */
public class User {

    Thread listenRemoteThread;

    private final Socket                    socket;
    private final InputStream               in;
    private final OutputStream              out;
    final         Controller<ServerCommand> serverCommandController;
    final public  Controller<MTGCommand>    mtgCommandController;
    public final  long                      serverTag;               // unique tag per server
    public final  Map<String, String>       config;

    public Hub hub;

    //--- MTG DATA -------------------------------------------------
    private boolean ready;
    public  Deck    deck;

    //----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates new player on server with given socket he's connected to and unique server tag.
     * Starts new thread listening for remote input from client.
     *
     * @param socket    players connection
     * @param serverTag tag assigned by the server
     * @throws IOException for socket errors
     */
    User(Socket socket, long serverTag) throws IOException {

        this.socket = socket;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();

        this.serverTag = serverTag;

        serverCommandController = new Controller<>(ServerCommand.class, this);
        serverCommandController.setEnabled(HELP, WHISPER, SAY, SHOUT);
        serverCommandController.setMasked(HELP, WHISPER, SAY, SHOUT);

        mtgCommandController = new Controller<>(MTGCommand.class, this);

        config = new ConcurrentHashMap<>();
        this.config.put("nick", "anon"); // fallback nick
        transmit(ConfigMsg.newBuilder().putConfiguration("remoteID", this.toString())); // transmit new identity

        (listenRemoteThread = new Thread(this::listenRemote)).start();
    }

    /**
     * Handles remote input from client and timeouts
     */
    private void listenRemote() {
        try {
            socket.setSoTimeout(20 * 1000);// 20 sec timeout, if client doesn't send the data in 20 sec, he is assumed dead

            Message msg;
            // listening loop
            while ((msg = Message.parseDelimitedFrom(in)) != null && !Thread.interrupted())
                handleRemoteMessage(msg);

        } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
            disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        disconnect();
    }

    /**
     * Handles the message received from the server
     *
     * @param msg message received from the server
     * @throws IOException idk, if something happens or some other shit
     */
    private void handleRemoteMessage(Message msg) throws IOException {
        switch (msg.getMsgTypeCase()) {
            case CONTROLMSG: // client sent control message
                switch (msg.getControlMsg().getCode()) {
                    case PING: // respond to ping
                        transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.PONG));
                        break;
                    case CLIENT_DISCONNECT: // handle disconnect cleanup work
                        disconnect();
                        break;
                    case UNRECOGNIZED: // some shit happens
                        Server.LOGGER.warning("Received unrecognized control message.");
                        break;
                }
                break;
            case CONFIGMSG: // client sent config
                var receivedConfig = msg.getConfigMsg().getConfigurationMap();
                String oldNick = this.toString();
                config.putAll(receivedConfig);
                if (receivedConfig.containsKey("nick")) { // if nick changed - inform all
                    Server.getInstance().broadcast(oldNick + " is now " + toString());
                    transmit(ConfigMsg.newBuilder().putConfiguration("remoteID", this.toString())); // transmit new identity
                }
            case COMMANDMSG: // client sent command
                String input = msg.getCommandMsg().getCommand();
                var parsed = serverCommandController.parse(input);
                if (parsed.cmd == null) parsed = serverCommandController.compile(SAY, input);

                if (parsed.cmd == SHOUT || parsed.cmd == SAY) { // chat message
                    // remove command from text and strip if it is shout
                    if (parsed.args.length < 2) {
                        transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg(parsed.cmd.help()));
                        return;
                    }
                    parsed.args[0] = input.substring(parsed.arg(0).length()).strip();

                    // if say and hub exists -> say, otherwise shout to server
                    if (parsed.cmd == SAY && hub != null) hub.broadcast(parsed.arg(0));
                    else Server.getInstance().broadcast(parsed.arg(0));

                } else if (parsed.cmd == WHISPER) {
                    if (parsed.args.length < 3) {
                        transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg(parsed.cmd.help()));
                        return;
                    }
                    parsed.args[3] = input.substring(parsed.args[1].length()).strip().substring(parsed.args[2].length()).strip();
                    User target = Server.getInstance().findUser(parsed.arg(1));
                    if (target == null) transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg("User offline or too generic."));
                    else transmit(ChatMsg.newBuilder().setFrom(this.toString()).setTo(target.toString()).setMsg(parsed.arg(3)));
                } else if (parsed.cmd == HELP) {
                    if (parsed.args.length > 1) // help is on client/not connected // fixme reply with Command.newBuilder
                        transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg(serverCommandController.help(parsed.arg(1))));
                    else
                        transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg(
                                Formatter.list(serverCommandController.enabledCommands.stream().map(Command::help).toArray(String[]::new))));
                }
                hub.hubFSM.process(parsed);
                break;

            case MSGTYPE_NOT_SET:
                Server.LOGGER.warning("Received empty message.");
                break;
        }
    }

    /**
     * Send message to remote client
     *
     * @param message message to send that is an instance of Message protobuf
     */
    public synchronized void transmit(Object message) {
        try {
            // build message from it's subtypes
            var msg = Messages.Message.newBuilder();

            if (message instanceof ConfigMsg.Builder)
                msg.setConfigMsg(((ConfigMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else if (message instanceof ControlMsg.Builder)
                msg.setControlMsg(((ControlMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else if (message instanceof CommandMsg.Builder)
                msg.setCommandMsg(((CommandMsg.Builder) message).build()).build().writeDelimitedTo(out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            // updates game, sends status to other player etc.
            if (hub != null) hub.removeUser(this);
            Server.getInstance().users.remove(serverTag); // remove from players list
            listenRemoteThread.interrupt(); // close the streams and halt the thread
            socket.close();
            Server.LOGGER.info("Client " + toString() + " disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------

    /**
     * Sets player ready to game status and returns true if he is indeed ready and false otherwise
     */
    public boolean setReady(boolean ready) {
        return this.ready = ready && deck != null;
    }

    public boolean isReady() { return ready;}


    /**
     * Returns nick#tag of a player, that is a unique identifier on the server
     *
     * @return nick#tag string
     */
    @Override
    public String toString() { return config.get("nick") + "#" + String.format("%04d", serverTag); }
}

