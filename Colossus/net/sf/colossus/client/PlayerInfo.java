package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.Log;
import net.sf.colossus.server.Player;
import net.sf.colossus.parser.TerrainRecruitLoader;


/**
 * PlayerInfo holds client-side public info about a player.
 * @version $Id$ 
 * @author David Ripton
 */


public final class PlayerInfo
{
    private Client client;

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
    private int mulligansLeft;

    /** Two-stage initialization. */
    PlayerInfo(Client client)
    {
        this.client = client;
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

        buf = (String)data.get(11);
        mulligansLeft = Integer.parseInt(buf);
    }


    boolean isDead()
    {
        return dead;
    }

    public String getName()
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

    public int getMulligansLeft()
    {
        return mulligansLeft;
    }
    
    boolean canTitanTeleport()
    {
        return (score >= TerrainRecruitLoader.getTitanTeleportValue());
    }

    boolean hasTeleported()
    {
        Iterator it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo info = client.getLegionInfo(markerId);
            if (info.hasTeleported())
            {
                return true;
            }
        }
        return false;
    }

    /** Return the number of this player's legions that have moved. */
    public int numLegionsMoved()
    {
        int count = 0;

        Iterator it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            LegionInfo legion = client.getLegionInfo(markerId);
            if (legion.hasMoved())
            {
                count++;
            }
        }
        return count;
    }

    public int numMobileLegions()
    {
        return getNumLegions() - numLegionsMoved();
    }

    /** Return a List of markerIds. */
    public List getLegionIds()
    {
        return client.getLegionsByPlayer(name);
    }
}
