package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

import me.tooster.common.Formatter;
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
                    System.out.println(Formatter.invalid("Unrecognized input."));
                    return this;
                } else if (compiled.cmd == CONNECT) {
                    fsm.client.serverIP = compiled.arg(1);
                    try {fsm.client.serverPort = Integer.parseInt(compiled.arg(2));} catch (NumberFormatException ignored) {}
                    if (fsm.client.serverIP.isBlank()) fsm.client.serverIP = fsm.client.config.get("serverIP");
                    if (fsm.client.serverPort == null) fsm.client.serverPort = Integer.valueOf(fsm.client.config.get("serverPort"));
                    return CONNECTING;
                }

                return this;
            }
        },

        CONNECTING {
            @Override
            public void onEnter(ClientStateMachine fsm, State prevState) {
                Client.LOGGER.info(String.format("Connecting to the server at %s:%s...", fsm.client.serverIP, fsm.client.serverPort));
                fsm.client.commandController.setEnabled(DISCONNECT);
                new Thread(fsm.client::listenRemote).start();
            }

            @Override
            public State process(ClientStateMachine fsm, Compiled<ClientCommand>... input) {
                var compiled = input[0];
                if (compiled.cmd == null) {
                    System.out.println(Formatter.invalid("Unrecognized input."));
                    return this;
                }
                switch (compiled.cmd) {
                    case DISCONNECT:
                    case CONNECTION_CLOSED:
                        fsm.client.disconnect();
                        return NOT_CONNECTED;
                    case CONNECTION_ESABLISHED:
                        return CONNECTED;
                    default:
                        return this;
                }
            }

        },

        CONNECTED {
            @Override
            public void onEnter(ClientStateMachine fsm, State prevState) {

                System.out.println("Connected to the server as " + fsm.client.remoteConfig.get("identity"));
                fsm.client.commandController.setEnabled(DISCONNECT);
            }

            @Override
            public State process(ClientStateMachine fsm, Compiled<ClientCommand>... input) {
                var compiled = input[0];
                if (compiled.cmd == null) {
                    fsm.client.transmit(Messages.CommandMsg.newBuilder().setCommand(compiled.arg(0))); // send raw input
                    return this;
                }
                switch (compiled.cmd) {
                    case CONFIG: // send only changed option
                        if (compiled.args.length < 3) fsm.client.transmit(ControlMsg.newBuilder().setCode(ControlMsg.Code.CONFIG));
                        else fsm.client.transmit(ControlMsg.newBuilder()
                                .setCode(ControlMsg.Code.CONFIG)
                                .putConfiguration(compiled.arg(1), compiled.arg(2)));
                        return this;
                    case DISCONNECT:
                    case CONNECTION_CLOSED:
                        fsm.client.disconnect();
                        return NOT_CONNECTED;
                    default:
                        return this;
                }
            }
        },

    }
}
