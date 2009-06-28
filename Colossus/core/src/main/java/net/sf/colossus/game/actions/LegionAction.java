package net.sf.colossus.game.actions;


import net.sf.colossus.game.Legion;


/**
 * A base class for all actions affecting a single legion in the game.
 *
 * This exists only for implementation purposes and is not intended to
 * be instantiated directly.
 */
public abstract class LegionAction implements GameAction
{
    protected final Legion legion;

    public LegionAction(Legion legion)
    {
        this.legion = legion;
    }

    /**
     * The legion that was changed.
     */
    public Legion getLegion()
    {
        return legion;
    }
}