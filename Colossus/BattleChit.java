import java.awt.*;
import java.util.*;

/**
 * Class BattleChit implements the GUI for a Titan chit representing
 * a creature on a BattleMap.
 * @version $Id$
 * @author David Ripton
 */

class BattleChit extends Chit
{
    private Player player;
    private Creature creature;
    private BattleMap map;
    private boolean moved = false;
    private boolean struck = false;

    private Hex currentHex;
    private Hex startingHex;

    // Damage taken
    private int hits = 0;


    BattleChit(int cx, int cy, int scale, String imageFilename,
        Container container, Creature creature, Hex hex, Player player,
        boolean inverted, BattleMap map)
    {
        super(cx, cy, scale, imageFilename, container, inverted);
        this.creature = creature;
        this.currentHex = hex;
        this.startingHex = hex;
        this.player = player;
        this.map = map;
    }


    public Creature getCreature()
    {
        return creature;
    }


    public int getHits()
    {
        return hits;
    }


    void checkForDeath()
    {
        if (hits >= creature.getPower())
        {
            setDead(true);
        }
    }


    public int getPower()
    {
        if (creature == Creature.titan)
        {
            return player.getTitanPower();
        }
        else
        {
            return creature.getPower();
        }
    }


    Player getPlayer()
    {
        return player;
    }


    boolean hasMoved()
    {
        return moved;
    }


    void commitMove()
    {
        moved = false;
    }


    boolean hasStruck()
    {
        return struck;
    }


    void commitStrikes()
    {
        struck = false;
    }


    Hex getCurrentHex()
    {
        return currentHex;
    }


    Hex getStartingHex()
    {
        return startingHex;
    }


    int numEngaged()
    {
        int count = 0;

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not engaged.
            if (currentHex.getHexside(i) != 'c')
            {
                Hex hex = currentHex.getNeighbor(i);
                if (hex != null)
                {
                    if (hex.isOccupied())
                    {
                        BattleChit chit = hex.getChit();
                        if (chit.getPlayer() != player)
                        {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    boolean isEngaged()
    {
        return (numEngaged() > 0);
    }


    void moveToHex(Hex hex)
    {
        currentHex.removeChit(this);
        currentHex = hex;
        currentHex.addChit(this);
        moved = true;
        map.markLastChitMoved(this);
        map.repaint();
    }


    void undoMove()
    {
        currentHex.removeChit(this);
        currentHex = startingHex;
        currentHex.addChit(this);
        moved = false;
        map.clearLastChitMoved();
        map.repaint();
    }
}
