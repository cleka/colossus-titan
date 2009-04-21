package net.sf.colossus.variant;

import java.util.Set;

/**
 *
 * @author Romain Dolbeau
 */
public interface IRecruiting {

    public int numberOfRecruiterNeeded(CreatureType recruiter, CreatureType recruit,
            MasterBoardTerrain terrain, MasterHex hex);
    public Set<CreatureType> getPossibleRecruits(
        MasterBoardTerrain terrain, MasterHex hex);
}
