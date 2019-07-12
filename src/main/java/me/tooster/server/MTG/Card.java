package me.tooster.server.MTG;

import me.tooster.server.User;
import me.tooster.server.ResourceManager;
import me.tooster.server.exceptions.CardException;
import me.tooster.server.exceptions.ManaFormatException;

import java.util.*;

/**
 * @brief represents card-object, be it in the hand, graveyard or on the board
 */
public class Card {
    private       Integer             ID;                                 // integer id that will be displayed on the board. Set by engine
    private final Deck                deck;                            // deck containing the card
    private       Deck.Pile           pile;                             // pile in which the card currently is
    private       User                owner;                               // owner of the card i.e. player, whose deck contained this card
    private       User                controller;                          // pile containing current card
    private       Mana                cost;                                  // mana cost of the card
    private final Map<String, Object> properties;       // reference to yaml map loaded by ResourceManager
    private final Map<String, Object> selfProperties;   // selfProperties for properties with read only
    private final EnumSet<Flag>       flags;                  // card specific flags to represent the status and selfProperties

    private Card(Deck deck, Map<String, Object> cardYaml) {
        this.deck = deck;
        properties = cardYaml;
        flags = EnumSet.noneOf(Flag.class);
        selfProperties = new HashMap<>();
        reset();
    }

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
        REACH, FLYING,
    }


    /**
     * Card factory.<br>
     * Creates instance of card based on name of card loaded into resource manager.
     *
     * @param cardname name of the card loaded into resource manager
     * @return Card instance representing a card loaded into ResourceManager
     */
    public static Card build(Deck deck, String cardname) throws CardException {
        return new Card(deck, ResourceManager.getInstance().getCard(cardname));
    }

    /**
     * Sets ID for this card and adds it to the mappings of the owner's hub.
     *
     * @param ID new ID for this object
     */
    public void setID(int ID) {
        if (this.ID != null) // remove old mapping
            deck.getOwner().getHub().unregisterObject(ID);
        this.ID = ID; // assign new mapping
        deck.getOwner().getHub().registerObject(ID, this);
    }

    public int getID() {return ID;}

    public void setController(User controller) { this.controller = controller; }

    public Set<Flag> getFlags() {return Collections.unmodifiableSet(flags);}

    public void setFlag(Flag flag) {flags.add(flag);}

    public void unsetFlag(Flag flag) {flags.remove(flag);}

    public void reset() {
        ID = null;
        controller = null;
        try {
            cost = new Mana((String) properties.get("mana"));
        } catch (ManaFormatException e) {
            System.err.println("Something is fucked up. Mana format should be checked during import step");
            e.printStackTrace();
            System.exit(1);
        }
        selfProperties.clear();
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
        return ((ArrayList<String>) properties.get("types")).contains(type.toString().toLowerCase());
    }
}
