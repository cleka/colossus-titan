package net.sf.colossus.server;


import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Game;
import net.sf.colossus.variant.CreatureType;


/**
 *
 * Class Caretaker represents the caretaker's stacks.
 * It also contains the (preliminary) Graveyard.
 * 
 * TODO get rid of this class by removing all String-based methods and moving
 *      listener out
 * 
 * @version $Id$
 * @author Bruce Sherrod
 * @author David Ripton
 * @author Tom Fruchterman
 * @author Romain Dolbeau
 */

public final class CaretakerServerSide extends Caretaker
{
    CaretakerServerSide(Game game)
    {
        super(game);
        addListener(new ChangeListener()
        {
            @Override
            public void creatureTypeAvailabilityUpdated(CreatureType type,
                int availableCount)
            {
                updateDisplays(type);
            }

            @Override
            public void creatureTypeDeadCountUpdated(CreatureType type,
                int deadCount)
            {
                updateDisplays(type);
            }
        });
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

    public void setCount(String creatureName, int availableCount)
    {
        CreatureType type = getGame().getVariant().getCreatureByName(
            creatureName);
        setCount(type, availableCount);
    }

    public void setDeadCount(String creatureName, int deadCount)
    {
        CreatureType type = getGame().getVariant().getCreatureByName(
            creatureName);
        setDeadCount(type, deadCount);
    }

    /** 
     * Update creatureName's count on all clients.
     * 
     * TODO move out of this class into external listener
     * TODO use CreatureType as parameter
     */
    void updateDisplays(CreatureType type)
    {
        Server server = ((GameServerSide)getGame()).getServer();
        if (server != null)
        {
            server.allUpdateCreatureCount(type, getCount(type),
                getDeadCount(type));
        }
    }
}
