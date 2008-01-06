package net.sf.colossus.server;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.CaretakerInfo;
import net.sf.colossus.client.PlayerInfo;


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

    protected static List<PlayerInfo> allPlayerInfo = new ArrayList<PlayerInfo>();
    private static List<CaretakerInfo> allCaretakerInfo = new ArrayList<CaretakerInfo>();
    private static Caretaker serverCaretaker = null;
    private static Game serverGame = null;
    private static List<CustomRecruitBase> allCustomRecruitBase = new ArrayList<CustomRecruitBase>();

    public CustomRecruitBase()
    {
        LOGGER.log(Level.FINEST, "CUSTOM: adding " + getClass().getName());
        allCustomRecruitBase.add(this);
    }

    /* full reset (change variant) */
    synchronized public static final void reset()
    {
        allPlayerInfo.clear();
        allCaretakerInfo.clear();
        allCustomRecruitBase.clear();
        serverGame = null;
        serverCaretaker = null;
    }

    /* partial reset (change game) */
    synchronized public static final void resetAllInstances()
    {

        allPlayerInfo.clear();
        allCaretakerInfo.clear();
        serverGame = null;
        serverCaretaker = null;

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

    synchronized public static final void addPlayerInfo(PlayerInfo pi)
    {
        allPlayerInfo.add(pi);
    }

    synchronized public static final void addCaretakerInfo(CaretakerInfo ci)
    {
        allCaretakerInfo.add(ci);
    }

    synchronized public static final void setCaretaker(Caretaker c)
    {
        serverCaretaker = c;
    }

    synchronized public static final void setGame(Game g)
    {
        serverGame = g;
    }

    synchronized protected final void setCount(String name, int newCount)
    {
        // first update all known CaretakerInfo (if we're client(s))
        Iterator<CaretakerInfo> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            CaretakerInfo ci = it.next();
            ci.updateCount(name, newCount, ci.getDeadCount(name));
        }
        // update the Caretaker if we're server
        if (serverCaretaker != null)
        {
            // first update the server's count
            serverCaretaker.setCount(name, newCount);
            // second force pushing the value to the client
            // the work might be duplicated, but this make
            // sure everything is coherent, and above make
            // sure the child class get the proper value
            // when calling getCount() even in a different JVM
            // not needed - this is done by setCount() already
            //serverCaretaker.updateDisplays(name);
        }
    }

    synchronized protected final int getCount(String name)
    {
        int count = -1;
        int oldcount = -1;
        Iterator<CaretakerInfo> it = allCaretakerInfo.iterator();
        while (it.hasNext() && (count == -1))
        {
            CaretakerInfo ci = it.next();
            oldcount = count;
            count = ci.getCount(name);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, not all CaretakerInfo's"
                        + " count match !");
            }
        }
        // second, update the Caretaker if we're server
        if ((serverCaretaker != null) && (count == -1))
        {
            oldcount = count;
            count = serverCaretaker.getCount(name);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, Caretaker's count "
                        + "doesn't match CaretakerInfo's counts!");
            }
        }
        return count;
    }

    synchronized protected final void setDeadCount(String name,
        int newDeadCount)
    {
        // first update all known CaretakerInfo (if we're client(s))
        Iterator<CaretakerInfo> it = allCaretakerInfo.iterator();
        while (it.hasNext())
        {
            CaretakerInfo ci = it.next();
            ci.updateCount(name, ci.getCount(name), newDeadCount);
        }
        // second, update the Caretaker if we're server
        if (serverCaretaker != null)
        {
            // same comments as setCount() above
            serverCaretaker.setDeadCount(name, newDeadCount);
            //serverCaretaker.updateDisplays(name);
        }
    }

    synchronized protected final int getDeadCount(String name)
    {
        int count = -1;
        int oldcount = -1;
        Iterator<CaretakerInfo> it = allCaretakerInfo.iterator();
        while (it.hasNext() && (count == -1))
        {
            CaretakerInfo ci = it.next();
            oldcount = count;
            count = ci.getDeadCount(name);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, not all CaretakerInfo's "
                        + "dead count match !");
            }
        }
        // second, update the Caretaker if we're server
        if ((serverCaretaker != null) && (count == -1))
        {
            oldcount = count;
            count = serverCaretaker.getDeadCount(name);
            if ((oldcount != -1) && (count != oldcount))
            {
                LOGGER.log(Level.SEVERE,
                    "in CustomRecruitBase, Caretaker's dead count "
                        + "doesn't match CaretakerInfo's counts!");
            }
        }
        return count;
    }

    synchronized protected final Legion getRecruitingLegion(String hexLabel)
    {
        if (serverGame == null)
        {
            return null;
        }
        int num = serverGame.getNumLegions(hexLabel);
        if (num == 0)
        {
            return null;
        }
        if (num == 1)
        { // only one Legion, it is the recruiting Legion
            Legion l = serverGame.getFirstLegion(hexLabel);
            return l;
        }
        if (num == 2)
        { // 2 legions, so we're in a Battle. Only the defender can recruit.
            return serverGame.getBattle().getDefender();
        }
        // num > 2 this should not happen during recruiting, 
        //   as only a three-way split can do that.
        LOGGER.log(Level.WARNING, "CUSTOM: 3 legions in recruiting hex "
            + hexLabel + " ?!?");
        return null;
    }

    /* subclasses must reimplement the following */

    /** List all Creature that can recruit in this terrain in a special way */
    abstract public List getAllPossibleSpecialRecruiters(String terrain);

    /** List all Creature that can be recruited in this terrain 
     * in a special way */
    abstract public List<Creature> getAllPossibleSpecialRecruits(String terrain);

    /** List Creature that can recruit in this terrain in a special way now */
    abstract public List<Creature> getPossibleSpecialRecruiters(String terrain,
        String hexLabel);

    /** List Creature that can be recruited in this terrain 
     * in a special way now */
    abstract public List<Creature> getPossibleSpecialRecruits(String terrain,
        String hexLabel);

    /** number of recruiter needed to get a recruit 
     * in a special way in this terrain now */
    abstract public int numberOfRecruiterNeeded(String recruiter,
        String recruit, String terrain, String hexLabel);

    /** bookkeeping function, called once after every player turn.
     private as it should only be called from everyoneAdvanceTurn() */
    abstract protected void changeOfTurn(int newActivePlayer);

    /** reset, called at the beginning of a game */
    abstract protected void resetInstance();
}
