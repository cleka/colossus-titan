package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.List;


public class Legion
{
    /**
     * The player/game combination owning this Legion.
     */
    private final PlayerState playerState;

    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    protected final String markerId;

    // TODO legions should be created through factory from the player instances
    public Legion(final PlayerState playerState, String markerId)
    {
        this.playerState = playerState;
        this.markerId = markerId;
    }

    public PlayerState getPlayer()
    {
        return playerState;
    }

    /**
     * TODO should be an unmodifable List<Creature>, but can't at the moment since both
     * derived classes and users might still expect to change it using the subtype they
     * know of
     */
    public List<? extends Creature> getCreatures()
    {
        return creatures;
    }

    public String getMarkerId()
    {
        return markerId;
    }
}
