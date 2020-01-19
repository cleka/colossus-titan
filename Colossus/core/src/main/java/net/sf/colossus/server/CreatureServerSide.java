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
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardHexside;


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

    public Game getGame()
    {
        return game;
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

    void undoMove()
    {
        setCurrentHex(getStartingHex());
        LOGGER.log(Level.INFO, getName() + " undoes move and returns to "
            + getStartingHex());
    }

    // TODO change to deal with target critters, instead of hexes?
    // Need to check also client side copies of similar functionality ;
    // on first glance looks they would be better with creatures as well.
    boolean canStrike(Creature target)
    {
        return battle.findTargetHexes(this, true).contains(
            target.getCurrentHex());
    }

    /** Calculate number of dice and strike number needed to hit target,
     *  and whether any carries and strike penalties are possible.
     *  The actual striking is now deferred to strike2(). */
    void strike(CreatureServerSide target)
    {
        battle.leaveCarryMode();
        carryPossible = true;
        if (battle.numInContact(this, false) < 2)
        {
            carryPossible = false;
        }

        int strikeNumber = game.getBattleStrikeSS().getStrikeNumber(this,
            target);
        int dice = game.getBattleStrikeSS().getDice(this, target);

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
                game.getServer().askChooseStrikePenalty(penaltyOptions);
                return;
            }
        }
        strike2(target, dice, strikeNumber);
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
            CreatureServerSide target = (CreatureServerSide)po.getTarget();
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
        if (game.getBattleStrikeSS().getDice(this, target) <= target
            .getPower() - target.getHits())
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
            PenaltyOption po = new PenaltyOption(game, this, target, game
                .getBattleStrikeSS().getDice(this, target), game
                .getBattleStrikeSS().getStrikeNumber(this, target));
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
        final int dice = game.getBattleStrikeSS().getDice(this, target);
        final int strikeNumber = game.getBattleStrikeSS().getStrikeNumber(
            this, target);

        CreatureServerSide victim = battle.getCreatureSS(neighbor);
        if (victim == null || victim.getPlayer() == getPlayer()
            || victim.isDead())
        {
            return;
        }
        int tmpDice = game.getBattleStrikeSS().getDice(this, victim);
        int tmpStrikeNumber = game.getBattleStrikeSS().getStrikeNumber(this,
            victim);

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
            PenaltyOption po = new PenaltyOption(game, this, target, tmpDice,
                tmpStrikeNumber);
            po.addCarryTarget(neighbor);
            penaltyOptions.add(po);
        }
    }

    /** Called after strike penalties are chosen.
     *  Roll the dice and apply damage.  Highlight legal carry targets. */
    private void strike2(CreatureServerSide target, int dice, int strikeNumber)
    {
        List<String> rolls = new ArrayList<String>();

        int damage;
        if (game.getOption(Options.pbBattleHits))
        {
            /**
             * Probability-based Battle Rolls:
             *
             * If set to true, give exactly (at least) the amount of hits as one could get
             * according to probability. E.g. for 6 dice and strike nr 4, 3 hits,
             * 6 dice and strike number 6 gives 1 hit.
             * Includes "accumulated wasted luck per creature", i.e. if a centaur
             * yields 1.5 wasted luck = 0.5, next time accumulated WL = 1
             * => give one hit more and subtract 1.0 from AWL.
             */

            damage = game.getBattleStrike().determineProbabilityBasedHits(
                this, target, dice, strikeNumber, rolls);
        }
        else
        {
            // Whether the rolling should take random number or from the sequence
            boolean randomized = !game
                .getOption(Options.fixedSequenceBattleDice);

            // Roll the dice:
            damage = game.getBattleStrike().rollDice(this, target, dice,
                strikeNumber, rolls, randomized);
        }

        int carryDamage = target.adjustHits(damage);
        if (!carryPossible)
        {
            carryDamage = 0;
        }
        battle.setCarryDamage(carryDamage);

        if (damage > 0)
        {
            // If damaged creature, then may have poisoned
            // or slowed it as well.
            int poison = getPoison();
            if (poison != 0)
            {
                target.addPoisonDamage(poison);
            }
            int slow = getSlows();
            if (slow != 0)
            {
                target.addSlowed(slow);
            }
        }

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
