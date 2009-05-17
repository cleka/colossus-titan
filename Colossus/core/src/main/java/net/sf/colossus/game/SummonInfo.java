package net.sf.colossus.game;


import net.sf.colossus.variant.CreatureType;


/**
 * Little helper class to store information about a summoning:
 * The target legion, the donor legion, and the summoned
 * creature(type) (creature as String, for now).
 *
 * TODO there is overlap with the SummonEvent here, although the
 * semantics are different. Maybe a constructor for SummonEvent could
 * use this class.
 */
public class SummonInfo
{
    final private Legion target;
    final private Legion donor;
    final private CreatureType unit;
    final private boolean noSummoningWanted;

    public SummonInfo(Legion target, Legion donor, CreatureType unit)
    {
        this.target = target;
        this.donor = donor;
        this.unit = unit;
        this.noSummoningWanted = false;
    }

    public SummonInfo()
    {
        this.target = null;
        this.donor = null;
        this.unit = null;
        this.noSummoningWanted = true;
    }

    public Legion getTarget()
    {
        return target;
    }

    public Legion getDonor()
    {
        return donor;
    }

    public CreatureType getUnit()
    {
        return unit;
    }

    public boolean noSummoningWanted()
    {
        return noSummoningWanted;
    }

    @Override
    public String toString()
    {
        if (noSummoningWanted)
        {
            return "SummonInfo: no Summoning Wanted.";
        }
        else
        {
            return "SummonInfo: Summon " + unit + " from " + donor + " into "
                + target + ".";
        }
    }
}
