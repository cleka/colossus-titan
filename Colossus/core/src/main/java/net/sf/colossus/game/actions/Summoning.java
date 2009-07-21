package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class Summoning extends AddCreatureAction
{
    private final Legion donor;

    public Summoning(Legion targetLegion, Legion donor,
        CreatureType summonedCreature)
    {
        super(targetLegion, summonedCreature);
        this.donor = donor;
    }

    @Override
    public String getReason()
    {
        return Constants.reasonSummon;
    }

    public Legion getDonor()
    {
        return donor;
    }

    @Override
    public String toString()
    {
        return String.format(
            "Summoning of creature of type %s from legion %s into legion %s",
            getAddedCreatureType(), getLegion(), getDonor());
    }
}
