package net.sf.colossus.client;


import net.sf.colossus.gui.BattleChit;
import net.sf.colossus.variant.BattleHex;


/**
 * One battle move for one critter.
 * @version $Id$
 * @author David Ripton
 */

public class CritterMove
{
    private int value;
    private final BattleChit critter;
    private final BattleHex startingHex;
    private final BattleHex endingHex;

    public CritterMove(BattleChit critter, BattleHex startingHex,
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

    public BattleChit getCritter()
    {
        return critter;
    }

    int getTag()
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

    @Override
    public String toString()
    {
        return critter.getDescription() + " to " + getEndingHex().getLabel();
    }
}
