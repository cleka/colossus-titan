package net.sf.colossus.variant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sf.colossus.common.Constants;
import net.sf.colossus.server.CustomRecruitBase;

/**
 * The recruiting sub-tree in a terrain (or several terrains)
 * @author Romain Dolbeau
 */
public class RecruitingSubTree
{

    private class RecruiterAndRecruit
    {

        final private CreatureType recruiter;
        final private CreatureType recruit;

        RecruiterAndRecruit(CreatureType recruiter, CreatureType recruit)
        {
            this.recruiter = recruiter;
            this.recruit = recruit;
        }

        public boolean equals(RecruiterAndRecruit rar)
        {
            return this.recruiter.equals(rar.recruiter) &&
                    this.recruit.equals(rar.recruit);
        }
    }
    private final Map<RecruiterAndRecruit, Integer> regular =
            new HashMap<RecruiterAndRecruit, Integer>();
    private final Map<CreatureType, Integer> any =
            new HashMap<CreatureType, Integer>();
    private final Map<CreatureType, Integer> anyNonLord =
            new HashMap<CreatureType, Integer>();
    private final Map<CreatureType, Integer> anyLord =
            new HashMap<CreatureType, Integer>();
    private final Map<CreatureType, Integer> anyDemiLord =
            new HashMap<CreatureType, Integer>();
    private final Set<CustomRecruitBase> allCustom = new HashSet<CustomRecruitBase>();

    public void addRegular(CreatureType recruiter, CreatureType recruit,
            int number)
    {
        regular.put(new RecruiterAndRecruit(recruiter, recruit), new Integer(
                number));
    }

    @SuppressWarnings("boxing")
    public void addAny(CreatureType recruit, int number)
    {
        any.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addNonLord(CreatureType recruit, int number)
    {
        anyNonLord.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addLord(CreatureType recruit, int number)
    {
        anyLord.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addDemiLord(CreatureType recruit, int number)
    {
        anyDemiLord.put(recruit, number);
    }

    public void addCustom(CustomRecruitBase crb)
    {
        allCustom.add(crb);
    }

    public int numberOfRecruiterNeeded(CreatureType recruiter,
            CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex)
    {
        int number = Constants.BIGNUM;
        RecruiterAndRecruit rar = new RecruiterAndRecruit(recruiter, recruit);
        if (regular.keySet().contains(rar))
        {
            number = Math.min(number, regular.get(rar).intValue());
        }
        if (any.keySet().contains(recruit))
        {
            number = Math.min(number, any.get(recruit).intValue());
        }
        if (!recruiter.isLord() && !recruiter.isDemiLord())
        {
            if (anyNonLord.keySet().contains(recruit))
            {
                number = Math.min(number, anyNonLord.get(recruit).intValue());
            }
        }
        if (recruiter.isLord())
        {
            if (anyLord.keySet().contains(recruit))
            {
                number = Math.min(number, anyLord.get(recruit).intValue());
            }
        }
        if (recruiter.isDemiLord())
        {
            if (anyDemiLord.keySet().contains(recruit))
            {
                number = Math.min(number, anyDemiLord.get(recruit).intValue());
            }
        }
        for (CustomRecruitBase crb : allCustom)
        {
            number = Math.min(number, crb.numberOfRecruiterNeeded(recruiter,
                    recruit, terrain,
                    hex));
        }
        return number;
    }
}
