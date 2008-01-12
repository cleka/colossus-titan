package net.sf.colossus.game;


import net.sf.colossus.Player;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.Variant;


/**
 * An ongoing game in Colossus.
 * 
 * As opposed to {@link Variant} this class holds information about an ongoing game
 * and its status.
 * 
 * Instances of this class are immutable.
 */
public class Game
{
    /**
     * The variant played in this game.
     */
    private final Variant variant;

    /**
     * The state of the different players in the game. 
     */
    private final PlayerState[] playerstates;

    public Game(Variant variant, Player[] players)
    {
        this.variant = variant;
        this.playerstates = new PlayerState[players.length];
        for (int i = 0; i < players.length; i++)
        {
            playerstates[i] = new PlayerState(this, players[i], i);
        }
    }

    public Variant getVariant()
    {
        if (variant != null)
        {
            return variant;
        }
        else
        {
            // TODO this is just temporarily until the variant member always gets initialized
            // properly
            return VariantSupport.getCurrentVariant();
        }
    }
}
