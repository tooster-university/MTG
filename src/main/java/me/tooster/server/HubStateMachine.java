package me.tooster.server;

import me.tooster.MTG.MTGStateMachine;
import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.common.Formatter;

import me.tooster.MTG.GameStateMachine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static me.tooster.server.ServerCommand.*;
import static me.tooster.common.proto.Messages.*;


class HubStateMachine extends FiniteStateMachine<Command.Compiled<ServerCommand>, Hub> {


    HubStateMachine() { super(State.NOT_IN_GAME); }

    private MTGStateMachine mtgFSM;

    enum State implements FiniteStateMachine.State<Command.Compiled<ServerCommand>, Hub> {
        NOT_IN_GAME { // players can import decks and select a deck.

            @Override
            public State process(Command.Compiled<ServerCommand> command, Hub hub) {
                User user = (User) command.controller.owner;
                switch (command.cmd) {
                    case HUB_ADD_USER:
                        user.transmit(ChatMsg.newBuilder().setFrom("HUB").setMsg("When you are ready, use '/ready'."));
                        return this;
                    case READY:
                        if (user.setReady(!user.isReady())) {
                            synchronized (hub.users) {
                                if (hub.users.values().stream().filter(User::isReady).count() == hub.userSlots) {
                                    User[] players = (User[]) hub.users.values().toArray();
                                    Collections.shuffle(Arrays.asList(players));
                                    hub.hubFSM.mtgFSM = new MTGStateMachine(players);
                                    return IN_GAME;
                                }
                            }
                        }
                }
                return this;
            }

            @Override
            public void onEnter(FiniteStateMachine.State prevState, Hub hub) {
                hub.broadcast("Ready for next game.");
            }
        },
        // game phase with it's own state machine.
        IN_GAME {
            @Override
            public void onEnter(FiniteStateMachine.State<Compiled<ServerCommand>, Hub> prevState, Hub hub) {
                hub.broadcast("Game started.");
            }

            @Override
            public FiniteStateMachine.State<Compiled<ServerCommand>, Hub> process(Compiled<ServerCommand> input, Hub context) {
                return this;
            }

        };
    }
}
