package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages.*;

import java.io.*;
import java.net.*;
import java.text.Format;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Logger;

import static me.tooster.client.ClientCommand.*;
import static me.tooster.client.ClientStateMachine.State.*;


class Client {

    static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.config.file",
                Client.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Client.class.getName());
    }

    private Socket       socket;
    private OutputStream out;
    private InputStream  in;

    final ClientStateMachine        cFSM;
    final Controller<ClientCommand> commandController; // ???

    HashMap<String, String> config; // for future serialization

    public Client() {
        cFSM = new ClientStateMachine(this);
        commandController = new Controller<>(ClientCommand.class, this);
        commandController.setEnabled(HELP, SHUTDOWN, CONFIG);
        commandController.setMasked(HELP, SHUTDOWN, CONFIG);
        config = new HashMap<>();
        config.put("nick", "Bob");
        config.put("serverIP", "127.0.0.1");
        config.put("serverPort", "62442");
    }

    private Thread listenLocalThread, listenRemoteThread;

    /**
     * Listens for command line input on local machine
     */
    void listenLocal() {
        listenLocalThread = Thread.currentThread();

        System.out.println("\33[36m]--==: MTG Client started :==--[\33[0m");
        LOGGER.finest(String.format("local config: [%s %s:%s]", config.get("nick"), config.get("serverIP"), config.get(
                "serverPort")));

        cFSM.process(commandController.compile(ClientCommand.CONNECT, config.get("serverIP"), config.get("serverPort")));

        Scanner cliScanner = new Scanner(System.in);
        String  input;

        parseLoop:
        do {
            input = cliScanner.nextLine();
            if (input.isBlank()) continue;
            var compiled = commandController.parse(input);
            if (compiled.cmd == null)  // command unrecognized - send as raw input to server
                cFSM.process(compiled);
            else if (!commandController.isEnabled(compiled.cmd))  // command disabled
                System.out.println(Formatter.error("Command disabled."));
            else
                switch (compiled.cmd) {
                    case HELP: // TODO: /help <client|server|hub|comment> to display help for parts, and default is /help client
                        // return whole client's and server's help for enabled commands
                        if (compiled.args.length == 1) {
                            System.out.println(String.join(
                                    "\n",
                                    commandController.enabledCommands.stream()
                                            .map(c -> Formatter.YELLOW + c.help() + Formatter.RESET).toArray(String[]::new)));
                            cFSM.process(compiled);

                        } else {
                            // check if arg[0] is on client, print message if it is, send help request to server otherwise
                            var helpCmd = commandController.parse(compiled.arg(1)).cmd;
                            if (helpCmd != null || cFSM.getCurrentState() != CONNECTED) // help is on client/not connected
                                System.out.println(Formatter.YELLOW + commandController.help(compiled.arg(1)) + Formatter.RESET);
                            else
                                cFSM.process(compiled);
                        }
                        break;
                    case SHUTDOWN:
                        cFSM.process(commandController.compile(CONNECTION_CLOSE));
                        listenLocalThread.interrupt();
                        break parseLoop;
                    default: // not parsed passed to server
                        cFSM.process(compiled);
                }

        } while (!Thread.interrupted());

        System.out.println("\33[36m]--==: MTG Client stopped :==--[$\33[0m");
    }

    /**
     * Connects to a server defined in IP and port variables and listens for input
     */
    void listenRemote() {
        try (Socket socket = new Socket()) {

            this.socket = socket;
            listenRemoteThread = Thread.currentThread();

            socket.connect(new InetSocketAddress(config.get("serverIP"), Integer.parseInt(config.get("serverPort"))),
                    20 * 1000); // 20 sec await for connection...

            OutputStream out = socket.getOutputStream();
            InputStream  in  = socket.getInputStream();

            this.in = in;
            this.out = out;

            boolean watchdog; // to check if connection is alive
            keepAlive:
            do { // every 10 seconds a control PING packet is sent to the server
                watchdog = false;
                socket.setSoTimeout(10 * 1000); // the 10 seconds are hardcoded for any activity from server

                Message msg;
                try {
                    msg = Message.parseDelimitedFrom(in);
                    if (msg == null) break keepAlive; // connection closed by server
                    else watchdog = true;
                } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
                    // keepalive
                    transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.PING));
                    socket.setSoTimeout(10 * 1000); // 10 seconds to get reply from server
                    msg = Message.parseDelimitedFrom(in); // this throws SocketTimeoutException if nothing came
                    watchdog = true;
                }

                handleRemoteMessage(msg);
                // if it came to this line, the string was read
            } while (watchdog && !Thread.interrupted());

        } catch (ConnectException e) {
            Client.LOGGER.warning(e.getMessage());
            System.out.println(Formatter.error(String.format("Cannot connect to the server at %s:%s", config.get("serverIP"),
                    config.get("serverPort"))));
        } catch (IOException e) {
            Client.LOGGER.severe(e.getMessage());
            e.printStackTrace();
        } finally {
            cFSM.process(commandController.compile(CONNECTION_CLOSE));
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
            case CONTROLMSG: // respond
                switch (msg.getControlMsg().getCode()) {
                    case SERVER_HELLO:
                        cFSM.process(commandController.compile(SERVER_HELLO));
                        break;
                    case SERVER_DENY:
                        cFSM.process(commandController.compile(CONNECTION_CLOSE));
                        break;
                    case UNRECOGNIZED:
                        Client.LOGGER.warning("Received unrecognized control message.");
                        break;
                }
                break;
            case CONFIGMSG:
                var remoteConfig = msg.getConfigMsg().getConfigurationMap();
                String remoteIdentity = remoteConfig.get("remoteID");
                if (remoteIdentity != null) { // remote identity
                    config.put("remoteID", remoteIdentity);
                    LOGGER.info("Remote identity: " + remoteIdentity);
                }
                break;
            case CHATMSG:
                var chat = msg.getChatMsg();
                System.out.println(Formatter.chat(
                        chat.getFrom(),
                        chat.getTo(),
                        chat.getFrom().equals(config.get("remoteID")),
                        chat.getMsg()));
                break;
            case MSGTYPE_NOT_SET:
                Client.LOGGER.warning("Received empty message.");
                break;
        }
    }

    /**
     * Disconnects client from the server and halts the listening thread
     */
    void disconnect() {
        try {
            if (cFSM.getCurrentState() == CONNECTED)
                transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.CLIENT_DISCONNECT));
            socket.close();
            listenRemoteThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
            listenLocalThread.interrupt(); // re-set the interrupt status
        }
        Client.LOGGER.info("Disconnected.");
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


