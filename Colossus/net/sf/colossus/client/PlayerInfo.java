package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.Log;
import net.sf.colossus.server.Player;


/**
 * PlayerInfo holds client-side public info about a player.
 * @version $Id$ 
 * @author David Ripton
 */


final class PlayerInfo
{
    private boolean dead;
    private String name;
    private int tower;
    private String color;
    private String playersElim;
    private int numLegions;
    private int numMarkers;
    private int numCreatures;
    private int creatureValue;
    private int titanPower;
    private int score;

    /** Two-stage initialization. */
    PlayerInfo()
    {
    }


    /** Takes a colon-separated string of form
     *  dead:name:tower:color:elim:legions:markers:creatures:value:titan:score
     */
    void update(String infoString)
    {
        java.util.List data = Split.split(':', infoString);
        String buf;

        buf = (String)data.get(0);
        dead = Boolean.valueOf(buf).booleanValue();

        name = ((String)data.get(1));

        buf = ((String)data.get(2));
        tower = Integer.parseInt(buf);

        color = (String)data.get(3);

        playersElim = (String)data.get(4);

        buf = (String)data.get(5);
        numLegions = Integer.parseInt(buf);

        buf = (String)data.get(6);
        numMarkers = Integer.parseInt(buf);

        buf = (String)data.get(7);
        numCreatures = Integer.parseInt(buf);

        buf = (String)data.get(8);
        titanPower = Integer.parseInt(buf);

        buf = (String)data.get(9);
        score = Integer.parseInt(buf);

        buf = (String)data.get(10);
        creatureValue = Integer.parseInt(buf);
    }


    boolean isDead()
    {
        return dead;
    }

    String getName()
    {
        return name;
    }

    int getTower()
    {
        return tower;
    }

    String getColor()
    {
        return color;
    }

    String getShortColor()
    {
        return Player.getShortColor(getColor());
    }

    String getPlayersElim()
    {
        return playersElim;
    }

    int getNumLegions()
    {
        return numLegions;
    }

    int getNumMarkers()
    {
        return numMarkers;
    }

    int getNumCreatures()
    {
        return numCreatures;
    }

    int getCreatureValue()
    {
        return creatureValue;
    }

    int getTitanPower()
    {
        return titanPower;
    }

    int getScore()
    {
        return score;
    }
}
