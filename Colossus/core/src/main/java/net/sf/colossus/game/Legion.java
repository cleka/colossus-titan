package net.sf.colossus.game;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Legion
{
    /**
     * The creatures in this legion.
     * 
     * A List of {@link Creature}s.
     */
    private final List creatures = new ArrayList();

    public List getCreatures()
    {
        return Collections.unmodifiableList(creatures);
    }
}
