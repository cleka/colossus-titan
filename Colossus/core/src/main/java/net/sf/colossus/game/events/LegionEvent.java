package net.sf.colossus.game.events;


import net.sf.colossus.game.Legion;


/**
 * A base class for all event affecting a single legion in the game.
 *
 * This exists only for implementation purposes and is not intended to
 * be instantiated directly.
 */
public abstract class LegionEvent implements GameEvent
{
    protected final Legion legion;

    public LegionEvent(Legion legion)
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