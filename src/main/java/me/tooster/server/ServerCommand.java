package me.tooster.server;

import me.tooster.common.Command;
import me.tooster.server.MTG.Card;

public enum ServerCommand implements Command {

    // generic - always enabled
    // ----------------------------------------------------

    @Alias({"?", "HELP"}) HELP,                            // returns list of enabled commands

    // prep phase
    // ----------------------------------------------------

    // fixme: "select" without parameters returns "null not imported"
    @Alias({"S", "SELECT"}) SELECT_DECK((player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0))),
    @Alias({"L", "DECKS"}) LIST_DECKS,
    @Alias({"D", "SHOW"}) SHOW_DECK(SELECT_DECK.predicate),
    @Alias({"K", "OK", "READY"}) READY,                    // marks player as ready in deck select gamePhase

    // internals
    // ----------------------------------------------------

    END_GAME,                                                 // thrown from GameStateFSM when game ends
    TIMEOUT,                                                  // executed if a player timeouted

    // game phase
    // ----------------------------------------------------
    @Alias({" ", "P", "PASS"}) PASS_PRIORITY,
    @Alias({"A", "ATK", "ATTACK", "DECLARE"}) DECLARE_ATTACK,
    @Alias({"D", "DEF", "DEFEND", "DECLARE"}) DECLARE_DEFEND,

    @Alias({"T", "TAP"}) TAP((player, args) -> args.stream().allMatch(
            ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
                    .getFlags().contains(Card.Flag.CAN_TAP))

    ),
    @Alias({"U", "UNTAP"}) UNTAP((player, args) -> args.stream().allMatch(
            ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
                    .getFlags().contains(Card.Flag.CAN_UNTAP))
    ),
    @Alias({"DR", "DRAW"}) DRAW,
    @Alias({"M", "MULLIGAN"}) MULLIGAN,
    @Alias({">", "C", "CAST"}) CAST;

    private static final Command[] _cachedValues = ServerCommand.values();

    @Override
    public Command[] cachedValues() { return _cachedValues; }
}
