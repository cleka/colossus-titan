package net.sf.colossus.ai;


import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariantHint;


/**
 * Abstract base class for variant-specific recruitment hinting.
 *
 * TODO: add implementations for the other IVariantHint methods, so this class
 * could be the default behaviour for new variants (avoiding each variant to
 * compile Java code).
 */
public abstract class AbstractHintProvider implements IVariantHint
{
    /**
     * No creature gets an offset by default, subclasses can override.
     */
    public int getHintedRecruitmentValueOffset(CreatureType creature,
        List<AIStyle> styles)
    {
        return 0;
    }
}
