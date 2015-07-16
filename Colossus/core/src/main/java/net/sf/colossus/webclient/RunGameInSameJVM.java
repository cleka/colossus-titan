package net.sf.colossus.webclient;


import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.appmain.GetPlayers;
import net.sf.colossus.common.IStartHandler;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.server.INotifyWebServer;
import net.sf.colossus.server.StartGameForWebclient;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IGameRunner;


public class RunGameInSameJVM extends Thread implements IGameRunner,
    INotifyWebServer
{
    private static final Logger LOGGER = Logger
        .getLogger(RunGameInSameJVM.class.getName());

    /** To exchange data between us and the GetPlayersWeb dialog
     *  when game is started locally
     */
    private final Options presetOptions;

    private final WebClient webClient;

    private final WhatNextManager whatNextManager;

    private static WebClient initiatingWebClient = null;

    private final String username;

    private final IStartHandler startHandler;

    public RunGameInSameJVM(GameInfo gi, WhatNextManager whatNextMgr,
        String username, WebClient webClient)
    {
        this.whatNextManager = whatNextMgr;
        this.username = username;
        this.webClient = webClient;

        setName("RunGameInSameJVM gameId " + gi.getGameId());
        gi.setGameRunner(this);
        LOGGER.info("RunGameInSameJVM for gameId " + gi.getGameId()
            + " created.");

        startHandler = new StartGameForWebclient(whatNextManager);

        presetOptions = new Options("server", true);
        gi.storeToOptionsObject(presetOptions, username, false);
    }

    @Override
    public void run()
    {
        runGameInSameJVM();
    }

    public void runGameInSameJVM()
    {
        // starts a runnable which waits on a mutex until
        // GetPlayersWeb dialog notifies the mutex;
        // when that happens, the runnable starts the game by calling
        // doInitiateStartLocally().
        runGetPlayersDialogAndWait(presetOptions, whatNextManager);
    }

    /*
     * Bring up the GetPlayersWeb dialog and then we wait,
     * until is has set startObject to the next action to do
     * and notified us to continue.
     */
    void runGetPlayersDialogAndWait(Options presetOptions,
        WhatNextManager whatNextManager)
    {
        Object playersDialogMutex = new Object();
        new GetPlayers(presetOptions, playersDialogMutex, whatNextManager,
            true);
        whatNextManager.setWhatToDoNext(WhatToDoNext.START_WEB_CLIENT, false);

        synchronized (playersDialogMutex)
        {
            try
            {
                playersDialogMutex.wait();
                LOGGER.info("GetPlayersWeb dialog notified us "
                    + "that it is ready.");
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, "WebClient.runGetPlayers"
                    + "DialogAndWait waiting for GetPlayersWeb "
                    + "to complete, wait interrupted?");
            }
        }
        playersDialogMutex = null;

        LOGGER.info("Initiating the local game starting");
        initiatingWebClient = getWebClient();
        startHandler.startWebGameLocally(presetOptions, username);
    }

    public void tellServerToInformOtherPlayers()
    {
        LOGGER.info("Started the server, informing other players!");

        String hostingPlayer = getHostingPlayerName();
        String hostingHost = getHostingHost();
        int hostingPort = getHostingPort();

        // Tell webServer to inform the other WebClients that
        // they can connect now.
        webClient.informStartingOnPlayerHost(hostingPlayer, hostingHost,
            hostingPort);
    }

    public static void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            LOGGER.log(Level.FINEST,
                "sleepFor: InterruptException caught... ignoring it...");
        }
    }

    private WebClient getWebClient()
    {
        return this.webClient;
    }

    /**
     * if a Game Server game was started locally on players computer,
     * then GameServerSide queries the starting web client from here.
     *
     * @return The last WebClient that initiated a game start.
     */
    public static synchronized WebClient getInitiatingWebClient()
    {
        WebClient wc = initiatingWebClient;
        initiatingWebClient = null;
        return wc;
    }

    public String getHostingPlayerName()
    {
        return username;
    }

    public String getHostingHost()
    {
        return "localhost";
    }

    public int getHostingPort()
    {
        return presetOptions.getIntOption(Options.serveAtPort);
    }

    // TODO make some reasonable use of the methods below which are needed
    // to satify the interface
    public boolean makeRunningGame()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean tryToStart()
    {
        return true;
    }

    public void setServerNull()
    {
        // TODO Auto-generated method stub
    }

    public boolean waitUntilGameStartedSuccessfully(int timeout)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean waitUntilReadyToAcceptClients(int timeout)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isActive()
    {
        return webClient != null;
    }

    public void readyToAcceptClients()
    {
        tellServerToInformOtherPlayers();
    }

    public void gotClient(String playerName, boolean remote)
    {
        LOGGER.info("SameJVM: Got " + (remote ? "remote" : "local")
            + " player " + playerName);
    }

    public void allClientsConnected()
    {
        LOGGER.info("SameJVM: All Clients connected!");
    }

    public void gameStartupCompleted()
    {
        LOGGER.info("SameJVM: Game Startup completed!");
    }

    public void gameStartupFailed(String reason)
    {
        LOGGER.info("SameJVM: Game Startup Failed, reason: " + reason);
    }

    // Probably not really in use right now, just for the interface
    public void serverStoppedRunning()
    {
        LOGGER.info("SameJVM: Server Stopped Running.");
    }

    // Not really in use right now, just for the interface
    public void gameIsSuspended()
    {
        LOGGER.info("SameJVM: Game is suspended.");
    }

}
