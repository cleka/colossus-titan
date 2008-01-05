package net.sf.colossus.variant;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.colossus.game.Game;


/**
 * Hub for all variant-specific information.
 * 
 * This class is meant to give access to all the information about a Colossus
 * game in the static sense: the master board layout, the battle board layouts,
 * available creatures, rules, etc. The information about a game in progress is
 * in the {@link Game} class.
 * 
 * Instances of this class are immutable.
 */
public class Variant
{
    private final List creatureTypes;
    private final List battleLands;
    private final MasterBoard masterBoard;

    public Variant(List creatureTypes, List battleLands,
        MasterBoard masterBoard)
    {
        // defensive copies to ensure immutability
        this.creatureTypes = new ArrayList(creatureTypes);
        this.battleLands = new ArrayList(battleLands);
        this.masterBoard = masterBoard;
    }

    public List getCreatureTypes()
    {
        return Collections.unmodifiableList(this.creatureTypes);
    }

    public List getBattleLands()
    {
        return Collections.unmodifiableList(this.battleLands);
    }

    public MasterBoard getMasterBoard()
    {
        return masterBoard;
    }
}
