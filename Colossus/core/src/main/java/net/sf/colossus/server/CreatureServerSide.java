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

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
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
 * @author David Ripton
 * @author Romain Dolbeau
 */
public class CreatureServerSide extends Creature implements BattleCritter
{
    private static final Logger LOGGER = Logger
        .getLogger(CreatureServerSide.class.getName());
    // TODO the creature would probably be better off not knowing
    // about the battle
    private BattleServerSide battle;

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
        super(creature, legion);
        assert game != null : "No server-side creature without a game";
        this.game = game;
        tag = ++tagCounter;
    }

    void setBattleInfo(BattleHex currentHex, BattleHex startingHex,
        BattleServerSide battle)
    {
        setCurrentHex(currentHex);
        setStartingHex(startingHex);
        this.battle = battle;
    }

    void setLegion(LegionServerSide legion)
    {
        this.legion = legion;
    }

    public int getTag()
    {
        return tag;
    }

    public boolean isDefender()
    {
        assert legion != null : "Legion must be set when calling isDefender()!";

        if (legion.equals(battle.getDefendingLegion()))
        {
            return true;
        }
        else if (legion.equals(battle.getAttackingLegion()))
        {
            return false;
        }
        else
        {
            LOGGER
                .severe("this creatures legion is neither attacking nor defending legion?");
            return false;
        }
    }

    BattleServerSide getBattle()
    {
        return battle;
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
                    CreatureServerSide other = battle.getCreatureSS(neighbor);
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
                    CreatureServerSide other = battle.getCreatureSS(neighbor);
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
        super.moveToHex(hexLabel);
        if (tellClients)
        {
            battle.getGame().getServer().allTellBattleMove(tag,
                getStartingHex(), getCurrentHex(), false);
        }
    }

    void undoMove()
    {
        BattleHex formerHexLabel = getCurrentHex();
        setCurrentHex(getStartingHex());
        LOGGER.log(Level.INFO, getName() + " undoes move and returns to "
            + getStartingHex());
        battle.getGame().getServer().allTellBattleMove(tag, formerHexLabel,
            getCurrentHex(), true);
    }

    // TODO change to deal with target critters, instead of hexes?
    // Need to check also client side copies of similar functionality ;
    // on first glance looks they would be better with creatures as well.
    boolean canStrike(Creature target)
    {
        return battle.findTargetHexes(this, true).contains(
            target.getCurrentHex());
    }

    /** Return the number of dice that will be rolled when striking this
     *  target, including modifications for terrain.
     * WARNING: this is duplicated in BattleClientSide
     */
    @SuppressWarnings("deprecation")
    protected int getDice(Creature target)
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

            /* TODO: remove TEST TEST TEST TEST TEST */
            /* getStrikingPower should be used instead of the logic above, but
             * 1) I'm not sure everyone will agree it belongs in Creature
             * 2) I haven't had time to verify it's correct.
             * Incidentally, if you're reading this after noticing the warning
             * below in your logfile, then it isn't correct ;-)
             */
            int checkStrikingPower = getStrikingPower(target, hex
                .getElevation(), targetHex.getElevation(), hex.getTerrain(),
                targetHex.getTerrain(), hex.getHexsideHazard(BattleServerSide
                    .getDirection(hex, targetHex, false)), targetHex
                    .getHexsideHazard(BattleServerSide.getDirection(targetHex,
                        hex, false)));

            if (checkStrikingPower != dice)
            {
                LOGGER.warning("attackerPower says " + dice
                    + " but checkStrikingPower says " + checkStrikingPower);
            }
            /* END TODO: remove TEST TEST TEST TEST TEST */
        }

        return dice;
    }

    /** WARNING: this is duplicated in BattleClientSide */
    @SuppressWarnings("deprecation")
    private int getAttackerSkill(Creature target)
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
                if (hazard.equals(HazardHexside.TOWER))
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
                if ((hazard.equals(HazardHexside.SLOPE) && !isNativeSlope())
                    || hazard.equals(HazardHexside.TOWER))
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
                .getElevation(), targetHex.getElevation(), hex.getTerrain(),
                targetHex.getTerrain(), hex.getHexsideHazard(BattleServerSide
                    .getDirection(hex, targetHex, false)), targetHex
                    .getHexsideHazard(BattleServerSide.getDirection(targetHex,
                        hex, false)));

            if (checkStrikingSkill != attackerSkill)
            {
                LOGGER
                    .warning(String
                        .format(
                            "For creature %s striking %s from %s(%d) to %s(%d) via %s/%s, "
                                + "we calculated %d as attacker skill, but getStrikingSkill says %d",
                            this, target, hex.getTerrain(), Integer
                                .valueOf(hex.getElevation()), targetHex
                                .getTerrain(), Integer.valueOf(targetHex
                                .getElevation()), hex
                                .getHexsideHazard(BattleServerSide
                                    .getDirection(hex, targetHex, false)),
                            targetHex.getHexsideHazard(BattleServerSide
                                .getDirection(targetHex, hex, false)), Integer
                                .valueOf(attackerSkill), Integer
                                .valueOf(checkStrikingSkill)));
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

    /** WARNING: this is duplicated in BattleClientSide */
    @SuppressWarnings("deprecation")
    protected int getStrikeNumber(Creature target)
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

        CreatureServerSide victim = battle.getCreatureSS(neighbor);
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

        LOGGER.log(Level.INFO, getName() + " in " + getCurrentHex()
            + " strikes " + target.getDescription() + " with strike number "
            + strikeNumber + ", rolling: " + rollString + ": " + damage
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
