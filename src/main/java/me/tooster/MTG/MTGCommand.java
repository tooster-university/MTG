package me.tooster.MTG;

import me.tooster.common.Command;
public enum MTGCommand implements Command {
    // game phase
    // ----------------------------------------------------
    @Alias({" ", "P", "PASS"}) PASS_PRIORITY,
    @Alias({"A", "ATK", "ATTACK", "DECLARE"}) DECLARE_ATTACK,
    @Alias({"D", "DEF", "DEFEND", "DECLARE"}) DECLARE_DEFEND,

    @Alias({"T", "TAP"}) TAP
//            ((player, args) -> args.stream().allMatch(
//            ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
//                    .getFlags().contains(Card.Flag.CAN_TAP)))
    ,
    @Alias({"U", "UNTAP"}) UNTAP
//            ((player, args) -> args.stream().allMatch(
//            ID -> ((Card) player.getHub().getObject(Integer.parseInt(ID)))
//                    .getFlags().contains(Card.Flag.CAN_UNTAP)))
    ,
    @Alias({"DR", "DRAW"}) DRAW,
    @Alias({"M", "MULLIGAN"}) MULLIGAN,
    @Alias({"!", "C", "CAST"}) CAST,

    // fixme: "select" without parameters returns "null not imported"
    @Alias({"S", "select"}) SELECT_DECK
    //((player, args) -> args.size() >= 1 && ResourceManager.getInstance().getDecks().contains(args.get(0)))
    ,
    @Alias({"L", "decks"}) LIST_DECKS,
    @Alias({"D", "show"}) SHOW_DECK,
    @Alias({"K", "ok", "ready"}) READY;                    // marks player as ready serverIn deck select gamePhase


    public static final MTGCommand[] cachedValues = MTGCommand.values();
}
