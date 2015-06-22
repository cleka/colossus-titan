package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.ICustomRecruitBase;
import net.sf.colossus.variant.IVariantKnower;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Implementation of a graph dedicated to the Recruit "Tree" (it's a directed
 * graph, not a tree, as we can have cycle in theory).
 *
 * Moved into game package. Does it belong more to game or variant package?
 *
 * TODO this is still string-based, see comment in {@link TerrainRecruitLoader}
 *
 * @author Romain Dolbeau
 */
public class RecruitGraph
{
    private static final Logger LOGGER = Logger.getLogger(RecruitGraph.class
        .getName());

    private Caretaker caretaker;
    private final IVariantKnower variantKnower;
    private final List<RecruitVertex> allVertex = new ArrayList<RecruitVertex>();
    private final List<RecruitEdge> allEdge = new ArrayList<RecruitEdge>();
    private final Map<String, RecruitVertex> creatureToVertex = new HashMap<String, RecruitVertex>();

    /** 99 creatures can muster one means: can not muster at all */
    public static final int BIGNUM = 99;

    /**
     * The vertex of the Recruit Graph
     *
     * @author Romain Dolbeau
     */
    private static class RecruitVertex
    {
        private final String cre;
        private final RecruitGraph graph;
        private final List<RecruitEdge> outgoingEdges = new ArrayList<RecruitEdge>();
        private final List<RecruitEdge> incomingEdges = new ArrayList<RecruitEdge>();

        RecruitVertex(String name, RecruitGraph graph)
        {
            this.cre = name;
            this.graph = graph;
        }

        List<RecruitEdge> getOutgoingEdges()
        {
            List<RecruitEdge> oe = new ArrayList<RecruitEdge>();
            oe.addAll(outgoingEdges);
            return oe;
        }

        List<RecruitEdge> getIncomingEdges()
        {
            List<RecruitEdge> ie = new ArrayList<RecruitEdge>();
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
            if (graph.getCaretaker() != null)
            {
                CreatureType type = graph.getVariant().getCreatureByName(cre);
                return graph.getCaretaker().getAvailableCount(type);
            }
            else
            {
                return BIGNUM;
            }
        }

        @Override
        // TODO override hashCode() or leave both
        public boolean equals(Object obj)
        {
            if (this.getClass() != obj.getClass())
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

        @Override
        public String toString()
        {
            return "RecruitVertex " + cre + " with " + outgoingEdges.size()
                + "exits";
        }
    }

    /**
     * The edge of the Recruit Graph
     *
     * @author Romain Dolbeau
     */
    private static class RecruitEdge
    {
        private final RecruitVertex src;
        private final RecruitVertex dst;
        private final int number;
        private final MasterBoardTerrain terrain;

        RecruitEdge(RecruitVertex src, RecruitVertex dst, int number,
            MasterBoardTerrain terrain)
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

        MasterBoardTerrain getTerrain()
        {
            return terrain;
        }

        // TODO override hashCode(), too -- or leave both
        @Override
        public boolean equals(Object obj)
        {
            if (this.getClass() != obj.getClass())
            {
                return false;
            }
            RecruitEdge o2 = (RecruitEdge)obj;
            return ((o2.getSource() == src) && (o2.getDestination() == dst)
                && (o2.getNumber() == number) && (o2.getTerrain()
                .equals(terrain)));
        }

        @Override
        public String toString()
        {
            return "RecruitEdge from " + number + " " + src.getCreatureName()
                + " to " + dst.getCreatureName() + " in " + terrain;
        }
    }

    /**
     * Models a recruit option for a given creature.
     *
     * This is an return object for the question which recruit options a particular
     * creature has. Each option consists of a terrain to muster in, a target creatures
     * and a number of start creatures required to upgrade.
     */
    public static final class RecruitOption
    {
        private final MasterBoardTerrain terrain;
        private final String startCreature;
        private final String targetCreature;
        private final int numberRequired;

        public RecruitOption(MasterBoardTerrain terrain, String startCreature,
            String targetCreature, int numberRequired)
        {
            super();
            this.terrain = terrain;
            this.startCreature = startCreature;
            this.targetCreature = targetCreature;
            this.numberRequired = numberRequired;
        }

        public MasterBoardTerrain getTerrain()
        {
            return terrain;
        }

        public String getStartCreature()
        {
            return startCreature;
        }

        public String getTargetCreature()
        {
            return targetCreature;
        }

        public int getNumberRequired()
        {
            return numberRequired;
        }
    }

    // TODO variantKnower is only a temporary solution. Instead the variant
    // should be passed in (not possible right now), or perhaps the Game,
    // from which the variant can be asked.
    public RecruitGraph(IVariantKnower variantKnower)
    {
        this.variantKnower = variantKnower;
        this.caretaker = null;
    }

    private RecruitVertex addVertex(String cre)
    {
        RecruitVertex temp = creatureToVertex.get(cre);
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
        RecruitVertex temp = creatureToVertex.get(cre);

        if (temp == null)
        {
            LOGGER.log(Level.FINEST, "CUSTOM: Adding non-existant creature: "
                + cre + " to the graph.");
            temp = addVertex(cre);
        }

        return temp;
    }

    private RecruitEdge addEdge(RecruitVertex src, RecruitVertex dst,
        int number, MasterBoardTerrain terrain)
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
    private List<RecruitVertex> traverse(RecruitVertex s,
        Set<RecruitVertex> visited, Legion legion)
    {
        List<RecruitVertex> all = new ArrayList<RecruitVertex>();

        if (s != null)
        {
            all.add(s);
            visited.add(s);

            List<RecruitEdge> oe = s.getOutgoingEdges();
            Iterator<RecruitEdge> it = oe.iterator();

            while (it.hasNext())
            {
                RecruitEdge e = it.next();
                RecruitVertex v = e.getDestination();
                String creName = s.getCreatureName();

                int already = (legion == null ? 0 : ((LegionClientSide)legion)
                    .numCreature(creName));

                /* only explore if
                 (1) not already visited
                 (2) enough in current legion + caretaker to traverse
                 (3) at least one of the destination still available
                 */
                if (!(visited.contains(v)))
                {
                    if (((s.getRemaining() + already) >= e.getNumber())
                        && (v.getRemaining() > 0))
                    {
                        all.addAll(traverse(v, visited, legion));
                    }
                    else
                    {
                        LOGGER.log(Level.FINEST,
                            "GRAPH: ignoring " + e
                                + " as not enough creatures are left (a: "
                                + already + " s: " + s.getRemaining() + " d: "
                                + v.getRemaining() + ")");
                    }
                }
            }
        }
        return all;
    }

    Caretaker getCaretaker()
    {
        return caretaker;
    }

    private Variant getVariant()
    {
        return variantKnower.getTheCurrentVariant();
    }

    /**
     * Give the List of RecruitEdge where the given creature is the source.
     * @param  cre Name of the recruiting creature
     * @return A List of all the outgoing RecruitEdge.
     */
    private List<RecruitEdge> getOutgoingEdges(String cre)
    {
        RecruitVertex temp = getVertex(cre);
        return temp.getOutgoingEdges();
    }

    /**
     * Give the List of RecruitEdge where the given creature is the destination.
     * @param  cre Name of the recruited creature
     * @return A List of all the incoming RecruitEdge.
     */
    private List<RecruitEdge> getIncomingEdges(String cre)
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
    private List<RecruitVertex> traverse(String cre, Legion legion)
    {
        return traverse(getVertex(cre), new HashSet<RecruitVertex>(), legion);
    }

    /* PUBLIC */

    /**
     * Add an edge is the graph from a Creature to another, in a given number,
     * in a given terrain.
     * @param src Name of the recruiting creature
     * @param dst Name of the recruited creature
     * @param number Number of recruiters
     * @param terrain Terrain where the recruiting occurs
     */
    public void addEdge(String src, String dst, int number,
        MasterBoardTerrain terrain)
    {
        addEdge(addVertex(src), addVertex(dst), number, terrain);
    }

    public int numberOfRecruiterNeeded(String recruiter, String recruit,
        MasterBoardTerrain terrain, MasterHex hex)
    {
        List<RecruitEdge> allEdge = getIncomingEdges(recruit);
        RecruitVertex source = getVertex(recruiter);
        CreatureType recruiterCre = getVariant().getCreatureByName(recruiter);
        CreatureType recruitCre = getVariant().getCreatureByName(recruit);
        // if the recruiter is a special such as Anything, avoid
        // crashing with NullPointerException
        boolean isLord = (recruiterCre == null ? false : recruiterCre.isLord());
        boolean isDemiLord = (recruiterCre == null ? false : recruiterCre
            .isDemiLord());
        int minValue = BIGNUM;

        Iterator<RecruitEdge> it = allEdge.iterator();
        while (it.hasNext())
        {
            RecruitEdge theEdge = it.next();
            if (theEdge.getTerrain().equals(terrain))
            {
                RecruitVertex tempSrc = theEdge.getSource();
                if ((source == tempSrc)
                    || (tempSrc.getCreatureName()
                        .equals(TerrainRecruitLoader.Keyword_Anything))
                    || ((!isLord) && (!isDemiLord) && (tempSrc
                        .getCreatureName()
                        .equals(TerrainRecruitLoader.Keyword_AnyNonLord)))
                    || ((isLord) && (tempSrc.getCreatureName()
                        .equals(TerrainRecruitLoader.Keyword_Lord)))
                    || ((isDemiLord) && (tempSrc.getCreatureName()
                        .equals(TerrainRecruitLoader.Keyword_DemiLord))))
                {
                    if (minValue > theEdge.getNumber())
                    {
                        minValue = theEdge.getNumber();
                    }
                }
                if (tempSrc.getCreatureName().startsWith(
                    TerrainRecruitLoader.Keyword_Special))
                {
                    ICustomRecruitBase cri = TerrainRecruitLoader
                        .getCustomRecruitBase(tempSrc.getCreatureName());
                    int v = cri.numberOfRecruiterNeeded(recruiterCre,
                        recruitCre, hex);
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
     * Set the Caretaker to use for availability of creatures.
     * @param caretaker The caretaker to use subsequently.
     */
    public void setCaretaker(Caretaker caretaker)
    {
        this.caretaker = caretaker;
    }

    /**
     * Clear the graph of all Vertex & Edge.
     */
    public void clear()
    {
        caretaker = null;
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

        List<RecruitEdge> outgoing = getOutgoingEdges(cre);
        Iterator<RecruitEdge> it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = it.next();

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
     * @return A List of all Terrains where recruitment
     *     is possible.
     */
    public List<MasterBoardTerrain> getAllTerrainsWhereThisNumberOfCreatureRecruit(
        String cre, int number)
    {
        List<MasterBoardTerrain> at = new ArrayList<MasterBoardTerrain>();

        List<RecruitEdge> outgoing = getOutgoingEdges(cre);
        Iterator<RecruitEdge> it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = it.next();

            if (e.getNumber() == number)
            {
                at.add(e.getTerrain());
            }
        }
        return at;
    }

    /**
     * A list of what a creature can recruit.
     */
    public List<RecruitOption> getAllThatThisCreatureCanRecruit(String cre)
    {
        List<RecruitOption> result = new ArrayList<RecruitOption>();

        List<RecruitEdge> outgoing = getOutgoingEdges(cre);
        Iterator<RecruitEdge> it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = it.next();
            result.add(new RecruitOption(e.getTerrain(), cre, e
                .getDestination().getCreatureName(), e.getNumber()));
        }
        return result;
    }

    /**
     * A list of what can recruit a creature.
     */
    public List<RecruitOption> getAllThatCanRecruitThisCreature(String cre)
    {
        List<RecruitOption> result = new ArrayList<RecruitOption>();

        List<RecruitEdge> outgoing = getIncomingEdges(cre);
        Iterator<RecruitEdge> it = outgoing.iterator();
        while (it.hasNext())
        {
            RecruitEdge e = it.next();
            result.add(new RecruitOption(e.getTerrain(), e.getSource()
                .getCreatureName(), cre, e.getNumber()));
        }
        return result;
    }

    /**
     * Return the name of the recruit for the given number of the given
     * recruiter in the given terrain, or null if there's none.
     * @param cre The recruiting creature.
     * @param number Number of creature
     * @param t Terrain in which the recruiting may occur.
     * @return The recruit.
     */
    public CreatureType getRecruitFromRecruiterTerrainNumber(CreatureType cre,
        MasterBoardTerrain t, int number)
    {
        List<RecruitEdge> outgoing = getOutgoingEdges(cre.getName());
        CreatureType v2 = null;

        Iterator<RecruitEdge> it = outgoing.iterator();

        while (it.hasNext())
        {
            RecruitEdge e = it.next();

            if ((e.getNumber() == number) && (e.getTerrain().equals(t)))
            {
                v2 = getVariant().getCreatureByName(
                    e.getDestination().getCreatureName());
            }
        }
        return v2;
    }

    /**
     * Return the name of the best possible creature that is reachable
     * trough the given creature from the given LegionInfo (can be null).
     * @param cre The recruiting creature.
     * @param legion The recruiting legion or null.
     * @return The best possible recruit.
     */
    public CreatureType getBestPossibleRecruitEver(String cre, Legion legion)
    {
        CreatureType best = getVariant().getCreatureByName(cre);
        int maxVP = -1;
        List<RecruitVertex> all = traverse(cre, legion);
        Iterator<RecruitVertex> it = all.iterator();
        while (it.hasNext())
        {
            RecruitVertex v2 = it.next();
            CreatureType creature = getVariant().getCreatureByName(
                v2.getCreatureName());
            int vp = (creature == null ? -1 : creature.getPointValue());
            if (vp > maxVP)
            {
                maxVP = vp;
                best = creature;
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
        List<RecruitVertex> all = traverse(lesser, null);
        Iterator<RecruitVertex> it = all.iterator();
        // Log.debug("isReachable('" + lesser + "', '" + greater + "')");
        int steps = 0;
        // distance including self - i.e. distance + 1
        while (it.hasNext() && steps < distance + 1)
        {
            RecruitVertex v2 = it.next();
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
