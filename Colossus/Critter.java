import javax.swing.*;
import java.util.*;

/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

public class Critter extends Creature
{
    private boolean visible;
    private Creature creature;
    private Legion legion;

    private BattleMap map;
    private Battle battle;
    private boolean moved;
    private boolean struck;

    private BattleHex currentHex;
    private BattleHex startingHex;

    /** Damage taken */
    private int hits;

    /** Mark whether this critter is a legal carry target. */
    private boolean carryFlag;

    private BattleChit chit;


    public Critter(Creature creature, boolean visible, Legion legion)
    {
        super(creature.name, creature.power, creature.skill,
            creature.rangestrikes, creature.flies, creature.nativeBramble,
            creature.nativeDrift, creature.nativeBog,
            creature.nativeSandDune, creature.nativeSlope, creature.lord,
            creature.demilord, creature.count, creature.pluralName);

        if (name != null)
        {
            this.creature = Creature.getCreatureFromName(name);
        }
        else
        {
            this.creature = null;
        }

        this.visible = visible;
        this.legion = legion;
        if (name.equals("Titan"))
        {
            setPower(getPlayer().getTitanPower());
        }
    }


    public void addBattleInfo(BattleHex hex, BattleMap map, BattleChit chit,
        Battle battle)
    {
        this.currentHex = hex;
        this.startingHex = hex;
        this.map = map;
        this.chit = chit;
        this.battle = battle;
    }


    public boolean isVisible()
    {
        return visible;
    }


    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }


    public Creature getCreature()
    {
        return creature;
    }


    public Legion getLegion()
    {
        return legion;
    }


    public BattleChit getChit()
    {
        return chit;
    }


    // All count-related functions must use the Creature archetype,
    // not this copy.

    public int getCount()
    {
        return creature.getCount();
    }


    public void setCount(int count)
    {
        creature.setCount(count);
    }


    public void takeOne()
    {
        creature.takeOne();
    }


    public void putOneBack()
    {
        creature.putOneBack();
    }


    /** Return only the base part of the image name for this critter. */
    public String getImageName(boolean inverted)
    {
        StringBuffer basename = new StringBuffer();
        if (inverted)
        {
            basename.append(Chit.invertedPrefix);
        }

        basename.append(name);

        if (name.equals("Titan") && getPower() >= 6 && getPower() <= 20)
        {
            // Use Titan14.gif for a 14-power titan, etc.  Use the generic
            // Titan.gif (with X-4) for ridiculously big titans, to avoid
            // the need for an infinite number of images.
            basename.append(getPower());
        }

        return basename.toString();
    }


    public int getPower()
    {
        // Update Titan power if necessary.
        if (name.equals("Titan"))
        {
            setPower(getPlayer().getTitanPower());
        }

        return power;
    }


    public int getPointValue()
    {
        return getPower() * skill;
    }


    public int getHits()
    {
        return hits;
    }


    public void heal()
    {
        hits = 0;
    }


    /** Apply damage to this critter.  Return the amount of excess damage
     *  done, which may sometimes carry to another target. */
    public int wound(int damage)
    {
        int excess = 0;

        if (damage > 0)
        {
            hits += damage;
            if (hits > power)
            {
                excess = hits - power;
                hits = power;
            }

            // Check for death.
            if (hits >= getPower())
            {
                setDead(true);
            }

            // Update damage displayed on chit.
            // Chit.repaint() doesn't work right after a creature is killed
            // by carry damage, so paint the whole hex.
            getCurrentHex().repaint();
        }

        return excess;
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


    /** Dead critters count as being in contact only if countDead is true. */
    public int numInContact(boolean countDead)
    {
        // Offboard creatures are not in contact.
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
                        Critter other = hex.getCritter();
                        if (other.getPlayer() != getPlayer() &&
                            (countDead || !other.isDead()))
                        {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }


    /** Dead critters count as being in contact only if countDead is true. */
    public boolean isInContact(boolean countDead)
    {
        return (numInContact(countDead) > 0);
    }


    public void moveToHex(BattleHex hex)
    {
        Game.logEvent(name + " moves from " + currentHex.getLabel() +
            " to " + hex.getLabel());

        currentHex.removeCritter(this);
        currentHex = hex;
        currentHex.addCritter(this);
        moved = true;
        battle.setLastCritterMoved(this);
        map.repaint();
    }


    public void undoMove()
    {
        currentHex.removeCritter(this);
        currentHex = startingHex;
        currentHex.addCritter(this);
        moved = false;
        battle.clearLastCritterMoved();
        Game.logEvent(name + " undoes move and returns to " +
            startingHex.getLabel());
        map.repaint();
    }


    // Return the number of dice that will be rolled when striking this
    // target, including modifications for terrain.
    public int getDice(Critter target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !isInContact(true);
        if (rangestrike)
        {
            dice /= 2;

            // Dragon rangestriking from volcano: +2
            if (name.equals("Dragon") &&
                currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // Dragon striking from volcano: +2
            if (name.equals("Dragon") && currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = battle.getDirection(currentHex, targetHex, false);
            char hexside = currentHex.getHexside(direction);
            char oppHexside = currentHex.getOppositeHexside(direction);

            // Native striking down a dune hexside: +2
            if (hexside == 'd' && isNativeSandDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hexside == 's' && isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (oppHexside == 'd' && !isNativeSandDune())
            {
                dice--;
            }
        }

        return dice;
    }


    private int getAttackerSkill(Critter target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = getSkill();

        boolean rangestrike = !isInContact(true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (currentHex.getTerrain() == 'r' && !isNativeBramble())
            {
                attackerSkill--;
            }

            if (currentHex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = battle.getDirection(currentHex, targetHex,
                    false);
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
                int direction = battle.getDirection(targetHex, currentHex,
                    false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !isNativeSlope()) ||
                    hexside == 'w')
                {
                    attackerSkill--;
                }
            }

        }
        else if (!name.equals("Warlock"))
        {
            // Range penalty
            if (battle.getRange(currentHex, targetHex) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!isNativeBramble())
            {
                attackerSkill -= battle.countBrambleHexes(currentHex,
                    targetHex);
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


    private int getStrikeNumber(Critter target)
    {
        BattleHex targetHex = target.getCurrentHex();
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1
        if (targetHex.getTerrain() == 'r' &&
            target.isNativeBramble() &&
            !isNativeBramble() &&
            !(rangestrike && name.equals("Warlock")))
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


    /** Allow the player to choose whether to take a penalty
     *  (fewer dice or higher strike number) in order to be
     *  allowed to carry.  Return true if the penalty is taken,
     *  or false if it is not. */
    private boolean chooseStrikePenalty(Collection carryTargets)
    {
        StringBuffer prompt = new StringBuffer(
            "Take strike penalty to allow carrying to ");

        Iterator it = carryTargets.iterator();
        while (it.hasNext())
        {
            Critter carryTarget = (Critter)it.next();
            BattleHex targetHex = carryTarget.getCurrentHex();
            prompt.append(carryTarget.getName() + " in " +
                targetHex.getDescription());
            if (it.hasNext())
            {
                prompt.append(", ");
            }
        }
        prompt.append("?");

        String [] options = new String[2];
        options[0] = "Take Penalty";
        options[1] = "Do Not Take Penalty";
        int answer = JOptionPane.showOptionDialog(map, prompt.toString(),
            "Take Strike Penalty?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return (answer == JOptionPane.YES_OPTION);
    }


    /** Calculate number of dice and strike number needed to hit target,
     *  and whether any carries are possible.  Roll the dice and apply
     *  damage.  Highlight legal carry targets. */
    public void strike(Critter target)
    {
        // Sanity check
        if (target.getPlayer() == getPlayer())
        {
            System.out.println(name + " tried to strike allied " +
                target.getName());
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

            ArrayList penaltyOptions = new ArrayList();

            for (int i = 0; i < 6; i++)
            {
                // Adjacent creatures separated by a cliff are not in contact.
                if (currentHex.getHexside(i) != 'c' &&
                    currentHex.getOppositeHexside(i) != 'c')
                {
                    BattleHex hex = currentHex.getNeighbor(i);
                    if (hex != null && hex != targetHex && hex.isOccupied())
                    {
                        Critter critter = hex.getCritter();
                        if (critter.getPlayer() != getPlayer() &&
                            !critter.isDead())
                        {
                            int tmpDice = getDice(critter);
                            int tmpStrikeNumber = getStrikeNumber(critter);

                            // Strikes not up across dune hexsides cannot
                            // carry up across dune hexsides.
                            if (currentHex.getOppositeHexside(i) == 'd')
                            {
                                int direction = battle.getDirection(targetHex,
                                    currentHex, false);
                                if (targetHex.getHexside(direction) != 'd')
                                {
                                    critter.setCarryFlag(false);
                                }
                            }

                            else if (tmpStrikeNumber > strikeNumber ||
                                tmpDice < dice)
                            {
                                // Add this scenario to the list.
                                penaltyOptions.add(new PenaltyOption(critter,
                                    tmpDice, tmpStrikeNumber));
                            }

                            else
                            {
                                critter.setCarryFlag(true);
                                numCarryTargets++;
                            }
                        }
                    }
                }
            }

            // Sort penalty options by number of dice (ascending), then by
            //    strike number (descending).
            Collections.sort(penaltyOptions);

            // Find the group of PenaltyOptions with identical dice and
            //    strike numbers.
            PenaltyOption option;
            ArrayList critters = new ArrayList();
            ListIterator lit = penaltyOptions.listIterator();
            while (lit.hasNext())
            {
                option = (PenaltyOption)lit.next();
                int tmpDice = option.getDice();
                int tmpStrikeNumber = option.getStrikeNumber();
                critters.clear();
                critters.add(option.getCritter());

                while (lit.hasNext())
                {
                    option = (PenaltyOption)lit.next();
                    if (option.getDice() == tmpDice &&
                        option.getStrikeNumber() == tmpStrikeNumber)
                    {
                        critters.add(option.getCritter());
                    }
                    else
                    {
                        lit.previous();
                        break;
                    }
                }

                // Make sure the penalty is still relevant.
                if (tmpStrikeNumber > strikeNumber || tmpDice < dice)
                {
                    if (chooseStrikePenalty(critters))
                    {
                        if (tmpStrikeNumber > strikeNumber)
                        {
                            strikeNumber = tmpStrikeNumber;
                        }
                        if (tmpDice < dice)
                        {
                            dice = tmpDice;
                        }
                        Iterator it2 = critters.iterator();
                        while (it2.hasNext())
                        {
                            Critter critter = (Critter)it2.next();
                            critter.setCarryFlag(true);
                            numCarryTargets++;
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

        int [] rolls = new int[dice];
        StringBuffer rollString = new StringBuffer(36);
        for (int i = 0; i < dice; i++)
        {
            rolls[i] = Game.rollDie();
            rollString.append(rolls[i]);

            if (rolls[i] >= strikeNumber)
            {
                damage++;
            }
        }

        int carryDamage = target.wound(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            battle.setCarryDamage(carryDamage);
            battle.highlightCarries();
        }

        // Record that this attacker has struck.
        struck = true;

        // Display the rolls in the showDice dialog.
        ShowDice showDice = battle.getShowDice();
        showDice.setAttacker(this);
        showDice.setDefender(target);
        showDice.setTargetNumber(strikeNumber);
        showDice.setRolls(rolls);
        showDice.setHits(damage);
        showDice.setCarries(carryDamage);
        showDice.setup();

        Game.logEvent(name + " in " + currentHex.getLabel() +
            " strikes " + target.getName() + " in " +
            targetHex.getLabel() + " with strike number " +
            strikeNumber + " : " + rollString + ": " + damage +
            (damage == 1 ? " hit" : " hits"));
    }


    public boolean getCarryFlag()
    {
        return carryFlag;
    }


    public void setCarryFlag(boolean flag)
    {
        carryFlag = flag;
    }


    public boolean isDead()
    {
        return (getHits() >= getPower());
    }


    public void setDead(boolean dead)
    {
        if (dead)
        {
            hits = getPower();
        }
        chit.setDead(dead);
    }


    public Player getPlayer()
    {
        return legion.getPlayer();
    }
}
