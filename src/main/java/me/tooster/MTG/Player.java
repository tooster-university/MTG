package me.tooster.MTG;

import me.tooster.server.User;

import java.util.HashSet;

/**
 * Represents player data for the MTG game that is abstracted away from User
 */
public final class Player {
    public final User user;
    public       Deck deck;
    public       Mana manaPool = new Mana();

    /** during mulligan step, flag to test if user has kept their hand */
    public boolean       handChoosen     = false;
    /** amount of mulligans taken */
    public int           mulligansTaken  = 0;
    /** corresponds to accumulated scry X amount */
    public int           scry            = 0;
    /** how many cards a player must discard */
    public int           cardsToDiscard  = 0;
    /** list of controlled cards; it should be kept in sync with deck BOARD pile */
    public HashSet<Card> controlledCards = new HashSet<>();

    public Player(User user) { this.user = user; }

    private static int ID = 0;

    static int nextID() {return ++ID;}

    @Override
    public String toString() {return user.toString();}
}
