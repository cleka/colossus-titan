package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.Glob;
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
    private String tower;
    private String color;
    private String playersElim;
    private int numLegions;
    private int numCreatures;
    private int creatureValue;
    private int titanPower;
    private int score;
    private int mulligansLeft;

    /** Sorted set of available legion markers for this player. */
    private SortedSet markersAvailable = null;


    /** Two-stage initialization. */
    PlayerInfo(Client client)
    {
        this.client = client;
        net.sf.colossus.server.CustomRecruitBase.addPlayerInfo(this);
    }


    /** Takes a colon-separated string of form
     *  dead:name:tower:color:elim:legions:markers:creatures:value:titan:score
     */
    void update(String infoString)
    {
        java.util.List data = Split.split(":", infoString);
        String buf;

        buf = (String)data.remove(0);
        dead = Boolean.valueOf(buf).booleanValue();

        name = ((String)data.remove(0));

        tower = ((String)data.remove(0));

        color = (String)data.remove(0);

        playersElim = (String)data.remove(0);

        buf = (String)data.remove(0);
        numLegions = Integer.parseInt(buf);

        buf = (String)data.remove(0);
        numCreatures = Integer.parseInt(buf);

        buf = (String)data.remove(0);
        titanPower = Integer.parseInt(buf);

        buf = (String)data.remove(0);
        score = Integer.parseInt(buf);

        buf = (String)data.remove(0);
        creatureValue = Integer.parseInt(buf);

        buf = (String)data.remove(0);
        mulligansLeft = Integer.parseInt(buf);

        if (!data.isEmpty())
        {
            if (markersAvailable == null)
            {
                markersAvailable = new TreeSet(new MarkerComparator(
                    getShortColor()));
            }
            else
            {
                markersAvailable.clear();
            }
            markersAvailable.addAll(data);
        }
    }


    boolean isDead()
    {
        return dead;
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
    }

    public String getName()
    {
        return name;
    }

    public String getTower()
    {
        return tower;
    }

    String getColor()
    {
        return color;
    }

    public String getShortColor()
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
        if (markersAvailable == null)
        {
            return 0;
        }
        else
        {
            return markersAvailable.size();
        }
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

    public int getScore()
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

    Set getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
    }

    void addMarkerAvailable(String markerId)
    {
        markersAvailable.add(markerId);
    }

    void removeMarkerAvailable(String markerId)
    {
        markersAvailable.remove(markerId);
    }

    void removeAllLegions()
    {
        Iterator it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String id = (String)it.next();
            client.removeLegion(id);
        }
    }
}
