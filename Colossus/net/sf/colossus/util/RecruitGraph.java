package net.sf.colossus.util;


import java.util.*;
import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.client.CaretakerInfo;

/**
 * Implementation of a graph dedicated to the Recruit "Tree" (it's a directed
 * graph, not a tree, as we can have cycle in theory).
 * @version $Id$
 * @author Romain Dolbeau
 */

public class RecruitGraph
{
    private CaretakerInfo caretakerInfo;
    private final List allVertex = new ArrayList();
    private final List allEdge = new ArrayList();
    private final Map creatureToVertex = new HashMap();

    /**
     * The vertex of the Recruit Graph
     * @version $Id$
     * @author Romain Dolbeau
     */
    private class RecruitVertex
    {
        private final Creature cre;
        private final RecruitGraph graph;
        private List outgoingEdges = new ArrayList();
    
        RecruitVertex(Creature cre, RecruitGraph graph)
        {
            this.cre = cre;
            this.graph = graph;
        }
    
        RecruitVertex(String name, RecruitGraph graph)
        {
            this.cre = Creature.getCreatureByName(name);
            this.graph = graph;
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
    
        Creature getCreature()
        {
            return cre;
        }
    
        String getCreatureName()
        {
            return cre.getName();
        }

        int getRemaining()
        {
            if (graph.getCaretakerInfo() != null)
            {
                return graph.getCaretakerInfo().getCount(cre);
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
                outgoingEdges.size() + "exits";
        }
    }

    /**
     * The edge of the Recruit Graph
     * @version $Id$
     * @author Romain Dolbeau
     */
    private class RecruitEdge
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

        RecruitVertex getSource()
        {
            return src;
        }

        RecruitVertex getDestination()
        {
            return dst;
        }

        int getNumber()
        {
            return number;
        }

        char getTerrain()
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
                dst.getCreatureName() + " in " + terrain;
        }
    }

    public RecruitGraph(CaretakerInfo caretakerInfo)
    {
        this.caretakerInfo = caretakerInfo;
    }

    public RecruitGraph()
    {
        this.caretakerInfo = null;
    }

    private RecruitVertex addVertex(String name)
    {
        return addVertex(Creature.getCreatureByName(name));
    }

    private RecruitVertex addVertex(Creature cre)
    {
        RecruitVertex temp = (RecruitVertex)creatureToVertex.get(cre);
        if (temp == null)
        {
            temp = new RecruitVertex(cre, this);
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

    /**
     * Traverse the graph (depth first), assuming that all vertex in visited have been already visited, and using the given legion for availability of creatures (along with the caretakerInfo).
     * @param s The base vertex
     * @param visited Already visited vertexes
     * @param legion The legion to use for availability
     * @return The list of all reachable Vertex from parameter s.
     */
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

                /* only explore if
                   (1) not already visited
                   (2) enough in current legion + caretaker to traverse
                   (3) at least one of the destination still available
                */
                if (!(visited.contains(v)))
                {
                    if (((s.getRemaining() + already) >= e.getNumber()) &&
                        (v.getRemaining() > 0))
                    {
                        all.addAll(traverse(v, visited, legion));
                    }
                    else
                    {
                        Log.debug("GRAPH: ignoring " + e +
                                  " as not enough creatures are left (a: " +
                                  already + " s: " + s.getRemaining() +
                                  " d: " + v.getRemaining() + ")");
                    }
                }
            }
        }
        return all;
    }

    CaretakerInfo getCaretakerInfo()
    {
        return caretakerInfo;
    }

    /**
     * Give the List of RecruitEdge where the given creature is the source.
     * @param cre Recruiting creature
     * @return A List of all the outgoing RecruitEdge.
     */
    private List getOutgoingEdges(Creature cre)
    {
        RecruitVertex temp = (RecruitVertex)creatureToVertex.get(cre);
        return temp.getOutgoingEdges();
    }

    /**
     * Give the List of RecruitEdge where the given creature is the source.
     * @param  name Name of the recruiting creature
     * @return A List of all the outgoing RecruitEdge.
     */
    private List getOutgoingEdges(String name)
    {
        return getOutgoingEdges(Creature.getCreatureByName(name));
    }


    /**
     * Give the List of RecruitVertex still reachable through the given creature.
     * @param cre Base creature
     * @return A List of all the reachable RecruitVertex.
     */
    private List traverse(Creature cre)
    {
        return traverse((RecruitVertex)creatureToVertex.get(cre),
                        new HashSet(),
                        null);
    }
    
    /**
     * Give the List of RecruitVertex still reachable through the given creature.
     * @param name Name of the base creature
     * @return A List of all the reachable RecruitVertex.
     */
    private List traverse(String name)
    {
        return traverse(Creature.getCreatureByName(name));
    }
    
    /**
     * Give the List of RecruitVertex still reachable through the given creature from the given Legion.
     * @param cre Base creature
     * @return A List of all the reachable RecruitVertex.
     */
    private List traverse(Creature cre, LegionInfo legion)
    {
        return traverse((RecruitVertex)creatureToVertex.get(cre),
                        new HashSet(),
                        legion);
    }
    
    /**
     * Give the List of RecruitVertex still reachable through the given creature from the given Legion.
     * @param name Name of the base creature
     * @return A List of all the reachable RecruitVertex.
     */
    private List traverse(String name, LegionInfo legion)
    {
        return traverse(Creature.getCreatureByName(name), legion);
    }

    /* PUBLIC */

    /**
     * Add an edge is the graph from a Creature to another, in a given number, in a given terrain.
     * @param src Recruiting creature
     * @param dst Recruited creature
     * @param number Number of recruiters
     * @param terrain Terrain where the recruiting occurs
     */
    public void addEdge(Creature src,
                               Creature dst,
                               int number, char terrain)
    {
        addEdge(addVertex(src),
                addVertex(dst),
                number, terrain);
    }

    /**
     * Add an edge is the graph from a Creature to another, in a given number, in a given terrain.
     * @param src Name of the recruiting creature
     * @param dst Name of the recruited creature
     * @param number Number of recruiters
     * @param terrain Terrain where the recruiting occurs
     * @return The new RecruitEdge.
     */
    public void addEdge(String src,
                               String dst,
                               int number, char terrain)
    {
        addEdge(addVertex(src),
                addVertex(dst),
                number, terrain);
    }

    /**
     * Set the CaretakerInfo to use for availability of creatures.
     * @param The caretakerInfo to use subsequently.
     */
    public void setCaretakerInfo(CaretakerInfo caretakerInfo)
    {
        this.caretakerInfo = caretakerInfo;
    }

    /**
     * Clear the graph of all Vertex & Edge.
     */
    public void clear()
    {
        caretakerInfo = null;
        allVertex.clear();
        allEdge.clear();
        creatureToVertex.clear();
    }
    
    /**
     * What is the maximum "useful" number of a given creature for
     * recruitment purpose (excluding "Any" or "AnyNonLord").
     * return value of -1 or 0 means the Creature cannot recruit except itself.
     * @param creature Creature considered.
     * @return The higher number of creatures needed to recruit something.
     */
    public int getMaximumUsefulNumber(Creature creature)
    {
        int mun = -1;
        
        java.util.List outgoing = getOutgoingEdges(creature);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();
            
            if (e.getNumber() > mun)
                mun = (int)e.getNumber();
        }
        return mun;
    }

    /**
     * What is the maximum "useful" number of a given creature for
     * recruitment purpose (excluding "Any" or "AnyNonLord").
     * return value of -1 or 0 means the Creature cannot recruit except itself.
     * @param name Name of the creature considered.
     * @return The higher number of creatures needed to recruit something.
     */
    public int getMaximumUsefulNumber(String name)
    {
        return getMaximumUsefulNumber(Creature.getCreatureByName(name));
    }

    /**
     * Return all the terrains (as Character in a List) where the given
     * number of creature of the given name can recruit.
     * @param cre Recruiting creature.
     * @param number Number of creature
     * @return A List of Character representing all Terrains where recruitment is possible.
     */
    public List getAllTerrainsWhereThisNumberOfCreatureRecruit(Creature cre,
                                                               int number)
    {
        java.util.List at = new ArrayList();
            
        java.util.List outgoing = getOutgoingEdges(cre);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();
            
            if (e.getNumber() == number)
                at.add(new Character(e.getTerrain()));
        }
        return at;
    }

    /**
     * Return all the terrains (as Character in a List) where the given
     * number of creature of the given name can recruit.
     * @param name Name of the recruiting creature.
     * @param number Number of creature
     * @return A List of Character representing all Terrains where recruitment is possible.
     */
    public List getAllTerrainsWhereThisNumberOfCreatureRecruit(String name,
                                                               int number)
    {
        return getAllTerrainsWhereThisNumberOfCreatureRecruit(Creature.getCreatureByName(name), number);
    }

    /**
     * Return the name of the recruit for the given number of the given recruiter in the given terrain, or null if there's none.
     * @param cre Recruiting creature.
     * @param number Number of creature
     * @param t Terrain in which the recruiting may occur.
     * @return Name of the recruit.
     */
    public String getRecruitFromRecruiterTerrainNumber(Creature cre,
                                                       char t,
                                                       int number)
    {
        java.util.List outgoing = getOutgoingEdges(cre);
        String v2 = null;

        Iterator it = outgoing.iterator();

        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();
            
            if ((e.getNumber() == number) &&
                (e.getTerrain() == t))
            {
                v2 = e.getDestination().getCreatureName();
            }
        }
        return v2;
    }

    /**
     * Return the name of the recruit for the given number of the given recruiter in the given terrain, or null if there's none.
     * @param name Name of the recruiting creature.
     * @param number Number of creature
     * @param t Terrain in which the recruiting may occur.
     * @return Name of the recruit.
     */
    public String getRecruitFromRecruiterTerrainNumber(String name,
                                                       char t,
                                                       int number)
    {
        return getRecruitFromRecruiterTerrainNumber(Creature.getCreatureByName(name), t, number);
    }

    /**
     * Return the name of the best possible creature that is reachable trough the given creature from the given LegionInfo (can be null).
     * @param cre Recruiting creature.
     * @param legion The recruiting legion or null.
     * @return Name of the best possible recruit.
     */
    public String getBestPossibleRecruitEver(Creature cre,
                                             LegionInfo legion)
    {
        String best = cre.getName();
        int maxVP = -1;
        List all = traverse(cre, legion);
        Iterator it = all.iterator();
        while (it.hasNext())
        {
            RecruitVertex v2 = (RecruitVertex)it.next();
            int vp = v2.getCreature().getPointValue();
            if (vp > maxVP)
            {
                maxVP = vp;
                best = v2.getCreatureName();
            }
        }
        return best;
    }

    /**
     * Return the name of the best possible creature that is reachable trough the given creature from the given LegionInfo (can be null).
     * @param name Name of the recruiting creature.
     * @param legion The recruiting legion or null.
     * @return Name of the best possible recruit.
     */
    public String getBestPossibleRecruitEver(String name,
                                             LegionInfo legion)
    {
        return getBestPossibleRecruitEver(Creature.getCreatureByName(name), legion);
    }
}
