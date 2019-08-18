package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import me.tooster.common.proto.Messages;
import me.tooster.common.proto.Messages.*;


import static me.tooster.client.ClientCommand.*;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
class ClientStateMachine extends FiniteStateMachine<Command.Compiled<ClientCommand>, Client> {

    ClientStateMachine() { super(State.NOT_CONNECTED); }

    enum State implements FiniteStateMachine.State<Command.Compiled<ClientCommand>, Client> {
        NOT_CONNECTED {
            @Override
            public void onEnter(FiniteStateMachine.State<Command.Compiled<ClientCommand>, Client> prevState, Client client) {
                client.commandController.setEnabled(CONNECT);
            }

            @Override
            public State process(Command.Compiled<ClientCommand> input, Client client) {

                if (input.cmd == null) {
                    Client.LOGGER.warning("Unrecognized input.");
                    return this;
                }
                if (input.cmd == CONNECT)
                    return CONNECTING;

                return this;
            }
        },

        CONNECTING {
            @Override
            public void onEnter(FiniteStateMachine.State<Command.Compiled<ClientCommand>, Client> prevState, Client client) {
                Client.LOGGER.info(String.format("Connecting to the server at %s:%s...",
                        client.config.get("serverIP"), client.config.get("serverPort")));
                client.commandController.setEnabled(DISCONNECT);
                new Thread(client::listenRemote).start();
            }

            @Override
            public State process(Command.Compiled<ClientCommand> input, Client client) {
                if (input.cmd == null) {
                    Client.LOGGER.warning("Unrecognized input.");
                    return this;
                }
                switch (input.cmd) {
                    case DISCONNECT:
                        client.disconnect();
                        return NOT_CONNECTED;
                    case SERVER_HELLO:
                        return CONNECTED;
                    case SERVER_DENY:
                        Client.LOGGER.warning("Server denied connection.");
                        client.disconnect();
                        return NOT_CONNECTED;
                    default:
                        return this;
                }
            }
        },

        CONNECTED {
            @Override
            public void onEnter(FiniteStateMachine.State<Compiled<ClientCommand>, Client> prevState, Client client) {
                Client.LOGGER.info("Connected to server.");
                client.commandController.setEnabled(DISCONNECT);
                client.transmit(ConfigMsg.newBuilder().putAllConfiguration(client.config)); // send initial configuration
            }

            @Override
            public State process(Command.Compiled<ClientCommand> input, Client client) {
                if (input.cmd == null) {
                    client.transmit(Messages.CommandMsg.newBuilder().setCommand(input.arg(0))); // send raw input
                    return this;
                }
                switch (input.cmd) {
                    case CONFIG:
                        client.transmit(ConfigMsg.newBuilder().putConfiguration(input.arg(1), input.arg(2)));
                        System.out.println(input.toString());
                        return this;
                    case DISCONNECT:
                        client.disconnect();
                        return NOT_CONNECTED;
                    default:
                        return this;
                }
            }
        },


    }
}
