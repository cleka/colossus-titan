package net.sf.colossus.variant;


import java.util.List;


public interface ICustomRecruitBase
{

    /**
     * List all creatures that can recruit in this terrain in a special way.
     */
    abstract public List<CreatureType> getAllPossibleSpecialRecruiters(
        MasterBoardTerrain terrain);

    /**
     * List all creatures that can be recruited in this terrain
     * in a special way.
     */
    abstract public List<CreatureType> getAllPossibleSpecialRecruits(
        MasterBoardTerrain terrain);

    /**
     * List creatures that can recruit in this terrain in a special way now.
     * @param terrain The MasterBoardTerrain considered for recruiting
     * @param hex The specific MasterHex considered for recruiting.
     * @return A List of possible special Recruiters in this hex.
     */
    abstract public List<CreatureType> getPossibleSpecialRecruiters(
        MasterBoardTerrain terrain, MasterHex hex);

    /**
     * List creatures that can be recruited in this terrain
     * in a special way now.
     * @param terrain The MasterBoardTerrain considered for recruiting
     * @param hex The specific MasterHex considered for recruiting
     * (for an example, see getPossibleSpecialRecruits() in
     * BalrogRecruitment.java in Balrog variant directory)
     * @return A List of possible special Recruits in this hex.
     */
    abstract public List<CreatureType> getPossibleSpecialRecruits(
        MasterBoardTerrain terrain, MasterHex hex);

    /**
     * Number of recruiters needed to get a recruit
     * in a special way in this terrain now.
     */
    abstract public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex);

}