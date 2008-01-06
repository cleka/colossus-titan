package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Legion
{
    /**
     * The creatures in this legion.
     */
    private final List<Creature> creatures = new ArrayList<Creature>();

    public List<Creature> getCreatures()
    {
        return Collections.unmodifiableList(creatures);
    }
}
