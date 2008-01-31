package net.sf.colossus.variant;


import java.awt.Color;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;


/**
 * A master board terrain.
 * 
 * This class describes a terrain on the master board, including its name, color and the 
 * layout of a generic battle land. It can occur multiple times on a master board layout
 * attached to the {@link MasterHex} class.
 */
public class MasterBoardTerrain
{
    private final String id;
    private final String displayName;
    private final Color color;

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
            int count = HexMap.getHazardCountInTerrain(hTerrain, this);
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
}
