package net.sf.colossus.util;


import java.util.*;
import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Caretaker;

/**
 * Implementation of a graph dedicated to the Recruit "Tree" (it's a directed
 * graph, not a tree, as we can have cycle in theory).
 * @version $Id$
 * @author Romain Dolbeau
 * @see net.sf.colossus.util.RecruitVertex
 * @see net.sf.colossus.util.RecruitEdge
 */

public class RecruitGraph
{
    private final Caretaker caretaker;
    private List allVertex = new ArrayList();
    private List allEdge = new ArrayList();
    private Map creatureToVertex = new HashMap();

    private RecruitVertex addVertex(String name)
    {
        return addVertex(Creature.getCreatureByName(name));
    }

    private RecruitVertex addVertex(Creature cre)
    {
        RecruitVertex temp = (RecruitVertex)creatureToVertex.get(cre);
        if (temp == null)
        {
            temp = new RecruitVertex(cre, caretaker);
            allVertex.add(temp);
            creatureToVertex.put(cre, temp);
        }
        return temp;
    }
    
    private RecruitEdge addEdge(RecruitVertex src,
                                RecruitVertex dst,
                                int number, char terrain)
    {
        RecruitEdge e = new RecruitEdge(src, dst, number, terrain);
        allEdge.add(e);
        src.addOutgoingEdge(e);
        return e;
    }

    private List traverse(RecruitVertex s, Set visited, LegionInfo legion)
    {
        List all = new ArrayList();

        if (s != null)
        {
            all.add(s);
            visited.add(s);
            
            List oe = s.getOutgoingEdges();
            
            Iterator it = oe.iterator();
            
            while (it.hasNext())
            {
                RecruitEdge e = (RecruitEdge)it.next();
                RecruitVertex v = e.getDestination();
                int already = (legion == null ? 0 :
                               legion.numCreature(s.getCreatureName()));

                if ((!(visited.contains(v))) &&
                    ((s.getRemaining() + already) >= e.getNumber()))
                {
                    all.addAll(traverse(v, visited, legion));
                }
            }
        }
        return all;
    }

    /* PUBLIC */

    public RecruitGraph(Caretaker caretaker)
    {
        this.caretaker = caretaker;
    }

    public RecruitEdge addEdge(Creature src,
                               Creature dst,
                               int number, char terrain)
    {
        return addEdge(addVertex(src),
                       addVertex(dst),
                       number, terrain);
    }

    public RecruitEdge addEdge(String src,
                               String dst,
                               int number, char terrain)
    {
        return addEdge(addVertex(src),
                       addVertex(dst),
                       number, terrain);
    }
 
    public List getOutgoingEdges(Creature cre)
    {
        RecruitVertex temp = (RecruitVertex)creatureToVertex.get(cre);
        return temp.getOutgoingEdges();
    }

    public List getOutgoingEdges(String name)
    {
        return getOutgoingEdges(Creature.getCreatureByName(name));
    }

    public List traverse(Creature cre)
    {
        return traverse((RecruitVertex)creatureToVertex.get(cre),
                        new HashSet(),
                        null);
    }
    
    public List traverse(String name)
    {
        return traverse(Creature.getCreatureByName(name));
    }

    public List traverse(Creature cre, LegionInfo legion)
    {
        return traverse((RecruitVertex)creatureToVertex.get(cre),
                        new HashSet(),
                        legion);
    }
    
    public List traverse(String name, LegionInfo legion)
    {
        return traverse(Creature.getCreatureByName(name), legion);
    }
}
