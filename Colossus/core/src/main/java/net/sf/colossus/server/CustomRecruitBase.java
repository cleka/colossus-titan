package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.game.Caretaker;
import net.sf.colossus.game.Player;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.ICustomRecruitBase;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * Base class to implement custom recruiting functions
 *   (i.e. anything that is not a-number-of-creature to another creature)
 *
 * @author Romain Dolbeau
 */
abstract public class CustomRecruitBase implements ICustomRecruitBase
{
    private static final Logger LOGGER = Logger
        .getLogger(CustomRecruitBase.class.getName());

    final protected static List<Player> allPlayers = new ArrayList<Player>();
    // TODO consider storing the Game instances instead, which would give access to both Caretaker and Player
    // instances
    private final static List<Caretaker> allCaretakerInfo = new ArrayList<Caretaker>();
    private static GameServerSide serverGame = null;
    private final static List<CustomRecruitBase> allCustomRecruitBase = new ArrayList<CustomRecruitBase>();

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

    protected synchronized void initCustomVariant()
    {
        // nothing to do, only Balrog needs this
    }

    synchronized static public final void initCustomVariantForAllCRBs()
    {
        Iterator<CustomRecruitBase> it = allCustomRecruitBase.iterator();
        while (it.hasNext())
        {
            CustomRecruitBase crb = it.next();
            crb.initCustomVariant();
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

    synchronized protected final void setCount(CreatureType type,
        int newCount, boolean reset)
    {
        // TODO Should only update server side, and propagate to all
        // clients via messages.
        // The way it's done now will probably fail for remote clients.

        // first update all known CaretakerInfo (if we're client(s))
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            Caretaker ci = it.next();
            if (reset)
            {
                ci.setAvailableCount(type, 0);
                ci.setDeadCount(type, 0);
            }
            else
            {
                ci.setAvailableCount(type, newCount);
            }
        }
        // update the Caretaker if we're server
        if (serverGame != null)
        {
            Caretaker ci = serverGame.getCaretaker();
            if (reset)
            {
                ci.setAvailableCount(type, 0);
                ci.setDeadCount(type, 0);
            }
            else
            {
                ci.setAvailableCount(type, newCount);
            }
        }
    }

    synchronized protected final void adjustAvailableCount(CreatureType type)
    {
        // first update all known CaretakerInfo (if we're client(s))
        Iterator<Caretaker> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            Caretaker ci = it.next();
            ci.adjustAvailableCount(type);
        }
        // update the Caretaker if we're server
        if (serverGame != null)
        {
            Caretaker ci = serverGame.getCaretaker();
            ci.adjustAvailableCount(type);
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

    /**
     * List all creatures that can recruit in this terrain in a special way.
     */
    abstract public List<CreatureType> getAllPossibleSpecialRecruiters(
        MasterBoardTerrain terrain);

    /**
     * List all creatures that can be recruited in this terrain
     * in a special way.
     */
    abstract public List<CreatureType> getAllPossibleSpecialRecruits(
        MasterBoardTerrain terrain);

    /**
     * List creatures that can recruit in this terrain in a special way now.
     * @param hex The specific MasterHex considered for recruiting.
     * @return A List of possible special Recruiters in this hex.
     */
    abstract public List<CreatureType> getPossibleSpecialRecruiters(
        MasterHex hex);

    /**
     * List creatures that can be recruited in this terrain
     * in a special way now.
     * @param hex The specific MasterHex considered for recruiting
     * (for an example, see getPossibleSpecialRecruits() in
     * BalrogRecruitment.java in Balrog variant directory)
     * @return A List of possible special Recruits in this hex.
     */
    abstract public List<CreatureType> getPossibleSpecialRecruits(MasterHex hex);

    /**
     * Number of recruiters needed to get a recruit
     * in a special way in this terrain now.
     */
    abstract public int numberOfRecruiterNeeded(CreatureType recruiter,
        CreatureType recruit, MasterHex hex);

    /**
     * Bookkeeping function, called once after every player turn.
     *
     * Protected as it should only be called from everyoneAdvanceTurn().
     */
    abstract protected void changeOfTurn(int newActivePlayer);

    /**
     * Reset, called at the beginning of a game.
     */
    abstract protected void resetInstance();
}
