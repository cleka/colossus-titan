package net.sf.colossus.client;


import java.util.*;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.Combos;


/**
 *  Predicts splits for one enemy player, and adjusts predictions as 
 *  creatures are revealed.
 *  @version $Id$
 *  @author David Ripton
 *  @author Kim Milvang-Jensen
 *  @see SplitPrediction.txt
 * 
 */

public class PredictSplitNode implements Comparable
{
    private final String markerId; // Not unique!
    private final int turnCreated;
    private CreatureInfoList creatures = new CreatureInfoList();

    // only if atSplit
    private CreatureInfoList removed = new CreatureInfoList();

    private final PredictSplitNode parent;
    // Size of child2 at the time this node was split.
    private int childSize2;
    private PredictSplitNode child1; // child that keeps the marker
    private PredictSplitNode child2; // child with the new marker
    private static CreatureInfoComparator cic = new CreatureInfoComparator();

    PredictSplitNode(
        String markerId,
        int turnCreated,
        CreatureInfoList cil,
        PredictSplitNode parent)
    {
        this.markerId = markerId;
        this.turnCreated = turnCreated;
        this.creatures = (CreatureInfoList)cil.clone();
        this.parent = parent;
        clearChildren();
    }

    private void clearChildren()
    {
        childSize2 = 0;
        child1 = null;
        child2 = null;
    }

    String getMarkerId()
    {
        return markerId;
    }

    String getFullName()
    {
        return markerId + '(' + turnCreated + ')';
    }

    PredictSplitNode getChild1()
    {
        return child1;
    }

    PredictSplitNode getChild2()
    {
        return child2;
    }

    PredictSplitNode getParent()
    {
        return parent;
    }

    int getTurnCreated()
    {
        return turnCreated;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(getFullName() + ":");
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            sb.append(" " + ci.toString());
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            sb.append(" " + ci.toString() + "-");
        }
        return sb.toString();
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getCreatures()
    {
        Collections.sort(creatures, cic);
        return creatures;
    }

    void setCreatures(CreatureInfoList creatures)
    {
        this.creatures = creatures;
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getRemovedCreatures()
    {
        CreatureInfoList cil = new CreatureInfoList();
        cil.addAll(removed);
        return cil;
    }

    /** Return list of CreatureInfo where certain == true. */
    CreatureInfoList getCertainCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isCertain())
            {
                list.add(ci);
            }
        }
        return list;
    }

    int numCertainCreatures()
    {
        return getCertainCreatures().size();
    }

    int numUncertainCreatures()
    {
        return getHeight() - numCertainCreatures();
    }

    boolean allCertain()
    {
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (!ci.isCertain())
            {
                return false;
            }
        }
        return true;
    }

    boolean hasSplit()
    {
        if (child1 == null && child2 == null)
        {
            return false;
        }
        if (child1 == null || child2 == null)
        {
            throw new PredictSplitsException("One child legion");
        }
        return true;
    }

    List getChildren()
    {
        List li = new ArrayList();
        if (hasSplit())
        {
            li.add(child1);
            li.add(child2);
        }
        return li;
    }

    /** Return true if all of this node's children, grandchildren, etc.
     *  have no uncertain creatures */
    boolean allDescendentsCertain()
    {
        if (child1 == null)
        {
            return true;
        }
        else
        {
            return child1.allCertain() && child2.allCertain() &&
                child1.allDescendentsCertain() &&
                child2.allDescendentsCertain();
        }
    }

    /** Return list of CreatureInfo where atSplit == true, plus removed
     *  creatures. */
    CreatureInfoList getAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            list.add(ci);
        }
        return list;
    }

    /** Return list of CreatureInfo where atSplit == false. */
    CreatureInfoList getAfterSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (!ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        return list;
    }

    /** Return list of CreatureInfo where both certain and atSplit are true,
     *  plus removed creatures. */
    CreatureInfoList getCertainAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (Iterator it = getCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isCertain() && ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            list.add(ci);
        }
        return list;
    }

    String getOtherChildMarkerId()
    {
        if (!markerId.equals(child1.getMarkerId()))
        {
            return child1.getMarkerId();
        }
        else
        {
            return child2.getMarkerId();
        }
    }

    int getHeight()
    {
        return creatures.size();
    }

    /** Return true if big is a superset of little. */
    static boolean superset(List big, List little)
    {
        List bigclone = new ArrayList(big);
        for (Iterator it = little.iterator(); it.hasNext();)
        {
            Object ob = it.next();
            if (!bigclone.remove(ob))
            {
                return false;
            }
        }
        return true;
    }

    /** Return true iff new information was sent to this legion's
     *  parent. */
    void revealCreatures(List cnl)
    {
        if (cnl == null)
        {
            // this means we are updating the parent, and the info gained is
            // computed from children
            cnl = new ArrayList();
            cnl.addAll(
                child1
                .getCertainAtSplitOrRemovedCreatures()
                .getCreatureNames());
            cnl.addAll(
                child2
                .getCertainAtSplitOrRemovedCreatures()
                .getCreatureNames());
        }

        List certainInfoGained =
            subtractLists(cnl, getCertainCreatures().getCreatureNames());

        if (!certainInfoGained.isEmpty())
        {
            for (Iterator it = certainInfoGained.iterator(); it.hasNext();)
            {
                String name = (String)it.next();
                this.creatures.add(new CreatureInfo(name, true, true));
            }
            this.parent.revealCreatures(null);
            // Note the parent is responsible for updating the CreatureInfo
            // for this node when calculating the predicted split.
        }
        else if (hasSplit())
        {
            reSplit();
        }
        else
        {
            // The reveal didn't contain any actual info; nothing to do.
        }

        if (this.creatures.size() != getHeight())
        {
            // Make sure the parent updates the guess to be consistant
            // with the actual size.
            throw new PredictSplitsException(
                "Certainty error in revealCreatures -- size is " +
                this.creatures.size() + " height is " + getHeight());
        }

    }

    // Hardcoded to default starting legion.
    public static boolean isLegalInitialSplitoff(List names)
    {
        if (names.size() != 4)
        {
            return false;
        }
        int count = 0;
        if (names.contains("Titan"))
        {
            count++;
        }
        if (names.contains("Angel"))
        {
            count++;
        }

        return count == 1;
    }

    /** 
     * Return a list of all legal combinations of splitoffs. 
     * Also update knownKeep and knownSplit if we conclude that more
     * creatures are certain.
     * @param childSize
     * @param knownKeep
     * @param knownSplit
     * @return
     */
    List findAllPossibleSplits(int childSize, List knownKeep, List knownSplit)
    {
        // Sanity checks
        if (knownSplit.size() > childSize)
        {
            throw new PredictSplitsException(
                "More known splitoffs than splitoffs");
        }
        if (creatures.size() > 8)
        {
            throw new PredictSplitsException("> 8 creatures in legion");
        }
        else if (creatures.size() == 8)
        {
            if (childSize != 4)
            {
                throw new PredictSplitsException("Illegal initial split");
            }
            if (!creatures.getCreatureNames().contains(Constants.titan))
            {
                throw new PredictSplitsException("No titan in 8-high legion");
            }
            if (!creatures.getCreatureNames().contains(Constants.angel))
            {
                throw new PredictSplitsException("No angel in 8-high legion");
            }
        }

        List knownCombo = new ArrayList();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);
        List certain = getCertainCreatures().getCreatureNames();
        if (!superset(certain, knownCombo))
        {
            throw new PredictSplitsException(
                "knownCombo contains uncertain creatures");
        }

        // Now determine by count arguments if we can determine know keepers
        // or splits. (If parent contains 3 certain rangers, and we split 5-2
        // then the 5 split contains a ranger. By the same argument
        // if the 5 stack grows a griffon from 3 lions, then there are only 2
        // unkowns in there from the split, so the 2 stack must contain a 
        // ranger. 
        List certainsToSplit = subtractLists(certain, knownCombo);
        Collections.sort(certainsToSplit);

        // Special code to take into account account the the first split
        // must include a lord in each stack
        int firstTurnUnknownLord = 0;
        if (this.turnCreated == 0)
        {

            boolean unknownTitan = certainsToSplit.remove("Titan");
            boolean unknownAngel = certainsToSplit.remove("Angel");
            if (unknownTitan && unknownAngel)
            {
                // ei. neither are positioned yet
                firstTurnUnknownLord = 1;
            }
            else if (unknownAngel)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains("Titan"))
                {
                    knownSplit.add("Angel");
                }
                else
                {
                    knownKeep.add("Angel");
                }
            }
            else if (unknownTitan)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains("Angel"))
                {
                    knownSplit.add("Titan");
                }
                else
                {
                    knownKeep.add("Titan");
                }
            }
        }

        int numUnknownsToKeep = creatures.size() - childSize -
            knownKeep.size();
        int numUnknownsToSplit = childSize - knownSplit.size();

        if (!certainsToSplit.isEmpty())
        {
            String nextCreature = "";
            String currCreature = "";
            int count = 0;

            Iterator it = certainsToSplit.iterator();
            boolean done = false;
            while (!done)
            {
                currCreature = nextCreature;
                if (it.hasNext())
                {
                    nextCreature = (String)it.next();
                }
                else
                {
                    nextCreature = "";
                    done = true;
                }

                if (!nextCreature.equals(currCreature))
                {
                    // Compute how many to keep or splt, and update the lists.
                    int numToKeep =
                        count - numUnknownsToSplit + firstTurnUnknownLord;
                    int numToSplit =
                        count - numUnknownsToKeep + firstTurnUnknownLord;
                    for (int i = 0; i < numToKeep; i++)
                    {
                        knownKeep.add(currCreature);
                        numUnknownsToKeep--;
                    }
                    for (int i = 0; i < numToSplit; i++)
                    {
                        knownSplit.add(currCreature);
                        numUnknownsToSplit--;
                    }
                    count = 1;
                }
                else
                {
                    count++;
                }
            }
        }

        List unknowns = creatures.getCreatureNames();

        // update knownCombo because knownKeep or knownSplit may have changed
        knownCombo.clear();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);

        for (Iterator it = knownCombo.iterator(); it.hasNext();)
        {
            String name = (String)it.next();
            unknowns.remove(name);
        }

        Combos combos = new Combos(unknowns, numUnknownsToSplit);

        Set possibleSplitsSet = new HashSet();
        for (Iterator it = combos.iterator(); it.hasNext();)
        {
            List combo = (List)it.next();
            List pos = new ArrayList();
            pos.addAll(knownSplit);
            pos.addAll(combo);
            if (getHeight() != 8)
            {
                possibleSplitsSet.add(pos);
            }
            else
            {
                if (isLegalInitialSplitoff(pos))
                {
                    possibleSplitsSet.add(pos);
                }
            }
        }
        List possibleSplits = new ArrayList(possibleSplitsSet);
        return possibleSplits;
    }

    // TODO Use SimpleAI version?
    /** Decide how to split this legion, and return a list of creatures names 
     *  to remove.  Return empty list on error. */
    List chooseCreaturesToSplitOut(List possibleSplits)
    {
        List firstElement = (List)possibleSplits.get(0);
        boolean maximize = (2 * firstElement.size() > getHeight());
        int bestKillValue = -1;
        List creaturesToRemove = new ArrayList();
        for (Iterator it = possibleSplits.iterator(); it.hasNext();)
        {
            List li = (List)it.next();
            int totalKillValue = 0;
            for (Iterator it2 = li.iterator(); it2.hasNext();)
            {
                String name = (String)it2.next();
                Creature creature = Creature.getCreatureByName(name);
                totalKillValue += creature.getKillValue();
            }
            if ((bestKillValue < 0) ||
                (!maximize && totalKillValue < bestKillValue) ||
                (maximize && totalKillValue > bestKillValue))
            {
                bestKillValue = totalKillValue;
                creaturesToRemove = li;
            }
        }
        return creaturesToRemove;
    }

    /** Return the number of times ob is found in li */
    int count(List li, Object ob)
    {
        int num = 0;
        for (Iterator it = li.iterator(); it.hasNext();)
        {
            if (ob.equals(it.next()))
            {
                num++;
            }
        }
        return num;
    }

    /** 
     * Computes the predicted split of childsize, given that we
     * may already know some pieces that are keept or spilt.
     * Also makes the new CreatureInfoLists.
     * Note that knownKeep and knownSplit will be altered, and be 
     * empty after call
     * @param childSize
     * @param knownKeep certain creatures to keep 
     * @param knownSplit certain creatures to split
     * @param keepList return argument
     * @param splitList return argument
     */
    void computeSplit(
        int childSize,
        List knownKeep,
        List knownSplit,
        CreatureInfoList keepList,
        CreatureInfoList splitList)
    {

        List possibleSplits =
            findAllPossibleSplits(childSize, knownKeep, knownSplit);

        List splitoffNames = chooseCreaturesToSplitOut(possibleSplits);

        // We now know how we want to split, caculate certainty and 
        // make the new creatureInfoLists
        for (Iterator it = creatures.iterator(); it.hasNext();)
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            String name = ci.getName();
            CreatureInfo newinfo = new CreatureInfo(name, false, true);
            if (splitoffNames.contains(name))
            {
                splitList.add(newinfo);
                splitoffNames.remove(name);
                // If in knownSplit, set certain
                if (knownSplit.contains(name))
                {
                    knownSplit.remove(name);
                    newinfo.setCertain(true);
                }
            }
            else
            {
                keepList.add(newinfo);
                // If in knownKeep, set certain
                if (knownKeep.contains(name))
                {
                    knownKeep.remove(name);
                    newinfo.setCertain(true);
                }
            }
        }
    }

    /**
     * Perform the initial split of a stack, and creater the children
     * @param childSize
     * @param otherMarkerId
     * @param turn
     */
    void split(int childSize, String otherMarkerId, int turn)
    {
        if (creatures.size() > 8)
        {
            throw new PredictSplitsException("> 8 creatures in legion");
        }
        if (hasSplit())
        {
            throw new PredictSplitsException(
                "use reSplit to recalculate old splits");
        }

        List knownKeep = new ArrayList();
        List knownSplit = new ArrayList();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize, knownKeep, knownSplit, keepList, splitList);

        child1 = new PredictSplitNode(markerId, turn, keepList, this);
        child2 = new PredictSplitNode(otherMarkerId, turn, splitList, this);
        childSize2 = child2.getHeight();
    }

    /**
     * Recompute the split of a stack, taking advantage of any information
     * potentially gained from the children
     *
     */
    void reSplit()
    {
        if (creatures.size() > 8)
        {
            throw new PredictSplitsException("> 8 creatures in legion");
        }

        List knownKeep =
            child1.getCertainAtSplitOrRemovedCreatures().getCreatureNames();
        List knownSplit =
            child2.getCertainAtSplitOrRemovedCreatures().getCreatureNames();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize2, knownKeep, knownSplit, keepList, splitList);

        // we have now predicted the split we need to inform the children
        child1.updateInitialSplitInfo(keepList);
        child2.updateInitialSplitInfo(splitList);
    }

    /** This takes potentially new information about the legion's composition
     * at split and applies the later changes to the legion to get a new
     * predicton of contents. It then recursively resplits.
     * @param newList
     */
    void updateInitialSplitInfo(CreatureInfoList newList)
    {
        // TODO Check if any new information was gained and stop if not.
        newList.addAll(getAfterSplitCreatures());
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext();)
        {
            newList.remove(it.next());
        }
        setCreatures(newList);

        // update children if we have any
        if (hasSplit())
        {
            reSplit();
        }
    }

    /** Recombine this legion and other, because it was not possible to
     *  move.  They must share a parent.  If either legion has the parent's
     *  markerId, then that legion will remain.  Otherwise this legion
     *  will remain.  Also used to undo splits.
     */
    void merge(PredictSplitNode other, int turn)
    {
        if (this.parent == other.parent)
        {
            // this is regular merge
            if (getMarkerId().equals(parent.getMarkerId()) ||
                other.getMarkerId().equals(parent.getMarkerId()))
            { // Cancel split.
                parent.clearChildren();
            }
            else
            {
                throw new PredictSplitsException(
                    "None of the legions carry the parent maker");
            }
        }
        else
        {
            // this must be a merge of a 3-way split
            // origNode -- father -- nodeB
            //          \ nodeA   \ third
            // this transforms into
            // origNode -- (nodeA + nodeB)
            //          \ third

            PredictSplitNode nodeA;
            PredictSplitNode nodeB;

            if (this.parent == other.parent.parent)
            {
                nodeA = this;
                nodeB = other;
            }
            else if (this.parent.parent == other.parent)
            {
                nodeA = other;
                nodeB = this;
            }
            else
            {
                throw new PredictSplitsException("Illegal merge");
            }
            PredictSplitNode father = nodeB.parent;
            PredictSplitNode origNode = nodeA.parent;
            PredictSplitNode thirdLegion;
            if (nodeB == father.child1)
            {
                thirdLegion = father.child2;
            }
            else
            {
                thirdLegion = father.child1;

            }

            if (origNode.getMarkerId().equals(thirdLegion.getMarkerId()))
            {
                // third is carries the original marker and nodeA is then
                // the splitoff from the origNode, just add creatures from nodeB
                nodeA.creatures.addAll(nodeB.creatures);
                origNode.childSize2 = nodeA.getHeight();
                origNode.child1=thirdLegion;
            }
            else
            {
                // attach thirdLegion as the split from the node, and
                // nodeA+nodeB as the keep
                origNode.child2 = thirdLegion;
                origNode.childSize2 = thirdLegion.getHeight();
                if (origNode.getMarkerId().equals(nodeA.getMarkerId()))
                {
                    nodeA.creatures.addAll(nodeB.creatures);
                    origNode.child1 = nodeA;
                }
                else
                {
                    nodeB.creatures.addAll(nodeA.creatures);
                    origNode.child1 = nodeB;
                }
            }
        }
    }

    void addCreature(String creatureName)
    {
        if (creatureName.startsWith("Titan"))
        {
            creatureName = "Titan";
        }
        if (getHeight() >= 7 && child1 == null)
        {
            throw new PredictSplitsException("Tried adding to 7-high legion");
        }
        CreatureInfo ci = new CreatureInfo(creatureName, true, false);
        creatures.add(ci);
    }

    void removeCreature(String creatureName)
    {
        if (creatureName.startsWith("Titan"))
        {
            creatureName = "Titan";
        }
        if (getHeight() <= 0)
        {
            throw new PredictSplitsException(
                "Tried removing from 0-high legion");
        }
        List cnl = new ArrayList();
        cnl.add(creatureName);
        revealCreatures(cnl);

        // Find the creature to remove
        Iterator it = creatures.iterator();
        // We have already checked height>0, so taking next is ok.
        CreatureInfo ci = (CreatureInfo)it.next();
        while (!(ci.isCertain() && ci.getName().equals(creatureName)))
        {
            if (!it.hasNext())
            {
                throw new PredictSplitsException(
                    "Tried to remove nonexistant creature");
            }
            ci = (CreatureInfo)it.next();
        }

        // Only need to track the removed creature for future parent split
        // predictions if it was here at the time of the split.
        if (ci.isAtSplit())
        {
            removed.add(ci);
        }
        it.remove();
    }

    void removeCreatures(List creatureNames)
    {
        revealCreatures(creatureNames);
        for (Iterator it = creatureNames.iterator(); it.hasNext();)
        {
            String name = (String)it.next();
            removeCreature(name);
        }
    }

    public int compareTo(Object object)
    {
        if (object instanceof PredictSplitNode)
        {
            PredictSplitNode other = (PredictSplitNode)object;
            return (toString().compareTo(other.toString()));
        }
        else
        {
            throw new ClassCastException();
        }
    }

    static List subtractLists(List big, List little)
    {
        ArrayList li = new ArrayList(big);
        for (Iterator it = little.iterator(); it.hasNext();)
        {
            li.remove(it.next());
        }
        return li;
    }

    /** Return the number of times name occurs in li */
    static int count(List li, String name)
    {
        int num = 0;
        for (Iterator it = li.iterator(); it.hasNext();)
        {
            String s = (String)it.next();
            if (s.equals(name))
            {
                num++;
            }
        }
        return num;
    }

    /** lili is a list of lists.  Return the minimum number of times name
     appears in any of the lists contained in lili. */
    static int minCount(List lili, String name)
    {
        int min = Integer.MAX_VALUE;
        for (Iterator it = lili.iterator(); it.hasNext();)
        {
            List li = (List)it.next();
            min = Math.min(min, count(li, name));
        }
        if (min == Integer.MAX_VALUE)
        {
            min = 0;
        }
        return min;
    }
}


class PredictSplitsException extends RuntimeException
{
    PredictSplitsException(String s)
    {
        super(s);
    }
}
