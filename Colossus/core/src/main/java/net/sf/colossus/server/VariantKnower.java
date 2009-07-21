package net.sf.colossus.server;


import net.sf.colossus.variant.IVariantKnower;
import net.sf.colossus.variant.Variant;


public class VariantKnower implements IVariantKnower
{
    public Variant getTheCurrentVariant()
    {
        return VariantSupport.getCurrentVariant();
    }

}
