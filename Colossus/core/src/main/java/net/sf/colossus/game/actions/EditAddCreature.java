package net.sf.colossus.game.actions;


import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class EditAddCreature extends AddCreatureAction
{

    public EditAddCreature(Legion legion, CreatureType creatureType)
    {
        super(legion, creatureType);
    }

    @Override
    public String getReason()
    {
        return Constants.reasonEdit;
    }

    @Override
    public String toString()
    {
        return String.format(
            "Manually adding of creature of type %s in legion %s",
            getAddedCreatureType(), getLegion());
    }
}
