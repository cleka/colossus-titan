package net.sf.colossus.xmlparser;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.CaretakerInfo;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.CustomRecruitBase;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.RecruitGraph;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * TerrainRecruitLoader load the terrains and recruits descriptions.
 * @author Romain Dolbeau
 * @version $Id$
 * @see net.sf.colossus.server.Creature
 */
public class TerrainRecruitLoader
{
    private static final Logger LOGGER = Logger.getLogger(TerrainRecruitLoader.class.getName());

    public static final String Keyword_Anything = "Anything";
    public static final String Keyword_AnyNonLord = "AnyNonLord";
    public static final String Keyword_Lord = "Lord";
    public static final String Keyword_Special = "Special:";

    /**
     * Map a String (representing a terrain) to a list of recruits.
     */
    private static HashMap strToRecruits = new HashMap();

    /**
     * Map a String (representing a terrain) to a terrain display name.
     */

    private static HashMap strToDisplayName = new HashMap();

    /**
     * Map a String (representing a terrain) to a terrain color.
     */
    private static HashMap strToColor = new HashMap();

    /**
     * Map a String (representing a terrain) to a boolean,
     * telling if a Creature can recruit in the usual way or not.
     */
    private static HashMap strToBelow = new HashMap();

    /**
     * Map a String (representing a terrain) to an 
     *   optional BattlelandsRandomizer filename.
     */
    private static HashMap strToRnd = new HashMap();

    /**
     * All the Strings that are valid terrains.
     */
    private static String[] terrains = null;

    /**
     * The list of Acquirable Creature, as acquirableData.
     * @see net.sf.colossus.xmlparser.TerrainRecruitLoader.acquirableData
     */
    private static List acquirableList = null;

    /** support for the custom recruiting functions ; map the class name to an
     instance of the class. */
    private static HashMap nameToInstance = new HashMap();

    /**
     * Representation of the Recruiting Graph (for use)
     * (sometimes called Recruiting Tree).
     */
    private static RecruitGraph graph = new RecruitGraph();

    /**
     * set the CaretakerInfo used by the graph
     * (needed to know what creatures are still available)
     */
    public static void setCaretakerInfo(CaretakerInfo caretakerInfo)
    {
        LOGGER.log(Level.FINEST, "GRAPH: Setting the CaretakerInfo");
        graph.setCaretakerInfo(caretakerInfo);
    }

    /**
     * Add an entire terrain recruiting list to the Recruiting Graph.
     * @param rl The list of RecruitNumber to add to the graph.
     */
    private static void addToGraph(ArrayList rl, String t)
    {
        Iterator it = rl.iterator();
        String v1 = null;
        boolean regularRecruit =
            ((Boolean)strToBelow.get(t)).booleanValue();
        try
        {
            while (it.hasNext())
            {
                recruitNumber tr = (recruitNumber)it.next();
                String v2 = tr.getName();
                if ((v2 != null) &&
                    !(v2.equals(Keyword_Anything)) &&
                    !(v2.equals(Keyword_AnyNonLord)) &&
                    !(v2.equals("Titan")) &&
                    !(v2.equals(Keyword_Lord)) &&
                    !(v2.startsWith(Keyword_Special)) &&
                    !(tr.getNumber() < 0))
                { // we musn't add the Edges going to non-recruitable
                    if (v1 != null)
                    {
                        graph.addEdge(v1, v2, tr.getNumber(), t);
                    }
                    // add the self-recruit & below-recruit loop
                    Iterator it2 = rl.iterator();
                    boolean done = false;
                    while (it2.hasNext() && !done)
                    {
                        recruitNumber tr2 = (recruitNumber)it2.next();
                        if ((tr == tr2) || // same List, same objects
                            regularRecruit)
                        {
                            // one can always recruit itself at one/zero
                            // level, and also below if regularRecruit is on.
                            String v3 = tr2.getName();
                            if (!(v3.equals(Keyword_Anything)) &&
                                !(v3.equals(Keyword_AnyNonLord)) &&
                                !(v3.equals("Titan")) &&
                                !(v3.equals(Keyword_Lord)) &&
                                !(v3.startsWith(Keyword_Special)))
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
                    java.util.List allRecruits =
                        cri.getAllPossibleSpecialRecruits(t);
                    Iterator it3 = allRecruits.iterator();
                    while (it3.hasNext())
                    {
                        Creature cre = (Creature)it3.next();
                        // use 99 so no-one will rely on this
                        graph.addEdge(v2, cre.getName(),
                            RecruitGraph.BIGNUM, t);
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
                "TerrainRecruitLoader: Destroying previous " +
                "``acquirableList'' ; this should never happen " +
                "during a game...");
            acquirableList = null;
        }
        if (terrains != null)
        {
            LOGGER.log(Level.FINEST,
                "TerrainRecruitLoader: Destroying previous " +
                "``terrains'' ; this should never happen during " +
                "a game...");
            terrains = null;
        }
        strToRecruits.clear();
        strToDisplayName.clear();
        strToColor.clear();
        strToBelow.clear();
        strToRnd.clear();
        nameToInstance.clear();
        graph.clear();
    }

    public TerrainRecruitLoader(InputStream terIS)
    {
        SAXBuilder builder = new SAXBuilder();
        try
        {
            Document doc = builder.build(terIS);
            Element root = doc.getRootElement();

            List terrains = root.getChildren("terrain");
            for (Iterator it = terrains.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
                handleTerrain(el);
            }

            List acquirables = root.getChildren("acquirable");
            for (Iterator it = acquirables.iterator(); it.hasNext();)
            {
                Element el = (Element)it.next();
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

    private void handleTerrain(Element el)
        throws JDOMException
    {
        String name = el.getAttributeValue("name");
        String displayName = el.getAttributeValue("display_name");
        if (displayName == null)
        {
            displayName = name;
        }
        String color = el.getAttributeValue("color");
        ArrayList rl = new ArrayList();

        boolean regularRecruit = el.getAttribute(
            "regular_recruit").getBooleanValue();
        List recruits = el.getChildren("recruit");
        for (Iterator it = recruits.iterator(); it.hasNext();)
        {
            Element recruit = (Element)it.next();
            String recruitName = recruit.getAttributeValue("name");
            int recruitNum = recruit.getAttribute("number").getIntValue();
            recruitNumber rn = new recruitNumber(recruitName, recruitNum);
            rl.add(rn);
        }

        TerrainRecruitLoader.strToRecruits.put(name, rl);
        TerrainRecruitLoader.strToDisplayName.put(name, displayName);
        TerrainRecruitLoader.strToColor
            .put(name, HTMLColor.stringToColor(color));
        TerrainRecruitLoader.strToBelow
            .put(name, new Boolean(regularRecruit));
        // XXX Random not yet supported:            
        TerrainRecruitLoader.strToRnd.put(name, null);

        if (TerrainRecruitLoader.terrains == null)
        {
            TerrainRecruitLoader.terrains = new String[1];
            TerrainRecruitLoader.terrains[0] = name;
        }
        else
        {
            String[] t2 = new String[TerrainRecruitLoader.terrains.length + 1];
            for (int i = 0; i < TerrainRecruitLoader.terrains.length; i++)
            {
                t2[i] = TerrainRecruitLoader.terrains[i];
            }
            t2[TerrainRecruitLoader.terrains.length] = name;
            TerrainRecruitLoader.terrains = t2;
        }

        addToGraph(rl, name);
    }

    private void handleAcquirable(Element el)
        throws JDOMException, ParseException
    {
        String name = el.getAttribute("name").getValue();
        int points = el.getAttribute("points").getIntValue();
        String terrain = el.getAttributeValue("terrain");
        acquirableData ad = new acquirableData(name, points);
        if (terrain != null)
        {
            ad.addTerrain(terrain);
        }
        addAcquirable(ad);
    }

    private void handleTitanImprove(Element el)
        throws JDOMException
    {
        TerrainRecruitLoader.titanImprove =
            el.getAttribute("points").getIntValue();
    }

    private void handleTitanTeleport(Element el)
        throws JDOMException
    {
        TerrainRecruitLoader.titanTeleport =
            el.getAttribute("points").getIntValue();
    }

    /**
     * Return an array of all the String representing a valid terrain.
     * @return An array of String, each representing a valid terrain.
     */
    public static String[] getTerrains()
    {
        return (String[])terrains.clone();
    }

    /**
     * Used internally to associate a creature name and the number of
     * creatures needed to recruit it.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class recruitNumber
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
        public recruitNumber(String n, int i)
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
        public String toString()
        {
            return("(" + number + "," + name + ")");
        }
    }
    public static CustomRecruitBase getCustomRecruitBase(String specialString)
    {
        CustomRecruitBase cri =
            (CustomRecruitBase)nameToInstance.get(specialString);
        if (cri != null)
        {
            return cri;
        }
        String className = specialString.substring(8);
        Object o = net.sf.colossus.util.ResourceLoader.getNewObject(className,
            VariantSupport.getVarDirectoriesList());
        if (o == null)
        {
            LOGGER.log(Level.SEVERE,
                "CustomRecruitBase doesn't exist for: " + specialString);
            return null;
        }
        cri = (CustomRecruitBase)o;
        nameToInstance.put(specialString, cri);
        return cri;
    }

    /**
     * Give an array of the starting creatures, those available in the first
     * turn and in a particular kind of Tower.
     * @param terrain The kind of Tower considered.
     * @return an array of Creature representing the starting creatures.
     * @see net.sf.colossus.server.Creature
     */
    public static Creature[] getStartingCreatures(String terrain)
    {
        Creature[] bc = new Creature[3];
        java.util.List to = getPossibleRecruits(terrain, null);
        bc[0] = (Creature)to.get(0);
        bc[1] = (Creature)to.get(1);
        bc[2] = (Creature)to.get(2);
        return(bc);
    }

    /**
     * Give the display name of the terrain.
     * @param tc String representing a terrain.
     * @return The display name of the terrain as a String.
     */
    public static String getTerrainDisplayName(String tc)
    {
        return((String)strToDisplayName.get(tc));
    }

    /**
     * Give the color of the terrain.
     * @param tc String representing a terrain.
     * @return The color of the terrain as Color.
     */
    public static java.awt.Color getTerrainColor(String tc)
    {
        return((java.awt.Color)strToColor.get(tc));
    }

    /**
     * Give the name of the random filename to use to generate this terrain,
     * or null if it's a static Battlelands.
     * @param tc String representing a terrain.
     * @return The name of the random source file as a String
     */
    public static String getTerrainRandomName(String tc)
    {
        return((String)strToRnd.get(tc));
    }

    /**
     * Give a modifiable list of the possible recruits in a terrain.
     * @param terrain String representing a terrain.
     * @return List of Creatures that can be recruited in the terrain.
     * @see net.sf.colossus.server.Creature
     */
    public static java.util.List getPossibleRecruits(String terrain,
        String hexLabel)
    {
        ArrayList al = (ArrayList)strToRecruits.get(terrain);
        ArrayList re = new ArrayList();
        Iterator it = al.iterator();
        while (it.hasNext())
        {
            recruitNumber tr = (recruitNumber)it.next();
            if ((tr.getNumber() >= 0) &&
                !(tr.getName().equals(Keyword_Anything)) &&
                !(tr.getName().equals(Keyword_AnyNonLord)) &&
                !(tr.getName().equals("Titan")) &&
                !(tr.getName().equals(Keyword_Lord)) &&
                !(tr.getName().startsWith(Keyword_Special)))
            {
                re.add(Creature.getCreatureByName(tr.getName()));
            }
            if (tr.getName().startsWith(Keyword_Special))
            {
                CustomRecruitBase cri =
                    getCustomRecruitBase(tr.getName());
                if (cri != null)
                {
                    List temp = cri.getPossibleSpecialRecruits(terrain,
                        hexLabel);
                    re.addAll(temp);
                }
            }
        }
        return(re);
    }

    /**
     * Give a modifiable list of the possible recruiters in a terrain.
     * @param terrain String representing a terrain.
     * @return List of Creatures that can recruit in the terrain.
     * @see net.sf.colossus.server.Creature
     */
    public static java.util.List getPossibleRecruiters(String terrain,
        String hexLabel)
    {
        ArrayList al = (ArrayList)strToRecruits.get(terrain);
        ArrayList re = new ArrayList();
        Iterator it = al.iterator();
        while (it.hasNext())
        {
            recruitNumber tr = (recruitNumber)it.next();
            if (!(tr.getName().equals(Keyword_Anything)) &&
                !(tr.getName().equals(Keyword_AnyNonLord)) &&
                !(tr.getName().equals(Keyword_Lord)) &&
                !(tr.getName().startsWith(Keyword_Special)))
            {
                re.add(Creature.getCreatureByName(tr.getName()));
            }
            else
            {
                if (tr.getName().equals(Keyword_Anything))
                { // anyone can recruit here...
                    java.util.List creatures = Creature.getCreatures();
                    return(new ArrayList(creatures));
                }
                if (tr.getName().equals(Keyword_AnyNonLord))
                { // anyone can recruit here...
                    java.util.List creatures = Creature.getCreatures();
                    return(new ArrayList(creatures));
                }
                if (tr.getName().equals(Keyword_Lord))
                {
                    java.util.List potential = Creature.getCreatures();
                    ListIterator lit = potential.listIterator();
                    while (lit.hasNext())
                    {
                        Creature creature = (Creature)lit.next();
                        if (creature.isLord())
                        {
                            re.add(creature);
                        }
                    }
                }
                if (tr.getName().startsWith(Keyword_Special))
                {
                    CustomRecruitBase cri =
                        getCustomRecruitBase(tr.getName());
                    if (cri != null)
                    {
                        List temp = cri.getPossibleSpecialRecruiters(terrain,
                            hexLabel);
                        re.addAll(temp);
                    }
                }
            }
        }
        return(re);
    }

    /**
     * Give the number of a given recruiters needed to recruit a given
     * Creature.
     * @param recruiter The Creature that wish to recruit.
     * @param recruit The Creature that is to be recruited.
     * @param terrain String representing a terrain, in which the
     * recruiting occurs.
     * @return Number of recruiter needed.
     * @see net.sf.colossus.server.Creature
     */
    public static int numberOfRecruiterNeeded(Creature recruiter,
        Creature recruit,
        String terrain,
        String hexLabel)
    {
        int g_value = graph.numberOfRecruiterNeeded(recruiter.getName(),
            recruit.getName(),
            terrain,
            hexLabel);
        return g_value;
    }

    public static boolean anonymousRecruitLegal(Creature recruit,
        String terrain,
        String hexLabel)
    {
        int g_value = graph.numberOfRecruiterNeeded(Keyword_Anything,
            recruit.getName(),
            terrain,
            hexLabel);
        if (g_value != 0)
        { // we really shoud ensure the caller *has* AnyNonLord...
            g_value = graph.numberOfRecruiterNeeded(Keyword_AnyNonLord,
                recruit.getName(),
                terrain,
                hexLabel);
        }
        return (g_value == 0);
    }

    /**
     * Used internally to record the Acquirable name, points needed for
     * recruitment, and the list of terrains in which the Acquirable dwells.
     * @author Romain Dolbeau
     * @version $Id$
     */
    private class acquirableData
    {
        private final String name;
        private final int value;
        private final List where;
        acquirableData(String n, int v)
        {
            name = n;
            value = v;
            where = new ArrayList();
        }

        String getName()
        {
            return name;
        }

        int getValue()
        {
            return value;
        }

        void addTerrain(String t)
        {
            where.add(t);
        }

        /**
         * Tell if the Acquirable can be Acquired in the terrain.
         * @param t The terrain in which the Acquirements occurs.
         * @return True if the Acquirable can be acquired here,
         * false otherwise.
         */
        boolean isAvailable(String t)
        {
            if (where.isEmpty() ||
                ((where.indexOf(t)) != -1))
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        public String toString()
        {
            return("Acquirable by name of " + name +
                ", available every " + value +
                (where.isEmpty() ? "" : ", in terrain " + where));
        }
    }

    private void addAcquirable(acquirableData ad)
        throws ParseException
    {
        if (acquirableList == null)
        {
            acquirableList = new ArrayList();
        }
        acquirableList.add(ad);
        if ((ad.getValue() % getAcquirableRecruitmentsValue()) != 0)
        {
            throw new ParseException("Wrong Value for an Acquirable : " +
                ad + " ; should multiple of " +
                getAcquirableRecruitmentsValue());
        }
    }

    /**
     * To obtain all the Creature that can be Acquired.
     * @return The list of name (as String) that can be Acquired
     */
    public static List getAcquirableList()
    {
        List al = new ArrayList();
        Iterator it = acquirableList.iterator();
        while (it.hasNext())
        {
            acquirableData ad = (acquirableData)it.next();
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
        acquirableData ad = (acquirableData)acquirableList.get(0);
        return ad.getValue();
    }

    /**
     * To obtain the first Acquirable (aka 'primary') Creature name.
     * This one is the starting Lord with the Titan.
     * @return The name of the primary Acquirable Creature.
     */
    public static String getPrimaryAcquirable()
    {
        acquirableData ad = (acquirableData)acquirableList.get(0);
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
    public static List getRecruitableAcquirableList(String t, int value)
    {
        List al = new ArrayList();
        if ((value % getAcquirableRecruitmentsValue()) != 0)
        {
            return al;
        }
        Iterator it = acquirableList.iterator();
        while (it.hasNext())
        {
            acquirableData ad = (acquirableData)it.next();
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
    public boolean isAcquirable(String name)
    {
        Iterator it = acquirableList.iterator();
        while (it.hasNext())
        {
            acquirableData ad = (acquirableData)it.next();
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
    public boolean isAcquirable(Creature c)
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
     * To obtain the amount of points needed for Titan Teleport.
     * @return The amount of points needed for Titan Teleport.
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
