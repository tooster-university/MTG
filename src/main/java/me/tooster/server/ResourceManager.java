package me.tooster.server;

import me.tooster.server.MTG.Card;
import me.tooster.server.MTG.Deck;
import me.tooster.server.MTG.Mana;
import me.tooster.server.exceptions.CardException;
import me.tooster.server.exceptions.DeckException;
import me.tooster.server.exceptions.ManaFormatException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Singleton for loading cards and decks into memory
 */
public class ResourceManager {

    private static final Logger LOGGER = Logger.getLogger(ResourceManager.class.getName());

    private static final ResourceManager instance = new ResourceManager();

    public static ResourceManager getInstance() { return instance; }

    private ResourceManager() { importAll(); }
    //------------------------------------------------------------------------------------------------------------------


    private Map<String, Map<String, Object>> decksYAML = new HashMap<>(); // mapping deck_name -> deckYML
    private Map<String, Map<String, Object>> cardsYAML = new HashMap<>(); // mappings card_name -> cardYML

    /**
     * Loads any YAML file with .yml or .yaml suffix, that is also not prefixed wit -- (two dashes)
     *
     * @param path path to YAML fie
     * @return YAML object as map
     * @throws IllegalArgumentException if yaml file doesn't exist or has improper format
     */
    private Map<String, Object> loadYAML(Path path) throws IllegalArgumentException, FileNotFoundException {
        String s = path.toString().toLowerCase();
        if (!s.startsWith("--") && (s.endsWith(".yml") || s.endsWith(".yaml"))) {
            Yaml yaml = new Yaml();
            Object map = yaml.load(new BufferedInputStream(new FileInputStream(path.toString())));
            if (!(map instanceof Map))
                throw new YAMLException("yaml file formatted not correctly");
            return (Map<String, Object>) map;

        } else
            throw new IllegalArgumentException("No such yaml file.");
    }

    /**
     * (re)imports card from cards/ folder;
     * yml/yaml extension can be omitted.
     * Fails silently with error to error stream if card didn't import correctly
     */
    public void importCard(Path cardPath) {
        try {
            Map<String, Object> cardYAML = loadYAML(cardPath);
            String name = (String) cardYAML.get("name");
            if (name == null)
                throw new YAMLException("card has no 'name' field specified");

            // type check
            List<String> types = (List<String>) cardYAML.getOrDefault("types", Collections.emptyList());
            for (String type : types)
                Card.Type.valueOf(type.toUpperCase()); // throws if type doesn't match

            // throws when mana(if exists) has invalid format
            String mana = (String) cardYAML.get("mana");
            if (mana != null)
                new Mana(mana);

            // ATK/DEF for creatures
            if (types.contains("creature") && !(cardYAML.containsKey("power") && cardYAML.containsKey("toughness")))
                throw new YAMLException("creature type must have power and toughness specified");

            // assign reference to default properties for the card
            cardsYAML.put(name, cardYAML);
            LOGGER.config("Imported card '" + name + "'");
        } catch (YAMLException | ManaFormatException | FileNotFoundException e) {
            LOGGER.warning("Couldn't import card '" + cardPath.getFileName() + "': \n" + e.toString());
        }
    }

    /**
     * (re)imports deck from decks/ folder
     * yml/yaml extension can be omitted.
     * Fails silently with error to error stream if deck didn't import correctly
     */
    public void importDeck(Path deckPath) {
        try {
            Map<String, Object> deckYAML = loadYAML(deckPath);
            String name = (String) deckYAML.get("name");
            if (name == null)
                throw new YAMLException("deck has no 'name' field specified");

            // load cards
            for (Deck.Pile pile : Deck.Pile.values()) {

                Map<String, Integer> cards = (Map<String, Integer>) deckYAML.get(pile.toString().toLowerCase());
                cards = (cards == null ? Collections.emptyMap() : cards);
                for (Map.Entry<String, Integer> cardEntry : cards.entrySet()) {
                    if (!cardsYAML.containsKey(cardEntry.getKey())) // if the card wasn't loaded - shit happens
                        throw new YAMLException("card '" + cardEntry.getKey() + "' wasn't imported");
                    List<String> types = (List<String>) cardsYAML.get(cardEntry.getKey()).get("types");
                    if (cardEntry.getValue() > 4 && // more than 4 in deck that are not basic land
                            types.stream().anyMatch(t -> !t.equals(Card.Type.LAND.toString().toLowerCase())))
                        throw new YAMLException(("deck can have maximum of 4 non-land cards with the same name"));
                }
            }

            decksYAML.put(name, deckYAML); // save to decks map in library
            LOGGER.config("Imported deck '" + name + "'");
        } catch (YAMLException | FileNotFoundException e) {
            LOGGER.warning("Couldn't import deck '" + deckPath.getFileName() + "': \n" + e.toString());
        }
    }

    public void importAll() {
        // walk the cards files
        Stream<Path> cards = null;
        try {
            cards = Files.walk(Paths.get(getClass().getResource("/cards").toURI()));
            cards.filter(Files::isRegularFile).forEach(this::importCard);

            // walk the decks files
            Stream<Path> decks = Files.walk(Paths.get(getClass().getResource("/decks").toURI()));
            decks.filter(Files::isRegularFile).forEach(this::importDeck);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("ResourceManager critical error. Cannot load cards or decks folder");
        }
    }

    /**
     * Returns propertiesYML file for imported card
     *
     * @param name name of imported card
     * @return YAML object from file
     * @throws CardException if the card wasn't loaded
     */
    public Map<String, Object> getCard(String name) throws CardException {
        if (cardsYAML.containsKey(name))
            return cardsYAML.get(name);
        else
            throw new CardException("Card '" + name + "' wasn't imported.");
    }

    /**
     * Returns propertiesYML file for imported deck
     *
     * @param name name of imported deck
     * @return YAML object from file
     * @throws CardException if the deck wasn't loaded
     */
    public Map<String, Object> getDeck(String name) throws DeckException {
        if (decksYAML.containsKey(name))
            return decksYAML.get(name);
        else
            throw new DeckException("Deck '" + name + "' wasn't imported.");
    }

    /**
     * Returns list of imported decks
     *
     * @return set of names associated with imported decks
     */
    public Set<String> getDecks() { return decksYAML.keySet(); }

    /**
     * Returns list of imported cards.
     *
     * @return set of names associated with imported cards
     */
    public Set<String> getCards() { return cardsYAML.keySet(); }
}
