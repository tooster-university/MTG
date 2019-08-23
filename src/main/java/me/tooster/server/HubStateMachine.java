package me.tooster.server;

import me.tooster.MTG.MTGStateMachine;
import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import java.util.Arrays;
import java.util.Collections;

import static me.tooster.server.ServerCommand.*;
import static me.tooster.common.proto.Messages.*;


class HubStateMachine extends FiniteStateMachine<HubStateMachine.State, HubStateMachine, Command.Compiled<ServerCommand>> {


    private final Hub hub;

    HubStateMachine(Hub hub) { super(State.NOT_IN_GAME); this.hub = hub; }

    private MTGStateMachine mtgFSM;

    enum State implements FiniteStateMachine.State<State, HubStateMachine, Compiled<ServerCommand>> {
        NOT_IN_GAME { // players can import decks and select a deck.

            @Override
            public State process(HubStateMachine fsm, Command.Compiled<ServerCommand> ... command) {
                var cmd = command[0].cmd;
                User user = (User) command[0].controller.owner;
                switch (cmd) {
                    case HUB_ADD_USER:
                        user.transmit(ChatMsg.newBuilder().setFrom("HUB").setMsg("When you are ready, use '/ready'."));
                        return this;
                    case READY:
                        if (user.setReady(!user.isReady())) {
                            synchronized (fsm.hub.users) {
                                if (fsm.hub.users.values().stream().filter(User::isReady).count() == fsm.hub.userSlots) {
                                    User[] players = (User[]) fsm.hub.users.values().toArray();
                                    Collections.shuffle(Arrays.asList(players));
                                    fsm.hub.hubFSM.mtgFSM = new MTGStateMachine(players);
                                    return IN_GAME;
                                }
                            }
                        }
                }
                return this;
            }

            @Override
            public void onEnter(HubStateMachine fsm, State prevState) {
                if(fsm.hub != null) fsm.hub.broadcast("Ready for next game.");
            }
        },
        // game phase with it's own state machine.
        IN_GAME {
            @Override
            public void onEnter(HubStateMachine fsm, State prevState) {

                fsm.hub.broadcast("Game started.");
            }

            @Override
            public State process(HubStateMachine fsm, Command.Compiled<ServerCommand> ... command) {
                return this;
            }

        };
    }
}
