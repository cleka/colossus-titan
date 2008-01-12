package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.colossus.Player;


/**
 * The state of a player in a game.
 * 
 * This class holds all information describing the state of a player in a game, such
 * as the current legions and the score. Instances of this class are always bound to
 * an instance of {@link Game}.
 */
public class PlayerState
{
    /**
     * The player whose state is modeled.
     */
    private final Player player;

    /**
     * The game this information belongs to.
     */
    private final Game game;

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

    public PlayerState(Game game, Player player, int number)
    {
        assert player != null : "Player required for joining a game";
        assert game != null : "No game without Game";
        assert number >= 0 : "Player number must not be negative";
        // TODO check for max on number once game has the players stored in it
        this.player = player;
        this.game = game;
        this.number = number;
    }

    /**
     * TODO there are quite a few places (mostly in the Client) where PlayerInfo/PlayerState
     * should be used instead of player -- calling this getter will be an indication of that.
     */
    public Player getPlayer()
    {
        return player;
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
}
