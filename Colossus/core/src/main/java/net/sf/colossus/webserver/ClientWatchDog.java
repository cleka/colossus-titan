package net.sf.colossus.webserver;


import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webcommon.User;


/**
 *  Sends ping requests to clients to check whether they are still alive
 *  and connected.
 */
public class ClientWatchDog extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(ClientWatchDog.class
        .getName());

    private boolean done;

    private static final long CHECK_INTERVAL_SECONDS = 30;

    public ClientWatchDog()
    {
        done = false;
    }

    @Override
    public void run()
    {
        while (!done)
        {
            Collection<User> users = User.getLoggedInUsers();
            int cnt = users.size();
            if (cnt > 0)
            {
                // String names = User.getLoggedInNamesAsString(", ");
                // LOGGER.finest(cnt + " users logged in (" + names
                //    + "), checking them...");
                for (User u : users)
                {
                    WebServerClientSocketThread client = (WebServerClientSocketThread)u
                    .getThread();
                    if (client != null)
                    {
                        long now = new Date().getTime();
                        client.requestPingIfNeeded(now);
                        // client.checkMaxIdleTime(now);
                    }
                }
            }
            else
            {
                // LOGGER
                //    .finest("No users logged in, nothing to do for watchdog");
            }
            sleepFor(CHECK_INTERVAL_SECONDS * 1000);
        }
        LOGGER.info("Done flag set, watchdog ends now...");
    }

    public void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            if (done)
            {
                LOGGER.log(Level.FINEST,
                    "InterruptException caught and done is set - all right!");
            }
            else
            {
                LOGGER.log(Level.WARNING,
                    "InterruptException caught... ignoring it...");
            }
        }
    }

    public void shutdown()
    {
        done = true;
        this.interrupt();
    }

}
