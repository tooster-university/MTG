package me.tooster.MTG;

import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.common.Formatter;
import me.tooster.server.Hub;
import me.tooster.server.ResourceManager;
import me.tooster.server.Server;
import me.tooster.server.User;

import java.util.*;

import static me.tooster.MTG.MTGCommand.*;
import static me.tooster.common.proto.Messages.*;

public class MTGStateMachine extends FiniteStateMachine<MTGStateMachine.State, MTGStateMachine, Command.Compiled<MTGCommand>> {

    private final Hub hub;

    private int readyCount; // how many players are currently ready
    private int requiredReadyCount; // how many players must be ready to start a game

    private final Vector<User> users; // users in order. Who /readies first goes first
    private       int          userTurn; // index of user with turn
    private       int          userPriority; // index of player with priority

    public MTGStateMachine(Hub hub, int requiredReadyCount) {
        super(State.GAME_PREPARE);
        this.hub = hub;
        users = new Vector<>(requiredReadyCount);
        this.requiredReadyCount = requiredReadyCount;

    }

    /**
     * Advance to the user with next priority
     *
     * @return this priority's user after advance
     */
    public User nextPriority() {
        userPriority = (++userPriority) % users.size();
        return users.get(userPriority);
    }

    /**
     * Advance to the user with next turn
     *
     * @return this turn's user after advance
     */
    public User nextTurn() {
        userTurn = (++userTurn) % users.size();
        return users.get(userTurn);
    }



    public enum State implements FiniteStateMachine.State<State, MTGStateMachine, Compiled<MTGCommand>> {
        GAME_PREPARE {
            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) {
                fsm.readyCount = fsm.userTurn = fsm.userPriority = 0;
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) {
                var cmd = input[0].cmd;
                User user = (User) input[0].controller.owner;
                if (cmd == null) return this;
                switch (cmd) {

                    case DECK_LIST: {
                        String[] decks = ResourceManager.getInstance().getDecks().toArray(new String[]{});
                        Arrays.sort(decks);
                        user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INFO)
                                .setMsg("Decks:\n" + String.join("\n", decks)));
                        break;
                    }

                    case DECK_SHOW: {
                        try {
                            String deckName = input[0].arg(2);
                            Set<Map.Entry<String, Object>> cards = ResourceManager.getInstance().getDeck(deckName).entrySet();
                            String[] strings = cards.stream().map(e -> e.getKey() + " x" + e.getValue()).toArray(String[]::new);
                            Arrays.sort(strings);
                            user.transmit(VisualMsg.newBuilder()
                                    .setVariant(VisualMsg.Variant.INFO)
                                    .setMsg(String.format("Deck '%s':\n%s", deckName, Formatter.list(strings))));
                        } catch (DeckException e) {
                            user.transmit(VisualMsg.newBuilder()
                                    .setVariant(VisualMsg.Variant.ERROR)
                                    .setMsg(e.getMessage()));
                        }
                    }

                    case DECK_SELECT: {
                        try {
                            String deckName = input[0].arg(2);
                            Deck.build(user, deckName);
                        } catch (DeckException e) {
                            user.transmit(VisualMsg.newBuilder()
                                    .setVariant(VisualMsg.Variant.ERROR)
                                    .setMsg(e.getMessage()));
                        } catch (CardException e) {
                            Server.LOGGER.severe(e.getMessage());
                        }
                        break;
                    }

                    case READY: {
                        var wasReady = user.isReady();
                        var isReady = user.setReady(!user.isReady());
                        if (!wasReady && !isReady)
                            user.transmit(VisualMsg.newBuilder()
                                    .setVariant(VisualMsg.Variant.INVALID)
                                    .setMsg("You have to pick a deck first."));
                        else {
                            if (isReady) {
                                fsm.readyCount += 1;
                                fsm.users.add(user);
                                user.mtgCommandController.disable(READY);
                            } else {
                                fsm.readyCount -= 1;
                                fsm.users.remove(user);
                                user.mtgCommandController.enable(READY);
                            }

                            fsm.hub.broadcast(user.toString() + " is " + (user.isReady() ? "ready " : "not ready ")
                                    + Formatter.formatProgress(fsm.readyCount, fsm.requiredReadyCount));

                            if (fsm.readyCount == fsm.requiredReadyCount)
                                return DRAW_HAND;
                        }
                        break;
                    }

                }
                return this;
            }
        }, // deck select etc.

        DRAW_HAND, // mulligans phase

        // game gamePhase
        UNTAP,
        UPKEEP,
        DRAW,

        MAIN_1,

        COMBAT_BEGIN,
        COMBAT_ATTACKERS,

        COMBAT_BLOCKERS,

        COMBAT_FIRST_STRIKE_DAMAGE,
        COMBAT_DAMAGE,
        COMBAT_END,

        MAIN_2,

        END_STEP,
        CLEANUP_STEP;

        @Override
        @Deprecated
        public State process(MTGStateMachine fsm, Command.Compiled<MTGCommand>... input) {
            return null;
        }

    }
}
