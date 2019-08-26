package me.tooster.MTG;

import me.tooster.server.User;
import me.tooster.server.ResourceManager;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Deck {

    private       User                           owner;
    private final Map<String, Object>            properties; // properties from YAML file
    private final EnumMap<Pile, ArrayList<Card>> piles = new EnumMap<>(Pile.class);


    public enum Pile {
        LIBRARY, HAND, GRAVEYARD, EXILE, SIDEBOARD, TABLE;

        private static final Pile[] cached = Pile.values();
        public static Pile[] cachedValues(){ return cached;}
    }


    private Deck(@NotNull User owner, @NotNull Map<String, Object> properties) throws CardException {
        this.properties = properties;
        this.owner = owner;
        for (Pile pile : Pile.cachedValues()) piles.put(pile, new ArrayList<>());
    }

    public void setOwner(User owner) { this.owner = owner; }

    public User getOwner() { return owner; }

    public Map<String, Object> getProperties() { return properties; }

    /**
     * Returns unmodifiable list of cards serverIn the given pile
     * @param pile
     * @return
     */
    public List<Card> getPile(Pile pile) { return
            Collections.unmodifiableList(piles.get(pile));
    }


    /**
     * Fisher-Yates shuffle of collection
     *
     * @param collection collection to sort
     */
    void shuffle(ArrayList<Card> collection) {
        Random random = new Random();
        int n = collection.size();
        for (int i = 0; i < n; i++) {
            int ridx = i + random.nextInt(n - i);
            Card t = collection.get(ridx);
            collection.set(ridx, collection.get(i));
            collection.set(i, t);
        }
    }

    /**
     * Resets deck - puts all cards serverIn library, shuffles, changes mulligan counter to 0
     */
    public void reset() {
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.HAND));
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.GRAVEYARD));
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.EXILE));
        piles.get(Pile.HAND).clear();
        piles.get(Pile.GRAVEYARD).clear();
        piles.get(Pile.EXILE).clear();
        shuffle(piles.get(Pile.LIBRARY));

        for (Card card : piles.get(Pile.LIBRARY))
            card.reset();

    }

    /**
     * Moves card at index <b>srcIdx</b> from <b>srcPile</b> to <b>dstPile</b> at <b>dstIdx</b>
     *
     * @param srcPile source pile
     * @param srcIdx  index serverIn source pile.
     *                Indexing starts at 0 from the top of the pile.
     * @param dstPile destination pile
     * @param dstIdx  index serverIn destination pile.
     *                inserting at index <i>i</i> means, that card will be at the index <i>i</i>
     *                counting from top as 0
     * @throws DeckException Thrown if index or
     */
    public void move(Pile srcPile, int srcIdx, Pile dstPile, int dstIdx) throws DeckException {
        if (piles.get(srcPile).isEmpty())
            throw new DeckException("Source pile is empty.");
        if (srcIdx < 0 || srcIdx >= piles.get(srcPile).size())
            throw new DeckException("Source index is invalid.");
        if (dstIdx < 0 || srcIdx > piles.get(dstPile).size())
            throw new DeckException("Destination index is invalid.");

        Card c = piles.get(srcPile).remove(srcIdx);
        piles.get(dstPile).add(dstIdx, c);
    }

    /**
     * Deck factory. Builds and assigns deck to the player.
     *
     * @param deckname name of the deck imported into program
     *                 - should be serverIn the list returned by <code>ResourceManager.getInstance().getDecks()</code>
     *                 If it was imported, it meets the composition of a deck: compulsory name, library fields and
     *                 optional sideboard field
     * @return new Deck object if deck was successfully created
     */
    public static Deck build(User owner, String deckname) throws DeckException, CardException {
        Deck deck = new Deck(owner, ResourceManager.getInstance().getDeck(deckname));
        owner.deck = deck;
        for (Pile pile : Pile.cachedValues()) { // for all piles saved serverIn deck.yml file
            Map<String, Integer> cardsYAML = (Map<String, Integer>) deck.properties.get(pile.toString().toLowerCase());
            ArrayList<Card> cardsPile = deck.piles.get(pile);
            for (Map.Entry<String, Integer> cardYAML : cardsYAML.entrySet()) // iterate over every card
                for (int i = 0; i < cardYAML.getValue(); i++) { // add <count> cards to deck
                    Card c = Card.build(deck, cardYAML.getKey());
//                    c.setID(owner.hub.nextID());
//                    owner.hub.registerObject(c.getID(), c);
                    cardsPile.add(c);
                }

        }
        return deck;
    }


}
