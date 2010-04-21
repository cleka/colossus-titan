package net.sf.colossus.client;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IOracleLegion;
import net.sf.colossus.variant.MasterHex;


/**
 * Client-side version of a legion.
 *
 * @author David Ripton
 */
public final class LegionClientSide extends Legion implements IOracleLegion
{
    private static final Logger LOGGER = Logger
        .getLogger(LegionClientSide.class.getName());

    private PredictSplitNode myNode;

    public LegionClientSide(Player player, String markerId, MasterHex hex)
    {
        super(player, markerId, hex);
        myNode = null;
    }

    private PredictSplitNode getNode(String markerId)
    {
        PredictSplits ps = getPlayer().getPredictSplits();
        PredictSplitNode node = ps.getLeaf(markerId);
        return node;
    }

    private PredictSplitNode getNode()
    {
        if (myNode == null)
        {
            myNode = getNode(getMarkerId());
        }
        return myNode;
    }

    @Override
    public int getHeight()
    {
        PredictSplitNode node = getNode();
        if (node == null)
        {
            return 0;
        }
        return node.getHeight();
    }

    /**
     * We don't use the creature list in this class yet, so we override this
     * to use the one from the {@link PredictSplitNode}.
     *
     * TODO fix this, particularly the use of creature names in here. Note that
     *      the current version also has the issue that every time this method
     *      is called a new list with new creatures is created, which will break
     *      identity checks.
     */
    @Override
    public List<? extends Creature> getCreatures()
    {
        List<Creature> result = new ArrayList<Creature>();
        for (String name : getContents())
        {
            result.add(new Creature(getPlayer().getGame().getVariant()
                .getCreatureByName(name), this));
        }
        return result;
    }

    /**
     * Return an immutable copy of the legion's contents, in sorted order.
     *
     * TODO get rid of this string-based version in favor of the typesafe ones
     */
    private List<String> getContents()
    {
        try
        {
            return Collections.unmodifiableList(getNode().getCreatures()
                .getCreatureNames());
        }
        catch (NullPointerException ex)
        {
            return new ArrayList<String>();
        }
    }

    /**
     * A less typesafe version of {@link #contains(CreatureType)}.
     *
     * TODO deprecate and remove
     */
    public boolean contains(String creatureName)
    {
        return getContents().contains(creatureName);
    }

    /**
     * TODO get rid of string-based version
     */
    public int numCreature(String creatureName)
    {
        int count = 0;
        for (CreatureType type : getCreatureTypes())
        {
            if (type.getName().equals(creatureName))
            {
                count++;
            }
        }
        return count;
    }

    /** Return a list of Strings.  Use the proper string for titans and
     *  unknown creatures. */
    public List<String> getImageNames()
    {
        List<String> names = new ArrayList<String>();
        names.addAll(getContents());
        int j = names.indexOf(Constants.titan);
        if (j != -1)
        {
            names.set(j, getPlayer().getTitanBasename());
        }
        while ((j = names.indexOf(Constants.angel)) != -1)
        {
            names.set(j, getPlayer().getAngelBasename());
        }
        return names;
    }

    /** Return a list of Booleans. */
    public List<Boolean> getCertainties()
    {
        List<Boolean> booleans = new ArrayList<Boolean>();
        List<CreatureInfo> cil = getNode().getCreatures();
        Iterator<CreatureInfo> it = cil.iterator();
        while (it.hasNext())
        {
            CreatureInfo ci = it.next();
            booleans.add(Boolean.valueOf(ci.isCertain()));
        }
        return booleans;
    }

    @Override
    public PlayerClientSide getPlayer()
    {
        return (PlayerClientSide)super.getPlayer();
    }

    /**
     * Add a new creature to this legion.
     */
    @Override
    public void addCreature(CreatureType creature)
    {
        getNode().addCreature(creature);
    }

    @Override
    public void removeCreature(CreatureType creature)
    {
        getNode().removeCreature(creature);
    }

    /**
     * Reveal creatures in this legion, some of which already may be known.
     */
    void revealCreatures(final List<CreatureType> creatures)
    {
        // TODO find better way of initialising the PredictSplits object in Player
        if (getPlayer().getPredictSplits() == null)
        {
            getPlayer().initPredictSplits(this, creatures);
        }
        getNode().revealCreatures(creatures);
    }

    void split(int childHeight, Legion child, int turn)
    {
        getNode().split(childHeight, child, turn);
        myNode = myNode.getChild1();
    }

    void merge(Legion splitoff)
    {
        LOGGER.log(Level.FINER, "LegionInfo.merge() for " + splitoff + " "
            + splitoff);
        getNode().merge(getNode(splitoff.getMarkerId()));
        // since this is potentially a merge of a 3-way split, be safe and
        // find the node again
        myNode = getNode(getMarkerId());
    }

    /** Return the point value of suspected contents of this legion. */
    @Override
    public int getPointValue()
    {
        int sum = 0;
        for (CreatureType type : getCreatureTypes())
        {
            if (type.isTitan())
            {
                // Titan skill is changed by variants.
                sum += getPlayer().getTitanPower() * type.getSkill();
            }
            else
            {
                sum += type.getPointValue();
            }
        }
        return sum;
    }

    /** Return the total point value of those creatures of this legion
     *  which are certain.
     */
    public int getCertainPointValue()
    {
        int sum = 0;
        for (CreatureType type : getNode().getCertainCreatures()
            .getCreatureTypes())
        {
            if (type.isTitan())
            {
                // Titan skill is changed by variants.
                sum += getPlayer().getTitanPower() * type.getSkill();
            }
            else
            {
                sum += type.getPointValue();
            }
        }
        return sum;
    }

    public int numUncertainCreatures()
    {
        return getNode().numUncertainCreatures();
    }
}
