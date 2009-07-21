package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class Acquisition extends AddCreatureAction
{
    public Acquisition(Legion legion, CreatureType creatureType)
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
            "Acquisition of creature of type %s in legion %s",
            getAddedCreatureType(), getLegion());
    }
}
