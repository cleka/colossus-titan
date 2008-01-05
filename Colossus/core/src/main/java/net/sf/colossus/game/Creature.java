package net.sf.colossus.game;


import net.sf.colossus.variant.CreatureType;


/**
 * A particular creature in a game.
 * 
 * This represents a creature in a game, such as a specific Cyclops as part of
 * a legion.
 * 
 * It includes the state for battles, i.e. the damage taken so far.
 */
public class Creature
{
    private final CreatureType type;

    public Creature(CreatureType type)
    {
        this.type = type;
    }

    public CreatureType getType()
    {
        return type;
    }
}
