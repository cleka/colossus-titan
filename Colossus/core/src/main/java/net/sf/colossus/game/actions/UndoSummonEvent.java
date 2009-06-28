package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;

public class UndoSummonEvent extends AddCreatureEvent
{
    public UndoSummonEvent(Legion legion, CreatureType creatureType)
    {
        super(legion, creatureType);
    }

    @Override
    public String getReason()
    {
        return Constants.reasonUndoSummon;
    }

    @Override
    public String toString()
    {
        return String
            .format(
                "UndoSummonEvent: return creature of type %s into legion %s by undoing a summon",
                getAddedCreatureType(), getLegion());
    }
}
