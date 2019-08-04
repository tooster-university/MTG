package me.tooster.server;

import me.tooster.common.Formatter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class Server implements Runnable {
    private static final Logger LOGGER;
    int port;
    Hub hub;

    static {
        System.setProperty("java.util.logging.config.file",
                Server.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Server.class.getName());
    }

    private Server() {}

    private static class SingletonHelper {
        private static final Server INSTANCE = new Server();
    }

    public static Server getInstance() { return SingletonHelper.INSTANCE; }

    // main thread listening for connections and creating connection-per-client threads
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Port for the server incoming connections must be specified");
            System.exit(1);
        }

//        Handler handlerObj = new ConsoleHandler();
//        handlerObj.setLevel(Level.ALL);
//        LOGGER.addHandler(handlerObj);
//        LOGGER.setLevel(Level.ALL);

        new Thread(new Server(Integer.parseInt(args[0]))).start(); // start the server
    }

    Server(int port) { this.port = port; }

    // server listening thread
    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            LOGGER.info("Server started at " + port + ".");

            LOGGER.info("Fetching data from resources...");
            ResourceManager.getInstance(); // prefetch decks

            LOGGER.info("Initializing the hub");
            hub = new Hub();
            // listen for clients
            LOGGER.info("Waiting for incoming client connections...");
            int tag = 0;
            while (true) {
                Socket userSocket = server.accept();
                LOGGER.info("Client " + ++tag + " connected.");

                User p = new User(userSocket, tag);
                new Thread(p).start();
                hub.addPlayer(p);
            }
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            System.exit(1);
        }
        LOGGER.info("Server stopped.");
    }
}