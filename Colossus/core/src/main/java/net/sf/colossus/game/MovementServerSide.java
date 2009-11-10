package net.sf.colossus.game;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.server.LegionServerSide;
import net.sf.colossus.server.PlayerServerSide;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.MasterHex;


public class MovementServerSide extends Movement
{
    private static final Logger LOGGER = Logger
        .getLogger(MovementServerSide.class.getName());


    public MovementServerSide(Game game, Options options)
    {
        super(game, options);

        LOGGER.finest("MovementServerSide instantiated");
    }

    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return
     *  one entry side since it doesn't matter.
     * @param bm TODO
     * @param game TODO
     * @param legion TODO
     * @param targetHex TODO
     * @param teleport TODO*/
    public Set<EntrySide> listPossibleEntrySides(Legion legion,
        MasterHex targetHex, boolean teleport)
    {
        Set<EntrySide> entrySides = new HashSet<EntrySide>();
        Player player = legion.getPlayer();
        int movementRoll = ((PlayerServerSide)player).getMovementRollSS();
        MasterHex currentHex = legion.getCurrentHex();

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll, false)
                .contains(targetHex))
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
        Set<String> tuples = findNormalMoves(currentHex, legion,
            movementRoll,
            findBlock(currentHex), Constants.NOWHERE, false);
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

                // Clemens 4.10.2007:
                // This optimization can lead to problems ("Illegal entry side")
                // in mountains/tundra on a movement roll 4, when client and
                // server store the items in their Move-hashmaps in different
                // order (different java version, platform, ... ?)
                // So, removed this optimization to see whether it fixes the bug:
                //  [colossus-Bugs-1789116 ] illegal move: 29 plain to 2000 tundra
                /*
                 // Don't bother finding more than one entry side if unoccupied.
                 if (!isOccupied(targetHexLabel))
                 {
                 return entrySides;
                 }
                 */
            }
        }
        return entrySides;
    }

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of
     *  hexLabel:entrySide tuples.
     *
     *  TODO use proper data structure instead of String serializations
     * @param game TODO
     * @param hex TODO
     * @param legion TODO
     * @param roll TODO
     * @param block TODO
     * @param cameFrom TODO
     * @param ignoreFriends TODO
     */
    public Set<String> findNormalMoves(MasterHex hex, Legion legion, int roll,
        int block, int cameFrom, boolean ignoreFriends)
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
                ignoreFriends));
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
                        ignoreFriends));
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
                        ignoreFriends));
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
     * @param player
     * @return Reason why it is not a valid teleport move, null if valid move
     */
    public String isValidTeleportMove(Legion legion, MasterHex hex,
        Player player, int roll)
    {
        Set<MasterHex> set = listTeleportMoves(legion, legion
            .getCurrentHex(),
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

    /** Verify whether this is a valid entry side.
     *
     * @param legion
     * @param hex
     * @param player
     * @return Reason why it is not a valid entry side, null if valid
     */
    public String isValidEntrySide(Legion legion, MasterHex hex,
        boolean teleport, EntrySide entrySide)
    {
        Set<EntrySide> legalSides = listPossibleEntrySides(legion,
            hex, teleport);
        if (!legalSides.contains(entrySide))
        {
            return "EntrySide '" + entrySide + "' is not valid, valid are: "
                + legalSides.toString();
        }
        return null;

    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting.  Include moves currently blocked by friendly
     *  legions if ignoreFriends is true. */
    public Set<MasterHex> listNormalMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        if (((LegionServerSide)legion).hasMoved())
        {
            return new HashSet<MasterHex>();
        }
        Set<String> tuples = findNormalMoves(hex, legion, movementRoll,
            findBlock(hex), Constants.NOWHERE, ignoreFriends);

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

    /** Return set of hexLabels describing where this legion can teleport.
     *  Include moves currently blocked by friendly legions if
     *  ignoreFriends is true.
     * @param game TODO*/
    public Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean ignoreFriends)
    {
        Player player = legion.getPlayer();
        Set<MasterHex> result = new HashSet<MasterHex>();
        if (movementRoll != 6 || legion.hasMoved() || player.hasTeleported())
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
                Set<MasterHex> towerSet = game.getVariant().getMasterBoard()
                    .getTowerSet();
                for (MasterHex tower : towerSet)
                {
                    if ((!game.isOccupied(tower) || (ignoreFriends && game
                        .getNumEnemyLegions(tower, player) == 0))
                        && (!(tower.equals(hex))))
                    {
                        result.add(tower);
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                Set<MasterHex> towerSet = game.getVariant().getMasterBoard()
                    .getTowerSet();
                for (MasterHex tower : towerSet)
                {
                    result.remove(tower);
                }
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan()
            && titanTeleportAllowed())
        {
            // Mark every hex containing an enemy stack that does not
            // already contain a friendly stack.
            for (Legion other : game.getAllEnemyLegions(player))
            {
                MasterHex otherHex = other.getCurrentHex();
                if (!game.isEngagement(otherHex) || ignoreFriends)
                {
                    result.add(otherHex);
                }
            }
        }
        result.remove(null);
        return result;
    }



}
