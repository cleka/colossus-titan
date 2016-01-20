package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.colossus.game.Legion;
import net.sf.colossus.util.Combos;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.Variant;


/**
 *  Predicts splits for one enemy player, and adjusts predictions as
 *  creatures are revealed.
 *  @author David Ripton
 *  @author Kim Milvang-Jensen
 *
 *  See docs/SplitPrediction.txt
 */

public class PredictSplitNode implements Comparable<PredictSplitNode>
{
    private final String markerId; // Not unique!
    private final int turnCreated;
    private CreatureInfoList creatures = new CreatureInfoList();

    // only if atSplit
    private final CreatureInfoList removed = new CreatureInfoList();

    private final PredictSplitNode parent;
    // Size of child2 at the time this node was split.
    private int childSize2;
    private PredictSplitNode child1; // child that keeps the marker
    private PredictSplitNode child2; // child with the new marker
    private final Variant variant;
    private final CreatureType titan;
    private final CreatureType angel;
    private static CreatureInfoComparator cic = new CreatureInfoComparator();

    PredictSplitNode(String markerId, int turnCreated, CreatureInfoList cil,
        PredictSplitNode parent, Variant variant)
    {
        this.markerId = markerId;
        this.turnCreated = turnCreated;
        this.creatures = cil.clone();
        this.parent = parent;
        this.variant = variant;
        this.titan = variant.getCreatureByName("Titan");
        this.angel = variant.getCreatureByName("Angel");
        clearChildren();
    }

    private void clearChildren()
    {
        childSize2 = 0;
        child1 = null;
        child2 = null;
    }

    public String getMarkerId()
    {
        return markerId;
    }

    public String getFullName()
    {
        return markerId + '(' + turnCreated + ')';
    }

    public PredictSplitNode getChild1()
    {
        return child1;
    }

    public PredictSplitNode getChild2()
    {
        return child2;
    }

    public PredictSplitNode getParent()
    {
        return parent;
    }

    public int getTurnCreated()
    {
        return turnCreated;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getFullName() + ":");
        for (CreatureInfo ci : getCreatures())
        {
            sb.append(" " + ci.toString());
        }
        for (CreatureInfo ci : getRemovedCreatures())
        {
            sb.append(" " + ci.toString() + "-");
        }
        return sb.toString();
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getCreatures()
    {
        boolean success = true;
        CreatureInfoList copy;
        try
        {
            copy = creatures.clone();
            Collections.sort(copy, cic);
        }
        catch (Exception e)
        {
            System.err.println("Exception " + e.toString()
                + " during getCreatures(); trying again");
            success = false;
            copy = creatures.clone();
            Collections.sort(copy, cic);
            success = true;
            System.err.println("getCreatures() succeeded on 2nd time.");
        }
        if (!success)
        {
            System.err.println("getCreatures() failed also on 2nd time.");
        }
        return copy;
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
        for (CreatureInfo ci : getCreatures())
        {
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
        for (CreatureInfo ci : getCreatures())
        {
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
        assert child1 != null || child2 != null : "One child legion";
        return true;
    }

    List<PredictSplitNode> getChildren()
    {
        List<PredictSplitNode> li = new ArrayList<PredictSplitNode>();
        if (hasSplit())
        {
            li.add(child1);
            li.add(child2);
        }
        return li;
    }

    /**
     * Return true if all of this node's children, grandchildren, etc. have no
     * uncertain creatures
     */
    boolean allDescendentsCertain()
    {
        if (child1 == null)
        {
            return true;
        }
        else
        {
            return child1.allCertain() && child2.allCertain()
                && child1.allDescendentsCertain()
                && child2.allDescendentsCertain();
        }
    }

    /**
     * Return list of CreatureInfo where atSplit == true, plus removed
     * creatures.
     */
    CreatureInfoList getAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (CreatureInfo ci : getCreatures())
        {
            if (ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (CreatureInfo ci : getRemovedCreatures())
        {
            list.add(ci);
        }
        return list;
    }

    /** Return list of CreatureInfo where atSplit == false. */
    CreatureInfoList getAfterSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (CreatureInfo ci : getCreatures())
        {
            if (!ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        return list;
    }

    /**
     * Return list of CreatureInfo where both certain and atSplit are true, plus
     * removed creatures.
     */
    CreatureInfoList getCertainAtSplitOrRemovedCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        for (CreatureInfo ci : getCreatures())
        {
            if (ci.isCertain() && ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (CreatureInfo ci : getRemovedCreatures())
        {
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

    /**
     * Return true if big is a superset of little.
     *
     * Note that this treats repeated elements as distinct, i.e. if the
     * little list contains two copies of something, then the big list has
     * to contain two copies, too. It differs in that regard from
     * {@linkplain Collection#containsAll(Collection)} which is implemented
     * in a fashion where this is not necessary (the specification as of JDK
     * 1.5 is actually blurry on the matter).
     */
    static <T> boolean superset(List<T> big, List<T> little)
    {
        List<T> bigclone = new ArrayList<T>(big);
        for (T ob : little)
        {
            if (!bigclone.remove(ob))
            {
                return false;
            }
        }
        return true;
    }

    void revealCreatures(List<CreatureType> cnl)
    {
        if (cnl == null)
        {
            // this means we are updating the parent, and the info gained is
            // computed from children
            cnl = new ArrayList<CreatureType>();
            cnl.addAll(child1.getCertainAtSplitOrRemovedCreatures()
                .getCreatureTypes());
            cnl.addAll(child2.getCertainAtSplitOrRemovedCreatures()
                .getCreatureTypes());
        }

        List<CreatureType> certainInfoGained = subtractLists(cnl,
            getCertainCreatures().getCreatureTypes());

        if (!certainInfoGained.isEmpty())
        {
            for (CreatureType type : certainInfoGained)
            {
                this.creatures.add(new CreatureInfo(type, true, true));
            }

            // TODO : added null guard, because during loading a game it went
            // up and up many times (7+) until it hit null.
            // Probably caused by incorrect legion contents...
            // So null guard here to find the reason for that...
            // Probably should never happen any more after loading of saved
            // games was fixed in 08/2008... (Clemens)

            assert this.parent != null : "Parent in PredictSplitNode is null, but should go up; "
                + "certain info gained is " + certainInfoGained;

            // it should never be null, but... in faulty game loading it
            // did happen. NullGuard just to avoid exceptions.
            // No LOGGER here, too lazy to add it just for this one here...
            if (this.parent != null)
            {
                this.parent.revealCreatures(null);
            }

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

        assert this.creatures.size() == getHeight() : "Certainty error in revealCreatures -- size is "
            + this.creatures.size() + " height is " + getHeight();
    }

    // Hardcoded to default starting legion.
    public boolean isLegalInitialSplitoff(List<CreatureType> types)
    {
        if (types.size() != 4)
        {
            return false;
        }
        int count = 0;
        if (types.contains(titan))
        {
            count++;
        }
        if (types.contains(angel))
        {
            count++;
        }

        return count == 1;
    }

    /**
     * Return a list of all legal combinations of splitoffs. Also update
     * knownKeep and knownSplit if we conclude that more creatures are certain.
     *
     * @param childSize
     * @param knownKeep
     * @param knownSplit
     * @return
     */
    List<List<CreatureType>> findAllPossibleSplits(int childSize,
        List<CreatureType> knownKeep, List<CreatureType> knownSplit)
    {
        // Sanity checks
        assert knownSplit.size() <= childSize : "More known splitoffs than splitoffs";
        assert creatures.size() <= 8 : "> 8 creatures in legion";
        assert creatures.size() != 8 || childSize == 4 : "Illegal initial split ("
            + childSize + "/" + creatures.size() + ")";
        assert creatures.size() != 8
            || creatures.getCreatureTypes().contains(titan) : "No titan in 8-high legion";
        assert creatures.size() != 8
            || creatures.getCreatureTypes().contains(angel) : "No angel in 8-high legion";

        List<CreatureType> knownCombo = new ArrayList<CreatureType>();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);
        List<CreatureType> certain = getCertainCreatures().getCreatureTypes();
        assert superset(certain, knownCombo) : "knownCombo contains uncertain creatures";

        // Now determine by count arguments if we can determine know keepers
        // or splits. (If parent contains 3 certain rangers, and we split 5-2
        // then the 5 split contains a ranger. By the same argument
        // if the 5 stack grows a griffon from 3 lions, then there are only 2
        // unkowns in there from the split, so the 2 stack must contain a
        // ranger.
        List<CreatureType> certainsToSplit = subtractLists(certain, knownCombo);
        Collections.sort(certainsToSplit);

        // Special code to take into account account the the first split
        // must include a lord in each stack
        int firstTurnUnknownLord = 0;
        if (this.turnCreated == 0)
        {

            boolean unknownTitan = certainsToSplit.remove(titan);
            boolean unknownAngel = certainsToSplit.remove(angel);
            if (unknownTitan && unknownAngel)
            {
                // ei. neither are positioned yet
                firstTurnUnknownLord = 1;
            }
            else if (unknownAngel)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains(titan))
                {
                    knownSplit.add(angel);
                }
                else
                {
                    knownKeep.add(angel);
                }
            }
            else if (unknownTitan)
            {
                // Titan known, set Angel certain
                if (knownKeep.contains(angel))
                {
                    knownSplit.add(titan);
                }
                else
                {
                    knownKeep.add(titan);
                }
            }
        }

        int numUnknownsToKeep = creatures.size() - childSize
            - knownKeep.size();
        int numUnknownsToSplit = childSize - knownSplit.size();

        if (!certainsToSplit.isEmpty())
        {
            CreatureType nextCreature = null;
            CreatureType currCreature = null;
            int count = 0;

            Iterator<CreatureType> it = certainsToSplit.iterator();
            boolean done = false;
            while (!done)
            {
                currCreature = nextCreature;
                if (it.hasNext())
                {
                    nextCreature = it.next();
                }
                else
                {
                    nextCreature = null;
                    done = true;
                }

                if (!safeEquals(currCreature, nextCreature))
                {
                    // Compute how many to keep or split, and update the lists.
                    int numToKeep = count - numUnknownsToSplit
                        + firstTurnUnknownLord;
                    int numToSplit = count - numUnknownsToKeep
                        + firstTurnUnknownLord;
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

        List<CreatureType> unknowns = creatures.getCreatureTypes();

        // update knownCombo because knownKeep or knownSplit may have changed
        knownCombo.clear();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);

        for (CreatureType cre : knownCombo)
        {
            unknowns.remove(cre);
        }

        Combos<CreatureType> combos = new Combos<CreatureType>(unknowns,
            numUnknownsToSplit);

        Set<List<CreatureType>> possibleSplitsSet = new HashSet<List<CreatureType>>();
        for (Iterator<List<CreatureType>> it = combos.iterator(); it.hasNext();)
        {
            List<CreatureType> combo = it.next();
            List<CreatureType> pos = new ArrayList<CreatureType>();
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
        List<List<CreatureType>> possibleSplits = new ArrayList<List<CreatureType>>(
            possibleSplitsSet);
        return possibleSplits;
    }

    private static <T> boolean safeEquals(T obj1, T obj2)
    {
        if (obj1 == null)
        {
            return (obj2 == null);
        }
        if (obj2 == null)
        {
            // this should happen on equals(null) anyway, but we
            // don't trust all equals(..) implementations
            return false;
        }
        return obj1.equals(obj2);
    }

    // TODO Use SimpleAI version?
    /**
     * Decide how to split this legion, and return a list of creatures names to
     * remove. Return empty list on error.
     */
    List<CreatureType> chooseCreaturesToSplitOut(
        List<List<CreatureType>> possibleSplits)
    {
        List<CreatureType> firstElement = possibleSplits.get(0);
        boolean maximize = (2 * firstElement.size() > getHeight());
        int bestKillValue = -1;
        List<CreatureType> creaturesToRemove = new ArrayList<CreatureType>();
        for (List<CreatureType> li : possibleSplits)
        {
            int totalKillValue = 0;
            for (CreatureType creature : li)
            {
                totalKillValue += creature.getKillValue();
            }
            if ((bestKillValue < 0)
                || (!maximize && totalKillValue < bestKillValue)
                || (maximize && totalKillValue > bestKillValue))
            {
                bestKillValue = totalKillValue;
                creaturesToRemove = li;
            }
        }
        return creaturesToRemove;
    }

    /** Return the number of times ob is found in li */
    int count(List<?> li, Object ob)
    {
        int num = 0;
        for (Object ob2 : li)
        {
            if (ob.equals(ob2))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * Computes the predicted split of childsize, given that we may already know
     * some pieces that are keept or spilt. Also makes the new
     * CreatureInfoLists. Note that knownKeep and knownSplit will be altered,
     * and be empty after call
     *
     * @param childSize
     * @param knownKeep
     *            certain creatures to keep
     * @param knownSplit
     *            certain creatures to split
     * @param keepList
     *            return argument
     * @param splitList
     *            return argument
     */
    void computeSplit(int childSize, List<CreatureType> knownKeep,
        List<CreatureType> knownSplit, CreatureInfoList keepList,
        CreatureInfoList splitList)
    {

        List<List<CreatureType>> possibleSplits = findAllPossibleSplits(
            childSize, knownKeep, knownSplit);

        List<CreatureType> splitoffs = chooseCreaturesToSplitOut(possibleSplits);

        // We now know how we want to split, caculate certainty and
        // make the new creatureInfoLists
        for (CreatureInfo ci : creatures)
        {
            CreatureType type = ci.getType();
            CreatureInfo newinfo = new CreatureInfo(ci.getType(), false, true);
            if (splitoffs.contains(type))
            {
                splitList.add(newinfo);
                splitoffs.remove(type);
                // If in knownSplit, set certain
                if (knownSplit.contains(type))
                {
                    knownSplit.remove(type);
                    newinfo.setCertain(true);
                }
            }
            else
            {
                keepList.add(newinfo);
                // If in knownKeep, set certain
                if (knownKeep.contains(type))
                {
                    knownKeep.remove(type);
                    newinfo.setCertain(true);
                }
            }
        }
    }

    /**
     * Perform the initial split of a stack, and create the children
     *
     * @param childSize
     * @param otherMarkerId
     * @param turn
     */
    void split(int childSize, Legion otherLegion, int turn)
    {
        assert creatures.size() <= 8 : "> 8 creatures in legion";
        assert !hasSplit() : "use reSplit to recalculate old splits";

        List<CreatureType> knownKeep = new ArrayList<CreatureType>();
        List<CreatureType> knownSplit = new ArrayList<CreatureType>();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize, knownKeep, knownSplit, keepList, splitList);

        // If both children have same height, in 50% of the cases mix it up
        // which child gets the "split off" content and which the "to keep".

        if (getHeight() == 2 * childSize)
        {
            // A creative way to produce a random boolean value:
            long now = new Date().getTime();
            boolean swapKeepAndSplit = ((now % 17) % 2 == 1);

            if (swapKeepAndSplit)
            {
                CreatureInfoList swapTmp = keepList;
                keepList = splitList;
                splitList = swapTmp;
            }
        }

        child1 = new PredictSplitNode(markerId, turn, keepList, this, variant);
        child2 = new PredictSplitNode(otherLegion.getMarkerId(), turn,
            splitList, this, variant);
        childSize2 = child2.getHeight();
    }

    /**
     * Recompute the split of a stack, taking advantage of any information
     * potentially gained from the children
     *
     */
    void reSplit()
    {
        assert creatures.size() <= 8 : "> 8 creatures in legion";

        List<CreatureType> knownKeep = child1
            .getCertainAtSplitOrRemovedCreatures().getCreatureTypes();
        List<CreatureType> knownSplit = child2
            .getCertainAtSplitOrRemovedCreatures().getCreatureTypes();

        CreatureInfoList keepList = new CreatureInfoList();
        CreatureInfoList splitList = new CreatureInfoList();

        computeSplit(childSize2, knownKeep, knownSplit, keepList, splitList);

        // we have now predicted the split we need to inform the children
        child1.updateInitialSplitInfo(keepList);
        child2.updateInitialSplitInfo(splitList);
    }

    /**
     * This takes potentially new information about the legion's composition at
     * split and applies the later changes to the legion to get a new predicton
     * of contents. It then recursively resplits.
     *
     * @param newList
     */
    void updateInitialSplitInfo(CreatureInfoList newList)
    {
        // TODO Check if any new information was gained and stop if not.
        newList.addAll(getAfterSplitCreatures());
        for (CreatureInfo ci : getRemovedCreatures())
        {
            newList.remove(ci);
        }
        setCreatures(newList);

        // update children if we have any
        if (hasSplit())
        {
            reSplit();
        }
    }

    /**
     * Recombine this legion and other, because it was not possible to move.
     * They must share a parent. If either legion has the parent's markerId,
     * then that legion will remain. Otherwise this legion will remain. Also
     * used to undo splits.
     */
    void merge(PredictSplitNode other)
    {
        if (this.parent == other.parent)
        {
            assert getMarkerId().equals(parent.getMarkerId())
                || other.getMarkerId().equals(parent.getMarkerId()) : "None of the legions carry the parent maker";

            // this is regular merge, cancel split.
            parent.clearChildren();
        }
        else
        {
            // this must be a merge of a 3-way split
            // origNode -- father -- nodeB
            // \ nodeA \ third
            // this transforms into
            // origNode -- (nodeA + nodeB)
            // \ third

            PredictSplitNode nodeA = null;
            PredictSplitNode nodeB = null;

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
            // check we got a valid combination, otherwise the nodes are not set
            assert (nodeA != null) && (nodeB != null) : "Illegal merge";

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
                origNode.child1 = thirdLegion;
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

    void addCreature(CreatureType type)
    {
        assert getHeight() < 7 || child1 == null : "Tried adding to 7-high legion";
        CreatureInfo ci = new CreatureInfo(type, true, false);
        creatures.add(ci);
    }

    void removeCreature(CreatureType type)
    {
        assert getHeight() > 0 : "Tried removing from 0-high legion";

        List<CreatureType> cnl = Collections.singletonList(type);
        revealCreatures(cnl);

        // Find the creature to remove
        Iterator<CreatureInfo> it = creatures.iterator();
        // We have already checked height>0, so taking next is ok.
        CreatureInfo ci = it.next();
        while (!(ci.isCertain() && ci.getType().equals(type)))
        {
            assert it.hasNext() : "Tried to remove nonexistant creature";
            ci = it.next();
        }

        // Only need to track the removed creature for future parent split
        // predictions if it was here at the time of the split.
        if (ci.isAtSplit())
        {
            removed.add(ci);
        }
        it.remove();
    }

    void removeCreatures(List<CreatureType> creatureTypes)
    {
        revealCreatures(creatureTypes);
        for (CreatureType type : creatureTypes)
        {
            removeCreature(type);
        }
    }

    // TODO Comparable not implemented properly since equals() not
    //      overridden
    public int compareTo(PredictSplitNode other)
    {
        return toString().compareTo(other.toString());
    }

    static <T> List<T> subtractLists(List<T> big, List<T> little)
    {
        ArrayList<T> li = new ArrayList<T>(big);
        for (T item : little)
        {
            li.remove(item);
        }
        return li;
    }

    /** Return the number of times name occurs in li */
    static int count(List<String> li, String name)
    {
        int num = 0;
        for (String s : li)
        {
            if (s.equals(name))
            {
                num++;
            }
        }
        return num;
    }

    /**
     * lili is a list of lists. Return the minimum number of times name appears
     * in any of the lists contained in lili.
     */
    static int minCount(List<List<String>> lili, String name)
    {
        int min = Integer.MAX_VALUE;
        for (List<String> li : lili)
        {
            min = Math.min(min, count(li, name));
        }
        if (min == Integer.MAX_VALUE)
        {
            min = 0;
        }
        return min;
    }
}
