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
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Caretaker;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.RecruitGraph;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;

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
 * @see net.sf.colossus.server.CreatureType
 */
public class TerrainRecruitLoader
{
    private static final Logger LOGGER = Logger
        .getLogger(TerrainRecruitLoader.class.getName());

    public static final String Keyword_Anything = "Anything";
    public static final String Keyword_AnyNonLord = "AnyNonLord";
    public static final String Keyword_Lord = "Lord";
    public static final String Keyword_DemiLord = "DemiLord";
    public static final String Keyword_Special = "Special:";

    /**
     * Map a terrain to a list of recruits.
     *
     * TODO integrate into {@link MasterBoardTerrain}
     */
    private static Map<MasterBoardTerrain, List<RecruitNumber>> strToRecruits = new HashMap<MasterBoardTerrain, List<RecruitNumber>>();

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
     * @see net.sf.colossus.xmlparser.TerrainRecruitLoader.AcquirableData
     */
    private static List<AcquirableData> acquirableList = null;

    /** support for the custom recruiting functions ; map the class name to an
     instance of the class. */
    private static Map<String, CustomRecruitBase> nameToInstance = new HashMap<String, CustomRecruitBase>();

    /**
     * Representation of the Recruiting Graph (for use)
     * (sometimes called Recruiting Tree).
     */
    private static RecruitGraph graph = new RecruitGraph();

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
                    CustomRecruitBase cri = getCustomRecruitBase(v2);
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
    public TerrainRecruitLoader(InputStream terIS)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(terIS);
            Element root = doc.getRootElement();

            List<Element> terrains = root.getChildren("terrain");
            for (Element el : terrains)
            {
                handleTerrain(el);
            }
            List<Element> aliases = root.getChildren("alias");
            for (Element el : aliases)
            {
                handleAlias(el);
            }

            List<Element> acquirables = root.getChildren("acquirable");
            for (Element el : acquirables)
            {
                handleAcquirable(el);
            }

            Element el = root.getChild("titan_improve");
            if (el != null)
            {
                handleTitanImprove(el);
            }

            el = root.getChild("titan_teleport");
            if (el != null)
            {
                handleTitanTeleport(el);
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

        MasterBoardTerrain terrain = new MasterBoardTerrain(name, displayName,
            HTMLColor.stringToColor(color));
        TerrainRecruitLoader.strToRecruits.put(terrain, rl);
        TerrainRecruitLoader.strToBelow.put(terrain, Boolean
            .valueOf(regularRecruit));
        // XXX Random not yet supported:
        TerrainRecruitLoader.strToRnd.put(terrain, null);

        terrains.put(name, terrain);

        addToGraph(rl, terrain);
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
        AcquirableData ad = new AcquirableData(name, points);
        String terrainId = el.getAttributeValue("terrain");
        if (terrainId != null)
        {
            MasterBoardTerrain terrain = TerrainRecruitLoader
                .getTerrainById(terrainId);
            if (terrain == null)
            {
                throw new ParseException("Illegal terrainId '" + terrainId
                    + "' in variant for aquirable '" + name + "'");
            }
            ad.addTerrain(terrain);
        }
        addAcquirable(ad);
    }

    private void handleTitanImprove(Element el) throws JDOMException
    {
        TerrainRecruitLoader.titanImprove = el.getAttribute("points")
            .getIntValue();
    }

    private void handleTitanTeleport(Element el) throws JDOMException
    {
        TerrainRecruitLoader.titanTeleport = el.getAttribute("points")
            .getIntValue();
    }

    /**
     * Return a collection of all possible terrains.
     *
     * @return A collection containing all instances of {@link MasterBoardTerrain}.
     */
    public static Collection<MasterBoardTerrain> getTerrains()
    {
        return Collections.unmodifiableCollection(terrains.values());
    }

    public static MasterBoardTerrain getTerrainById(String id)
    {
        return terrains.get(id);
    }

    /**
     * Used internally to associate a creature name and the number of
     * creatures needed to recruit it.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class RecruitNumber
    {

        /**
         * Name of the creature
         */
        private final String name;

        /**
         * Number of creatures needed to recruit it, depend on the terrain.
         */
        private final int number;

        /**
         * @param n Name of the creature
         * @param i Number of creatures needed to recruit it in the
         * terrain considered.
         */
        public RecruitNumber(String n, int i)
        {
            name = n;
            number = i;
        }

        String getName()
        {
            return name;
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
            return ("(" + number + "," + name + ")");
        }
    }

    public static CustomRecruitBase getCustomRecruitBase(String specialString)
    {
        CustomRecruitBase cri = nameToInstance.get(specialString);
        if (cri != null)
        {
            return cri;
        }
        String className = specialString.substring(8);
        Object o = net.sf.colossus.util.ResourceLoader.getNewObject(className,
            VariantSupport.getVarDirectoriesList());
        if (o == null)
        {
            LOGGER.log(Level.SEVERE, "CustomRecruitBase doesn't exist for: "
                + specialString);
            return null;
        }
        cri = (CustomRecruitBase)o;
        nameToInstance.put(specialString, cri);
        return cri;
    }

    /**
     * Give an array of the starting creatures, those available in the first
     * turn and in a particular kind of Tower.
     * @todo FIXME: this heuristic (first 3 creatures in the tower) should
     * be replaced by a real entry in the Tower terrain (similar to startlist).
     * @param hex The specific Tower considered.
     * @return an array of Creature representing the starting creatures.
     * @see net.sf.colossus.server.CreatureType
     */
    public static CreatureType[] getStartingCreatures(
        MasterHex hex)
    {
        CreatureType[] bc = new CreatureType[3];
        List<CreatureType> to = getPossibleRecruits(hex.getTerrain(), hex);
        bc[0] = to.get(0);
        bc[1] = to.get(1);
        bc[2] = to.get(2);
        return (bc);
    }

    /**
     * Tell whether given type is in the loaded variant a start creature,
     * i.e. one of those one gets in the initial legion in the tower.
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
        boolean isSC = false;
        for (MasterBoardTerrain terrain : getTerrains())
        {
            if (terrain.isTower())
            {/* temporarily broken, it needs a MasteHex now ... */
                CreatureType[] bc = getStartingCreatures(null/*terrain*/);
                if (type.equals(bc[0]) || type.equals(bc[1])
                    || type.equals(bc[2]))
                {
                    isSC = true;
                    return isSC;
                }
            }
        }
        return isSC;
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
     * @see net.sf.colossus.server.CreatureType
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
                result.add(VariantSupport.getCurrentVariant()
                    .getCreatureByName(tr.getName()));
            }
            if (tr.getName().startsWith(Keyword_Special))
            {
                CustomRecruitBase cri = getCustomRecruitBase(tr.getName());
                if (cri != null)
                {
                    List<? extends CreatureType> temp = cri
                        .getPossibleSpecialRecruits(terrain, hex);
                    result.addAll(temp);
                }
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
     * @see net.sf.colossus.server.CreatureType
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
                re.add(VariantSupport.getCurrentVariant().getCreatureByName(
                    tr.getName()));
            }
            else
            {
                if (tr.getName().equals(Keyword_Anything))
                { // anyone can recruit here...
                    return new ArrayList<CreatureType>(VariantSupport
                        .getCurrentVariant().getCreatureTypes());
                }
                if (tr.getName().equals(Keyword_AnyNonLord))
                { // anyone can recruit here...
                    // TODO: why two cases if the same result as the last one
                    return new ArrayList<CreatureType>(VariantSupport
                        .getCurrentVariant().getCreatureTypes());
                }
                if (tr.getName().equals(Keyword_Lord))
                {
                    List<CreatureType> potential = VariantSupport
                        .getCurrentVariant().getCreatureTypes();
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
                        .getCurrentVariant().getCreatureTypes();
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
                    CustomRecruitBase cri = getCustomRecruitBase(tr.getName());
                    if (cri != null)
                    {
                        List<CreatureType> temp = cri
                            .getPossibleSpecialRecruiters(terrain, hex);
                        re.addAll(temp);
                    }
                }
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
     * @see net.sf.colossus.server.CreatureType
     */
    public static int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex)
    {
        int g_value = graph.numberOfRecruiterNeeded(recruiter.getName(),
            recruit.getName(), terrain, hex);
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
     * Used internally to record the Acquirable name, points needed for
     * recruiting, and the list of terrains in which the Acquirable dwells.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class AcquirableData
    {
        private final String name;
        private final int value;
        private final List<MasterBoardTerrain> where;

        AcquirableData(String n, int v)
        {
            name = n;
            value = v;
            where = new ArrayList<MasterBoardTerrain>();
        }

        String getName()
        {
            return name;
        }

        int getValue()
        {
            return value;
        }

        void addTerrain(MasterBoardTerrain t)
        {
            where.add(t);
        }

        /**
         * Tell if the Acquirable can be Acquired in the terrain.
         * @param t The terrain in which the Acquirements occurs.
         * @return True if the Acquirable can be acquired here,
         * false otherwise.
         */
        boolean isAvailable(MasterBoardTerrain t)
        {
            if (where.isEmpty() || ((where.indexOf(t)) != -1))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        @Override
        public String toString()
        {
            return ("Acquirable by name of " + name + ", available every "
                + value + (where.isEmpty() ? "" : ", in terrain " + where));
        }
    }

    private void addAcquirable(AcquirableData ad) throws ParseException
    {
        if (acquirableList == null)
        {
            acquirableList = new ArrayList<AcquirableData>();
        }
        acquirableList.add(ad);
        if ((ad.getValue() % getAcquirableRecruitmentsValue()) != 0)
        {
            throw new ParseException("Wrong Value for an Acquirable : " + ad
                + " ; should multiple of " + getAcquirableRecruitmentsValue());
        }
    }

    /**
     * To obtain all the Creature that can be Acquired.
     * @return The list of name (as String) that can be Acquired
     */
    public static List<String> getAcquirableList()
    {
        List<String> al = new ArrayList<String>();
        Iterator<AcquirableData> it = acquirableList.iterator();
        while (it.hasNext())
        {
            AcquirableData ad = it.next();
            al.add(ad.getName());
        }
        return al;
    }

    /**
     * To obtain the base amount of points needed for Acquirement.
     * All Acquirements must occur at integer multiple of this.
     * @return The base amount of points needed for Acquirement.
     */
    public static int getAcquirableRecruitmentsValue()
    {
        AcquirableData ad = acquirableList.get(0);
        return ad.getValue();
    }

    /**
     * To obtain the first Acquirable (aka 'primary') Creature name.
     * This one is the starting Lord with the Titan.
     * @return The name of the primary Acquirable Creature.
     */
    public static String getPrimaryAcquirable()
    {
        AcquirableData ad = acquirableList.get(0);
        return ad.getName();
    }

    /**
     * To obtain all the Creature that can be acquired at the given amount of
     * points in the given terrain.
     * @param t The Terrain in which the recruitment occurs.
     * @param value The number of points at which the recruitment occurs.
     * Valid values are constrained.
     * @return The list of name (as String) that can be acquired in this
     * terrain, for this amount of points.
     * @see #getAcquirableRecruitmentsValue()
     */
    public static List<String> getRecruitableAcquirableList(
        MasterBoardTerrain t, int value)
    {
        List<String> al = new ArrayList<String>();
        if ((value % getAcquirableRecruitmentsValue()) != 0)
        {
            return al;
        }
        Iterator<AcquirableData> it = acquirableList.iterator();
        while (it.hasNext())
        {
            AcquirableData ad = it.next();
            if (ad.isAvailable(t) && ((value % ad.getValue()) == 0))
            {
                al.add(ad.getName());
            }
        }
        return al;
    }

    /**
     * Check if the Creature whose name is in parameter is an Acquirable
     * creature or not.
     * @param name The name of the Creature inquired.
     * @return If the creature is Acquirable.
     */
    public static boolean isAcquirable(String name)
    {
        Iterator<AcquirableData> it = acquirableList.iterator();
        while (it.hasNext())
        {
            AcquirableData ad = it.next();
            if (name.equals(ad.getName()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the Creature in parameter is an Acquirable creature or not.
     * @param c The Creature inquired.
     * @return If the creature is Acquirable.
     */
    public static boolean isAcquirable(CreatureType c)
    {
        return isAcquirable(c.getName());
    }

    /** Base amount of points needed for Titan improvement. */
    private static int titanImprove = 100;

    /** Amount of points needed for Titan Teleport. */
    private static int titanTeleport = 400;

    /* re-set the default values each time a new TER file is loaded */
    {
        titanImprove = 100;
        titanTeleport = 400;
    }

    /**
     * To obtain the base amount of points needed for Titan improvement.
     * @return The base amount of points needed for Titan improvement.
     */
    public static int getTitanImprovementValue()
    {
        return titanImprove;
    }

    /**
     * To obtain the amount of points needed for Titan teleport.
     * @return The amount of points needed for Titan teleport.
     */
    public static int getTitanTeleportValue()
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
}
