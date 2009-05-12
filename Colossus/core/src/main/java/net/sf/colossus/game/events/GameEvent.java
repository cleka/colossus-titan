package net.sf.colossus.game.events;


import net.sf.colossus.game.Player;


/**
 * A class modelling all events that can happen in a Colossus game.
 *
 * This is the base class for all game-related event handling in Colossus. It
 * is not meant to be instantiated directly, but rather to be subclassed.
 *
 * All event objects should be immutable and thus threadsafe.
 */
public abstract class GameEvent
{
    private final int turn;

    private final Player player;

    /**
     * Creates a new game event.
     *
     * @param turn The turn in which the game happened.
     * @param player The player whose moved caused the event.
     */
    public GameEvent(int turn, Player player)
    {
        this.turn = turn;
        this.player = player;
    }

    /**
     * The turn in which the event happened.
     */
    public int getTurn()
    {
        return turn;
    }

    /**
     * The player whose move caused the event.
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * Returns a textual summary for logging purposes.
     */
    @SuppressWarnings("boxing")
    @Override
    public String toString()
    {
        return String.format("Game event in turn %d, player %s moving",
            getTurn(), getPlayer());
    }
}
