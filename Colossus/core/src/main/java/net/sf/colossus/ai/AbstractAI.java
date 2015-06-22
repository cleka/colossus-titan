package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.colossus.ai.helper.BattleEvalConstants;
import net.sf.colossus.ai.helper.LegionMove;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Battle;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattleStrike;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.DevRandom;
import net.sf.colossus.util.Probs;
import net.sf.colossus.util.ValueRecorder;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.variant.IHintOracle;
import net.sf.colossus.variant.IVariantHint;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Abstract implementation of the Colossus AI interface.
 * This class should hold most of the helper functions.
 * Ideally, most (all) "data-gathering" functions from the AIs
 * should be here, mostly as "final protected". AIs should mostly
 * only use information gathered from here to make decisions.
 * There's still a LOT of work to do...
 *
 * @author Romain Dolbeau
 * Also contains extracted code from SimpleAI:
 * @author Bruce Sherrod, David Ripton, Romain Dolbeau
 * Also contains extracted code from RationalAI:
 * @author Bruce Sherrod, David Ripton, Romain Dolbeau, Corwin Joy
 */
public abstract class AbstractAI implements AI
{
    final static private Logger LOGGER = Logger.getLogger(AbstractAI.class
        .getName());

    final public BattleEvalConstants bec = new BattleEvalConstants();
    final protected CreatureValueConstants cvc = new CreatureValueConstants();
    /** The Client we're working for. */
    final protected Client client;
    protected Variant variant;

    /** Our random source. */
    final protected Random random = new DevRandom();
    /**
     * For the Oracle Hint stuff, the play style we use.
     *
     * This can be replaced by AI implementation.
     */
    protected List<IVariantHint.AIStyle> hintSectionUsed = Collections
        .singletonList(IVariantHint.AIStyle.Offensive);

    protected AbstractAI(Client client)
    {
        this.client = client;
    }

    public void setVariant(Variant v)
    {
        this.variant = v;
    }

    final public CreatureType getVariantRecruitHint(LegionClientSide legion,
        MasterHex hex, List<CreatureType> recruits)
    {
        return VariantSupport.getRecruitHint(hex.getTerrain(), legion,
            recruits, new AbstractAIOracle(legion, hex, recruits),
            hintSectionUsed);
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
        for (Player enemyPlayer : client.getGameClientSide().getPlayers())
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

    final protected int getNumberOfWaysToTerrain(Legion legion, MasterHex hex,
        String terrainTypeName)
    {
        int total = 0;
        for (int roll = 1; roll <= 6; roll++)
        {
            Set<MasterHex> tempset = client.getMovement().listAllMoves(legion,
                hex, roll, true);
            if (doesSetContainHexWithTerrain(tempset, terrainTypeName))
            {
                total++;
            }
        }
        return total;
    }

    final private boolean doesSetContainHexWithTerrain(Set<MasterHex> set,
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
    final public Map<BattleHex, Integer> findStrikeMap()
    {
        Map<BattleHex, Integer> map = new HashMap<BattleHex, Integer>();
        for (BattleCritter critter : client.getActiveBattleUnits())
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
    final protected Map<BattleCritter, Double> generateDamageMap()
    {
        Map<BattleCritter, Double> map = new HashMap<BattleCritter, Double>();
        for (BattleCritter critter : client.getActiveBattleUnits())
        {
            // Offboard critters can't strike.
            if (critter.getCurrentHex().getLabel().startsWith("X"))
            {
                continue;
            }
            Set<BattleHex> set = client.findStrikes(critter.getTag());
            for (BattleHex targetHex : set)
            {
                BattleCritter target = getBattleUnit(targetHex);
                int dice = getBattleStrike().getDice(critter, target);
                int strikeNumber = getBattleStrike().getStrikeNumber(critter,
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
        return VariantSupport.getInitialSplitHint(hex, hintSectionUsed);
    }

    /** Get the 'kill value' of a creature on a specific terrain.
     * @param battleCritter The BattleCritter whose value is requested.
     * @param terrain The terrain on which the value is requested, or null.
     * @return The 'kill value' value of the critter, on terrain if non-null
     */
    public int getKillValue(final BattleCritter battleCritter,
        final MasterBoardTerrain terrain)
    {
        return getKillValue(battleCritter.getType(), terrain);
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
     * Shortcut to ask for the acquirables basic value from the variant
     * @link #Variant.getAcquirableRecruitmentsValue()
     *
     * @return The acquirableRecruitmentsValue
     */
    protected int getAcqStepValue()
    {
        return variant.getAcquirableRecruitmentsValue();
    }

    /**
     * Return true if the legion could recruit or acquire something
     * better than its worst creature in hexLabel.
     */
    final protected boolean couldRecruitUp(Legion legion, MasterHex hex,
        Legion enemy)
    {
        CreatureType weakest = legion.getCreatureTypes().get(
            legion.getHeight() - 1);
        // Consider recruiting.
        List<CreatureType> recruits = client.findEligibleRecruits(legion, hex);
        if (!recruits.isEmpty())
        {
            CreatureType bestRecruit = recruits.get(recruits.size() - 1);
            if (bestRecruit != null
                && getHintedRecruitmentValue(bestRecruit, legion,
                    hintSectionUsed) > getHintedRecruitmentValue(weakest,
                    legion, hintSectionUsed))
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
            int arv = getAcqStepValue();
            int nextScore = ((currentScore / arv) + 1) * arv;
            CreatureType bestRecruit = null;
            while ((currentScore + pointValue) >= nextScore)
            {
                List<String> ral = variant.getRecruitableAcquirableList(
                    hex.getTerrain(), nextScore);
                for (String creatureName : ral)
                {
                    CreatureType tempRecruit = variant
                        .getCreatureByName(creatureName);
                    if ((bestRecruit == null)
                        || (getHintedRecruitmentValue(tempRecruit, legion,
                            hintSectionUsed) >= getHintedRecruitmentValue(
                            bestRecruit, legion, hintSectionUsed)))
                    {
                        bestRecruit = tempRecruit;
                    }
                }
                nextScore += arv;
            }
            if (bestRecruit != null
                && getHintedRecruitmentValue(bestRecruit, legion,
                    hintSectionUsed) > getHintedRecruitmentValue(weakest,
                    legion, hintSectionUsed))
            {
                return true;
            }
        }
        return false;
    }

    // The NonTitan forms moved here from variant.CreatureType so that variant
    // does not (just for those two calls from VariantSupport) depend on
    // server any more. AIs are the only users of that functionality anyway.

    public int getHintedRecruitmentValueNonTitan(CreatureType creature)
    {
        return creature.getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(creature);
    }

    public int getHintedRecruitmentValueNonTitan(CreatureType creature,
        List<IVariantHint.AIStyle> styles)
    {
        return creature.getPointValue()
            + VariantSupport.getHintedRecruitmentValueOffset(creature, styles);
    }

    protected final int getHintedRecruitmentValue(CreatureType creature,
        Legion legion, List<IVariantHint.AIStyle> styles)
    {
        if (!creature.isTitan())
        {
            return getHintedRecruitmentValueNonTitan(creature, styles);
        }
        Player player = legion.getPlayer();
        int power = player.getTitanPower();
        int skill = creature.getSkill();
        return power * skill
            * VariantSupport.getHintedRecruitmentValueOffset(creature, styles);
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

    final protected boolean hasOpponentNativeCreature(HazardTerrain terrain)
    {
        boolean honc = false;
        for (BattleCritter critter : client.getInactiveBattleUnits())
        {
            if (critter.getType().isNativeIn(terrain))
            {
                honc = true;
                break;
            }
        }
        LOGGER.finest("Opponent " + (honc ? "has" : "doesn't have")
            + " native(s) from " + terrain.getName());
        return honc;
    }

    final protected int rangeToClosestOpponent(final BattleHex hex)
    {
        int range = Constants.BIGNUM;
        for (BattleCritter critter : client.getInactiveBattleUnits())
        {
            BattleHex hex2 = critter.getCurrentHex();
            int r = Battle.getRange(hex, hex2, false);
            if (r < range)
                range = r;
        }
        return range;
    }

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
    final public static LegionMove makeLegionMove(int[] indexes,
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
    final private boolean trimCritterMoves(
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

    protected class AbstractAIOracle implements IHintOracle
    {

        private final LegionClientSide legion;
        private final MasterHex hex;
        private final List<CreatureType> recruits;
        private Map<MasterHex, List<Legion>>[] enemyAttackMap = null;

        AbstractAIOracle(LegionClientSide legion, MasterHex hex,
            List<CreatureType> recruits)
        {
            this.legion = legion;
            this.hex = hex;
            this.recruits = recruits;

        }

        public boolean canReach(String terrainTypeName)
        {
            int now = getNumberOfWaysToTerrain(legion, hex, terrainTypeName);
            return (now > 0);
        }

        public int creatureAvailable(String name)
        {
            return creatureAvailable(variant.getCreatureByName(name));
        }

        public int creatureAvailable(CreatureType creatureType)
        {
            return client.getReservedRemain(creatureType);
        }

        public boolean canRecruit(String name)
        {
            return recruits.contains(variant.getCreatureByName(name));
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

        MoveInfo(Legion legion, MasterHex hex, int value, int difference,
            ValueRecorder why)
        {
            this.legion = legion;
            this.hex = hex;
            this.value = value;
            this.difference = difference;
            this.why = why;
        }
    }

    public void initBattle()
    {
        LOGGER.finer("A battle started.");
    }

    public void cleanupBattle()
    {
        LOGGER.finer("A battle is finished.");
    }

    // TODO get directly, not via Client
    protected BattleUnit getBattleUnit(BattleHex hex)
    {
        return client.getBattleCS().getBattleUnit(hex);
    }

    public BattleStrike getBattleStrike()
    {
        return client.getGame().getBattleStrike();
    }

    final public int countCreatureAccrossAllLegionFromPlayer(Creature creature)
    {
        Player player = creature.getPlayer();
        int count = 0;

        for (Legion legion : player.getLegions())
        {
            for (Creature creature2 : legion.getCreatures())
            {
                if (creature2.getType().equals(creature.getType()))
                {
                    count++;
                }
            }
        }
        return count;
    }

    public Caretaker getCaretaker()
    {
        return client.getGame().getCaretaker();
    }
}