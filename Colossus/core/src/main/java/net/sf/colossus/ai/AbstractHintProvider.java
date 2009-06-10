package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariantHint;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Abstract base class for variant-specific recruitment hinting.
 *
 * TODO: add implementations for the other IVariantHint methods, so this class
 * could be the default behaviour for new variants (thus getting rid of the
 * requirement that each variant has to compile Java code).
 */
public abstract class AbstractHintProvider implements IVariantHint
{
    private final Variant variant;

    public AbstractHintProvider(Variant variant)
    {
        this.variant = variant;
    }

    /**
     * No creature gets an offset by default, subclasses can override.
     */
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> styles)
    {
        return 0;
    }

    protected Variant getVariant()
    {
        return variant;
    }

    protected CreatureType getCreatureType(String creatureName)
    {
        return variant.getCreatureByName(creatureName);
    }

    protected MasterHex getMasterHex(String hexLabel)
    {
        return variant.getMasterBoard().getHexByLabel(hexLabel);
    }

    protected MasterBoardTerrain getTerrain(String id)
    {
        return variant.getTerrainById(id);
    }

    // Convert list of recruits from Creature to String for easier compares.
    public static List<String> creaturesToStrings(List<CreatureType> creatures)
    {
        List<String> recruits = new ArrayList<String>();
        for (CreatureType creature : creatures)
        {
            recruits.add(creature.getName());
        }
        return recruits;
    }
}
