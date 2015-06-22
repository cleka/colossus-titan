package net.sf.colossus.game;


import java.util.logging.Logger;

import net.sf.colossus.common.Options;


/**
 * Class MovementClientSide contains the client-side masterboard move logic.
 * Some methods already pulled up to game.Movement.
 * There are still some methods that need pulling up, but they need more
 * refactoring before that can be done.
 *
 * @author David Ripton
 */
public final class MovementClientSide extends Movement
{
    private static final Logger LOGGER = Logger
        .getLogger(MovementClientSide.class.getName());

    public MovementClientSide(Game game, Options options)
    {
        super(game, options);
        LOGGER.finest("MovementClientSide instantiated");
    }

}
