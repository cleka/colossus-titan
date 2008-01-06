package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.HexMap;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class Critter represents an individual Titan Character.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class Critter extends net.sf.colossus.game.Creature implements
    Comparable<Critter>
{
    private static final Logger LOGGER = Logger.getLogger(Critter.class
        .getName());

    private String markerId;
    private Battle battle;
    private boolean struck;
    private String currentHexLabel;
    private String startingHexLabel;

    /** Damage taken */
    private int hits = 0;
    private final Game game;

    /** Unique identifier for each critter. */
    private final int tag;

    /** Counter used to assign unique tags. */
    private static int tagCounter = -1;
    private final SortedSet<PenaltyOption> penaltyOptions = new TreeSet<PenaltyOption>();
    private boolean carryPossible;

    public Critter(Creature creature, String markerId, Game game)
    {
        super(creature);
        this.markerId = markerId;
        this.game = game;
        tag = ++tagCounter;
    }

    void addBattleInfo(String currentHexLabel, String startingHexLabel,
        Battle battle)
    {
        this.currentHexLabel = currentHexLabel;
        this.startingHexLabel = startingHexLabel;
        this.battle = battle;
    }

    /**
     * TODO MODEL -- getType() should be used once everything needed is moved
     *      up into the CreatureType class
     */
    Creature getCreature()
    {
        return (Creature)getType();
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

    int getTag()
    {
        return tag;
    }

    Battle getBattle()
    {
        return battle;
    }

    String getDescription()
    {
        return getName() + " in " + getCurrentHex().getDescription();
    }

    private int getPower()
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
        return getCreature().getPower();
    }

    int getHits()
    {
        return hits;
    }

    void setHits(int hits)
    {
        this.hits = hits;
    }

    void heal()
    {
        hits = 0;
    }

    /** Apply damage to this critter.  Return the amount of excess damage
     *  done, which may sometimes carry to another target. */
    int wound(int damage)
    {
        int excess = 0;

        if (damage > 0)
        {
            int oldhits = hits;
            hits = hits + damage;
            if (hits > getPower())
            {
                excess = hits - getPower();
                hits = getPower();
            }

            LOGGER.log(Level.INFO, "Critter " + getDescription() + ": "
                + oldhits + " + " + damage + " => " + hits + "; " + excess
                + " excess");

            // Check for death.
            if (hits >= getPower())
            {
                LOGGER.log(Level.INFO, "Critter " + getDescription()
                    + " is now dead: (hits=" + hits + " > power=" + getPower()
                    + ")");
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

    protected BattleHex getCurrentHex()
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
    private int numInContact(boolean countDead)
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
                    if (other != null && other.getPlayer() != getPlayer()
                        && (countDead || !other.isDead()))
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
    protected boolean isInContact(boolean countDead)
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
                    if (other != null && other.getPlayer() != getPlayer()
                        && (countDead || !other.isDead()))
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
    void moveToHex(String hexLabel, boolean tellClients)
    {
        currentHexLabel = hexLabel;
        if (tellClients)
        {
            battle.getGame().getServer().allTellBattleMove(tag,
                startingHexLabel, currentHexLabel, false);
        }
    }

    void undoMove()
    {
        String formerHexLabel = currentHexLabel;
        currentHexLabel = startingHexLabel;
        LOGGER.log(Level.INFO, getName() + " undoes move and returns to "
            + startingHexLabel);
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
    protected int getDice(Critter target)
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
            if (isNativeVolcano()
                && hex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // volcanoNative striking from volcano: +2
            if (isNativeVolcano()
                && hex.getTerrain().equals(HazardTerrain.VOLCANO))
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
            else if (!isNativeSandDune()
                && hex.getOppositeHexside(direction) == 'd')
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
            if (hex.getTerrain().equals(HazardTerrain.BRAMBLES)
                && !isNativeBramble())
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
            if (Battle.getRange(hex, targetHex, false) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!isNativeBramble())
            {
                attackerSkill -= countBrambleHexes(targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            if (targetHex.hasWall())
            {
                int heightDeficit = targetHex.getElevation()
                    - hex.getElevation();
                if (heightDeficit > 0)
                {
                    // Because of the design of the tower map, a strike to
                    // a higher tower hex always crosses one wall per
                    // elevation difference.
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    protected int countBrambleHexes(final BattleHex targetHex)
    {
        return battle.countBrambleHexes(getCurrentHex(), targetHex);
    }

    protected int getStrikeNumber(Critter target)
    {
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-magicMissile: +1
        if (target.getCurrentHex().getTerrain().equals(HazardTerrain.BRAMBLES)
            && target.isNativeBramble() && !isNativeBramble()
            && !(rangestrike && useMagicMissile()))
        {
            strikeNumber++;
        }

        // Native defending in stone, from strike by a non-native: +1
        // Native defending in stone, from rangestrike by a non-native
        //     non-magicMissile: +1
        if (target.getCurrentHex().getTerrain().equals(HazardTerrain.STONE)
            && target.isNativeStone() && !isNativeStone()
            && !(rangestrike && useMagicMissile()))
        {
            strikeNumber++;
        }

        // Native defending in tree, from strike by a non-native: +1
        // Native defending in tree, from rangestrike by a non-native
        //     non-magicMissile: no effect
        if (target.getCurrentHex().getTerrain().equals(HazardTerrain.TREE)
            && target.isNativeTree() && !isNativeTree() && !(rangestrike))
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
    synchronized void strike(Critter target)
    {
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
            findCarries(target);
            if (!penaltyOptions.isEmpty())
            {
                chooseStrikePenalty();
                return;
            }
        }
        strike2(target, dice, strikeNumber);
    }

    private void chooseStrikePenalty()
    {
        game.getServer().askChooseStrikePenalty(penaltyOptions);
    }

    /** Side effects. */
    void assignStrikePenalty(String prompt)
    {
        PenaltyOption po = matchingPenaltyOption(prompt);
        if (po != null)
        {
            Critter target = po.getTarget();
            int dice = po.getDice();
            int strikeNumber = po.getStrikeNumber();
            carryPossible = (po.numCarryTargets() >= 1);
            battle.setCarryTargets(po.getCarryTargets());
            strike2(target, dice, strikeNumber);
        }
        else
        {
            LOGGER.log(Level.WARNING, "Illegal penalty option " + prompt);
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
        Iterator<PenaltyOption> it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = it.next();
            if (prompt.equals(po.toString()))
            {
                return po;
            }
        }
        return null;
    }

    // XXX Should be able to return penaltyOptions and carry targets
    // rather than use side effects.

    /** Side effects on penaltyOptions, Battle.carryTargets */
    void findCarries(Critter target)
    {
        battle.clearCarryTargets();
        penaltyOptions.clear();

        // Abort if no carries are possible.
        if (getDice(target) <= target.getPower() - target.getHits())
        {
            return;
        }

        // Look for possible carries in each direction.
        for (int i = 0; i < 6; i++)
        {
            if (possibleCarryToDir(target.getCurrentHex(), i))
            {
                findCarry(target, getCurrentHex().getNeighbor(i));
            }
        }

        if (!penaltyOptions.isEmpty())
        {
            // Add the non-penalty option as a choice.
            PenaltyOption po = new PenaltyOption(this, target,
                getDice(target), getStrikeNumber(target));
            penaltyOptions.add(po);

            // Add all non-penalty carries to every PenaltyOption.
            Iterator<PenaltyOption> it = penaltyOptions.iterator();
            while (it.hasNext())
            {
                po = it.next();
                po.addCarryTargets(battle.getCarryTargets());
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
        if (hex.getOppositeHexside(dir) == 'd'
            && targetHex
                .getHexside(Battle.getDirection(targetHex, hex, false)) != 'd')
        {
            return false;
        }
        return true;
    }

    /** For a strike on target, find any carries (including those
     *  only allowed via strike penalty) to the creature in neighbor
     *  Side effects on penaltyOptions, Battle.carryTargets */
    private void findCarry(Critter target, BattleHex neighbor)
    {
        final int dice = getDice(target);
        final int strikeNumber = getStrikeNumber(target);

        Critter victim = battle.getCritter(neighbor);
        if (victim == null || victim.getPlayer() == getPlayer()
            || victim.isDead())
        {
            return;
        }
        int tmpDice = getDice(victim);
        int tmpStrikeNumber = getStrikeNumber(victim);

        // Can't actually get a bonus to carry.
        if (tmpDice > dice)
        {
            tmpDice = dice;
        }
        if (tmpStrikeNumber < strikeNumber)
        {
            tmpStrikeNumber = strikeNumber;
        }

        // Abort if no carries are possible.
        if (tmpDice <= target.getPower() - target.getHits())
        {
            return;
        }

        if (tmpStrikeNumber == strikeNumber && tmpDice == dice)
        {
            // Can carry with no need for a penalty.
            battle.addCarryTarget(neighbor.getLabel());
        }

        else
        {
            // Add this scenario to the list, reusing an
            // existing PenaltyOption if possible.
            Iterator<PenaltyOption> it = penaltyOptions.iterator();
            while (it.hasNext())
            {
                PenaltyOption po = it.next();
                if (po.getDice() == tmpDice
                    && po.getStrikeNumber() == tmpStrikeNumber)
                {
                    po.addCarryTarget(neighbor.getLabel());
                    return;
                }
            }
            // No match, so create a new PenaltyOption.
            PenaltyOption po = new PenaltyOption(this, target, tmpDice,
                tmpStrikeNumber);
            po.addCarryTarget(neighbor.getLabel());
            penaltyOptions.add(po);
        }
    }

    /** Called after strike penalties are chosen.
     *  Roll the dice and apply damage.  Highlight legal carry targets. */
    private void strike2(Critter target, int dice, int strikeNumber)
    {
        // Roll the dice.
        int damage = 0;
        // Check if we roll or if we don't
        boolean randomized = !(game.getOption(Options.nonRandomBattleDice));

        List<String> rolls = new ArrayList<String>();
        StringBuffer rollString = new StringBuffer(36);

        for (int i = 0; i < dice; i++)
        {
            int roll = (randomized ? Dice.rollDie() : Dice.rollDieNonRandom());
            rolls.add("" + roll);
            rollString.append(roll);

            if (roll >= strikeNumber)
            {
                damage++;
            }
        }

        LOGGER.log(Level.INFO, getName() + " in " + currentHexLabel
            + " strikes " + target.getDescription() + " with strike number "
            + strikeNumber + " : " + rollString + ": " + damage
            + (damage == 1 ? " hit" : " hits"));

        int carryDamage = target.wound(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }
        battle.setCarryDamage(carryDamage);

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            LOGGER.log(Level.INFO, carryDamage
                + (carryDamage == 1 ? " carry available"
                    : " carries available"));
        }

        // Record that this attacker has struck.
        setStruck(true);

        if (game != null)
        {
            game.getServer().allTellStrikeResults(this, target, strikeNumber,
                rolls, damage, carryDamage,
                battle.getCarryTargetDescriptions());
        }
    }

    Set<PenaltyOption> getPenaltyOptions()
    {
        return Collections.unmodifiableSortedSet(penaltyOptions);
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
        }
    }

    /** Inconsistent with equals() */
    public int compareTo(Critter critter)
    {
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

    // big ugly overloading, in case our Creature isn't really a Creature,
    // but a subclass of Creature.
    // getPower() is not there, as it it already overloaded above
    // to support Titan Power.

    public String getName()
    {
        return getCreature().getName();
    }

    public int getMaxCount()
    {
        return getCreature().getMaxCount();
    }

    public boolean isLord()
    {
        return getCreature().isLord();
    }

    public boolean isDemiLord()
    {
        return getCreature().isDemiLord();
    }

    public boolean isLordOrDemiLord()
    {
        return getCreature().isLordOrDemiLord();
    }

    public boolean isImmortal()
    {
        return getCreature().isImmortal();
    }

    public boolean isTitan()
    {
        return getCreature().isTitan();
    }

    public String getPluralName()
    {
        return getCreature().getPluralName();
    }

    public String getImageName()
    {
        return getCreature().getImageName();
    }

    public String getDisplayName()
    {
        return getCreature().getDisplayName();
    }

    public String[] getImageNames()
    {
        return getCreature().getImageNames();
    }

    public int getSkill()
    {
        return getCreature().getSkill();
    }

    public int getPointValue()
    {
        // Must use our local, Titan-aware getPower()
        // return getCreature().getPointValue();
        return getPower() * getSkill();
    }

    public int getHintedRecruitmentValue()
    {
        // Must use our local, Titan-aware getPointValue()
        // return getCreature().getHintedRecruitmentValue();
        return getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(getCreature()
                .getName());
    }

    public int getHintedRecruitmentValue(String[] section)
    {
        // Must use our local, Titan-aware getPointValue()
        // return getCreature().getHintedRecruitmentValue(section);
        return getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(getCreature()
                .getName(), section);
    }

    public boolean isRangestriker()
    {
        return getCreature().isRangestriker();
    }

    public boolean isFlier()
    {
        return getCreature().isFlier();
    }

    public boolean isNativeTerrain(HazardTerrain t)
    {
        return getCreature().isNativeTerrain(t);
    }

    public boolean isNativeHexside(char h)
    {
        return getCreature().isNativeHexside(h);
    }

    public boolean isNativeBramble()
    {
        return getCreature().isNativeBramble();
    }

    public boolean isNativeDrift()
    {
        return getCreature().isNativeDrift();
    }

    public boolean isNativeBog()
    {
        return getCreature().isNativeBog();
    }

    public boolean isNativeSandDune()
    {
        return getCreature().isNativeSandDune();
    }

    public boolean isNativeSlope()
    {
        return getCreature().isNativeSlope();
    }

    public boolean isNativeVolcano()
    {
        return getCreature().isNativeVolcano();
    }

    public boolean isNativeRiver()
    {
        return getCreature().isNativeRiver();
    }

    public boolean isNativeStone()
    {
        return getCreature().isNativeStone();
    }

    public boolean isNativeTree()
    {
        return getCreature().isNativeTree();
    }

    public boolean isWaterDwelling()
    {
        return getCreature().isWaterDwelling();
    }

    public boolean useMagicMissile()
    {
        return getCreature().useMagicMissile();
    }

    public boolean isSummonable()
    {
        return getCreature().isSummonable();
    }

    @Override
    public String toString()
    {
        return getCreature().toString();
    }

    @Override
    public int hashCode()
    {
        return getCreature().hashCode();
    }
}
