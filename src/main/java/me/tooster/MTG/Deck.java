package me.tooster.MTG;

import me.tooster.server.ResourceManager;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class Deck {

    public final  PlayerData                     owner;
    public final  Map<String, Object>            model; // model from YAML file
    private final EnumMap<Pile, ArrayList<Card>> piles = new EnumMap<>(Pile.class);
    private       int                            size; // initial size of deck, useful for when we don't want to count tokens on board
    private       int                            sideboardSize;


    public enum Pile {
        LIBRARY, HAND, GRAVEYARD, EXILE, SIDEBOARD, BOARD;

        private static final Pile[] cached = Pile.values();

        public static Pile[] cachedValues() { return cached;}
    }

    //------------------------------------
    private Deck(@NotNull PlayerData owner, @NotNull Map<String, Object> model) throws CardException {
        this.model = model;
        this.owner = owner;
        for (Pile pile : Pile.cachedValues()) piles.put(pile, new ArrayList<>());
    }

    /**
     * @return Returns size of this deck i.e. sum of cards in all the piles
     */
    public int size(boolean withSideboard) { return withSideboard ? size + sideboardSize : size; }

    /**
     * Returns unmodifiable list of cards in the given pile
     *
     * @param pile pile to get
     * @return Returns UNMODIFIABLE list of piles
     */
    public List<Card> getPile(Pile pile) {
        return Collections.unmodifiableList(piles.get(pile));
    }

    /**
     * Resets deck - puts all cards in library, shuffles, changes mulligan counter to 0
     */
    public void reset() {
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.HAND));
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.GRAVEYARD));
        piles.get(Pile.LIBRARY).addAll(piles.get(Pile.EXILE));
        piles.get(Pile.HAND).clear();
        piles.get(Pile.GRAVEYARD).clear();
        piles.get(Pile.EXILE).clear();
        Collections.shuffle(piles.get(Pile.LIBRARY)); // built-in Fisher-Yates shuffle

        for (Card card : piles.get(Pile.LIBRARY))
            card.reset();

    }

    /**
     * Moves card at index <b>srcIdx</b> from <b>srcPile</b> to <b>dstPile</b> at <b>dstIdx</b>
     *
     * @param srcPile source pile
     * @param srcIdx  index in source pile.
     *                Indexing starts at 0 from the top of the pile.
     * @param dstPile destination pile
     * @param dstIdx  index in destination pile.
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
     * Deck factory. Builds and assigns deck to the player, resets the deck to playable state aka clears piles leaving only LIB.
     * See {@link me.tooster.MTG.Deck#load(Supplier, PlayerData, String)}
     *
     * @return returns new Deck with cards only in LIBRARY and SIDEBOARD if deck was successfully created
     */
    public static Deck build(Supplier<Integer> IDGenerator, @NotNull PlayerData owner, @NotNull String deckName)
            throws DeckException, CardException {

        Deck deck = load(IDGenerator, owner, deckName);
        deck.reset();
        return deck;
    }

    /**
     * @param IDGenerator generator for unique ID's of objects in game
     * @param deckName    name of the deck imported into program
     *                    - should be in the list returned by <code>ResourceManager.instance().getDecks()</code>
     *                    If it was imported, it meets the composition of a deck: compulsory name, library fields and
     *                    optional sideboard field
     * @param owner       to-be-owner of this deck. Decks are not transitive between players.
     * @return returns new Deck as loaded from YAML (all cards in respective piles) if deck was successfully created
     */
    public static Deck load(Supplier<Integer> IDGenerator, @NotNull PlayerData owner, @NotNull String deckName)
            throws DeckException, CardException {

        Deck deck = new Deck(owner, Collections.unmodifiableMap(ResourceManager.instance().getDeck(deckName)));
        for (Pile pile : Pile.cachedValues()) { // for all piles saved in deck.yml file
            Map<String, Integer> cardsYAML =
                    (Map<String, Integer>) deck.model.getOrDefault(pile.toString().toLowerCase(), Collections.emptyMap());
            ArrayList<Card> cardsPile = deck.piles.get(pile);
            for (Map.Entry<String, Integer> cardYAML : cardsYAML.entrySet()) // iterate over every card
                for (int i = 0; i < cardYAML.getValue(); i++) { // add <count> cards to deck
                    Card c = Card.build(IDGenerator.get(), deck, cardYAML.getKey());
                    cardsPile.add(c);
                }

            deck.size =
                    deck.piles.entrySet().stream().filter(kv -> kv.getKey() != Pile.SIDEBOARD).mapToInt(kv -> kv.getValue().size()).sum();
            deck.sideboardSize =
                    deck.piles.entrySet().stream().filter(kv -> kv.getKey() == Pile.SIDEBOARD).mapToInt(kv -> kv.getValue().size()).sum();
        }
        return deck;
    }

}
