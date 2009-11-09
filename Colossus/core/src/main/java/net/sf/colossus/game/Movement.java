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
 * Class Movement handles client-side masterboard moves
 * (now pulled up to game; server side code could start using this
 * instead of the GameServerSide methods).
 *
 * @author David Ripton
 */
// XXX There is massive duplication of code between this class and
// the server-side movement code in Game.  Need to completely refactor.
public final class Movement
{
    private static final Logger LOGGER = Logger.getLogger(Movement.class
        .getName());

    private final Game game;
    private final Options options;

    public Movement(Game game, Options options)
    {
        this.game = game;
        this.options = options;
    }

    /** Set the entry side relative to the hex label. */
    private static EntrySide findEntrySide(MasterHex hex, int cameFrom)
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

    /** Recursively find conventional moves from this hex.
     *  If block >= 0, go only that way.  If block == -1, use arches and
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of
     *  hexLabel:entrySide tuples.
     *
     *  TODO get rid of this String serialization and return a proper data
     *       structure
     */
    private Set<String> findNormalMoves(MasterHex hex, Legion legion,
        int roll, int block, int cameFrom, MasterHex fromHex)
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
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6, null));
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
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null));

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
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6, null));
                }
            }
        }

        return result;
    }

    /** Recursively find all unoccupied hexes within roll hexes, for
     *  tower teleport. */
    private Set<MasterHex> findNearbyUnoccupiedHexes(MasterHex hex,
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

    private static int findBlock(MasterHex hex)
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
            findBlock(hex), Constants.NOWHERE, fromHex);

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

    private boolean towerTeleportAllowed()
    {
        if (getOption(Options.noTowerTeleport))
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && getOption(Options.noFirstTurnT2TTeleport))
        {
            return false;
        }
        return true;
    }

    private boolean towerToNonTowerTeleportAllowed()
    {
        if (!towerTeleportAllowed())
        {
            return false;
        }
        if (getOption(Options.towerToTowerTeleportOnly))
        {
            return false;
        }
        return true;
    }

    public boolean titanTeleportAllowed()
    {
        if (getOption(Options.noTitanTeleport))
        {
            return false;
        }
        if (game.getTurnNumber() == 1
            && getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    /** Return set of hexLabels describing where this legion can teleport. */
    public Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll)
    {
        return listTeleportMoves(legion, hex, movementRoll, false);
    }

    /** Return set of hexLabels describing where this legion can teleport. */
    private Set<MasterHex> listTeleportMoves(Legion legion, MasterHex hex,
        int movementRoll, boolean inAdvance)
    {
        Player player = legion.getPlayer();

        Set<MasterHex> result = new HashSet<MasterHex>();
        if (hex == null
            || (!inAdvance && (movementRoll != 6 || legion.hasMoved() || player
                .hasTeleported())))
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
                for (MasterHex tower : game.getVariant()
                    .getMasterBoard().getTowerSet())
                {
                    if (!game.isOccupied(tower)
                        && !(tower.equals(hex)))
                    {
                        result.add(tower);
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                result.removeAll(game.getVariant()
                    .getMasterBoard().getTowerSet());
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
                if (!game.isEngagement(otherHex))
                {
                    result.add(otherHex);
                }
            }
        }
        result.remove(null);
        return result;
    }

    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return
     *  one entry side since it doesn't matter. */
    public Set<EntrySide> listPossibleEntrySides(Legion legion,
        MasterHex targetHex,
        boolean teleport)
    {
        Set<EntrySide> entrySides = new HashSet<EntrySide>();
        int movementRoll = game.getMovementRoll();
        MasterHex currentHex = legion.getCurrentHex();

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll).contains(
                targetHex))
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
            findBlock(currentHex), Constants.NOWHERE, null);
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
                // Don't bother finding more than one entry side if unoccupied.
                if (!game.isOccupied(targetHex))
                {
                    return entrySides;
                }
            }
        }
        return entrySides;
    }

    // NOTE Using listener would be nicer, but problematic with the
    // string/boolean issues with options coming from server.
    // See details in the comment there where Movement is instantiated.
    private boolean getOption(String optName)
    {
        return options.getOption(optName);
    }

}
