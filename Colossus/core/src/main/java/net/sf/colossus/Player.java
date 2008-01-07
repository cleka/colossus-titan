package net.sf.colossus;


import java.util.HashMap;
import java.util.Map;

import net.sf.colossus.game.Game;
import net.sf.colossus.game.PlayerState;
import net.sf.colossus.variant.Variant;


/**
 * A player (person or AI) able to play Colossus games.
 * 
 * This interface models just the player, not the actual involvement in a game.
 * To create an ongoing game, an instance of {@link Variant} and a number of
 * instances of this class are needed, the {@link Game} instance then models 
 * the dynamic aspects of a game and stores the player's state in the game in
 * {@link PlayerState}.
 */
public class Player
{
    /**
     * A name for this player for UI purposes.
     */
    private String name;

    private static final Map<String, Player> KNOWN_PLAYERS = new HashMap<String, Player>();

    public Player(String name)
    {
        this.name = name;
        KNOWN_PLAYERS.put(name, this);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        KNOWN_PLAYERS.remove(this.name);
        this.name = name;
        KNOWN_PLAYERS.put(name, this);
    }

    public static Player getPlayerByName(String name)
    {
        Player player = KNOWN_PLAYERS.get(name);
        // TODO remove implicit player creation once the rest of the code
        // creates the players
        if (player == null)
        {
            player = new Player(name);
        }
        return player;
    }
}
