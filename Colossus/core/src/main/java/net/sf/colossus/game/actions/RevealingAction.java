package net.sf.colossus.game.actions;


import net.sf.colossus.variant.CreatureType;


/**
 * An action that might reveal one or more creatures in a legion.
 */
public interface RevealingAction
{
    CreatureType[] getRevealedCreatures();
}
