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
    private boolean moved = false;

    private Hex currentHex;
    private Hex startingHex;

    // Damage taken
    private int hits = 0;


    BattleChit(int cx, int cy, int scale, String imageFilename,
        Container container, Creature creature, Hex hex, Player player)
    {
        super(cx, cy, scale, imageFilename, container);
        this.creature = creature;
        this.currentHex = hex;
        this.startingHex = hex;
        this.player = player;
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


    Hex getCurrentHex()
    {
        return currentHex;
    }
    
    
    Hex getStartingHex()
    {
        return startingHex;
    }


    void moveToHex(Hex hex)
    {
        currentHex.removeChit(this);
        currentHex = hex;
        currentHex.addChit(this);
        moved = true;
        // legion.markLastChitMoved(this);
    }
}
