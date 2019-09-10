package me.tooster.MTG;

import me.tooster.MTG.exceptions.InsufficientManaException;
import me.tooster.MTG.exceptions.ManaFormatException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing mana poll/mana cost
 */
public class Mana {
    public enum Color {
        // those represent both cost and mana pool
        WHITE, BLUE, BLACK, RED, GREEN, COLORLESS, // "diamond" symbol mana
        // those two below can only be used in mana cost, not stored mana pool:
        GENERIC_X,  // variable generic mana
        GENERIC; // numeric mana, # is just a placeholder for number

        final static String symbols = "WUBRGCX#";

        char symbol() {
            return symbols.charAt(this.ordinal());
        }

        static Color fromCode(char symbol) {
            switch (symbol) {
                case 'W': return Color.WHITE;
                case 'U': return Color.BLUE;
                case 'B': return Color.BLACK;
                case 'R': return Color.RED;
                case 'G': return Color.GREEN;
                case 'C': return Color.COLORLESS;
                case 'X': return Color.GENERIC_X;
                case '#': return Color.GENERIC;
            }
            return null;
        }

        boolean isCollectible() {
            return this != GENERIC && this != GENERIC_X;
        }

        boolean isColored() {
            return this.isCollectible() && this != COLORLESS;
        }
    }

    private EnumMap<Color, Integer> pool = new EnumMap<>(Color.class); // color mana will be >0, colorless mana... ?...

    /**
     * See {@link me.tooster.MTG.Mana#Mana(String)}.
     */
    public Mana() {this("0");}

    /**
     * Clones the mana pool
     * @param mana mana to clone
     */
    public Mana(Mana mana) {
        pool = new EnumMap<>(mana.pool); // I hope it works as intended and doesn't use boxed primitives
    }

    /**
     * Creates mana object representing mana cost or mana poll.
     *
     * @param mana Mana format consisting of any combination of:
     *             <ul>
     *             <li>color mana symbols: W,U,B,R,G</li>
     *
     *             <li>colorless mana symbol: C</li>
     *             <li>variable cost: X</li>
     *             <li>generic mana: &lt;a positive integer&gt;</li>
     *             </ul>
     */
    public Mana(@NotNull String mana) throws ManaFormatException {
        if (mana.equals("0")) return;

        Pattern manaRegex = Pattern.compile("[\\dWUBRGCX]+");
        Matcher manaMatcher = manaRegex.matcher(mana);
        if (!manaMatcher.matches()) throw new ManaFormatException(mana);

        // find numbers in mana format
        Pattern genericRegex = Pattern.compile("\\d+");
        Matcher numberMatcher = genericRegex.matcher(mana);
        int genericCost = 0;
        while (numberMatcher.find()) genericCost += Integer.parseInt(numberMatcher.group());

        pool.put(Color.GENERIC, genericCost);

        for (int i = 0; i < mana.length(); i++) {
            Color color = Color.fromCode(mana.charAt(i));
            if (color != null) pool.put(color, pool.getOrDefault(color, 0) + 1);
        }
    }

    /**
     * @return Returns representation of mana object with standard symbols: W, U, B, R, G, C, &lt;integer&gt;
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Color mana : pool.keySet())
            if (mana != Color.GENERIC) for (int n = pool.get(mana); n > 0; n--)
                s.append(mana.symbol());

        if (pool.containsKey(Color.GENERIC)) s.append(pool.get(Color.GENERIC));

        if (pool.containsKey(Color.GENERIC_X)) s.append("[X=").append(getX()).append("]");

        return s.toString();
    }

    /**
     * Returns the mana pool representing stored/required mana
     */
    public final EnumMap<Color, Integer> getPool() {
        return pool;
    }

    /**
     * Sets this mana pool anew.
     *
     * @param pool
     */
    public void setPool(Mana pool) {
        for (Color mana : pool.getPool().keySet())
            this.pool.put(mana, pool.getPool().get(mana));
    }

    /**
     * @return For cost, returns number of X costs.
     * For stored pool returns set number X.
     * Defaults to 0.
     */
    public int getX() {
        return pool.getOrDefault(Color.GENERIC_X, 0);
    }

    /**
     * Sets current X value in mana pool. Should be only used in pool, not in cost.
     *
     * @param X new X value
     */
    public void setX(int X) {
        pool.put(Color.GENERIC_X, X);
    }

    /**
     * @return Returns list of colors representing mana set. If the card has any of
     * WHITE, BLUE, BLACK, RED, GREEN, the color is a mix of all of the contained colors.
     * Otherwise the array contains only one item: COLORLESS
     */
    public EnumSet<Color> getColorIdentity() {
        EnumSet<Color> colors = EnumSet.noneOf(Color.class);
        for (Color mana : pool.keySet())
            if (mana.isColored()) colors.add(mana);

        if (colors.size() == 0) colors.add(Color.COLORLESS);

        return colors;
    }

    /**
     * @param extraMana mana to add. GENERIC_X mana is ignored.
     */
    public void addMana(Mana extraMana) {
        for (Color mana : extraMana.getPool().keySet())
            if (mana != Color.GENERIC_X)
                pool.put(mana, extraMana.getPool().get(mana) + pool.getOrDefault(mana, 0));
    }

    /**
     * Removes <b>choosenMana</b> from mana pool. GENERIC_X mustn't be in the <b>choosenMana</b>.
     *
     * @param choosenMana choosenMana
     * @throws InsufficientManaException if trying to remove more mana than in storage
     */
    public void removeMana(Mana choosenMana) throws InsufficientManaException {
        if (!this.satisfies(choosenMana))
            throw new InsufficientManaException(this, choosenMana);

        for (Color color : choosenMana.getPool().keySet())
            if (color != Color.GENERIC_X)
                pool.put(color, pool.getOrDefault(color, 0) - choosenMana.getPool().get(color));
    }

    /**
     * Clears the mana pool setting 0 for each mana type.
     */
    public void flushMana() {
        pool.clear();
    }

    /**
     * Converts collectible mana to generic mana.
     * Mutates the object.
     * Use only with pool types, not cost types.
     *
     * @param choosenMana mana in current pool to be converted to generic.
     * @return this object with converted generic mana
     * @throws InsufficientManaException if there is not enough mana
     */
    public Mana convertToGeneric(Mana choosenMana) throws InsufficientManaException {
        if (!this.satisfies(choosenMana))
            throw new InsufficientManaException(this, choosenMana);

        for (Color mana : choosenMana.getPool().keySet()) {
            if (mana != Color.GENERIC_X && mana != Color.GENERIC) {
                pool.put(mana, pool.get(mana) - choosenMana.getPool().get(mana));
                pool.put(Color.GENERIC, pool.getOrDefault(Color.GENERIC, 0) + choosenMana.getPool().get(mana));
            }
        }
        return this;
    }

    /**
     * Tries to pay <b>requiredMana</b> with stored mana pool.
     * If current mana pool satisfies, the pool is decreased accordingly.
     * Colored mana must be converted beforehand to generic using <code>withGenericAs()</code>, to include the
     * generic and variable(X) total cost.
     *
     * @param requiredMana amount of mana that must be paid.
     *                     GENERIC_X should equal the number of X counters in card cost
     * @throws InsufficientManaException when current pool has too few mana to pay for <b>requiredMana/b>.
     */
    public void payFor(Mana requiredMana) throws InsufficientManaException {
        if (!this.satisfies(requiredMana))
            throw new InsufficientManaException(this, requiredMana);

        // subtract collectible and generic cost
        for (Color mana : requiredMana.getPool().keySet())
            if (mana != Color.GENERIC_X)
                pool.put(mana, pool.get(mana) - requiredMana.getPool().get(mana));

        // subtract variable(X) cost
        if (requiredMana.getPool().containsKey(Color.GENERIC_X))
            pool.put(Color.GENERIC,
                    pool.getOrDefault(Color.GENERIC, 0)
                            - pool.getOrDefault(Color.GENERIC_X, 0)
                            * requiredMana.getPool().getOrDefault(Color.GENERIC_X, 0));


    }

    /**
     * Checks, if this mana pool can be used to pay <b>requiredMana</b>.
     * Be wary, that this object should have it's colored mana already
     * converted to generic mana for this to produce good output, i.e.
     * if current mana pool is {GG} and required is {2}, <code>satisfies()</code> returns false.
     * use <code>withGenericAs(Mana("GG")</code> to produce proper results.
     *
     * @param requiredMana amount of mana that must be paid
     * @return true if requiredMana can be paid with this mana pool,
     * where generic and variable mana is is already converted.
     */
    public boolean satisfies(Mana requiredMana) {

        // check colored+colorless mana, count spare mana to spend
        for (Color mana : requiredMana.getPool().keySet()) {
            if (mana.isCollectible()) {
                if (requiredMana.getPool().getOrDefault(mana, 0) >
                        pool.getOrDefault(mana, 0))
                    return false;
            }

        }

        // counts generic and variable mana; variable mana also must be converted to generic in pool beforehand
        return (pool.getOrDefault(Color.GENERIC, 0) >=
                requiredMana.getPool().getOrDefault(Color.GENERIC, 0)
                        + requiredMana.getPool().getOrDefault(Color.GENERIC_X, 0)
                        * pool.getOrDefault(Color.GENERIC_X, 0));

    }
}
