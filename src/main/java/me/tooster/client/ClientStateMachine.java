package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

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
                        serverConnectionThread = new Thread(() -> {

                            try (Socket sock = new Socket(client.IP, client.port)) {
                                client.socket = sock;
                                BufferedReader in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                                PrintWriter    out = new PrintWriter(sock.getOutputStream());

                                sock.setSoTimeout(15 * 1000); // 15 second watchdog timeout
                                String s;
                                while ((s = in.readLine()) != null)
                                    System.out.println(s); // display server reply

                                Client.LOGGER.info("Client disconnected from the server.");
                            } catch (IOException e) {
                                Client.LOGGER.severe("Error occurred while connecting to server.");
                                e.printStackTrace();
                            }
                        });
                        serverConnectionThread.start();
                        return CONNECTED;
                    case CHANGE_NAME:
                        client.nick = input.args[0];
                        return this;
                    case SHUTDOWN:
                        try {
                            client.socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return this;
                    default:
                        return this;
                }
            }
        },
        CONNECTED {
            @Override
            public ClientStage process(Command.Compiled<ClientCommand> input, Client client) {

                return this;
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
