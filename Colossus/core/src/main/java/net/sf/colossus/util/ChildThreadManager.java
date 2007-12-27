package net.sf.colossus.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/** Keeps track of children threads (usually the socket threads), 
 *  for example at server side the one FileServerThread and one
 *  SocketServerThread per client.
 *  A child thread can register her, and when it finishes,
 *  i.e. when the run() method is ending, unregister again.
 *  The parent, usually the main() thread of the application,
 *  after starting all the threads (doing all needed setup for a 
 *  game), calls then waitUntilAllChildThreadsGone(); that one 
 *  returns when all children threads have unregistered.    
 *  (for this to work it is necessary, that each child thread, e.g.
 *  SocketServerThread, is properly exiting it's "wait on the socket
 *  input stream" loop).
 *  
 *  
 *  @version $Id$
 *  @author Clemens Katzer
 */

public class ChildThreadManager
{
    private static final Logger LOGGER =
        Logger.getLogger(ChildThreadManager.class.getName());

    private ArrayList childThreads;
    private ArrayList unregisteringChilds;

    private String id;
    private boolean debug = false;

    public ChildThreadManager(String id)
    {
        childThreads = new ArrayList();
        unregisteringChilds = new ArrayList();
        this.id = id;
        net.sf.colossus.webcommon.FinalizeManager.register(this, id);
    }

    public void setDebug(boolean val)
    {
        debug = val;
    }

    private synchronized void waitForSomething()
    {
        if (!unregisteringChilds.isEmpty())
        {
            printdebug("ChildThreadManager.waitForSomething() : no need to wait, something in the queue ");
            return;
        }

        try
        {
            this.notify();
            this.wait();
        }
        catch (InterruptedException e)
        {
            printdebug("ChildThreadManager.waitForSomething() : got interrupted...");
        }
        printdebug("ChildThreadManager.waitForSomething() : something happened");
    }

    private synchronized void reaper()
    {
        if (!unregisteringChilds.isEmpty())
        {
            Iterator it = unregisteringChilds.iterator();
            while (it.hasNext())
            {
                Thread child = (Thread)it.next();
                String name = child.getName();
                try
                {
                    child.join();
                    printdebug("-- ok, join for child " + name
                        + " went fine. Removing it from list...");
                    it.remove();
                    childThreads.remove(child);
                }
                catch (InterruptedException e)
                {
                    printdebug("###### reaper() #######: join for " + name
                        + " got InterruptedException!!");
                }
            }

            ArrayList list = new ArrayList();
            Iterator it2 = childThreads.iterator();
            while (it2.hasNext())
            {
                Thread c = (Thread)it2.next();
                String name = c.getName();
                list.add(name);
            }

            printdebug("\n###########\nreaper(): now there are "
                + childThreads.size() + " children threads left:"
                + list.toString());
        }
        else
        {
            printdebug("\n#######\nreaper(): list empty...");
        }
        System.gc();
        System.runFinalization();
    }

    private boolean allChildrenGone()
    {
        boolean gone = childThreads.isEmpty();
        printdebug("wUACTG(): allChildrenGone() returning " + gone);
        return gone;
    }

    public synchronized void waitUntilAllChildThreadsGone()
    {
        boolean done = this.allChildrenGone();
        while (!done)
        {
            printdebug("wUACTG(): before wait for something");
            this.waitForSomething();
            printdebug("wUACTG(): before reaper");
            this.reaper();
            printdebug("wUACTG(): before allChildrenGone");
            done = this.allChildrenGone();
            printdebug("wUACTG(): done now " + done);
        }
        printdebug("wUACTG(): after while loop");
    }

    public synchronized void registerToThreadManager(Thread child)
    {
        String name = child.getName();
        printdebug("Threadmanager: added " + name);
        childThreads.add(child);
    }

    public synchronized void unregisterFromThreadManager(Thread child)
    {
        printdebug("unregisterFromTM " + child);
        unregisteringChilds.add(child);

        printdebug("Unregistrering list now: " + 
            unregisteringChilds.toString());
        printdebug("Still alive    list now: " + childThreads.toString());
        printdebug("Callin notify...");
        this.notify();
    }

    public void cleanup()
    {
        childThreads.clear();
        unregisteringChilds.clear();
    }

    private void printdebug(String message)
    {
        if (debug)
        {
            LOGGER.log(Level.FINEST, "CTM " + id + ": " + message);
        }
    }
}
