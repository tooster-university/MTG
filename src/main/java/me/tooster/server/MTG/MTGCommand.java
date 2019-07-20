package me.tooster.server.MTG;

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
    @Alias({"!", "C", "CAST"}) CAST;


    public static final MTGCommand[] cachedValues = MTGCommand.values();

    @Override
    public Command[] list() {return cachedValues;}
}
