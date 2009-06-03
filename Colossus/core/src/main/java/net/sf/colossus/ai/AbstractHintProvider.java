package net.sf.colossus.ai;


import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariantHint;
import net.sf.colossus.variant.Variant;


/**
 * Abstract base class for variant-specific recruitment hinting.
 *
 * TODO: add implementations for the other IVariantHint methods, so this class
 * could be the default behaviour for new variants (avoiding each variant to
 * compile Java code).
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

    public Variant getVariant()
    {
        return variant;
    }
}
