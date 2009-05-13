package net.sf.colossus.game.events;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class SummonEvent extends AddCreatureEvent
{
    private final Legion donor;

    public SummonEvent(int turn, Legion targetLegion, Legion donor,
        CreatureType summonedCreature)
    {
        super(turn, (targetLegion == null) ? null : targetLegion.getPlayer(),
            targetLegion, summonedCreature);
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

    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {
        return String
            .format(
                "In turn %d, player %s has summoned creature of type %s from legion %s into legion %s",
                getTurn(), getPlayer(), getAddedCreatureType(), getLegion(),
                getDonor());
    }
}
