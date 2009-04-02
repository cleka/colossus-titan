package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.gui.BattleChit;
import net.sf.colossus.server.HintOracleInterface;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.util.Probs;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Abstract implementation of the Colossus AI interface.
 * This class should hold most of the helper functions.
 * Ideally, most (all) "data-gathering" functions from the AIs
 * should be here, mostly as "final protected". AIs should mostly
 * only use information gathered from here to make decisions.
 * There's still a LOT of work to do...
 *
 * @version $Id$
 * @author Romain Dolbeau
 * Also contains extracted code from SimpleAI:
 * @author Bruce Sherrod, David Ripton, Romain Dolbeau
 * Also contains extracted code from RationalAI:
 * @author Bruce Sherrod, David Ripton, Romain Dolbeau, Corwin Joy
 */
abstract public class AbstractAI implements AI
{

    protected BattleEvalConstants bec = new BattleEvalConstants();
    protected CreatureValueConstants cvc = new CreatureValueConstants();
    /** The Client we're working for. */
    final protected Client client;
    /** Our random source. */
    final protected Random random = new DevRandom();
    /** for the Oracle Hint tuff, the section we use.
     * This can be replaced by AI implementation.
     */
    protected String[] hintSectionUsed = { Constants.sectionOffensiveAI };

    protected AbstractAI(Client client)
    {
        this.client = client;
    }

    final public CreatureType getVariantRecruitHint(LegionClientSide legion,
        MasterHex hex, List<CreatureType> recruits)
    {
        String recruitName = VariantSupport.getRecruitHint(hex.getTerrain(),
            legion, recruits, new AbstractAIOracle(legion, hex, recruits),
            hintSectionUsed);
        if (recruitName == null)
        {
            return recruits.get(recruits.size() - 1);
        }
        if ((recruitName.equals("nothing")) || (recruitName.equals("Nothing")))
        {
            // suggest recruiting nothing
            return null;
        }
        CreatureType recruit = client.getGame().getVariant()
            .getCreatureByName(recruitName);
        if (!(recruits.contains(recruit)))
        {
            LOGGER.warning("HINT: Invalid Hint for this variant !"
                + " (can\'t recruit " + recruitName + "; recruits="
                + recruits.toString() + ") in " + hex.getTerrain());
            return recruits.get(recruits.size() - 1);
        }
        return recruit;
    }

    /**
     * arrays and generics don't work well together -- TODO replace the
     *  array with a list or model some intermediate classes
     */
    @SuppressWarnings(value = "unchecked")
    final protected Map<MasterHex, List<Legion>>[] buildEnemyAttackMap(
        Player player)
    {
        Map<MasterHex, List<Legion>>[] enemyMap = (Map<MasterHex, List<Legion>>[])new HashMap<?, ?>[7];
        for (int i = 1; i <= 6; i++)
        {
            enemyMap[i] = new HashMap<MasterHex, List<Legion>>();
        }
        // for each enemy player
        for (Player enemyPlayer : client.getPlayers())
        {
            if (enemyPlayer == player)
            {
                continue;
            }
            // for each legion that player controls
            for (Legion legion : enemyPlayer.getLegions())
            {
                // for each movement roll he might make
                for (int roll = 1; roll <= 6; roll++)
                {
                    // count the moves he can get to
                    Set<MasterHex> set;
                    // Only allow Titan teleport
                    // Remember, tower teleports cannot attack
                    if (legion.hasTitan()
                        && legion.getPlayer().canTitanTeleport()
                        && client.getMovement().titanTeleportAllowed())
                    {
                        set = client.getMovement().listAllMoves(legion,
                            legion.getCurrentHex(), roll);
                    }
                    else
                    {
                        set = client.getMovement().listNormalMoves(legion,
                            legion.getCurrentHex(), roll);
                    }
                    for (MasterHex hex : set)
                    {
                        for (int effectiveRoll = roll; effectiveRoll <= 6; effectiveRoll++)
                        {
                            // legion can attack to hexlabel on a effectiveRoll
                            List<Legion> list = enemyMap[effectiveRoll]
                                .get(hex);
                            if (list == null)
                            {
                                list = new ArrayList<Legion>();
                            }
                            if (list.contains(legion))
                            {
                                continue;
                            }
                            list.add(legion);
                            enemyMap[effectiveRoll].put(hex, list);
                        }
                    }
                }
            }
        }
        return enemyMap;
    }

    final protected int getNumberOfWaysToTerrain(Legion legion,
        MasterHex hex, String terrainTypeName)
    {
        int total = 0;
        for (int roll = 1; roll <= 6; roll++)
        {
            Set<MasterHex> tempset = client.getMovement().listAllMoves(legion,
                hex, roll, true);
            if (doesSetContainsHexWithTerrain(tempset, terrainTypeName))
            {
                total++;
            }
        }
        return total;
    }

    final protected boolean doesSetContainsHexWithTerrain(Set<MasterHex> set,
        String terrainTypeName)
    {
        for (MasterHex hex : set)
        {
            if (hex.getTerrain().getDisplayName().equals(terrainTypeName))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a map of target hex label to number
     * of friendly creatures that can strike it
     */
    final protected Map<BattleHex, Integer> findStrikeMap()
    {
        Map<BattleHex, Integer> map = new HashMap<BattleHex, Integer>();
        for (BattleChit critter : client.getActiveBattleChits())
        {
            Set<BattleHex> targets = client.findStrikes(critter.getTag());
            for (BattleHex targetHex : targets)
            {
                Integer old = map.get(targetHex);
                if (old == null)
                {
                    map.put(targetHex, Integer.valueOf(1));
                }
                else
                {
                    map.put(targetHex, Integer.valueOf(old.intValue() + 1));
                }
            }
        }
        return map;
    }

    /**
     * Create a map containing each target and the number of hits it would
     * likely take if all possible creatures attacked it.
     */
    final protected Map<BattleChit, Double> generateDamageMap()
    {
        Map<BattleChit, Double> map = new HashMap<BattleChit, Double>();
        for (BattleChit critter : client.getActiveBattleChits())
        {
            // Offboard critters can't strike.
            if (critter.getCurrentHex().getLabel().startsWith("X"))
            {
                continue;
            }
            Set<BattleHex> set = client.findStrikes(critter.getTag());
            for (BattleHex targetHex: set)
            {
                BattleChit target = client.getBattleChit(targetHex);
                int dice = client.getStrike().getDice(critter, target);
                int strikeNumber = client.getStrike().getStrikeNumber(critter,
                    target);
                double h = Probs.meanHits(dice, strikeNumber);
                if (map.containsKey(target))
                {
                    double d = map.get(target).doubleValue();
                    h += d;
                }
                map.put(target, new Double(h));
            }
        }
        return map;
    }

    /** Return which creature the variant suggest splitting at turn 1 when
     * starting in a specific hex.
     * @param hex The masterboard hex where the split occurs.
     * @return The List of Creaturetype to split.
     */
    final protected List<CreatureType> getInitialSplitHint(MasterHex hex)
    {
        List<String> byName = VariantSupport.getInitialSplitHint(hex,
            hintSectionUsed);
        if (byName == null)
        {
            return null;
        }
        List<CreatureType> byCreature = new ArrayList<CreatureType>();
        for (String name : byName)
        {
            CreatureType cre = client.getGame().getVariant()
                .getCreatureByName(name);
            if (cre == null)
            {
                LOGGER.severe("HINT: Unknown creature in hint (" + name
                    + "), aborting.");
                return null;
            }
            byCreature.add(cre);
        }
        return byCreature;
    }

    /** Get the 'kill value' of a creature on a specific terrain.
     * @param chit The BattleChit whose value is requested.
     * @param terrain The terrain on which the value is requested, or null.
     * @return The 'kill value' value of chit, on terrain if non-null
     */
    protected int getKillValue(final BattleChit chit,
        final MasterBoardTerrain terrain)
    {
        return getKillValue(chit.getCreature(), terrain);
    }

    /** Get the 'kill value' of a creature on an unspecified terrain.
     * @param creature The CreatureType whose value is requested.
     * @return The 'kill value' value of creature.
     */
    protected int getKillValue(final CreatureType creature)
    {
        return getKillValue(creature, null);
    }

    /** Get the 'kill value' of a creature on a specific terrain.
     * @param creature The CreatureType whose value is requested.
     * @param terrain The terrain on which the value is requested, or null
     * @return The 'kill value' value of chit, on terrain if non-null
     */
    private int getKillValue(final CreatureType creature,
        MasterBoardTerrain terrain)
    {
        int val;
        if (creature == null)
        {
            LOGGER.warning("Called getKillValue with null creature");
            return 0;
        }
        // get non-terrain modified part of kill value
        val = creature.getKillValue();
        // modify with terrain
        if (terrain != null && terrain.hasNativeCombatBonus(creature))
        {
            val += cvc.HAS_NATIVE_COMBAT_BONUS;
        }
        return val;
    }

    /**
     * Return true if the legion could recruit or acquire something
     * better than its worst creature in hexLabel.
     */
    final protected boolean couldRecruitUp(Legion legion, MasterHex hex, Legion enemy)
    {
        CreatureType weakest = client.getGame().getVariant().
                getCreatureByName(((LegionClientSide) legion).getContents().
                get(legion.getHeight() - 1));
        // Consider recruiting.
        List<CreatureType> recruits = client.findEligibleRecruits(legion, hex);
        if (!recruits.isEmpty())
        {
            CreatureType bestRecruit = recruits.get(recruits.size() - 1);
            if (bestRecruit != null &&
                    getHintedRecruitmentValue(bestRecruit, legion,
                    hintSectionUsed) >
                    getHintedRecruitmentValue(weakest, legion, hintSectionUsed))
            {
                return true;
            }
        }
        // Consider acquiring angels.
        if (enemy != null)
        {
            int pointValue = enemy.getPointValue();
            boolean wouldFlee = flee(enemy, legion);
            if (wouldFlee)
            {
                pointValue /= 2;
            }
            // should work with all variants
            int currentScore = legion.getPlayer().getScore();
            int arv = TerrainRecruitLoader.getAcquirableRecruitmentsValue();
            int nextScore = ((currentScore / arv) + 1) * arv;
            CreatureType bestRecruit = null;
            while ((currentScore + pointValue) >= nextScore)
            {
                List<String> ral =
                        TerrainRecruitLoader.getRecruitableAcquirableList(hex.getTerrain(),
                        nextScore);
                for (String creatureName : ral)
                {
                    CreatureType tempRecruit =
                            client.getGame().getVariant().
                            getCreatureByName(creatureName);
                    if ((bestRecruit == null) ||
                            (getHintedRecruitmentValue(tempRecruit, legion,
                            hintSectionUsed) >=
                            getHintedRecruitmentValue(bestRecruit, legion,
                            hintSectionUsed)))
                    {
                        bestRecruit = tempRecruit;
                    }
                }
                nextScore += arv;
            }
            if (bestRecruit != null &&
                    getHintedRecruitmentValue(bestRecruit, legion,
                    hintSectionUsed) >
                    getHintedRecruitmentValue(weakest, legion, hintSectionUsed))
            {
                return true;
            }
        }
        return false;
    }

    protected final int getHintedRecruitmentValue(CreatureType creature,
            Legion legion, String[] section)
    {
        if (!creature.isTitan())
        {
            return creature.getHintedRecruitmentValue(section);
        }
        Player player = legion.getPlayer();
        int power = player.getTitanPower();
        int skill = creature.getSkill();
        return power * skill *
                VariantSupport.getHintedRecruitmentValueOffset(creature.getName(),
                section);
    }

    /** Various constants used by the AIs code for battle evaluation.
     * Each specific AI should be able to override them
     * to tweak the evaluation results w/o rewriting the code.
     */
    protected class BattleEvalConstants
    {
        /* per critter */

        int OFFBOARD_DEATH_SCALE_FACTOR = -150;
        int NATIVE_BONUS_TERRAIN = 40; // 50 -- old value
        int NATIVE_BOG = 20;
        int NON_NATIVE_PENALTY_TERRAIN = -100;
        int PENALTY_DAMAGE_TERRAIN = -200;
        int FIRST_RANGESTRIKE_TARGET = 300;
        int EXTRA_RANGESTRIKE_TARGET = 100;
        int RANGESTRIKE_TITAN = 500;
        int RANGESTRIKE_WITHOUT_PENALTY = 100;
        int ATTACKER_ADJACENT_TO_ENEMY = 400;
        int DEFENDER_ADJACENT_TO_ENEMY = -20;
        int ADJACENT_TO_ENEMY_TITAN = 1300;
        int ADJACENT_TO_RANGESTRIKER = 500;
        int ATTACKER_KILL_SCALE_FACTOR = 25; // 100
        int DEFENDER_KILL_SCALE_FACTOR = 1; // 100
        int KILLABLE_TARGETS_SCALE_FACTOR = 0; // 10
        int ATTACKER_GET_KILLED_SCALE_FACTOR = -20;
        int DEFENDER_GET_KILLED_SCALE_FACTOR = -40;
        int ATTACKER_GET_HIT_SCALE_FACTOR = -1;
        int DEFENDER_GET_HIT_SCALE_FACTOR = -2;
        int TITAN_TOWER_HEIGHT_BONUS = 2000;
        int DEFENDER_TOWER_HEIGHT_BONUS = 80;
        int TITAN_FORWARD_EARLY_PENALTY = -10000;
        int TITAN_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 400;
        int DEFENDER_BY_EDGE_OR_BLOCKINGHAZARD_BONUS = 1;
        int DEFENDER_FORWARD_EARLY_PENALTY = -60;
        int ATTACKER_DISTANCE_FROM_ENEMY_PENALTY = -300;
        int ADJACENT_TO_BUDDY = 100;
        int ADJACENT_TO_BUDDY_TITAN = 600; // 200
        int GANG_UP_ON_CREATURE = 50;
        /* per legion */
        /** Bonus when no defender will be reachable by the attacker
         * next half-turn.
         */
        int DEF__NOBODY_GETS_HURT = 2000;
        /** Bonus when no defender will be reachable by more than one
         * attacker next half-turn.
         */
        int DEF__NOONE_IS_GANGBANGED = 200;
        /** Bonus when at most one defender will be reachable by the
         * attacker next half-turn.
         */
        int DEF__AT_MOST_ONE_IS_REACHABLE = 100;
    }

    /** Various constants used by the AIs code for creature evaluation.
     * Each specific AI should be able to override them
     * to tweak the evaluation results w/o rewriting the code.
     */
    protected class CreatureValueConstants
    {

        /** Bonus to the 'kill value' when the terrain offer a bonus
         * in combat to the creature.
         * 0 by default, so the default 'kill value' is the 'kill value'
         * returned by the creature type.
         * SimpleAI (and all its subclasses) override this to 3.
         */
        int HAS_NATIVE_COMBAT_BONUS = 0;
    }

    /** Test whether a Legion belongs to a Human player */
    final protected boolean isHumanLegion(Legion legion)
    {
        return !legion.getPlayer().isAI();
    }

    /** Test whether a Legion belongs to an AI player */
    final protected boolean isAILegion(Legion legion)
    {
        return legion.getPlayer().isAI();
    }

    /** Get the variant played */
    final protected Variant getVariantPlayed()
    {
        return this.client.getGame().getVariant();
    }

    final protected boolean hasOpponentNativeCreature(HazardTerrain terrain)
    {
        boolean honc = false;
        for (BattleChit critter : client.getInactiveBattleChits())
        {
            if (critter.getCreature().isNativeIn(terrain))
            {
                honc = true;
                break;
            }
        }
        return honc;
    }

    final protected int rangeToClosestOpponent(final BattleHex hex) {
        int range = Constants.BIGNUM;
        for (BattleChit critter : client.getInactiveBattleChits())
        {
            BattleHex hex2 = critter.getCurrentHex();
            int r = Battle.getRange(hex, hex2, false);
            if (r < range)
                range = r;
        }
        return range;
    }

    final static private Logger LOGGER = Logger.getLogger(AbstractAI.class
        .getName());

    /** allCritterMoves is a List of sorted MoveLists.  A MoveList is a
     *  sorted List of CritterMoves for one critter.  Return a sorted List
     *  of LegionMoves.  A LegionMove is a List of one CritterMove per
     *  mobile critter in the legion, where no two critters move to the
     *  same hex.
     *  This implementation try to build a near-exhaustive List of all
     *  possible moves. It will be fully exhaustive if forceAll is true.
     *  Otherwise, it will try to limit to a reasonable number (the exact
     *  algorithm is in nestForLoop)
     */
    final protected Collection<LegionMove> generateLegionMoves(
        final List<List<CritterMove>> allCritterMoves, boolean forceAll)
    {
        List<List<CritterMove>> critterMoves = new ArrayList<List<CritterMove>>(
            allCritterMoves);
        while (trimCritterMoves(critterMoves))
        {// Just trimming
        }

        // Now that the list is as small as possible, start finding combos.
        List<LegionMove> legionMoves = new ArrayList<LegionMove>();
        int[] indexes = new int[critterMoves.size()];

        nestForLoop(indexes, indexes.length, critterMoves, legionMoves,
            forceAll);

        LOGGER.finest("generateLegionMoves got " + legionMoves.size()
            + " legion moves");
        return legionMoves;
    }

    /** Set of hex name, to check for duplicates.
     * I assume the reason it is a class variable and not a local variable
     * inside the function is performance (avoiding creation/recreation).
     */
    final private Set<BattleHex> duplicateHexChecker = new HashSet<BattleHex>();

    /** Private helper for generateLegionMoves
     *  If forceAll is true, generate all possible moves. Otherwise,
     *  this function tries to limit the number of moves.
     *  This function uses an intermediate array of indexes (called
     *  and this is not a surprise, 'indexes') using recursion.
     *  The minimum number of try should be the level (level one
     *  is the most important creature and should always get his
     *  own favorite spot, higher levels need to be able to fall back
     *  on a not-so-good choice).
     */
    final private void nestForLoop(int[] indexes, final int level,
        final List<List<CritterMove>> critterMoves,
        List<LegionMove> legionMoves, boolean forceAll)
    {
        // TODO See if doing the set test at every level is faster than
        // always going down to level 0 then checking.
        if (level == 0)
        {
            duplicateHexChecker.clear();
            boolean offboard = false;
            for (int j = 0; j < indexes.length; j++)
            {
                List<CritterMove> moveList = critterMoves.get(j);
                if (indexes[j] >= moveList.size())
                {
                    return;
                }
                CritterMove cm = moveList.get(indexes[j]);
                BattleHex endingHex = cm.getEndingHex();
                if (endingHex.getLabel().startsWith("X"))
                {
                    offboard = true;
                }
                else if (duplicateHexChecker.contains(endingHex))
                {
                    // Need to allow duplicate offboard moves, in case 2 or
                    // more creatures cannot enter.
                    return;
                }
                duplicateHexChecker.add(cm.getEndingHex());
            }

            LegionMove lm = makeLegionMove(indexes, critterMoves);
            // Put offboard moves last, so they'll be skipped if the AI
            // runs out of time.
            if (offboard)
            {
                legionMoves.add(lm);
            }
            else
            {
                legionMoves.add(0, lm);
            }
        }
        else
        {
            int howmany = critterMoves.get(level - 1).size();
            int size = critterMoves.size();
            // try and limit combinatorial explosion
            // criterions here:
            // 1) at least level moves per creatures (if possible!)
            // 2) not too many moves in total...
            int thresh = level + 1; // default: a bit more than the minimum
            if (size < 5)
            {
                thresh = level + 16;
            }
            else if (size < 6)
            {
                thresh = level + 8;
            }
            else if (size < 7)
            {
                thresh = level + 3;
            }
            if (thresh < level) // safety belt... for older codes.
            {
                thresh = level;
            }
            if (!forceAll && (howmany > thresh))
            {
                howmany = thresh;
            }
            for (int i = 0; i < howmany; i++)
            {
                indexes[level - 1] = i;
                nestForLoop(indexes, level - 1, critterMoves, legionMoves,
                    forceAll);
            }
        }
    }

    /** critterMoves is a List of sorted MoveLists. indexes is
     *  a list of indexes, one per MoveList.
     *  This return a LegionMove, made of one CritterMove per
     *  MoveList. The CritterMove is selected by the index.
     */
    final static LegionMove makeLegionMove(int[] indexes,
        List<List<CritterMove>> critterMoves)
    {
        LegionMove lm = new LegionMove();
        for (int i = 0; i < indexes.length; i++)
        {
            List<CritterMove> moveList = critterMoves.get(i);
            CritterMove cm = moveList.get(indexes[i]);
            lm.add(cm);
        }
        return lm;
    }

    /** Modify allCritterMoves in place, and return true if it changed. */
    final protected boolean trimCritterMoves(
        List<List<CritterMove>> allCritterMoves)
    {
        Set<BattleHex> takenHexes = new HashSet<BattleHex>(); // XXX reuse?
        boolean changed = false;

        // First trim immobile creatures from the list, and add their
        // hexes to takenHexes.
        Iterator<List<CritterMove>> it = allCritterMoves.iterator();
        while (it.hasNext())
        {
            List<CritterMove> moveList = it.next();
            if (moveList.size() == 1)
            {
                // This critter is not mobile, and its hex is taken.
                CritterMove cm = moveList.get(0);
                takenHexes.add(cm.getStartingHex());
                it.remove();
                changed = true;
            }
        }

        // Now trim all moves to taken hexes from all movelists.
        it = allCritterMoves.iterator();
        while (it.hasNext())
        {
            List<CritterMove> moveList = it.next();
            for (CritterMove cm : moveList)
            {
                if (takenHexes.contains(cm.getEndingHex()))
                {
                    it.remove();
                    changed = true;
                }
            }
        }

        return changed;
    }

    protected class AbstractAIOracle implements HintOracleInterface
    {

        LegionClientSide legion;
        MasterHex hex;
        List<CreatureType> recruits;
        Map<MasterHex, List<Legion>>[] enemyAttackMap = null;

        AbstractAIOracle(LegionClientSide legion, MasterHex hex,
            List<CreatureType> recruits2)
        {
            this.legion = legion;
            this.hex = hex;
            this.recruits = recruits2;

        }

        public boolean canReach(String terrainTypeName)
        {
            int now = getNumberOfWaysToTerrain(legion, hex, terrainTypeName);
            return (now > 0);
        }

        public int creatureAvailable(String name)
        {
            // TODO name doesn't seem to always refer to an actual creature
            //      type, which means the next line can return null, then
            //      causing an NPE in getReservedRemain(..)
            // Still TODO ?
            //      Fixed "Griffon vs. Griffin" in Undead, which was the
            //      reason in all cases I got that exception (Clemens).
            CreatureType type = client.getGame().getVariant()
                .getCreatureByName(name);
            int count = client.getReservedRemain(type);
            return count;
        }

        public boolean canRecruit(String name)
        {
            return recruits.contains(client.getGame().getVariant()
                .getCreatureByName(name));
        }

        public String hexLabel()
        {
            return hex.getLabel();
        }

        public int biggestAttackerHeight()
        {
            if (enemyAttackMap == null)
            {
                enemyAttackMap = buildEnemyAttackMap(client.getOwningPlayer());
            }
            int worst = 0;
            for (int i = 1; i < 6; i++)
            {
                List<Legion> enemyList = enemyAttackMap[i].get(legion
                    .getCurrentHex());
                if (enemyList != null)
                {
                    for (Legion enemy : enemyList)
                    {
                        if ((enemy).getHeight() > worst)
                        {
                            worst = (enemy).getHeight();
                        }
                    }
                }
            }
            return worst;
        }
    }

    /** little helper to store info about possible moves */
    protected class MoveInfo
    {

        final Legion legion;
        /** hex to move to.  if hex == null, then this means sit still. */
        final MasterHex hex;
        final int value;
        final int difference; // difference from sitting still
        final ValueRecorder why; // explain value

        MoveInfo(Legion legion, MasterHex hex, int value,
            int difference, ValueRecorder why)
        {
            this.legion = legion;
            this.hex = hex;
            this.value = value;
            this.difference = difference;
            this.why = why;
        }
    }

    /** An integer value, along with a detailed record of how and why
     * the value has the value it has.
     * @author Romain Dolbeau
     */
    protected class ValueRecorder
    {

        /** The current value */
        private int value = 0;
        /** All the explanations and value changes */
        private final StringBuffer why = new StringBuffer();

        /** Augment the value.
         * @param v By how much the value change.
         * @param r The reason of the change.
         */
        public void add(int v, String r)
        {
            if (why.toString().equals("") || v < 0)
            {
                why.append("" + v);
            }
            else
            {
                why.append("+" + v);
            }
            why.append(" [" + r + "]");
            value += v;
        }

        /** Reset the value to a specific value.
         * @param v The new value to use.
         * @param r The reason of the change.
         */
        public void resetTo(int v, String r)
        {
            why.append(" | " + v);
            why.append(" [ " + r + "]");
            value = v;
        }

        /** Get the value.
         * @return The current value.
         */
        public int getValue()
        {
            return value;
        }

        /** Get the detailed explanations and final value as String.
         * @return The detailed explanations and final value.
         */
        @Override
        public String toString()
        {
            return why.toString() + " = " + value;
        }
    }
}