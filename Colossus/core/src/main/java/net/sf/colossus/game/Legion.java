package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
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

    public Legion(final PlayerState playerState)
    {
        this.playerState = playerState;
    }

    public PlayerState getPlayer()
    {
        return playerState;
    }

    public List<Creature> getCreatures()
    {
        return Collections.unmodifiableList(creatures);
    }
}
