package me.tooster.MTG;

import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import me.tooster.MTG.models.DeckModel;
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

    //TODO: jestem na etapie ładowania decków z yamla do modeli -> potem trza sięwziąć w kolejności za SBA, stack, cast, tap, ATK, DEF

    private final Hub                 hub;
    private       int                 requiredReadyCount; // how many players must be ready to start a game
    public        Map<String, Object> config; // config loaded from main folder
    private final Map<User, Player>   playersData; // players in order
    private final Vector<User>        playersOrder;
    private       int                 turnPlayerIdx;
    private       int                 priorityPlayerIdx;

    public MTGStateMachine(Hub hub, int requiredReadyCount) {
        super(State.GAME_PREPARE);
        this.hub = hub;
        this.requiredReadyCount = requiredReadyCount;
        config = ResourceManager.instance().getConfig(); // TODO: validate and crash in start() if config structure is wrong
        playersData = new LinkedHashMap<>(requiredReadyCount);
        playersOrder = new Vector<>(requiredReadyCount);
    }

    /**
     * Tries to add a user to the game.
     * If joining is a success, commands are set accordingly.
     * Proper info is sent to user if join was unsuccessful.
     *
     * @param user user that want's to join the game
     * @return returns true if user was added and processed as candidate for the game
     */
    public synchronized boolean tryAddUser(User user) {
        user.mtgCommandController.setEnabled(DECK_SELECT, DECK_LIST, DECK_SHOW);
        user.mtgCommandController.setMasked(DECK_SELECT, DECK_LIST, DECK_SHOW);
        if (getCurrentState() == MTGStateMachine.State.GAME_PREPARE) {
            user.mtgCommandController.enable(READY);
            user.transmit(VisualMsg.newBuilder()
                    .setFrom("HUB")
                    .setTo(user.toString())
                    .setMsg("Use '" + READY.mainAlias() + "' when ready."));
            return true;
        }

        user.transmit(VisualMsg.newBuilder()
                .setVariant(VisualMsg.Variant.INVALID)
                .setMsg("Game already in progress, you have to wait for it to end."));
        return false;
    }

    /**
     * Removes user from game, triggers forfeit on a game and resets his state
     *
     * @param user
     */
    public synchronized void removeUser(User user) {
        if (getCurrentState() == State.GAME_PREPARE) process(user.mtgCommandController.compile(FORFEIT));
        user.mtgCommandController.disable(READY);
    }

    /**
     * @return Returns user with current priority.
     */
    public User getPriorityUser() {return playersOrder.get(priorityPlayerIdx);}

    /**
     * @return Returns user with current turn.
     */
    public User getTurnUser() {return playersOrder.get(turnPlayerIdx);}

    /**
     * Advance to the user with next priority.
     *
     * @param verbose if true, informs players who has priority
     * @return this priority's user after advance
     */
    private User passPriority(boolean verbose) {
        priorityPlayerIdx = (++priorityPlayerIdx) % playersOrder.size();
        User priorityPlayer = playersOrder.get(priorityPlayerIdx);
        hub.broadcast("%ss move.", priorityPlayer);
        return priorityPlayer;
    }

    /**
     * Advance to the user with next turn
     *
     * @return this turn's user after advance
     */
    private User passTurn() {
        turnPlayerIdx = (++turnPlayerIdx) % playersData.size();
        User turnPlayer = playersOrder.get(turnPlayerIdx);
        hub.broadcast("%ss turn begins.", turnPlayer);
        return turnPlayer;
    }

    /**
     * Transmits pile info to other user
     *
     * @param recipient user to send pile info to
     * @param pd
     * @param pile
     */
    private static void transmitPile(User recipient, Player pd, DeckModel.Pile pile) {
        pd.user.transmit(VisualMsg.newBuilder()
                .setVariant(VisualMsg.Variant.INFO)
                .setMsg(String.format("%s %s: [%d]%s",
                        recipient == pd.user ? "your " : pd.user,
                        pile.name().toLowerCase(),
                        pd.deck.piles.get(pile).size(),
                        Formatter.list(pd.deck.piles.get(pile).toArray()))));
    }

    @Override
    public void prepcocess(Compiled<MTGCommand>... input) {
        User user = (User) input[0].controller.owner;
        if (user != null && !input[0].isEnabled()) throw new CommandDisabledException(input[0].cmd);
    }

    public enum State implements FiniteStateMachine.State<State, MTGStateMachine, Compiled<MTGCommand>> {
        GAME_PREPARE {
            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) {
                fsm.turnPlayerIdx = fsm.priorityPlayerIdx = 0;
                fsm.playersData.clear();
                fsm.playersOrder.clear();
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) {
                var cmd = input[0].cmd;
                User user = (User) input[0].controller.owner;
                if (cmd == null) return this;
                switch (cmd) {

                    case DECK_LIST: {
                        String[] decks = ResourceManager.instance().getLoadedDecks().toArray(new String[]{});
                        Arrays.sort(decks);
                        user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INFO)
                                .setMsg("Decks:\n" + String.join("\n", decks)));
                        break;
                    }

                    case DECK_SHOW: {
                        try {
                            String deckName = input[0].arg(1);
                            deckName = deckName.isBlank() ? user.config.get("deck") : deckName;
                            if (deckName.isBlank()) {
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.INVALID)
                                        .setMsg("You have to specify deck's name."));
                                return this;
                            }
                            var cards = ((Map<String, Integer>) ResourceManager.instance().getDeckModel(deckName).piles.get(DeckModel.Pile.LIBRARY)).entrySet();
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
                        break;
                    }

                    case DECK_SELECT: {
                        try {
                            String deckName = input[0].arg(2);

                            if (deckName.isBlank()) {
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.INVALID)
                                        .setMsg("You have to specify deck's name."));
                                return this;
                            }
                            user.config.put("deck", deckName); // safe deck name in user's config for future use
                            user.transmit(VisualMsg.newBuilder()
                                    .setFrom("HUB")
                                    .setMsg("Selected deck: '" + deckName + "'"));

                            if (!ResourceManager.instance().getLoadedDecks().contains(deckName))
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.WARNING)
                                        .setMsg("Deck " + deckName + " hasn't been imported."));

                        } catch (DeckException e) {
                            user.transmit(VisualMsg.newBuilder()
                                    .setVariant(VisualMsg.Variant.ERROR)
                                    .setMsg(e.getMessage()));
                            Server.LOGGER.warning(e.getMessage());
                        } catch (CardException e) {
                            Server.LOGGER.severe(e.getMessage());
                        }
                        break;
                    }

                    case READY: {
                        boolean wasReady;
                        boolean isReady = false;
                        if (fsm.playersData.containsKey(user)) {
                            wasReady = true;
                            fsm.playersData.remove(user);
                        } else {
                            wasReady = false;
                            try {
                                var pd = new Player(user);
                                pd.deck = Deck.build(Player::nextID, pd, ResourceManager.instance().getDeckModel(user.config.get("deck")));
                                fsm.playersData.put(user, pd);
                                isReady = true;
                            } catch (DeckException | CardException e) {
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.ERROR)
                                        .setMsg(e.getMessage()));
                                Server.LOGGER.warning(e.getMessage());
                            }
                        }

                        if (wasReady != isReady)
                            fsm.hub.broadcast("%s is %s. Ready players: %s", user, isReady ? "ready" : "not ready",
                                    Formatter.formatProgress(fsm.playersData.size(), fsm.requiredReadyCount));

                        if (fsm.playersData.size() == fsm.requiredReadyCount) return DRAW_HAND;

                        break;
                    }

                }
                return this;
            }

            @Override
            public void onExit(MTGStateMachine fsm, State nextState) {
                fsm.playersData.forEach((user, value) -> {
                    fsm.playersOrder.add(user);
                    user.mtgCommandController.setEnabled(FORFEIT);
                });

            }
        }, // deck select etc.

        DRAW_HAND {
            /**
             * Performs paris mulligan for player. It returns a hand to library, shuffles it and draws hand with one card less.
             * If number of cards in hand is 0, no further mulligans can take place
             * @param pd data of player to perform mulligan on
             */
            private void parisMulligan(Player pd) {
                int size = pd.deck.piles.get(DeckModel.Pile.HAND).size();
                pd.deck.reset();
                Collections.shuffle(pd.deck.piles.get(DeckModel.Pile.LIBRARY));
                drawHand(pd, size - 1);
                if (size == 0) pd.handChoosen = true; // force start if hand size is 0
            }

            /**
             * Performs vancouver mulligan, same as paris mulligan but if mulligan was issued, player gains opportunity to scry 1
             * @param pd data of player to perform mulligan on
             */
            private void vancouverMulligan(Player pd) { // same as paris, but adds `scry 1` if mulligan was taken
                parisMulligan(pd);
                pd.scry = 1;
            }

            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) {
                fsm.hub.broadcast("Game started. Order: %s", fsm.playersOrder.toString());
                fsm.playersData.values().forEach(pd -> drawHand(pd, (int) fsm.config.get("max_hand")));
                fsm.getPriorityUser().mtgCommandController.enable(MULLIGAN, KEEP);
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) { // right now it's paris mulligan
                var cmd = input[0].cmd;
                if (cmd == null) return this;
                User user = (User) input[0].controller.owner;
                Player pd = fsm.playersData.get(user);
                if (!pd.handChoosen)
                    fsm.hub.broadcast("%s decided to %s their hand.", user.toString(), cmd == KEEP ? "keep" : "mulligan");
                if (cmd == KEEP) pd.handChoosen = true;
                else pd.mulligansTaken++;

                fsm.getPriorityUser().mtgCommandController.disable(MULLIGAN, KEEP);
                do {fsm.passPriority(false);}
                while (fsm.playersData.get(fsm.getPriorityUser()).handChoosen && fsm.getPriorityUser() != fsm.playersOrder.get(0));

                if (fsm.getPriorityUser() == fsm.playersOrder.get(0)) { // new round of mulligans
                    fsm.playersData.values().stream()
                            .filter(_pd -> !_pd.handChoosen).forEach(this::parisMulligan);
                }
                fsm.getPriorityUser().mtgCommandController.enable(MULLIGAN, KEEP);
                return fsm.playersData.values().stream().allMatch(_pd -> _pd.handChoosen) ? UNTAP : this; // start game if hands picked
            }
        }, // mulligansUsed phase

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

        /**
         * Draws cards from the top of user's library to hand and informs hub about it.
         * @param pd data of player that draws cards
         * @param amount
         */
        private static void drawHand(Player pd, int amount) {
            for (int i = 0; i < amount; i++)
                pd.deck.move(DeckModel.Pile.LIBRARY, 0, DeckModel.Pile.HAND, 0);
            pd.user.hub.broadcast("%s drew %d %s", pd.user, amount, amount == 1 ? "card" : "cards");
            MTGStateMachine.transmitPile(pd.user, pd, DeckModel.Pile.HAND);
        }

        @Override
        @Deprecated
        public State process(MTGStateMachine fsm, Command.Compiled<MTGCommand>... input) {
            return null;
        }

    }
}
