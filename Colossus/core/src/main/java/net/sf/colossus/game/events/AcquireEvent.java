package net.sf.colossus.game.events;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class AcquireEvent extends AddCreatureEvent
{
    public AcquireEvent(Legion legion, CreatureType creatureType)
    {
        super(legion, creatureType);
    }

    @Override
    public String getReason()
    {
        return Constants.reasonAcquire;
    }

    @Override
    public String toString()
    {
        return String.format(
            "AcquireEvent: acquire creature of type %s in legion %s",
            getAddedCreatureType(), getLegion());
    }
}
