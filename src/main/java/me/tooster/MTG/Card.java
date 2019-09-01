package me.tooster.MTG;

import me.tooster.server.User;
import me.tooster.server.ResourceManager;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.ManaFormatException;

import java.util.*;

/**
 * @brief represents card-object, be it in the hand, graveyard or on the board
 */
public class Card {
    public final int                 ID;                // integer id that will be displayed on the board.
    public final Deck                deck;              // deck containing the card
    public final Map<String, Object> properties;        // properties for model with read only
    public final EnumSet<Flag>       flags;             // card specific flags to represent the status and properties
    public final Map<String, Object> model;             // reference to yaml map loaded by ResourceManager
    private      Deck.Pile           pile;              // pile in which the card currently is
    private      User                controller;        // pile containing current card
    private      Mana                cost;              // mana cost of the card

    /**
     * Represents card type: land, creature, sorcery, instant, enchantment, artifact
     */
    public enum Type {
        LAND, CREATURE, ARTIFACT, ENCHANTMENT, PLANESWALKER, INSTANT, SORCERY;

    }

    /**
     * Card-instance specific flags that modify it's behaviour.
     * For example blocker that due to enchantment can attack will have CAN_ATTACK enabled in it's set
     */
    public enum Flag {
        CAN_TAP, CAN_UNTAP, CAN_ATTACK, CAN_DEFEND,
        IS_TAPPED,
        REACH, FLYING;
    }

    //------------------------------------
    private Card(int ID, Deck deck, Map<String, Object> cardYaml) {
        this.ID = ID;
        this.deck = deck;
        model = cardYaml;
        flags = EnumSet.noneOf(Flag.class);
        properties = new HashMap<>();
        reset();
    }

    /**
     * Card factory.<br>
     * Creates instance of card based on name of card loaded into resource manager.
     *
     * @param cardname name of the card loaded into resource manager
     * @return Card instance representing a card loaded into ResourceManager
     */
    public static Card build(int ID, Deck deck, String cardname) throws CardException {
        return new Card(ID, deck, Collections.unmodifiableMap(ResourceManager.instance().getCard(cardname)));
    }

    public void setController(User controller) { this.controller = controller; }

    public void reset() {
        controller = null;
        try {
            cost = new Mana((String) model.get("mana"));
        } catch (ManaFormatException e) {
            System.err.println("Something is fucked up. Mana format should be checked during import step");
            e.printStackTrace();
            System.exit(1);
        }
        properties.clear();
        flags.clear();
    }

    /**
     * Returns true if card has given type
     *
     * @param type type to check for
     * @return
     * @see Card.Type
     */
    public boolean hasType(Type type) {
        return ((ArrayList<String>) model.get("types")).contains(type.toString().toLowerCase());
    }

    @Override
    public String toString() { return model.getOrDefault("name", "") + "@" + ID; }
}
