package me.tooster.MTG;

import me.tooster.server.User;

public final class PlayerData {
    public final User    user;
    public       Deck    deck;
    public       boolean handChoosen = false; //
    public       Mana    manaPool    = new Mana();

    public PlayerData(User user) { this.user = user; }

    private static int ID = 0;

    static int nextID() {return ++ID;}
}
