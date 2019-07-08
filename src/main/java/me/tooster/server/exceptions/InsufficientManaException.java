package me.tooster.server.exceptions;

import me.tooster.server.MTG.Mana;

public class InsufficientManaException extends ManaException{
    public InsufficientManaException(Mana mana, Mana required) {
        super("Cannot get {"+required+"} from {"+mana+"}.");
    }
}