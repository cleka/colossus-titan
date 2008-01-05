package net.sf.colossus;



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
public interface Player
{
    // TODO
}
