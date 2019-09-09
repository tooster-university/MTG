package me.tooster.MTG;

import me.tooster.common.Command;

import java.util.EnumSet;

public enum MTGCommand implements Command {

    // --- all purpose ------------------------------------
    @Alias("/deck select") @Help("/deck select <deckName> selects a deck by name.") DECK_SELECT,
    @Alias({"/deck list", "/decks"}) @Help("lists all available decks.") DECK_LIST,
    @Alias("/deck") @Help("/deck [deckName] displays info about current/specified deck.") DECK_SHOW,

    @Alias({"/k", "/yes", "/confirm"}) @Help("generic command to confirm actions") CONFIRM,
    @Alias({"/no", "/decline", "/deny"}) @Help("generic command to confirm actions") DENY,
    // ----------------------------------------------------
    @Alias("/ready") @Help("switches the ready/not ready state.") READY,
    @Alias({"/forfeit", "/surrender"}) @Help("issued by a coward to run from a challenge.") FORFEIT,

    // game phase
    // ----------------------------------------------------
    @Alias("/mulligan") @Help("execute mulligan") MULLIGAN, // todo: make aliases to other commands cuz MULLIGAN === DENY
    @Alias("/keep") @Help("keep the current hand") KEEP,

    @Alias("/discard") @Help("discards a card") DISCARD,

    @Alias({"/", "/pass"}) PASS_PRIORITY,
    @Alias({"/a", "/atk", "/attack"}) DECLARE_ATTACK,
    @Alias({"/d", "/def", "/defend"}) DECLARE_DEFEND,

    @Alias({"/t", "/tap"}) TAP,
    @Alias({"/u", "/untap"}) UNTAP,
    @Alias({"/dr", "/draw"}) DRAW,
    @Alias({"/c", "/cast"}) CAST,
    ;

    public static final EnumSet<MTGCommand> commands = EnumSet.allOf(MTGCommand.class);

}
