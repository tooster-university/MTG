package me.tooster.MTG;

import me.tooster.common.Command;

import java.util.EnumSet;

public enum MTGCommand implements Command {

    // --- all purpose decks ------------------------------
    @Alias("/deck select") @Help("selects a deck.") DECK_SELECT,
    @Alias({"/deck list", "/decks"}) @Help("lists all available decks.") DECK_LIST,
    @Alias("/deck") @Help("/deck [deckName] Displays info about selected or given deck.") DECK_SHOW,

    // game phase
    // ----------------------------------------------------
    @Alias("/mulligan") MULLIGAN,

    @Alias({"/", "/pass"}) PASS_PRIORITY,
    @Alias({"/a", "/atk", "/attack"}) DECLARE_ATTACK,
    @Alias({"/d", "/def", "/defend"}) DECLARE_DEFEND,

    @Alias({"/t", "/tap"}) TAP,
    @Alias({"/u", "/untap"}) UNTAP,
    @Alias({"/dr", "/draw"}) DRAW,
    @Alias({"/c", "/cast"}) CAST,
    ;

    public static final
    EnumSet<MTGCommand> commands = EnumSet.allOf(MTGCommand.class);

}
