package me.tooster.MTG;

import me.tooster.MTG.models.CardModel;
import me.tooster.MTG.models.DeckModel;
import me.tooster.server.ResourceManager;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static me.tooster.MTG.models.DeckModel.Pile;

public class Deck {

    public final DeckModel                      model; // model from YAML file
    public final Player                         owner;
    public final EnumMap<Pile, ArrayList<Card>> piles;

    //------------------------------------

    /**
     * Private constructor to create a deck game object that has it's reference to the original model, it's owner and all other fields
     * ready to safely populate
     *
     * @param owner owner of the deck
     * @param model original model to build the deck from
     */
    private Deck(@NotNull Player owner, @NotNull DeckModel model) {
        this.model = model;
        this.owner = owner;
        piles = new EnumMap<>(Pile.class);
        for (Pile pile : Pile.cachedValues) piles.put(pile, new ArrayList<>());
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

        for (Card card : piles.get(Pile.LIBRARY)) card.reset();

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
            throw new DeckException("Source pile is empty");
        if (srcIdx < 0 || srcIdx >= piles.get(srcPile).size())
            throw new DeckException("Source index is invalid");
        if (dstIdx < 0 || srcIdx > piles.get(dstPile).size())
            throw new DeckException("Destination index is invalid");

        Card c = piles.get(srcPile).remove(srcIdx);
        piles.get(dstPile).add(dstIdx, c);
    }

    /**
     * Deck factory. Builds and assigns deck to the player, resets the deck to playable state aka clears piles leaving only LIB.
     *
     * @param IDGenerator generator for unique ID's of objects in game
     * @param model       model of the deck imported into program
     * @param owner       to-be-owner of this deck. Decks are not transitive between players, they are assigned permanently.
     * @return returns new Deck with state ready to play
     */
    public static Deck build(Supplier<Integer> IDGenerator, @NotNull Player owner, @NotNull DeckModel model)
            throws DeckException, CardException {

        Deck deck = new Deck(owner, model);

        // populate piles
        deck.piles.forEach((pile, cards) -> {
            deck.model.piles.get(pile).forEach(cardModel -> cards.add(Card.build(IDGenerator, deck, cardModel)));
        });

        deck.reset();
        return deck;
    }

    /**
     * @return Returns stream of cards from this deck, in no particular order.
     */
    public Stream<Card> cardStream() {
        Stream.Builder<Card> sb = Stream.builder();
        piles.values().stream().flatMap(Collection::stream).forEach(sb::add);
        return sb.build();
    }
}
