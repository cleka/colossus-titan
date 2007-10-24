package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.Log;


/**
 * LegionInfo holds client-side public info about a legion.
 * @version $Id$ 
 * @author David Ripton
 */


public final class LegionInfo
{
    private Client client;

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
    PredictSplitNode myNode;
    boolean isMyLegon;

    LegionInfo(String markerId, Client client)
    {
        this.markerId = markerId;
        this.playerName = client.getPlayerNameByMarkerId(markerId);
        this.client = client;
        myNode=null;
        isMyLegon = playerName.equals(client.getPlayerName());
    }

    private PredictSplitNode getNode(String markerId)
    {
        PredictSplits ps = client.getPredictSplits(playerName);
        PredictSplitNode node = ps.getLeaf(markerId);
        return node;
    }

    private PredictSplitNode getNode()
    {
        if (myNode==null)
        {
            myNode= getNode(this.markerId);
        }
        return myNode;
    }

    void setMarker(Marker marker)
    {
        this.marker = marker;
    }

    Marker getMarker()
    {
        return marker;
    }

    public boolean isMyLegion()
    {
        return isMyLegon;
    }
    
    public int getHeight()
    {
        PredictSplitNode node = getNode();
        if (node == null) {
            return 0;
        }
        return node.getHeight();
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
        try
        {
            return Collections.unmodifiableList(
                getNode().getCreatures().getCreatureNames());
        }
        catch (NullPointerException ex)
        {
            return new ArrayList();
        }
    }

    public boolean contains(String creatureName)
    {
        return getContents().contains(creatureName);
    }

    // TODO: great benefit from speeding this up
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

    // TODO: ... or speed this up
    public int numCreature(Creature creature)
    {
        return numCreature(creature.getName());
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    List getImageNames()
    {
        List names = new ArrayList();
        names.addAll(getContents());
        int j = names.indexOf(Constants.titan);
        if (j != -1)
        {
            names.set(j, getTitanBasename());
        }
        return names;
    }

    /** Return a list of Booleans. */
    List getCertainties()
    {
        List booleans = new ArrayList();
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

    void split(int childHeight, String childId, int turn)
    {
        getNode().split(childHeight, childId, turn);
        myNode=myNode.getChild1();
    }

    void merge(String splitoffId, int turn)
    {
        Log.debug("LegionInfo.merge() for " + markerId + " " + splitoffId);
        getNode().merge(getNode(splitoffId), turn);
        // since this is potentially a merge of a 3-way split, be safe and 
        // find the node again
        myNode= getNode(this.markerId);
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

    /** Return the total point value of those creatures of this legion
     *  which are certain.
     */
    public int getCertainPointValue()
    {
        int sum = 0;
        Iterator it = getNode().getCertainCreatures().
            getCreatureNames().iterator();
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

    public int numUncertainCreatures()
    {
        return getNode().numUncertainCreatures();
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
            return c2.getKillValue() - c1.getKillValue();
        }
    }
}
