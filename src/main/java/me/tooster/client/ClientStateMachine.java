package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
class ClientStateMachine extends FiniteStateMachine<Command.Compiled<ClientCommand>, Client> {

    ClientStateMachine() { super(State.NOT_CONNECTED); }

    enum State implements FiniteStateMachine.State<Command.Compiled<ClientCommand>, Client> {
        NOT_CONNECTED {
            @Override
            public State process(Command.Compiled<ClientCommand> input, Client client) {
                switch (input.cmd) {
                    case CONNECT:
                        try (Socket sock = new Socket(client.IP, client.port);
                             PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

                            client.socket = sock;

                            serverConnectionThread = new Thread(() -> {
                                try {
                                    boolean watchdog; // to check if connection is alive
                                    keepAlive:
                                    do { // every 10 seconds a control PING packet is sent to the server
                                        watchdog = false;
                                        client.socket.setSoTimeout(10 * 1000); // the 10 seconds are hardcoded, while 20 are on

                                        String s;
                                        try {
                                            s = client.serverIn.readLine();
                                            if (s == null) break keepAlive; // connection closed by server
                                        } catch (SocketTimeoutException ignored) { // timeout reached without any data from server
                                            client.serverOut.println("PING"); // keepalive
                                            client.socket.setSoTimeout(10 * 1000); // 10 seconds to get reply from server
                                            s = client.serverIn.readLine(); // this throws SocketTimoutException if nothing came
                                            watchdog = true;
                                        }
                                        // if it came to this line, the string was read
                                        System.out.println(s); // display server reply

                                    } while (watchdog);

                                } catch (IOException e) {
                                    Client.LOGGER.warning(e.getMessage());
                                    e.printStackTrace();
                                }
                                client.cFSM.process(client.cmdController.compile(ClientCommand.DISCONNECT));

                                Client.LOGGER.info("Client disconnected from the server.");
                            });
                            serverConnectionThread.start();

                        } catch (SocketException e) {
                            Client.LOGGER.warning("Socket exception:");
                        } catch (IOException e) {
                            Client.LOGGER.severe("Error occurred while connecting to the server.");
                            e.printStackTrace();
                        }

                        return CONNECTED;

                    case CHANGE_NAME:
                        client.nick = input.args[0];
                        return this;

                    default:
                        return this;
                }
            }
        },
        CONNECTED {
            @Override
            public State process(Command.Compiled<ClientCommand> input, Client client) {
                switch (input.cmd) {
                    case HELP:
                        client.serverOut.println(input.toString());
                        return this;
                    case CHANGE_NAME:
                        State.NOT_CONNECTED.process(input, client);
                        System.out.println(input.toString());
                        return this;
                    case DISCONNECT:
                        try {
                            client.socket.close(); // close socket on connection end
                            Client.LOGGER.info("Disconnected from server.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return NOT_CONNECTED;
                    default:
                        return this;
                }
            }
        };

        Thread serverConnectionThread;

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

    }
}
