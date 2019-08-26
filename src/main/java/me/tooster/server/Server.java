package me.tooster.server;

import me.tooster.common.ChatRoom;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static me.tooster.common.proto.Messages.*;


public class Server implements ChatRoom<User> {
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

            LOGGER.fine("Fetching data from resources...");
            ResourceManager.getInstance(); // prefetch decks

            LOGGER.fine("Initializing the hub.");
            hub = new Hub();
            // listen for clients
            LOGGER.fine("Waiting for incoming client connections...");
            while (!Thread.interrupted()) {
                Socket userSocket = server.accept();
                User user = new User(userSocket, nextTag());

                (user.listenRemoteThread = new Thread(user::listenRemote)).start();

            }
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        LOGGER.info("Server stopped.");
    }

    /**
     * Returns user based on some identity. can be a tag or starting letters of nick or full name
     *
     * @param identity any of tag, nick or starting identity of player
     * @return returns user if exactly one user is found, null otherwise
     */
    public User findUser(String identity) {
        User u = null;
        //return by tag
        try { u = users.get(Long.parseLong(identity)); } catch (NumberFormatException ignored) {}
        if (u != null) return u;
        // return by name
        var matching = users.values().stream().filter(id -> id.toString().startsWith(identity)).toArray();
        if (matching.length != 1) return null;
        return (User) matching[0];
    }

    @Override
    public void broadcast(String message) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(VisualMsg.newBuilder()
                    .setVariant(VisualMsg.Variant.CHAT)
                    .setFrom("SERVER")
                    .setTo("SERVER")
                    .setMsg(message)));
        }
    }

    @Override
    public void shout(User from, String message) {
        synchronized (users) {
            users.values().forEach(u -> u.transmit(VisualMsg.newBuilder()
                    .setVariant(VisualMsg.Variant.CHAT)
                    .setFrom(from.toString())
                    .setTo("SERVER")
                    .setMsg(message)));
        }
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