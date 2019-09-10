package me.tooster.MTG;

import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import me.tooster.MTG.exceptions.InsufficientManaException;
import me.tooster.MTG.exceptions.ManaFormatException;
import me.tooster.MTG.models.CardModel;
import me.tooster.MTG.models.DeckModel;
import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.common.Formatter;
import me.tooster.server.Hub;
import me.tooster.server.ResourceManager;
import me.tooster.server.Server;
import me.tooster.server.User;

import java.util.*;
import java.util.stream.Collectors;

import static me.tooster.MTG.MTGCommand.*;
import static me.tooster.common.proto.Messages.*;

public class MTGStateMachine extends FiniteStateMachine<MTGStateMachine.State, MTGStateMachine, Command.Compiled<MTGCommand>> {

    private final Hub                 hub;
    private       int                 requiredReadyCount; // how many players must be ready to start a game
    public        Map<String, Object> config; // config loaded from main folder
    private final Map<User, Player>   playersData; // players in order
    private final Vector<Player>      playersOrder;
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
     * @param user user to remove
     */
    public synchronized void removeUser(User user) {
        if (getCurrentState() == State.GAME_PREPARE) process(user.mtgCommandController.compile(FORFEIT));
        user.mtgCommandController.disable(READY);
    }

    /**
     * @return Returns user with current priority.
     */
    public Player getPriorityPlayer() {return playersOrder.get(priorityPlayerIdx);}

    /**
     * @return Returns user with current turn.
     */
    public Player getTurnUser() {return playersOrder.get(turnPlayerIdx);}

    /**
     * Advance to the user with next priority.
     *
     * @param message if not null, broadcast this message while passing priority
     * @return this priority's user after advance
     */
    private Player passPriority(String message) {
        priorityPlayerIdx = (++priorityPlayerIdx) % playersOrder.size();
        Player priorityPlayer = playersOrder.get(priorityPlayerIdx);
        if (message != null) hub.broadcast(message);
        return priorityPlayer;
    }

    /**
     * @return Returns next player after the one with currentPriority
     */
    private Player getNextPriorityPlayer() { return playersOrder.get((priorityPlayerIdx + 1) % playersOrder.size()); }

    /**
     * Advance to the user with next turn
     *
     * @return this turn's user after advance
     */
    private Player passTurn(String message) {
        turnPlayerIdx = (++turnPlayerIdx) % playersData.size();
        Player turnPlayer = playersOrder.get(turnPlayerIdx);
        if (message != null) hub.broadcast("%ss turn begins.", turnPlayer);
        return turnPlayer;
    }

    /**
     * Transmits pile info to other user
     *
     * @param recipient user to send pile info to
     * @param pd        data from which the pile should be extracted
     * @param pile      pile to send info about
     */
    private static void transmitPile(User recipient, Player pd, DeckModel.Pile pile) {
        recipient.transmit(VisualMsg.newBuilder()
                .setVariant(VisualMsg.Variant.INFO)
                .setMsg(String.format("%s %s: [%d]\n%s",
                        recipient == pd.user ? "your " : pd.user,
                        pile.name().toLowerCase(),
                        pd.deck.piles.get(pile).size(),
                        Formatter.list(pd.deck.piles.get(pile).toArray()))));
    }

    /**
     * Transmits the actual board to the recipient
     *
     * @param recipient user to send board to
     */
    private void transmitBoard(User recipient) {
        StringBuilder board = new StringBuilder();
        playersOrder.forEach(p -> board.append(p).append(" controls:\n")
                .append(Formatter.list(p.controlledCards.stream().map(Card::toString).sorted().toArray())).append("\n"));

        recipient.transmit(VisualMsg.newBuilder()
                .setVariant(VisualMsg.Variant.INFO)
                .setMsg(board.toString()));
    }


    /**
     * Draws cards from the top of user's library to hand and informs hub about it.
     *
     * @param pd     data of player that draws cards
     * @param amount amount of cards to draw
     */
    private static void drawHand(Player pd, int amount) {
        for (int i = 0; i < amount; i++)
            pd.deck.move(DeckModel.Pile.LIBRARY, 0, DeckModel.Pile.HAND, 0);
        pd.user.hub.broadcast("%s drew %d %s", pd, amount, amount == 1 ? "card" : "cards");
        MTGStateMachine.transmitPile(pd.user, pd, DeckModel.Pile.HAND);
    }

    @Override
    public void prepcocess(MTGStateMachine fsm, Compiled<MTGCommand>... input) throws AbortTransition {
        User user = (User) input[0].controller.owner;
        var cmd = input[0].cmd;
        if (cmd == null) throw new AbortTransition();
        if (user != null && !input[0].isEnabled()) throw new CommandDisabledException(cmd);
        if (cmd == BOARD)
            fsm.transmitBoard(user);
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
                            var cards = ResourceManager.instance().getDeckModel(deckName).piles.get(DeckModel.Pile.LIBRARY);
                            Map<String, Long> amounts = cards.stream().collect(Collectors.groupingBy(cm -> cm.name, Collectors.counting()));
                            String[] strings =
                                    amounts.entrySet().stream().map(e -> e.getKey() + " x" + e.getValue()).toArray(String[]::new);
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
                fsm.playersData.forEach((user, pd) -> {
                    fsm.playersOrder.add(pd);
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

            /** Prompts player to pick a card */
            private void prompt(Player pd) {
                pd.user.transmit(VisualMsg.newBuilder()
                        .setMsg("use " + KEEP.mainAlias() + " or " + MULLIGAN.mainAlias() + " to choose a deck.")
                        .setVariant(VisualMsg.Variant.PROMPT));
            }

            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) {
                fsm.hub.broadcast("Game started. %s goes first", fsm.getPriorityPlayer());
                fsm.playersData.values().forEach(pd -> drawHand(pd, (int) fsm.config.get("max_hand")));
                fsm.getPriorityPlayer().user.mtgCommandController.enable(MULLIGAN, KEEP);
                prompt(fsm.getPriorityPlayer());
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) { // right now it's paris mulligan
                var cmd = input[0].cmd;
                User user = (User) input[0].controller.owner;
                Player pd = fsm.playersData.get(user);
                if (!pd.handChoosen)
                    fsm.hub.broadcast("%s decided to %s their hand.", user.toString(), cmd == KEEP ? "keep" : "mulligan");
                if (cmd == KEEP) pd.handChoosen = true;
                else pd.mulligansTaken++;

                fsm.getPriorityPlayer().user.mtgCommandController.disable(MULLIGAN, KEEP);
                do {fsm.passPriority(null);}
                while (fsm.getPriorityPlayer().handChoosen && fsm.getPriorityPlayer() != fsm.playersOrder.get(0));

                if (fsm.getPriorityPlayer() == fsm.playersOrder.get(0))  // new round of mulligans
                    fsm.playersData.values().stream().filter(_pd -> !_pd.handChoosen).forEach(this::parisMulligan);

                if (fsm.playersData.values().stream().allMatch(_pd -> _pd.handChoosen))
                    return MAIN_1;

                fsm.getPriorityPlayer().user.mtgCommandController.enable(MULLIGAN, KEEP);
                fsm.hub.broadcast("%s choosing...", fsm.getPriorityPlayer());
                prompt(fsm.getPriorityPlayer());
                return this; // start game if hands picked
            }

            @Override
            public void onExit(MTGStateMachine fsm, MTGStateMachine.State nextState) {
                fsm.playersOrder.forEach(pd -> {
                    pd.user.mtgCommandController.setEnabled(BOARD);
                    pd.user.mtgCommandController.mask(BOARD);
                });
            }
        },
        // mulligansUsed phase

//        // game gamePhase
//        UNTAP,
//        UPKEEP,
//        DRAW,

        MAIN_1 {
            @Override
            public void onEnter(MTGStateMachine fsm, MTGStateMachine.State prevState) {
                var pp = fsm.getPriorityPlayer();

                fsm.hub.broadcast("Main phase for begins %s", pp);
                pp.deck.piles.get(DeckModel.Pile.BOARD).forEach(Card::untap);
                drawHand(pp, 1);
                pp.user.mtgCommandController.enable(CAST, TAP, MANA_CONVERT, PASS_PRIORITY);
                pp.user.transmit(VisualMsg.newBuilder()
                        .setVariant(VisualMsg.Variant.PROMPT)
                        .setMsg("You can " + CAST + " a card if you have mana and " + TAP + " a land to gain mana"));
            }

            @Override
            public MTGStateMachine.State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) {
                var command = input[0];
                User user = (User) command.controller.owner;
                Player pd = fsm.playersData.get(user);
                if (command.cmd == MANA_CONVERT) {
                    if (command.args.length < 2) {
                        user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INVALID).setMsg("You have to specify mana to convert"));
                        return this;
                    }
                    try {
                        pd.manaPool.convertToGeneric(new Mana(command.arg(2)));
                        fsm.hub.broadcast("%s converted %s mana to generic", pd, command.arg(1));
                    } catch (ManaFormatException e) {
                        user.transmit(VisualMsg.newBuilder().setVariant(VisualMsg.Variant.ERROR).setMsg("Invalid mana format"));
                        return this;
                    }

                } else if (command.cmd == CAST || command.cmd == TAP) {
                    if (command.args.length < 2) {
                        user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INVALID)
                                .setMsg("You have to specify cards."));
                        return this;
                    }
                    for (int i = 1; i < command.args.length; i++) {


                        String cardName = command.arg(i);
                        var card = pd.deck.findCard(cardName);

                        if (card == null) user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INVALID)
                                .setMsg("Cannot find card " + cardName));

                        else if (command.cmd == TAP) {
                            if (!card.tap())
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.INVALID)
                                        .setMsg("Cannot tap " + card));
                            else
                                fsm.hub.broadcast("%s tapped %s", user, card);

                        } else { // cmd == CAST
                            try {
                                if (!card.cast(pd))
                                    user.transmit(VisualMsg.newBuilder()
                                            .setVariant(VisualMsg.Variant.INVALID)
                                            .setMsg("Cannot cast " + card));
                                else
                                    fsm.hub.broadcast("%s cast %s", user, card);
                            } catch (InsufficientManaException e) {
                                user.transmit(VisualMsg.newBuilder()
                                        .setVariant(VisualMsg.Variant.INVALID)
                                        .setMsg("You don't have enough mana. use " + MANA_CONVERT + " to convert mana to generic mana."));
                            }
                        }
                    }

                    return this;
                } else if (command.cmd == PASS_PRIORITY)
                    return COMBAT_ATTACKERS;


                Server.LOGGER.warning("unhandled input in " + this + ": " + input[0]);
                return this;
            }

            @Override
            public void onExit(MTGStateMachine fsm, State nextState) {
                var cc = fsm.getPriorityPlayer().user.mtgCommandController;
                cc.setEnabled();
            }
        },

        //        COMBAT_BEGIN,
        COMBAT_ATTACKERS {
            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) throws AbortTransition {
                fsm.getPriorityPlayer().user.mtgCommandController.setEnabled(SELECT, PASS_PRIORITY);
                fsm.getPriorityPlayer().user.transmit(VisualMsg.newBuilder()
                        .setVariant(VisualMsg.Variant.PROMPT)
                        .setMsg("Select attackers"));
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) {
                var command = input[0];
                var pd = fsm.playersData.get((User) command.controller.owner);
                if (command.cmd == SELECT) {
                    if (command.args.length < 2) {
                        pd.user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INVALID)
                                .setMsg("You have to specify a card."));
                        return this;
                    }
                    String cardName = command.arg(1);
                    var card = pd.deck.findCard(cardName);

                    if (card == null) pd.user.transmit(VisualMsg.newBuilder()
                            .setVariant(VisualMsg.Variant.INVALID)
                            .setMsg("Cannot find card " + cardName));
                    else if (card.setAttacking(!card.flags.contains(Card.Flag.IS_ATTACKING)))
                        fsm.hub.broadcast("%s is%s attacking", card, card.flags.contains(Card.Flag.IS_ATTACKING) ? "" : " not");
                } else if (command.cmd == PASS_PRIORITY)
                    return COMBAT_DEFENDERS;
                return this;
            }

            @Override
            public void onExit(MTGStateMachine fsm, State nextState) throws AbortTransition {
                fsm.getPriorityPlayer().user.mtgCommandController.setEnabled();
                fsm.passPriority("Declare defenders");
            }
        },

        COMBAT_DEFENDERS {
            @Override
            public void onEnter(MTGStateMachine fsm, State prevState) throws AbortTransition {
                fsm.getPriorityPlayer().user.mtgCommandController.setEnabled(SELECT, PASS_PRIORITY);
                fsm.getPriorityPlayer().user.transmit(VisualMsg.newBuilder()
                        .setVariant(VisualMsg.Variant.PROMPT)
                        .setMsg("Select defenders"));
            }

            @Override
            public State process(MTGStateMachine fsm, Compiled<MTGCommand>... input) {
                var command = input[0];
                var pd = fsm.playersData.get((User) command.controller.owner);
                if (command.cmd == SELECT) {
                    if (command.args.length < 3) {
                        pd.user.transmit(VisualMsg.newBuilder()
                                .setVariant(VisualMsg.Variant.INVALID)
                                .setMsg("You have to specify a card and what it defends."));
                        return this;
                    }
                    String defenderName = command.arg(1);
                    String attackerName = command.arg(2);
                    var attacker = pd.deck.findCard(attackerName);
                    var defender = fsm.getNextPriorityPlayer().deck.findCard(defenderName);

                    if (attacker == null || defender == null) pd.user.transmit(VisualMsg.newBuilder()
                            .setVariant(VisualMsg.Variant.INVALID)
                            .setMsg("Invalid def-atk pair " + defenderName + " " + attackerName));
                    else if (defender.setDefending(!defender.flags.contains(Card.Flag.IS_DEFENDING)))
                        fsm.hub.broadcast("%s is%s defending against %s", defender, defender.flags.contains(Card.Flag.IS_DEFENDING) ? "" : " not", attacker);
                } else if (command.cmd == PASS_PRIORITY)
                    return null;
                return this;
            }

            @Override
            public void onExit(MTGStateMachine fsm, State nextState) throws AbortTransition {
                fsm.hub.broadcast("Hurr Durr attacking not implemented xD exiting");
            }
        },

//        COMBAT_FIRST_STRIKE_DAMAGE,
//        COMBAT_DAMAGE,
//        COMBAT_END,

//        MAIN_2,

//        END_STEP,
//        CLEANUP_STEP
        ;
    }
}
