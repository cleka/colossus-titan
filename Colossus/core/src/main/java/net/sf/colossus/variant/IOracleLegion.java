package net.sf.colossus.variant;


/**
 *  A legion, given as parameter to VariantHintOracle
 *
 *  @author Clemens Katzer
 */

public interface IOracleLegion
{
    boolean contains(String string);

    boolean contains(CreatureType creatureType);

    int numCreature(String string);

    int numCreature(CreatureType creatureType);

    int getHeight();
}
