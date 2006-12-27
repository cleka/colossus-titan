package net.sf.colossus.util;


import java.util.*;
import net.sf.colossus.client.LegionInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.client.CaretakerInfo;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;
import net.sf.colossus.server.CustomRecruitBase;


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

    /** 99 creatures can muster one means: can not muster at all */
    public static final int BIGNUM = 99;

    /**
     * The vertex of the Recruit Graph
     * @version $Id$
     * @author Romain Dolbeau
     */
    private class RecruitVertex
    {
        private final String cre;
        private final RecruitGraph graph;
        private List outgoingEdges = new ArrayList();
        private List incomingEdges = new ArrayList();

        RecruitVertex(String name, RecruitGraph graph)
        {
            this.cre = name;
            this.graph = graph;
        }

        List getOutgoingEdges()
        {
            List oe = new ArrayList();
            oe.addAll(outgoingEdges);
            return oe;
        }

        List getIncomingEdges()
        {
            List ie = new ArrayList();
            ie.addAll(incomingEdges);
            return ie;
        }

        void addOutgoingEdge(RecruitEdge e)
        {
            if (!(outgoingEdges.contains(e)))
            {
                outgoingEdges.add(e);
            }
        }

        void addIncomingEdge(RecruitEdge e)
        {
            if (!(incomingEdges.contains(e)))
            {
                incomingEdges.add(e);
            }
        }

        String getCreatureName()
        {
            return cre;
        }

        int getRemaining()
        {
            if (graph.getCaretakerInfo() != null)
            {
                return graph.getCaretakerInfo().getCount(cre);
            }
            else
            {
                return BIGNUM;
            }
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof RecruitVertex))
            {
                return false;
            }
            RecruitVertex o2 = (RecruitVertex)obj;
            if (o2.getCreatureName().equals(cre))
            {
                return true;
            }
            return false;
        }

        public String toString()
        {
            return "RecruitVertex " + cre + " with " +
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
        private final String terrain;

        RecruitEdge(RecruitVertex src,
                RecruitVertex dst,
                int number, String terrain)
        {
            this.src = src;
            this.dst = dst;
            this.number = number;
            this.terrain = terrain;
        }

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

        String getTerrain()
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
            return ((o2.getSource() == src) &&
                    (o2.getDestination() == dst) &&
                    (o2.getNumber() == number) &&
                    (o2.getTerrain().equals(terrain)));
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

    private RecruitVertex addVertex(String cre)
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

    private RecruitVertex getVertex(String cre)
    {
        RecruitVertex temp = (RecruitVertex)creatureToVertex.get(cre);

        if (temp == null)
        {
            Log.debug("CUSTOM: Adding non-existant creature: " + cre +
                    " to the graph.");
            temp = addVertex(cre);
        }

        return temp;
    }

    private RecruitEdge addEdge(RecruitVertex src,
            RecruitVertex dst,
            int number, String terrain)
    {
        RecruitEdge e = new RecruitEdge(src, dst, number, terrain);
        allEdge.add(e);
        src.addOutgoingEdge(e);
        dst.addIncomingEdge(e);
        return e;
    }

    /**
     * Traverse the graph (depth first), assuming that all vertex in visited
     * have been already visited, and using the given legion for availability
     * of creatures (along with the caretakerInfo). This will ignore any
     * strange stuff such as Anything, AnyNonLord, and so on. OTOH It will
     * not ignore the Titan.
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
     * @param  cre Name of the recruiting creature
     * @return A List of all the outgoing RecruitEdge.
     */
    private List getOutgoingEdges(String cre)
    {
        RecruitVertex temp = getVertex(cre);
        return temp.getOutgoingEdges();
    }

    /**
     * Give the List of RecruitEdge where the given creature is the destination.
     * @param  cre Name of the recruited creature
     * @return A List of all the incoming RecruitEdge.
     */
    private List getIncomingEdges(String cre)
    {
        RecruitVertex temp = getVertex(cre);
        return temp.getIncomingEdges();
    }

    /**
     * Give the List of RecruitVertex still reachable through the given
     * creature from the given Legion.
     * @param cre Name of the base creature
     * @return A List of all the reachable RecruitVertex.
     */
    private List traverse(String cre, LegionInfo legion)
    {
        return traverse(getVertex(cre),
                new HashSet(),
                legion);
    }

    /* PUBLIC */

    /**
     * Add an edge is the graph from a Creature to another, in a given number,
     * in a given terrain.
     * @param src Name of the recruiting creature
     * @param dst Name of the recruited creature
     * @param number Number of recruiters
     * @param terrain Terrain where the recruiting occurs
     * @return The new RecruitEdge.
     */
    public void addEdge(String src,
            String dst,
            int number, String terrain)
    {
        addEdge(addVertex(src),
                addVertex(dst),
                number, terrain);
    }

    public int numberOfRecruiterNeeded(String recruiter,
            String recruit,
            String terrain,
            String hexLabel)
    {
        List allEdge = getIncomingEdges(recruit);
        RecruitVertex source = getVertex(recruiter);
        Creature recruiterCre = Creature.getCreatureByName(recruiter);
        // if the recruiter is a special such as Anything, avoid
        // crashing with NullPointerException
        boolean isLord = (recruiterCre == null ?
                false :
                recruiterCre.isLord());
        boolean isDemiLord = (recruiterCre == null ?
                false :
                recruiterCre.isDemiLord());
        int minValue = BIGNUM;

        Iterator it = allEdge.iterator();
        while (it.hasNext())
        {
            RecruitEdge theEdge = (RecruitEdge)it.next();
            if (theEdge.getTerrain().equals(terrain))
            {
                RecruitVertex tempSrc = theEdge.getSource();
                if ((source == tempSrc) ||
                        (tempSrc.getCreatureName().equals(
                        TerrainRecruitLoader.Keyword_Anything)) ||
                        ((!isLord) &&
                        (!isDemiLord) &&
                        (tempSrc.getCreatureName().equals(
                        TerrainRecruitLoader.Keyword_AnyNonLord))) ||
                        ((isLord) && (tempSrc.getCreatureName().equals(
                        TerrainRecruitLoader.Keyword_Lord))))
                {
                    if (minValue > theEdge.getNumber())
                    {
                        minValue = theEdge.getNumber();
                    }
                }
                if (tempSrc.getCreatureName().startsWith(
                    TerrainRecruitLoader.Keyword_Special))
                {
                    CustomRecruitBase cri = TerrainRecruitLoader
                        .getCustomRecruitBase(tempSrc.getCreatureName());
                    int v = cri.numberOfRecruiterNeeded(recruiter,
                            recruit,
                            terrain,
                            hexLabel);
                    if (v < minValue)
                    {
                        minValue = v;
                    }
                }
            }
        }
        return minValue;
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
     * @param cre Name of the creature considered.
     * @return The higher number of creatures needed to recruit something.
     */
    public int getMaximumUsefulNumber(String cre)
    {
        int mun = -1;

        List outgoing = getOutgoingEdges(cre);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();

            if (e.getNumber() > mun)
            {
                mun = e.getNumber();
            }
        }
        return mun;

    }

    /**
     * Return all the terrains (as String in a List) where the given
     * number of creature of the given name can recruit.
     * @param cre Name of the recruiting creature.
     * @param number Number of creature
     * @return A List of String representing all Terrains where recruitment
     *     is possible.
     */
    public List getAllTerrainsWhereThisNumberOfCreatureRecruit(String cre,
            int number)
    {
        List at = new ArrayList();

        List outgoing = getOutgoingEdges(cre);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();

            if (e.getNumber() == number)
            {
                at.add(e.getTerrain());
            }
        }
        return at;
    }

    /**
     * a triple if lists what the creature 'CRE' can recruit.
     * the positions in the lists accord to each other.
     * <code>[ [ter1, ter2, ...], [cre1, cre2, ...], [num1, num2, ...] ]</code>
     * means that in terrain='ter1' with 'num1' of 'CRE' you get one 'cre1'.
     * @return [ [terrains], [creature names], [numbers] ]
     */
    public List[] getAllThatThisCreatureCanRecruit(String cre)
    {
        List[] result = new List[3]; // [[terrain],[creature],[number]] 
        result[0] = new ArrayList();
        result[1] = new ArrayList();
        result[2] = new ArrayList();
        //
        List outgoing = getOutgoingEdges(cre);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();
            result[2].add(new Integer(e.getNumber()));
            result[1].add(e.getDestination().getCreatureName());
            result[0].add(e.getTerrain());
        }
        return result;
    }

    /** 
     * a triple if lists what can recruit the creature 'CRE'.
     * the positions in the lists accord to each other.
     * <code>[ [ter1, ter2, ...], [cre1, cre2, ...], [num1, num2, ...] ]</code>
     * means that in terrain='ter1' with 'num1' of 'cre1' you get one 'cre'.
     * @return [ [terrains], [creature names], [numbers] ]
     */
    public List[] getAllThatCanRecruitThisCreature(String cre)
    {
        List[] result = new List[3]; // [[terrain],[creature],[number]] 
        result[0] = new ArrayList();
        result[1] = new ArrayList();
        result[2] = new ArrayList();
        //
        List outgoing = getIncomingEdges(cre);
        Iterator it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();
            result[2].add(new Integer(e.getNumber()));
            result[1].add(e.getSource().getCreatureName());
            result[0].add(e.getTerrain());
        }
        return result;
    }


    /**
     * Return the name of the recruit for the given number of the given
     * recruiter in the given terrain, or null if there's none.
     * @param cre Name of the recruiting creature.
     * @param number Number of creature
     * @param t Terrain in which the recruiting may occur.
     * @return Name of the recruit.
     */
    public String getRecruitFromRecruiterTerrainNumber(String cre,
            String t,
            int number)
    {
        List outgoing = getOutgoingEdges(cre);
        String v2 = null;

        Iterator it = outgoing.iterator();

        while (it.hasNext())
        {
            RecruitEdge e = (RecruitEdge)it.next();

            if ((e.getNumber() == number) &&
                    (e.getTerrain().equals(t)))
            {
                v2 = e.getDestination().getCreatureName();
            }
        }
        return v2;
    }

    /**
     * Return the name of the best possible creature that is reachable
     * trough the given creature from the given LegionInfo (can be null).
     * @param cre Name of the recruiting creature.
     * @param legion The recruiting legion or null.
     * @return Name of the best possible recruit.
     */
    public String getBestPossibleRecruitEver(String cre,
            LegionInfo legion)
    {
        String best = cre;
        int maxVP = -1;
        List all = traverse(cre, legion);
        Iterator it = all.iterator();
        while (it.hasNext())
        {
            RecruitVertex v2 = (RecruitVertex)it.next();
            Creature creature = Creature.getCreatureByName(
                v2.getCreatureName());
            int vp = (creature == null ? -1 : creature.getPointValue());
            if (vp > maxVP)
            {
                maxVP = vp;
                best = v2.getCreatureName();
            }
        }
        return best;
    }

    /**
     * Determine if a creature given by 'lesser' could potentially
     * summon the higher valued creature given by 'greater' within N steps.  
     * This is used to determine if 'lesser' is redundant for mustering purposes
     * if we have 'greater'
     * Here we limit the search to 'distance' (typically 2) recruit steps 
     * since otherwise every creature
     * is 'reachable' via a downmuster at the tower and starting all over which
     * is not what we are interested in.
     * @param lesser Name of the recruiting creature.
     * @param greater Name of the recruit we are trying to get to
     * @param distance number of steps to consider
     */
    public boolean isRecruitDistanceLessThan(String lesser, String greater,
            int distance)
    {
        List all = traverse(lesser, null);
        Iterator it = all.iterator();
        // Log.debug("isReachable('" + lesser + "', '" + greater + "')");
        int steps = 0;
        // distance including self - i.e. distance + 1
        while (it.hasNext() && steps < distance + 1)
        {
            RecruitVertex v2 = (RecruitVertex)it.next();
            String name = v2.getCreatureName();
            // Log.debug("isReachable: '" + name + "'");
            if (name.compareTo(greater) == 0)
            {
                // Log.debug("Matched");
                return true;
            }
            else
            {
                // Log.debug("Unmatched");
            }
            steps++;
        }
        return false;
    }
}
