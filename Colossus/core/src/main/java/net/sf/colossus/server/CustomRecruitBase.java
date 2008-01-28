package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 * Base class to implement custom recruiting functions 
 *   (i.e. anything that is not a-number-of-creature to another creature)
 * @version $Id$
 * @author Romain Dolbeau
 */
abstract public class CustomRecruitBase
{
    private static final Logger LOGGER = Logger
        .getLogger(CustomRecruitBase.class.getName());

    protected static List<Player> allPlayers = new ArrayList<Player>();
    // TODO consider storing the Game instances instead, which would give access to both Caretaker and Player
    // instances
    private static List<Caretaker> allCaretakerInfo = new ArrayList<Caretaker>();
    private static GameServerSide serverGame = null;
    private static List<CustomRecruitBase> allCustomRecruitBase = new ArrayList<CustomRecruitBase>();

    public CustomRecruitBase()
    {
        LOGGER.log(Level.FINEST, "CUSTOM: adding " + getClass().getName());
        allCustomRecruitBase.add(this);
    }

    /* full reset (change variant) */
    synchronized public static final void reset()
    {
        allPlayers.clear();
        allCaretakerInfo.clear();
        allCustomRecruitBase.clear();
        serverGame = null;
    }

    /* partial reset (change game) */
    synchronized public static final void resetAllInstances()
    {

        allPlayers.clear();
        allCaretakerInfo.clear();
        serverGame = null;

        Iterator<CustomRecruitBase> it = allCustomRecruitBase.iterator();
        while (it.hasNext())
        {
            CustomRecruitBase crb = it.next();
            crb.resetInstance();
        }
    }

    synchronized public static final void everyoneAdvanceTurn(
        int newActivePlayer)
    {
        Iterator<CustomRecruitBase> it = allCustomRecruitBase.iterator();
        while (it.hasNext())
        {
            CustomRecruitBase crb = it.next();
            crb.changeOfTurn(newActivePlayer);
        }
    }

    synchronized public static final void addPlayerClientSide(Player pi)
    {
        allPlayers.add(pi);
    }

    synchronized public static final void addCaretakerClientSide(Caretaker ci)
    {
        allCaretakerInfo.add(ci);
    }

    synchronized public static final void setGame(GameServerSide g)
    {
        serverGame = g;
    }

    synchronized protected final void setCount(CreatureType type, int newCount)
    {
        // first update all known CaretakerInfo (if we're client(s))
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            Caretaker ci = it.next();
            ci.setAvailableCount(type, newCount);
        }
        // update the Caretaker if we're server
        if (serverGame != null)
        {
            // first update the server's count
            serverGame.getCaretaker().setAvailableCount(type, newCount);
        }
    }

    synchronized protected final int getCount(CreatureType type)
    {
        int count = -1;
        int oldcount = -1;
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext() && (count == -1))
        {
            Caretaker ci = it.next();
            oldcount = count;
            count = ci.getAvailableCount(type);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, not all CaretakerInfo's"
                        + " count match !");
            }
        }
        // second, update the Caretaker if we're server
        if ((serverGame != null) && (count == -1))
        {
            oldcount = count;
            count = serverGame.getCaretaker().getAvailableCount(type);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, Caretaker's count "
                        + "doesn't match CaretakerInfo's counts!");
            }
        }
        return count;
    }

    synchronized protected final void setDeadCount(CreatureType type,
        int newDeadCount)
    {
        // first update all known CaretakerInfo (if we're client(s))
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            Caretaker ci = it.next();
            ci.setDeadCount(type, newDeadCount);
        }
        // second, update the Caretaker if we're server
        if (serverGame != null)
        {
            // same comments as setCount() above
            serverGame.getCaretaker().setDeadCount(type, newDeadCount);
        }
    }

    synchronized protected final int getDeadCount(CreatureType type)
    {
        int count = -1;
        int oldcount = -1;
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext() && (count == -1))
        {
            Caretaker ci = it.next();
            oldcount = count;
            count = ci.getDeadCount(type);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, not all CaretakerInfo's "
                        + "dead count match !");
            }
        }
        // second, update the Caretaker if we're server
        if ((serverGame != null) && (count == -1))
        {
            oldcount = count;
            count = serverGame.getCaretaker().getDeadCount(type);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, Caretaker's dead count "
                        + "doesn't match CaretakerInfo's counts!");
            }
        }
        return count;
    }

    synchronized protected final Legion getRecruitingLegion(MasterHex hex)
    {
        if (serverGame == null)
        {
            return null;
        }
        int num = serverGame.getNumLegions(hex);
        if (num == 0)
        {
            return null;
        }
        if (num == 1)
        { // only one Legion, it is the recruiting Legion
            Legion l = serverGame.getFirstLegion(hex);
            return l;
        }
        if (num == 2)
        { // 2 legions, so we're in a Battle. Only the defender can recruit.
            return serverGame.getBattle().getDefender();
        }
        // num > 2 this should not happen during recruiting, 
        //   as only a three-way split can do that.
        LOGGER.log(Level.WARNING, "CUSTOM: 3 legions in recruiting hex " + hex
            + " ?!?");
        return null;
    }

    /* subclasses must reimplement the following */

    /** List all Creature that can recruit in this terrain in a special way */
    abstract public List<CreatureType> getAllPossibleSpecialRecruiters(
        String terrain);

    /** List all Creature that can be recruited in this terrain 
     * in a special way */
    abstract public List<CreatureType> getAllPossibleSpecialRecruits(
        String terrain);

    /** List Creature that can recruit in this terrain in a special way now 
     *
     * TODO the terrain parameter might be superfluous
     */
    abstract public List<CreatureType> getPossibleSpecialRecruiters(
        String terrain, MasterHex hex);

    /** List Creature that can be recruited in this terrain 
     * in a special way now
     *
     * TODO the terrain parameter might be superfluous
     */
    abstract public List<CreatureType> getPossibleSpecialRecruits(
        String terrain, MasterHex hex);

    /** number of recruiter needed to get a recruit 
     * in a special way in this terrain now
     *
     * TODO the terrain parameter might be superfluous
     */
    abstract public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, String terrain, MasterHex hex);

    /** bookkeeping function, called once after every player turn.
     private as it should only be called from everyoneAdvanceTurn() */
    abstract protected void changeOfTurn(int newActivePlayer);

    /** reset, called at the beginning of a game */
    abstract protected void resetInstance();
}
