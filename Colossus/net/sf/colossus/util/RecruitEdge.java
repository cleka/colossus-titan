package net.sf.colossus.util;


import java.util.*;


/**
 * The edge of the Recruit Graph
 * @version $Id$
 * @author Romain Dolbeau
 * @see net.sf.colossus.util.RecruitGraph
 * @see net.sf.colossus.util.RecruitVertex
 */

public class RecruitEdge
{
    private final RecruitVertex src;
    private final RecruitVertex dst;
    private final int number;
    private final char terrain;

    RecruitEdge(RecruitVertex src,
                RecruitVertex dst,
                int number, char terrain)
    {
        this.src = src;
        this.dst = dst;
        this.number = number;
        this.terrain = terrain;
    }

    /* PUBLIC */

    public RecruitVertex getSource()
    {
        return src;
    }

    public RecruitVertex getDestination()
    {
        return dst;
    }

    public int getNumber()
    {
        return number;
    }

    public char getTerrain()
    {
        return terrain;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof RecruitEdge))
        {
            return false;
        }
        RecruitEdge o2 = (RecruitEdge)obj;
        if ((o2.getSource() == src) &&
            (o2.getDestination() == dst) &&
            (o2.getNumber() == number) &&
            (o2.getTerrain() == terrain))
        {
            return true;
        }
        return false;
    }

    public String toString()
    {
        return "RecruitEdge from " + number + " " +
            src.getCreatureName() + " to " +
            dst.getCreatureName() + " in " + terrain + ". ";
    }
}
