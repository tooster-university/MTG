package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.Formatter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static me.tooster.client.ClientCommand.*;

public class Client implements Runnable {

    private static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.config.file",
                Client.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Client.class.getName());
    }

    private final ClientStateMachine                cFSM;
    private final Command.Controller<ClientCommand> cmdController;

    private String nick;
    private String IP;
    private int    port;

    Client(@NotNull String nick, @NotNull String IP, int port) {
        this.nick = nick;
        this.IP = IP;
        this.port = port;
        cFSM = new ClientStateMachine();
        cmdController = new Command.Controller<>();
        System.out.println("\33[36m]--==: MTG Client started :==--[$\33[0m");
    }


    @Override
    public void run() {

        cFSM.process(cmdController.compile(CHANGE_NAME, nick));
        if (IP != null)
            cFSM.process(cmdController.compile(CONNECT, IP, String.valueOf(port)));

        Scanner scanner = new Scanner(System.in);
        String  input;

        parseLoop:
        do {
            input = scanner.nextLine();
            Command.Compiled<ClientCommand> compiled = cmdController.parse(input);

            switch (compiled.cmd) {
                case HELP:
                    if (compiled.args.length == 1 &&
                            Arrays.stream(cachedValues).anyMatch(c -> c.matches(compiled.args[0])))
                        System.out.println(ClientCommand.valueOf(compiled.args[0].toUpperCase()).help());
                    else {

                        String s = Formatter.YELLOW +
                                cmdController.enabledCommands
                                        .stream()
                                        .map(Enum::toString)
                                        .collect(Collectors.joining("\n")) + Formatter.RESET;
                        System.out.println(Formatter.UNDERLINE + "Client commands:\n" + Formatter.RESET + s);
                        cFSM.process(compiled);
                    }
                    break;
                case SHUTDOWN:
                    break parseLoop;
                default:
                    cFSM.process(compiled, this);
            }
        } while (true);

    }

    /**
     * Tries to connect to the server at given address and port with a timeout of 10 seconds
     *
     * @param host host of the server
     * @param port port of the server
     * @return Socket of the server if connection succeeded or null if it didn't
     */
    private static Socket connect(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 10 * 1000);
            return socket;
        } catch (UnknownHostException e) {
            System.err.println("Unknown server host.");
        } catch (IOException e) {
            System.err.println("Error connecting to the server.");
        }
        return null;
    }

    /**
     * Displays output to the screen on separate thread
     */
    private static class Receiver implements Runnable {

        Socket         socket;
        BufferedReader in;

        Receiver(Socket sockin) throws IOException {

            socket = sockin;
            in = new BufferedReader(new InputStreamReader(sockin.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String s;
                while ((s = in.readLine()) != null)
                    System.out.println(String.format(s)); // display server reply

                socket.close();

                LOGGER.info("Client disconnected.");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

    }

    //------------------------------------------------------------------------------------------------------------------

    // run with arguments <nick> [<IP> <port>]
    public static void main(String[] args) {
        if (args.length != 1 && args.length != 3) {
            System.err.println("Pass <nick> [<IP> <port>] as arguments.");
            System.exit(1);
        }

        Client client = new Client(args[0],
                args.length > 1 ? args[1] : "127.0.0.1",
                args.length > 1 ? Integer.parseInt(args[2]) : 62442);

        new Thread(client).start();
    }
}


