package net.sf.colossus.variant;

import java.util.List;

/**
 * All CreatureType existing in a Variant
 * @author Romain Dolbeau
 */
public interface AllCreatureType {
    /**
     * Retrieve all the CreatureType in the game. They are sorted by name.
     * @return The immutable list of all CreatureType in the Variant.
     */
    public List<CreatureType> getCreatures();
    /**
     * Convert a name into the actual CreatureType
     * @param name The name of the CreatureType
     * @return The CreatureType of name name.
     */
    public CreatureType getCreatureByName(String name);
}
