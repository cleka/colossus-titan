package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.colossus.client.MarkerComparator;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.PlayerServerSide;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * A player in a game.
 * 
 * This class holds all information describing a player in a game, such
 * as the current legions and the score. Instances of this class are always bound to
 * an instance of {@link Game}.
 * 
 * TODO there is an excessive amount of methods around the markersAvailable list.
 */
public class Player
{
    /**
     * The game this information belongs to.
     */
    private final Game game;

    /**
     * A name for this player for UI purposes and as identifier.
     */
    private String name;

    /**
     * The current legions owned by this player.
     * 
     * TODO not yet used on the client side
     */
    private final List<Legion> legions = new ArrayList<Legion>();

    /**
     * The number of the player in the game.
     * 
     * TODO clarify if this is just an arbitrary number (in which case we might want
     * to get rid of it) or the actual turn sequence
     */
    private final int number;

    /**
     * Set to true if the player is dead.
     * 
     * TODO check if that isn't equivalent to not having legions anymore
     */
    private boolean dead;

    /**
     * The starting tower of the player.
     * 
     * TODO this should be kind-of final: once a tower has been assigned, it shouldn't
     *      change anymore -- but assigning the towers has probably to happen a while
     *      after all players are created. We could at least at an assertion into the
     *      setter that it is allowed to change the value only if it was not set before.
     */
    private MasterHex startingTower;

    /**
     * The label of the color we use.
     * 
     * TODO this should really be an object representing a markerset
     * TODO similar to {@link #startingTower} this should be set only once but probably
     *      can't be set in the constructor.
     */
    private String color;

    /**
     * The type of player: local human, AI or network.
     * 
     * TODO make typesafe version
     * TODO shouldn't this be final? It should be possible to set that in the constructor.
     *      Unless we have to allow changes e.g. for humans dropping out of the game (in
     *      which case the todo should be read as "add some documentation regarding that ;-) ).
     */
    private String type;

    /**
     * A string representing all players eliminated by this player.
     * 
     * The format is just a sequence of the short, two-character versions
     * of the colors, e.g. "BkRd".
     * 
     * TODO this should really be a List<Player>
     */
    private String playersEliminated = "";

    private int mulligansLeft;

    private int score;

    /**
     * Sorted set of available legion markers for this player. 
     */
    private final SortedSet<String> markersAvailable = new TreeSet<String>(
        new MarkerComparator(getShortColor()));

    public Player(Game game, String playerName, int number)
    {
        assert game != null : "No game without Game";
        assert playerName != null : "Player needs a name";
        assert number >= 0 : "Player number must not be negative";
        // TODO check for max on number once game has the players stored in it
        this.game = game;
        this.name = playerName;
        this.number = number;
        this.dead = false;
    }

    public Game getGame()
    {
        return game;
    }

    /**
     * TODO should be List<Legion>, but currently subclasses still use more specific types
     * TODO should be unmodifiable, but at least {@link PlayerServerSide#die(Player, boolean)} still
     *      removes items
     */
    public List<? extends Legion> getLegions()
    {
        return this.legions;
    }

    @Override
    public boolean equals(Object obj)
    {
        // make sure we don't compare apples and eggs
        assert obj.getClass() == this.getClass() : getClass().getName()
            + " compared with something else";
        return super.equals(obj);
    }

    public int getNumber()
    {
        return number;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isDead()
    {
        return dead;
    }

    // TODO it would probably be safer not to have this method and instead set the
    // state only in the constructor or during die()
    public void setDead(boolean dead)
    {
        this.dead = dead;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public boolean isAI()
    {
        return type.endsWith(Constants.ai);
    }

    public void setStartingTower(MasterHex startingTower)
    {
        this.startingTower = startingTower;
    }

    public MasterHex getStartingTower()
    {
        return startingTower;
    }

    public void setColor(String color)
    {
        this.color = color;
    }

    public String getColor()
    {
        return color;
    }

    public String getShortColor()
    {
        String color = getColor();
        if (color == null)
        {
            return null;
        }
        else
        {
            return Constants.getShortColorName(color);
        }
    }

    public String getPlayersElim()
    {
        return playersEliminated;
    }

    public void setPlayersElim(String playersEliminated)
    {
        this.playersEliminated = playersEliminated;
    }

    public void addPlayerElim(Player player)
    {
        playersEliminated = playersEliminated + player.getShortColor();
    }

    public Legion getLegionByMarkerId(String markerId)
    {
        for (Legion legion : getLegions())
        {
            if (legion.getMarkerId().equals(markerId))
            {
                return legion;
            }
        }
        return null;
    }

    public Legion getTitanLegion()
    {
        for (Legion legion : getLegions())
        {
            if (legion.hasTitan())
            {
                return legion;
            }
        }
        return null;
    }

    public void addLegion(Legion legion)
    {
        legions.add(legion);
    }

    public void removeLegion(Legion legion)
    {
        legions.remove(legion);
    }

    public void removeAllLegions()
    {
        legions.clear();
    }

    public void addMarkerAvailable(String markerId)
    {
        markersAvailable.add(markerId);
    }

    public void removeMarkerAvailable(String markerId)
    {
        markersAvailable.remove(markerId);
    }

    public void clearMarkersAvailable()
    {
        markersAvailable.clear();
    }

    public Set<String> getMarkersAvailable()
    {
        return Collections.unmodifiableSortedSet(markersAvailable);
    }

    public int getNumMarkersAvailable()
    {
        return markersAvailable.size();
    }

    public String getFirstAvailableMarker()
    {
        synchronized (markersAvailable)
        {
            if (markersAvailable.isEmpty())
            {
                return null;
            }
            return markersAvailable.first();
        }
    }

    public boolean isMarkerAvailable(String markerId)
    {
        return markersAvailable.contains(markerId);
    }

    /** Removes the selected marker from the list of those available.
     *  Returns the markerId if it was present, or null if it was not. */
    public String selectMarkerId(String markerId)
    {
        if (markersAvailable.remove(markerId))
        {
            return markerId;
        }
        else
        {
            return null;
        }
    }

    public int getNumCreatures()
    {
        int count = 0;
        for (Legion legion : getLegions())
        {
            count += legion.getHeight();
        }
        return count;
    }

    /**
     * Overridden for debug/logging purposes.
     */
    @Override
    public String toString()
    {
        return getName();
    }

    public void setMulligansLeft(int mulligansLeft)
    {
        this.mulligansLeft = mulligansLeft;
    }

    public int getMulligansLeft()
    {
        return mulligansLeft;
    }

    public void setScore(int score)
    {
        this.score = score;
    }

    public int getScore()
    {
        return score;
    }

    public int getTitanPower()
    {
        return 6 + getScore()
            / TerrainRecruitLoader.getTitanImprovementValue();
    }

    public boolean canTitanTeleport()
    {
        return getScore() >= TerrainRecruitLoader.getTitanTeleportValue();
    }

    /** 
     * Return the total value of all of this player's creatures. 
     */
    public int getTotalPointValue()
    {
        int total = 0;
        for (Legion legion : getLegions())
        {
            total += legion.getPointValue();
        }
        return total;
    }

    public boolean hasTeleported()
    {
        for (Legion info : getLegions())
        {
            if (info.hasTeleported())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the player has already moved.
     * 
     * @return true iff at least one legion of the player has been moved
     */
    public boolean hasMoved()
    {
        for (Legion legion : getLegions())
        {
            if (legion.hasMoved())
            {
                return true;
            }
        }
        return false;
    }

    public int getNumLegions()
    {
        return getLegions().size();
    }
}
