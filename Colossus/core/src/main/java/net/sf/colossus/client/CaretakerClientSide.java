package net.sf.colossus.client;


import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.CreatureType;


/**
 *  Client-side cache of Caretaker.
 *  @version $Id$
 *  @author David Ripton
 *  
 *  TODO remove whole class by getting rid of String versions
 */
public final class CaretakerClientSide extends Caretaker
{
    public CaretakerClientSide(Game game)
    {
        super(game);
    }

    public void updateCount(String creatureName, int availableCount,
        int deadCount)
    {
        CreatureType type = getGame().getVariant().getCreatureByName(
            creatureName);
        setCount(type, availableCount);
        setDeadCount(type, deadCount);
    }

    public int getCount(String creatureName)
    {
        CreatureType type = getGame().getVariant().getCreatureByName(
            creatureName);
        return getCount(type);
    }

    public int getDeadCount(String creatureName)
    {
        CreatureType type = getGame().getVariant().getCreatureByName(
            creatureName);
        return getDeadCount(type);
    }
}
