package net.sf.colossus.game;


/**
 * Class Movement has the masterboard move logic - or that part that could
 * already be unified and pulled up from server/client sides.
 * There are still some methods that need pulling up, but they need more
 * refactoring before that can be done.
 *
 * @author Clemens Katzer (created the new combined game.Movement class)
 * @author David Ripton (e.g. original client.Movement class)
 * @author possibly: Bruce Sherrod, Romain Dolbeau (old server.Game class)
 */
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.MasterHex;


abstract public class Movement
{
    private static final Logger LOGGER = Logger.getLogger(Movement.class
        .getName());

    protected final Game game;
    protected final Options options;

    public Movement(Game game, Options options)
    {
        // just here so that LOGGER is used :)
        LOGGER.finest("Movement instantiated");

        this.game = game;
        this.options = options;
    }

    /** Set the entry side relative to the hex label. */
    protected static EntrySide findEntrySide(MasterHex hex, int cameFrom)
    {
        int entrySide = -1;
        if (cameFrom != -1)
        {
            if (hex.getTerrain().hasStartList())
            {
                entrySide = 3;
            }
            else
            {
                entrySide = (6 + cameFrom - hex.getLabelSide()) % 6;
            }
        }
        return EntrySide.values()[entrySide];
    }

    protected static int findBlock(MasterHex hex)
    {
        int block = Constants.ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == Constants.HexsideGates.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }
        return block;
    }

    /** Recursively find all unoccupied hexes within roll hexes, for
     *  tower teleport. */
    protected Set<MasterHex> findNearbyUnoccupiedHexes(MasterHex hex,
        Legion legion, int roll, int cameFrom)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        Set<MasterHex> result = new HashSet<MasterHex>();

        if (!game.isOccupied(hex))
        {
            result.add(hex);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom
                    && (hex.getExitType(i) != Constants.HexsideGates.NONE || hex
                        .getEntranceType(i) != Constants.HexsideGates.NONE))
                {
                    result.addAll(findNearbyUnoccupiedHexes(
                        hex.getNeighbor(i), legion, roll - 1, (i + 3) % 6));
                }
            }
        }

        return result;
    }

    public boolean titanTeleportAllowed()
    {
        if (options.getOption(Options.noTitanTeleport))
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && options.getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    protected boolean towerTeleportAllowed()
    {
        if (options.getOption(Options.noTowerTeleport))
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && options.getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    protected boolean towerToTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && options.getOption(Options.noFirstTurnT2TTeleport))
        {
            return false;
        }
        return true;
    }

    protected boolean towerToNonTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (options.getOption(Options.towerToTowerTeleportOnly))
        {
            return false;
        }
        return true;
    }

    /** Return set of hexLabels describing where this legion can teleport.
     *  @return set of hexlabels
     */
    protected Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean inAdvance)
    {
        Player player = legion.getPlayer();
        Set<MasterHex> result = new HashSet<MasterHex>();

        if (hex == null)
        {
            LOGGER.warning("listTeleportMoves called with null hex!");
            return result;
        }

        if (((movementRoll != 6 || legion.hasMoved() || player.hasTeleported()) && !inAdvance))
        {
            return result;
        }

        // Tower teleport
        if (hex.getTerrain().isTower() && legion.numLords() > 0
            && towerTeleportAllowed())
        {
            // Mark every unoccupied hex within 6 hexes.
            if (towerToNonTowerTeleportAllowed())
            {
                result.addAll(findNearbyUnoccupiedHexes(hex, legion, 6,
                    Constants.NOWHERE));
            }

            if (towerToTowerTeleportAllowed())
            {
                // Mark every unoccupied tower.
                for (MasterHex tower : game.getVariant().getMasterBoard()
                    .getTowerSet())
                {
                    if (!game.isOccupied(tower) && !(tower.equals(hex)))
                    {
                        result.add(tower);
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                result.removeAll(game.getVariant().getMasterBoard()
                    .getTowerSet());
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan()
            && titanTeleportAllowed())
        {
            // Mark every hex containing an enemy stack that does not
            // already contain a friendly stack.
            for (Legion other : game.getEnemyLegions(player))
            {
                MasterHex otherHex = other.getCurrentHex();
                if (!game.containsOpposingLegions(otherHex))
                {
                    result.add(otherHex);
                }
            }
        }
        result.remove(null);
        return result;
    }

    /** Return set of hexLabels describing where this legion can teleport. */
    public Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll)
    {
        // Call with inAdvance=false
        return listTeleportMoves(legion, hex, movementRoll, false);
    }

    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return
     *  one entry side since it doesn't matter. */
    public Set<EntrySide> listPossibleEntrySides(Legion legion,
        MasterHex targetHex, boolean teleport)
    {
        Set<EntrySide> entrySides = new HashSet<EntrySide>();
        int movementRoll = game.getMovementRoll();
        MasterHex currentHex = legion.getCurrentHex();

        if (teleport)
        {
            if (isValidTeleportMove(legion, targetHex, movementRoll) == null)
            {
                // Startlisted terrain only have bottom entry side.
                // Don't bother finding more than one entry side if unoccupied.
                if (!game.isOccupied(targetHex)
                    || targetHex.getTerrain().hasStartList())
                {
                    entrySides.add(EntrySide.BOTTOM);
                    return entrySides;
                }
                else
                {
                    entrySides.add(EntrySide.BOTTOM);
                    entrySides.add(EntrySide.LEFT);
                    entrySides.add(EntrySide.RIGHT);
                    return entrySides;
                }
            }
            else
            {
                return entrySides;
            }
        }

        // Normal moves.
        Set<String> tuples = findNormalMoves(currentHex, legion, movementRoll,
            findBlock(currentHex), Constants.NOWHERE, null, false);
        Iterator<String> it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = it.next();
            List<String> parts = Split.split(':', tuple);
            String hl = parts.get(0);

            if (hl.equals(targetHex.getLabel()))
            {
                String buf = parts.get(1);
                entrySides.add(EntrySide.fromLabel(buf));
            }
        }
        return entrySides;
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
        Set<MasterHex> set = listNormalMoves(legion, hex, movementRoll, false,
            null, inAdvance);
        set.addAll(listTeleportMoves(legion, hex, movementRoll, inAdvance));
        return set;
    }

    public Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll)
    {
        return listNormalMoves(legion, hex, movementRoll, false, null, false);
    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting.
     *  Include moves currently blocked by friendly legions
     *  if ignoreFriends is true.
     *  @return set of hexlabels
     */
    public Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends, MasterHex fromHex,
        boolean inAdvance)
    {
        if (hex == null || (legion.hasMoved() && !inAdvance))
        {
            return new HashSet<MasterHex>();
        }

        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, fromHex, ignoreFriends);

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

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.
     *
     *  TODO get rid of this String serialization and return a proper data
     *       structure
     *
     *  @return a set of hexLabel:entrySide tuples.
     */
    public Set<String> findNormalMoves(MasterHex hex, Legion legion, int roll,
        int block, int cameFrom, MasterHex fromHex, boolean ignoreFriends)
    {
        Set<String> result = new HashSet<String>();
        Player player = legion.getPlayer();

        // TODO is fromHex actually useful?
        // if (game.getNumEnemyLegions(hex, player) > 0 && !hex.equals(fromHex))
        // Server side didn't have it at all, and in client side it was not
        // passed on to the recursive calls. So it's set in first hex anyway,
        // but in first hex there can't be an enemy legion, or can there?
        //
        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        // Do a check versus fromHexLabel if we are evaluating
        // passing through this hex
        if (game.getNumEnemyLegions(hex, player) > 0 && !hex.equals(fromHex))
        {
            if (game.getNumFriendlyLegions(hex, player) == 0 || ignoreFriends)
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
            // This is the originally server side functionality:
            // XXX fix
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            List<? extends Legion> legions = player.getLegions();
            for (Legion otherLegion : legions)
            {
                if (!ignoreFriends && otherLegion != legion
                    && hex.equals(otherLegion.getCurrentHex()))
                {
                    return result;
                }
            }
            /* The part below is how it was in MovementClientSide.
             * When I tried to use that, the cycle/spin case with split
             * legions produces NAKs - server thinks the player has valid
             * conventional moves.
             */
            /*
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions that have already moved.
            // Account for spin cycles.
            List<Legion> legions = game.getFriendlyLegions(hex, player);
            if (!legions.isEmpty() && !ignoreFriends)
            {
                // it's enough to check one; there can never be a moved one
                // and a not moved one in same hex
                if (legions.get(0).hasMoved())
                {
                    return result;
                }
            }
            */
            if (cameFrom != -1)
            {
                result.add(hex.getLabel() + ":"
                    + findEntrySide(hex, cameFrom).getLabel());
                return result;
            }
        }
        else if (roll < 0)
        {
            LOGGER.log(
                Level.SEVERE,
                "Movement.findNormalMoves() was called with negative roll number "
                    + roll + "; legion " + legion.getMarkerId()
                    + ", fromHex = " + fromHex.getLabel() + ", hex="
                    + hex.getLabel());
            return result;
        }

        if (block >= 0)
        {
            result.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6, null,
                ignoreFriends));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARCH
                    .ordinal() && i != cameFrom)
                {
                    result.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null,
                        ignoreFriends));
                }
            }
        }
        else if (block == Constants.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i).ordinal() >= Constants.HexsideGates.ARROW
                    .ordinal() && i != cameFrom)
                {
                    result.addAll(findNormalMoves(hex.getNeighbor(i), legion,
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null,
                        ignoreFriends));
                }
            }
        }

        return result;
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
        Set<MasterHex> set = listTeleportMoves(legion, legion.getCurrentHex(),
            roll, false);

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
            roll, false, null, false);

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

    /** Verify whether this is a valid entry side.
    *
    *  @param legion The moving legion
    *  @param hex The aimed target hex
    *  @param teleport Whether this is a teleported move
    *  @return Reason why it is not a valid entry side, null if valid
    */
    public String isValidEntrySide(Legion legion, MasterHex hex,
        boolean teleport, EntrySide entrySide)
    {
        Set<EntrySide> legalSides = listPossibleEntrySides(legion, hex,
            teleport);
        if (!legalSides.contains(entrySide))
        {
            return "EntrySide '" + entrySide + "' is not valid, valid are: "
                + legalSides.toString();
        }
        return null;
    }

}
