package net.sf.colossus.variant;


import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
public class MasterBoardTerrain
{
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
     */
    private final String displayName;
    /** Subtitle, for the Battlelands. Cosmetic only, but nice */
    private       String subtitle;
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

    public MasterBoardTerrain(String id, String displayName, Color color)
    {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.subtitle = null;
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
        final char[] hazardSide = BattleHex.getHexsides();

        for (int i = 0; i < hazardSide.length; i++)
        {
            int count = this.getHazardSideCount(hazardSide[i]);
            if (BattleHex.isNativeBonusHexside(hazardSide[i])
                && (creature).isNativeHexside(hazardSide[i]))
            {
                bonusHazardSideCount += count;
            }
            else
            {
                if (BattleHex.isNonNativePenaltyHexside(hazardSide[i])
                    && !(creature).isNativeHexside(hazardSide[i]))
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
        HashMap<HazardTerrain, Integer> hazardNumberMap)
    {
        this.hazardNumberMap = hazardNumberMap;
    }

    public int getHazardCount(HazardTerrain terrain)
    {
        return hazardNumberMap.get(terrain).intValue();
    }

    public void setHazardSideNumberMap(
        HashMap<Character, Integer> hazardSideNumberMap)
    {
        this.hazardSideNumberMap = hazardSideNumberMap;
    }

    public int getHazardSideCount(char hazardSide)
    {
        return hazardSideNumberMap.get(Character.valueOf(hazardSide))
            .intValue();
    }
}
