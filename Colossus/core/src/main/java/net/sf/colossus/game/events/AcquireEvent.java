package net.sf.colossus.game.events;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class AcquireEvent extends AddCreatureEvent
{
    public AcquireEvent(int turn, Legion legion, CreatureType creatureType)
    {
        super(turn, legion.getPlayer(), legion, creatureType);
    }

    @Override
    public String getReason()
    {
        return Constants.reasonAcquire;
    }
    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {
        return String.format(
            "In turn %d, player %s acquired creature of type %s in legion %s",
            getTurn(), getPlayer().getName(),
            getAddedCreatureType().getName(), getLegion().getMarkerId());
    }
}
