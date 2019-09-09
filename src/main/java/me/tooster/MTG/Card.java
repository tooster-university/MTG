package me.tooster.MTG;

import me.tooster.MTG.models.CardModel;
import me.tooster.MTG.models.DeckModel;
import me.tooster.server.User;
import me.tooster.server.ResourceManager;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.ManaFormatException;

import java.util.*;
import java.util.function.Supplier;

/**
 * @brief represents card-object, be it in the hand, graveyard or on the board
 */
public class Card {
    public final CardModel           model;             // reference to yaml map loaded by ResourceManager
    public final CardModel           overridenModel;    // any model changes are written to this object
    public final int                 ID;                // integer id that will be displayed on the board.
    public final Deck                deck;              // deck containing the card
    public final Map<String, Object> properties;        // properties for model with read only
    public final EnumSet<Flag>       flags;             // card specific flags to represent the status and properties
    private      DeckModel.Pile      pile;              // pile in which the card currently is
    private      Player              controller;        // pile containing current card
    private      Mana                cost;              // mana cost of the card


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
    private Card(int ID, Deck deck, CardModel model) {
        this.ID = ID;
        this.deck = deck;
        this.model = model;
        overridenModel = new CardModel();
        flags = EnumSet.noneOf(Flag.class);
        properties = new HashMap<>();
        reset();
    }

    /**
     * Card factory.<br>
     * Creates instance of card based on name of card loaded into resource manager.
     *
     * @param IDGenerator IDGenerator that produces unique IDs for any object in current game
     * @param deck        deck in which the card will be placed
     * @param model       model of the card loaded into resource manager
     * @return Card instance representing a card loaded into ResourceManager
     */
    public static Card build(Supplier<Integer> IDGenerator, Deck deck, CardModel model) throws CardException { // TODO: validation, split
        // into load/build
        Card card = new Card(IDGenerator.get(), deck, model);
        card.reset();
        return card;
    }

    /**
     * Sets this cards controller
     *
     * @param controller
     */
    public void setController(Player controller) { this.controller = controller; }

    public void reset() {
        controller = null;
        cost = new Mana(model.mana);
        properties.clear();
        flags.clear();
    }

    @Override
    public String toString() { return model.name + "@" + ID; }
}
