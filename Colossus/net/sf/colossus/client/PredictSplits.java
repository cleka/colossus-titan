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
    private CreatureComparator creatureComp = new CreatureComparator();


    PredictSplits(String playerName, String rootId, CreatureInfoList creatures)
    {
        setRoot(new Node(rootId, creatures, null));
    }

    void setRoot(Node root)
    {
        this.root = root;
    }


    private class Node
    {
        final String markerId;       // Not unique!
        CreatureInfoList creatures;  // List of CreatureInfo

        int childSize1;              // At the time this node was split.
        int childSize2;              // At the time this node was split.

        final Node parent;
        Node child1;                 // At the time this node was split.
        Node child2;                 // At the time this node was split.

        Node(String markerId, CreatureInfoList creatures, Node parent)
        {
            this.markerId = markerId;
            this.creatures = (CreatureInfoList)creatures.clone();
            this.parent = parent;
        }

        String getMarkerId()
        {
            return markerId;
        }

        /** Return immutable list of CreatureInfo */
        CreatureInfoList getCreatures()
        {
            return (CreatureInfoList)Collections.unmodifiableList(creatures);
        }

        /** Return immutable list of CreatureInfo where certain == true. */
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
            return (CreatureInfoList)Collections.unmodifiableList(list);
        }

        /** Return immutable list of CreatureInfo where atSplit == true. */
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
            return (CreatureInfoList)Collections.unmodifiableList(list);
        }

        List getCreatureNames()
        {
            List list = new ArrayList();
            Iterator it = creatures.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                list.add(ci.getName());
            }
            return list;
        }

        int getHeight()
        {
            return creatures.size();
        }

        void revealAllCreatures(CreatureInfoList list)
        {
            if (getHeight() != creatures.size())
            {
                Log.error("Wrong height in Node.revealAllCreatures()");
                return;
            }

            // Confirm that all creatures that were certain are there.
            CreatureInfoList certain = getCertainCreatures();
            Iterator it = certain.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                if (list.numCreature(ci) < certain.numCreature(ci))
                {
                    Log.error("Certainty error in Node.revealAllCreatures()");
                    return;
                }
            }

            // Then mark all creatures as certain and then communicate this
            // to the parent, to adjust other legions.

            if (certain.size() == getHeight())
            {
                // No need to make changes -- we already know everything.
                return;
            }

            // Set atSplit false if the creature array already contained
            // a corresponding entry with atSplit false.
            CreatureInfoList copy = (CreatureInfoList)list.clone();
            it = copy.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                ci.setCertain(true);

                // If false, then we knew it was here, and can fix below.
                ci.setAtSplit(true); 
                if (creatures.contains(ci));
                {
                    CreatureInfo original = creatures.getCreatureInfo(ci);
                    boolean atSplit = original.isAtSplit();
                    ci.setAtSplit(atSplit);
                    creatures.remove(original);
                }
            }
            // Then copy the modified clone back to the creatures list.
            creatures = (CreatureInfoList)copy.clone();
            parent.tellChildContents(this);
        }

        void revealSomeCreatures(CreatureInfoList list)
        {
            if (getHeight() < creatures.size())
            {
                Log.error("Wrong height in Node.revealSomeCreatures()");
                return;
            }

            // Use a copy rather than the original so we can remove
            // creatures as we check, for multiples.
            CreatureInfoList copy = (CreatureInfoList)list.clone();

            int count = copy.size();
            // Confirm that all creatures that were certain still fit
            // along with the revealed creatures.
            Iterator it = getCertainCreatures().iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                if (!copy.remove(ci))
                {
                    count++;
                }
            }
            if (count > getHeight())
            {
                Log.error("Certainty error in Node.revealSomeCreatures()");
                return;
            }

            // Then mark passed creatures as certain and then 
            // communicate this to the parent, to adjust other legions.

            if (getCertainCreatures().size() == getHeight())
            {
                // No need -- we already know everything.
                return;
            }

            copy = (CreatureInfoList)list.clone();
            count = 0;
            it = copy.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                ci.setCertain(true);
                ci.setAtSplit(true);   // If not atSplit, would be certain.
                if (creatures.numCreature(ci) < copy.numCreature(ci))
                {
                    creatures.add(ci);
                    count++;
                }
            }

            // Need to remove count uncertain creatures.
            for (int i = 0; i < count; i++)
            {
                creatures.removeLastUncertainCreature();
            }

            parent.tellChildContents(this);
        }


        /*  flippedMarkers is true if the smaller splitoff got the 
         *  original legion marker. */
        void split(int childSize, String otherMarkerId,
            boolean flippedMarkers, CreatureInfoList knownSplitoffs)
        {
            CreatureInfoList splitoffCreatures = chooseCreaturesToSplitOut(
                childSize, knownSplitoffs);
            List splitoffNames = new ArrayList();
            Iterator it = splitoffCreatures.iterator();
            while (it.hasNext())
            {
                Creature creature = (Creature)it.next();
                splitoffNames.add(creature.getName());
            }

            // lists of CreatureInfo
            CreatureInfoList strongList = new CreatureInfoList();
            CreatureInfoList weakList = new CreatureInfoList();
            it = getCreatures().iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                String name = ci.getName();
                CreatureInfo newInfo = new CreatureInfo(name, false, true);
                if (splitoffNames.contains(name))
                {
                    weakList.add(newInfo);
                    splitoffNames.remove(name);
                }
                else
                {
                    strongList.add(newInfo);
                }
            }

            // Assume that the bigger stack got the better creatures.
            // If the two splitoffs have equal sizes, then we don't know
            // which is the "good" one, so just pick at random.
            if (childSize + childSize == getHeight())
            {
                flippedMarkers = (Dice.rollDie() <= 3);
            }
            if (flippedMarkers)
            {
                child1 = new Node(otherMarkerId, strongList, this);
                childSize1 = child1.getHeight();
                child2 = new Node(markerId, weakList, this);
                childSize2 = child2.getHeight();
            }
            else
            {
                child1 = new Node(markerId, strongList, this);
                childSize1 = child1.getHeight();
                child2 = new Node(otherMarkerId, weakList, this);
                childSize2 = child2.getHeight();
            }
        }

        /** Tell this parent legion the known contents of one of its
         *  children.  Based on this info, it may be able to tell its
         *  other child and/or its parent something. */
        void tellChildContents(Node child)
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
                return;
            }


            // All child creatures that were there at the time of the
            // split should be in this legion as well.  If not, then
            // we need to adjust this legion's contents and tell its parent
            // and other child.
            if (!creatures.contains(child.getAtSplitCreatures()))
            {
Log.debug("Adjusting parent legion " + markerId);

// TODO

            }

        }


        // TODO Merge with SimpleAI version.
        /** Decide how to split this legion, and return a list of Creatures to 
         *  remove.  Return null on error. */
        CreatureInfoList chooseCreaturesToSplitOut(int childSize, 
            CreatureInfoList knownSplitoffs)
        {
            // Sanity checks
            if (knownSplitoffs.size() > childSize)
            {
                Log.error("More known splitoffs than spiltoffs");
                return null;
            }
            if (creatures.size() == 8)
            {
                if (childSize != 4)
                {
                    Log.error("Illegal initial split");
                    return null;
                }
                if (!getCreatureNames().contains(Constants.angel))
                {
                    Log.error("No angel in 8-high legion");
                    return null;
                }
            }
            if (!creatures.contains(knownSplitoffs))
            {
                Log.error("Known splitoffs not in parent legion");
                return null;
            }

            CreatureInfoList clones = (CreatureInfoList)creatures.clone();

            // Move the weaker creatures to the end of the legion.
            Collections.sort(clones, creatureComp);

            // Move known splitoffs to the end of the legion.
            Iterator it = knownSplitoffs.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                clones.remove(ci);
                clones.add(clones.size() - 1, ci);
            }

            // If initial split, move angel to the end.
            if (getHeight() == 8)
            {
                // Maintain flags.
                CreatureInfo angel = creatures.getCreatureInfo(
                    new CreatureInfo(Constants.angel, false, false));
                clones.remove(angel);
                clones.add(clones.size() - 1, angel);
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
    }




    private class CreatureInfo implements Cloneable
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
    }

    private class CreatureInfoList extends ArrayList
    {
        int numCreature(CreatureInfo type)
        {
            int count = 0;
            Iterator it = this.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                if (ci.equals(type))
                {
                    count++;
                }
            }
            return count;
        }

        /** Return the first CreatureInfo that matches the passed type. */
        CreatureInfo getCreatureInfo(CreatureInfo type)
        {
            Iterator it = this.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                if (ci.equals(type))
                {
                    return ci;
                }
            }
            return null;
        }

        /** Return true if this list contains all elements of the other
         *  list, including the right number of duplicates. */
        boolean contains(CreatureInfoList other)
        {
            CreatureInfoList dup = (CreatureInfoList)this.clone();
            Iterator it = other.iterator();
            while (it.hasNext())
            {
                CreatureInfo ci = (CreatureInfo)it.next();
                if (!dup.remove(ci))
                {
                    return false;
                }
            }
            return true;
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
    }


    /** Sort creatures in decreasing order of importance.  Keep identical 
     *  creatures together with a secondary sort by creature name. */
    final class CreatureComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            Creature creature1 = (Creature)o1;
            Creature creature2 = (Creature)o2;
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
}

