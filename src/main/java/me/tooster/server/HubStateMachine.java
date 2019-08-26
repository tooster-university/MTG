package me.tooster.server;

import me.tooster.MTG.MTGStateMachine;
import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.common.Formatter;

import java.util.Arrays;
import java.util.Collections;

import static me.tooster.server.ServerCommand.*;
import static me.tooster.common.proto.Messages.*;


class HubStateMachine extends FiniteStateMachine<HubStateMachine.State, HubStateMachine, Command.Compiled<ServerCommand>> {


    private final Hub hub;

    HubStateMachine(Hub hub) {
        super(State.NOT_IN_GAME);
        this.hub = hub;
    }

    private MTGStateMachine mtgFSM;

    enum State implements FiniteStateMachine.State<State, HubStateMachine, Compiled<ServerCommand>> {
        NOT_IN_GAME { // players can import decks and select a deck.

            @Override
            public void onEnter(HubStateMachine fsm, State prevState) {
                if (fsm.hub != null) fsm.hub.broadcast("Waiting for players to get ready for a new game.");
            }

            @Override
            public State process(HubStateMachine fsm, Command.Compiled<ServerCommand>... command) {
                var cmd = command[0].cmd;
                User user = (User) command[0].controller.owner;
                switch (cmd) {
                    case HUB_ADD_USER:
                        user.serverCommandController.enable(READY);
                        user.setReady(false);
                        user.transmit(VisualMsg.newBuilder().setFrom("HUB").setMsg("When you are ready, use '" + READY.mainAlias() + "'."));
                        return this;
                    case READY:
                        synchronized (fsm.hub.users) {
                            var wasReady = user.isReady();
                            user.setReady(!user.isReady());
                            if (wasReady == user.isReady())
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.INVALID)
                                        .setMsg("You didn't pick a deck yet."));
                            else {
                                int readyCount = (int) fsm.hub.users.values().stream().filter(User::isReady).count();
                                fsm.hub.broadcast(user.toString() + " is " + (user.isReady() ? "ready" : "not ready")
                                        + Formatter.formatProgress(readyCount, fsm.hub.userSlots));
                                if (readyCount == fsm.hub.userSlots) {
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
        },
        // game phase with it's own state machine.
        IN_GAME {
            @Override
            public void onEnter(HubStateMachine fsm, State prevState) {
                synchronized (fsm.hub.users) {
                    for (User user : fsm.hub.users.values())
                        user.serverCommandController.disable(READY);
                }
                fsm.hub.broadcast("Game started.");
            }

            @Override
            public State process(HubStateMachine fsm, Command.Compiled<ServerCommand>... command) {
                return this;
            }

        };
    }
}
