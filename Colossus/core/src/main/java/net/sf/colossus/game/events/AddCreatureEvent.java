package net.sf.colossus.game.events;

import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


/**
 * An event emitted whenever a creature gets added to a legion.
 *
 * This is meant to be used only as interface or through subclasses.
 *
 * TODO make abstract once History handles the subclasses properly
 */
public class AddCreatureEvent extends LegionEvent implements
    RevealEvent
{
    private final CreatureType creatureType;

    public AddCreatureEvent(Legion legion,
        CreatureType creatureType)
    {
        super(legion);
        this.creatureType = creatureType;
    }

    /**
     * The type of creature that was added.
     */
    public CreatureType getAddedCreatureType()
    {
        return creatureType;
    }

    public CreatureType[] getRevealedCreatures()
    {
        return new CreatureType[] { creatureType };
    }

    /**
     * Returns a string representing the reason for the addition.
     *
     * TODO remove in favour of using the event hierarchy
     * TODO should be abstract here, but History still creates instances of this class
     */
    public String getReason()
    {
        return "unknown";
    }

    @Override
    public String toString()
    {
        return String.format(
            "AddCreatureEvent: add creature of type %s to legion %s",
            getAddedCreatureType(), getLegion());
    }
}
