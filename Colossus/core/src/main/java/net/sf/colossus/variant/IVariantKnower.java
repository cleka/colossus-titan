package net.sf.colossus.variant;


/**
 * Some object from which Game can ask for the current variant,
 * instead of the static VariantSupport.getCurrentVariant() way.
 *
 * All other places should ask it from Game, they should not need
 * to get this one here...
 *
 * TODO This is meant as TEMPORARY SOLUTION.
 *
 * On the long run we should try to get rid of this and variant or
 * variant name passed in to game constructor, but we are not there
 * yet...
 *
 * @author Clemens Katzer
 */
public interface IVariantKnower
{

    public Variant getTheCurrentVariant();
}
