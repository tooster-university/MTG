package me.tooster.server;

import me.tooster.MTG.Deck;
import me.tooster.common.Command;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages;
import me.tooster.MTG.MTGCommand;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.tooster.server.ServerCommand.*;
import static me.tooster.MTG.MTGCommand.*;
import static me.tooster.common.proto.Messages.*;

/**
 * High level class representing user connected to the server
 */
public class User {

    private static final int TIMEOUT_CLIENT_HELLO_MS = 5_000; // timeout between connection established and CLIENT_HELLO
    private static final int TIMEOUT_MSG_MS          = 0; //20_000; // timeout between messages from client

    //----------------------------------------------------------------------------------------------------------------------------

    Thread listenRemoteThread;

    private final Socket                    socket;
    private final InputStream               in;
    private final OutputStream              out;
    final         Controller<ServerCommand> serverCommandController;
    final public  Controller<MTGCommand>    mtgCommandController;
    public final  long                      serverTag;               // unique tag per server
    public final  Map<String, String>       config;

    public Hub hub;

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
        serverCommandController.setEnabled(HELP, WHISPER, SAY, SHOUT, WHO);
        serverCommandController.setMasked(HELP, WHISPER, SAY, SHOUT, WHO);

        mtgCommandController = new Controller<>(MTGCommand.class, this);

        config = new ConcurrentHashMap<>() {{
            put("nick", "anon");
            put("fullControl", "true");
            put("deck", "default");
        }};
    }

    /**
     * This method performs handshake with the server.
     *
     * @throws IOException throws if handshake failed.
     */
    void handshake() throws IOException {
        socket.setSoTimeout(TIMEOUT_CLIENT_HELLO_MS); // 5 sec timeout for CLIENT_HELLO
        Message msg = Message.parseDelimitedFrom(in);
        // assert that first message is CONTROL, CLIENT_HELLO
        if (msg.getMsgTypeCase() == Message.MsgTypeCase.CONTROLMSG || msg.getControlMsg().getCode() == ControlMsg.Code.CLIENT_HELLO) {
            var receivedConfig = msg.getControlMsg().getConfigurationMap();
            config.putAll(receivedConfig); // nick and others in future
            transmit(ControlMsg.newBuilder()
                    .setCode(ControlMsg.Code.SERVER_HELLO)
                    .putConfiguration("identity", this.toString())); // transmit identity
        } else {
            transmit(ControlMsg.newBuilder()
                    .setCode(ControlMsg.Code.SERVER_DENY)
                    .putConfiguration("details", "Didn't receive CLIENT_HELLO"));
            throw new SecurityException("Protocol violation - didn't receive CLIENT_HELLO.");
        }
    }

    /**
     * Handles remote input from client and timeouts
     */
    void listenRemote() {
        try {

            handshake(); // handshake must be on this thread so it won't block main server thread

            var users = Server.getInstance().users;
            users.put(serverTag, this); // register in users map
            Server.LOGGER.info("Client " + toString() + " connected.");
            Server.getInstance().broadcast(toString() + " joined the server.");

            Server.getInstance().hub.addUser(this);

            socket.setSoTimeout(TIMEOUT_MSG_MS);// 20 sec timeout, if client doesn't send the data in 20 sec, he is assumed dead

            // listening loop
            Message msg;
            while ((msg = Message.parseDelimitedFrom(in)) != null && !Thread.interrupted())
                handleRemoteMessage(msg);

        } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
            Server.LOGGER.fine(this.toString() + ": timeout reached.");
        } catch (SocketException | SecurityException e) {
            Server.LOGGER.fine(this.toString() + ": " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    /**
     * Handles the message received from the server
     *
     * @param msg message received from the server
     * @throws IOException idk, if something happens or some other shit
     */
    private void handleRemoteMessage(Message msg) throws IOException {

        switch (msg.getMsgTypeCase()) {
            case CONTROLMSG: { // client sent control message. Any invalid control message will result in connection abort
                var receivedConfig = msg.getControlMsg().getConfigurationMap();
                switch (msg.getControlMsg().getCode()) {
                    case PING: // respond to ping
                        transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.PONG));
                        break;
                    case CONFIG: // config sent later
                        Server.LOGGER.finest(String.format("#%s received config:\n%s", serverTag, msg.toString()));
                        if (receivedConfig.isEmpty()) {
                            transmit(VisualMsg.newBuilder().setFrom("SERVER").setTo(this.toString()).setMsg(config.toString()));
                        } else {
                            var oldNick = this.toString();
                            config.putAll(receivedConfig);
                            if (!oldNick.equals(this.toString())) // if nick changed - inform all
                                Server.getInstance().broadcast(oldNick + " is now " + toString());
                        }
                        break;
                    case CLIENT_DISCONNECT: // handle disconnect cleanup work
                        Server.LOGGER.info(toString() + " requested disconnect.");
                        disconnect();
                        break;
                    default:
                        Server.LOGGER.warning("Invalid message received. Connection will be aborted.");
                        disconnect();
                }
                break;
            }

            case COMMANDMSG: { // client sent command


                try {
                    String input = msg.getCommandMsg().getCommand();
                    Compiled mtgParsed = mtgCommandController.parse(input);
                    Compiled parsed = serverCommandController.parse(input);

                    if (mtgParsed.cmd != null && !mtgParsed.isEnabled()) throw new Command.CommandDisabledException((Command) mtgParsed.cmd);

                    if (mtgParsed.cmd != null) {
                        hub.fsm.process(mtgParsed);
                    } else {
                        Compiled<ServerCommand> command = parsed;
                        if (command.cmd == null) { // default to '/say'
                            input = SAY.mainAlias() + " " + input;
                            command = serverCommandController.parse(input); // parse again to SAY
                        }
                        if (!command.isEnabled()) throw new IllegalArgumentException();

                        switch (command.cmd) {
                            case SHOUT:
                            case SAY: {
                                if (command.args.length < 2) { // if single /say or /shout without text was received, send usage
                                    transmit(VisualMsg.newBuilder()
                                            .setVariant(VisualMsg.Variant.INVALID)
                                            .setMsg(command.cmd.help()));
                                    return;
                                }
                                // remove command from text and strip if it is shout
                                var text = Formatter.removePart(0, input);

                                // if say and hub exists -> say, otherwise shout to server
                                if (command.cmd == SAY && hub != null) hub.shout(this, text);
                                else Server.getInstance().shout(this, text);
                                break;
                            }

                            case WHISPER: {
                                if (command.args.length < 3) { // if whisper doesn't have recipient and text specified, send usage
                                    transmit(VisualMsg.newBuilder()
                                            .setVariant(VisualMsg.Variant.INVALID)
                                            .setMsg(command.cmd.help()));
                                    return;
                                }
                                // remove command and recipient from command
                                var text = Formatter.removePart(0, Formatter.removePart(1, input));

                                User target = Server.getInstance().findUser(command.args[1]);
                                if (target == null || target == this)
                                    transmit(VisualMsg.newBuilder()
                                            .setVariant(VisualMsg.Variant.INVALID)
                                            .setMsg("User is offline or too many matching recipients."));
                                else {
                                    var chat = VisualMsg.newBuilder()
                                            .setVariant(VisualMsg.Variant.CHAT)
                                            .setFrom(this.toString())
                                            .setTo(target.toString())
                                            .setMsg(text);
                                    target.transmit(chat);
                                    this.transmit(chat);
                                }
                                break;
                            }

                            case HELP: {
                                if (command.args.length > 1) { // help for specific command
                                    var helpCmd = serverCommandController.parse(command.arg(1)).cmd;
                                    if (helpCmd != null) {
                                        transmit(VisualMsg.newBuilder()
                                                .setVariant(VisualMsg.Variant.CHAT)
                                                .setFrom("SERVER")
                                                .setMsg(Command.help(helpCmd)));

                                    } else {
                                        var helpMtgCmd = mtgCommandController.parse(command.arg(1)).cmd;
                                        transmit(VisualMsg.newBuilder()
                                                .setVariant(helpMtgCmd == null || !helpMtgCmd.hasHelp() ? VisualMsg.Variant.INVALID
                                                                                                        : VisualMsg.Variant.CHAT)
                                                .setFrom("SERVER")
                                                .setMsg(Command.help(helpCmd)));
                                    }
                                } else {// global help
                                    transmit(VisualMsg.newBuilder()
                                            .setFrom("SERVER")
                                            .setMsg(String.join("\n", serverCommandController.enabledCommands.stream()
                                                    .map(c -> Command.help(c)).toArray(String[]::new))));

                                    transmit(VisualMsg.newBuilder()
                                            .setFrom("HUB")
                                            .setMsg(String.join("\n", mtgCommandController.enabledCommands.stream()
                                                    .map(c -> Command.help(c)).toArray(String[]::new))));
                                }
                                break;
                            }

                            case WHO: {
                                transmit(VisualMsg.newBuilder()
                                        .setFrom("SERVER")
                                        .setMsg("Online users: " + String.join(", ",
                                                Server.getInstance().users.values().stream().map(User::toString).toArray(String[]::new))));
                            }
                        }
                    }
                } catch (CommandDisabledException e) {
                    transmit(VisualMsg.newBuilder()
                            .setVariant(VisualMsg.Variant.ERROR)
                            .setMsg(e.getMessage()));
                }
                break;
            }

            case MSGTYPE_NOT_SET: {
                Server.LOGGER.warning("Received empty message while listening.");
                break;
            }
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
            if (message instanceof ControlMsg.Builder)
                msg.setControlMsg(((ControlMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else if (message instanceof CommandMsg.Builder)
                msg.setCommandMsg(((CommandMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else if (message instanceof VisualMsg.Builder)
                msg.setVisualMsg(((VisualMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else
                throw new IllegalArgumentException("Invalid message type");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * finalizes user disconnection from server
     */
    public void disconnect() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                // updates game, sends status to other player etc.
                if (hub != null) hub.removeUser(this);
                Server.getInstance().users.remove(serverTag);
                Server.getInstance().broadcast(toString() + "disconnected.");
                Server.LOGGER.info("Client " + toString() + " disconnected.");
            }
            listenRemoteThread.interrupt(); // close the streams and halt the thread
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------


    /**
     * Returns nick#tag of a player, that is a unique identifier on the server
     *
     * @return nick#tag string
     */
    @Override
    public String toString() { return config.get("nick") + "#" + String.format("%04d", serverTag); }
}

