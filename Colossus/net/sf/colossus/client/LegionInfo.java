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

    /** immutable */
    private String markerId;
    /** immutable */
    private String playerName;

    private String hexLabel;
    private int height;
    private Marker marker;
    private String lastRecruit;
    private boolean moved;
    private boolean teleported;
    private int entrySide;
    private boolean recruited;

    /** Creature name strings for *known* contents.  Never null. */
    private List contents = new ArrayList();


    LegionInfo(String markerId, Client client)
    {
        this.markerId = markerId;
        this.playerName = client.getPlayerNameByMarkerId(markerId);
        this.client = client;
    }


    void setMarker(Marker marker)
    {
        this.marker = marker;
    }

    Marker getMarker()
    {
        return marker;
    }

    void setHeight(int height)
    {
        this.height = height;
    }

    public int getHeight()
    {
        return height;
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


    /** Return an immutable copy of the legion's contents. */
    public List getContents()
    {
        return Collections.unmodifiableList(contents);
    }

    boolean contains(String creatureName)
    {
       return getContents().contains(creatureName);
    }

    int numCreature(String creatureName)
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

        int numUnknowns = getHeight() - names.size();
        for (int i = 0; i < numUnknowns; i++)
        {
            names.add("Unknown");
        }

        int j = names.indexOf(Constants.titan);
        if (j != -1)
        {
            names.set(j, getTitanBasename());
        }

        Collections.sort(names, new CreatureNameComparator());

        return names;
    }


    public void sortContents()
    {
        Collections.sort(contents, new CreatureNameComparator());
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


    /** Replace the existing contents for this legion with these. */
    void setContents(List names)
    {
        height = names.size();
        contents.clear();
        contents.addAll(names);
    }

    /** Remove all contents for this legion. */
    void clearContents()
    {
        contents.clear();
    }

    /** Add a new creature to this legion. */
    void addCreature(String name)
    {
        height++;
        contents.add(name);
    }


    void removeCreature(String name)
    {
        height--;
        contents.remove(name);
    }

    /** Reveal creatures in this legion, some of which already may be known. */
    void revealCreatures(final List names)
    {
        if (contents.isEmpty())
        {
            contents.addAll(names);
        }
        else
        {
            List newNames = new ArrayList();
            newNames.addAll(contents);

            List oldScratch = new ArrayList();
            oldScratch.addAll(contents);

            Iterator it = names.iterator();
            while (it.hasNext())
            {
                String name = (String)it.next();
                // If it's already there, don't add it, but remove it from
                // the list in case we have multiples of this creature.
                if (!oldScratch.remove(name))
                {
                    newNames.add(name);
                }
            }
            contents = newNames;
        }
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


    // XXX Hardcoded to just archangels and angels.  Use summonables.
    public String bestSummonable()
    {
        if (getContents().contains("Archangel"))
        {
            return "Archangel";
        }
        if (getContents().contains("Angel"))
        {
            return "Angel";
        }
        return null;
    }

    public boolean hasSummonable()
    {
        return (bestSummonable() != null);
    }

    /** Return the point value of *known* contents of this legion. */
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
                // Assumes titan skill is never changed by variants.
                sum += info.getTitanPower() * 4;
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

    // Not exact -- does not verify that other legion is enemy.
    boolean isEngaged()
    {
        int numInHex = client.getLegionsByHex(getHexLabel()).size();
        return (numInHex == 2);
    }


    void setLastRecruit(String lastRecruit)
    {
        this.lastRecruit = lastRecruit;
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
