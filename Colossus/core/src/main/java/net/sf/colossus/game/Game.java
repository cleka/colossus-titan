package net.sf.colossus.game;


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
     * 
     * TODO use List instead
     */
    private final Player[] playerstates;

    public Game(Variant variant, String[] playerNames)
    {
        this.variant = variant;
        this.playerstates = new Player[playerNames.length];
        for (int i = 0; i < playerNames.length; i++)
        {
            playerstates[i] = new Player(this, playerNames[i], i);
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
