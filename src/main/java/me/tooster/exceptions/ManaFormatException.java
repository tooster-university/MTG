package me.tooster.exceptions;

public class ManaFormatException extends ManaException {
    public ManaFormatException(String mana) {
        super("Invalid mana format: '"+mana+"'.");
    }
}
