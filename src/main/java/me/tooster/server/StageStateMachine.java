package me.tooster.server;

import me.tooster.common.FiniteStateMachine;
import me.tooster.common.MessageFormatter;
import me.tooster.server.MTG.Deck;
import me.tooster.server.MTG.GameStateMachine;
import me.tooster.server.exceptions.CardException;
import me.tooster.server.exceptions.DeckException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

class StageStateMachine extends FiniteStateMachine<ServerCommand.Compiled, Hub> {

    StageStateMachine() { super(Stage.PREPARE); }
    enum Stage implements FiniteStateMachine.State<ServerCommand.Compiled, Hub> {
        PREPARE { // players can import decks and select a deck.


            @Override
            public Stage process(ServerCommand.Compiled cc, Hub hub) {
                switch (cc.command) {
                    case LIST_DECKS: {
                        String[] decks = ResourceManager.getInstance().getDecks().toArray(new String[]{});
                        Arrays.sort(decks);
                        cc.getPlayer().transmit(MessageFormatter.response("Decks:\n" + MessageFormatter.list(decks)));
                        return this;
                    }
                    case SHOW_DECK: {
                        Set<Map.Entry<String, Object>> cards =
                                ResourceManager.getInstance().getDeck(cc.args[0]).entrySet();
                        String[] strings =
                                cards.stream().map(e -> e.getKey() + " x" + e.getValue()).toArray(String[]::new);
                        Arrays.sort(strings);

                        cc.getPlayer().transmit(MessageFormatter.response("Cards in '" + cc.args[0] + "':\n"
                                + MessageFormatter.list(strings)));
                        return this;
                    }
                    case SELECT_DECK: {
                        try {
                            Deck deck = Deck.build(cc.getPlayer(), cc.args[1]);
                            cc.getPlayer().setDeck(deck);
                        } catch (DeckException | CardException e) {
                            cc.getPlayer().transmit(MessageFormatter.error(e.getMessage()));
                        }
                        return this;
                    }
                    case READY:
                        if (cc.getPlayer().getDeck() == null) {
                            cc.getPlayer().transmit(MessageFormatter.error("You must select deck first."));
                            return this;
                        } else {
                            cc.getPlayer().getFlags().add(User.Flag.READY);
                            // hub.players cannot be empty here
                            if (!hub.getPlayers().stream().allMatch(p -> p.getFlags().contains(User.Flag.READY))) {
                                hub.broadcast(MessageFormatter.broadcast("Waiting for all players to be ready."));
                                return this;
                            }
                        }
                }
                hub.broadcast("Starting a game. " + hub.getPlayers().get(0) + " goes first.");
                hub.setGameFSM(new GameStateMachine());
                return GAME;
            }

            @Override
            public void onEnter(State prevState, Hub hub) {
                hub.broadcast(MessageFormatter.broadcast("Waiting for players."));
            }
        },
        // game phase with it's own state machine.
        GAME {
            @Override
            public Stage process(ServerCommand.Compiled cc, Hub hub) {
                if (cc.command == ServerCommand.END_GAME) {
                    return PREPARE;
                }
                return this; // same state
            }

            @Override
            public void onExit(State nextState, Hub hub) {
                hub.broadcast(MessageFormatter.broadcast("Winner: " + hub.getGameFSM().getWinner()));
            }
        };
    }
}
