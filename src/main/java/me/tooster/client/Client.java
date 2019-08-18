package me.tooster.client;

import me.tooster.common.Formatter;
import me.tooster.common.proto.Messages.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
        cFSM = new ClientStateMachine();
        commandController = new Controller<>(ClientCommand.class, this);
        commandController.setEnabled(HELP, SHUTDOWN, CONFIG);
        commandController.setMasked(HELP, SHUTDOWN, CONFIG);
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

        System.out.println("\33[36m]--==: MTG Client started :==--[$\33[0m");
        LOGGER.finest(String.format("local config: [%s %s:%s]", config.get("nick"), config.get("serverIP"), config.get("serverPort")));

        cFSM.process(commandController.compile(ClientCommand.CONNECT, config.get("serverIP"), config.get("serverPort")));

        Scanner cliScanner = new Scanner(System.in);
        String input;

        parseLoop:
        do {
            input = cliScanner.nextLine();
            var compiled = commandController.parse(input);
            if (compiled.cmd == null)  // command unrecognized - send as raw input to server
                cFSM.process(compiled, this);
            else if (!commandController.isEnabled(compiled.cmd))  // command disabled
                System.out.println(Formatter.error("Command disabled."));
            else
                switch (compiled.cmd) {
                    case HELP: // TODO: /help <client|server|hub|comment> to display help for parts, and default is /help client
                        if (compiled.args.length == 0) {
                            // return whole client's and server's help
                            System.out.println(Formatter.YELLOW + String.join("\n",
                                    commandController.help())
                                    + Formatter.RESET);
                            cFSM.process(compiled);

                        } else {
                            // check if arg[0] is on client, print message if it is, send help request to server otherwise
                            if (ClientCommand.commands.contains(commandController.parse(compiled.arg(1)).cmd))
                                System.out.println(Formatter.YELLOW + String.join("\n",
                                        commandController.help())
                                        + Formatter.RESET);
                            else
                                cFSM.process(compiled);
                        }
                        break;
                    case SHUTDOWN:
                        disconnect();
                        listenLocalThread.interrupt();
                        break parseLoop;
                    default: // not parsed passed to server
                        cFSM.process(compiled, this);
                }

        } while (!Thread.interrupted());

        System.out.println("\33[36m]--==: MTG Client stopped :==--[$\33[0m");
    }

    /**
     * Connects to a server defined in IP and port variables and listens for input
     */
    void listenRemote() {
        try (Socket socket = new Socket();
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            listenRemoteThread = Thread.currentThread();

            socket.connect(new InetSocketAddress(config.get("serverIP"), Integer.parseInt(config.get("serverPort"))), 20 * 1000); // 20 sec await for connection...

            this.socket = socket;
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

            disconnect();
        } catch (IOException e) {
            Client.LOGGER.warning(e.getMessage());
            e.printStackTrace();
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
                        cFSM.process(commandController.compile(SERVER_DENY));
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
            listenRemoteThread.join(); // wait for thread
        } catch (InterruptedException | IOException e) {
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

        switch (args.length) {
            default:
            case 3:                client.config.put("serverIP", args[2]);
            case 2:                client.config.put("serverPort", args[1]);
            case 1:                client.config.put("nick", args[0]);
            case 0:
        }

        new Thread(client::listenLocal).start();
    }
}


