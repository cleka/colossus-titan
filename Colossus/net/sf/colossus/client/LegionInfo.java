package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;


/**
 * LegionInfo holds client-side public info about a legion.
 * @version $Id$ 
 * @author David Ripton
 */


public final class LegionInfo
{
    private Client client;
    private PredictSplits ps;

    /** immutable */
    private String markerId;
    /** immutable */
    private String playerName;

    private String hexLabel;
    private Marker marker;
    private String lastRecruit;
    private boolean moved;
    private boolean teleported;
    private int entrySide;
    private boolean recruited;


    LegionInfo(String markerId, Client client)
    {
        this.markerId = markerId;
        this.playerName = client.getPlayerNameByMarkerId(markerId);
        this.client = client;
    }

    private Node getNode(String markerId)
    {
        PredictSplits ps = client.getPredictSplits(playerName);
        Node node = ps.getLeaf(markerId);
        return node;
    }

    private Node getNode()
    {
        return getNode(this.markerId);
    }


    void setMarker(Marker marker)
    {
        this.marker = marker;
    }

    Marker getMarker()
    {
        return marker;
    }

    public int getHeight()
    {
        try
        {
            return getNode().getHeight();
        }
        catch (NullPointerException ex)
        {
            return 8;
        }
    }

    public void setHexLabel(String hexLabel)
    {
        this.hexLabel = hexLabel;
    }

    public String getHexLabel()
    {
        return hexLabel; 
    }

    public MasterHex getCurrentHex()
    {
        return MasterBoard.getHexByLabel(getHexLabel()); 
    }


    public String getPlayerName()
    {
        return playerName;
    }


    public String getMarkerId()
    {
        return markerId;
    }


    /** Return an immutable copy of the legion's contents, in sorted order. */
    public List getContents()
    {
        return Collections.unmodifiableList(
            getNode().getCreatures().getCreatureNames());
    }

    boolean contains(String creatureName)
    {
       return getContents().contains(creatureName);
    }

    public int numCreature(String creatureName)
    {
        int count = 0;
        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (name.equals(creatureName))
            {
                count++;
            }
        }
        return count;
    }

    int numSummonableCreature()
    {
        int count = 0;
        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            Creature c = Creature.getCreatureByName((String)it.next());
            if (c.isSummonable())
            {
                count++;
            }
        }
        return count;
    }

    public int numCreature(Creature creature)
    {
        return numCreature(creature.getName());
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    java.util.List getImageNames()
    {
        java.util.List names = new ArrayList();
        names.addAll(getContents());
        int j = names.indexOf(Constants.titan);
        if (j != -1)
        {
            names.set(j, getTitanBasename());
        }
        return names;
    }

    /** Return a list of Booleans. */
    java.util.List getCertainties()
    {
        java.util.List booleans = new ArrayList();
        List cil = getNode().getCreatures();
        Iterator it = cil.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            booleans.add(new Boolean(ci.isCertain()));
        }
        return booleans;
    }


    public PlayerInfo getPlayerInfo()
    {
        return client.getPlayerInfo(playerName);
    }


    /** Return the full basename for a titan in legion markerId,
     *  first finding that legion's player, player color, and titan size.
     *  Default to Constants.titan if the info is not there. */
    String getTitanBasename()
    {
        try
        {
            PlayerInfo info = client.getPlayerInfo(playerName);
            String color = info.getColor();
            int power = info.getTitanPower();
            return "Titan-" + power + "-" + color;
        }
        catch (Exception ex)
        {
            return Constants.titan;
        }
    }


    /** Add a new creature to this legion. */
    void addCreature(String name)
    {
        getNode().addCreature(name);
    }

    void removeCreature(String name)
    {
        getNode().removeCreature(name);
    }

    /** Reveal creatures in this legion, some of which already may be known. */
    void revealCreatures(final List names)
    {
        getNode().revealCreatures(names);
    }

    void split(int childHeight, String childId, List splitoffs, int turn)
    {
        getNode().split(childHeight, childId, turn, splitoffs);
    }

    void merge(String splitoffId, int turn)
    {
Log.debug("LegionInfo.merge() for " + markerId + " " + splitoffId);
        Node splitoff = getNode(splitoffId);
        getNode().merge(splitoff, turn);
    }


    public boolean hasTitan()
    {
        return getContents().contains(Constants.titan);
    }

    public int numLords()
    {
        int count = 0;

        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (name.startsWith(Constants.titan))
            {
                count++;
            }
            else
            {
                Creature creature = Creature.getCreatureByName(name);
                if (creature != null && creature.isLord())
                {
                    count++;
                }
            }
        }
        return count;
    }


    public String bestSummonable()
    {
        Creature best = null;

        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            Creature creature = Creature.getCreatureByName(name);
            if (creature.isSummonable())
            {
                if (best == null || creature.getPointValue() > 
                    best.getPointValue())
                {
                    best = creature;
                }
            }
        }

        if (best == null)
        {
            return null;
        }
        return best.getName();
    }

    public boolean hasSummonable()
    {
        return (bestSummonable() != null);
    }

    /** Return the point value of suspected contents of this legion. */
    public int getPointValue()
    {
        int sum = 0;
        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (name.startsWith(Constants.titan))
            {
                PlayerInfo info = client.getPlayerInfo(playerName);
                // Titan skill is changed by variants.
                sum += info.getTitanPower() *
                    Creature.getCreatureByName("Titan").getSkill();
            }
            else
            {
                sum += Creature.getCreatureByName(name).getPointValue();
            }
        }
        return sum;
    }

    /** Legions are sorted in descending order of known total point value,
        with the titan legion always coming first.  This is inconsistent
        with equals().  Really only useful for comparing own legions. */
    public int compareTo(Object object)
    {
        if (object instanceof LegionInfo)
        {
            LegionInfo other = (LegionInfo)object;
            if (hasTitan())
            {
                return Integer.MIN_VALUE;
            }
            else if (other.hasTitan())
            {
                return Integer.MAX_VALUE;
            }
            else
            {
                return (other.getPointValue() - this.getPointValue());
            }
        }
        else
        {
            throw new ClassCastException();
        }
    }

    // XXX Not exact -- does not verify that other legion is enemy.
    boolean isEngaged()
    {
        int numInHex = client.getLegionsByHex(getHexLabel()).size();
        return (numInHex == 2);
    }


    void setLastRecruit(String lastRecruit)
    {
        if (lastRecruit == null || lastRecruit.equals("null"))
        {
            this.lastRecruit = null;
            setRecruited(false);
        }
        else
        {
            this.lastRecruit = lastRecruit;
            setRecruited(true);
        }
    }

    String getLastRecruit()
    {
        return lastRecruit;
    }


    public void setMoved(boolean moved)
    {
        this.moved = moved;
    }

    public boolean hasMoved()
    {
        return moved;
    }

    void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    boolean hasTeleported()
    {
        return teleported;
    }

    void setEntrySide(int entrySide)
    {
        this.entrySide = entrySide;
    }

    int getEntrySide()
    {
        return entrySide;
    }


    void setRecruited(boolean recruited)
    {
        this.recruited = recruited;
    }

    boolean hasRecruited()
    {
        return recruited;
    }

    /** Return true if the legion has moved and can recruit. */
    public boolean canRecruit()
    {
        return hasMoved() && getHeight() < 7 &&
            !hasRecruited() && !getPlayerInfo().isDead() &&
            !client.findEligibleRecruits(getMarkerId(), 
                getHexLabel()).isEmpty();
    }

    public String toString()
    {
        return markerId;
    }


    /** Sorts Titans first, then decreasing order of kill value, then
     *  Unknowns last */
    class CreatureNameComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            String s1 = (String)o1;
            String s2 = (String)o2;
            if (s1.equals(s2))
            {
                return 0;
            }
            if (s1.equals("Unknown"))
            {
                return 1;
            }
            if (s2.equals("Unknown"))
            {
                return -1;
            }
            if (s1.startsWith(Constants.titan))
            {
                return -1;
            }
            if (s2.startsWith(Constants.titan))
            {
                return 1;
            }
            Creature c1 = Creature.getCreatureByName(s1);
            Creature c2 = Creature.getCreatureByName(s2);
            return SimpleAI.getKillValue(c2) - SimpleAI.getKillValue(c1);
        }
    }
}
