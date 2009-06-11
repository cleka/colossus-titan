package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.Variant;


/**
 *  Predicts splits for one enemy player, and adjusts predictions as
 *  creatures are revealed.
 *
 *  @author David Ripton
 *  See docs/SplitPrediction.txt
 */

public final class PredictSplits
{
    private static final Logger LOGGER = Logger.getLogger(PredictSplits.class
        .getName());

    private final PredictSplitNode root; // All contents of root must be known.
    private final NodeTurnComparator nodeTurnComparator = new NodeTurnComparator();

    PredictSplits(String rootId, List<CreatureType> creatureTypes,
        Variant variant)
    {
        assert variant != null;
        CreatureInfoList infoList = new CreatureInfoList();
        for (CreatureType type : creatureTypes)
        {
            CreatureInfo ci = new CreatureInfo(type, true, true);
            infoList.add(ci);
        }
        root = new PredictSplitNode(rootId, 0, infoList, null, variant);
    }

    /** Return all non-empty childless nodes in subtree starting from node. */
    List<PredictSplitNode> getLeaves(PredictSplitNode node)
    {
        List<PredictSplitNode> leaves = new ArrayList<PredictSplitNode>();
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

        TreeSet<Integer> prunes = new TreeSet<Integer>(
            new ReverseIntegerComparator());

        // If duplicate markerIds, prune the older node.
        for (int i = 0; i < leaves.size(); i++)
        {
            for (int j = 0; j < leaves.size(); j++)
            {
                if (i != j)
                {
                    PredictSplitNode leaf1 = leaves.get(i);
                    PredictSplitNode leaf2 = leaves.get(j);
                    if (leaf1.getMarkerId().equals(leaf2.getMarkerId()))
                    {
                        assert leaf1.getTurnCreated() != leaf2
                            .getTurnCreated() : "Leaf nodes have to have different markerId or turn";
                        if (leaf1.getTurnCreated() < leaf2.getTurnCreated())
                        {
                            prunes.add(Integer.valueOf(i));
                        }
                        else
                        {
                            prunes.add(Integer.valueOf(j));
                        }
                    }
                }
            }
        }
        // Remove in reverse order to keep indexes consistent.
        for (Integer in : prunes)
        {
            leaves.remove(in.intValue());
        }

        return leaves;
    }

    /** Return all non-empty nodes in subtree starting from node. */
    List<PredictSplitNode> getNodes(PredictSplitNode node)
    {
        List<PredictSplitNode> nodes = new ArrayList<PredictSplitNode>();
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

    class ReverseIntegerComparator implements Comparator<Integer>
    {
        // Sort in reverse, so we don't disturb array
        // indexes when removing.
        public int compare(Integer in1, Integer in2)
        {
            return in2.compareTo(in1);
        }
    }

    /** Print all childless nodes in tree. */
    void printLeaves()
    {
        LOGGER.log(Level.FINEST, "");
        List<PredictSplitNode> leaves = getLeaves(root);
        Collections.sort(leaves);
        for (PredictSplitNode leaf : leaves)
        {
            LOGGER.log(Level.FINEST, leaf.toString());
        }
        LOGGER.log(Level.FINEST, "");
    }

    /** Print all nodes in tree. */
    void printNodes()
    {
        LOGGER.log(Level.FINEST, "");
        List<PredictSplitNode> nodes = getNodes(root);
        Collections.sort(nodes, nodeTurnComparator);
        for (PredictSplitNode node : nodes)
        {
            LOGGER.log(Level.FINEST, node.toString());
        }
        LOGGER.log(Level.FINEST, "");
    }

    /** Return the leaf PredictSplitNode with matching markerId. */
    PredictSplitNode getLeaf(String markerId)
    {
        List<PredictSplitNode> leaves = getLeaves(root);
        for (PredictSplitNode leaf : leaves)
        {
            if (markerId.equals(leaf.getMarkerId()))
            {
                return leaf;
            }
        }
        return null;
    }

    public PredictSplitNode getRoot()
    {
        return root;
    }
}


class NodeTurnComparator implements Comparator<PredictSplitNode>
{
    public int compare(PredictSplitNode n1, PredictSplitNode n2)
    {
        int diff = n1.getTurnCreated() - n2.getTurnCreated();
        if (diff != 0)
        {
            return diff;
        }
        diff = n1.getParent().toString().compareTo(n2.getParent().toString());
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
}
