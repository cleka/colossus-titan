import javax.swing.*;
import java.util.*;

/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

public final class Critter extends Creature
{
    private boolean visible;
    private Creature creature;
    private Legion legion;

    private BattleMap map;
    private Battle battle;
    private boolean struck;

    private BattleHex currentHex;
    private BattleHex startingHex;

    private String currentHexLabel;
    private String startingHexLabel;

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
        if (name.equals("Titan") && legion != null)
        {
            setPower(getPlayer().getTitanPower());
        }
    }


    public void addBattleInfo(BattleHex currentHex, BattleHex startingHex,
        BattleMap map, BattleChit chit, Battle battle)
    {
        this.currentHex = currentHex;
        this.startingHex = startingHex;
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


    public void setLegion(Legion legion)
    {
        this.legion = legion;
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
        String basename = super.getImageName(inverted);

        if (name.equals("Titan") && getPower() >= 6 && getPower() <= 20)
        {
            // Use Titan14.gif for a 14-power titan, etc.  Use the generic
            // Titan.gif (with X-4) for ridiculously big titans, to avoid
            // the need for an infinite number of images.
            basename = basename + getPower();
        }

        return basename;
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


    public void setHits(int hits)
    {
        this.hits = hits;
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
        return (startingHex != currentHex);
    }


    public void commitMove()
    {
        startingHex = currentHex;
    }


    public boolean hasStruck()
    {
        return struck;
    }


    public void setStruck(boolean struck)
    {
        this.struck = struck;
    }


    public BattleHex getCurrentHex()
    {
        return currentHex;
    }


    public BattleHex getStartingHex()
    {
        return startingHex;
    }


    public String getCurrentHexLabel()
    {
        if (currentHex != null)
        {
            return currentHex.getLabel();
        }
        else
        {
            return currentHexLabel;
        }
    }


    public void setCurrentHexLabel(String label)
    {
        this.currentHexLabel = label;
    }


    public String getStartingHexLabel()
    {
        if (startingHex != null)
        {
            return startingHex.getLabel();
        }
        else
        {
            return startingHexLabel;
        }
    }


    public void setStartingHexLabel(String label)
    {
        this.startingHexLabel = label;
    }


    /** Return the number of enemy creatures in contact with this critter.
     *  Dead critters count as being in contact only if countDead is true. */
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
                    Critter other = hex.getCritter();
                    if (other != null && other.getPlayer() != getPlayer() &&
                        (countDead || !other.isDead()))
                    {
                        count++;
                    }
                }
            }
        }

        return count;
    }


    /** Return true if there are any enemies adjacent to this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    public boolean isInContact(boolean countDead)
    {
        // Offboard creatures are not in contact.
        if (currentHex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (currentHex.getHexside(i) != 'c' &&
                currentHex.getOppositeHexside(i) != 'c')
            {
                BattleHex hex = currentHex.getNeighbor(i);
                if (hex != null)
                {
                    Critter other = hex.getCritter();
                    if (other != null && other.getPlayer() != getPlayer() &&
                        (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /** If there is exactly one live enemy critter in contact, return it.
     *  Otherwise return null. */
    public Critter getForcedStrikeTarget()
    {
        // Offboard creatures are not in contact.
        if (currentHex.isEntrance())
        {
            return null;
        }

        Critter target = null;
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
                    Critter other = hex.getCritter();
                    if (other != null && other.getPlayer() != getPlayer() &&
                        !other.isDead())
                    {
                        count++;
                        if (count >= 2)
                        {
                            return null;
                        }
                        target = other;
                    }
                }
            }
        }

        return target;
    }


    public void moveToHex(BattleHex hex)
    {
        Game.logEvent(name + " moves from " + currentHex.getLabel() +
            " to " + hex.getLabel());

        currentHex.removeCritter(this);
        currentHex = hex;
        currentHex.addCritter(this);
        battle.setLastCritterMoved(this);
        map.repaint();
    }


    public void undoMove()
    {
        currentHex.removeCritter(this);
        currentHex = startingHex;
        currentHex.addCritter(this);
        Game.logEvent(name + " undoes move and returns to " +
            startingHex.getLabel());
        map.repaint();
    }


    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain. */
    public int getDice(Critter target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !isInContact(true);
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice >>= 1;

            // Dragon rangestriking from volcano: +2
            if (name.equals("Dragon") && currentHex.getTerrain() == 'v')
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
            int direction = Battle.getDirection(currentHex, targetHex, false);
            char hexside = currentHex.getHexside(direction);

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
            else if (!isNativeSandDune() &&
                currentHex.getOppositeHexside(direction) == 'd')
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
                int direction = Battle.getDirection(currentHex, targetHex,
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
                int direction = Battle.getDirection(targetHex, currentHex,
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
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation() -
                    currentHex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    attackerSkill -= heightDeficit;
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
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1
        if (target.getCurrentHex().getTerrain() == 'r' &&
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
        if (carryPossible && (dice <= target.getPower() - target.getHits()))
        {
            carryPossible = false;
        }

        int strikeNumber = getStrikeNumber(target);

        // Figure whether number of dice or strike number needs to be
        // penalized in order to carry.
        if (carryPossible)
        {
            boolean haveCarryTarget = false;
            ArrayList penaltyOptions = new ArrayList();

            for (int i = 0; i < 6; i++)
            {
                // Adjacent creatures separated by a cliff are not in contact.
                if (currentHex.getHexside(i) != 'c' &&
                    currentHex.getOppositeHexside(i) != 'c')
                {
                    BattleHex hex = currentHex.getNeighbor(i);
                    if (hex != null && hex != targetHex)
                    {
                        Critter critter = hex.getCritter();
                        if (critter != null && critter.getPlayer() !=
                            getPlayer() && !critter.isDead())
                        {
                            int tmpDice = getDice(critter);
                            int tmpStrikeNumber = getStrikeNumber(critter);

                            // Strikes not up across dune hexsides cannot
                            // carry up across dune hexsides.
                            if (currentHex.getOppositeHexside(i) == 'd')
                            {
                                int direction = Battle.getDirection(targetHex,
                                    currentHex, false);
                                if (targetHex.getHexside(direction) != 'd')
                                {
                                    critter.setCarryFlag(false);
// XXX debug
System.out.println("DENIED CARRY UP DUNE HEXSIDE");
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
                                haveCarryTarget = true;
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
                            haveCarryTarget = true;
                        }
                    }
                }
            }

            if (!haveCarryTarget)
            {
                carryPossible = false;
            }
        }


        // Roll the dice.
        int damage = 0;

        int [] rolls = new int[dice];
        StringBuffer rollString = new StringBuffer(36);

        Game game = battle.getGame();
        if (game != null && Game.getOption(Game.chooseHits))
        {
            do
            {
                String answer = JOptionPane.showInputDialog(map,
                    "Input number of hits (0-" + dice + ")");
                try
                {
                    damage = Integer.parseInt(answer);
                }
                catch (NumberFormatException e)
                {
                    damage = -1;
                }
            }
            while (damage < 0 || damage > dice);
            for (int i = 0; i < damage; i++)
            {
                rolls[i] = 6;
                rollString.append(rolls[i]);
            }
            for (int i = damage; i < dice; i++)
            {
                rolls[i] = 1;
                rollString.append(rolls[i]);
            }
        }
        else
        {
            for (int i = 0; i < dice; i++)
            {
                rolls[i] = Game.rollDie();
                rollString.append(rolls[i]);

                if (rolls[i] >= strikeNumber)
                {
                    damage++;
                }
            }
        }

        Game.logEvent(name + " in " + currentHex.getLabel() +
            " strikes " + target.getName() + " in " +
            targetHex.getLabel() + " with strike number " +
            strikeNumber + " : " + rollString + ": " + damage +
            (damage == 1 ? " hit" : " hits"));

        int carryDamage = target.wound(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            Game.logEvent(carryDamage + (carryDamage == 1 ?
                " carry available" : " carries available"));
            battle.setCarryDamage(carryDamage);
            battle.highlightCarries();
        }

        // Record that this attacker has struck.
        struck = true;

        // Display the rolls in the BattleDice dialog, if enabled.
        if (Game.getOption(Game.showDice))
        {
            BattleDice battleDice = battle.getBattleDice();
            battleDice.setValues(this, target, strikeNumber,
                rolls, damage, carryDamage);
            battleDice.setup();
        }
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
