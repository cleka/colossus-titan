package net.sf.colossus.ai.helper;


import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.variant.BattleHex;


/**
 * One battle move for one critter.
 *
 * @author David Ripton
 */

public class CritterMove
{
    private int value;
    private final BattleCritter critter;
    private final BattleHex startingHex;
    private final BattleHex endingHex;

    public CritterMove(BattleCritter critter, BattleHex startingHex,
        BattleHex endingHex)
    {
        super();
        this.critter = critter;
        this.startingHex = startingHex;
        this.endingHex = endingHex;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    public BattleCritter getCritter()
    {
        return critter;
    }

    public int getTag()
    {
        return critter.getTag();
    }

    public BattleHex getStartingHex()
    {
        return startingHex;
    }

    public BattleHex getEndingHex()
    {
        return endingHex;
    }

    /**
     * Normal use case: hypothetical scenario, critter has already been moved
     * back; we can use getDescription() which adds the startHex
     */
    @Override
    public String toString()
    {
        return critter.getDescription() + " to " + getEndingHex().getLabel();
    }

    private String getLabelNotNull(BattleHex hex)
    {
        return (hex == null ? "NULL" : hex.getLabel());
    }

    /**
     * When called interactively from BattleMap, critter is already in his
     * new location; we need to use startingHex and endingHex.
     * @return
     */
    public String toStringAsIs()
    {
        String desc = critter.getType().getName();
        if (startingHex != null && !startingHex.equals(endingHex))
        {
            desc += " " + getLabelNotNull(startingHex) + " => "
                + getLabelNotNull(endingHex);
        }
        else
        {
            BattleHex hex = startingHex != null ? startingHex : endingHex;
            desc += " := " + getLabelNotNull(hex);
        }
        return desc;
    }
}
