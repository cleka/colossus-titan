package net.sf.colossus.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.IClient;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Options;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */
public final class Server implements IServer
{
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static StartupProgress startLog;
    
    private Game game;

    /**
     *  Maybe also save things like the originating IP, in case a
     *  connection breaks and we need to authenticate reconnects.
     *  Do not share these references. */
    private List clients = new ArrayList();
    private List remoteClients = new ArrayList();
    private List remoteLogHandlers = new ArrayList();

    /** Map of player name to client. */
    private Map clientMap = new HashMap();

    /** Number of remote clients we're waiting for. */
    private int waitingForClients;

    /** Server socket port. */
    private int port;

    // Cached strike information.
    private Critter striker;
    private Critter target;
    private int strikeNumber;
    private List rolls;

    // Network stuff
    private ServerSocket serverSocket;
    // list of Socket that are currently active
    private List activeSocketList =
            Collections.synchronizedList(new ArrayList());
    private int numClients;
    private int maxClients;

    /* static so that new instance of Server can destroy a
     * previously allocated FileServerThread */
    private static Thread fileServerThread = null;

    Server(Game game, int port)
    {
        this.game = game;
        this.port = port;

        if ( startLog != null ) 
        {
            this.startLog.dispose();
            this.startLog = null;
        }
        this.startLog = new StartupProgress(this);

        waitingForClients = game.getNumLivingPlayers();
    }

    void initSocketServer()
    {
        numClients = 0;
        maxClients = game.getNumLivingPlayers();
        LOGGER.log(Level.FINEST, "initSocketServer maxClients = " + maxClients);
        LOGGER.log(Level.FINEST, "About to create server socket on port " + port);
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(port, Constants.MAX_MAX_PLAYERS);
            serverSocket.setReuseAddress(true);
        }
        catch (IOException ex)
        {
            Log.error("Could not create socket. Configure networking in OS.", ex);
            System.exit(1);
        }
        createLocalClients();

        logToStartLog("\nStarting up, waiting for " + waitingForClients 
                      + " clients");
        while (numClients < maxClients)
        {
            waitForConnection();
        }
    }

    void initFileServer()
    {
        // must be called *after* initSocketServer()
        // this may induce a race condition
        // if the client ask for a file *before*
        // the file server is up.

        LOGGER.log(Level.FINEST, "About to create file server socket on port " + (port + 1));

        if (fileServerThread != null)
        {
            try
            {
                LOGGER.log(Level.FINEST, "Stopping the FileServerThread ");
                ((FileServerThread)fileServerThread).stopGoingOn();
                fileServerThread.interrupt();
                fileServerThread.join();
            }
            catch (Exception e)
            {
                LOGGER.log(Level.FINEST, "Oups couldn't stop the FileServerThread " + e);
            }
            fileServerThread = null;
        }

        if (!activeSocketList.isEmpty())
        {
            fileServerThread = new FileServerThread(activeSocketList,
                    port + 1);
            fileServerThread.start();
        }
        else
        {
            LOGGER.log(Level.FINEST, "No active remote client, not lauching the file server.");
        }
    }

    private void waitForConnection()
    {
        Socket clientSocket = null;
        try
        {
            clientSocket = serverSocket.accept();
            Log.event("Got client connection from " +
                    clientSocket.getInetAddress().toString());
            logToStartLog("Got client connection from IP: " 
                   + clientSocket.getInetAddress().toString());

            synchronized (activeSocketList)
            {
                activeSocketList.add(clientSocket);
            }
            numClients++;
        }
        catch (IOException ex)
        {
            Log.error(ex);
            return;
        }

        new SocketServerThread(this, clientSocket, activeSocketList).start();
    }

    /** Each server thread's name is set to its player's name. */
    String getPlayerName()
    {
        return Thread.currentThread().getName();
    }

    private Player getPlayer()
    {
        return game.getPlayer(getPlayerName());
    }

    /** return true if the active player is the player owning this client */
    private boolean isActivePlayer()
    {
        return getPlayerName().equals(game.getActivePlayerName());
    }

    private boolean isBattleActivePlayer()
    {
        return game.getBattle() != null &&
                game.getBattle().getActivePlayerName() != null &&
                getPlayerName().equals(game.getBattle().getActivePlayerName());
    }

    private void createLocalClients()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            if (!player.isDead() &&
                    !player.getType().endsWith(Constants.network))
            {
                createLocalClient(player.getName());
            }
        }
    }

    private void createLocalClient(String playerName)
    {
        LOGGER.log(Level.FINEST, "Called Server.createLocalClient() for " + playerName);
        new Client("127.0.0.1", port, playerName, false);
    }

    synchronized void addClient(final IClient client, final String playerName,
            final boolean remote)
    {
        LOGGER.log(Level.FINEST, "Called Server.addClient() for " + playerName);
        clients.add(client);

        if (remote)
        {
            addRemoteClient(client, playerName);
            logToStartLog("Remote player " + playerName + " signed on.");
        }
        else
        {
            addLocalClient(client, playerName);
            logToStartLog("Local player " + playerName + " signed on.");
        }

        waitingForClients--;
        Log.event("Decremented waitingForClients to " + waitingForClients);
        
        if ( waitingForClients > 0 )
        {
            String pluralS = (waitingForClients > 1 ? "s" : "");
            logToStartLog(" ==> Waiting for " + waitingForClients
                    + " more client" + pluralS + " to sign on.\n");
        }
          
        if (waitingForClients <= 0)
        {
            logToStartLog("\nGot all clients, starting the game.\n");
            if (game.isLoadingGame())
            {
                game.loadGame2();
            }
            else
            {
                game.newGame2();
            }
            startLog.setCompleted();
        }
    }

    private void addLocalClient(final IClient client, final String playerName)
    {
        clientMap.put(playerName, client);
    }

    private void addRemoteClient(final IClient client, final String playerName)
    {
        String name = playerName;
        int slot = game.findNetworkSlot(playerName);
        if (slot == -1)
        {
            return;
        }

        RemoteLogHandler remoteLogHandler = new RemoteLogHandler();
        remoteLogHandler.setServer(this);
        LOGGER.addHandler(remoteLogHandler);
        remoteLogHandlers.add(remoteLogHandler);

        remoteClients.add(client);

        if (!game.isLoadingGame())
        {
            name = game.getUniqueName(playerName);
        }

        clientMap.put(name, client);
        Player player = game.getPlayer(slot);
        player.setName(name);
        // In case we had to change a duplicate name.
        setPlayerName(name, name);
    }

    void disposeAllClients()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.dispose();
        }
        clients.clear();
        
        for (Iterator iter = remoteLogHandlers.iterator(); iter.hasNext(); )
        {
            RemoteLogHandler handler = (RemoteLogHandler) iter.next();
            LOGGER.removeHandler(handler);
        }
        remoteLogHandlers.clear();
        
        if (serverSocket != null)
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException ex)
            {
                Log.error(ex.toString());
            }
        }
    }

    void allUpdatePlayerInfo(boolean treatDeadAsAlive)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.updatePlayerInfo(getPlayerInfo(treatDeadAsAlive));
        }
    }

    void allUpdatePlayerInfo()
    {
        allUpdatePlayerInfo(false);
    }

    void allUpdateCreatureCount(String creatureName, int count, int deadCount)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.updateCreatureCount(creatureName, count, deadCount);
        }
    }

    void allTellMovementRoll(int roll)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellMovementRoll(roll);
        }
    }

    public void leaveCarryMode()
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called leaveCarryMode()");
            return;
        }
        Battle battle = game.getBattle();
        battle.leaveCarryMode();
    }

    public void doneWithBattleMoves()
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() +
                    " illegally called doneWithBattleMoves()");
            getClient(getPlayerName()).nak(Constants.doneWithBattleMoves,
                    "Illegal attempt to end phase");
            return;
        }
        Battle battle = game.getBattle();
        battle.doneWithMoves();
    }

    public synchronized void doneWithStrikes()
    {
        Battle battle = game.getBattle();
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doneWithStrikes()");
            getClient(getPlayerName()).nak(Constants.doneWithStrikes,
                    "Wrong player");
        }
        else if (!battle.getBattlePhase().isFightPhase())
        {
            getClient(getPlayerName()).nak(Constants.doneWithStrikes,
                    "Wrong phase");
        }
        else if (!battle.doneWithStrikes())
        {
            getClient(getPlayerName()).nak(Constants.doneWithStrikes,
                    "Forced strikes remain");
        }
    }

    private IClient getClient(String playerName)
    {
        if (clientMap.containsKey(playerName))
        {
            return (IClient)clientMap.get(playerName);
        }
        else
        {
            return null;
        }
    }

    synchronized void allInitBoard()
    {
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (!player.isDead())
            {
                IClient client = getClient(player.getName());
                if (client != null)
                {
                    client.initBoard();
                }
            }
        }
    }

    synchronized void allTellAllLegionLocations()
    {
        List markerIds = game.getAllLegionIds();
        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            allTellLegionLocation(markerId);
        }
    }

    void allTellLegionLocation(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        String hexLabel = legion.getCurrentHexLabel();

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellLegionLocation(markerId, hexLabel);
        }
    }

    void allRemoveLegion(String markerId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.removeLegion(markerId);
        }
    }

    void allTellPlayerElim(String playerName, String slayerName,
            boolean updateHistory)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellPlayerElim(playerName, slayerName);
        }
        if (updateHistory)
        {
            game.playerElimEvent(playerName, slayerName);
        }
    }

    void allTellGameOver(String message)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellGameOver(message);
        }
    }

    /** Needed if loading game outside the split phase. */
    synchronized void allSetupTurnState()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupTurnState(game.getActivePlayerName(),
                    game.getTurnNumber());
        }
    }

    void allSetupSplit()
    {
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            IClient client = getClient(player.getName());
            if (client != null)
            {
                client.setupSplit(game.getActivePlayerName(),
                        game.getTurnNumber());
            }
        }
        allUpdatePlayerInfo();
    }

    void allSetupMove()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupMove();
        }
    }

    void allSetupFight()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupFight();
        }
    }

    void allSetupMuster()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupMuster();
        }
    }

    void allSetupBattleSummon()
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleSummon(battle.getActivePlayerName(),
                    battle.getTurnNumber());
        }
    }

    void allSetupBattleRecruit()
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleRecruit(battle.getActivePlayerName(),
                    battle.getTurnNumber());
        }
    }

    void allSetupBattleMove()
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleMove(battle.getActivePlayerName(),
                    battle.getTurnNumber());
        }
    }

    void allSetupBattleFight()
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            if (battle != null)
            {
                client.setupBattleFight(battle.getBattlePhase(),
                        battle.getActivePlayerName());
            }
        }
    }

    synchronized void allPlaceNewChit(Critter critter)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.placeNewChit(critter.getName(),
                critter.getMarkerId().equals(game.getBattle().getDefenderId()),
                critter.getTag(), critter.getCurrentHexLabel());
        }
    }

    void allRemoveDeadBattleChits()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.removeDeadBattleChits();
        }
    }

    void allTellEngagementResults(String winnerId, String method,
                                    int points, int turns)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellEngagementResults(winnerId, method, points, turns);
        }
    }

    void nextEngagement()
    {
        IClient client = getClient(game.getActivePlayerName());
        client.nextEngagement();
    }

    /** Find out if the player wants to acquire an angel or archangel. */
    synchronized void askAcquireAngel(String playerName, String markerId,
            List recruits)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion.getHeight() < 7)
        {
            IClient client = getClient(playerName);
            if (client != null)
            {
                client.askAcquireAngel(markerId, recruits);
            }
        }
    }

    public synchronized void acquireAngel(String markerId, String angelType)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            if (!getPlayerName().equals(legion.getPlayerName()))
            {
                Log.error(getPlayerName() + " illegally called acquireAngel()");
                return;
            }
            legion.addAngel(angelType);
        }
    }

    void createSummonAngel(Legion legion)
    {
        IClient client = getClient(legion.getPlayerName());
        client.createSummonAngel(legion.getMarkerId());
    }

    void reinforce(Legion legion)
    {
        IClient client = getClient(legion.getPlayerName());
        client.doReinforce(legion.getMarkerId());
    }

    public void doSummon(String markerId, String donorId, String angel)
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doSummon()");
            return;
        }
        Legion legion = game.getLegionByMarkerId(markerId);
        Legion donor = game.getLegionByMarkerId(donorId);
        Creature creature = null;
        if (angel != null)
        {
            creature = Creature.getCreatureByName(angel);
        }
        game.doSummon(legion, donor, creature);
    }

    /**
     * Handle mustering for legion.
     * if recruiting with nothing, recruiterName is a non-null String
     * that contains "null".
     */
    public void doRecruit(String markerId, String recruitName,
            String recruiterName)
    {
        IClient client = getClient(getPlayerName());

        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called doRecruit()");
            client.nak(Constants.doRecruit, "Wrong player");
            return;
        }

        if (legion != null && (legion.hasMoved() || game.getPhase() ==
                Constants.Phase.FIGHT) && legion.canRecruit())
        {
            legion.sortCritters();
            Creature recruit = null;
            Creature recruiter = null;
            if (recruitName != null)
            {
                recruit = Creature.getCreatureByName(recruitName);
                recruiter = Creature.getCreatureByName(recruiterName);
                if (recruit != null)
                {
                    game.doRecruit(legion, recruit, recruiter);
                }
            }

            if (!legion.canRecruit())
            {
                didRecruit(legion, recruit, recruiter);
            }
        }
        else
        {
            Log.error("Illegal recruit");
            client.nak(Constants.doRecruit, "Illegal recruit");
            return;
        }

        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Constants.Phase.FIGHT)
        {
            if (game.getBattle() != null)
            {
                game.getBattle().doneReinforcing();
            }
            else
            {
                game.doneReinforcing();
            }
        }
    }

    void didRecruit(Legion legion, Creature recruit, Creature recruiter)
    {
        allUpdatePlayerInfo();

        int numRecruiters = (recruiter == null ? 0 :
                TerrainRecruitLoader.numberOfRecruiterNeeded(
                recruiter, recruit, legion.getCurrentHex().getTerrain(),
                legion.getCurrentHex().getLabel()));
        String recruiterName = null;
        if (recruiter != null)
        {
            recruiterName = recruiter.getName();
        }

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.didRecruit(legion.getMarkerId(), recruit.getName(),
                    recruiterName, numRecruiters);
        }

        List recruiterNames = new ArrayList();
        for (int i = 0; i < numRecruiters; i++)
        {
            recruiterNames.add(recruiterName);
        }
        game.revealEvent(true, null, legion.getMarkerId(), recruiterNames);
        game.addCreatureEvent(legion.getMarkerId(), recruit.getName());
    }

    void undidRecruit(Legion legion, String recruitName)
    {
        allUpdatePlayerInfo();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.undidRecruit(legion.getMarkerId(), recruitName);
        }
        game.removeCreatureEvent(legion.getMarkerId(), recruitName);
    }

    public synchronized void engage(String hexLabel)
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called engage()");
            return;
        }
        game.engage(hexLabel);
    }

    void allTellEngagement(String hexLabel, Legion attacker, Legion defender)
    {
        LOGGER.log(Level.FINEST, "allTellEngagement() " + hexLabel);
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellEngagement(hexLabel, attacker.getMarkerId(),
                    defender.getMarkerId());
        }
    }

    /** Ask ally's player whether he wants to concede with ally. */
    void askConcede(Legion ally, Legion enemy)
    {
        IClient client = getClient(ally.getPlayerName());
        client.askConcede(ally.getMarkerId(), enemy.getMarkerId());
    }

    public void concede(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called concede()");
            return;
        }
        game.concede(markerId);
    }

    public void doNotConcede(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called doNotConcede()");
            return;
        }
        game.doNotConcede(markerId);
    }

    /** Ask ally's player whether he wants to flee with ally. */
    void askFlee(Legion ally, Legion enemy)
    {
        IClient client = getClient(ally.getPlayerName());
        client.askFlee(ally.getMarkerId(), enemy.getMarkerId());
    }

    public void flee(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called flee()");
            return;
        }
        game.flee(markerId);
    }

    public void doNotFlee(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called doNotFlee()");
            return;
        }
        game.doNotFlee(markerId);
    }

    void twoNegotiate(Legion attacker, Legion defender)
    {
        IClient client1 = getClient(defender.getPlayerName());
        client1.askNegotiate(attacker.getMarkerId(), defender.getMarkerId());

        IClient client2 = getClient(attacker.getPlayerName());
        client2.askNegotiate(attacker.getMarkerId(), defender.getMarkerId());
    }

    /** playerName makes a proposal. */
    public void makeProposal(String proposalString)
    {
        // XXX Validate calling player
        game.makeProposal(getPlayerName(), proposalString);
    }

    /** Tell playerName about proposal. */
    void tellProposal(String playerName, Proposal proposal)
    {
        IClient client = getClient(playerName);
        client.tellProposal(proposal.toString());
    }

    public void fight(String hexLabel)
    {
        // XXX Validate calling player
        game.fight(hexLabel);
    }

    public void doBattleMove(int tag, String hexLabel)
    {
        IClient client = getClient(getPlayerName());
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doBattleMove()");
            client.nak(Constants.doBattleMove, "Wrong player");
            return;
        }
        boolean moved = game.getBattle().doMove(tag, hexLabel);
        if (!moved)
        {
            Log.error("Battle move failed");
            client.nak(Constants.doBattleMove, "Illegal move");
        }
    }

    void allTellBattleMove(int tag, String startingHex, String endingHex,
            boolean undo)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellBattleMove(tag, startingHex, endingHex, undo);
        }
    }

    public synchronized void strike(int tag, String hexLabel)
    {
        IClient client = getClient(getPlayerName());
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called strike()");
            client.nak(Constants.strike, "Wrong player");
            return;
        }
        Battle battle = game.getBattle();
        if (battle == null)
        {
            Log.error("null battle in Server.strike()");
            client.nak(Constants.strike, "No battle");
            return;
        }
        Legion legion = battle.getActiveLegion();
        if (legion == null)
        {
            Log.error("null active legion in Server.strike()");
            client.nak(Constants.strike, "No active legion");
            return;
        }
        Critter critter = legion.getCritterByTag(tag);
        if (critter == null)
        {
            Log.error("No critter with tag " + tag + " in Server.strike()");
            client.nak(Constants.strike, "No critter with that tag");
            return;
        }
        Critter target = battle.getCritter(hexLabel);
        if (target == null)
        {
            Log.error("No target in hex " + hexLabel + " in Server.strike()");
            client.nak(Constants.strike, "No target in that hex");
            return;
        }
        if (target.getPlayer() == critter.getPlayer())
        {
            Log.error(critter.getDescription() + " tried to strike allied " +
                target.getDescription());
            client.nak(Constants.strike, "Target is friendly");
            return;
        }
        if (critter.hasStruck())
        {
            Log.error(critter.getDescription() + " tried to strike twice");
            client.nak(Constants.strike, "Critter already struck");
            return;
        }
        critter.strike(target);
    }

    public synchronized void applyCarries(String hexLabel)
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called applyCarries()");
            return;
        }
        Battle battle = game.getBattle();
        Critter target = battle.getCritter(hexLabel);
        battle.applyCarries(target);
    }

    public void undoBattleMove(String hexLabel)
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called undoBattleMove()");
            return;
        }
        game.getBattle().undoMove(hexLabel);
    }

    synchronized void allTellStrikeResults(Critter striker, Critter target,
            int strikeNumber, List rolls, int damage, int carryDamageLeft,
            Set carryTargetDescriptions)
    {
        // Save strike info so that it can be reused for carries.
        this.striker = striker;
        this.target = target;
        this.strikeNumber = strikeNumber;
        this.rolls = rolls;

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellStrikeResults(striker.getTag(), target.getTag(),
                    strikeNumber, rolls, damage, target.isDead(), false,
                    carryDamageLeft, carryTargetDescriptions);
        }
    }

    synchronized void allTellCarryResults(Critter carryTarget,
            int carryDamageDone, int carryDamageLeft,
            Set carryTargetDescriptions)
    {
        if (striker == null || target == null || rolls == null)
        {
            Log.error("Called allTellCarryResults() without setup.");
            if (striker == null)
            {
                Log.error("null striker");
            }
            if (target == null)
            {
                Log.error("null target");
            }
            if (rolls == null)
            {
                Log.error("null rolls");
            }
            return;
        }
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellStrikeResults(striker.getTag(), carryTarget.getTag(),
                    strikeNumber, rolls, carryDamageDone, carryTarget.isDead(),
                    true, carryDamageLeft, carryTargetDescriptions);
        }
    }

    synchronized void allTellHexDamageResults(Critter target, int damage)
    {
        this.target = target;

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellStrikeResults(Constants.hexDamage, target.getTag(),
                    0, null, damage, target.isDead(), false, 0, null);
        }
    }

    /** Takes a Set of PenaltyOptions. */
    void askChooseStrikePenalty(SortedSet penaltyOptions)
    {
        String playerName = game.getBattle().getActivePlayerName();
        IClient client = getClient(playerName);
        ArrayList choices = new ArrayList();
        Iterator it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = (PenaltyOption)it.next();
            striker = po.getStriker();
            choices.add(po.toString());
        }
        client.askChooseStrikePenalty(choices);
    }

    public void assignStrikePenalty(String prompt)
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() +
                    " illegally called assignStrikePenalty()");
            getClient(getPlayerName()).nak(Constants.assignStrikePenalty,
                    "Wrong player");
        }
        else if (striker.hasStruck())
        {
            Log.error(getPlayerName() +
                    " assignStrikePenalty -- already struck");
            getClient(getPlayerName()).nak(Constants.assignStrikePenalty,
                    "Critter already struck");
        }
        else
        {
            striker.assignStrikePenalty(prompt);
        }
    }

    synchronized void allInitBattle(String masterHexLabel)
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.initBattle(masterHexLabel, battle.getTurnNumber(),
                    battle.getActivePlayerName(), battle.getBattlePhase(),
                    battle.getAttackerId(), battle.getDefenderId());
        }
    }

    void allCleanupBattle()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.cleanupBattle();
        }
    }

    public void mulligan()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called mulligan()");
            return;
        }
        int roll = game.mulligan();
        Log.event(getPlayerName() + " takes a mulligan and rolls " + roll);
    }

    public void undoSplit(String splitoffId)
    {
        if (!isActivePlayer())
        {
            return;
        }
        game.getActivePlayer().undoSplit(splitoffId);
    }

    void undidSplit(String splitoffId, String survivorId,
            boolean updateHistory, int turn)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.undidSplit(splitoffId, survivorId, turn);
        }
        if (updateHistory)
        {
            game.mergeEvent(splitoffId, survivorId);
        }
    }

    public void undoMove(String markerId)
    {
        if (!isActivePlayer())
        {
            return;
        }
        Legion legion = game.getLegionByMarkerId(markerId);
        String formerHexLabel = legion.getCurrentHexLabel();
        game.getActivePlayer().undoMove(markerId);
        String currentHexLabel = legion.getCurrentHexLabel();

        Player player = game.getPlayer(game.getActivePlayerName());
        // needed in undidMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = player.splitLegionHasForcedMove();

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.undidMove(markerId, formerHexLabel, currentHexLabel,
                    splitLegionHasForcedMove);
        }
    }

    public void undoRecruit(String markerId)
    {
        if (!isActivePlayer())
        {
            return;
        }
        game.getActivePlayer().undoRecruit(markerId);
    }

    public void doneWithSplits()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doneWithSplits()");
            getClient(getPlayerName()).nak(Constants.doneWithSplits,
                    "Wrong player");
        }
        else if (game.getTurnNumber() == 1 &&
                game.getActivePlayer().getNumLegions() == 1)
        {
            getClient(getPlayerName()).nak(Constants.doneWithSplits,
                    "Must split on first turn");
        }
        else
        {
            game.advancePhase(Constants.Phase.SPLIT, getPlayerName());
        }
    }

    public void doneWithMoves()
    {
        Player player = game.getActivePlayer();
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doneWithMoves()");
            getClient(getPlayerName()).nak(Constants.doneWithMoves,
                    "Wrong player");
        }
        // If any legion has a legal non-teleport move, then
        // the player must move at least one legion.
        else if (player.legionsMoved() == 0 &&
                player.countMobileLegions() > 0)
        {
            LOGGER.log(Level.FINEST, "At least one legion must move.");
            getClient(getPlayerName()).nak(Constants.doneWithMoves,
                    "Must move at least one legion");
        }
        // If legions share a hex and have a legal
        // non-teleport move, force one of them to take it.
        else if (player.splitLegionHasForcedMove())
        {
            LOGGER.log(Level.FINEST, "Split legions must be separated.");
            getClient(getPlayerName()).nak(Constants.doneWithMoves,
                    "Must separate split legions");
        }
        // Otherwise, recombine all split legions still in
        // the same hex, and move on to the next phase.
        else
        {
            player.recombineIllegalSplits();
            game.advancePhase(Constants.Phase.MOVE, getPlayerName());
        }
    }

    public void doneWithEngagements()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() +
                    " illegally called doneWithEngagements()");
            getClient(getPlayerName()).nak(Constants.doneWithEngagements,
                    "Wrong player");
        }
        // Advance only if there are no unresolved engagements.
        else if (game.findEngagements().size() > 0)
        {
            getClient(getPlayerName()).nak(Constants.doneWithEngagements,
                    "Must resolve engagements");
        }
        else
        {
            game.advancePhase(Constants.Phase.FIGHT, getPlayerName());
        }
    }

    public void doneWithRecruits()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() +
                    " illegally called doneWithRecruits()");
            getClient(getPlayerName()).nak(Constants.doneWithRecruits,
                    "Wrong player");
        }
        else
        {
            Player player = game.getActivePlayer();
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            player.setMulligansLeft(0);

            game.advancePhase(Constants.Phase.MUSTER, getPlayerName());
        }
    }

    // XXX Notify all players.
    public void withdrawFromGame()
    {
        Player player = getPlayer();
        // If player quits while engaged, set slayer.
        String slayerName = null;
        Legion legion = player.getTitanLegion();
        if (legion != null && game.isEngagement(legion.getCurrentHexLabel()))
        {
            slayerName = game.getFirstEnemyLegion(
                    legion.getCurrentHexLabel(), player).getPlayerName();
        }
        player.die(slayerName, true);
        if (player == game.getActivePlayer())
        {
            game.advancePhase(game.getPhase(), getPlayerName());
        }
    }

    public void setDonor(String markerId)
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called setDonor()");
            return;
        }
        Player player = game.getActivePlayer();
        Legion donor = game.getLegionByMarkerId(markerId);
        if (donor != null && donor.getPlayer() == player)
        {
            player.setDonor(donor);
        }
        else
        {
            Log.error("Bad arg to Server.getDonor() for " + markerId);
        }
    }

    private List getPlayerInfo(boolean treatDeadAsAlive)
    {
        List info = new ArrayList(game.getNumPlayers());
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            info.add(player.getStatusInfo(treatDeadAsAlive));
        }
        return info;
    }

    public void doSplit(String parentId, String childId, String results)
    {
        LOGGER.log(Level.FINEST, "Server.doSplit " + parentId + " " + childId + " " +
                results);
        IClient client = getClient(getPlayerName());
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doSplit()");
            client.nak(Constants.doSplit, "Wrong player");
            return;
        }
        if (!game.doSplit(parentId, childId, results))
        {
            Log.error("split failed for " + parentId);
            client.nak(Constants.doSplit, "Illegal split");
        }
    }

    /** Callback from game after this legion was split off. */
    void didSplit(String hexLabel, String parentId, String childId, int height)
    {
        LOGGER.log(Level.FINEST, "Server.didSplit " + hexLabel + " " + parentId + " " +
                childId + " " + height);
        allUpdatePlayerInfo();

        IClient activeClient = getClient(game.getActivePlayerName());

        Legion child = game.getLegionByMarkerId(childId);
        List splitoffs = child.getImageNames();
        activeClient.didSplit(hexLabel, parentId, childId, height, splitoffs,
                game.getTurnNumber());

        game.splitEvent(parentId, childId, splitoffs);

        if (!game.getOption(Options.allStacksVisible))
        {
            splitoffs.clear();
        }

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            if (client != activeClient)
            {
                client.didSplit(hexLabel, parentId, childId, height,
                        splitoffs, game.getTurnNumber());
            }
        }
    }

    /** Call from History during load game only */
    void didSplit(String parentId, String childId, List splitoffs, int turn)
    {
        IClient activeClient = getClient(game.getActivePlayerName());
        int childSize = splitoffs.size();
        activeClient.didSplit(null, parentId, childId, childSize, splitoffs,
                turn);

        if (!game.getOption(Options.allStacksVisible))
        {
            splitoffs.clear();
        }

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            if (client != activeClient)
            {
                client.didSplit(null, parentId, childId, childSize, splitoffs,
                        turn);
            }
        }
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
            boolean teleport, String teleportingLord)
    {
        IClient client = getClient(getPlayerName());
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doMove()");
            client.nak(Constants.doMove, "Wrong player");
            return;
        }
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion == null)
        {
            Log.error("Legion not found");
            client.nak(Constants.doMove, "No such legion");
            return;
        }

        String startingHexLabel = legion.getCurrentHexLabel();
        if (game.doMove(markerId, hexLabel, entrySide, teleport,
                teleportingLord))
        {
            allTellDidMove(markerId, startingHexLabel, hexLabel, entrySide,
                    teleport);
        }
        else
        {
            Log.error("Move failed");
            client.nak(Constants.doMove, "Illegal move");
        }
    }

    void allTellDidMove(String markerId, String startingHexLabel,
            String endingHexLabel, String entrySide, boolean teleport)
    {
        Player player = game.getPlayer(game.getActivePlayerName());
        // needed in didMove to decide whether to dis/enable button
        boolean splitLegionHasForcedMove = player.splitLegionHasForcedMove();

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.didMove(markerId, startingHexLabel, endingHexLabel,
                    entrySide, teleport, splitLegionHasForcedMove);
        }
    }

    void allTellAddCreature(String markerId, String creatureName,
            boolean updateHistory)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.addCreature(markerId, creatureName);
        }
        if (updateHistory)
        {
            game.addCreatureEvent(markerId, creatureName);
        }
    }

    void allTellRemoveCreature(String markerId, String creatureName,
            boolean updateHistory)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.removeCreature(markerId, creatureName);
        }
        if (updateHistory)
        {
            game.removeCreatureEvent(markerId, creatureName);
        }
    }

    void allRevealLegion(Legion legion)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.revealCreatures(legion.getMarkerId(),
                    legion.getImageNames());
        }
        game.revealEvent(true, null, legion.getMarkerId(),
                legion.getImageNames());
    }

    /** pass to all clients the 'revealEngagedCreatures' message,
     * then fire an 'revealEvent' to the history.
     * @author Towi, copied from allRevealLegion
     * @param legion the legion marker to reveal which is in a battle
     * @param isAttacker true if the 'legion' is the atackker in the
     *   battle, false for the defender.
     */
    void allRevealEngagedLegion(final Legion legion, final boolean isAttacker)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient) it.next();
            client.revealEngagedCreatures(
                legion.getMarkerId(), legion.getImageNames(), isAttacker);
        }
        game.revealEvent(true, null, legion.getMarkerId(),
                legion.getImageNames());
    }

    /** Call from History during load game only */
    void allRevealLegion(String markerId, List creatureNames)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.revealCreatures(markerId, creatureNames);
        }
    }

    void oneRevealLegion(Legion legion, String playerName)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.revealCreatures(legion.getMarkerId(),
                    legion.getImageNames());
        }
        List li = new ArrayList();
        li.add(playerName);
        game.revealEvent(false, li, legion.getMarkerId(),
                legion.getImageNames());
    }

    /** Call from History during load game only */
    void oneRevealLegion(String playerName, String markerId, List creatureNames)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.revealCreatures(markerId, creatureNames);
        }
        List li = new ArrayList();
        li.add(playerName);
    }

    void allFullyUpdateLegionStatus()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            if (client != null)
            {
                Iterator it2 = game.getAllLegions().iterator();
                while (it2.hasNext())
                {
                    Legion legion = (Legion)it2.next();
                    client.setLegionStatus(legion.getMarkerId(),
                            legion.hasMoved(), legion.hasTeleported(),
                            legion.getEntrySide(), legion.getRecruitName());
                }
            }
        }
    }

    void allFullyUpdateAllLegionContents()
    {
        Iterator it = game.getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            allRevealLegion(legion);
        }
    }

    void allRevealCreatures(Legion legion, List creatureNames)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.revealCreatures(legion.getMarkerId(), creatureNames);
        }
        game.revealEvent(true, null, legion.getMarkerId(), creatureNames);
    }

    /** Call from History during load game only */
    void oneRevealCreatures(String playerName, String markerId,
            List creatureNames)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.revealCreatures(markerId, creatureNames);
        }
    }

    /** Call from History during load game only */
    void allRevealCreatures(String markerId, List creatureNames)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.revealCreatures(markerId, creatureNames);
        }
    }

    // XXX Disallow these in network games?
    public void newGame()
    {
        Start.startupDialog(game, null);
    }

    public void loadGame(String filename)
    {
        game.loadGame(filename);
    }

    public void saveGame(String filename)
    {
        game.saveGame(filename);
    }

    /** Used to change a player name after color is assigned. */
    void setPlayerName(String playerName, String newName)
    {
        LOGGER.log(Level.FINEST, "Server.setPlayerName() from " + playerName + " to " +
                newName);
        IClient client = getClient(playerName);
        client.setPlayerName(newName);
        clientMap.remove(playerName);
        clientMap.put(newName, client);
    }

    synchronized void askPickColor(String playerName,
            final List colorsLeft)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.askPickColor(colorsLeft);
        }
    }

    public synchronized void assignColor(String color)
    {
        if (!getPlayerName().equals(game.getNextColorPicker()))
        {
            Log.error(getPlayerName() + " illegally called assignColor()");
            return;
        }
        if (getPlayer() == null || getPlayer().getColor() == null)
        {
            game.assignColor(getPlayerName(), color);
        }
    }

    void askPickFirstMarker(String playerName)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.askPickFirstMarker();
        }
    }

    public void assignFirstMarker(String markerId)
    {
        Player player = game.getPlayer(getPlayerName());
        if (!player.getMarkersAvailable().contains(markerId))
        {
            Log.error(getPlayerName() +
                    " illegally called assignFirstMarker()");
            return;
        }
        player.setFirstMarker(markerId);
        game.nextPickColor();
    }

    /** Hack to set color on load game. */
    synchronized void allSetColor()
    {
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            String name = player.getName();
            String color = player.getColor();
            IClient client = getClient(name);
            if (client != null)
            {
                client.setColor(color);
            }
        }
    }

    // XXX We use Server as a hook for PhaseAdvancer to get to options,
    // but this is ugly.
    int getIntOption(String optname)
    {
        return game.getIntOption(optname);
    }

    void oneSetOption(String playerName, String optname, String value)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    void oneSetOption(String playerName, String optname, boolean value)
    {
        oneSetOption(playerName, optname, String.valueOf(value));
    }

    void allSetOption(String optname, String value)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setOption(optname, value);
        }
    }

    void allSetOption(String optname, boolean value)
    {
        allSetOption(optname, String.valueOf(value));
    }

    void allSetOption(String optname, int value)
    {
        allSetOption(optname, String.valueOf(value));
    }

    /** DO NOT USE:
     * package so that it can be called from Log4J Appender.
     *
     */
    void allLog(String message)
    {
        Iterator it = remoteClients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.log(message);
        }
    }

    void logToStartLog(String message)
    {
        if ( startLog != null )
        {
            startLog.append(message);
        }
    }
    
    // for ServerSTartupProgress, if users wants to cancel during load
    // (e.g. one client did not come in)
    public void panicExit()
    {
        System.out.println("Panic exit...");
        disposeAllClients();
        System.exit(1);
    }
}
