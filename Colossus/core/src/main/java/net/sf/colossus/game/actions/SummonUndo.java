package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class SummonUndo extends AddCreatureAction
{
    public SummonUndo(Legion legion, CreatureType creatureType)
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
        return String.format(
            "Creature of type %s returned into legion %s by undoing a summon",
            getAddedCreatureType(), getLegion());
    }
}
