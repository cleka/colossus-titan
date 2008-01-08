package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.colossus.Player;


public class Legion
{
    /**
     * The player who owns this Legion.
     */
    private final Player player;

    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    public Legion(final Player player)
    {
        this.player = player;
    }

    public Player getPlayer()
    {
        return player;
    }

    public List<Creature> getCreatures()
    {
        return Collections.unmodifiableList(creatures);
    }
}
