package me.tooster.MTG;

import me.tooster.common.Command;

import java.util.EnumSet;

public enum MTGCommand implements Command {
    // todo: @Usage annotation
    // --- all purpose ------------------------------------
    @Alias("/deck select") @Help("/deck select <deckName> selects a deck by name.") DECK_SELECT,
    @Alias({"/deck list", "/decks"}) @Help("lists all available decks.") DECK_LIST,
    @Alias("/deck") @Help("/deck [deckName] displays info about current/specified deck.") DECK_SHOW,

    @Alias({"/k", "/yes", "/confirm"}) @Help("generic command to confirm actions") CONFIRM,
    @Alias({"/no", "/decline", "/deny"}) @Help("generic command to confirm actions") DENY,
    @Alias({"/s", "/select", "/pick"}) @Help("generic command for selecting objects") SELECT,
    // ----------------------------------------------------
    @Alias("/ready") @Help("switches the ready/not ready state.") READY,
    @Alias({"/forfeit", "/surrender"}) @Help("issued by a coward to run from a challenge.") FORFEIT,

    // game phase
    // ----------------------------------------------------
    @Alias("/mulligan") @Help("execute mulligan") MULLIGAN, // todo: make aliases to other commands cuz MULLIGAN === DENY
    @Alias("/keep") @Help("keep the current hand") KEEP,
    @Alias("/discard") @Help("discards a card") DISCARD,

    @Alias({"/", "/pass"}) @Help("passes the priority") PASS_PRIORITY,
    @Alias("/board") @Help("displays the current board") BOARD,

    @Alias("/mana convert") @Help("/mana convert <manaFormat> to convert mana to generic mana") MANA_CONVERT,

    @Alias({"/t", "/tap"}) @Help("/tap <cards...> taps the cards") TAP,
//    @Alias({"/u", "/untap"}) @Help("/tap <cards...> utaps the cards") UNTAP,
//    @Alias({"/dr", "/draw"}) DRAW,
    @Alias({"/c", "/cast"}) @Help("/cast <card> to cast a card") CAST,
    ;

    public static final EnumSet<MTGCommand> commands = EnumSet.allOf(MTGCommand.class);

}
