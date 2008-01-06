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
    private final List<CreatureType> creatureTypes;
    private final List<BattleLand> battleLands;
    private final MasterBoard masterBoard;

    public Variant(List<CreatureType> creatureTypes,
        List<BattleLand> battleLands, MasterBoard masterBoard)
    {
        // defensive copies to ensure immutability
        this.creatureTypes = new ArrayList<CreatureType>(creatureTypes);
        this.battleLands = new ArrayList<BattleLand>(battleLands);
        this.masterBoard = masterBoard;
    }

    public List<CreatureType> getCreatureTypes()
    {
        return Collections.unmodifiableList(this.creatureTypes);
    }

    public List<BattleLand> getBattleLands()
    {
        return Collections.unmodifiableList(this.battleLands);
    }

    public MasterBoard getMasterBoard()
    {
        return masterBoard;
    }
}
