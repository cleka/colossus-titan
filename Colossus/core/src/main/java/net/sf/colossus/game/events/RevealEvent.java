package net.sf.colossus.game.events;


import net.sf.colossus.variant.CreatureType;

public interface RevealEvent
{
    CreatureType[] getRevealedCreatures();
}
