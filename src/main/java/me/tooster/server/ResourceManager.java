package me.tooster.server;

import me.tooster.MTG.Deck;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.MTG.exceptions.DeckException;
import me.tooster.MTG.exceptions.ManaFormatException;
import me.tooster.MTG.models.CardModel;
import me.tooster.MTG.models.DeckModel;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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

    public static ResourceManager instance() { return instance; }

    private ResourceManager() {}
    //------------------------------------------------------------------------------------------------------------------


    private Map<String, Object>    config     = new HashMap<>(); // config.yml
    private Map<String, DeckModel> deckModels = new HashMap<>(); // mapping deck_name -> deckYML TODO: model class
    private Map<String, CardModel> cardModels = new HashMap<>(); // mappings card_name -> cardYML TODO: model class

    /**
     * Loads any YAML file with .yml or .yaml suffix, that is also not prefixed wit -- (two dashes)
     *
     * @param resource resource qualified from resources root e.g. cards/falcon[.yaml|.yml]
     * @return YAML object as map
     * @throws IllegalArgumentException if yaml file doesn't exist or has improper format
     */
    public Map<String, Object> loadYAML(String resource) throws IllegalArgumentException, URISyntaxException, FileNotFoundException {
        resource = resource.substring(0, resource.length() - (resource.endsWith(".yaml") ? 5 : resource.endsWith(".yml") ? 4 : 0));
        var file = getClass().getResource(resource + ".yaml");
        if ((file = getClass().getResource(resource + ".yml")) == null)
            throw new FileNotFoundException("cannot find yaml file '" + resource + "'");
        var found = Paths.get(file.toURI());
        String s = found.getFileName().toString().toLowerCase();
        if (s.startsWith("--")) throw new IllegalArgumentException("Requested yaml file is disabled.");

        Object map = new Yaml().load(new BufferedInputStream(new FileInputStream(found.toString())));
        if (!(map instanceof Map)) throw new YAMLException("Error while reading yaml - maybe it has incorrect format?");
        return (Map<String, Object>) map;
    }

    /**
     * Re-imports the config file.
     *
     * @return returns true if config was imported successfully
     */
    public boolean importConfig() throws FileNotFoundException, URISyntaxException {
        config = loadYAML("/config.yml");
        LOGGER.config("Imported config.yml");
        return true;
    }

    /**
     * (re)imports card from cards/ folder;
     * yml/yaml extension can be omitted.
     * Fails silently with error to error stream if card didn't import correctly
     *
     * @param cardModel name od card model file to load
     * @return returns loaded model if import was successful, null otherwise
     */
    private CardModel importCardModel(String cardModel) {
        try {
            CardModel cm = new CardModel(loadYAML("/cards/" + cardModel));
            // assign reference to default properties for the card
            cardModels.put(cm.name, cm);
            LOGGER.config("Imported card model '" + cm.name + "'");
            return cm;
        } catch (YAMLException | FileNotFoundException | CardException | URISyntaxException e) {
            LOGGER.warning("Couldn't import card model '" + cardModel + "': \n" + e.toString());
            return null;
        }
    }

    /**
     * (re)imports deck from decks/ folder
     * yml/yaml extension can be omitted.
     * Fails silently with error to error stream if deck didn't import correctly
     *
     * @param deckModel name od deck model file to load
     * @return returns loaded model if import was successful, null otherwise
     */
    private DeckModel importDeckModel(String deckModel) {
        try {
            DeckModel dm = new DeckModel(loadYAML("/decks/" + deckModel));
            deckModels.put(dm.name, dm); // save to decks map in library
            LOGGER.config("Imported deck model '" + dm.name + "'");
            return dm;
        } catch (YAMLException | FileNotFoundException | DeckException | URISyntaxException e) {
            LOGGER.warning("Couldn't import deck model '" + deckModel + "': \n" + e.toString());
            return null;
        }
    }

    /**
     * Imports all resources - cards, decks and config.yml file
     */
    public void importAll() {
        // walk the cards files
        try {
            importConfig();
            Files.walk(Paths.get(getClass().getResource("/cards").toURI()))
                    .filter(Files::isRegularFile).forEach(f -> importCardModel(f.getFileName().toString()));

            // walk the decks files
            Files.walk(Paths.get(getClass().getResource("/decks").toURI()))
                    .filter(Files::isRegularFile).forEach(f -> importDeckModel(f.getFileName().toString()));
        } catch (IOException | URISyntaxException e) {
            LOGGER.severe("ResourceManager critical error. Cannot load cards or decks folder");
            e.printStackTrace();
        }
    }

    /**
     * Lazily loads config and returns it.
     *
     * @return lazy loads config if not loaded
     */
    public synchronized @NotNull Map<String, Object> getConfig() {
        if (config == null) {
            try {
                importConfig();
            } catch (URISyntaxException | FileNotFoundException e) {
                LOGGER.severe("ResourceManager critical error. Cannot get config file");
                e.printStackTrace();
            }
        }
        return Collections.unmodifiableMap(config);
    }

    /**
     * Lazily loads card model
     *
     * @param name name of imported card
     * @return YAML object from file
     * @throws CardException if the card wasn't loaded
     */
    public CardModel getCardModel(String name) throws CardException {
        if (cardModels.containsKey(name))
            return cardModels.get(name);
        else {
            var model = importCardModel(name);
            if (model != null) {
                cardModels.put(model.name, model);
                return model;
            } else throw new CardException("Card model '" + name + "' not found.");
        }
    }

    /**
     * Returns propertiesYML file for imported deck
     *
     * @param name name of imported deck
     * @return YAML object from file
     * @throws CardException if the deck wasn't loaded
     */
    public DeckModel getDeckModel(String name) throws DeckException {
        if (deckModels.containsKey(name))
            return deckModels.get(name);
        else {
            var model = importDeckModel(name);
            if (model != null) {
                deckModels.put(model.name, model);
                return model;
            } else throw new DeckException("Deck model '" + name + "' not found.");
        }
    }

    /**
     * Returns list of imported decks
     *
     * @return set of names associated with imported decks
     */
    public Set<String> getLoadedDecks() { return deckModels.keySet(); }

    /**
     * Returns list of imported cards.
     *
     * @return set of names associated with imported cards
     */
    public Set<String> getLoadedCards() { return cardModels.keySet(); }
}
