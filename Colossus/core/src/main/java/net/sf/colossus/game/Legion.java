package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.List;

import net.sf.colossus.variant.MasterHex;


public class Legion
{
    /**
     * The player/game combination owning this Legion.
     */
    private final Player player;

    /**
     * The current position of the legion on the masterboard.
     */
    private MasterHex currentHex;

    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    protected final String markerId;

    private boolean moved;

    private boolean teleported;

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
     * TODO replace all occurrences with {@link #moveTo(MasterHex)}
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
    public void moveTo(MasterHex newPosition)
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
        return creatures.size();
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
}
