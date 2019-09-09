package me.tooster.MTG.models;

import me.tooster.MTG.exceptions.DeckException;
import me.tooster.common.Model;
import me.tooster.server.ResourceManager;

import java.util.*;

import static me.tooster.MTG.models.DeckModel.Pile.SIDEBOARD;


public class DeckModel implements Model {
    public  String                              name  = "";
    public  EnumMap<Pile, ArrayList<CardModel>> piles = new EnumMap<>(Pile.class);
    private int                                 size  = 0; // size, that is number of cards in deckbox


    /**
     * Used to create empty deck model
     */
    public DeckModel() { for (Pile pile : Pile.values()) piles.put(pile, new ArrayList<>()); }

    /**
     * Loads deck model from data and validates it's correctness
     *
     * @param data
     * @throws DeckException
     */
    @SuppressWarnings("unchecked")
    public DeckModel(Map<String, Object> data) throws DeckException {
        this();
        if ((name = (String) data.get("name")) == null) throw new DeckException("deck has no 'name' field specified");

        Map<CardModel, Integer> cardCount = new HashMap<>();

        // load cards
        for (Pile pile : Pile.cachedValues) { // for each pile defined
            // read card x amount maps from pile in yaml
            var yamlCards = (Map<String, Integer>) data.getOrDefault(pile.toString().toLowerCase(), Collections.emptyMap());
            var cardModelsArray = piles.get(pile);
            yamlCards.forEach((c, n) -> {
                if (n < 0) throw new DeckException("Invalid number of cards: " + c + " x" + n);
                var cm = ResourceManager.instance().getCardModel(c);
                for (int i = 0; i < n; i++) cardModelsArray.add(cm); // populate pile with cards
                size += n; // amp the deck stats
                cardCount.put(cm, cardCount.getOrDefault(cm, 0) + n); // amp the cards counters
            });
        }

        int minLibSize = (int) data.get("min_library");
        int maxSideboardSize = (int) data.get("max_sideboard");
        if (size - piles.get(SIDEBOARD).size() < minLibSize) throw new DeckException("Deck must have at least " + minLibSize + "cards");
        if (piles.get(SIDEBOARD).size() > maxSideboardSize) throw new DeckException("Maximum sideboard size: " + maxSideboardSize);

        int limit = (int) ResourceManager.instance().getConfig().getOrDefault("max_same_name_cards_in_deck", 4);
        cardCount.forEach((cm, n) -> {
            if (n > limit && !cm.unlimitedCopiesInDeck)
                throw new DeckException("Up to " + limit + " of " + cm.name + " can be in the deck.");
        });
    }

    /**
     * @param withSideboard if set to false it doesn't count cards in sideboard.
     * @return Returns size of this deck i.e. sum of cards in all the piles or without sideboard if that's
     */
    public int size(boolean withSideboard) { return withSideboard ? size + piles.get(SIDEBOARD).size() : size; }

    public enum Pile {
        LIBRARY, HAND, GRAVEYARD, EXILE, SIDEBOARD, BOARD;

        public static final Pile[] cachedValues = Pile.values();
    }
}
