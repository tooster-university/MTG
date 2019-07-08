package me.tooster.client;

import me.tooster.server.Player;
import me.tooster.server.Server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Logger;

public class Client {

    private static final Logger LOGGER;
    private String nick;
    private Socket socket;

    static {
        System.setProperty("java.util.logging.config.file",
                Server.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Server.class.getName());
    }


    // run with arguments <IP> <port> <nick>
    // main thread reads command line
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Pass <nick> <IP> <port> as arguments.");
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);
        String s;
        if (!(s = scanner.nextLine()).isEmpty())
            out.println(s);

        Socket socket = connect(args[1], Integer.parseInt(args[2]));
        if(socket != null)
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

        Socket socket;
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
}


