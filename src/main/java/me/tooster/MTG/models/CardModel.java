package me.tooster.MTG.models;

import me.tooster.MTG.Keywords;
import me.tooster.MTG.Mana;
import me.tooster.MTG.exceptions.CardException;
import me.tooster.common.Model;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class CardModel implements Model {

    public String                          name                  = "";
    public Mana                            mana                  = new Mana();
    public EnumSet<Supertype>              supertypes            = EnumSet.noneOf(Supertype.class);
    public EnumSet<Type>                   types                 = EnumSet.noneOf(Type.class);
    public EnumSet<Subtype>                subtypes              = EnumSet.noneOf(Subtype.class);
    public Integer                         power                 = null;
    public Integer                         toughness             = null;
    public EnumSet<Keywords.StaticAbility> staticAbilities       = EnumSet.noneOf(Keywords.StaticAbility.class);
    public boolean                         unlimitedCopiesInDeck = false; // it's true for all cards that can be unlimited in deck


    /**
     * Used to create empty model
     */
    public CardModel() {}

    /**
     * Creates new model from serialized data with option to be read only.
     * Always call super on derived objects.
     *
     * @param data serialized model data to deserialize
     * @throws InstantiationException when validation fails
     */
    @SuppressWarnings("unchecked")
    public CardModel(@NotNull Map<String, Object> data) throws CardException {
        this();
        try {
            if ((name = (String) data.get("name")) == null) throw new CardException("card has no 'name' field specified");
            mana = new Mana((String) data.get("mana"));

            ((List<String>) data.getOrDefault("supertypes", Collections.emptyList())).stream()
                    .map(t -> Supertype.valueOf(t.toUpperCase())).forEach(supertypes::add);
            ((List<String>) data.getOrDefault("types", Collections.emptyList())).stream()
                    .map(t -> Type.valueOf(t.toUpperCase())).forEach(types::add);
            ((List<String>) data.getOrDefault("subtypes", Collections.emptyList())).stream()
                    .map(t -> Subtype.valueOf(t.toUpperCase())).forEach(subtypes::add);

            power = (Integer) data.get("power");
            toughness = (Integer) data.get("toughness");

            if (types.contains(Type.CREATURE) && (power == null || toughness == null))
                throw new CardException("creature type must have power and toughness specified");

            ((List<String>) data.getOrDefault("static abilities", Collections.emptyList())).stream()
                    .map(t -> Keywords.StaticAbility.valueOf(t.toUpperCase())).forEach(staticAbilities::add);
            
            unlimitedCopiesInDeck = (boolean) data.getOrDefault("allow_many",
                    supertypes.contains(Supertype.BASIC) && types.contains(Type.LAND));
        } catch (Exception e) {
            throw new CardException(e.getMessage());
        }
    }

    public enum Type {LAND, CREATURE, ARTIFACT, ENCHANTMENT, PLANESWALKER, INSTANT, SORCERY;}

    public enum Supertype {BASIC,}

    public enum Subtype {BEAST, BIRD, ELK,}
}
