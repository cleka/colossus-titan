package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class SummonEvent extends AddCreatureEvent
{
    private final Legion donor;

    public SummonEvent(Legion targetLegion, Legion donor,
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
        return String
            .format(
                "SummonEvent: summon creature of type %s from legion %s into legion %s",
                getAddedCreatureType(), getLegion(), getDonor());
    }
}
