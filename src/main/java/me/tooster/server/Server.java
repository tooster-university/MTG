package me.tooster.server;

import me.tooster.common.proto.Messages;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static me.tooster.common.proto.Messages.*;
import static me.tooster.server.ServerCommand.*;


public class Server {
    public static final Logger LOGGER;

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


    //----------------------------------------------------------------------------------------------------------------------------
    int port = 62442;
    Hub hub; // TODO: list of hubs and /join <hub> and /leave server commands

    private static long            tag   = 0;
    final          Map<Long, User> users = new ConcurrentHashMap<>();

    /**
     * @return next unique tag in numerical order
     */
    public static long nextTag() { return ++tag; }

    /**
     * Listens for client connections and accepts/denies them
     */
    private void awaitClients() {
        try (ServerSocket server = new ServerSocket(port)) {
            LOGGER.info("Server started at port" + port + ".");

            LOGGER.info("Fetching data from resources...");
            ResourceManager.getInstance(); // prefetch decks

            LOGGER.info("Initializing the hub.");
            hub = new Hub();
            // listen for clients
            LOGGER.info("Waiting for incoming client connections...");
            while (!Thread.interrupted()) {
                Socket userSocket = server.accept();

                User user = new User(userSocket, nextTag());
                user.transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.SERVER_HELLO));
                users.put(user.serverTag, user); // register in users map
                LOGGER.info("Client " + user.serverTag + " connected.");

                hub.addUser(user);

            }
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        LOGGER.info("Server stopped.");
    }

    /**
     * Broadcasts message to all users on the server
     *
     * @param msg message to broadcast
     */
    synchronized void broadcast(String msg) {
        users.values().forEach(u -> u.transmit(ChatMsg.newBuilder().setFrom("SERVER").setMsg(msg)));
    }

    /**
     * Returns user based on some identity. can be a tag or starting letters of nick or full name
     *
     * @param identity any of tag, nick or starting identity of player
     * @return user if exactly one user is found, null otherwise
     */
    public User findUser(String identity) {
        User u = null;
        try { u = users.get(Long.parseLong(identity)); } catch (NumberFormatException ignored) {}
        if (u != null) return u;
        var matching = users.values().stream().filter(id -> id.toString().startsWith(identity));
        if (matching.count() == 1) return matching.findFirst().get();
        return null;
    }

    //----------------------------------------------------------------------------------------------------------------------------
    // main thread listening for connections and creating connection-per-client threads
    public static void main(String[] args) {
        if (args.length > 0) Server.getInstance().port = Integer.parseInt(args[0]);

        //        Handler handlerObj = new ConsoleHandler();
        //        handlerObj.setLevel(Level.ALL);
        //        LOGGER.addHandler(handlerObj);
        //        LOGGER.setLevel(Level.ALL);

        new Thread(Server.getInstance()::awaitClients).start(); // start the server
    }

}