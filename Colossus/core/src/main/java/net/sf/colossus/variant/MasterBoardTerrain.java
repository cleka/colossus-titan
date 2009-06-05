package net.sf.colossus.variant;


import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;


/**
 * A master board terrain.
 *
 * This class describes a terrain on the master board, including its name, color and the
 * layout of a generic battle land. It can occur multiple times on a master board layout
 * attached to the {@link MasterHex} class.
 *
 * Battle land information could probably split out into another class, which could then
 * be immutable.
 */
public class MasterBoardTerrain implements Comparable<MasterBoardTerrain>
{
    private static final Logger LOGGER = Logger
        .getLogger(MasterBoardTerrain.class.getName());

    /** The (unique) identifier of this terrain.
     * Should also be used for all Battlelands purpose.
     */
    private final String id;

    /** The name displayed on the Masterboard.
     * Should also be used for all recruiting purpose.
     * WARNING: this is not done that way yet. It shoud be, so that a single
     * name on the Masterboard will represent a single recruiting branch,
     * even if it' backed by several different Battlelands. This would also
     * remove a lot of duplicated entries in the Full Recruit Tree.
     * WIP.
     * ADDITIONAL WARNING: What about variant such as Balrog? The recruitment
     * is Hex-specific, not Terrain-specific...
     */
    private final String displayName;
    /** Subtitle, for the Battlelands. Cosmetic only, but nice */
    private String subtitle;
    private final Color color;
    /** TODO this should be a List<BattleHex> ... or a List<GUIBattleHex> ???
     * If non-null, this is the list of hexes a defending legion will start
     * in, in a similar way to the Tower in the Default variant.
     */
    private List<String> startList;
    /** Whether this is a Tower-like building, with regards to starting the
     * game, not recruiting or defender entering in a non-default location on
     * the Battlemap.
     */
    private boolean isTower;
    private Map<HazardTerrain, Integer> hazardNumberMap;
    // TODO this should be a Map<HazardHexside, Integer>
    private Map<Character, Integer> hazardSideNumberMap;

    // TODO right now we set up both, until all queries can use the new form
    private Map<HazardHexside, Integer> hexsideHazardNumberMap;

    /** The other MasterBoardTerrain using the same recruit tree */
    private final Set<MasterBoardTerrain> aliases = new TreeSet<MasterBoardTerrain>();
    /** Whether this terrain uses another Terrain recruit tree. */
    private final boolean isAlias;

    /** The recruiting tree of this terrain */
    IRecruiting recruitingSubTree;

    public MasterBoardTerrain(String id, String displayName, Color color,
        boolean isAlias)
    {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.subtitle = null;
        this.isAlias = isAlias;
    }

    public void setRecruitingSubTree(IRecruiting rst)
    {
        this.recruitingSubTree = rst;
    }

    public IRecruiting getRecruitingSubTree() {
        return recruitingSubTree;
    }

    public MasterBoardTerrain(String id, String displayName, Color color)
    {
        this(id, displayName, color, false);
    }

    public int compareTo(MasterBoardTerrain m)
    {
        return this.id.compareTo(m.id);
    }

    public void addAlias(MasterBoardTerrain t)
    {
        aliases.add(t);
    }

    public boolean isAlias()
    {
        return isAlias;
    }

    public Set<MasterBoardTerrain> getAliases()
    {
        return Collections.unmodifiableSet(aliases);
    }

    public String getId()
    {
        return id;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public void setSubtitle(String s)
    {
        subtitle = s;
    }

    public Color getColor()
    {
        return color;
    }

    // TODO get rid of dependencies into client package
    public boolean hasNativeCombatBonus(CreatureType creature)
    {
        int bonusHazardCount = 0;
        int bonusHazardSideCount = 0;

        for (HazardTerrain hTerrain : HazardTerrain.getAllHazardTerrains())
        {
            int count = this.getHazardCount(hTerrain);
            if (hTerrain.isNativeBonusTerrain()
                && creature.isNativeIn(hTerrain))
            {
                bonusHazardCount += count;
            }
            else
            {
                if (hTerrain.isNonNativePenaltyTerrain()
                    && !creature.isNativeIn(hTerrain))
                {
                    bonusHazardCount -= count;
                }
            }
        }

        final Collection<HazardHexside> hazardTypes = HazardHexside
            .getAllHazardHexsides();

        for (HazardHexside hazard : hazardTypes)
        {
            int count = this.getHazardHexsideCount(hazard);

            if (hazard.isNativeBonusHexside()
                && (creature).isNativeHexsideHazard(hazard))
            {
                bonusHazardSideCount += count;
            }
            else
            {
                if (hazard.isNonNativePenaltyHexside()
                    && !(creature).isNativeHexsideHazard(hazard))
                {
                    bonusHazardSideCount -= count;
                }
            }
        }
        if (((bonusHazardCount + bonusHazardSideCount) > 0)
            && ((bonusHazardCount >= 3) || (bonusHazardSideCount >= 5)))
        {
            return true;
        }
        return false;
    }

    public void setStartList(List<String> startList)
    {
        this.startList = startList;
    }

    public List<String> getStartList()
    {
        if (startList == null)
        {
            return null;
        }
        return Collections.unmodifiableList(startList);
    }

    public void setTower(boolean isTower)
    {
        this.isTower = isTower;
    }

    public boolean isTower()
    {
        return isTower;
    }

    public boolean hasStartList()
    {
        return startList != null;
    }

    public void setHazardNumberMap(
        Map<HazardTerrain, Integer> hazardNumberMap)
    {
        this.hazardNumberMap = hazardNumberMap;
    }

    public int getHazardCount(HazardTerrain terrain)
    {
        return hazardNumberMap.get(terrain).intValue();
    }

    public void setHazardSideNumberMap(
        Map<Character, Integer> hazardSideNumberMap)
    {
        this.hazardSideNumberMap = hazardSideNumberMap;
    }

    public int getHazardSideCount(char hazardSide)
    {
        return hazardSideNumberMap.get(Character.valueOf(hazardSide))
            .intValue();
    }

    public void setHexsideHazardNumberMap(
        Map<HazardHexside, Integer> hexsideHazardNumberMap)
    {
        this.hexsideHazardNumberMap = hexsideHazardNumberMap;
    }

    // TODO Keep the old style as paranoid counterCheck and compare results.
    //      If this does now show up problems, the old way can be eliminated
    //      (refactored 2009-04-06 by Clemens)
    public int getHazardHexsideCount(HazardHexside hazard)
    {
        int oldCount = getHazardSideCount(hazard.getCode());
        int newCount = hexsideHazardNumberMap.get(hazard).intValue();
        if (oldCount != newCount)
        {
            LOGGER.warning("Refactored getCount for hexside hazard types "
                + "returns different value (" + newCount + ") than old "
                + "one does (" + oldCount + ")");
        }
        return newCount;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MasterBoardTerrain other = (MasterBoardTerrain)obj;
        if (id == null)
        {
            if (other.id != null)
                return false;
        }
        return id.equals(other.id);
    }
}
