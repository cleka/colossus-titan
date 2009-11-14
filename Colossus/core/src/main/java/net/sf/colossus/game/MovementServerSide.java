package net.sf.colossus.game;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.MasterHex;


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

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.
     *
     *  TODO use proper data structure instead of String serializations
     * @param hex TODO
     * @param legion TODO
     * @param roll TODO
     * @param block TODO
     * @param cameFrom TODO
     * @param fromHex TODO
     * @param ignoreFriends TODO
     * @param game TODO
     *  @return a set of hexLabel:entrySide string tuples.
     */
    @Override
    public Set<String> findNormalMoves(MasterHex hex, Legion legion, int roll,
        int block, int cameFrom, MasterHex fromHex, boolean ignoreFriends)
    {
        Set<String> set = new HashSet<String>();
        Player player = legion.getPlayer();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (game.getNumEnemyLegions(hex, player) > 0)
        {
            if (game.getNumFriendlyLegions(hex, player) == 0 || ignoreFriends)
            {
                // Set the entry side relative to the hex label.
                if (cameFrom != -1)
                {
                    set.add(hex.getLabel() + ":"
                        + MovementServerSide.findEntrySide(hex, cameFrom)
                            .getLabel());
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // XXX fix
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            List<? extends Legion> legions = player.getLegions();
            for (Legion otherLegion : legions)
            {
                if (!ignoreFriends && otherLegion != legion
                    && hex.equals(otherLegion.getCurrentHex()))
                {
                    return set;
                }
            }

            if (cameFrom != -1)
            {
                set.add(hex.getLabel()
                    + ":"
                    + MovementServerSide.findEntrySide(hex, cameFrom)
                        .getLabel());
                return set;
            }
        }

        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6,
                fromHex, ignoreFriends));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARCH
                    .ordinal()
                    && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        fromHex, ignoreFriends));
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
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6,
                        fromHex, ignoreFriends));
                }
            }
        }
        return set;
    }

    /** Verify whether this is a valid normal move.
    *
    * @param legion
    * @param hex
    * @param player
    * @return Reason why it is not a normal move, null if valid move
    */
    public String isValidNormalMove(Legion legion, MasterHex hex,
        Player player, int roll)
    {
        Set<MasterHex> set = listNormalMoves(legion, legion.getCurrentHex(),
            roll, false);

        if (!set.contains(hex))
        {
            String marker = legion.getMarkerId() + " "
                + ((LegionServerSide)legion).getMarkerName();
            return "List for normal moves " + set + " + of " + marker
                + " from " + legion.getCurrentHex() + " does not contain '"
                + hex + "'";
        }
        return null;
    }

    /** Verify whether this is a valid teleport move.
     *
     * @param legion
     * @param hex
     * @param roll
     * @return Reason why it is not a valid teleport move, null if valid move
     */
    public String isValidTeleportMove(Legion legion, MasterHex hex, int roll)
    {
        Set<MasterHex> set = listTeleportMovesSS(legion, legion
            .getCurrentHex(), roll);

        if (!set.contains(hex))
        {
            String marker = legion.getMarkerId() + " "
                + ((LegionServerSide)legion).getMarkerName();
            return "List for teleport moves " + set + " of " + marker
                + " from " + legion.getCurrentHex() + " does not contain '"
                + hex + "'";
        }
        return null;
    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting.  Include moves currently blocked by friendly
     *  legions if ignoreFriends is true.
     *  @return set of hexlabels
     */
    public Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        if (((LegionServerSide)legion).hasMoved())
        {
            return new HashSet<MasterHex>();
        }
        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, null, ignoreFriends);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set<MasterHex> result = new HashSet<MasterHex>();
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hexLabel = parts.get(0);

            result.add(game.getVariant().getMasterBoard().getHexByLabel(
                hexLabel));
        }
        return result;
    }

    @Override
    public Set<MasterHex> listTeleportMovesXX(Legion legion, MasterHex hex,
        int movementRoll)
    {
        return listTeleportMovesSS(legion, hex, movementRoll);
    }

}
