package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.server.Creature;


/**
 * Class BattleMovement does client-side battle move calculations.
 *
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

// XXX Massively duplicated code.  Merge later.


final class BattleMovement
{
    private Client client;


    BattleMovement(Client client)
    {
        this.client = client;
    }


    /** Recursively find moves from this hex.  Return an array of hex IDs for
     *  all legal destinations.  Do not double back.  */
    private Set findMoves(BattleHex hex, Creature creature, boolean flies,
        int movesLeft, int cameFrom)
    {
        Set set = new HashSet();
        for (int i = 0; i < 6; i++)
        {
            // Do not double back.
            if (i != cameFrom)
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    int reverseDir = (i + 3) % 6;
                    int entryCost;

                    BattleChit bogey = client.getBattleChit(
                        neighbor.getLabel());
                    if (bogey == null)
                    {
                        entryCost = neighbor.getEntryCost(creature, 
                            reverseDir);
                    }
                    else
                    {
                        entryCost = BattleHex.IMPASSIBLE_COST;
                    }

                    if (entryCost <= movesLeft)
                    {
                        // Mark that hex as a legal move.
                        set.add(neighbor.getLabel());

                        // If there are movement points remaining, continue
                        // checking moves from there.  Fliers skip this
                        // because flying is more efficient.
                        if (!flies && movesLeft > entryCost)
                        {
                            set.addAll(findMoves(neighbor, creature, flies,
                                movesLeft - entryCost, reverseDir));
                        }
                    }

                    // Fliers can fly over any hex for 1 movement point,
                    // but some Hex cannot be flown over by some creatures.
                    if (flies &&
                        movesLeft > 1 &&
                        neighbor.canBeFlownOverBy(creature))
                    {
                        set.addAll(findMoves(neighbor, creature, flies,
                            movesLeft - 1, reverseDir));
                    }
                }
            }
        }
        return set;
    }

    /** This method is called by the defender on turn 1 in the tower.
     *  So we know that there are no enemies on board, and all allies
     *  are mobile. */
    private Set findUnoccupiedTowerHexes()
    {
        char t = client.getBattleTerrain();
        Set set = new HashSet();
        Iterator it = HexMap.getTowerStartList(t).iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            BattleHex hex = HexMap.getHexByLabel(t, hexLabel);
            if (!isOccupied(hexLabel))
            {
                set.add(hex.getLabel());
            }
        }
        return set;
    }

    private boolean isOccupied(String hexLabel)
    {
        return !client.getBattleChits(hexLabel).isEmpty();
    }


    Set showMoves(int tag)
    {
        BattleChit chit = client.getBattleChit(tag);
        return showMoves(chit);
    }

    /** Find all legal moves for this critter. The returned list
     *  contains hex IDs, not hexes. */
    Set showMoves(BattleChit chit)
    {
        Set set = new HashSet();
        if (!chit.hasMoved() && !client.isInContact(chit, false))
        {
            if (HexMap.terrainIsTower(client.getBattleTerrain()) && (
                client.getBattleTurnNumber() == 1) &&
                client.getBattleActiveMarkerId().equals(
                    client.getDefenderMarkerId()))
            {
                set = findUnoccupiedTowerHexes();
            }
            else
            {
                Creature creature = Creature.getCreatureByName(
                    chit.getCreatureName());
                BattleHex hex = HexMap.getHexByLabel(
                    client.getBattleTerrain(), chit.getHexLabel());
                set = findMoves(hex, creature, creature.isFlier(), 
                    creature.getSkill(), -1);
            }
        }
        return set;
    }
}
