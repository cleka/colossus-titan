package net.sf.colossus.variant;


import java.util.Set;


/**
 * Interface for recruiting. All recruiting should go through one of those.
 * Eventually.
 * @author Romain Dolbeau
 */
public interface IRecruiting
{
    /** Return the number of recruiter needed to obtain a recruit in hex
     * @param recruiter The Recruiter
     * @param recruit The Recruit
     * @param hex The hexagon in which the recruiting occurs
     * @return The number of recruiter needed to obtain a recruit in hex
     */
    public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterHex hex);

    /** Return all the CreatureType that can be (somehow) recruited in the hex.
     * @param hex The hexagon to consider
     * @return All CreatureType that can be recruited in hex
     */
    public Set<CreatureType> getPossibleRecruits(MasterHex hex);

    /** Return all the CreatureType that can be recruits (something) in the hex.
     * @param hex The hexagon to consider
     * @return All CreatureType that can recruit in hex
     */
    public Set<CreatureType> getPossibleRecruiters(MasterHex hex);

    public int maximumNumberNeededOf(CreatureType ct, MasterHex hex);
}
