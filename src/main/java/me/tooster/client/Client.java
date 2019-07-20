package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.CommandController;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Logger;

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
        cmdController = new Command.Controller<ClientCommand>();
        System.out.println("\33[36m]--==: MTG Client started :==--[$\33[0m");
    }


    @Override
    public void run() {

        cFSM.process(cmdController.compile(CHANGE_NAME, nick));
        if (IP != null)
            cFSM.process(cmdController.compile(CONNECT, IP, String.valueOf(port)));

        Scanner                         scanner = new Scanner(System.in);
        String                          input;
        Command.Compiled<ClientCommand> parsed;

        parseLoop:
        do {
            input = scanner.nextLine();
            parsed = cmdController.parse(input);

            switch (parsed.cmd) {
                case SHUTDOWN: break parseLoop;
                case HELP:
                    String[] help = cmdController.getEnabled()
            }

        } while (parsed)
        String input;
        if (!(input = scanner.nextLine()).isEmpty()) {

        }

        Socket socket = connect(args[1], Integer.parseInt(args[2]));
        if (socket != null)
            new Thread(new Receiver(socket)).start(); // create reader from server

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(args[2]); // send nick to server as config data
        out.flush();
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


