package net.sf.colossus.variant;


import java.util.List;
import java.util.SortedSet;


/**
 * All CreatureType existing in a Variant
 * @author Romain Dolbeau
 */
public interface AllCreatureType
{
    /**
     * Retrieve all the CreatureType in the game. They are sorted by name.
     * @return The immutable list of all CreatureType in the Variant.
     */
    public List<CreatureType> getCreatureTypesAsList();

    /**
     * Retrieve all the CreatureType in the game. The set is sorted by the
     * natural order of CreatureType
     * @return The immutable SortedSet of all CreatureType in the Variant.
     */
    public SortedSet<CreatureType> getCreatureTypes();

    /**
     * Convert a name into the actual CreatureType
     * @param name The name of the CreatureType
     * @return The CreatureType of name name.
     */
    public CreatureType getCreatureTypeByName(String name);
}
