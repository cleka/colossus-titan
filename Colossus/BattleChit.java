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


    void strike(BattleChit target)
    {
        Hex targetHex = target.getCurrentHex();

        boolean carryPossible = true;
        if (numEngaged() < 2)
        {
            carryPossible = false;
        }

        int dice = getPower();

        boolean rangestrike = !isEngaged();
        if (rangestrike)
        {
            carryPossible = false;
            dice /= 2;

            // Dragon rangestriking from volcano: +2
            if (creature == Creature.dragon && currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // Dragon striking from volcano: +2
            if (creature == Creature.dragon && currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }

            if (currentHex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(currentHex, targetHex, false);
                char hexside = currentHex.getHexside(direction);
                // Native striking down a dune hexside: +2
                if (hexside == 'd' && creature.isNativeSandDune())
                {
                    dice += 2;
                }
                // Native striking down a slope hexside: +1
                else if (hexside == 's' && creature.isNativeSlope())
                {
                    dice++;
                }
            }
            else if (targetHex.getElevation() > currentHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(targetHex, currentHex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up a dune hexside: -1
                if (hexside == 'd' && !creature.isNativeSandDune())
                {
                    dice--;
                }
            }
        }

        if (dice <= target.getPower() - target.getHits())
        {
            carryPossible = false;
        }

        int attackerSkill = creature.getSkill();
        int defenderSkill = target.getCreature().getSkill();

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (currentHex.getTerrain() == 'r' && !creature.isNativeBramble())
            {
                attackerSkill--;
            }
        
            if (currentHex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(currentHex, targetHex, false);
                char hexside = currentHex.getHexside(direction);
                // Striking down across wall: +1
                if (hexside == 'w') 
                {
                    attackerSkill++;
                }
            }
            else if (currentHex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(targetHex, currentHex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !creature.isNativeSlope()) ||
                    hexside == 'w')
                {
                    attackerSkill--;
                }
            }

        }
        else if (creature != Creature.warlock)
        {
            // Range penalty
            if (map.getRange(currentHex, targetHex) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!creature.isNativeBramble())
            {
                attackerSkill -= map.countBrambleHexes(currentHex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            boolean wall = false;
            for (int i = 0; i < 6; i++) 
            {
                if (targetHex.getHexside(i) == 'w')
                {
                    wall = true;
                }
            }
            if (wall)
            {
                if (targetHex.getElevation() > currentHex.getElevation())
                {
                    attackerSkill -= (targetHex.getElevation() -
                        currentHex.getElevation());
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain() == 'v')
            {
                attackerSkill--;
            }
        }
        

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1
        if (targetHex.getTerrain() == 'r' && 
            target.getCreature().isNativeBramble() && 
            !creature.isNativeBramble() &&
            !(rangestrike && creature == Creature.warlock))
        {
            strikeNumber++;
        }

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
        if (carryPossible && carry > 0)
        {
            System.out.println(carry + " possible carries");
        }

        // Record that this attacker has struck.
        struck = true;
    }
}
