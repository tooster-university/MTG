package me.tooster.MTG;

import me.tooster.MTG.exceptions.InsufficientManaException;
import me.tooster.MTG.models.CardModel;
import me.tooster.MTG.models.DeckModel;
import me.tooster.MTG.exceptions.CardException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

/**
 * @brief represents card-object, be it in the hand, graveyard or on the board etc.
 * <p>
 * Main assumption is that any constant status effects such as being able to tap due to being a land etc. are setup from model, and should
 * be always treated as priority when checking logic. For example to check if card can tap, one should check CAN_TAP flag instead of
 * asking if `types` contains "LAND" and other context specific things. For any locks such as 'this creature cannot attack' a proper locking
 * method should be added
 */
public class Card {
    public final CardModel      model;             // reference to yaml map loaded by ResourceManager
    public final int            ID;                // integer id that will be displayed on the board.
    public final Deck           deck;              // deck containing the card
    public final EnumSet<Flag>  flags;             // card specific flags to represent the status and properties
    public       DeckModel.Pile pile;              // pile in which the card currently is
    private      Player         controller;        // pile containing current card
    private      Mana           cost;              // mana cost of the card


    /**
     * Card-instance specific flags that modify it's behaviour.
     * For example blocker that due to enchantment can attack will have CAN_ATTACK enabled in it's set
     */
    public enum Flag {
        CAN_TAP, CAN_UNTAP,
        IS_TAPPED,
        CAN_ATTACK, CAN_DEFEND,
        IS_ATTACKING, IS_DEFENDING
    }
    //------------------------------------

    private Card(int ID, Deck deck, CardModel model) { //TODO: make Card extend CardModel
        this.ID = ID;
        this.deck = deck;
        this.model = model;
        flags = EnumSet.noneOf(Flag.class);
        reset();
    }

    @Override
    public String toString() { return model.name + "@" + ID; }

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

    public void reset() {
        controller = null;
        flags.clear();

        cost = new Mana(model.mana);
        if (model.types.contains(CardModel.Type.LAND)) {
            cost = new Mana("0");
            flags.add(Flag.CAN_TAP);
            flags.add(Flag.CAN_UNTAP);
        } else if (model.types.contains(CardModel.Type.CREATURE)) {
            flags.add(Flag.CAN_ATTACK);
            flags.add(Flag.CAN_DEFEND);
        }
    }

    /**
     * Sets this cards controller
     *
     * @param controller
     */
    public void setController(Player controller) {
        if (this.controller != null)
            controller.controlledCards.remove(this);
        this.controller = controller;
        controller.controlledCards.add(this);
    }

    public void setPile(DeckModel.Pile pile) {this.pile = pile;}

    /**
     * Returns mana cost of this card.  This cost is a new modifiable mana object
     *
     * @return mana requirement for this card
     */
    public Mana getCost() { return cost; }

    /**
     * Casts a card from the current pile to the board and sets the controller
     *
     * @param caster
     */
    public boolean cast(@NotNull Player caster) throws InsufficientManaException {
        if (controller != null) return false; // already cast
        caster.manaPool.payFor(cost); // pay the cost
        setController(caster);
        deck.move(this, this.pile, DeckModel.Pile.BOARD); // put on board
        return true;
    }

    /**
     * Untaps this card.
     *
     * @return true if untap was a success
     */
    public boolean untap() {
        if (!flags.contains(Flag.IS_TAPPED) || !flags.contains(Flag.CAN_UNTAP)) return false;
        flags.remove(Flag.IS_TAPPED);
        return true;
    }

    /**
     * Taps this card.
     *
     * @return true if action was a success
     */
    public boolean tap() {
        if (flags.contains(Flag.IS_TAPPED) || !flags.contains(Flag.CAN_TAP)) return false;
        flags.add(Flag.IS_TAPPED);
        if (model.types.contains(CardModel.Type.LAND))
            controller.manaPool.addMana(model.mana);
        return true;
    }

    public boolean setAttacking(boolean isAttacking) { // todo: MUST_ATTACK
        if(!flags.contains(Flag.CAN_ATTACK)) return false;
        if(isAttacking) flags.add(Flag.IS_ATTACKING);
        else flags.remove(Flag.IS_ATTACKING);
        return true;
    }

    public boolean setDefending(boolean isDefending) {
        if(!flags.contains(Flag.CAN_DEFEND)) return false;
        if(isDefending) flags.add(Flag.IS_DEFENDING);
        else flags.remove(Flag.IS_DEFENDING);
        return true;
    }
}
