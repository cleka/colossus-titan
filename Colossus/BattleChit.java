import java.awt.*;
import java.util.*;
import javax.swing.*;

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

    private BattleHex currentHex;
    private BattleHex startingHex;

    // Damage taken
    private int hits = 0;

    // Mark whether this chit is a legal carry target.
    private boolean carryFlag = false;


    BattleChit(int cx, int cy, int scale, String imageFilename,
        Container container, Creature creature, BattleHex hex,
        Legion legion, BattleMap map)
    {
        super(cx, cy, scale, imageFilename, container);
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


    public void setHits(int damage)
    {
        hits = damage;
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


    public void checkForDeath()
    {
        if (hits >= getPower())
        {
            setDead(true);
            currentHex.repaint();
        }
    }


    public Legion getLegion()
    {
        return legion;
    }


    public Player getPlayer()
    {
        return legion.getPlayer();
    }


    public boolean hasMoved()
    {
        return moved;
    }


    public void commitMove()
    {
        startingHex = currentHex;
        moved = false;
    }


    public boolean hasStruck()
    {
        return struck;
    }


    public void commitStrike()
    {
        struck = false;
    }


    public BattleHex getCurrentHex()
    {
        return currentHex;
    }


    public BattleHex getStartingHex()
    {
        return startingHex;
    }


    // Dead chits count as chits in contact only if countDead is true.
    public int numInContact(boolean countDead)
    {
        // Offboard chits are not in contact.
        if (currentHex.isEntrance())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (currentHex.getHexside(i) != 'c' &&
                currentHex.getOppositeHexside(i) != 'c')
            {
                BattleHex hex = currentHex.getNeighbor(i);
                if (hex != null)
                {
                    if (hex.isOccupied())
                    {
                        BattleChit chit = hex.getChit();
                        if (chit.getPlayer() != getPlayer() &&
                            (countDead || !chit.isDead()))
                        {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    // Dead chits count as chits in contact only if countDead is true.
    public boolean inContact(boolean countDead)
    {
        return (numInContact(countDead) > 0);
    }


    public void moveToHex(BattleHex hex)
    {
        currentHex.removeChit(this);
        currentHex = hex;
        currentHex.addChit(this);
        moved = true;
        map.markLastChitMoved(this);
        map.repaint();
    }


    public void undoMove()
    {
        currentHex.removeChit(this);
        currentHex = startingHex;
        currentHex.addChit(this);
        moved = false;
        map.clearLastChitMoved();
        map.repaint();
    }


    // Return the number of dice that will be rolled when striking this
    // target, including modifications for terrain.
    public int getDice(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !inContact(true);
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
            // Dice can be modified by terrain.
            // Dragon striking from volcano: +2
            if (creature == Creature.dragon && currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = map.getDirection(currentHex, targetHex, false);
            char hexside = currentHex.getHexside(direction);
            char oppHexside = currentHex.getOppositeHexside(direction);

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
            // Non-native striking up a dune hexside: -1
            else if (oppHexside == 'd' && !creature.isNativeSandDune())
            {
                dice--;
            }
        }

        return dice;
    }


    public int getAttackerSkill(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = creature.getSkill();

        boolean rangestrike = !inContact(true);

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

        return attackerSkill;
    }


    public int getStrikeNumber(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();
        boolean rangestrike = !inContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getCreature().getSkill();

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

        return strikeNumber;
    }


    // Allow the player to choose whether to take a penalty
    // (fewer dice or higher strike number) in order to be
    // allowed to carry.  Return true if the penalty is taken,
    // or false if it is not.
    private boolean chooseStrikePenalty(BattleChit carryTarget)
    {
        String yesString = "Take Penalty";
        String noString = "Do Not Take Penalty";
        String promptString = "Take strike penalty to allow carrying to " +
            carryTarget.getCreature().getName() + " in " +
            carryTarget.getCurrentHex().getTerrainName().toLowerCase() + "?";

        Object[] options = {yesString, noString};
        int optval = JOptionPane.showOptionDialog(map, promptString,
            "Take Strike Penalty?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, noString);

        return (optval == JOptionPane.YES_OPTION);
    }


    // Calculate number of dice and strike number needed to hit target,
    // and whether any carries are possible.  Roll the dice and apply
    // damage.  Highlight legal carry targets.
    public void strike(BattleChit target)
    {
        // sanity check
        if (target.getLegion().getPlayer() == legion.getPlayer())
        {
            System.out.println("tried to strike own creature!");
            return;
        }

        BattleHex targetHex = target.getCurrentHex();

        boolean carryPossible = true;
        if (numInContact(false) < 2)
        {
            carryPossible = false;
        }

        int dice = getDice(target);

        // Carries are only possible if the striker is rolling more dice than
        // the target has hits remaining.
        if (dice <= target.getPower() - target.getHits())
        {
            carryPossible = false;
        }

        int strikeNumber = getStrikeNumber(target);

        // Figure whether number of dice or strike number needs to be
        // penalized in order to carry.
        if (carryPossible)
        {
            // Count legal carry targets.
            int numCarryTargets = 0;

            for (int i = 0; i < 6; i++)
            {
                // Adjacent creatures separated by a cliff are not in contact.
                if (currentHex.getHexside(i) != 'c' &&
                    currentHex.getOppositeHexside(i) != 'c')
                {
                    BattleHex hex = currentHex.getNeighbor(i);
                    if (hex != null && hex != targetHex && hex.isOccupied())
                    {
                        BattleChit chit = hex.getChit();
                        if (chit.getPlayer() != getPlayer() && !chit.isDead())
                        {
                            int tmpDice = getDice(chit);
                            int tmpStrikeNumber = getStrikeNumber(chit);

                            // Strikes not up across dune hexsides cannot
                            // carry up across dune hexsides.
                            if (currentHex.getOppositeHexside(i) == 'd')
                            {
                                int direction = map.getDirection(targetHex,
                                    currentHex, false);
                                if (targetHex.getHexside(direction) != 'd')
                                {
                                    chit.setCarryFlag(false);
                                }
                            }

                            else if (tmpStrikeNumber > strikeNumber ||
                                tmpDice < dice)
                            {
                                // Allow choosing a less effective strike in
                                // order to possibly carry.
                                if (chooseStrikePenalty(chit))
                                {
                                    strikeNumber = tmpStrikeNumber;
                                    dice = tmpDice;
                                    chit.setCarryFlag(true);
                                    numCarryTargets++;
                                }
                                else
                                {
                                    chit.setCarryFlag(false);
                                }
                            }

                            else
                            {
                                chit.setCarryFlag(true);
                                numCarryTargets++;
                            }
                        }
                    }
                }
            }

            if (numCarryTargets == 0)
            {
                carryPossible = false;
            }
        }


        // Roll the dice.
        int damage = 0;

        System.out.print("Rolling " + dice + " dice needing " + strikeNumber
            + " to hit:   ");
        for (int i = 0; i < dice; i++)
        {
            int roll = (int) Math.ceil(6 * Math.random());

            // XXX: Display the rolls somehow.
            System.out.print(roll);

            if (roll >= strikeNumber)
            {
                damage++;
            }
        }
        System.out.println("   : " + damage + " hits");

        int totalDamage = target.getHits();
        totalDamage += damage;
        int carryDamage = 0;
        int power = target.getPower();
        if (totalDamage > power)
        {
            carryDamage = totalDamage - power;
            totalDamage = power;
        }
        target.setHits(totalDamage);
        target.checkForDeath();
        target.repaint();

        // Let the attacker choose whether to carry, if applicable.
        if (carryPossible && carryDamage > 0)
        {
            System.out.println(carryDamage + " possible carries");
            map.highlightCarries(this, carryDamage);
        }

        // Record that this attacker has struck.
        struck = true;
    }


    public boolean getCarryFlag()
    {
        return carryFlag;
    }


    public void setCarryFlag(boolean flag)
    {
        carryFlag = flag;
    }


    public void paint(Graphics g)
    {
        super.paint(g);

        if (hits > 0 && !isDead())
        {
            String sHits = Integer.toString(hits);
            Rectangle rect = getBounds();

            // Construct a font 3 times the size of the current font.
            Font oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            Font font = new Font(name, style, 3 * size);
            g.setFont(font);

            FontMetrics fontMetrics = g.getFontMetrics();
            int fontHeight = fontMetrics.getAscent();
            int fontWidth = fontMetrics.stringWidth(sHits);

            // Provide a high-contrast background for the number.
            g.setColor(java.awt.Color.white);
            g.fillRect(rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height - fontHeight) / 2,
                fontWidth, fontHeight);

            // Show number of hits taken in red.
            g.setColor(java.awt.Color.red);

            g.drawString(sHits, rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height + fontHeight) / 2);

            // Restore the font.
            g.setFont(oldFont);
        }
    }
}
