package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


public abstract class Legion
{
    /**
     * The player/game combination owning this Legion.
     */
    private final Player player;

    /**
     * The current position of the legion on the masterboard.
     * 
     * TODO it would be good to establish an invariant "not null" on this one,
     *      which currently doesn't seem to be feasible yet -- even the setter
     *      still gets called with null on game starts
     */
    private MasterHex currentHex;

    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    protected final String markerId;

    private boolean moved;

    private boolean teleported;

    /**
     * The side this legion entered a battle in.
     */
    private int entrySide;

    // TODO legions should be created through factory from the player instances
    public Legion(final Player playerState, String markerId)
    {
        this.player = playerState;
        this.markerId = markerId;
    }

    public Player getPlayer()
    {
        return player;
    }

    /**
     * Places the legion using a hex label.
     * 
     * TODO replace all occurrences with {@link #setCurrentHex(MasterHex)}
     * 
     * @param hexLabel the label of the new hex to move to
     */
    public void setHexLabel(String hexLabel)
    {
        this.currentHex = player.getGame().getVariant().getMasterBoard()
            .getHexByLabel(hexLabel);
    }

    /**
     * Places the legion into the new position.
     * 
     * @param newPosition the hex that will be the new position
     * @see #getCurrentHex()
     */
    public void setCurrentHex(MasterHex newPosition)
    {
        this.currentHex = newPosition;
    }

    /**
     * Returns the current position of the legion as hex label.
     * 
     * @return the label of the hex the legion is currently on.
     * 
     * TODO remove in favor of {@link #getCurrentHex()}
     */
    public String getHexLabel()
    {
        return currentHex != null ? currentHex.getLabel() : null;
    }

    /**
     * Returns the current position of the legion.
     * 
     * @return the hex the legion currently is on.
     * 
     * @see #setCurrentHex(MasterHex)
     */
    public MasterHex getCurrentHex()
    {
        return currentHex;
    }

    /**
     * TODO should be an unmodifiable List, but can't at the moment since both
     * derived classes and users might still expect to change it 
     * TODO should be List<Creature>, but subtypes are still covariant
     */
    public List<? extends Creature> getCreatures()
    {
        return creatures;
    }

    public String getMarkerId()
    {
        return markerId;
    }

    public boolean hasTitan()
    {
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isTitan())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of creatures in this legion.
     * 
     * @return the number of creatures in the legion
     */
    public int getHeight()
    {
        return getCreatures().size();
    }

    public void setMoved(boolean moved)
    {
        this.moved = moved;
    }

    public boolean hasMoved()
    {
        return moved;
    }

    public void setTeleported(boolean teleported)
    {
        this.teleported = teleported;
    }

    public boolean hasTeleported()
    {
        return teleported;
    }

    public boolean contains(CreatureType type)
    {
        return getCreatures().contains(type);
    }

    public void setEntrySide(int entrySide)
    {
        this.entrySide = entrySide;
    }

    public int getEntrySide()
    {
        return entrySide;
    }

    /**
     * TODO unify between the two derived classes or even better: replace with code
     *      for getting the image 
     */
    public abstract List<String> getImageNames();

    /**
     * TODO unify between the two derived classes if possible -- the handling of Titans
     *      is quite different, although it should have the same result
     */
    public abstract int getPointValue();

    /**
     * TODO unify between the two derived classes if possible
     */
    public abstract boolean hasRecruited();

    public boolean hasSummonable()
    {
        for (Creature creature : getCreatures())
        {
            if (creature.getType().isSummonable())
            {
                return true;
            }
        }
        return false;
    }

    public boolean canFlee()
    {
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isLord())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * TODO unify between the two derived classes
     */
    public int numCreature(CreatureType creatureType)
    {
        int count = 0;
        for (Creature critter : getCreatures())
        {
            if (critter.getType().equals(creatureType))
            {
                count++;
            }
        }
        return count;
    }

    public int numLords()
    {
        int count = 0;
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isLord())
            {
                count++;
            }
        }
        return count;
    }

    public int numRangestrikers()
    {
        int count = 0;
        for (Creature critter : getCreatures())
        {
            if (critter.getType().isRangestriker())
            {
                count++;
            }
        }
        return count;
    }
}
