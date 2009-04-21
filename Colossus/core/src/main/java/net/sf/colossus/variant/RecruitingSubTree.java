package net.sf.colossus.variant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.sf.colossus.common.Constants;

/**
 * The recruiting sub-tree in a terrain (or several terrains)
 * @author Romain Dolbeau
 */
public class RecruitingSubTree implements IRecruiting
{
    private static final Logger LOGGER = Logger
        .getLogger(RecruitingSubTree.class.getName());

    private class RecruiterAndRecruit
    {
        final private CreatureType recruiter;
        final private CreatureType recruit;

        RecruiterAndRecruit(CreatureType recruiter, CreatureType recruit)
        {
            this.recruiter = recruiter;
            this.recruit = recruit;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof RecruiterAndRecruit))
            {
                return false;
            }
            RecruiterAndRecruit rar = (RecruiterAndRecruit)o;
            return this.getRecruiter().equals(rar.getRecruiter()) &&
                    this.getRecruit().equals(rar.getRecruit());
        }

        @Override
        public int hashCode()
        {
            int hash = 3;
            hash =
                    31 * hash +
                    (this.getRecruiter() != null ? this.getRecruiter().hashCode() : 0);
            hash =
                    31 * hash +
                    (this.getRecruit() != null ? this.getRecruit().hashCode() : 0);
            return hash;
        }

        @Override
        public String toString()
        {
            return getRecruiter().getName() + " recruits " + getRecruit().getName();
        }

        /**
         * @return the recruiter
         */
        public CreatureType getRecruiter()
        {
            return recruiter;
        }

        /**
         * @return the recruit
         */
        public CreatureType getRecruit()
        {
            return recruit;
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
    private final Set<ICustomRecruitBase> allCustom = new HashSet<ICustomRecruitBase>();

    private boolean completed = false;

    public RecruitingSubTree() {
        
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (RecruiterAndRecruit rar : regular.keySet())
        {
            buf.append(rar.toString());
            buf.append(" gives ");
            buf.append(regular.get(rar));
            buf.append("\n");
        }
        for (CreatureType ct : any.keySet()) {
            buf.append("Any ");
            buf.append(any.get(ct));
            buf.append(" recruits ");
            buf.append(ct.getName());
            buf.append("\n");
        }
        for (CreatureType ct : anyNonLord.keySet()) {
            buf.append("Any ");
            buf.append(anyNonLord.get(ct));
            buf.append(" non lord recruits ");
            buf.append(ct.getName());
            buf.append("\n");
        }
        for (CreatureType ct : anyLord.keySet()) {
            buf.append("Any ");
            buf.append(anyLord.get(ct));
            buf.append(" lord recruits ");
            buf.append(ct.getName());
            buf.append("\n");
        }
        for (CreatureType ct : anyDemiLord.keySet()) {
            buf.append("Any ");
            buf.append(anyDemiLord.get(ct));
            buf.append(" demilord recruits ");
            buf.append(ct.getName());
            buf.append("\n");
        }
        for (ICustomRecruitBase crb : allCustom) {
            buf.append("Custom by class " + crb.getClass().getName());
            buf.append("\n");
        }
        return buf.toString();
    }

    private boolean isRegularAncestorOf(final CreatureType a, final CreatureType b, final Set<CreatureType> checked) {
        if (regular.containsKey(new RecruiterAndRecruit(a, b))) {
            return true;
        }
        for (RecruiterAndRecruit rar : regular.keySet())
        {
            final CreatureType c = rar.getRecruiter();
            final CreatureType d = rar.getRecruit();
            if (c.equals(a) && !checked.contains(d))
            {
                Set<CreatureType> checked2 = new HashSet<CreatureType>(checked);
                checked2.add(a);
                if (isRegularAncestorOf(d,b,checked2))
                    return true;
            }
        }
        return false;
    }

    private void completeGraph()
    {
        Set<CreatureType> allInvolved = new HashSet<CreatureType>();
        /* first, extract all creature really used (recruiter or recruit) in this terrain */
        for (RecruiterAndRecruit rar : regular.keySet())
        {
            allInvolved.add(rar.getRecruiter());
            allInvolved.add(rar.getRecruit());
        }
        Set<RecruiterAndRecruit> extra = new HashSet<RecruiterAndRecruit>();
        /* then for all combinations, check which are ancestor to which other
         * in the tree. This information is stored in an intermediate set,
         * otherwise we would add artificial ancestors
         */
        for (CreatureType a : allInvolved)
        {
            for (CreatureType b : allInvolved)
            {
                RecruiterAndRecruit rar = new RecruiterAndRecruit(a,b);
                if (!regular.containsKey(rar))
                {
                    Set<CreatureType> checked = new HashSet<CreatureType>();
                    checked.add(b);
                    if (isRegularAncestorOf(b, a, checked)) {
                        LOGGER.finest("Completing: adding " + a.getName() + " to " + b.getName());
                        extra.add(rar);
                    }
                }
            }
        }
        /* finally, a creature can recruit all its ancestor with 1 */
        for (RecruiterAndRecruit rar : extra) {
            regular.put(rar, new Integer(1));
        }
    }

    public void complete(boolean regularRecruit) {
        completed = true;
        if (regularRecruit)
        {
            completeGraph();
        }
    }

    public void addRegular(CreatureType recruiter, CreatureType recruit,
            int number)
    {
        assert recruit != null : "Oups, recruit must not be null";
        assert recruiter != null : "Oups, recruiter must not be null";
        assert number > 0 : "Oups, number should be > 0";
        // regular version
        regular.put(new RecruiterAndRecruit(recruiter, recruit),
                    new Integer(number));
    }

    @SuppressWarnings("boxing")
    public void addAny(CreatureType recruit, int number)
    {
        assert completed == false : "Oups, can't add after being completed";
        assert recruit != null : "Oups, recruit must not be null";
        assert number > 0 : "Oups, number should be > 0";
        any.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addNonLord(CreatureType recruit, int number)
    {
        assert completed == false : "Oups, can't add after being completed";
        assert recruit != null : "Oups, recruit must not be null";
        assert number > 0 : "Oups, number should be > 0";
        anyNonLord.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addLord(CreatureType recruit, int number)
    {
        assert completed == false : "Oups, can't add after being completed";
        assert recruit != null : "Oups, recruit must not be null";
        assert number > 0 : "Oups, number should be > 0";
        anyLord.put(recruit, number);
    }

    @SuppressWarnings("boxing")
    public void addDemiLord(CreatureType recruit, int number)
    {
        assert completed == false : "Oups, can't add after being completed";
        assert recruit != null : "Oups, recruit must not be null";
        assert number > 0 : "Oups, number should be > 0";
        anyDemiLord.put(recruit, number);
    }

    public void addCustom(ICustomRecruitBase crb)
    {
        assert completed == false : "Oups, can't add after being completed";
        assert crb != null : "Oups, ICustomRecruitBase must not be null";
        allCustom.add(crb);
    }

    public int numberOfRecruiterNeeded(CreatureType recruiter,
            CreatureType recruit, MasterBoardTerrain terrain, MasterHex hex)
    {
        int number = Constants.BIGNUM;
        LOGGER.finest("Start for recruiter and recruit : " + recruiter.getName() + " & " + recruit.getName());
        if (recruiter.equals(recruit))
        {
            LOGGER.finest("Recruiter and recruit are identical = 1 " + recruiter.getName() + " & " + recruit.getName());
            number = 1;
        }
        RecruiterAndRecruit rar = new RecruiterAndRecruit(recruiter, recruit);
        if (regular.keySet().contains(rar))
        {
            LOGGER.finest("Recruiter and recruit are regular = " + regular.get(rar) + " " + recruiter.getName() + " & " + recruit.getName());
            number = Math.min(number, regular.get(rar).intValue());
        }
        if (any.keySet().contains(recruit))
        {
            LOGGER.finest("Recruit in any = " + regular.get(rar) + " " + recruit.getName());
            number = Math.min(number, any.get(recruit).intValue());
        }
        if (!recruiter.isLord() && !recruiter.isDemiLord())
        {
            if (anyNonLord.keySet().contains(recruit))
            {
                LOGGER.finest("Recruit in anyNonLord = " + regular.get(rar) + " " + recruit.getName());
                number = Math.min(number, anyNonLord.get(recruit).intValue());
            }
        }
        if (recruiter.isLord())
        {
            if (anyLord.keySet().contains(recruit))
            {
                LOGGER.finest("Recruit in anyLord = " + regular.get(rar) + " " + recruit.getName());
                number = Math.min(number, anyLord.get(recruit).intValue());
            }
        }
        if (recruiter.isDemiLord())
        {
            if (anyDemiLord.keySet().contains(recruit))
            {
                LOGGER.finest("Recruit in anyDemiLord = " + regular.get(rar) + " " + recruit.getName());
                number = Math.min(number, anyDemiLord.get(recruit).intValue());
            }
        }
        for (ICustomRecruitBase crb : allCustom)
        {
            LOGGER.finest("Checking with CRB " + crb.getClass().getName());
            number = Math.min(number, crb.numberOfRecruiterNeeded(recruiter,
                    recruit, terrain,
                    hex));
        }
        return number;
    }

    public Set<CreatureType> getPossibleRecruits(MasterBoardTerrain terrain,
            MasterHex hex)
    {
        Set<CreatureType> possibleRecruits = new TreeSet<CreatureType>();

        for (RecruiterAndRecruit rar : regular.keySet())
        {
            CreatureType recruit = rar.getRecruit();
            if (!recruit.isTitan())
            {
                possibleRecruits.add(recruit);
            }
            else
            {
                LOGGER.warning("TITAN as regular recruit ????");
                LOGGER.warning(this.toString());
            }
            CreatureType recruiter = rar.getRecruiter();
            if (!recruiter.isTitan())
            {
                possibleRecruits.add(recruiter);
            }
        }
        for (CreatureType ct : any.keySet()) {
            possibleRecruits.add(ct);
        }
        for (CreatureType ct : anyNonLord.keySet()) {
            possibleRecruits.add(ct);
        }
        for (CreatureType ct : anyLord.keySet()) {
            possibleRecruits.add(ct);
        }
        for (CreatureType ct : anyDemiLord.keySet()) {
            possibleRecruits.add(ct);
        }
        for (ICustomRecruitBase cri : allCustom)
        {
            List<CreatureType> temp = cri.getPossibleSpecialRecruits(terrain, hex);
            possibleRecruits.addAll(temp);
        }
        return possibleRecruits;
    }
}
