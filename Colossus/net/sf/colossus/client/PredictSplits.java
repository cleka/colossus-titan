package net.sf.colossus.client;

import java.util.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Dice;
import net.sf.colossus.server.Constants;

/**
 *  Predicts splits for one enemy player, and adjusts predictions as 
 *  creatures are revealed.
 *  @version $Id$
 *  @author David Ripton
 *  @see SplitPrediction.txt
 */

public final class PredictSplits
{
    private String playerName;
    private Node root;           // All contents of root must be known.


    PredictSplits(String playerName, String rootId, 
        CreatureNameList creatureNames)
    {
        CreatureInfoList infoList = new CreatureInfoList();
        Iterator it = creatureNames.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            CreatureInfo ci = new CreatureInfo(name, true, true);
            infoList.add(ci);
        }
        root = new Node(rootId, 0, infoList, null);
    }


    /** Return all non-empty childless nodes in subtree starting from node. */
    java.util.List getLeaves(Node node)
    {
        java.util.List leaves = new ArrayList();
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
                    }
                    else if (leaf1.getTurnCreated() < leaf2.getTurnCreated())
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
        // Remove in reverse order to keep indexes consistent.
        Iterator it = prunes.iterator();
        while (it.hasNext())
        {
            Integer in = (Integer)it.next();
            leaves.remove(in.intValue());
        }

        return leaves;
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
        java.util.List leaves = getLeaves(root);
        Iterator it = leaves.iterator();
        while (it.hasNext())
        {
            Node leaf = (Node)it.next();
            Log.debug(leaf.toString());
        }
    }

    /** Return the leaf node with matching markerId. */
    Node getLeaf(String markerId)
    {
        java.util.List leaves = getLeaves(root);
        Iterator it = leaves.iterator();
        while (it.hasNext())
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


/** Sort creatures in decreasing order of importance.  Keep identical 
 *  creatures together with a secondary sort by creature name. */
final class CreatureInfoComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        CreatureInfo info1 = (CreatureInfo)o1;
        CreatureInfo info2 = (CreatureInfo)o2;
        Creature creature1 = Creature.getCreatureByName(info1.getName());
        Creature creature2 = Creature.getCreatureByName(info2.getName());
        int diff = SimpleAI.getKillValue(creature2) - 
            SimpleAI.getKillValue(creature1);
        if (diff != 0)
        {
            return diff;
        }
        else
        {
            return creature1.getName().compareTo(creature2.getName());
        }
    }
}


class Node
{
    final String markerId;       // Not unique!
    final int turnCreated;
    CreatureInfoList creatures = new CreatureInfoList();
    CreatureInfoList removed = new CreatureInfoList(); // only if atSplit
    final Node parent;
    int childSize1 = 0;          // At the time this node was split.
    int childSize2 = 0;          // At the time this node was split.
    Node child1;                 // At the time this node was split.
    Node child2;                 // At the time this node was split.
    boolean flipped;
    int turnSplit = -1;
    static CreatureInfoComparator cic = new CreatureInfoComparator();

    Node(String markerId, int turnCreated, CreatureInfoList cil, Node parent)
    {
        this.markerId = markerId;
        this.turnCreated = turnCreated;
        this.creatures = (CreatureInfoList)cil.clone();
        this.parent = parent;
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

    int getTurnCreated()
    {
        return turnCreated;
    }

    public String toString()
    {
        Collections.sort(creatures, cic);
        StringBuffer sb = new StringBuffer(getFullName() + ":");
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            sb.append(" " + ci.toString());
        }
        return sb.toString();
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getCreatures()
    {
        return creatures;
    }

    void setCreatures(CreatureInfoList creatures)
    {
        this.creatures = creatures;
    }

    /** Return list of CreatureInfo */
    CreatureInfoList getRemovedCreatures()
    {
        return removed;
    }

    /** Return list of CreatureInfo where certain == true. */
    CreatureInfoList getCertainCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
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
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (!ci.isCertain())
            {
                return false;
            }
        }
        return true;
    }

    void setAllCertain()
    {
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            ci.setCertain(true);
        }
    }

    /** Return list of CreatureInfo where atSplit == true. */
    CreatureInfoList getAtSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        return list;
    }

    /** Return list of CreatureInfo where atSplit == false. */
    CreatureInfoList getAfterSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (!ci.isAtSplit())
            {
                list.add(ci);
            }
        }
        return list;
    }

    /** Return list of CreatureInfo where both certain and atSplit are true. */
    CreatureInfoList getCertainAtSplitCreatures()
    {
        CreatureInfoList list = new CreatureInfoList();
        Iterator it = getCreatures().iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.isCertain() && ci.isAtSplit())
            {
                list.add(ci);
            }
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
            Log.error("Node.tellChildContents() Not my child");
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


    void revealSomeCreatures(CreatureNameList cnl)
    {
Log.debug("revealSomeCreatures() for " + this + " " + cnl);
        CreatureInfoList cil = new CreatureInfoList();
        Iterator it = cnl.iterator();
        while (it.hasNext())
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
        it = certain.iterator();
        while (it.hasNext())
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
                "Certainty error in revealSomeCreatures -- count is " + count);
        }

        // Mark passed creatures as certain and then communicate this to
        // parent, to adjust other legions.

        if (getCertainCreatures().size() == getHeight())
        {
            // We already know everything.
            return;
        }

        dupe = (CreatureInfoList)cil.clone();
        count = 0;
        it = dupe.iterator();
        while (it.hasNext())
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
        it = certain.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (dupe.contains(ci))
            {
                dupe.remove(ci);
            }
        }
        it = dupe.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            Iterator it2 = creatures.iterator();
            while (it2.hasNext())
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

Log.debug("revealSomeCreatures() " + this);
        parent.tellChildContents(this);
    }

    void revealAllCreatures(CreatureNameList cnl)
    {
        CreatureInfoList cil = new CreatureInfoList();
        Iterator it = cnl.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            cil.add(new CreatureInfo(name, true, true));
        }

        // Confirm that all creatures that were certain are there.
        CreatureInfoList certain = getCertainCreatures();
        it = certain.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (cil.numCreature(ci.getName()) < 
                certain.numCreature(ci.getName()))
            {
                throw new PredictSplitsException(
                    "Certainty error in revealAllCreatures()");
            }
        }
        revealSomeCreatures(cnl);
    }

    /** Return true if creatures in children are consistent with self. */
    boolean childCreaturesMatch()
    {
        if (child1 == null)
        {
            return true;
        }

        CreatureInfoList allCreatures = new CreatureInfoList();
        allCreatures.addAll(child1.getAtSplitCreatures());
        allCreatures.addAll(child1.getRemovedCreatures());
        allCreatures.addAll(child2.getAtSplitCreatures());
        allCreatures.addAll(child2.getRemovedCreatures());

        Iterator it = allCreatures.iterator();
        while (it.hasNext())
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


    // TODO Merge with SimpleAI version.
    /** Decide how to split this legion, and return a list of Creatures to 
     *  remove.  Return null on error. */
    CreatureInfoList chooseCreaturesToSplitOut(int childSize, 
        CreatureInfoList knownSplit, CreatureInfoList knownKeep)
    {
        // Sanity checks
        if (knownSplit.size() > childSize)
        {
            Log.error("More known splitoffs than spiltoffs");
            return null;
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
        if (!creatures.isSupersetOf(knownCombo))
        {
            throw new PredictSplitsException("Known creatures not in parent");
        }

        CreatureInfoList clones = (CreatureInfoList)creatures.clone();

        // Move the weaker creatures to the end of the legion.
        Collections.sort(clones, cic);

        // Move known splitoffs to the end of the legion.
        Iterator it = knownSplit.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            clones.remove(ci);
            clones.add(clones.size() - 1, ci);
        }

        // Move known non-splitoffs to the beginning.
        CreatureInfoList temp = new CreatureInfoList();  // no removeLast()
        it = knownKeep.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            clones.remove(ci);
            temp.add(ci);
        }
        it = temp.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            clones.add(0, ci);
        }

        // If initial split, move angel to the end.
        if (getHeight() == 8)
        {
            // Maintain flags.
            CreatureInfo angel = clones.getCreatureInfo(Constants.angel);
            clones.remove(angel);
            clones.add(angel);
        }

        CreatureInfoList creaturesToRemove = new CreatureInfoList();
        for (int i = 0; i < childSize; i++)
        {
            CreatureInfo last = (CreatureInfo)clones.remove(
                clones.size() - 1);
            last.setAtSplit(true);
            creaturesToRemove.add(last);
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
            knownKeep.addAll(child1.getCertainAtSplitCreatures());
            knownKeep.addAll(child1.getRemovedCreatures());
            knownSplit.addAll(child2.getCertainAtSplitCreatures());
            knownSplit.addAll(child2.getRemovedCreatures());
        }

        CreatureInfoList splitoffCreatures = chooseCreaturesToSplitOut(
            childSize, knownSplit, knownKeep);
        CreatureNameList splitoffNames = new CreatureNameList();
        Iterator it = splitoffCreatures.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            splitoffNames.add(ci.getName());
        }
Log.debug("splitoffNames is " + splitoffNames);

        CreatureInfoList strongList = new CreatureInfoList();
        CreatureInfoList weakList = new CreatureInfoList();
        it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            String name = ci.getName();
            CreatureInfo newinfo = new CreatureInfo(name, false, true);
            if (splitoffNames.contains(name))
            {
                weakList.add(newinfo);
                splitoffNames.remove(name);
                // If in knownSplit, set certain
                if (knownSplit.removeCreatureByName(name))
                {
                    newinfo.setCertain(true);
                }
            }
            else
            {
                strongList.add(newinfo);
                // If in knownKeep, set certain
                if (knownKeep.removeCreatureByName(name))
                {
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

        // Assume that the bigger stack got the better creatures.
        if (childSize + childSize <= getHeight())
        {
            flipped = false;
        }
        else
        {
            flipped = true;
        }

        String marker1 = null;
        String marker2 = null;
        if (flipped)
        {
            marker1 = otherMarkerId;
            marker2 = markerId;
        }
        else
        {
            marker1 = markerId;
            marker2 = otherMarkerId;
        }

        CreatureInfoList li1 = new CreatureInfoList();
        li1.addAll(strongList);
        li1.addAll(afterSplit1);
        it = removed1.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            li1.remove(ci);
        }
        CreatureInfoList li2 = new CreatureInfoList();
        li2.addAll(weakList);
        li2.addAll(afterSplit2);
        it = removed2.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            li2.remove(ci);
        }

        if (child1 == null)
        {
            child1 = new Node(marker1, turn, li1, this);
            child2 = new Node(marker2, turn, li2, this);
        }
        else
        {
            child1.setCreatures(li1);
            child2.setCreatures(li2);
        }

        if (childSize1 == 0)
        {
            childSize1 = child1.getHeight();
            childSize2 = child2.getHeight();
        }
Log.debug("child1: " + child1);
Log.debug("child2: " + child2);
    }

    /** Tell this parent legion the known contents of one of its
     *  children.  Based on this info, it may be able to tell its
     *  other child and/or its parent something. */
    void tellChildContents(Node child)
    {
        Node otherChild = getOtherChild(child);

        // Add child creatures that were there at the time of the split
        // should be in this node as well.  If not, then we need to
        // adjust this legion's contents and tell its parent and other kid.
        CreatureInfoList childAtSplit = child.getAtSplitCreatures();

Log.debug("tellChildContents for node " + this + " from node " + child);
Log.debug("child atSplit is " + childAtSplit);


        if (!creatures.isSupersetOf(childAtSplit))
        {
            if (parent == null)
            {
                throw new PredictSplitsException("Root legion contents wrong");
            }
            else
            {
                // Re-predict this node's parent's split.
Log.debug("Re-predicting parent split");
                revealSomeCreatures(childAtSplit.getCreatureNames());
            }
        }

        // Only resplit if necessary.
        if (!childCreaturesMatch())
        {
            split(childSize2, getOtherChildMarkerId(), 
                REUSE_EXISTING_TURN);
        }

        // Mark child's known-at-split contents as certain in this node.
        CreatureInfoList certain = child.getCertainAtSplitCreatures();
        Iterator it = certain.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (creatures.numCreature(ci.getName()) < 
                certain.numCreature(ci.getName()))
            {
                throw new PredictSplitsException("Need to resplit my parent");
            }
            Iterator it2 = creatures.iterator();
            while (it2.hasNext())
            {
                CreatureInfo mine = (CreatureInfo)it2.next();
                if (mine.getName().equals(ci.getName()) && 
                    !mine.isCertain())
                {
                    mine.setCertain(true);
                    break;
                }
            }
        }

        // If parent and one child are certain, so is the other child.
        if (allCertain() && child.allCertain())
        {
            otherChild.setAllCertain();
        }
    }


    void addCreature(String creatureName)
    {
        if (getHeight() >= 7 && child1 == null)
        {
            throw new PredictSplitsException("Tried adding to 7-high legion");
        }
        CreatureInfo ci = new CreatureInfo(creatureName, true, false);
        creatures.add(ci);
    }

    void removeCreature(String creatureName)
    {
        if (getHeight() <= 0)
        {
            throw new PredictSplitsException(
                "Tried removing from 0-high legion");
        }
        CreatureInfo ci = creatures.getCreatureInfo(creatureName);
        CreatureNameList cnl = new CreatureNameList();
        cnl.add(ci.getName());
        revealSomeCreatures(cnl);

        // Only need to track the removed creature for future parent split
        // predictions if it was here at the time of the split.
        if (ci.isAtSplit())
        {
            removed.add(ci);
        }
        creatures.removeCreatureByName(creatureName);
    }

    void removeCreatures(CreatureNameList creatureNames)
    {
        Iterator it = creatureNames.iterator();
        while (it.hasNext())
        {
            String name = (String)it.next();
            removeCreature(name);
        }
    }

    void removeAllCreatures()
    {
        CreatureNameList cnl = new CreatureNameList();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            cnl.add(ci.getName());
        }
        removeCreatures(cnl);
    }
}


/** Simple naming class, for clarity. */
class CreatureNameList extends ArrayList
{
}


class CreatureInfoList extends ArrayList
{
    int numCreature(String creatureName)
    {
        int count = 0;
        Iterator it = iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (creatureName.equals(ci.getName()))
            {
                count++;
            }
        }
        return count;
    }

    /** Return the first CreatureInfo that matches the passed name. */
    CreatureInfo getCreatureInfo(String creatureName)
    {
        Iterator it = iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (ci.getName().equals(creatureName))
            {
                return ci;
            }
        }
        return null;
    }

    void removeLastUncertainCreature()
    {
        ListIterator lit = this.listIterator(this.size());
        while (lit.hasPrevious())
        {
            CreatureInfo ci = (CreatureInfo)lit.previous();
            if (!ci.isCertain())
            {
                lit.remove();
                return;
            }
        }
        Log.error("No uncertain creatures");
    }

    boolean isSupersetOf(CreatureInfoList other)
    {
        Iterator it = other.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (numCreature(ci.getName()) < 
                other.numCreature(ci.getName()))
            {
                return false;
            }
        }
        return true;
    }

    /** Remove the first element matching name.  Return true if found. */
    boolean removeCreatureByName(String name)
    {
        Iterator it = iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            if (name.equals(ci.getName()))
            {
                it.remove();
                return true;
            }
        }
        return false;
    }

    CreatureNameList getCreatureNames()
    {
        CreatureNameList list = new CreatureNameList();
        Iterator it = iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = (CreatureInfo)it.next();
            list.add(ci.getName());
        }
        return list;
    }
}


class CreatureInfo implements Cloneable
{
    private final String name;
    
    // Are we sure this creature is in this legion?
    private boolean certain; 

    // Was the creature here when this legion was split off?
    private boolean atSplit; 

    CreatureInfo(String name, boolean certain, boolean atSplit)
    {
        this.name = name;
        this.certain = certain;
        this.atSplit = atSplit;
    }


    String getName()
    {
        return name;
    }

    void setCertain(boolean certain)
    {
        this.certain = certain;
    }

    boolean isCertain()
    {
        return certain;
    }

    void setAtSplit(boolean atSplit)
    {
        this.atSplit = atSplit;
    }

    boolean isAtSplit()
    {
        return atSplit;
    }


    public Object clone()
    {
        return new CreatureInfo(name, certain, atSplit);
    }

    /** Two CreatureInfo objects match if the names match. */
    public boolean equals(Object other)
    {
        if (!(other instanceof CreatureInfo))
        {
            throw new ClassCastException();
        }
        return name.equals(((CreatureInfo)other).name);
    }

    /** Two CreatureInfo objects match if the names match. */
    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(name);
        if (!certain)
        {
            sb.append('?');
        }
        if (!atSplit)
        {
            sb.append('*');
        }
        return sb.toString();
    }
}

// XXX RuntimeException for now -- handle later
class PredictSplitsException extends RuntimeException
{
    PredictSplitsException(String s)
    {
        super(s);
    }
}

