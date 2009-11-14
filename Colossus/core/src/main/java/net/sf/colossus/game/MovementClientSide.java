package net.sf.colossus.game;


import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.MasterHex;


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
    private static final Logger LOGGER = Logger.getLogger(MovementClientSide.class
        .getName());

    public MovementClientSide(Game game, Options options)
    {
        super(game, options);
    }

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of
     *  hexLabel:entrySide tuples.
     *
     *  TODO get rid of this String serialization and return a proper data
     *       structure
     * @param ignoreFriends TODO
     */
    @Override
    protected Set<String> findNormalMoves(MasterHex hex, Legion legion,
        int roll, int block, int cameFrom, MasterHex fromHex, boolean ignoreFriends)
    {
        Set<String> result = new HashSet<String>();
        Player player = legion.getPlayer();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        // Do a check versus fromHexLabel if we are evaluating
        // passing through this hex
        if (game.getEnemyLegions(hex, player).size() > 0
            && !hex.equals(fromHex))
        {
            if (game.getFriendlyLegions(hex, player)
                .size() == 0)
            {
                // Set the entry side relative to the hex label.
                if (cameFrom != -1)
                {
                    result.add(hex.getLabel() + ":"
                        + findEntrySide(hex, cameFrom).getLabel());
                }
            }
            return result;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions that have already moved.
            // Account for spin cycles.
            if (game.getFriendlyLegions(hex, player)
                .size() > 0)
            {
                List<Legion> legions = game.getLegionsByHex(hex);
                if (legions.get(0).hasMoved())
                {
                    return result;
                }
            }

            if (cameFrom != -1)
            {
                result.add(hex.getLabel() + ":"
                    + findEntrySide(hex, cameFrom).getLabel());
                return result;
            }
        }
        else if (roll < 0)
        {
            LOGGER.log(Level.SEVERE, "Movement.findNormalMoves() roll < 0");
            return null;
        }

        if (block >= 0)
        {
            result.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6, null, ignoreFriends));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARCH
                    .ordinal()
                    && i != cameFrom)
                {
                    result.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null, ignoreFriends));

                }
            }
        }
        else if (block == Constants.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARROW
                    .ordinal()
                    && i != cameFrom)
                {
                    result.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null, ignoreFriends));
                }
            }
        }

        return result;
    }

    /** Return set of hexLabels describing where this legion can move. */
    public Set<MasterHex> listAllMoves(Legion legion, MasterHex hex,
        int movementRoll)
    {
        return listAllMoves(legion, hex, movementRoll, false);
    }

    /** Return set of hexLabels describing where this legion can move. */
    public Set<MasterHex> listAllMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean inAdvance)
    {
        Set<MasterHex> set = listNormalMoves(legion, hex, movementRoll,
            inAdvance, null);
        set.addAll(listTeleportMoves(legion, hex, movementRoll, inAdvance));
        return set;
    }

    public Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll)
    {
        return listNormalMoves(legion, hex, movementRoll, false, null);
    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting. */
    private Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean inAdvance, MasterHex fromHex)
    {
        if (hex == null || (legion.hasMoved() && !inAdvance))
        {
            return new HashSet<MasterHex>();
        }

        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, fromHex, false);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set<MasterHex> result = new HashSet<MasterHex>();
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hexLabel = parts.get(0);
            result.add(game.getVariant().getMasterBoard()
                .getHexByLabel(hexLabel));
        }
        return result;
    }

    @Override
    public Set<MasterHex> listTeleportMovesXX(Legion legion, MasterHex hex, int movementRoll)
    {
        return listTeleportMovesCS(legion, hex,movementRoll);
    }
}
