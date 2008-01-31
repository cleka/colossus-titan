package net.sf.colossus.variant;


import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;


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
    private final String id;
    private final String displayName;
    private final Color color;
    // TODO this should be a List<BattleHex> (and BattleHex should be part of the variant package)
    private List<String> towerStartList;
    private boolean isTower;
    private HashMap<HazardTerrain, Integer> hazardNumberMap;

    public MasterBoardTerrain(String id, String displayName, Color color)
    {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    // TODO this is still used in many places where the object reference should be used
    public String getId()
    {
        return id;
    }

    public String getDisplayName()
    {
        return displayName;
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
            int count = HexMap
                .getHazardSideCountInTerrain(hazardSide[i], this);
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

    public void setTowerStartList(List<String> towerStartList)
    {
        this.towerStartList = towerStartList;
    }

    public List<String> getTowerStartList()
    {
        return Collections.unmodifiableList(towerStartList);
    }

    public void setTower(boolean isTower)
    {
        this.isTower = isTower;
    }

    public boolean isTower()
    {
        return isTower;
    }

    public boolean hasTowerStartList()
    {
        return towerStartList != null;
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
}
