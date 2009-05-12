package net.sf.colossus.game.events;


import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;


/**
 * A base class for all event affecting a single legion in the game.
 *
 * This exists only for implementation purposes and is not intended to
 * be instantiated directly.
 */
public abstract class LegionEvent extends GameEvent
{
    protected final Legion legion;

    // TODO shall we infer the player from the legion or are there cases where that wouldn't be correct
    public LegionEvent(int turn, Player player, Legion legion)
    {
        super(turn, player);
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