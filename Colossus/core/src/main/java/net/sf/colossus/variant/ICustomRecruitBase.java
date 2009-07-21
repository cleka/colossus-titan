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
     * @param hex The specific MasterHex considered for recruiting.
     * @return A List of possible special Recruiters in this hex.
     */
    public List<CreatureType> getPossibleSpecialRecruiters(MasterHex hex);

    /**
     * List creatures that can be recruited in this terrain
     * in a special way now.
     * @param hex The specific MasterHex considered for recruiting
     * (for an example, see getPossibleSpecialRecruits() in
     * BalrogRecruitment.java in Balrog variant directory)
     * @return A List of possible special Recruits in this hex.
     */
    public List<CreatureType> getPossibleSpecialRecruits(MasterHex hex);

    /**
     * Number of recruiters needed to get a recruit
     * in a special way in this terrain now.
     */
    public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterHex hex);

}