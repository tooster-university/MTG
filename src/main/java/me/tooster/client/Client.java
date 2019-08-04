package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.Formatter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static me.tooster.client.ClientCommand.*;

public class Client implements Runnable {

    protected static final Logger LOGGER;

    static {
        System.setProperty("java.util.logging.config.file",
                Client.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Client.class.getName());
    }

    protected final ClientStateMachine                cFSM;
    protected final Command.Controller<ClientCommand> cmdController;

    protected String         nick;
    protected Socket         socket;
    protected PrintWriter    serverOut;
    protected BufferedReader serverIn;
    protected String         IP;
    protected int            port;

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
                    // if args == 0 -> return this whole help and servers
                    // if args > 0 -> check if arg[0] parses, print message if yes, if no send help request to server
                    ClientCommand c = null;
                    if (compiled.args.length > 0) {
                        try {
                            c = ClientCommand.valueOf(compiled.args[0].toUpperCase());
                            System.out.println(Formatter.UNDERLINE + "Client commands:\n" + Formatter.RESET
                                    + Formatter.YELLOW + String.join("\n", cmdController.help(c)) + Formatter.RESET);
                        } catch (IllegalArgumentException ignored) { // no such client command
                            cFSM.process(compiled);
                        }
                    }
                    break;
                case SHUTDOWN:
                    break parseLoop;
                default: // unparsed passed to server
                    cFSM.process(compiled, this);
            }
        } while (true);

        System.out.println("\33[36m]--==: MTG Client stopped :==--[$\33[0m");
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


