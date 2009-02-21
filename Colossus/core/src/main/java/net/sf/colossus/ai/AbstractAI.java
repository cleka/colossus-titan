package net.sf.colossus.ai;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.CritterMove;

/**
 * Abstract implementation of the Colossus AI interface.
 * This class should hold most of the helper functions.
 * 
 * @version $Id: SimpleAI.java 3556 2009-02-20 08:37:40Z dolbeau $
 * @author Bruce Sherrod, David Ripton
 * @author Romain Dolbeau
 */
abstract public class AbstractAI implements AI
{    
    protected BattleEvalConstants bec = new BattleEvalConstants();

    /** Various constans used by the AIs code.
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
        int TITAN_BY_EDGE_OR_TREE_BONUS = 400;
        int DEFENDER_FORWARD_EARLY_PENALTY = -60;
        int ATTACKER_DISTANCE_FROM_ENEMY_PENALTY = -300;
        int ADJACENT_TO_BUDDY = 100;
        int ADJACENT_TO_BUDDY_TITAN = 600; // 200
        int GANG_UP_ON_CREATURE = 50;
        /* per legion */
        int DEF__NOBODY_GETS_HURT = 2000;
        int DEF__NOONE_IS_GANGBANGED = 200;
        int DEF__AT_MOST_ONE_IS_REACHABLE = 100;
    }


    private static final Logger LOGGER = Logger.getLogger(AbstractAI.class.getName());

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
    final Collection<LegionMove> generateLegionMoves(
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

        nestForLoop(indexes, indexes.length, critterMoves, legionMoves, forceAll);

        LOGGER.finest("findLegionMoves got " + legionMoves.size() + " legion moves");
        return legionMoves;
    }

    private final Set<String> duplicateHexChecker = new HashSet<String>();
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
    private final void nestForLoop(int[] indexes, final int level,
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
                String endingHexLabel = cm.getEndingHexLabel();
                if (endingHexLabel.startsWith("X"))
                {
                    offboard = true;
                }
                else if (duplicateHexChecker.contains(endingHexLabel))
                {
                    // Need to allow duplicate offboard moves, in case 2 or
                    // more creatures cannot enter.
                    return;
                }
                duplicateHexChecker.add(cm.getEndingHexLabel());
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
            int howmany = critterMoves.get(level-1).size();
            int size = critterMoves.size();
            // try and limit combinatorial explosion
            // criterions here:
            // 1) at least level moves per creatures (if possible!)
            // 2) not too many moves in total...
            int thresh = level + 1; // default: a bit more than the minimum
            if (size < 5)
                thresh = level + 16;
            else if (size < 6)
                thresh = level + 8;
            else if (size < 7)
                thresh = level + 3;
            if (thresh < level) // safety belt... for older codes.
                thresh = level;
            if (!forceAll && (howmany > thresh))
                howmany = thresh;
            for (int i = 0; i < howmany; i++)
            {
                indexes[level-1] = i;
                nestForLoop(indexes, level - 1, critterMoves, legionMoves, forceAll);
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
    final boolean trimCritterMoves(List<List<CritterMove>> allCritterMoves)
    {
        Set<String> takenHexLabels = new HashSet<String>(); // XXX reuse?
        boolean changed = false;

        // First trim immobile creatures from the list, and add their
        // hexes to takenHexLabels.
        Iterator<List<CritterMove>> it = allCritterMoves.iterator();
        while (it.hasNext())
        {
            List<CritterMove> moveList = it.next();
            if (moveList.size() == 1)
            {
                // This critter is not mobile, and its hex is taken.
                CritterMove cm = moveList.get(0);
                takenHexLabels.add(cm.getStartingHexLabel());
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
                if (takenHexLabels.contains(cm.getEndingHexLabel()))
                {
                    it.remove();
                    changed = true;
                }
            }
        }

        return changed;
    }
}