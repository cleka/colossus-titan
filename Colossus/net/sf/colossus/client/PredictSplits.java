package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.Combos;
import net.sf.colossus.util.Log;


/**
 *  Predicts splits for one enemy player, and adjusts predictions as 
 *  creatures are revealed.
 *  @version $Id$
 *  @author David Ripton
 *  @see SplitPrediction.txt
 */

public final class PredictSplits
{
    private Node root;           // All contents of root must be known.
    private NodeTurnComparator nodeTurnComparator = new NodeTurnComparator();

    PredictSplits(String playerName, String rootId,
            List creatureNames)
    {
        CreatureInfoList infoList = new CreatureInfoList();
        for (Iterator it = creatureNames.iterator(); it.hasNext(); )
        {
            String name = (String)it.next();
            CreatureInfo ci = new CreatureInfo(name, true, true);
            infoList.add(ci);
        }
        root = new Node(rootId, 0, infoList, null);
    }

    /** Return all non-empty childless nodes in subtree starting from node. */
    List getLeaves(Node node)
    {
        List leaves = new ArrayList();
        if (node.getChild1() == null)
        {
            if (!node.getCreatures().isEmpty())
            {
                leaves.add(node);
            }
        }
        else
        {
            leaves.addAll(getLeaves(node.getChild1()));
            leaves.addAll(getLeaves(node.getChild2()));
        }

        TreeSet prunes = new TreeSet(new ReverseIntegerComparator());

        // If duplicate markerIds, prune the older node.
        for (int i = 0; i < leaves.size(); i++)
        {
            for (int j = 0; j < leaves.size(); j++)
            {
                if (i != j)
                {
                    Node leaf1 = (Node)leaves.get(i);
                    Node leaf2 = (Node)leaves.get(j);
                    if (leaf1.getMarkerId().equals(leaf2.getMarkerId()))
                    {
                        if (leaf1.getTurnCreated() == leaf2.getTurnCreated())
                        {
                            throw new PredictSplitsException(
                                "Two leaf nodes with same markerId and turn");
                        }
                        else if (leaf1.getTurnCreated() <
                                leaf2.getTurnCreated())
                        {
                            prunes.add(new Integer(i));
                        }
                        else
                        {
                            prunes.add(new Integer(j));
                        }
                    }
                }
            }
        }
        // Remove in reverse order to keep indexes consistent.
        for (Iterator it = prunes.iterator(); it.hasNext(); )
        {
            Integer in = (Integer)it.next();
            leaves.remove(in.intValue());
        }

        return leaves;
    }

    /** Return all non-empty nodes in subtree starting from node. */
    List getNodes(Node node)
    {
        List nodes = new ArrayList();
        if (!node.getCreatures().isEmpty())
        {
            nodes.add(node);
        }
        if (node.getChild1() != null)
        {
            nodes.addAll(getNodes(node.getChild1()));
            nodes.addAll(getNodes(node.getChild2()));
        }
        return nodes;
    }

    class ReverseIntegerComparator implements Comparator
    {
        // Sort in reverse, so we don't disturb array 
        // indexes when removing.
        public int compare(Object o1, Object o2)
        {
            Integer in1 = (Integer)o1;
            Integer in2 = (Integer)o2;
            return in2.compareTo(in1);
        }
    }

    /** Print all childless nodes in tree. */
    void printLeaves()
    {
        Log.debug("");
        List leaves = getLeaves(root);
        Collections.sort(leaves);
        for (Iterator it = leaves.iterator(); it.hasNext(); )
        {
            Node leaf = (Node)it.next();
            Log.debug(leaf.toString());
        }
        Log.debug("");
    }

    /** Print all nodes in tree. */
    void printNodes()
    {
        Log.debug("");
        List nodes = getNodes(root);
        Collections.sort(nodes, nodeTurnComparator);
        for (Iterator it = nodes.iterator(); it.hasNext(); )
        {
            Node node = (Node)it.next();
            Log.debug(node.toString());
        }
        Log.debug("");
    }

    /** Return the leaf node with matching markerId. */
    Node getLeaf(String markerId)
    {
        List leaves = getLeaves(root);
        for (Iterator it = leaves.iterator(); it.hasNext(); )
        {
            Node leaf = (Node)it.next();
            if (markerId.equals(leaf.getMarkerId()))
            {
                return leaf;
            }
        }
        return null;
    }
}


class Node implements Comparable
{
    private final String markerId;       // Not unique!
    private final int turnCreated;
    private CreatureInfoList creatures = new CreatureInfoList();

    // only if atSplit
    private CreatureInfoList removed = new CreatureInfoList();

    private final Node parent;
    private int childSize1;          // At the time this node was split.
    private int childSize2;          // At the time this node was split.
    private Node child1;             // At the time this node was split.
    private Node child2;             // At the time this node was split.
    private int turnSplit;
    private static CreatureInfoComparator cic = new CreatureInfoComparator();

    Node(String markerId, int turnCreated, CreatureInfoList cil, Node parent)
    {
        this.markerId = markerId;
        this.turnCreated = turnCreated;
        this.creatures = (CreatureInfoList)cil.clone();
        this.parent = parent;
        clearChildren();
    }

    private void clearChildren()
    {
        childSize1 = 0;
        childSize2 = 0;
        child1 = null;
        child2 = null;
        turnSplit = -1;
    }

    String getMarkerId()
    {
        return markerId;
    }

    String getFullName()
    {
        return markerId + '(' + turnCreated + ')';
    }

    Node getChild1()
    {
        return child1;
    }

    Node getChild2()
    {
        return child2;
    }

    Node getParent()
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
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            sb.append(" " + ci.toString());
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext(); )
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
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isCertain())
            {
                list.add(ci);
            }
        }
        return list;
    }

    boolean allCertain()
    {
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (!ci.isCertain())
            {
                return false;
            }
        }
        return true;
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
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext(); )
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
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
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
        for (Iterator it = getCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isCertain() && ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        for (Iterator it = getRemovedCreatures().iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            list.add(ci);
        }
        return list;
    }

    Node getOtherChild(Node child)
    {
        Node otherChild = null;
        if (child == child1)
        {
            otherChild = child2;
        }
        else if (child == child2)
        {
            otherChild = child1;
        }
        else
        {
            throw new PredictSplitsException(
                    "Node.updateChildContents() Not my child");
        }
        return otherChild;
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
        for (Iterator it = little.iterator(); it.hasNext(); )
        {
            Object ob = it.next();
            if (!bigclone.remove(ob))
            {
                return false;
            }
        }
        return true;
    }

    void revealCreatures(List cnl)
    {
        List names = getCertainCreatures().getCreatureNames();
        if (cnl.isEmpty() || 
            (superset(names, cnl) && allDescendentsCertain()))
        {
            return;
        }

        CreatureInfoList cil = new CreatureInfoList();
        for (Iterator it = cnl.iterator(); it.hasNext(); )
        {
            String name = (String)it.next();
            cil.add(new CreatureInfo(name, true, true));
        }

        // Use a copy so we can remove creatures as we check for dupes.
        CreatureInfoList dupe = (CreatureInfoList)cil.clone();

        // Confirm that all creatures that were certain still fit along
        // with the revealed creatures.
        int count = dupe.size();
        CreatureInfoList certain = getCertainCreatures();
        for (Iterator it = certain.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (dupe.contains(ci))
            {
                dupe.remove(ci);
            }
            else
            {
                count++;
            }
        }

        if (count > getHeight())
        {
            throw new PredictSplitsException(
                    "Certainty error in revealCreatures -- count is " +
                    count + " height is " + getHeight());
        }

        // Mark passed creatures as certain and then communicate this to
        // parent, to adjust other legions.

        dupe = (CreatureInfoList)cil.clone();
        count = 0;
        for (Iterator it = dupe.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            ci.setCertain(true);
            ci.setAtSplit(true); // If not atSpilt, then would be certain.
            if (creatures.numCreature(ci.getName()) <
                    dupe.numCreature(ci.getName()))
            {
                creatures.add(ci);
                count++;
            }
        }

        // Ensure that the creatures in cnl are now marked certain.
        dupe = (CreatureInfoList)cil.clone();
        certain = getCertainCreatures();
        for (Iterator it = certain.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (dupe.contains(ci))
            {
                dupe.remove(ci);
            }
        }
        for (Iterator it = dupe.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            for (Iterator it2 = creatures.iterator(); it2.hasNext(); )
            {
                CreatureInfo ci2 = (CreatureInfo)it2.next();
                if (!ci2.isCertain() && ci2.getName().equals(ci.getName()))
                {
                    ci2.setCertain(true);
                    break;
                }
            }
        }

        // Need to remove count uncertain creatures.
        for (int i = 0; i < count; i++)
        {
            creatures.removeLastUncertainCreature();
        }

        if (parent != null)
        {
            parent.updateChildContents();
        }
    }

    /** Return true if creatures in children are consistent with self. */
    boolean childCreaturesMatch()
    {
        if (child1 == null)
        {
            return true;
        }

        CreatureInfoList allCreatures = new CreatureInfoList();
        allCreatures.addAll(child1.getAtSplitOrRemovedCreatures());
        allCreatures.addAll(child2.getAtSplitOrRemovedCreatures());

        for (Iterator it = allCreatures.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (allCreatures.numCreature(ci.getName()) !=
                    creatures.numCreature(ci.getName()))
            {
                return false;
            }
        }
        return true;
    }

    // Hardcoded to default starting legion.
    public boolean isLegalInitialSplitoff()
    {
        if (getHeight() != 4)
        {
            return false;
        }
        List names = creatures.getCreatureNames();
        int count = 0;
        for (Iterator it = names.iterator(); it.hasNext(); )
        {
            String name = (String)it.next();
            if (name.equals("Titan") || name.equals("Angel"))
            {
                count++;
            }
        }
        return count == 1;
    }

    /** Return a list of all legal combinations of splitoffs. */
    CreatureInfoList findAllPossibleSplits(int childSize,
            CreatureInfoList knownKeep, CreatureInfoList knownSplit)
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

        CreatureInfoList knownCombo = new CreatureInfoList();
        knownCombo.addAll(knownSplit);
        knownCombo.addAll(knownKeep);
        if (!superset(creatures, knownCombo))
        {
            revealCreatures(knownCombo.getCreatureNames());
            return findAllPossibleSplits(childSize, knownKeep, knownSplit);
        }

        CreatureInfoList unknowns = (CreatureInfoList)creatures.clone();
        for (Iterator it = knownCombo.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            unknowns.remove(ci);
        }

        int numUnknownsToSplit = childSize - knownSplit.size();

        Combos combos = new Combos(unknowns, numUnknownsToSplit);

        CreatureInfoList possibleSplits = new CreatureInfoList();
        for (Iterator it = combos.iterator(); it.hasNext(); )
        {
            List combo = (List)it.next();
            CreatureInfoList pos = new CreatureInfoList();
            pos.addAll(knownSplit);
            pos.addAll(combo);
            if (getHeight() != 8)
            {
                possibleSplits.add(pos);
            }
            else
            {
                Node posnode = new Node("none", -1, pos, this);
                if (posnode.isLegalInitialSplitoff())
                {
                    possibleSplits.add(pos);
                }
            }
        }
        return possibleSplits;
    }

    // TODO Use SimpleAI version?
    /** Decide how to split this legion, and return a list of Creatures to 
     *  remove.  Return null on error. */
    CreatureInfoList chooseCreaturesToSplitOut(List pos)
    {
        List firstElement = (List)pos.get(0);
        boolean maximize = (2 * firstElement.size() > getHeight());
        int bestKillValue = -1;
        CreatureInfoList creaturesToRemove = new CreatureInfoList();
        for (Iterator it = pos.iterator(); it.hasNext(); )
        {
            CreatureInfoList li = (CreatureInfoList)it.next();
            int totalKillValue = 0;
            for (Iterator it2 = li.iterator(); it2.hasNext(); )
            {
                CreatureInfo ci = (CreatureInfo)it2.next();
                Creature creature = Creature.getCreatureByName(ci.getName());
                totalKillValue += SimpleAI.getKillValue(creature);
            }
            if ((bestKillValue == -1) ||
                    (!maximize && totalKillValue < bestKillValue) ||
                    (maximize && totalKillValue > bestKillValue))
            {
                bestKillValue = totalKillValue;
                creaturesToRemove = li;
            }
        }
        return creaturesToRemove;
    }

    final int REUSE_EXISTING_TURN = -1;

    void split(int childSize, String otherMarkerId, int turn)
    {
        if (creatures.size() > 8)
        {
            throw new PredictSplitsException("> 8 creatures in legion");
        }

        if (turn == REUSE_EXISTING_TURN)
        {
            turn = turnSplit;
        }
        else
        {
            turnSplit = turn;
        }

        CreatureInfoList knownKeep = new CreatureInfoList();
        CreatureInfoList knownSplit = new CreatureInfoList();
        if (child1 != null)
        {
            knownKeep.addAll(child1.getCertainAtSplitOrRemovedCreatures());
            knownSplit.addAll(child2.getCertainAtSplitOrRemovedCreatures());
        }

        CreatureInfoList pos = findAllPossibleSplits(childSize, knownKeep,
                knownSplit);
        CreatureInfoList splitoffCreatures = chooseCreaturesToSplitOut(pos);
        List splitoffNames = splitoffCreatures.getCreatureNames();

        List knownKeepNames = new ArrayList();
        List knownSplitNames = new ArrayList();
        if (allCertain())
        {
            List creatureNames = creatures.getCreatureNames();
            List posSplitNames = new ArrayList();
            List posKeepNames = new ArrayList();
            for (Iterator it = pos.iterator(); it.hasNext(); )
            {
                CreatureInfoList cil = (CreatureInfoList)it.next();
                List names = cil.getCreatureNames();
                posSplitNames.add(names);
                posKeepNames.add(subtractLists(creatureNames, names));
            }
            for (Iterator it = creatureNames.iterator(); it.hasNext(); )
            {
                String name = (String)it.next();
                if (!knownKeepNames.contains(name))
                {
                    int minKeep = minCount(posKeepNames, name);
                    for (int i = 0; i < minKeep; i++)
                    {
                        knownKeepNames.add(name);
                    }
                }
                if (!knownSplitNames.contains(name))
                {
                    int minSplit = minCount(posSplitNames, name);
                    for (int i = 0; i < minSplit; i++)
                    {
                        knownSplitNames.add(name);
                    }
                }
            }
        }
        /* If either knownKeep or knownSplit is the full size of that
           child legion, then the certainty of creatures in the other
           child legion is the same as in the parent. */
        else
        {
            if (knownSplit.size() == childSize)
            {
                List certain = getCertainCreatures();
                for (Iterator it = knownSplit.iterator(); it.hasNext(); )
                {
                    CreatureInfo ci = (CreatureInfo)it.next();
                    certain.remove(ci);
                }
                knownKeep = (CreatureInfoList)certain;
            }
            else if (knownKeep.size() == childSize)
            {
                List certain = getCertainCreatures();
                for (Iterator it = knownKeep.iterator(); it.hasNext(); )
                {
                    CreatureInfo ci = (CreatureInfo)it.next();
                    certain.remove(ci);
                }
                knownSplit = (CreatureInfoList)certain;
            }

            knownKeepNames = knownKeep.getCreatureNames();
            knownSplitNames = knownSplit.getCreatureNames();
        }

        CreatureInfoList strongList = new CreatureInfoList();
        CreatureInfoList weakList = new CreatureInfoList();
        for (Iterator it = creatures.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            String name = ci.getName();
            CreatureInfo newinfo = new CreatureInfo(name, false, true);
            if (splitoffNames.contains(name))
            {
                weakList.add(newinfo);
                splitoffNames.remove(name);
                // If in knownSplit, set certain
                if (knownSplitNames.contains(name))
                {
                    knownSplitNames.remove(name);
                    newinfo.setCertain(true);
                }
            }
            else
            {
                strongList.add(newinfo);
                // If in knownKeep, set certain
                if (knownKeepNames.contains(name))
                {
                    knownKeepNames.remove(name);
                    newinfo.setCertain(true);
                }
            }
        }

        CreatureInfoList afterSplit1 = new CreatureInfoList();
        CreatureInfoList afterSplit2 = new CreatureInfoList();
        CreatureInfoList removed1 = new CreatureInfoList();
        CreatureInfoList removed2 = new CreatureInfoList();
        if (child1 != null)
        {
            afterSplit1.addAll(child1.getAfterSplitCreatures());
            afterSplit2.addAll(child2.getAfterSplitCreatures());
            removed1.addAll(child1.getRemovedCreatures());
            removed2.addAll(child2.getRemovedCreatures());
        }

        String marker1 = markerId;
        String marker2 = otherMarkerId;

        CreatureInfoList strongFinal = new CreatureInfoList();
        strongFinal.addAll(strongList);
        strongFinal.addAll(afterSplit1);
        for (Iterator it = removed1.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            strongFinal.remove(ci);
        }
        CreatureInfoList weakFinal = new CreatureInfoList();
        weakFinal.addAll(weakList);
        weakFinal.addAll(afterSplit2);
        for (Iterator it = removed2.iterator(); it.hasNext(); )
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            weakFinal.remove(ci);
        }

        if (child1 == null)
        {
            child1 = new Node(marker1, turn, strongFinal, this);
            child2 = new Node(marker2, turn, weakFinal, this);
        }
        else
        {
            child1.setCreatures(strongFinal);
            child2.setCreatures(weakFinal);
        }

        if (childSize1 == 0)
        {
            childSize1 = child1.getHeight();
            childSize2 = child2.getHeight();
        }

        child1.resplitDescendents();
        child2.resplitDescendents();
    }

    /** Recombine this legion and other, because it was not possible to
     *  move.  They must share a parent.  If either legion has the parent's
     *  markerId, then that legion will remain.  Otherwise this legion
     *  will remain.  Also used to undo splits.*/
    void merge(Node other, int turn)
    {
        if (other == null)
        {
            return;
        }
        if (parent != other.parent)
        {
            Log.warn("Can't merge non-siblings");
            return;
        }
        if (getMarkerId().equals(parent.getMarkerId()) ||
                other.getMarkerId().equals(parent.getMarkerId()))
        {
            // Cancel split.
            parent.clearChildren();
        }
        else
        {
            // Cancel split, then resplit parent into just this legion.
            parent.clearChildren();
            parent.split(getHeight() + other.getHeight(), markerId, turn);
        }
    }

    /** Tell this parent legion the updated contents of its children. */
    void updateChildContents()
    {
        List cnl = new ArrayList();
        cnl.addAll(child1.getCertainAtSplitOrRemovedCreatures().
                getCreatureNames());
        cnl.addAll(child2.getCertainAtSplitOrRemovedCreatures().
                getCreatureNames());
        revealCreatures(cnl);
        split(childSize2, getOtherChildMarkerId(), REUSE_EXISTING_TURN);
    }

    void resplitDescendents()
    {
        if (child1 != null)
        {
            split(childSize2, getOtherChildMarkerId(), REUSE_EXISTING_TURN);
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

        CreatureInfo ci = creatures.getCreatureInfo(creatureName);
        if (ci == null)
        {
            throw new PredictSplitsException(
                    "Tried to remove nonexistant creature");
        }

        // Only need to track the removed creature for future parent split
        // predictions if it was here at the time of the split.
        if (ci.isAtSplit())
        {
            removed.add(ci);
        }
        creatures.removeCreatureByName(creatureName);
    }

    void removeCreatures(List creatureNames)
    {
        for (Iterator it = creatureNames.iterator(); it.hasNext(); )
        {
            String name = (String)it.next();
            removeCreature(name);
        }
    }

    public int compareTo(Object object)
    {
        if (object instanceof Node)
        {
            Node other = (Node)object;
            return (toString().compareTo(other.toString()));
        }
        else
        {
            throw new ClassCastException();
        }
    }

    static List subtractLists(List big, List little)
    {
        ArrayList li = new ArrayList();
        li.addAll(big);
        for (Iterator it = little.iterator(); it.hasNext(); )
        {
            Object el = it.next();
            li.remove(el);
        }
        return li;
    }

    /** Return the number of times name occurs in li */
    static int count(List li, String name)
    {
        int num = 0;
        for (Iterator it = li.iterator(); it.hasNext(); )
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
        for (Iterator it = lili.iterator(); it.hasNext(); )
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


// TODO Make this a checked exception.
class PredictSplitsException extends RuntimeException
{
    PredictSplitsException(String s)
    {
        super(s);
    }
}


class NodeTurnComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        if (o1 instanceof Node && o2 instanceof Node)
        {
            Node n1 = (Node)o1;
            Node n2 = (Node)o2;
            int diff = n1.getTurnCreated() - n2.getTurnCreated();
            if (diff != 0)
            {
                return diff;
            }
            diff = n1.getParent().toString().compareTo(
                    n2.getParent().toString());
            if (diff != 0)
            {
                return diff;
            }
            diff = n2.getCreatures().size() - n1.getCreatures().size();
            if (diff != 0)
            {
                return diff;
            }
            return (n1.toString().compareTo(n2.toString()));
        }
        else
        {
            throw new ClassCastException();
        }
    }
}
