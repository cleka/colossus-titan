package net.sf.colossus.variant;

import java.util.List;

/**
 *
 * @author ROmain Dolbeau
 */
public interface AllCreatureType {
    public List<CreatureType> getCreatures();
    public CreatureType getCreatureByName(String name);
}
