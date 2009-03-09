package net.sf.colossus.client;

import net.sf.colossus.gui.BattleChit;


/**
 * One battle move for one critter.
 * @version $Id$
 * @author David Ripton
 */

public class CritterMove
{
    private int value;
    private final BattleChit critter;
    private final String startingHexLabel;
    private final String endingHexLabel;

    public CritterMove(BattleChit critter, String startingHexLabel,
        String endingHexLabel)
    {
        super();
        this.critter = critter;
        this.startingHexLabel = startingHexLabel;
        this.endingHexLabel = endingHexLabel;
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

    public String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    public String getEndingHexLabel()
    {
        return endingHexLabel;
    }

    @Override
    public String toString()
    {
        return critter.getDescription() + " to " + getEndingHexLabel();
    }
}
