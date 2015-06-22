package net.sf.colossus.game;


import java.util.logging.Logger;

import net.sf.colossus.common.Options;


/**
 * Class MovementServerSide contains the server-side masterboard move logic
 * which has earlier been part of Game(ServerSide).
 * Some methods already pulled up to game.Movement.
 * There are still some methods that need pulling up, but they need more
 * refactoring before that can be done.
 *
 * @author David Ripton
 */

public class MovementServerSide extends Movement
{
    private static final Logger LOGGER = Logger
        .getLogger(MovementServerSide.class.getName());

    public MovementServerSide(Game game, Options options)
    {
        super(game, options);
        LOGGER.finest("MovementServerSide instantiated");
    }

}
