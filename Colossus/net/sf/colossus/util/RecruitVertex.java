package net.sf.colossus.util;


import java.util.*;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Caretaker;

/**
 * The vertex of the Recruit Graph
 * @version $Id$
 * @author Romain Dolbeau
 * @see net.sf.colossus.util.RecruitGraph
 * @see net.sf.colossus.util.RecruitEdge
 */

public class RecruitVertex
{
    private final Creature cre;
    private final Caretaker caretaker;
    private List outgoingEdges = new ArrayList();
    
    RecruitVertex(Creature cre, Caretaker caretaker)
    {
        this.cre = cre;
        this.caretaker = caretaker;
    }
    
    RecruitVertex(String name, Caretaker caretaker)
    {
        this.cre = Creature.getCreatureByName(name);
        this.caretaker = caretaker;
    }
    
    List getOutgoingEdges()
    {
        List oe = new ArrayList();
        oe.addAll(outgoingEdges);
        return oe;
    }
    
    void addOutgoingEdge(RecruitEdge e)
    {
        if (!(outgoingEdges.contains(e)))
        {
            outgoingEdges.add(e);
        }
    }

    /* PUBLIC */
    
    public Creature getCreature()
    {
        return cre;
    }
    
    public String getCreatureName()
    {
        return cre.getName();
    }

    public int getRemaining()
    {
        if (caretaker != null)
        {
            return caretaker.getCount(cre);
        }
        else
        {
            return 99;
        }
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof RecruitVertex))
        {
            return false;
        }
        RecruitVertex o2 = (RecruitVertex)obj;
        if (o2.getCreature() == cre)
        {
            return true;
        }
        return false;
    }
}
