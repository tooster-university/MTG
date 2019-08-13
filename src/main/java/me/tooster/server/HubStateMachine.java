package me.tooster.server;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.common.Formatter;
import me.tooster.server.MTG.GameStateMachine;


class HubStateMachine extends FiniteStateMachine<Command.Compiled<ServerCommand>, User> {

    Hub hub;
    HubStateMachine(Hub hub) { this.hub = hub; super(State.NOT_IN_GAME); }

    enum State implements FiniteStateMachine.State<Command.Compiled<ServerCommand>, User> {
        NOT_IN_GAME { // players can import decks and select a deck.

            @Override
            public State process(Command.Compiled<ServerCommand> cc, User user) {
                switch (cc.cmd) {
                    case HELP:
                        ServerCommand c = null;
                        try {
                            if (cc.args.length > 0)
                                c = ServerCommand.valueOf(cc.args[0].toUpperCase());
                        } catch (IllegalArgumentException ignored) { }
                        user.transmit(String.join("\n", user.cmdController.help(c)));
                        return this;
                    case PING:
                        user.transmit("PONG");
                        return this;
                    case DISCONNECT:
                        hub.
                        break;
                    case SHOUT: break;
                    case WHISPER: break;
                }
                hub.broadcast("Starting a game. " + hub.getUsers().get(0) + " goes first.");
                hub.setGameFSM(new GameStateMachine());
                return IN_GAME;
            }

            @Override
            public void onEnter(FiniteStateMachine.State prevState, Hub hub) {
                hub.broadcast(Formatter.broadcast("Waiting for players."));
            }
        },
        // game phase with it's own state machine.
        IN_GAME {
            @Override
            public State process(ServerCommand.Compiled, User user) {
                if (cc.command == ServerCommand.END_GAME) {
                    return NOT_IN_GAME;
                }
                return this; // same state
            }

            @Override
            public void onExit(FiniteStateMachine.State nextState, Hub hub) {
                hub.broadcast(Formatter.broadcast("Winner: " + hub.getGameFSM().getWinner()));
            }
        };
    }
}
