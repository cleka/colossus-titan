package net.sf.colossus.client;

import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.Constants;

/**
 *  Basic information about one creature, for split prediction.
 *  @version $Id$
 *  @author David Ripton
 */
class CreatureInfo implements Cloneable
{
    private final String name;
    
    // Are we sure this creature is in this legion?
    private boolean certain; 

    // Was the creature here when this legion was split off?
    private boolean atSplit; 

    CreatureInfo(String name, boolean certain, boolean atSplit)
    {
        if (name.startsWith("Titan"))
        {
            name = "Titan";
        }
        this.name = name;
        this.certain = certain;
        this.atSplit = atSplit;
    }


    String getName()
    {
        return name;
    }

    void setCertain(boolean certain)
    {
        this.certain = certain;
    }

    boolean isCertain()
    {
        return certain;
    }

    void setAtSplit(boolean atSplit)
    {
        this.atSplit = atSplit;
    }

    boolean isAtSplit()
    {
        return atSplit;
    }


    public Object clone()
    {
        return new CreatureInfo(name, certain, atSplit);
    }

    /** Two CreatureInfo objects match if the names match. */
    public boolean equals(Object other)
    {
        if (!(other instanceof CreatureInfo))
        {
            throw new ClassCastException();
        }
        return name.equals(((CreatureInfo)other).name);
    }

    /** Two CreatureInfo objects match if the names match. */
    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(name);
        if (!certain)
        {
            sb.append('?');
        }
        if (!atSplit)
        {
            sb.append('*');
        }
        return sb.toString();
    }
}
