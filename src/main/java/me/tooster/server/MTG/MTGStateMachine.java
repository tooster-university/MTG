package me.tooster.server.MTG;

import me.tooster.common.Command;
import me.tooster.common.FiniteStateMachine;
import me.tooster.server.User;

public class MTGStateMachine extends FiniteStateMachine<Command.Compiled<MTGCommand>, Player> {
    public MTGStateMachine() { super(null); }

    enum MTGState implements FiniteStateMachine.State<Command.Compiled<MTGCommand>, User> {
        DECK_SELECT{
            @Override
            public State<Command.Compiled<MTGCommand>, User> process(Command.Compiled<MTGCommand> input, User context) {
                switch(input.cmd){
                    case LIST_DECKS: {
                        String[] decks = ResourceManager.getInstance().getDecks().toArray(new String[]{});
                        Arrays.sort(decks);
                        cc.getPlayer().transmit(Formatter.response("Decks:\n" + Formatter.list(decks)));
                        return this;
                    }
                    case SHOW_DECK: {
                        Set<Map.Entry<String, Object>> cards =
                                ResourceManager.getInstance().getDeck(cc.args[0]).entrySet();
                        String[] strings =
                                cards.stream().map(e -> e.getKey() + " x" + e.getValue()).toArray(String[]::new);
                        Arrays.sort(strings);

                        cc.getPlayer().transmit(Formatter.response("Cards serverIn '" + cc.args[0] + "':\n"
                                + Formatter.list(strings)));
                        return this;
                    }
                    case SELECT_DECK: {
                        try {
                            Deck deck = Deck.build(cc.getPlayer(), cc.args[1]);
                            cc.getPlayer().setDeck(deck);
                        } catch (DeckException | CardException e) {
                            cc.getPlayer().transmit(Formatter.error(e.getMessage()));
                        }
                        return this;
                    }
                    case READY:
                        if (cc.getPlayer().getDeck() == null) {
                            cc.getPlayer().transmit(Formatter.error("You must select deck first."));
                            return this;
                        } else {
                            cc.getPlayer().getFlags().add(User.Flag.READY);
                            // hub.players cannot be empty here
                            if (!hub.getUsers().stream().allMatch(p -> p.getFlags().contains(User.Flag.READY))) {
                                hub.broadcast(Formatter.broadcast("Waiting for all players to be ready."));
                                return this;
                            }
                        }
                }
                return null;
            }
        }
    }
}
