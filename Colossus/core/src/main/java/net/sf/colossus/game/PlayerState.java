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
     * 
     * A List of {@link Legion}s.
     */
    private final List legions = new ArrayList();

    public PlayerState(Game game, Player player)
    {
        this.player = player;
        this.game = game;
    }

    public Player getPlayer()
    {
        return player;
    }

    public Game getGame()
    {
        return game;
    }

    public List getLegions()
    {
        return Collections.unmodifiableList(this.legions);
    }
}
