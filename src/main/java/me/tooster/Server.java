package me.tooster;

import me.tooster.MTG.Player;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {
    private static final Logger LOGGER;
    int port;

    static {
        System.setProperty("java.util.logging.config.file",
                Server.class.getClassLoader().getResource("logging.properties").getFile());
        LOGGER = Logger.getLogger(Server.class.getName());
    }

    // main thread listening for connections and creating connection-per-client threads
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Pass port as first argument.");
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
            LOGGER.info("Server started at " + port + ". Waiting for clients to connect...");
            ResourceManager.getInstance(); // prefetch decks
            // listen for clients
            int tag = 0;
            while (true) {
                Socket serverClient = server.accept();
                LOGGER.info( "Client " + ++tag + " connected.");
                ClientThread ct = new ClientThread(serverClient, new Hub(), tag);
                new Thread(ct).start();
            }
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            System.exit(1);
        }
        LOGGER.info("Server stopped.");
    }

    /**
     * Client listening and replying to user input
     */
    private static class ClientThread implements Runnable {

        Socket socket;
        Hub hub;
        int tag;
        Player player;

        /**
         * Creates new thread for Player
         *
         * @param inSocket socket on which server will listen for player's input
         * @param hub      hub to which the player connects
         * @param tag      server tag for player
         */
        ClientThread(Socket inSocket, Hub hub, int tag) {
            socket = inSocket;
            this.hub = hub;
            this.tag = tag;
        }

        // thread for listening to client
        @Override
        public void run() {

            ResourceManager.getInstance();
            // create new playable player with given tag and socket for transmission
            // player implements auto close-able so in case of socket failure, it is properly closed
            try (Player player = new Player(socket, tag)) {


                if (hub.addPlayer(player)) {
                    socket.setSoTimeout(0); // 3 minute timeout
                    player.listen();
                } else
                    player.transmit(Utils.formatError("Game is already in progress, cannot join the hub"));

            } catch (SocketTimeoutException | SocketException e) {
                LOGGER.info("Player timeouted or connection reset.");
            } catch (IOException e) {
                LOGGER.severe( "Socket error.");
                e.printStackTrace();
            } catch (Exception e) {
                LOGGER.severe("WTF happened.");
                e.printStackTrace();
            }
        }
    }
}