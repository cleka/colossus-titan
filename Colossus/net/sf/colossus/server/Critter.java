package net.sf.colossus.server;


import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.Probs;
import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;


/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 */

final class Critter extends Creature implements Comparable
{
    private boolean visible;
    private Creature creature;
    private String markerId;
    private Battle battle;
    private boolean struck;
    private String currentHexLabel;
    private String startingHexLabel;
    /** Damage taken */
    private int hits;
    private Game game;
    /** Unique identifier for each critter. */
    private int tag;
    /** Counter used to assign unique tags. */
    private static int tagCounter = -1;
    private SortedSet penaltyOptions = new TreeSet();
    private boolean carryPossible;


    Critter(Creature creature, boolean visible, String markerId, Game game)
    {
        super(creature);

        this.creature = creature;
        this.visible = visible;
        this.markerId = markerId;
        this.game = game;
        tag = ++tagCounter;
    }


    /** Deep copy for AI. */
    Critter AICopy(Game game)
    {
        Critter newCritter = new Critter(creature, visible, markerId, game);

        newCritter.battle = battle;
        newCritter.struck = struck;
        newCritter.currentHexLabel = currentHexLabel;
        newCritter.startingHexLabel = startingHexLabel;
        newCritter.hits = hits;
        newCritter.tag = tag;

        return newCritter;
    }


    void addBattleInfo(String currentHexLabel, String startingHexLabel,
        Battle battle)
    {
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.battle = battle;
    }

    void setGame(Game game)
    {
        this.game = game;
    }

    boolean isVisible()
    {
        return visible;
    }

    void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    Creature getCreature()
    {
        return creature;
    }

    String getMarkerId()
    {
        return markerId;
    }

    void setMarkerId(String markerId)
    {
        this.markerId = markerId;
    }

    Legion getLegion()
    {
        return game.getLegionByMarkerId(markerId);
    }

    Player getPlayer()
    {
        return game.getPlayerByMarkerId(markerId);
    }

    String getPlayerName()
    {
        return game.getPlayerByMarkerId(markerId).getName();
    }

    int getTag()
    {
        return tag;
    }


    Battle getBattle()
    {
        return battle;
    }

    /** Return only the base part of the image name for this critter. */
    public String getImageName(boolean inverted)
    {
        String basename = super.getImageName(inverted);

        if (isTitan() && getPower() >= 6 && getPower() <= 20)
        {
            // Use Titan14.gif for a 14-power titan, etc.  Use the generic
            // Titan.gif (with X-4) for ridiculously big titans, to avoid
            // the need for an infinite number of images.
            basename = basename + getPower();
        }

        return basename;
    }


    String getDescription()
    {
        return getName() + " in " + getCurrentHex().getDescription();
    }


    public int getPower()
    {
        if (isTitan())
        {
            Player player = getPlayer();
            if (player != null)
            {
                return player.getTitanPower();
            }
            else
            {
                // Just in case player is dead.
                return 6;
            }
        }
        return super.getPower();
    }


    int getHits()
    {
        return hits;
    }


    void setHits(int hits)
    {
        this.hits = hits;
        // TODO remove
        battle.getGame().getServer().allSetBattleChitHits(tag, hits);
    }


    void heal()
    {
        hits = 0;
    }


    boolean wouldDieFrom(int hits)
    {
        return (hits + getHits() > getPower());
    }


    /** Apply damage to this critter.  Return the amount of excess damage
     *  done, which may sometimes carry to another target. */
    int wound(int damage)
    {
        int excess = 0;

        if (damage > 0)
        {
            setHits(hits + damage);
            if (hits > getPower())
            {
                excess = hits - getPower();
                hits = getPower();
            }

            // Check for death.
            if (hits >= getPower())
            {
                setDead(true);
            }
        }

        return excess;
    }


    boolean hasMoved()
    {
        return (!currentHexLabel.equals(startingHexLabel));
    }


    void commitMove()
    {
        startingHexLabel = currentHexLabel;
    }


    boolean hasStruck()
    {
        return struck;
    }


    void setStruck(boolean struck)
    {
        this.struck = struck;
    }


    BattleHex getCurrentHex()
    {
        return HexMap.getHexByLabel(battle.getTerrain(), currentHexLabel);
    }

    BattleHex getStartingHex()
    {
        return HexMap.getHexByLabel(battle.getTerrain(), startingHexLabel);
    }

    String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    void setCurrentHexLabel(String label)
    {
        this.currentHexLabel = label;
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    void setStartingHexLabel(String label)
    {
        this.startingHexLabel = label;
    }


    /** Return the number of enemy creatures in contact with this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    int numInContact(boolean countDead)
    {
        BattleHex hex = getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    Critter other = battle.getCritter(neighbor);
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


    /** Return the number of friendly creatures adjacent to this critter.
     *  Dead critters do not count. */
    int numAdjacentAllies()
    {
        BattleHex hex = getCurrentHex();
        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            BattleHex neighbor = hex.getNeighbor(i);
            if (neighbor != null)
            {
                Critter other = battle.getCritter(neighbor);
                if (other != null && other.getPlayer() == getPlayer() &&
                    !other.isDead())
                {
                    count++;
                }
            }
        }
        return count;
    }


    /** Return true if there are any enemies adjacent to this critter.
     *  Dead critters count as being in contact only if countDead is true. */
    boolean isInContact(boolean countDead)
    {
        BattleHex hex = getCurrentHex();

        // Offboard creatures are not in contact.
        if (hex.isEntrance())
        {
            return false;
        }

        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (!hex.isCliff(i))
            {
                BattleHex neighbor = hex.getNeighbor(i);
                if (neighbor != null)
                {
                    Critter other = battle.getCritter(neighbor);
                    if (other != null && !other.getPlayerName().equals(
                        getPlayerName()) &&
                        (countDead || !other.isDead()))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    /** Most code should use Battle.doMove() instead, since it checks
     *  for legality and logs the move. */
    void moveToHex(String hexLabel)
    {
        currentHexLabel = hexLabel;
        battle.getGame().getServer().allTellBattleMove(tag, startingHexLabel,
            currentHexLabel, false);
    }


    void undoMove()
    {
        String formerHexLabel = currentHexLabel;
        currentHexLabel = startingHexLabel;
        Log.event(getName() + " undoes move and returns to " +
            startingHexLabel);
        battle.getGame().getServer().allTellBattleMove(tag, formerHexLabel,
            currentHexLabel, true);
    }


    boolean canStrike(Critter target)
    {
        String hexLabel = target.getCurrentHexLabel();
        return battle.findStrikes(this, true).contains(hexLabel);
    }


    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain. */
    int getDice(Critter target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !isInContact(true);
        if (rangestrike)
        {
            // Divide power in half, rounding down.
            dice /= 2;

            // volcanoNative rangestriking from volcano: +2
            if (isNativeVolcano() && hex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (isNativeVolcano() && hex.getTerrain() == 'v')
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = Battle.getDirection(hex, targetHex, false);
            char hexside = hex.getHexside(direction);

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
                hex.getOppositeHexside(direction) == 'd')
            {
                dice--;
            }
        }

        return dice;
    }


    private int getAttackerSkill(Critter target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = getSkill();

        boolean rangestrike = !isInContact(true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (hex.getTerrain() == 'r' && !isNativeBramble())
            {
                attackerSkill--;
            }

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = Battle.getDirection(hex, targetHex, false);
                char hexside = hex.getHexside(direction);
                // Striking down across wall: +1
                if (hexside == 'w')
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = Battle.getDirection(targetHex, hex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !isNativeSlope()) || hexside == 'w')
                {
                    attackerSkill--;
                }
            }
        }
        else if (!useMagicMissile())
        {
            // Range penalty
            if (battle.getRange(hex, targetHex, false) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!isNativeBramble())
            {
                attackerSkill -= battle.countBrambleHexes(hex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation() -
                    hex.getElevation();
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


    int getStrikeNumber(Critter target)
    {
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-magicMissile: +1
        if (target.getCurrentHex().getTerrain() == 'r' &&
            target.isNativeBramble() &&
            !isNativeBramble() &&
            !(rangestrike && useMagicMissile()))
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


    /** Calculate number of dice and strike number needed to hit target,
     *  and whether any carries and strike penalties are possible.  The
     *  actual striking is now deferred to strike2(). */
    void strike(Critter target)
    {
        // Sanity check
        if (target.getPlayer() == getPlayer())
        {
            Log.error(getDescription() + " tried to strike allied " +
                target.getDescription());
            return;
        }

        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        battle.leaveCarryMode();
        carryPossible = true;
        if (numInContact(false) < 2)
        {
            carryPossible = false;
        }

        int strikeNumber = getStrikeNumber(target);
        int dice = getDice(target);

        // Carries are only possible if the striker is rolling more dice than
        // the target has hits remaining.
        if (carryPossible && (dice <= target.getPower() - target.getHits()))
        {
            carryPossible = false;
        }

        if (carryPossible)
        {
            findCarries(target, dice, strikeNumber);

            if (!penaltyOptions.isEmpty())
            {
                // Add the non-penalty option as a choice.
                PenaltyOption po = new PenaltyOption(this, target, dice, 
                    strikeNumber);
                po.addCarryTargets(battle.getCarryTargets());
                penaltyOptions.add(po);

                if (game.getServer().getClientOption(getPlayerName(),
                    Options.autoStrike))
                {
                    po = getPlayer().aiChooseStrikePenalty(
                        Collections.unmodifiableSortedSet(penaltyOptions));
                    if (po == null)
                    {
                        Log.error("aiChooseStrikePenalty returned null!");
                    }
                    else
                    {
Log.debug("aiChooseStrikePenalty returned: " + po.toString()); 
                        assignStrikePenalty(po.toString());
                        return;
                    }
                }
                else
                {
                    game.getServer().askChooseStrikePenalty(penaltyOptions);
                    return;
                }
            }
        }

        strike2(target, dice, strikeNumber);
    }

    void assignStrikePenalty(String prompt)
    {
Log.debug("calling Critter.assignStrikePenalty() with " + prompt);
        PenaltyOption po = matchingPenaltyOption(prompt); 
        if (po != null)
        {
            Log.debug("Valid penalty option " + po.toString());
            Critter target = po.getTarget();
            int dice = po.getDice();
            int strikeNumber = po.getStrikeNumber();
            carryPossible = (po.numCarryTargets() >= 1);
            battle.setCarryTargets(po.getCarryTargets());
            strike2(target, dice, strikeNumber);
        }
        else
        {
            Log.warn("Illegal penalty option " + prompt);
        }
    }

    /** Return true if the passed prompt matches one of the stored
     *  penalty options. */
    private PenaltyOption matchingPenaltyOption(String prompt)
    {
        if (penaltyOptions == null)
        {
            return null;
        }
        Iterator it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = (PenaltyOption)it.next();
            if (prompt.equals(po.toString()))
            {
                return po;
            }
        }
        return null;
    }


    /** Return true if there's any chance that this critter could take
     *  a strike penalty to carry when striking target.  Side effects
     *  on penaltyOptions, battle.carryTargets. */
    boolean possibleStrikePenalty(Critter target)
    {
        int dice = getDice(target);
        int strikeNumber = getStrikeNumber(target);
        findCarries(target, dice, strikeNumber);
        return (!penaltyOptions.isEmpty());
    }

    // Side effects on penaltyOptions, Battle.carryTargets
    private void findCarries(Critter target, int dice, int strikeNumber)
    {
Log.debug("findCarries " + this.getDescription() + " striking " + target.getDescription() + " dice:" + dice + " strikeNumber:" + strikeNumber);

        battle.clearCarryTargets();
        penaltyOptions.clear();

        // Look for possible carries in each direction.
        for (int i = 0; i < 6; i++)
        {
            if (possibleCarryToDir(target.getCurrentHex(), i))
            {
                findCarry(target, dice, strikeNumber, 
                    getCurrentHex().getNeighbor(i));
            }
        }
    }

    /** Return true if carries are possible to the hex in direction
     *  dir, considering only terrain. */
    private boolean possibleCarryToDir(BattleHex targetHex, int dir)
    {
        BattleHex hex = getCurrentHex();
        BattleHex neighbor = hex.getNeighbor(dir); 

        if (neighbor == null || neighbor == targetHex)
        {
            return false;
        }
        if (hex.isCliff(dir))
        {
            return false;
        }
        // Strikes not up across dune hexsides cannot carry up across 
        // dune hexsides.
        if (hex.getOppositeHexside(dir) == 'd' && targetHex.getHexside(
            Battle.getDirection(targetHex, hex, false)) != 'd')
        {
            return false;
        }
        return true;
    }

    /** For a strike on target, find any carries (including those
     *  only allowed via strike penalty) to the creature in neighbor */
    private void findCarry(Critter target, int dice, int strikeNumber, 
        BattleHex neighbor)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        Critter victim = battle.getCritter(neighbor);
        if (victim == null || victim.getPlayer() == getPlayer() || 
            victim.isDead())
        {
            return;
        }
        int tmpDice = getDice(victim);
        int tmpStrikeNumber = getStrikeNumber(victim);

        if (tmpStrikeNumber <= strikeNumber && tmpDice >= dice)
        {
            // Can carry with no need for a penalty.
            battle.addCarryTarget(neighbor.getLabel());
Log.debug("added carry target " + victim.getDescription());
        }

        else
        {
            // Add this scenario to the list, reusing an
            // existing PenaltyOption if possible.
            Iterator it = penaltyOptions.iterator();
            while (it.hasNext())
            {
                PenaltyOption po = (PenaltyOption)it.next();
                if (po.getDice() == tmpDice &&
                    po.getStrikeNumber() == tmpStrikeNumber)
                {
Log.debug("existing penalty option: " + po.toString());
                    po.addCarryTarget(neighbor.getLabel());
                    return;
                }
            }
            // No match, so create a new one.
            PenaltyOption po =  new PenaltyOption(this, target,
                tmpDice, tmpStrikeNumber);
            po.addCarryTarget(neighbor.getLabel());
            penaltyOptions.add(po);
Log.debug("new penalty option: " + po.toString());
        }
    }


    /** Called after strike penalties are chosen.
     *  Roll the dice and apply damage.  Highlight legal carry targets. */
    private void strike2(Critter target, int dice, int strikeNumber)
    {
        if (hasStruck())
        {
            Log.debug("Removed extra strike2() call for " + getDescription());
            return;
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

        Log.event(getName() + " in " + currentHexLabel +
            " strikes " + target.getDescription() + " with strike number " +
            strikeNumber + " : " + rollString + ": " + damage +
            (damage == 1 ? " hit" : " hits"));

        int carryDamage = target.wound(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }
        battle.setCarryDamage(carryDamage);

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            Log.event(carryDamage + (carryDamage == 1 ?
                " carry available" : " carries available"));
        }

        // Record that this attacker has struck.
        struck = true;

        if (game != null)
        {
            game.getServer().allTellStrikeResults(this, target, 
                strikeNumber, rolls, damage, carryDamage,
                battle.getCarryTargetDescriptions());
        }
    }


    boolean isDead()
    {
        return (getHits() >= getPower());
    }

    void setDead(boolean dead)
    {
        if (dead)
        {
            hits = getPower();
            // XXX Remove
            battle.getGame().getServer().allSetBattleChitDead(tag);
        }
    }

    /** Inconsistent with equals() */
    public int compareTo(Object object)
    {
        Critter critter = (Critter)object;

        if (isTitan())
        {
            return -1;
        }
        if (critter.isTitan())
        {
            return 1;
        }
        int diff = critter.getPointValue() - getPointValue();
        if (diff != 0)
        {
            return diff;
        }
        if (isRangestriker() && !critter.isRangestriker())
        {
            return -1;
        }
        if (!isRangestriker() && critter.isRangestriker())
        {
            return 1;
        }
        if (isFlier() && !critter.isFlier())
        {
            return -1;
        }
        if (!isFlier() && critter.isFlier())
        {
            return 1;
        }
        return getName().compareTo(critter.getName());
    }
}
