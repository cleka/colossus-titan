package net.sf.colossus.client;


import java.util.*;

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
    private PredictSplitNode root;    // All contents of root must be known.
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
        root = new PredictSplitNode(rootId, 0, infoList, null);
    }

    /** Return all non-empty childless nodes in subtree starting from node. */
    List getLeaves(PredictSplitNode node)
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
                    PredictSplitNode leaf1 = (PredictSplitNode)leaves.get(i);
                    PredictSplitNode leaf2 = (PredictSplitNode)leaves.get(j);
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
    List getNodes(PredictSplitNode node)
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
            PredictSplitNode leaf = (PredictSplitNode)it.next();
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
            PredictSplitNode node = (PredictSplitNode)it.next();
            Log.debug(node.toString());
        }
        Log.debug("");
    }

    /** Return the leaf PredictSplitNode with matching markerId. */
    PredictSplitNode getLeaf(String markerId)
    {
        List leaves = getLeaves(root);
        for (Iterator it = leaves.iterator(); it.hasNext(); )
        {
            PredictSplitNode leaf = (PredictSplitNode)it.next();
            if (markerId.equals(leaf.getMarkerId()))
            {
                return leaf;
            }
        }
        return null;
    }
}


class NodeTurnComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        if (o1 instanceof PredictSplitNode && o2 instanceof PredictSplitNode)
        {
            PredictSplitNode n1 = (PredictSplitNode)o1;
            PredictSplitNode n2 = (PredictSplitNode)o2;
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
