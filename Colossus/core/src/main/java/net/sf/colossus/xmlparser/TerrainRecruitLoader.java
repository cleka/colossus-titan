package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.RecruitGraph;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.VariantKnower;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.ObjectCreationException;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.AllCreatureType;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.ICustomRecruitBase;
import net.sf.colossus.variant.IVariantInitializer;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.RecruitingSubTree;
import net.sf.colossus.variant.Variant.AcquirableData;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * TerrainRecruitLoader load the terrains and recruits descriptions.
 *
 * TODO check if any of the methods still needs the "String terrain" parameter
 *
 * TODO we still use plenty of strings in here since the creatures are mixed with the
 *      special recruit requirements such as Anything/Lord/AnyNonLord or the custom
 *      recruits marked by the "Special:" keyword
 *
 * @author Romain Dolbeau
 * @version $Id$
 * @see net.sf.colossus.variant.CreatureType
 */
public class TerrainRecruitLoader implements IVariantInitializer
{
    private static final Logger LOGGER = Logger
        .getLogger(TerrainRecruitLoader.class.getName());

    public static final String Keyword_Anything = "Anything";
    public static final String Keyword_AnyNonLord = "AnyNonLord";
    public static final String Keyword_Lord = "Lord";
    public static final String Keyword_DemiLord = "DemiLord";
    public static final String Keyword_Special = "Special:";

    /* Only needed during loaded. During game time, this should be
     * queried from the Variant.
     */
    private int aquirableRecruitmentsValue;

    /** Base amount of points needed for Titan improvement. */
    private int titanImprove = 100;

    /** Amount of points needed for Titan Teleport. */
    private int titanTeleport = 400;


    /**
     * Map a terrain to a list of recruits.
     *
     * TODO integrate into {@link MasterBoardTerrain}
     */
    private static Map<MasterBoardTerrain, List<RecruitNumber>> strToRecruits = new HashMap<MasterBoardTerrain, List<RecruitNumber>>();
    /**
     * Map a terrain to a list of recruits.
     *
     * TODO integrate into {@link MasterBoardTerrain}
     */
    private static Map<MasterBoardTerrain, List<StartingNumber>> strToStarters = new HashMap<MasterBoardTerrain, List<StartingNumber>>();

    /**
     * Map a terrain to a boolean,
     * telling if a Creature can recruit in the usual way or not.
     *
     * TODO integrate into {@link MasterBoardTerrain}
     */
    private static Map<MasterBoardTerrain, Boolean> strToBelow = new HashMap<MasterBoardTerrain, Boolean>();

    /**
     * Map a terrain to an
     *   optional BattlelandsRandomizer filename.
     *
     * TODO integrate into {@link MasterBoardTerrain}
     */
    private static Map<MasterBoardTerrain, String> strToRnd = new HashMap<MasterBoardTerrain, String>();

    /**
     * A map from the terrain names to the terrains.
     */
    private static Map<String, MasterBoardTerrain> terrains = new HashMap<String, MasterBoardTerrain>();

    /**
     * The list of Acquirable Creature, as acquirableData.
     * @see net.sf.colossus.variant.Variant.AcquirableData
     */
    private static List<AcquirableData> acquirableList = null;

    /** support for the custom recruiting functions ; map the class name to an
     instance of the class. */
    private static Map<String, CustomRecruitBase> nameToInstance = new HashMap<String, CustomRecruitBase>();

    /**
     * Representation of the Recruiting Graph (for use)
     * (sometimes called Recruiting Tree).
     * TODO the VariantKnower is meant only as temporary solution; when
     * variant loading and all this stuff here is not static any more,
     * variant should be passed in or set afterwards or something...
     */
    private static RecruitGraph graph = new RecruitGraph(new VariantKnower());

    /** The AllCreatureType object to use, needed to convert from String (name)
     * to the actual CreatureType.
     */
    private final AllCreatureType creatureTypes;

    /**
     * set the Caretaker used by the graph
     * (needed to know what creatures are still available)
     */
    public static void setCaretaker(Caretaker caretaker)
    {
        LOGGER.log(Level.FINEST, "GRAPH: Setting the CaretakerInfo");
        graph.setCaretaker(caretaker);
    }

    private static boolean isConcreteCreature(String name)
    {
        return (!(name.equals(Keyword_Anything))
            && !(name.equals(Keyword_AnyNonLord))
            && !(name.equals(Keyword_Lord))
            && !(name.equals(Keyword_DemiLord)) && !(name
            .startsWith(Keyword_Special)));
    }

    /**
     * Add an entire terrain recruiting list to the Recruiting Graph.
     * @param rl The list of RecruitNumber to add to the graph.
     */
    private static void addToGraph(List<RecruitNumber> rl, MasterBoardTerrain t)
    {
        Iterator<RecruitNumber> it = rl.iterator();
        String v1 = null;
        boolean regularRecruit = strToBelow.get(t).booleanValue();
        try
        {
            while (it.hasNext())
            {
                RecruitNumber tr = it.next();
                String v2 = tr.getName();
                if ((v2 != null) && !(v2.equals("Titan"))
                    && isConcreteCreature(v2) && !(tr.getNumber() < 0))
                { // we musn't add the Edges going to non-recruitable
                    if (v1 != null)
                    {
                        graph.addEdge(v1, v2, tr.getNumber(), t);
                    }
                    // add the self-recruit & below-recruit loop
                    Iterator<RecruitNumber> it2 = rl.iterator();
                    boolean done = false;
                    while (it2.hasNext() && !done)
                    {
                        RecruitNumber tr2 = it2.next();
                        if ((tr == tr2) || // same List, same objects
                            regularRecruit)
                        {
                            // one can always recruit itself at one/zero
                            // level, and also below if regularRecruit is on.
                            String v3 = tr2.getName();
                            if (isConcreteCreature(v3) && !v3.equals("Titan"))
                            {
                                if ((tr2.getNumber() > 0))
                                {
                                    graph.addEdge(v2, v3, 1, t);
                                }
                                else if ((tr2.getNumber() == 0))
                                {
                                    graph.addEdge(v2, v3, 0, t);
                                }
                            }
                        }
                        if (tr == tr2)
                        {
                            done = true;
                        }
                    }
                }
                if ((v2 != null) && v2.startsWith(Keyword_Special))
                {
                    // special recruitment, need to add edge
                    // between the special aned every possible recruit
                    ICustomRecruitBase cri = getCustomRecruitBase(v2);
                    List<CreatureType> allRecruits = cri
                        .getAllPossibleSpecialRecruits(t);
                    for (CreatureType cre : allRecruits)
                    {
                        // use 99 so no-one will rely on this
                        graph.addEdge(v2, cre.getName(), RecruitGraph.BIGNUM,
                            t);
                    }
                }
                v1 = v2;
            }
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Couldn't fill graph.", e);
        }
    }

    /* make sure that all static elements are null or empty when creating a
     * new TerrainRecruitLoader object...
     */
    {
        if (acquirableList != null)
        {
            LOGGER.log(Level.FINEST,
                "TerrainRecruitLoader: Destroying previous "
                    + "``acquirableList'' ; this should never happen "
                    + "during a game...");
            acquirableList = null;
        }
        if (!terrains.isEmpty())
        {
            LOGGER.log(Level.FINEST,
                "TerrainRecruitLoader: Destroying previous "
                    + "``terrains'' ; this should never happen during "
                    + "a game...");
            terrains.clear();
        }
        strToRecruits.clear();
        strToBelow.clear();
        strToRnd.clear();
        nameToInstance.clear();
        graph.clear();
    }

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    public TerrainRecruitLoader(InputStream terIS, AllCreatureType creatureTypes)
    {
        this.creatureTypes = creatureTypes;
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(terIS);
            Element root = doc.getRootElement();

            List<Element> lterrains = root.getChildren("terrain");
            for (Element el : lterrains)
            {
                handleTerrain(el);
            }
            List<Element> aliases = root.getChildren("alias");
            for (Element el : aliases)
            {
                handleAlias(el);
            }

            if (acquirableList == null)
            {
                acquirableList = new ArrayList<AcquirableData>();
            }
            List<Element> acquirables = root.getChildren("acquirable");
            for (Element el : acquirables)
            {
                handleAcquirable(el);
            }

            Element el = root.getChild("titan_improve");
            if (el != null)
            {
                titanImprove = el.getAttribute("points").getIntValue();
            }

            el = root.getChild("titan_teleport");
            if (el != null)
            {
                titanTeleport = el.getAttribute("points").getIntValue();
            }
        }
        catch (JDOMException ex)
        {
            LOGGER.log(Level.SEVERE, "JDOM exception caught", ex);
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "IO exception caught", ex);
        }
        catch (ParseException ex)
        {
            LOGGER.log(Level.SEVERE, "Parse exception caught", ex);
        }
    }

    // we need to cast since JDOM is not generified
    @SuppressWarnings("unchecked")
    private void handleTerrain(Element el) throws JDOMException
    {
        String name = el.getAttributeValue("name");
        String displayName = el.getAttributeValue("display_name");
        if (displayName == null)
        {
            displayName = name;
        }
        String color = el.getAttributeValue("color");

        MasterBoardTerrain terrain = new MasterBoardTerrain(name, displayName,
            HTMLColor.stringToColor(color));

        ArrayList<RecruitNumber> rl = new ArrayList<RecruitNumber>();
        boolean regularRecruit = el.getAttribute("regular_recruit")
            .getBooleanValue();
        List<Element> recruits = el.getChildren("recruit");
        for (Element recruit : recruits)
        {
            String recruitName = recruit.getAttributeValue("name");
            int recruitNum = recruit.getAttribute("number").getIntValue();
            RecruitNumber rn = new RecruitNumber(recruitName, recruitNum);
            rl.add(rn);
        }

        ArrayList<StartingNumber> sl = new ArrayList<StartingNumber>();
        List<Element> starters = el.getChildren("starting");
        int total = 0;
        for (Element starter : starters)
        {
            String starterName = starter.getAttributeValue("name");
            int starterNum = starter.getAttribute("number").getIntValue();
            if (starterNum != 2)
            {
                LOGGER.warning("Only '2' is a supported value for starting"
                    + " creatures at the moment ...");
            }
            StartingNumber rn = new StartingNumber(starterName, starterNum);
            sl.add(rn);
            total += starterNum;
        }
        if (!sl.isEmpty())
        {
            if (total != 6)
            {
                LOGGER.warning("There isn't exactly 6 starting creatures in"
                    + " this terrain ! " + total + " were found in " + name);
            }
            TerrainRecruitLoader.strToStarters.put(terrain, sl);
        }

        TerrainRecruitLoader.strToRecruits.put(terrain, rl);
        TerrainRecruitLoader.strToBelow.put(terrain, Boolean
            .valueOf(regularRecruit));
        // XXX Random not yet supported:
        TerrainRecruitLoader.strToRnd.put(terrain, null);

        terrains.put(name, terrain);

        addToGraph(rl, terrain);

        RecruitingSubTree rst = buildRecruitingSubTree(rl, regularRecruit);

        terrain.setRecruitingSubTree(rst);
    }

    private RecruitingSubTree buildRecruitingSubTree(List<RecruitNumber> rl, boolean regularRecruit)
    {
        RecruitingSubTree rst = new RecruitingSubTree(this.creatureTypes);
        RecruitNumber recruiter = null;
        for (RecruitNumber recruit : rl)
        {
            if (recruit.getName().equals(Keyword_Anything) ||
                recruit.getName().equals(Keyword_AnyNonLord) ||
                recruit.getName().equals(Keyword_Lord) ||
                recruit.getName().equals(Keyword_DemiLord) ||
                recruit.getName().equals("Titan"))
            {
                recruiter = recruit;
                continue;
            }
            if (recruit.getName().startsWith(Keyword_Special))
            {
                rst.addCustom(getCustomRecruitBase(recruit.getName()));
                recruiter = null;
                continue;
            }
            if (recruit.getNumber() < 0) {
                assert regularRecruit == false : "Oups, number for recruit is " + recruit.getNumber() + " but regularRecruit is true";
                recruiter = recruit;
                continue;
            }
            if (recruiter != null)
            {
                if (recruiter.getName().equals(Keyword_Anything))
                {
                    rst.addAny(creatureTypes.getCreatureTypeByName(
                            recruit.getName()),
                            recruit.getNumber());
                }
                else if (recruiter.getName().equals(Keyword_AnyNonLord))
                {
                    rst.addNonLord(creatureTypes.getCreatureTypeByName(
                            recruit.getName()),
                            recruit.getNumber());
                }
                else if (recruiter.getName().equals(Keyword_Lord))
                {
                    rst.addLord(creatureTypes.getCreatureTypeByName(
                            recruit.getName()),
                            recruit.getNumber());
                }
                else if (recruiter.getName().equals(Keyword_DemiLord))
                {
                    rst.addDemiLord(creatureTypes.getCreatureTypeByName(
                            recruit.getName()),
                            recruit.getNumber());
                }
                else
                {
                    rst.addRegular(creatureTypes.getCreatureTypeByName(
                            recruiter.getName()),
                            creatureTypes.getCreatureTypeByName(
                            recruit.getName()),
                            recruit.getNumber());
                }
            }
            recruiter = recruit;
        }
        rst.complete(regularRecruit);
        return rst;
    }

    // we need to cast since JDOM is not generified
    private void handleAlias(Element el) throws ParseException
    {
        String name = el.getAttributeValue("name");
        String source = el.getAttributeValue("source");
        String displayName = el.getAttributeValue("display_name");
        if (displayName == null)
        {
            displayName = name;
        }
        String color = el.getAttributeValue("color");
        MasterBoardTerrain source_terrain = terrains.get(source);
        if (source_terrain == null)
        {
            throw new ParseException("Alias uses an invalid source name");
        }

        MasterBoardTerrain terrain = new MasterBoardTerrain(name, displayName,
            HTMLColor.stringToColor(color), true);

        TerrainRecruitLoader.strToRecruits.put(terrain, strToRecruits
            .get(source_terrain));
        TerrainRecruitLoader.strToBelow.put(terrain, strToBelow
            .get(source_terrain));
        // XXX Random not yet supported:
        TerrainRecruitLoader.strToRnd.put(terrain, null);

        terrains.put(name, terrain);

        addToGraph(strToRecruits.get(source_terrain), terrain);

        source_terrain.addAlias(terrain);

        terrain.setRecruitingSubTree(source_terrain.getRecruitingSubTree());
    }

    private void handleAcquirable(Element el) throws JDOMException,
        ParseException
    {
        String name = el.getAttribute("name").getValue();
        if (name == null)
        {
            throw new ParseException("Acquirable is missing name attribute");
        }
        // TODO the name attribute should be validated to be an actual creature
        // name. Currently this is not yet possible since we don't necessarily
        // have the creatures yet. It could be fixed with a quick hack, reordering
        // the loading process and passing the List<CreatureType> around, but the
        // proper solution would be passing the results of this class as objects
        // around instead of using static code, then validate consistency on
        // construction of the Variant instance, which would get all the necessary
        // information to do that
        int points = el.getAttribute("points").getIntValue();
        if (points == 0)
        {
            throw new ParseException("Acquirable '" + name
                + "' has invalid points");
        }

        List<MasterBoardTerrain> terrains = new ArrayList<MasterBoardTerrain>();

        String terrainId = el.getAttributeValue("terrain");
        if (terrainId != null)
        {
            MasterBoardTerrain terrain = getTerrainById(terrainId);
            if (terrain == null)
            {
                throw new ParseException("Illegal terrainId '" + terrainId
                    + "' in variant for aquirable '" + name + "'");
            }
            terrains.add(terrain);
        }

        if (acquirableList.size() == 0)
        { // First acquirable - initialize the base value
            aquirableRecruitmentsValue = points;
        }

        if ((points % aquirableRecruitmentsValue) != 0)
        {
            throw new ParseException("Wrong point value " + points
                + " for Acquirable with name=" + name + " in terrain="
                + terrainId + " : should be multiple of "
                + aquirableRecruitmentsValue);
        }

        AcquirableData ad = new AcquirableData(name, points, terrains);
        acquirableList.add(ad);
    }

    /**
     * Return a collection of all possible terrains.
     * TODO get rid of this. Only once place still needs it.
     *
     * @return A collection containing all instances of {@link MasterBoardTerrain}.
     */
    public static Collection<MasterBoardTerrain> getTerrainsStatic()
    {
        return Collections.unmodifiableCollection(terrains.values());
    }

    /**
     * Return a collection of all possible terrains.
     * NOTE: Only meant to be used for Variant Initialization!
     *       In normal cases this list should be get from variant object.
     *
     * @return A collection containing all instances of {@link MasterBoardTerrain}.
     */
    public Collection<MasterBoardTerrain> getTerrains()
    {
        return Collections.unmodifiableCollection(terrains.values());
    }

    protected static MasterBoardTerrain getTerrainById(String id)
    {
        return terrains.get(id);
    }

    /** Helper class, associating a Creature and a Number
     * The basic identification is the name (because of the hack of using
     * special name for special stuff...) but the CreatureType is there to
     * avoid reloading from the Variant all the time.
     * We can't look-up at creation time, because the variant isn't available
     * yet, so we delay until the first call to getCreature.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private abstract class CreatureAndNumber
    {
        /**
         * The Creature in the pair (if it exists)
         */
        private CreatureType creature = null;
        /**
         * The Name
         */
        private final String name;

        /**
         * The Number in the pair
         */
        private final int number;

        private boolean checked = false;

        /**
         * @param n The Name of the creature
         * @param i The Number
         */
        public CreatureAndNumber(String n, int i)
        {
            name = n;
            number = i;
        }

        String getName()
        {
            return name;
        }

        CreatureType getCreature()
        {
            if (!checked)
            {
                checked = true;
                if (isConcreteCreature(name))
                {
                    creature = VariantSupport.getCurrentVariant()
                        .getCreatureByName(name);
                }
                else
                {
                    creature = null;
                }
            }
            assert (creature != null);
            return creature;
        }

        int getNumber()
        {
            return number;
        }

        /**
         * Textual representation of the data.
         * @return Textual representation of the data as a String.
         */
        @Override
        public String toString()
        {
            return ("(" + getNumber() + "," + getName() + ")");
        }
    }

    /**
     * Used internally to associate a creature name and the number of
     * creatures needed to recruit it.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class RecruitNumber extends CreatureAndNumber
    {

        /**
         * @param n Name of the creature
         * @param i Number of creatures needed to recruit it in the
         * terrain considered.
         */
        public RecruitNumber(String n, int i)
        {
            super(n, i);
        }
    }

    /**
     * Used internally to associate a creature name and the number received
     * when starting a game.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class StartingNumber extends CreatureAndNumber
    {

        /**
         * @param n Name of the creature
         * @param i Number of creatures when starting.
         */
        public StartingNumber(String n, int i)
        {
            super(n, i);
        }
    }

    public static ICustomRecruitBase getCustomRecruitBase(String specialString)
    {
        CustomRecruitBase cri = nameToInstance.get(specialString);
        if (cri != null)
        {
            return cri;
        }
        String className = specialString.substring(8);
        try
        {
            Object o = StaticResourceLoader.getNewObject(className,
                VariantSupport.getVarDirectoriesList());
            cri = (CustomRecruitBase)o;
            nameToInstance.put(specialString, cri);
            return cri;
        }
        catch (ObjectCreationException e)
        {
            // TODO it might be better to throw an exception to avoid fail late scenarios
            LOGGER.log(Level.SEVERE, "CustomRecruitBase doesn't exist for: "
                + specialString);
            return null;
        }
    }

    /**
     * Give an array of the starting creatures, those available in the first
     * turn and in a particular kind of Tower.
     * TODO this heuristic (first 3 creatures in the tower) should
     * be replaced by a real entry in the Tower terrain (similar to startlist).
     * @param hex The specific Tower considered.
     * @return an array of Creature representing the starting creatures.
     * @see net.sf.colossus.variant.CreatureType
     */
    public static CreatureType[] getStartingCreatures(MasterHex hex)
    {
        List<StartingNumber> sl = strToStarters.get(hex.getTerrain());
        if ((sl == null) || sl.isEmpty())
        {
            if (!hex.getTerrain().isTower())
            {
                LOGGER.warning("getStartingCreatures should not be called"
                    + " on a terrain that is'nt a Tower and hasn't a list "
                    + "of starting creatures.");
                return null;
            }
            LOGGER.warning("No starting creatures found in Tower "
                + hex.getLabel()
                + ", please fix the variant. Using first three creatures in"
                + " the recruiting tree instead.");
            CreatureType[] bc = new CreatureType[3];
            List<CreatureType> to = getPossibleRecruits(hex.getTerrain(), hex);
            bc[0] = to.get(0);
            bc[1] = to.get(1);
            bc[2] = to.get(2);
            return (bc);
        }
        CreatureType[] bc = new CreatureType[sl.size()];
        for (int i = 0; i < bc.length; i++)
        {
            bc[i] = sl.get(i).getCreature();
        }
        return bc;
    }

    /**
     * Tell whether given type is in the loaded variant a start creature,
     * i.e. one of those one gets in the initial legion in the tower (any
     * tower).
     *
     * I plan to use this for e.g. HexRecruitTreePanel, to show there
     * how one can get to have a certain creature:
     * start creature
     * -or- acquirable
     * -or- recruitable by N of from prev. in tree,
     * -or- recruitable by any/Lord/DemiLord/anyNonLord
     * -or- recruitable by N of something else (e.g. Titan=>Warlock)
     * @param type
     * @return true if this is a start creature in the loaded variant
     */
    public static boolean isStartCreature(CreatureType type)
    {
        String name = type.getName();
        for (List<StartingNumber> sl : strToStarters.values())
        {
            for (StartingNumber sn : sl)
            {
                if (sn.getName().equals(name))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Give the name of the random filename to use to generate this terrain,
     * or null if it's a static Battlelands.
     *
     * @param masterBoardTerrain A master board terrain.
     * @return The name of the random source file as a String
     */
    public static String getTerrainRandomName(
        MasterBoardTerrain masterBoardTerrain)
    {
        return strToRnd.get(masterBoardTerrain);
    }

    /**
     * Give a modifiable list of the possible recruits in a terrain.
     * @param terrain The terrain to consider.
     * @param hex The specific hex to consider. It shouldn't be null during
     * the actual recruiting, but it can be null when doing evaluation (it's
     * only used for special recruiting in custom variants).
     * @return List of Creatures that can be recruited in the terrain.
     * @see net.sf.colossus.variant.CreatureType
     */
    public static List<CreatureType> getPossibleRecruits(
        MasterBoardTerrain terrain, MasterHex hex)
    {
        List<RecruitNumber> al = strToRecruits.get(terrain);
        List<CreatureType> result = new ArrayList<CreatureType>();
        Iterator<RecruitNumber> it = al.iterator();
        while (it.hasNext())
        {
            RecruitNumber tr = it.next();
            if ((tr.getNumber() >= 0) && isConcreteCreature(tr.getName())
                && !tr.getName().equals("Titan"))
            {
                result.add(tr.getCreature());
            }
            if (tr.getName().startsWith(Keyword_Special))
            {
                ICustomRecruitBase cri = getCustomRecruitBase(tr.getName());
                if (cri != null)
                {
                    List<? extends CreatureType> temp = cri
                        .getPossibleSpecialRecruits(hex);
                    result.addAll(temp);
                }
            }
        }

        Set<CreatureType> theSet = terrain.getRecruitingSubTree().
                getPossibleRecruits(hex);
        Set<CreatureType> theSet2 = new TreeSet<CreatureType>(result);
        if (!theSet.equals(theSet2))
        {
            LOGGER.warning("Oups, discrepancy between old (graph-based) and " +
                    "new (RST-based) values for getPossibleRecruits");
            LOGGER.warning("Old one is:");
            for (CreatureType ct : theSet2)
            {
                LOGGER.warning("\t" + ct.getName());
            }
            LOGGER.warning("New one is:");
            for (CreatureType ct : theSet)
            {
                LOGGER.warning("\t" + ct.getName());
            }
        }

        return result;
    }

    /**
     * Give a modifiable list of the possible recruiters in a terrain.
     *
     * TODO if clients need to modify they should make copies themselves, it
     * seems better if have this class return an unmodifiable list
     *
     * @param terrain String representing a terrain.
     * @return List of Creatures that can recruit in the terrain.
     * @see net.sf.colossus.variant.CreatureType
     */
    public static List<CreatureType> getPossibleRecruiters(
        MasterBoardTerrain terrain, MasterHex hex)
    {
        List<RecruitNumber> al = strToRecruits.get(terrain);
        List<CreatureType> re = new ArrayList<CreatureType>();
        Iterator<RecruitNumber> it = al.iterator();
        while (it.hasNext())
        {
            RecruitNumber tr = it.next();
            if (isConcreteCreature(tr.getName()))
            {
                re.add(tr.getCreature());
            }
            else
            {
                if (tr.getName().equals(Keyword_Anything))
                { // anyone can recruit here...
                    return new ArrayList<CreatureType>(VariantSupport
                        .getCurrentVariant().getCreatureTypesAsList());
                }
                if (tr.getName().equals(Keyword_AnyNonLord))
                { // anyone can recruit here...
                    // TODO: why two cases if the same result as the last one
                    return new ArrayList<CreatureType>(VariantSupport
                        .getCurrentVariant().getCreatureTypesAsList());
                }
                if (tr.getName().equals(Keyword_Lord))
                {
                    List<CreatureType> potential = VariantSupport
                        .getCurrentVariant().getCreatureTypesAsList();
                    Iterator<CreatureType> itCr = potential.iterator();
                    while (itCr.hasNext())
                    {
                        CreatureType creature = itCr.next();
                        if (creature.isLord())
                        {
                            re.add(creature);
                        }
                    }
                }
                if (tr.getName().equals(Keyword_DemiLord))
                {
                    List<CreatureType> potential = VariantSupport
                        .getCurrentVariant().getCreatureTypesAsList();
                    Iterator<CreatureType> itCr = potential.iterator();
                    while (itCr.hasNext())
                    {
                        CreatureType creature = itCr.next();
                        if (creature.isDemiLord())
                        {
                            re.add(creature);
                        }
                    }
                }
                if (tr.getName().startsWith(Keyword_Special))
                {
                    ICustomRecruitBase cri = getCustomRecruitBase(tr.getName());
                    if (cri != null)
                    {
                        List<CreatureType> temp = cri
                            .getPossibleSpecialRecruiters(hex);
                        re.addAll(temp);
                    }
                }
            }
        }

        Set<CreatureType> theSet = terrain.getRecruitingSubTree().
                getPossibleRecruiters(hex);
        Set<CreatureType> theSet2 = new TreeSet<CreatureType>(re);
        if (!theSet.equals(theSet2))
        {
            LOGGER.warning("Oups, discrepancy between old (graph-based) and " +
                    "new (RST-based) values for getPossibleRecruiters");
            LOGGER.warning("Old one is:");
            for (CreatureType ct : theSet2)
            {
                LOGGER.warning("\t" + ct.getName());
            }
            LOGGER.warning("New one is:");
            for (CreatureType ct : theSet)
            {
                LOGGER.warning("\t" + ct.getName());
            }
        }

        return (re);
    }

    /**
     * Give the number of a given recruiters needed to recruit a given
     * Creature.
     *
     * TODO do we need the terrain parameter
     *
     * @param recruiter The Creature that wish to recruit.
     * @param recruit The Creature that is to be recruited.
     * @param terrain String representing a terrain, in which the
     * recruiting occurs.
     * @return Number of recruiter needed.
     * @see net.sf.colossus.variant.CreatureType
     */
    public static int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex)
    {
        int g_value = graph.numberOfRecruiterNeeded(recruiter.getName(),
            recruit.getName(), terrain, hex);

        int theNumber = terrain.getRecruitingSubTree().numberOfRecruiterNeeded(
                recruiter, recruit, hex);

        if (g_value != theNumber) {
            LOGGER.warning("Oups, discrepancy between old (graph-based) and "+
                    "new (RST-based) values for numberOfRecruiterNeeded ; " +
                    "old is " + g_value + " while new is " + theNumber +
                    " when " + recruiter.getName() + " recruits " +
                    recruit.getName() + " in " + terrain.getId() + " on hex " +
                    hex.getLabel());
            LOGGER.warning("The RST is\n" + terrain.getRecruitingSubTree().toString());
        }

        return g_value;
    }

    public static boolean anonymousRecruitLegal(CreatureType recruit,
        MasterBoardTerrain terrain, MasterHex hex)
    {
        int g_value = graph.numberOfRecruiterNeeded(Keyword_Anything, recruit
            .getName(), terrain, hex);
        if (g_value != 0)
        { // we really should ensure the caller *has* AnyNonLord...
            g_value = graph.numberOfRecruiterNeeded(Keyword_AnyNonLord,
                recruit.getName(), terrain, hex);
        }
        return (g_value == 0);
    }

    /**
     * To obtain the base amount of points needed for Titan improvement.
     * @return The base amount of points needed for Titan improvement.
     */
    public int getTitanImprovementValue()
    {
        return titanImprove;
    }

    /**
     * To obtain the amount of points needed for Titan teleport.
     * @return The amount of points needed for Titan teleport.
     */
    public int getTitanTeleportValue()
    {
        return titanTeleport;
    }

    /**
     * to obtain the recruit graph
     */
    public static RecruitGraph getRecruitGraph()
    {
        return graph;
    }

    public List<AcquirableData> getAcquirablesList()
    {
        return acquirableList;
    }


    public static class NullTerrainRecruitLoader implements
        IVariantInitializer
    {
        private static final Logger LOGGER = Logger
            .getLogger(TerrainRecruitLoader.NullTerrainRecruitLoader.class
                .getName());

        private final boolean showNullWarning;

        /**
         * Create an do-basically-Nothing TerrainRecruitLoader that can
         * be used as TerrainInitialiser e.g. during Unit Testing.
         * In real games normally a real TerrainRecruitLoader should be used,
         * accessed via the IVariantInitializer interface.
         * But the variable to hold the trl should be initialized with
         * something to avoid NPEs...
         * This one here serves that purpose, but it will then show warnings
         * when querying values from it.
         *
         * @param showNullWarning Set to true if you really want to use the
         * defaults and not get warnings about querying them.
         * Intended for unit testing setup.
         */
        public NullTerrainRecruitLoader(boolean showNullWarning)
        {
            this.showNullWarning = showNullWarning;
        }

        public NullTerrainRecruitLoader()
        {
            this(true);
        }

        public List<AcquirableData> getAcquirablesList()
        {
            // TODO Auto-generated method stub
            return new ArrayList<AcquirableData>();
        }

        public Collection<MasterBoardTerrain> getTerrains()
        {
            // TODO Auto-generated method stub
            return new ArrayList<MasterBoardTerrain>();
        }

        public int getTitanImprovementValue()
        {
            // TODO Auto-generated method stub
            warnThatNullTerrainRecruitLoader("getTitanImprovementValue");
            return 100;
        }

        public int getTitanTeleportValue()
        {
            // TODO Auto-generated method stub
            return 400;
        }

        private void warnThatNullTerrainRecruitLoader(String message)
        {
            if (showNullWarning)
            {
                LOGGER
                    .warning("You are querying the value for "
                        + message
                        + " from a NullTerrainRecruitLoader. Are you sure this is what you want?");
            }
        }

    }
}
