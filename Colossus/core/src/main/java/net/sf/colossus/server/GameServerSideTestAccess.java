package net.sf.colossus.server;


import java.util.HashMap;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.variant.Variant;


public class GameServerSideTestAccess extends GameServerSide
{
    private static final Logger LOGGER = Logger
        .getLogger(GameServerSideTestAccess.class.getName());

    private final HashMap<String, Client> localClients = new HashMap<String, Client>();

    private static GameServerSideTestAccess lastGame = null;

    /**
     * The normal constructor to be used everywhere
     * @param whatNextMgr A WhatNextManager object which manages the main
     *        control flow which thing to do 'next' when this game is over.
     * @param serverOptions The server side options, initialized from the
     *        GetPlayers dialog and/or command line options.
     * @param variant Variant of this game
     */
    public GameServerSideTestAccess(WhatNextManager whatNextMgr,
        Options serverOptions, Variant variant)
    {
        super(whatNextMgr, serverOptions, variant);

        lastGame = this;
        LOGGER.info("Test Access for GameServerSide instantiated.");
    }

    @Override
    protected void storeLocalClient(String playerName, Client c)
    {
        LOGGER.finest("GSSTestAccess: Created local client with name "
            + playerName + ", isNull: " + (c == null));

        localClients.put(playerName, c);

    }

    public void showLocalClients()
    {
        StringBuffer sb = new StringBuffer("Local clients are:");
        for (String name : localClients.keySet())
        {
            sb.append("\nCLIENT: " + name);
        }
        LOGGER.info(sb.toString());
    }

    public Client getClientForName(String name)
    {
        return localClients.get(name);
    }

    public static GameServerSideTestAccess getLastGame()
    {
        return lastGame;
    }

    public static void clearLastGame()
    {
        lastGame = null;
    }

    private final Object gameUpMutex = new Object();

    @Override
    protected void notifyTestCaseGameIsUpNow()
    {
        synchronized (gameUpMutex)
        {
            gameUpMutex.notify();
        }
    }

    static public GameServerSideTestAccess staticWaitThatGameComesUp()
    {
        for (int i = 0; i < 10; i++)
        {
            if (lastGame != null)
            {
                LOGGER.fine("i=" + i + ", got a game now...");
                lastGame.waitThatGameComesUp();
                LOGGER.fine("i=" + i + ", wait on Mutex returned!");
                return lastGame;
            }
            else
            {
                LOGGER.fine("i=" + i + ", lastGame still null...");
            }
            WhatNextManager.sleepFor(1000);
        }
        return null;
    }

    public void waitThatGameComesUp()
    {
        try
        {
            synchronized (gameUpMutex)
            {
                gameUpMutex.wait();
            }
        }
        catch (InterruptedException e)
        {
            LOGGER.severe("Waiting game to come up interrupted?!?");
        }
    }

    public void waitThatGameIsCompleted()
    {
        super.waitUntilGameFinishes();
    }

}
