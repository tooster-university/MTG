package me.tooster.client;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
class ClientStateMachine extends FiniteStateMachine<Command.Compiled<ClientCommand>, Client> {

    ClientStateMachine() { super(ClientStage.NOT_CONNECTED); }

    enum ClientStage implements State<Command.Compiled<ClientCommand>, Client> {
        NOT_CONNECTED {
            @Override
            public ClientStage process(Command.Compiled<ClientCommand> input, Client context) {
                switch (input.cmd){
                    case HELP: break;
                    case CONNECT: break;
                    case CHANGE_NAME: break;
                    case SHUTDOWN: break;
                }
                return this;
            }
        },
        CONNECTED {
            @Override
            public ClientStage process(Command.Compiled<ClientCommand> input, Client context) {
                return null;
            }
        }

    }
}
