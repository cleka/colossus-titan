package net.sf.colossus.client;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.server.Player;
import net.sf.colossus.util.Split;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.Log;


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
    private String type;
    private String playersElim;
    private int numLegions;
    private int numCreatures;
    private int titanPower;
    private int score;
    private int mulligansLeft;

    /** Sorted set of available legion markers for this player. */
    private SortedSet markersAvailable = new TreeSet(new MarkerComparator(
        getShortColor()));

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
        List data = Split.split(":", infoString);
        String buf;

        buf = (String)data.remove(0);
        setDead(Boolean.valueOf(buf).booleanValue());

        setName((String)data.remove(0));

        setTower((String)data.remove(0));

        setColor((String)data.remove(0));
        
        setType((String)data.remove(0));  

        setPlayersElim((String)data.remove(0));

        buf = (String)data.remove(0);
        setNumLegions(Integer.parseInt(buf));

        buf = (String)data.remove(0);
        setNumCreatures(Integer.parseInt(buf));

        buf = (String)data.remove(0);
        setTitanPower(Integer.parseInt(buf));

        buf = (String)data.remove(0);
        setScore(Integer.parseInt(buf));

        buf = (String)data.remove(0);
        setMulligansLeft(Integer.parseInt(buf));

        setMarkersAvailable(data);
        
        /*
        Log.debug("Player info " + infoString);
        Log.debug("player color " + color);
        Log.debug("player type " + type);
        Log.debug("players elim " + playersElim);
        Log.debug("player legions " + numLegions);
         **/
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
    }

    boolean isDead()
    {
        return dead;
    }
    
    boolean isAI()
    {
        return type.endsWith(Constants.ai);
    }

    void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    void setTower(String tower)
    {
        this.tower = tower;
    }

    public String getTower()
    {
        return tower;
    }

    void setColor(String color)
    {
        this.color = color;
    }
    
    String getColor()
    {
        return color;
    }
    
    void setType(String type)
    {
        this.type = type;
    }
       
    String getType()
    {
        return type;
    }

    String getShortColor()
    {
        return Player.getShortColor(getColor());
    }

    void setPlayersElim(String playersElim)
    {
        this.playersElim = playersElim;
    }

    String getPlayersElim()
    {
        return playersElim;
    }

    void setNumLegions(int numLegions)
    {
        this.numLegions = numLegions;
    }

    int getNumLegions()
    {
        return numLegions;
    }

    void setMarkersAvailable(Collection markersAvailable)
    {
        this.markersAvailable.clear();
        if (!markersAvailable.isEmpty())
        {
            this.markersAvailable.addAll(markersAvailable);
        }
    }

    void addMarkerAvailable(String markerId)
    {
        markersAvailable.add(markerId);
    }

    void removeMarkerAvailable(String markerId)
    {
        markersAvailable.remove(markerId);
    }

    Set getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
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

    void setNumCreatures(int numCreatures)
    {
        this.numCreatures = numCreatures;
    }

    int getNumCreatures()
    {
        return numCreatures;
    }

    void setTitanPower(int titanPower)
    {
        this.titanPower = titanPower;
    }

    int getTitanPower()
    {
        return titanPower;
    }

    void setScore(int score)
    {
        this.score = score;
    }

    public int getScore()
    {
        return score;
    }

    boolean canTitanTeleport()
    {
        return (score >= TerrainRecruitLoader.getTitanTeleportValue());
    }

    void setMulligansLeft(int mulligansLeft)
    {
        this.mulligansLeft = mulligansLeft;
    }

    int getMulligansLeft()
    {
        return mulligansLeft;
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
    int numLegionsMoved()
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

    int numMobileLegions()
    {
        return getNumLegions() - numLegionsMoved();
    }

    /** Return a List of markerIds. */
    List getLegionIds()
    {
        return client.getLegionsByPlayer(name);
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
