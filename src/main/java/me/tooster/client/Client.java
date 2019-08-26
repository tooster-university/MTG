package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages.*;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;

import static me.tooster.client.ClientCommand.*;
import static me.tooster.client.ClientStateMachine.State.*;


class Client {

    static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.config.file", Client.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Client.class.getName());
    }

    private static final int TIMEOUT_SERVER_HELLO_MS = 5_000; // timeout between CLIENT_HELLO and SERVER_HELLO
    private static final int TIMEOUT_MSG_MS          = 0; //15_000; // timeout between messages
    //----------------------------------------------------------------------------------------------------------------------------
    String serverIP;  // currently connected server's IP
    long   serverPort; // currently connected server's port

    private Socket       socket;
    private OutputStream out;
    private InputStream  in;

    final ClientStateMachine        cFSM;
    final Controller<ClientCommand> commandController; // ???

    HashMap<String, String> config; // for future serialization
    HashMap<String, String> remoteConfig; // config received from server


    //----------------------------------------------------------------------------------------------------------------------------


    public Client() {
        cFSM = new ClientStateMachine(this);
        commandController = new Controller<>(ClientCommand.class, this);
        commandController.setEnabled(HELP, SHUTDOWN, CONFIG);
        commandController.setMasked(HELP, SHUTDOWN, CONFIG);
        config = new HashMap<>() {{
            put("nick", "Bob");
            put("serverIP", "127.0.0.1");
            put("serverPort", "62442");
        }};
        remoteConfig = new HashMap<>();
    }

    private Thread listenLocalThread, listenRemoteThread;

    /**
     * Listens for command line input on local machine
     */
    void listenLocal() {
        listenLocalThread = Thread.currentThread();

        System.out.println(String.format("%s%s--==: MTG Client started :==--%s", Formatter.CYAN, Formatter.INVERT, Formatter.RESET));
        LOGGER.finest("Local config: " + config.toString());

        cFSM.process(commandController.compile(ClientCommand.CONNECT, config.get("serverIP"), config.get("serverPort")));

        Scanner cliScanner = new Scanner(System.in);
        String input;

        parseLoop:
        do {
            input = cliScanner.nextLine();
            if (input.isBlank()) continue;
            var compiled = commandController.parse(input);
            if (compiled.cmd == null)  // command unrecognized - send as raw input to server
                cFSM.process(compiled);
            else if (!compiled.isEnabled())  // command disabled
                System.out.println(Formatter.error("Command disabled."));
            else switch (compiled.cmd) {
                    case HELP:
                        // return whole client's and server's help for enabled commands
                        if (compiled.args.length == 1) { // "/help" without parameters
                            System.out.println(String.join("\n", commandController.enabledCommands.stream()
                                    .filter(Command::hasHelp)
                                    .map(c -> Formatter.YELLOW + Command.help(c) + Formatter.RESET)
                                    .toArray(String[]::new)));

                            synchronized (cFSM) {
                                if (cFSM.getCurrentState() == CONNECTED)
                                    transmit(CommandMsg.newBuilder().setCommand(input));
                            }

                        } else {
                            // check if arg[0] is on client, print message if it is, send help request to server otherwise
                            var helpCmd = commandController.parse(compiled.arg(1)).cmd;
                            synchronized (cFSM) {
                                if (helpCmd != null || cFSM.getCurrentState() != CONNECTED) // help is on client/not connected
                                    System.out.println(Formatter.YELLOW + Command.help(helpCmd) + Formatter.RESET);
                                else transmit(CommandMsg.newBuilder().setCommand(input));
                            }
                        }
                        break;
                    case SHUTDOWN:
                        cFSM.process(commandController.compile(CONNECTION_CLOSED));
                        listenLocalThread.interrupt();
                        break parseLoop;
                    default: // not parsed passed to server
                        cFSM.process(compiled);
                }

        } while (!Thread.interrupted());


        System.out.println(String.format("%s%s--==: MTG Client stopped :==--%s", Formatter.CYAN, Formatter.INVERT, Formatter.RESET));
    }

    /**
     * This method performs handshake with the server.
     *
     * @throws IOException throws if handshake failed.
     */
    private void handshake() throws IOException {
        transmit(ControlMsg.newBuilder()
                .setCode(ControlMsg.Code.CLIENT_HELLO)
                .putConfiguration("nick", config.get("nick")));
        socket.setSoTimeout(TIMEOUT_SERVER_HELLO_MS); // 5 sec to wait for SERVER_HELLO
        Message msg = Message.parseDelimitedFrom(in);
        if (msg.getMsgTypeCase() == Message.MsgTypeCase.CONTROLMSG) { // must be a CONTROL either SERVER_HELLO or SERVER_DENY message
            var remoteConfig = msg.getControlMsg().getConfigurationMap();
            if (msg.getControlMsg().getCode() == ControlMsg.Code.SERVER_HELLO)
                this.remoteConfig.putAll(remoteConfig);
            else if (msg.getControlMsg().getCode() == ControlMsg.Code.SERVER_DENY)
                throw new SocketException(remoteConfig.get("details"));
            else
                throw new SecurityException("Protocol violation - message is neither " + ControlMsg.Code.SERVER_HELLO +
                        " nor " + ControlMsg.Code.SERVER_DENY);
        } else throw new SecurityException("Protocol violation - message is not a " + Message.MsgTypeCase.CONTROLMSG);
    }

    /**
     * Connects to a server defined in IP and port variables and listens for input
     */
    void listenRemote() {
        try (Socket socket = new Socket()) {

            this.socket = socket;
            listenRemoteThread = Thread.currentThread();

            socket.connect(new InetSocketAddress(config.get("serverIP"), Integer.parseInt(config.get("serverPort"))),
                    2 * TIMEOUT_MSG_MS); // await for connection...

            out = socket.getOutputStream();
            in = socket.getInputStream();


            handshake();
            cFSM.process(commandController.compile(CONNECTION_ESABLISHED));


            keepAlive:
            do { // every 10 seconds a control PING packet is sent to the server
                socket.setSoTimeout(TIMEOUT_MSG_MS); // the 10 seconds are hardcoded for any activity from server
                Message msg;
                try {
                    msg = Message.parseDelimitedFrom(in);
                    if (msg == null) break keepAlive; // connection closed by server
                } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
                    // keepalive
                    transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.PING));
                    msg = Message.parseDelimitedFrom(in); // this throws SocketTimeoutException if nothing came
                }

                handleRemoteMessage(msg);
                // if it came to this line, the string was read
            } while (!Thread.interrupted());

        } catch (ConnectException e) {
            Client.LOGGER.warning(e.getMessage());
            System.out.println(Formatter.error(String.format("Cannot connect to the server at %s:%s",
                    config.get("serverIP"),
                    config.get("serverPort"))));
        } catch (SocketException e) { // connection reset, closed or others
            Client.LOGGER.warning(e.getMessage());
        } catch (SecurityException e) {
            Client.LOGGER.severe(e.getMessage());
        } catch (IOException e) { // some unexpected exceptions
            Client.LOGGER.severe(e.getMessage());
            e.printStackTrace();
        } finally { // finalize by disconnecting
            cFSM.process(commandController.compile(CONNECTION_CLOSED));
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
            case CONTROLMSG: { // respond
                var receivedConfig = msg.getControlMsg().getConfigurationMap();
                switch (msg.getControlMsg().getCode()) {
                    case PONG: break;
                    case CONFIG:
                        remoteConfig.putAll(receivedConfig);
                        break;
                    default:
                        Client.LOGGER.warning("Received wrong control while listening: " + msg.toString());
                }
                break;
            }

            case VISUALMSG: {
                var visualMsg = msg.getVisualMsg();
                var text = visualMsg.getMsg();
                switch (visualMsg.getVariant()) {
                    case CHAT: System.out.println(Formatter.chat(
                            visualMsg.getFrom(),
                            visualMsg.getTo(),
                            visualMsg.getTo().equals(remoteConfig.get("identity")),
                            text));
                        break;
                    case INFO: System.out.println(Formatter.response(text));
                        break;
                    case INVALID: System.out.println(Formatter.invalid(text));
                        break;
                    case ERROR: System.out.println(Formatter.error(text));
                        break;
                    case TIP: System.out.println(Formatter.tip(text));
                        break;
                    case PROMPT: System.out.println(Formatter.prompt(text));
                        break;
                    case UNRECOGNIZED:
                        LOGGER.warning("Unrecognized visual message variant: " + text);
                        break;
                }
                break;
            }

            case MSGTYPE_NOT_SET:
                Client.LOGGER.warning("Received empty message while listening.");
                break;
        }
    }

    /**
     * Disconnects client from the server and halts the listening thread
     */
    void disconnect() {
        try {
            if (cFSM.getCurrentState() == CONNECTED && !socket.isClosed()) {
                transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.CLIENT_DISCONNECT));
                socket.close();
            }
            listenRemoteThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
            listenLocalThread.interrupt(); // re-set the interrupt status
        }
        System.out.println(String.format("%s%sDisconnected.%s", Formatter.YELLOW, Formatter.INVERT, Formatter.RESET));
    }

    /**
     * Sends message to server
     *
     * @param message message to send
     */
    void transmit(Object message) {
        try {
            // build message from it's subtypes
            var msg = Message.newBuilder();
            LOGGER.finest("Transmit: " + message.toString().replaceAll("\\n", " ").trim());
            if (message instanceof ControlMsg.Builder)
                msg.setControlMsg(((ControlMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else if (message instanceof CommandMsg.Builder)
                msg.setCommandMsg(((CommandMsg.Builder) message).build()).build().writeDelimitedTo(out);
            else
                throw new IllegalArgumentException("Invalid message type");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    // arguments [nick server_IP server_port] can be passed
    public static void main(String[] args) {
        Client client = new Client();
        client.cFSM.start();

        switch (args.length) {
            default:
            case 3: client.config.put("serverIP", args[2]);
            case 2: client.config.put("serverPort", args[1]);
            case 1: client.config.put("nick", args[0]);
            case 0:
        }

        LOGGER.info("Starting client...");
        new Thread(client::listenLocal).start();
    }
}


