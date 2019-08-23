package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import me.tooster.common.proto.Messages;
import me.tooster.common.proto.Messages.*;


import static me.tooster.client.ClientCommand.*;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
class ClientStateMachine extends FiniteStateMachine<ClientStateMachine.State, ClientStateMachine, Command.Compiled<ClientCommand>> {

    Client client;

    ClientStateMachine(Client client) {
        super(State.NOT_CONNECTED);
        this.client = client;
    }

    enum State implements FiniteStateMachine.State<State, ClientStateMachine, Command.Compiled<ClientCommand>> {
        NOT_CONNECTED {
            @Override
            public void onEnter(ClientStateMachine fsm, State prevState) {
                fsm.client.commandController.setEnabled(CONNECT);
            }

            @Override
            public State process(ClientStateMachine fsm, Command.Compiled<ClientCommand>... input) {
                var compiled = input[0];
                if (compiled.cmd == null) {
                    Client.LOGGER.warning("Unrecognized input.");
                    return this;
                }
                else if (compiled.cmd == CONNECT)
                    return CONNECTING;

                return this;
            }
        },

        CONNECTING {
            @Override
            public void onEnter(ClientStateMachine fsm, State prevState) {
                Client.LOGGER.info(String.format("Connecting to the server at %s:%s...",
                        fsm.client.config.get("serverIP"), fsm.client.config.get("serverPort")));
                fsm.client.commandController.setEnabled(DISCONNECT);
                new Thread(fsm.client::listenRemote).start();
            }

            @Override
            public State process(ClientStateMachine fsm, Compiled<ClientCommand>... input) {
                var compiled = input[0];
                if (compiled.cmd == null) {
                    Client.LOGGER.warning("Unrecognized input.");
                    return this;
                }
                switch (compiled.cmd) {
                    case DISCONNECT:
                    case CONNECTION_CLOSE:
                        fsm.client.disconnect();
                        return NOT_CONNECTED;
                    case SERVER_HELLO:
                        return CONNECTED;
                    default:
                        return this;
                }
            }

        },

        CONNECTED {
            @Override
            public void onEnter(ClientStateMachine fsm, State prevState) {

                Client.LOGGER.info("Connected to server.");
                fsm.client.commandController.setEnabled(DISCONNECT);
                fsm.client.transmit(ConfigMsg.newBuilder().putAllConfiguration(fsm.client.config)); // send initial configuration
            }

            @Override
            public State process(ClientStateMachine fsm, Compiled<ClientCommand>... input) {
                var compiled = input[0];
                if (compiled.cmd == null) {
                    fsm.client.transmit(Messages.CommandMsg.newBuilder().setCommand(compiled.arg(0))); // send raw input
                    return this;
                }
                switch (compiled.cmd) {
                    case CONFIG:
                        fsm.client.transmit(ConfigMsg.newBuilder().putConfiguration(compiled.arg(1), compiled.arg(2)));
                        System.out.println(compiled.toString());
                        return this;
                    case DISCONNECT:
                    case CONNECTION_CLOSE:
                        fsm.client.disconnect();
                        return NOT_CONNECTED;
                    default:
                        return this;
                }
            }
        },

    }
}
