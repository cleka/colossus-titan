package net.sf.colossus.client;

import java.util.*;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.Log;
import net.sf.colossus.server.Constants;


/**
 * Class Movement handles client-side masterboard moves.
 * @version $Id$
 * @author David Ripton
 */

// XXX There is massive duplication of code between this class and
// the server-side movement code in Game.  Need to completely refactor
// this after networking is done.

public final class Movement
{
    Client client;


    Movement(Client client)
    {
        this.client = client;
    }


    /** Set the entry side relative to the hex label. */
    private static int findEntrySide(MasterHex hex, int cameFrom)
    {
        int entrySide = -1;
        if (cameFrom != -1)
        {
            if (HexMap.terrainHasStartlist(hex.getTerrain())) 
            {
                entrySide = 3;
            }
            else
            {
                entrySide = (6 + cameFrom - hex.getLabelSide()) % 6;
            }
        }
        return entrySide;
    }


    /** Recursively find conventional moves from this hex.  
     *  If block >= 0, go only that way.  If block == -1, use arches and 
     *  arrows.  If block == -2, use only arrows.  Do not double back in
     *  the direction you just came from.  Return a set of 
     *  hexLabel:entrySide tuples. */
    private Set findNormalMoves(MasterHex hex, LegionInfo legion,
        int roll, int block, int cameFrom)
    {
        Set set = new HashSet();
        String hexLabel = hex.getLabel();
        PlayerInfo player = legion.getPlayerInfo();

        // If there are enemy legions in this hex, mark it
        // as a legal move and stop recursing.  If there is
        // also a friendly legion there, just stop recursing.
        if (client.getNumEnemyLegions(hexLabel, player.getName()) > 0)
        {
            if (client.getNumFriendlyLegions(hexLabel, player.getName()) == 0)
            {
                // Set the entry side relative to the hex label.
                if (cameFrom != -1)
                {
                    set.add(hexLabel + ":" + BattleMap.entrySideName(
                        findEntrySide(hex, cameFrom)));
                }
            }
            return set;
        }

        if (roll == 0)
        {
            // This hex is the final destination.  Mark it as legal if
            // it is unoccupied by friendly legions.
            // Account for spin cycles.
            if (client.getNumFriendlyLegions(hexLabel, player.getName()) > 0)
            {
                java.util.List markerIds = client.getLegionsByHex(hexLabel);
                if (markerIds.size() > 1 || !legion.getMarkerId().equals(
                    (String)markerIds.get(0)))
                {
                    return set;
                }
            }

            if (cameFrom != -1)
            {
                set.add(hexLabel + ":" + BattleMap.entrySideName(
                    findEntrySide(hex, cameFrom)));
                return set;
            }
        }
        else if (roll < 0)
        {
            Log.error("Movement.findNormalMoves() roll < 0");
            return null;
        }

        if (block >= 0)
        {
            set.addAll(findNormalMoves(hex.getNeighbor(block), legion,
                roll - 1, Constants.ARROWS_ONLY, (block + 3) % 6));
        }
        else if (block == Constants.ARCHES_AND_ARROWS)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= Constants.ARCH && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion, 
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6));
                        
                }
            }
        }
        else if (block == Constants.ARROWS_ONLY)
        {
            for (int i = 0; i < 6; i++)
            {
                if (hex.getExitType(i) >= Constants.ARROW && i != cameFrom)
                {
                    set.addAll(findNormalMoves(hex.getNeighbor(i), legion, 
                        roll - 1, Constants.ARROWS_ONLY, (i + 3) % 6));
                }
            }
        }

        return set;
    }


    /** Recursively find all unoccupied hexes within roll hexes, for
     *  tower teleport. */
    private Set findNearbyUnoccupiedHexes(MasterHex hex, LegionInfo legion, 
        int roll, int cameFrom)
    {
        // This hex is the final destination.  Mark it as legal if
        // it is unoccupied.
        String hexLabel = hex.getLabel();
        Set set = new HashSet();

        if (!client.isOccupied(hexLabel))
        {
            set.add(hexLabel);
        }

        if (roll > 0)
        {
            for (int i = 0; i < 6; i++)
            {
                if (i != cameFrom && (hex.getExitType(i) != Constants.NONE ||
                   hex.getEntranceType(i) != Constants.NONE))
                {
                    set.addAll(findNearbyUnoccupiedHexes(hex.getNeighbor(i),
                        legion, roll - 1, (i + 3) % 6));
                }
            }
        }

        return set;
    }

    /** Return set of hexLabels describing where this legion can move. */
    public Set listAllMoves(LegionInfo legion, MasterHex hex, int movementRoll)
    {
        return listAllMoves(legion, hex, movementRoll, false);
    }

    /** Return set of hexLabels describing where this legion can move. */
    public Set listAllMoves(LegionInfo legion, MasterHex hex, int movementRoll,
                            boolean inAdvance)
    {
        Set set = listNormalMoves(legion, hex, movementRoll, inAdvance);
        set.addAll(listTeleportMoves(legion, hex, movementRoll, inAdvance));
        return set;
    }


    private static int findBlock(MasterHex hex)
    {
        int block = Constants.ARCHES_AND_ARROWS;
        for (int j = 0; j < 6; j++)
        {
            if (hex.getExitType(j) == Constants.BLOCK)
            {
                // Only this path is allowed.
                block = j;
            }
        }
        return block;
    }

    /** Return set of hexLabels describing where this legion can move
     *  without teleporting. */
    public Set listNormalMoves(LegionInfo legion, MasterHex hex, 
        int movementRoll)
    {
        return listNormalMoves(legion, hex, movementRoll, false);
    }
    
    /** Return set of hexLabels describing where this legion can move
     *  without teleporting. */
    public Set listNormalMoves(LegionInfo legion, MasterHex hex, 
        int movementRoll, boolean inAdvance)
    {
        if (legion.hasMoved() && (!inAdvance))
        {
            return new HashSet();
        }

        Set tuples = findNormalMoves(hex, legion, movementRoll, 
            findBlock(hex), Constants.NOWHERE);

        // Extract just the hexLabels from the hexLabel:entrySide tuples.
        Set hexLabels = new HashSet();
        Iterator it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = (String)it.next();
            java.util.List parts = Split.split(':', tuple);
            String hexLabel = (String)parts.get(0);
            hexLabels.add(hexLabel);
        }
        return hexLabels;
    }

    private boolean towerTeleportAllowed()
    {
        if (client.getOption(Options.noTowerTeleport))
        {
            return false;
        }
        if (client.getTurnNumber() == 1 &&
            client.getOption(Options.noFirstTurnTeleport))
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
        if (client.getTurnNumber() == 1 && 
            client.getOption(Options.noFirstTurnT2TTeleport))
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
        if (client.getOption(Options.towerToTowerTeleportOnly))
        {
            return false;
        }
        return true;
    }

    
    private boolean titanTeleportAllowed()
    {
        if (client.getOption(Options.noTitanTeleport))
        {
            return false;
        }
        if (client.getTurnNumber() == 1 && 
            client.getOption(Options.noFirstTurnTeleport))
        {
            return false;
        }
        return true;
    }

    /** Return set of hexLabels describing where this legion can teleport. */
    Set listTeleportMoves(LegionInfo legion, MasterHex hex, int movementRoll)
    {
        return listTeleportMoves(legion, hex, movementRoll, false);
    }
        
    /** Return set of hexLabels describing where this legion can teleport. */
    Set listTeleportMoves(LegionInfo legion, MasterHex hex, int movementRoll,
                          boolean inAdvance)
    {
        PlayerInfo player = legion.getPlayerInfo();

        Set set = new HashSet();
        if ((!inAdvance) &&
            (movementRoll != 6 || legion.hasMoved() || player.hasTeleported()))
        {
            return set;
        }

        // Tower teleport
        if (HexMap.terrainIsTower(hex.getTerrain()) && legion.numLords() > 0 &&
            towerTeleportAllowed())
        {
            // Mark every unoccupied hex within 6 hexes.
            if (towerToNonTowerTeleportAllowed())
            {
                set.addAll(findNearbyUnoccupiedHexes(hex, legion, 6,
                    Constants.NOWHERE));
            }

            if (towerToTowerTeleportAllowed())
            {
                // Mark every unoccupied tower.
                Set towerSet = MasterBoard.getTowerSet();
                Iterator it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    if (MasterBoard.getHexByLabel(hexLabel) != null)
                    {
                        if (!client.isOccupied(hexLabel) 
                            && (!(hexLabel.equals(hex.getLabel()))))
                        {
                            set.add(hexLabel);
                        }
                    }
                }
            }
            else
            {
                // Remove nearby towers from set.
                Set towerSet = MasterBoard.getTowerSet();
                Iterator it = towerSet.iterator();
                while (it.hasNext())
                {
                    String hexLabel = (String)it.next();
                    set.remove(hexLabel);
                }
            }
        }

        // Titan teleport
        if (player.canTitanTeleport() && legion.hasTitan() &&
            titanTeleportAllowed())
        {
            // Mark every hex containing an enemy stack that does not
            // already contain a friendly stack.
            Iterator it = client.getEnemyLegions(player.getName()).iterator();
            while (it.hasNext())
            {
                String markerId = (String)it.next();
                LegionInfo other = client.getLegionInfo(markerId);
                {
                    String hexLabel = other.getHexLabel();
                    if (!client.isEngagement(hexLabel))
                    {
                        set.add(hexLabel);
                    }
                }
            }
        }
        return set;
    }


    /** Return a Set of Strings "Left" "Right" or "Bottom" describing
     *  possible entry sides.  If the hex is unoccupied, just return 
     *  one entry side since it doesn't matter. */
    Set listPossibleEntrySides(String markerId, String targetHexLabel,
        boolean teleport)
    {
        Set entrySides = new HashSet();
        LegionInfo legion = client.getLegionInfo(markerId);
        int movementRoll = client.getMovementRoll();
        MasterHex currentHex = MasterBoard.getHexByLabel(legion.getHexLabel());
        MasterHex targetHex = MasterBoard.getHexByLabel(targetHexLabel);

        if (teleport)
        {
            if (listTeleportMoves(legion, currentHex, movementRoll).
                contains(targetHexLabel))
            {
                // Startlisted terrain only have bottom entry side.
                // Don't bother finding more than one entry side if unoccupied.
                if (!client.isOccupied(targetHexLabel) ||
                    HexMap.terrainHasStartlist(targetHex.getTerrain()))
                    
                {
                    entrySides.add(Constants.bottom);  
                    return entrySides;
                }
                else
                {
                    entrySides.add(Constants.bottom);
                    entrySides.add(Constants.left);  
                    entrySides.add(Constants.right);  
                    return entrySides;
                }
            }
            else
            {
                return entrySides;
            }
        }

        // Normal moves.
        Set tuples = findNormalMoves(currentHex, legion, movementRoll, 
            findBlock(currentHex), Constants.NOWHERE);
        Iterator it = tuples.iterator();
        while (it.hasNext())
        {
            String tuple = (String)it.next();
            java.util.List parts = Split.split(':', tuple);
            String hl = (String)parts.get(0);
            if (hl.equals(targetHexLabel))
            {
                String buf = (String)parts.get(1);
                entrySides.add(buf);
                // Don't bother finding more than one entry side if unoccupied.
                if (!client.isOccupied(targetHexLabel))
                {
                    return entrySides;
                }
            }
        }
        return entrySides;
    }
}
