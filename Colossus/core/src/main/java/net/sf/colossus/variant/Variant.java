package net.sf.colossus.variant;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.swing.text.Document;

import net.sf.colossus.game.Game;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;


/**
 * Hub for all variant-specific information.
 *
 * This class is meant to give access to all the information about a Colossus
 * game in the static sense: the master board layout, the battle board layouts,
 * available creatures, rules, etc. The information about a game in progress is
 * in the {@link Game} class.
 *
 * Instances of this class are immutable.
 *
 * TODO add access to the markers by having a class for them
 * TODO same thing for the colors/markersets
 */
public class Variant
{
    private static final Logger LOGGER = Logger.getLogger(Variant.class
        .getName());

    private final AllCreatureType creatureTypes;
    private final List<CreatureType> summonableCreatureTypes;
    private final Collection<MasterBoardTerrain> terrains;
    private final List<AcquirableData> acquirableList;
    private final MasterBoard masterBoard;
    private final Document readme;
    private final String variantName;
    private final int titanImprove;
    private final int titanTeleport;

    /**
     * A map for fast lookup of creatures by their name.
     *
     * This is a cache to find creatures by their case-insensitive name quickly.
     */
    private final Map<String, CreatureType> creatureTypeByNameCache = new HashMap<String, CreatureType>();

    public Variant(IVariantInitializer variantInitializer,
        AllCreatureType creatureTypes, MasterBoard masterBoard,
        Document readme, String name)
    {
        this.creatureTypes = creatureTypes;
        // defensive copies to ensure immutability
        this.acquirableList = variantInitializer.getAcquirablesList();
        this.titanTeleport = variantInitializer.getTitanTeleportValue();
        this.titanImprove = variantInitializer.getTitanImprovementValue();

        // create some caches for faster lookups -- by name and by the "summonable" attribute
        initCreatureNameCache();
        this.summonableCreatureTypes = new ArrayList<CreatureType>();
        CollectionHelper.copySelective(this.creatureTypes.getCreatureTypes(),
            this.summonableCreatureTypes, new Predicate<CreatureType>()
            {
                public boolean matches(CreatureType creatureType)
                {
                    return creatureType.isSummonable();
                }
            });

        this.terrains = variantInitializer.getTerrains();
        this.masterBoard = masterBoard;
        this.readme = readme;
        this.variantName = name;
    }

    public List<CreatureType> getCreatureTypesAsList()
    {
        return this.creatureTypes.getCreatureTypesAsList();
    }

    public SortedSet<CreatureType> getCreatureTypes()
    {
        return this.creatureTypes.getCreatureTypes();
    }

    public Collection<MasterBoardTerrain> getTerrains()
    {
        return this.terrains;
    }

    /**
     * Retrieves the terrain with the given identifier.
     *
     * @param id The identifier for the terrain. Must be a valid for this variant.
     * @return The matching terrain.
     * @throws IllegalArgumentException iff the identifier does not refer to an
     *          existing terrain in this variant.
     */
    public MasterBoardTerrain getTerrainById(String id)
    {
        for (MasterBoardTerrain terrain : this.terrains)
        {
            if (terrain.getId().equals(id))
            {
                return terrain;
            }

        }
        throw new IllegalArgumentException("No terrain with the ID " + id);
    }

    public MasterBoard getMasterBoard()
    {
        return masterBoard;
    }

    public Document getReadme()
    {
        return readme;
    }

    public String getName()
    {
        return variantName;
    }

    /**
     * Look up a creature type by its name.
     *
     * The lookup is case-insensitive at the moment (TODO: check if that makes
     * sense at all).
     *
     * TODO in the long run noone should really need this since the names shouldn't
     * be passed around by themselves
     * ... except for resolving name that came over network... (Clemens)
     *
     * @param name Name of a creature type. Not null.
     * @return CreatureType with the given name, null no such creature type.
     */
    public CreatureType getCreatureByName(final String name)
    {
        assert name != null;
        if (name.equals("null"))
        {
            LOGGER.warning("Attempt to resolve String 'null' to CreatureType");
            Thread.dumpStack();
        }
        if (name.equals("Anything"))
        {
            // TODO they should be properly handled
            LOGGER.info("Attempt to resolve 'Anything' to CreatureType");

            // Thread.dumpStack();
            /*
            WARNING: Could not find creature with name Anything
            java.lang.Exception: Stack trace
                    at java.lang.Thread.dumpStack(Thread.java:1158)
                    at net.sf.colossus.variant.Variant.getCreatureByName(Variant.java:163)
                    at net.sf.colossus.game.RecruitGraph.numberOfRecruiterNeeded(RecruitGraph.java:412)
                    at net.sf.colossus.xmlparser.TerrainRecruitLoader.anonymousRecruitLegal(TerrainRecruitLoader.java:977)
                    at net.sf.colossus.server.GameServerSide.anonymousRecruitLegal(GameServerSide.java:1991)
                    at net.sf.colossus.server.GameServerSide.doRecruit(GameServerSide.java:2010)
                    at net.sf.colossus.server.Server.doRecruit(Server.java:1613)
                    at net.sf.colossus.server.ClientHandler.callMethod(ClientHandler.java:384)
                    at net.sf.colossus.server.ClientHandler.doCallMethodInTryBlock(ClientHandler.java:267)
                    at net.sf.colossus.server.ClientHandler.processInput(ClientHandler.java:155)
                    at net.sf.colossus.server.Server.waitOnSelector(Server.java:427)
                    at net.sf.colossus.server.Server.run(Server.java:179)
            */
            return null;
        }

        String lowerCaseName = name.toLowerCase();
        // XXX Why do I need to special-case angels but not titans?
        if (lowerCaseName.startsWith("angel"))
        {
            lowerCaseName = "angel";
        }
        CreatureType result = creatureTypeByNameCache.get(lowerCaseName);
        if (result == null)
        {
            // TODO find every case where this happens and get rid of it,
            // we should be able to assert a result
            LOGGER.warning("Could not find creature with name " + name);
        }
        return result;
    }

    private void initCreatureNameCache()
    {
        // find it the slow way and add to cache.
        for (CreatureType creatureType : this.creatureTypes.getCreatureTypes())
        {
            creatureTypeByNameCache.put(creatureType.getName().toLowerCase(),
                creatureType);
        }

        // "null" (not a null pointer...) is used for recruiter
        // when it is anonymous, so it is known and legal,
        // mapped to null (a null pointer, this time).
        // TODO avoid using nulls altogether
        creatureTypeByNameCache.put("null", null);
    }

    /**
     * Checks if a creature with the given name exists.
     *
     * @param name (case insensitive) name of a creature, must not be null.
     * @return true if this names represents a creature
     */
    public boolean isCreature(final String name)
    {
        return creatureTypeByNameCache.containsKey(name.toLowerCase());
    }

    public List<CreatureType> getSummonableCreatureTypes()
    {
        return summonableCreatureTypes;
    }

    /**
     * Used internally to record the Acquirable name, points needed for
     * recruiting, and the list of terrains in which the Acquirable dwells.
     * @author Romain Dolbeau
     */
    public static class AcquirableData
    {
        // TODO should be CreatureType
        private final String name;
        private final int value;
        private final List<MasterBoardTerrain> where;

        public AcquirableData(String n, int v,
            List<MasterBoardTerrain> terrains)
        {
            name = n;
            value = v;
            where = terrains;
        }

        String getName()
        {
            return name;
        }

        int getValue()
        {
            return value;
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

    /**
     * To obtain all the Creature that can be Acquired.
     * @return The list of name (as String) that can be Acquired
     */
    public List<String> getAcquirableList()
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
    public int getAcquirableRecruitmentsValue()
    {
        AcquirableData ad = acquirableList.get(0);
        return ad.getValue();
    }

    /**
     * To obtain the first Acquirable (aka 'primary') Creature name.
     * This one is the starting Lord with the Titan.
     * @return The name of the primary Acquirable Creature.
     */
    public String getPrimaryAcquirable()
    {
        AcquirableData ad = acquirableList.get(0);
        return ad.getName();
    }

    /**
     * To obtain all the Creature that can be acquired at the given amount of
     * points in the given terrain.
     *
     * TODO should return List<CreatureType>
     *
     * @param t The Terrain in which the recruitment occurs.
     * @param value The number of points at which the recruitment occurs.
     * Valid values are constrained.
     * @return The list of name (as String) that can be acquired in this
     * terrain, for this amount of points.
     * @see #getAcquirableRecruitmentsValue()
     */
    public List<String> getRecruitableAcquirableList(MasterBoardTerrain t,
        int value)
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
    private boolean isAcquirable(String name)
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
    public boolean isAcquirable(CreatureType c)
    {
        return isAcquirable(c.getName());
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

    public int[] getReinforcementTurns()
    {
        int[] reinforcementTurns = { 4 };
        return reinforcementTurns;
    }

    public int getMaxBattleTurns()
    {
        return 7;
    }
}
