package net.sf.colossus.variant;

import java.util.Set;

/**
 *
 * @author Romain Dolbeau
 */
public interface IRecruiting
{

    public int numberOfRecruiterNeeded(CreatureType recruiter,
            CreatureType recruit, MasterHex hex);

    public Set<CreatureType> getPossibleRecruits(MasterHex hex);

    public Set<CreatureType> getPossibleRecruiters(MasterHex hex);
}
