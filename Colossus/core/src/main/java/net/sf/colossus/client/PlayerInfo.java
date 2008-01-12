package net.sf.colossus.client;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.colossus.game.PlayerState;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Player;
import net.sf.colossus.util.Split;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * PlayerInfo holds client-side public info about a player.
 * 
 * @version $Id$ 
 * @author David Ripton
 */

public final class PlayerInfo extends PlayerState
{
    private Client client;

    private boolean dead;
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
    private final SortedSet<String> markersAvailable = new TreeSet<String>(
        new MarkerComparator(getShortColor()));

    /** 
     * Two-stage initialization at the moment, only some data here, the rest comes
     * through {@link #update(String)}.
     * 
     * TODO: the object should be properly initialized in the constructor
     */
    PlayerInfo(Client client, net.sf.colossus.Player player, int number)
    {
        super(client.getGame(), player, number);
        this.client = client;
        net.sf.colossus.server.CustomRecruitBase.addPlayerInfo(this);
        String playerName;
        if (client.getOwningPlayer() != null)
        {
            playerName = client.getOwningPlayer().getPlayer().getName();
        }
        else
        {
            playerName = "UNKNOWN";
        }
        net.sf.colossus.webcommon.InstanceTracker.register(this, playerName);
    }

    /** Takes a colon-separated string of form
     *  dead:name:tower:color:elim:legions:markers:creatures:value:titan:score
     */
    void update(String infoString)
    {
        List<String> data = Split.split(":", infoString);
        String buf;

        buf = data.remove(0);
        setDead(Boolean.valueOf(buf).booleanValue());

        getPlayer().setName(data.remove(0));

        setTower(data.remove(0));

        setColor(data.remove(0));

        setType(data.remove(0));

        setPlayersElim(data.remove(0));

        buf = data.remove(0);
        setNumLegions(Integer.parseInt(buf));

        buf = data.remove(0);
        setNumCreatures(Integer.parseInt(buf));

        buf = data.remove(0);
        setTitanPower(Integer.parseInt(buf));

        buf = data.remove(0);
        setScore(Integer.parseInt(buf));

        buf = data.remove(0);
        setMulligansLeft(Integer.parseInt(buf));

        setMarkersAvailable(data);
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
    }

    boolean isDead()
    {
        return dead;
    }

    public boolean isAI()
    {
        return type.endsWith(Constants.ai);
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

    public String getShortColor()
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

    public int getNumLegions()
    {
        return numLegions;
    }

    void setMarkersAvailable(Collection<String> markersAvailable)
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

    public Set<String> getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
    }

    public int getNumMarkers()
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

    public int getTitanPower()
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

    public boolean canTitanTeleport()
    {
        return (score >= TerrainRecruitLoader.getTitanTeleportValue());
    }

    void setMulligansLeft(int mulligansLeft)
    {
        this.mulligansLeft = mulligansLeft;
    }

    public int getMulligansLeft()
    {
        return mulligansLeft;
    }

    boolean hasTeleported()
    {
        Iterator<String> it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
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

        Iterator<String> it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
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
    public List<String> getLegionIds()
    {
        return client.getLegionsByPlayerState(this);
    }

    void removeAllLegions()
    {
        Iterator<String> it = getLegionIds().iterator();
        while (it.hasNext())
        {
            String id = it.next();
            client.removeLegion(id);
        }
    }

    public void setClientNull()
    {
        this.client = null;
    }
}
