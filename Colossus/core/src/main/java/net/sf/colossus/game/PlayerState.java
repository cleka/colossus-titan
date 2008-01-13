package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * A player in a game.
 * 
 * This class holds all information describing a player in a game, such
 * as the current legions and the score. Instances of this class are always bound to
 * an instance of {@link Game}.
 */
public class PlayerState
{
    /**
     * The game this information belongs to.
     */
    private final Game game;

    /**
     * A name for this player for UI purposes and as identifier.
     */
    private String name;

    /**
     * The current legions owned by this player.
     */
    private final List<Legion> legions = new ArrayList<Legion>();

    /**
     * The number of the player in the game.
     * 
     * TODO clarify if this is just an arbitrary number (in which case we might want
     * to get rid of it) or the actual turn sequence
     */
    private final int number;

    /**
     * Set to true if the player is dead.
     * 
     * TODO check if that isn't equivalent to not having legions anymore
     */
    private boolean dead;

    public PlayerState(Game game, String playerName, int number)
    {
        assert game != null : "No game without Game";
        assert playerName != null : "Player needs a name";
        assert number >= 0 : "Player number must not be negative";
        // TODO check for max on number once game has the players stored in it
        this.game = game;
        this.name = playerName;
        this.number = number;
        this.dead = false;
    }

    public Game getGame()
    {
        return game;
    }

    public List<? extends Legion> getLegions()
    {
        return Collections.unmodifiableList(this.legions);
    }

    @Override
    public boolean equals(Object obj)
    {
        // make sure we don't compare apples and eggs
        assert obj.getClass() == this.getClass() : getClass().getName()
            + " compared with something else";
        return super.equals(obj);
    }

    public int getNumber()
    {
        return number;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isDead()
    {
        return dead;
    }

    public void setDead(boolean dead)
    {
        this.dead = dead;
    }
}
