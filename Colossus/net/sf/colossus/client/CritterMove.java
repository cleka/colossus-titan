package net.sf.colossus.client;

import java.util.*;

/**
 * One battle move for one critter.
 * @version $Id$
 * @author David Ripton
 */


class CritterMove
{
    private int value;
    private BattleChit critter;
    private String startingHexLabel;
    private String endingHexLabel;

    CritterMove(BattleChit critter, String startingHexLabel,
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

    BattleChit getCritter()
    {
        return critter;
    }

    int getTag()
    {
        return critter.getTag();
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    String getEndingHexLabel()
    {
        return endingHexLabel;
    }

    BattleHex getStartingHex(String terrain)
    {
        return HexMap.getHexByLabel(terrain, startingHexLabel);
    }

    BattleHex getEndingHex(String terrain)
    {
        return HexMap.getHexByLabel(terrain, endingHexLabel);
    }

    public String toString()
    {
        return critter.getDescription() + " to " + getEndingHexLabel();
    }
}
