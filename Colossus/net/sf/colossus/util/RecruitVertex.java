package net.sf.colossus.util;


import java.util.*;
import net.sf.colossus.server.Creature;
import net.sf.colossus.client.CaretakerInfo;

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
    private final CaretakerInfo caretakerInfo;
    private List outgoingEdges = new ArrayList();
    
    RecruitVertex(Creature cre, CaretakerInfo caretakerInfo)
    {
        this.cre = cre;
        this.caretakerInfo = caretakerInfo;
    }
    
    RecruitVertex(String name, CaretakerInfo caretakerInfo)
    {
        this.cre = Creature.getCreatureByName(name);
        this.caretakerInfo = caretakerInfo;
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
        if (caretakerInfo != null)
        {
            return caretakerInfo.getCount(cre);
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

    public String toString()
    {
        return "RecruitVertex " + cre.getName() + " with " +
            outgoingEdges.size() + "exits. ";
    }
}
