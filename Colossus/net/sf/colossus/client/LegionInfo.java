package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.SimpleAI;


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

    int getHeight()
    {
        return height;
    }

    void setHexLabel(String hexLabel)
    {
        this.hexLabel = hexLabel;
    }

    String getHexLabel()
    {
        return hexLabel; 
    }


    String getPlayerName()
    {
        return playerName;
    }


    String getMarkerId()
    {
        return markerId;
    }


    /** Return an immutable copy of the legion's contents. */
    List getContents()
    {
        return Collections.unmodifiableList(contents);
    }

    boolean contains(String creatureName)
    {
       return getContents().contains(creatureName);
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

        int j = names.indexOf("Titan");
        if (j != -1)
        {
            names.set(j, getTitanBasename());
        }

        // XXX Ick.  Clean this up.
        Collections.sort(names, 
            new Comparator()
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
                    if (s1.startsWith("Titan"))
                    {
                        return -1;
                    }
                    if (s2.startsWith("Titan"))
                    {
                        return 1;
                    }
                    Creature c1 = Creature.getCreatureByName(s1);
                    Creature c2 = Creature.getCreatureByName(s2);
                    return SimpleAI.getKillValue(c2) -
                        SimpleAI.getKillValue(c1);
                }
            }
        );

        return names;
    }


    /** Return the full basename for a titan in legion markerId,
     *  first finding that legion's player, player color, and titan size.
     *  Default to "Titan" if the info is not there. */
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
            return "Titan";
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


    boolean hasTitan()
    {
        return getContents().contains("Titan");
    }


    // XXX Hardcoded to just archangels and angels for now.
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

    boolean hasSummonable()
    {
        return (bestSummonable() != null);
    }

    /** Return the point value of *known* contents of this legion. */
    private int getPointValue()
    {
        int sum = 0;
        Iterator it = getContents().iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            if (name.equals("Titan"))
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
Log.debug("LegionInfo.isEngaged() says there are " + numInHex);
        return (numInHex == 2);
    }
}

