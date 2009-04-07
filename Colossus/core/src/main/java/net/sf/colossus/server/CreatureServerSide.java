package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;
import net.sf.colossus.variant.HazardTerrain;


/**
 * Class Critter represents an individual Titan Character.
 *
 * TODO this duplicates functionality from the {@link CreatureType} class,
 * mostly due to the fact that the latter doesn't handle the Titans
 * properly
 *
 * TODO a lot of the code in here is about the battle rules, often
 * implemented in combination with the Battle class. It would be much
 * easier if this class was just a dumb critter and the rules of battles
 * are all in the Battle class.
 *
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public class CreatureServerSide extends Creature
{
    /**
     * Implements an order on Critters by some definition of importance.
     *
     * The order is:
     * - titans first
     * - then sorted by points value
     * - then sorted by rangestriker or not
     * - then sorted by flyer or not
     * - then by name
     *
     * TODO this is actually applicable on the CreatureType level
     */
    public static final Comparator<CreatureServerSide> IMPORTANCE_ORDER = new Comparator<CreatureServerSide>()
    {
        public int compare(CreatureServerSide critter1,
            CreatureServerSide critter2)
        {
            if (critter1.isTitan())
            {
                return -1;
            }
            if (critter2.isTitan())
            {
                return 1;
            }
            int diff = critter2.getPointValue() - critter1.getPointValue();
            if (diff != 0)
            {
                return diff;
            }
            if (critter1.isRangestriker() && !critter2.isRangestriker())
            {
                return -1;
            }
            if (!critter1.isRangestriker() && critter2.isRangestriker())
            {
                return 1;
            }
            if (critter1.isFlier() && !critter2.isFlier())
            {
                return -1;
            }
            if (!critter1.isFlier() && critter2.isFlier())
            {
                return 1;
            }
            return critter1.getName().compareTo(critter2.getName());
        }
    };

    private static final Logger LOGGER = Logger
        .getLogger(CreatureServerSide.class.getName());

    private Legion legion;
    private BattleServerSide battle;
    private boolean struck;
    private BattleHex currentHex;
    private BattleHex startingHex;

    /** Damage taken */
    private int hits = 0;

    /**
     * The game this creature belongs to.
     *
     * Never null.
     */
    private final GameServerSide game;

    /** Unique identifier for each critter. */
    private final int tag;

    /** Counter used to assign unique tags. */
    private static int tagCounter = -1;
    private final SortedSet<PenaltyOption> penaltyOptions = new TreeSet<PenaltyOption>();
    private boolean carryPossible;

    public CreatureServerSide(CreatureType creature, Legion legion,
        GameServerSide game)
    {
        super(creature);
        assert game != null : "No server-side creature without a game";
        this.legion = legion;
        this.game = game;
        tag = ++tagCounter;
    }

    void setBattleInfo(BattleHex currentHex, BattleHex startingHex,
        BattleServerSide battle)
    {
        this.currentHex = currentHex;
        this.startingHex = startingHex;
        this.battle = battle;
    }

    String getMarkerId()
    {
        return legion.getMarkerId();
    }

    void setLegion(LegionServerSide legion)
    {
        this.legion = legion;
    }

    Legion getLegion()
    {
        return legion;
    }

    Player getPlayer()
    {
        return legion.getPlayer();
    }

    int getTag()
    {
        return tag;
    }

    BattleServerSide getBattle()
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
                return ((PlayerServerSide)player).getTitanPower();
            }
            else
            {
                // Just in case player is dead.
                return 6;
            }
        }
        return getType().getPower();
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
        return (!currentHex.equals(startingHex));
    }

    void commitMove()
    {
        startingHex = currentHex;
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
        return currentHex;
    }

    BattleHex getStartingHex()
    {
        return startingHex;
    }

    void setCurrentHex(BattleHex hex)
    {
        this.currentHex = hex;
    }

    void setStartingHex(BattleHex hex)
    {
        this.startingHex = hex;
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
                    CreatureServerSide other = battle.getCritter(neighbor);
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
                    CreatureServerSide other = battle.getCritter(neighbor);
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
    void moveToHex(BattleHex hexLabel, boolean tellClients)
    {
        currentHex = hexLabel;
        if (tellClients)
        {
            battle.getGame().getServer().allTellBattleMove(tag,
                startingHex, currentHex, false);
        }
    }

    void undoMove()
    {
        BattleHex formerHexLabel = currentHex;
        currentHex = startingHex;
        LOGGER.log(Level.INFO, getName() + " undoes move and returns to "
            + startingHex);
        battle.getGame().getServer().allTellBattleMove(tag,
            formerHexLabel, currentHex, true);
    }

    boolean canStrike(CreatureServerSide target)
    {
        String hexLabel = target.getCurrentHex().getLabel();
        return battle.findStrikes(this, true).contains(hexLabel);
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     * WARNING: this is duplicated in Strike
     */
    protected int getDice(CreatureServerSide target)
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
            HazardHexside hazard = hex.getHexsideHazard(direction);

            // Native striking down a dune hexside: +2
            if (hazard == HazardHexside.DUNE && isNativeDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hazard == HazardHexside.SLOPE && isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (!isNativeDune()
                && hex.getOppositeHazard(direction) == HazardHexside.DUNE)
            {
                dice--;
            }
        }

        return dice;
    }

    /** WARNING: this is duplicated in Strike */
    private int getAttackerSkill(CreatureServerSide target)
    {
        BattleHex hex = getCurrentHex();
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = getSkill();

        boolean rangestrike = !isInContact(true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // striking out of possible hazard
            attackerSkill -= hex.getTerrain().getSkillPenaltyStrikeFrom(
                this.isNativeTerrain(hex.getTerrain()),
                target.isNativeTerrain(hex.getTerrain()));

            if (hex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleServerSide.getDirection(hex, targetHex,
                    false);
                HazardHexside hazard = hex.getHexsideHazard(direction);

                // Striking down across wall: +1
                if (hazard == HazardHexside.TOWER)
                {
                    attackerSkill++;
                }
            }
            else if (hex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = BattleServerSide.getDirection(targetHex, hex,
                    false);
                HazardHexside hazard = targetHex.getHexsideHazard(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hazard == HazardHexside.SLOPE && !isNativeSlope())
                    || hazard == HazardHexside.TOWER)
                {
                    attackerSkill--;
                }
            }
            /* TODO: remove TEST TEST TEST TEST TEST */
            /* getStrikingSkill should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Creature
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int checkStrikingSkill = getStrikingSkill(target, hex
                .getElevation(), targetHex.getElevation(),
                hex.getTerrain(), targetHex.getTerrain(),
                hex.getHexsideHazard(BattleServerSide.getDirection(hex,
                    targetHex, false)),
                targetHex.getHexsideHazard(BattleServerSide.getDirection(
                    targetHex, hex, false)));

            if (checkStrikingSkill != attackerSkill)
            {
                LOGGER.warning("attackerSkill says " + attackerSkill
                    + " but checkStrikingSkill says " + checkStrikingSkill);
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */
        }
        else if (!useMagicMissile())
        {
            // Range penalty
            /* Range 4 means a penalty of 1 to the attacker.
             * I hereby extend this so that range 5 means a penalty of 2,
             * and so one, for creature with higher skill.
             */
            int range = BattleServerSide.getRange(hex, targetHex, false);
            if (range >= 4)
            {
                attackerSkill -= (range - 3);
            }
            int bramblesPenalty = 0;
            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!isNativeBramble())
            {
                bramblesPenalty += countBrambleHexes(targetHex);
            }
            /* TODO: remove TEST TEST TEST TEST TEST */
            /* computeSkillPenaltyRangestrikeThrough should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Battle
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int interveningPenalty = battle
                .computeSkillPenaltyRangestrikeThrough(getCurrentHex(),
                    targetHex, this);
            if (interveningPenalty != bramblesPenalty)
            {
                LOGGER.warning("bramblesPenalty says " + bramblesPenalty
                    + " but interveningPenalty says " + interveningPenalty);
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */

            attackerSkill -= bramblesPenalty;

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
                    /* actually this need some better logic, as some Wall are
                     * in a completely different patterns that the Tower
                     * nowaday
                     */
                    attackerSkill -= heightDeficit;
                }
            }

            // Rangestrike into volcano: -1
            /* actually, it's only for native ... but then non-native are
             * blocked. Anyway this will should to HazardTerrain someday.
             */
            if (targetHex.getTerrain().equals(HazardTerrain.VOLCANO))
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }

    /** @deprecated another function with explicit reference to Bramble
     * that should be fixed.
     */
    @Deprecated
    protected int countBrambleHexes(final BattleHex targetHex)
    {
        return battle.countBrambleHexes(getCurrentHex(), targetHex);
    }

    /** WARNING: this is duplicated in Strike */
    protected int getStrikeNumber(CreatureServerSide target)
    {
        boolean rangestrike = !isInContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        HazardTerrain terrain = target.getCurrentHex().getTerrain();

        if (!rangestrike)
        {
            // Strike number can be modified directly by terrain.
            strikeNumber += terrain.getSkillBonusStruckIn(this
                .isNativeTerrain(terrain), target.isNativeTerrain(terrain));
        }
        else
        {
            // Native defending in bramble, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.BRAMBLES)
                && target.isNativeBramble() && !isNativeBramble()
                && !useMagicMissile())
            {
                strikeNumber++;
            }

            // Native defending in stone, from rangestrike by a non-native
            //     non-magicMissile: +1
            if (terrain.equals(HazardTerrain.STONE) && target.isNativeStone()
                && !isNativeStone() && !useMagicMissile())
            {
                strikeNumber++;
            }
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }

    /** Calculate number of dice and strike number needed to hit target,
     *  and whether any carries and strike penalties are possible.
     *  The actual striking is now deferred to strike2(). */
    void strike(CreatureServerSide target)
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
        (game).getServer().askChooseStrikePenalty(penaltyOptions);
    }

    /** Side effects. */
    void assignStrikePenalty(String prompt)
    {
        if (prompt.equals(Constants.cancelStrike))
        {
            LOGGER.log(Level.INFO, "Strike cancelled on pickCarryDialog");
            penaltyOptions.clear();
            battle.clearCarryTargets();
            return;
        }

        PenaltyOption po = matchingPenaltyOption(prompt);
        if (po != null)
        {
            CreatureServerSide target = po.getTarget();
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
    void findCarries(CreatureServerSide target)
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
        int targDir = BattleServerSide.getDirection(targetHex, hex, false);
        if (hex.getOppositeHazard(dir) == HazardHexside.DUNE
            && targetHex.getHexsideHazard(targDir) != HazardHexside.DUNE)
        {
            return false;
        }
        return true;
    }

    /** For a strike on target, find any carries (including those
     *  only allowed via strike penalty) to the creature in neighbor
     *  Side effects on penaltyOptions, Battle.carryTargets */
    private void findCarry(CreatureServerSide target, BattleHex neighbor)
    {
        final int dice = getDice(target);
        final int strikeNumber = getStrikeNumber(target);

        CreatureServerSide victim = battle.getCritter(neighbor);
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
            battle.addCarryTarget(neighbor);
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
                    po.addCarryTarget(neighbor);
                    return;
                }
            }
            // No match, so create a new PenaltyOption.
            PenaltyOption po = new PenaltyOption(this, target, tmpDice,
                tmpStrikeNumber);
            po.addCarryTarget(neighbor);
            penaltyOptions.add(po);
        }
    }

    /** Called after strike penalties are chosen.
     *  Roll the dice and apply damage.  Highlight legal carry targets. */
    private void strike2(CreatureServerSide target, int dice, int strikeNumber)
    {
        // Roll the dice.
        int damage = 0;
        // Check if we roll or if we don't
        boolean randomized = !game.getOption(Options.nonRandomBattleDice);

        List<String> rolls = new ArrayList<String>();
        StringBuilder rollString = new StringBuilder(36);

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

        LOGGER.log(Level.INFO, getName() + " in " + currentHex + " strikes "
            + target.getDescription() + " with strike number " + strikeNumber
            + ", rolling: " + rollString + ": " + damage
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

        game.getServer().allTellStrikeResults(this, target, strikeNumber,
            rolls, damage, carryDamage, battle.getCarryTargetDescriptions());
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

    // big ugly overloading, in case our Creature isn't really a Creature,
    // but a subclass of Creature.
    // getPower() is not there, as it it already overloaded above
    // to support Titan Power.

    public String getName()
    {
        return getType().getName();
    }

    public int getMaxCount()
    {
        return getType().getMaxCount();
    }

    public boolean isLord()
    {
        return getType().isLord();
    }

    public boolean isDemiLord()
    {
        return getType().isDemiLord();
    }

    public boolean isLordOrDemiLord()
    {
        return getType().isLordOrDemiLord();
    }

    public boolean isImmortal()
    {
        return getType().isImmortal();
    }

    public boolean isTitan()
    {
        return getType().isTitan();
    }

    public String getPluralName()
    {
        return getType().getPluralName();
    }

    public String[] getImageNames()
    {
        return getType().getImageNames();
    }

    public int getSkill()
    {
        return getType().getSkill();
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
            + VariantSupport.getHintedRecruitmentValueOffset(getType()
                .getName());
    }

    public int getHintedRecruitmentValue(String[] section)
    {
        // Must use our local, Titan-aware getPointValue()
        // return getCreature().getHintedRecruitmentValue(section);
        return getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(getType()
                .getName(), section);
    }

    public boolean isRangestriker()
    {
        return getType().isRangestriker();
    }

    public boolean isFlier()
    {
        return getType().isFlier();
    }

    public boolean isNativeTerrain(HazardTerrain t)
    {
        return getType().isNativeIn(t);
    }

    public boolean isNativeHexside(HazardHexside hazard)
    {
        return getType().isNativeHexside(hazard.getCode());
    }

    /** @deprecated all isNative<HazardTerrain> are obsolete, one should use
     * isNativeTerrain(<HazardTerrain>) instead, with no explicit reference
     * to the name. This will ease adding new HazardTerrain in variant.
     */
    @Deprecated
    public boolean isNativeBramble()
    {
        return getType().isNativeIn(HazardTerrain.BRAMBLES);
    }

    public boolean isNativeDune()
    {
        return getType().isNativeDune();
    }

    public boolean isNativeSlope()
    {
        return getType().isNativeSlope();
    }

    /** @deprecated all isNative<HazardTerrain> are obsolete, one should use
     * isNativeTerrain(<HazardTerrain>) instead, with no explicit reference
     * to the name. This will ease adding new HazardTerrain in variant.
     */
    @Deprecated
    public boolean isNativeVolcano()
    {
        return getType().isNativeIn(HazardTerrain.VOLCANO);
    }

    public boolean isNativeRiver()
    {
        return getType().isNativeRiver();
    }

    /** @deprecated all isNative<HazardTerrain> are obsolete, one should use
     * isNativeTerrain(<HazardTerrain>) instead, with no explicit reference
     * to the name. This will ease adding new HazardTerrain in variant.
     */
    @Deprecated
    public boolean isNativeStone()
    {
        return getType().isNativeIn(HazardTerrain.STONE);
    }

    @Deprecated
    public boolean isWaterDwelling()
    {
        return getType().isWaterDwelling();
    }

    public boolean useMagicMissile()
    {
        return getType().useMagicMissile();
    }

    public boolean isSummonable()
    {
        return getType().isSummonable();
    }

    // TODO noone seems to be calling this, so we might as well remove it
    // Check first, though -- maybe by adding an assert false here and then
    // run some stresstests and a game or two
    @Override
    public String toString()
    {
        return getType().toString();
    }

    // TODO only hashCode() but not equals() is overridden. This implementation
    // makes all Creatures(Critters) of the same CreatureType(Creature) equal,
    // which seems highly suspicious
    @Override
    public int hashCode()
    {
        return getType().hashCode();
    }
}
