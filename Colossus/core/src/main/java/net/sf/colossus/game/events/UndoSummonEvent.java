package net.sf.colossus.game.events;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;

public class UndoSummonEvent extends AddCreatureEvent
{
    public UndoSummonEvent(int turn, Legion legion,
        CreatureType creatureType)
    {
        super(turn, legion.getPlayer(), legion, creatureType);
    }

    @Override
    public String getReason()
    {
        return Constants.reasonUndoSummon;
    }

    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {
        return String
            .format(
                "In turn %d, player %s returned creature of type %s into legion %s by undoing a summon",
                getTurn(), getPlayer().getName(), getAddedCreatureType()
                    .getName(), getLegion().getMarkerId());
    }
}
