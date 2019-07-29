package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
class ClientStateMachine extends FiniteStateMachine<Command.Compiled<ClientCommand>, Client> {

    ClientStateMachine() { super(ClientStage.NOT_CONNECTED); }

    enum ClientStage implements State<Command.Compiled<ClientCommand>, Client> {
        NOT_CONNECTED {
            @Override
            public ClientStage process(Command.Compiled<ClientCommand> input, Client client) {
                switch (input.cmd) {
                    case CONNECT:
                        try (Socket sock = new Socket(client.IP, client.port);
                             PrintWriter out = new PrintWriter(sock.getOutputStream());
                             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

                            client.socket = sock;
                            sock.setSoTimeout(15 * 1000); // 15 second watchdog timeout

                            serverConnectionThread = new Thread(() -> {
                                try {
                                    String s;
                                    while ((s = in.readLine()) != null)
                                        System.out.println(s); // display server reply

                                    client.cFSM.process(client.cmdController.compile(ClientCommand.DISCONNECT));
                                } catch (IOException e) {
                                    Client.LOGGER.warning(e.getMessage());
                                    e.printStackTrace();
                                }


                            });
                            serverConnectionThread.start();

                            Client.LOGGER.info("Client disconnected from the server.");
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
            public ClientStage process(Command.Compiled<ClientCommand> input, Client client) {
                switch (input.cmd) {
                    case HELP:
                        client.out.println(input.toString());
                        return this;
                    case CHANGE_NAME:
                        ClientStage.NOT_CONNECTED.process(input, client);
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
