package me.tooster.MTG.exceptions;

import me.tooster.MTG.Mana;

public class InsufficientManaException extends ManaException{
    public InsufficientManaException(Mana mana, Mana required) {
        super("Cannot get {"+required+"} mana from {"+mana+"}.");
    }
}
