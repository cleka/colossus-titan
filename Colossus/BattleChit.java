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
    private Legion legion;
    private Creature creature;
    private BattleMap map;
    private boolean moved = false;
    private boolean struck = false;

    private Hex currentHex;
    private Hex startingHex;

    // Damage taken
    private int hits = 0;


    BattleChit(int cx, int cy, int scale, String imageFilename,
        Container container, Creature creature, Hex hex, Legion legion,
        boolean inverted, BattleMap map)
    {
        super(cx, cy, scale, imageFilename, container, inverted);
        this.creature = creature;
        this.currentHex = hex;
        this.startingHex = hex;
        this.map = map;
        this.legion = legion;
    }


    public Creature getCreature()
    {
        return creature;
    }


    public int getHits()
    {
        return hits;
    }


    void setHits(int damage)
    {
        hits = damage;
    }


    void checkForDeath()
    {
        if (hits >= creature.getPower())
        {
            setDead(true);
            currentHex.repaint();
        }
    }


    public int getPower()
    {
        if (creature == Creature.titan)
        {
            return legion.getPlayer().getTitanPower();
        }
        else
        {
            return creature.getPower();
        }
    }


    Legion getLegion()
    {
        return legion;
    }


    Player getPlayer()
    {
        return legion.getPlayer();
    }


    boolean hasMoved()
    {
        return moved;
    }


    void commitMove()
    {
        startingHex = currentHex;
        moved = false;
    }


    boolean hasStruck()
    {
        return struck;
    }


    void commitStrike()
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
                        if (chit.getPlayer() != getPlayer() && !chit.isDead())
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


    // XXX: Need to deal with carries.
    void strike(BattleChit target)
    {
        int dice = getPower();

        // XXX: This can be modified by terrain.
        // Native striking down a dune hexside: +2
        // Native striking down a slope hexside: +1
        // Non-native striking up a dune hexside: -1
        // Dragon striking or rangestriking from volcano: +2

        Hex targetHex = target.getCurrentHex();

        boolean rangestrike = !isEngaged();
        if (rangestrike)
        {
            dice /= 2;
            // Dragon rangestriking from volcano: +2
            if (creature == Creature.dragon && currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // XXX: Add these.
            // Native striking down a dune hexside: +2
            // Native striking down a slope hexside: +1
            // Non-native striking up a dune hexside: -1
            // Dragon striking from volcano: +2

        }

        int attackerSkill = creature.getSkill();
        int defenderSkill = target.getCreature().getSkill();

        // XXX: Skill can be modified by terrain.
        // Non-native striking out of bramble: -1
        // Non-native striking up slope: -1
        // Down across wall: +1
        // Up across wall: -1
        // Non-native non-warlock rangestrikes: -1 per intervening bramble hex
        // Non-warlock rangestrike into volcano: -1
        // Non-warlock rangestrike up across wall: -1 per wall

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // XXX: strikeNumber can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        // Roll the dice.
        int damage = 0;

        System.out.print("Rolling " + dice + " dice needing " + strikeNumber
            + " to hit:   ");
        for (int i = 0; i < dice; i++)
        {
            int roll = (int) Math.ceil(6 * Math.random());

            // XXX: Display the rolls?
            System.out.print(roll);

            if (roll >= strikeNumber)
            {
                damage++;
            }
        }
        System.out.println("   : " + damage + " hits");

        int totalDamage = target.getHits();
        totalDamage += damage;
        int carry = 0;
        int power = target.getPower();
        if (totalDamage > power)
        {
            carry = totalDamage - power;
            totalDamage = power;
        }
        target.setHits(totalDamage);
        target.checkForDeath();

        // XXX: Let the attacker choose whether to carry, if applicable.

        // Record that this attacker has struck.
        struck = true;
    }
}
