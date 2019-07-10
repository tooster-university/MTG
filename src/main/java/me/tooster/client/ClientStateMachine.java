package me.tooster.client;

import me.tooster.common.FiniteStateMachine;

/**
 * See the {@link me.tooster.common.FiniteStateMachine me.tooster.common.FiniteStateMachine&lt;I, C&gt;}
 */
public class ClientStateMachine extends FiniteStateMachine<ClientCommand.Compiled, Client> {

    protected ClientStateMachine() {
        super(ClientStage.NOT_CONNECTED);
    }

    enum ClientStage implements State<ClientCommand.Compiled, Client>{
        NOT_CONNECTED{
            @Override
            public ClientStage process(ClientCommand.Compiled input, Client context) {
                return null;
            }
        },
        CONNECTED{
            @Override
            public ClientStage process(ClientCommand.Compiled input, Client context) {
                return null;
            }
        }

    }
}
