package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import javax.swing.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.client.IClient;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.Proposal;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.client.VariantSupport;

/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */
public final class Server implements IServer
{
    private Game game;

    /** 
     *  Maybe also save things like the originating IP, in case a 
     *  connection breaks and we need to authenticate reconnects.  
     *  Do not share these references. */
    private List clients = new ArrayList();
    private List remoteClients = new ArrayList();

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
    private int damage;
    private List rolls;

    // Network stuff
    private ServerSocket serverSocket;
    private Socket [] clientSockets = new Socket[VariantSupport.getMaxPlayers()];
    private int numClients;
    private int maxClients;


    Server(Game game, int port)
    {
        this.game = game;
        this.port = port;
        waitingForClients = game.getNumLivingPlayers();
    }

    void initSocketServer()
    {
        numClients = 0;
        maxClients = game.getNumLivingPlayers();
Log.debug("initSocketServer maxClients = " + maxClients);
Log.debug("About to create server socket on port " + port);
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
                serverSocket = null;
            }
            serverSocket = new ServerSocket(port, VariantSupport.getMaxPlayers());
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            ex.printStackTrace();
            return;
        }
        createLocalClients();

        while (numClients < maxClients)
        {
            waitForConnection();
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
            clientSockets[numClients] = clientSocket;
            numClients++;
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            ex.printStackTrace();
            return;
        }

        new SocketServerThread(this, clientSocket).start();
    }


    /** Each server thread's name is set to its player's name. */
    private String getPlayerName()
    {
        return Thread.currentThread().getName();
    }

    private Player getPlayer()
    {
        return game.getPlayer(getPlayerName());
    }

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
Log.debug("Called Server.createLocalClient() for " + playerName);
        IClient client = new Client(Constants.localhost, port, playerName, 
            false);
    }


    synchronized void addClient(final IClient client, final String playerName,
        final boolean remote)
    {
Log.debug("Called Server.addClient() for " + playerName);
        clients.add(client);

        if (remote)
        {
            addRemoteClient(client, playerName);
        }
        else
        {
            addLocalClient(client, playerName);
        }

        waitingForClients--;
        Log.event("Decremented waitingForClients to " + waitingForClients);
        if (waitingForClients <= 0)
        {
            if (game.isLoadingGame())
            {
                game.loadGame2();
            }
            else
            {
                game.newGame2();
            }
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

        Log.setServer(this);
        Log.setToRemote(true);
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


    void allUpdatePlayerInfo()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.updatePlayerInfo(getPlayerInfo());
        }
    }

    void allUpdateCreatureCount(String creatureName, int count)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.updateCreatureCount(creatureName, count);
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
            return;
        }
        Battle battle = game.getBattle();
        battle.doneWithMoves();
    }


    public void doneWithStrikes()
    {
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doneWithStrikes()");
            return;
        }
        Battle battle = game.getBattle();
        if (!battle.doneWithStrikes())
        {
            showMessageDialog("Must take forced strikes");
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
            Log.error("No client in clientMap for " + playerName);
            return null;
        }
    }


    /** Return the name of the first human-controlled client, or null if
     *  all clients are AI-controlled. */
    private String getFirstHumanClientName()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            if (player.isHuman())
            {
                return player.getName();
            }
        }
        return null;
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


    private void showMessageDialog(final String message)
    {
        showMessageDialog(getPlayerName(), message);
    }

    void showMessageDialog(String playerName, String message)
    {
        IClient client = getClient(playerName);
        client.showMessageDialog(message);
    }

    void allShowMessageDialog(String message)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.showMessageDialog(message);
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
    void allSetupTurnState()
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
                client.setupSplit(player.getMarkersAvailable(),
                    game.getActivePlayerName(), game.getTurnNumber());
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
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleSummon(game.getBattle().getActivePlayerName(),
                game.getBattle().getTurnNumber());
        }
    }

    void allSetupBattleRecruit()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleRecruit(
                game.getBattle().getActivePlayerName(),
                game.getBattle().getTurnNumber());
        }
    }

    void allSetupBattleMove()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleMove(
                game.getBattle().getActivePlayerName(),
                game.getBattle().getTurnNumber());
        }
    }

    void allSetupBattleFight()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setupBattleFight(game.getBattle().getPhase(),
                game.getBattle().getActivePlayerName());
        }
    }


    synchronized void allPlaceNewChit(Critter critter)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.placeNewChit(critter.getImageName(),
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


    void allHighlightEngagements()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.highlightEngagements();
        }
    }

    void nextEngagement()
    {
        IClient client = getClient(game.getActivePlayerName());
        client.nextEngagement();
    }


    /** Find out if the player wants to acquire an angel or archangel. */
    void askAcquireAngel(String playerName, String markerId, List recruits)
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

    public void acquireAngel(String markerId, String angelType)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called acquireAngel()");
            return;
        }

        if (legion != null)
        {
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


    /** Handle mustering for legion. */
    public void doRecruit(String markerId, String recruitName,
        String recruiterName)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (!getPlayerName().equals(legion.getPlayerName()))
        {
            Log.error(getPlayerName() + " illegally called doRecruit()");
            return;
        }
        if (legion != null && (legion.hasMoved() || game.getPhase() ==
            Constants.FIGHT) && legion.canRecruit())
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
        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Constants.FIGHT)
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

        int numRecruiters = TerrainRecruitLoader.numberOfRecruiterNeeded(
            recruiter, recruit, legion.getCurrentHex().getTerrain());
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
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called strike()");
            return;
        }
        boolean moved = game.getBattle().doMove(tag, hexLabel);
        if (moved)
        {
            Critter critter = game.getBattle().getCritter(tag);
            String startingHexLabel = critter.getStartingHexLabel();
            allTellBattleMove(tag, startingHexLabel, hexLabel, false);
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
        if (!isBattleActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called strike()");
            return;
        }
        Battle battle = game.getBattle();
        battle.getActiveLegion().getCritterByTag(tag).strike(
            battle.getCritter(hexLabel));
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
        this.damage = damage;
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
        int carryDamageDone, int carryDamageLeft, Set carryTargetDescriptions)
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

    
    synchronized void allTellDriftDamageResults(Critter target, int damage)
    {
        this.target = target;
        this.damage = damage;

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.tellStrikeResults(-1, target.getTag(), 0, null, damage, 
                target.isDead(), false, 0, null);
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
            return;
        }
        striker.assignStrikePenalty(prompt);
    }

    synchronized void allInitBattle(String masterHexLabel)
    {
        Battle battle = game.getBattle();
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.initBattle(masterHexLabel, battle.getTurnNumber(),
                battle.getActivePlayerName(), battle.getPhase(),
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
        if (roll != -1)
        {
            allTellMovementRoll(roll);
        }
    }


    public void undoSplit(String splitoffId)
    {
        if (!isActivePlayer())
        {
            return;
        }
        game.getActivePlayer().undoSplit(splitoffId);
    }

    void undidSplit(String splitoffId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.undidSplit(splitoffId);
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
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.undidMove(markerId, formerHexLabel, currentHexLabel);
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
            return;
        }
        if (game.getTurnNumber() == 1 &&
            game.getActivePlayer().getNumLegions() == 1)
        {
            showMessageDialog("Must split initial legion");
            return;
        }
        game.advancePhase(Constants.SPLIT, getPlayerName());
    }

    public void doneWithMoves()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doneWithMoves()");
            return;
        }

        Player player = game.getActivePlayer();

        // If any legion has a legal non-teleport move, then
        // the player must move at least one legion.
        if (player.legionsMoved() == 0 &&
            player.countMobileLegions() > 0)
        {
Log.debug("At least one legion must move.");
            showMessageDialog("At least one legion must move.");
            return;
        }
        // If legions share a hex and have a legal
        // non-teleport move, force one of them to take it.
        else if (player.splitLegionHasForcedMove())
        {
Log.debug("Split legions must be separated.");
            showMessageDialog("Split legions must be separated.");
            return;
        }
        // Otherwise, recombine all split legions still in
        // the same hex, and move on to the next phase.
        else
        {
            player.recombineIllegalSplits();
            game.advancePhase(Constants.MOVE, getPlayerName());
        }
    }

    public void doneWithEngagements()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + 
                " illegally called doneWithEngagements()");
            return;
        }
        // Advance only if there are no unresolved engagements.
        if (game.findEngagements().size() > 0)
        {
            showMessageDialog("Must resolve engagements");
            return;
        }
        game.advancePhase(Constants.FIGHT, getPlayerName());
    }

    public void doneWithRecruits()
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + 
                " illegally called doneWithRecruits()");
            return;
        }
        Player player = game.getActivePlayer();
        player.commitMoves();

        // Mulligans are only allowed on turn 1.
        player.setMulligansLeft(0);

        game.advancePhase(Constants.MUSTER, getPlayerName());
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
        game.advancePhase(game.getPhase(), getPlayerName());
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


    private List getPlayerInfo()
    {
        List info = new ArrayList(game.getNumPlayers());
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            info.add(player.getStatusInfo());
        }
        return info;
    }


    public void doSplit(String parentId, String childId, String results)
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doSplit()");
            return;
        }
        game.doSplit(parentId, childId, results);
    }

    /** Callback from game after this legion was split off. */
    void didSplit(String hexLabel, String parentId, String childId, int height)
    {
        allUpdatePlayerInfo();

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.didSplit(hexLabel, parentId, childId, height);
        }
    }


    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord)
    {
        if (!isActivePlayer())
        {
            Log.error(getPlayerName() + " illegally called doMove()");
            return;
        }
        Legion legion = game.getLegionByMarkerId(markerId);
        String startingHexLabel = legion.getCurrentHexLabel();

        if (game.doMove(markerId, hexLabel, entrySide, teleport,
            teleportingLord))
        {
            allTellDidMove(markerId, startingHexLabel, hexLabel, entrySide,
                teleport);
        }
    }

    void allTellDidMove(String markerId, String startingHexLabel,
        String endingHexLabel, String entrySide, boolean teleport)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.didMove(markerId, startingHexLabel, endingHexLabel,
                entrySide, teleport);
        }
    }


    void allTellAddCreature(String markerId, String creatureName)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.addCreature(markerId, creatureName);
        }
    }

    void allTellRemoveCreature(String markerId, String creatureName)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.removeCreature(markerId, creatureName);
        }
    }

    void allRevealLegion(Legion legion)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.setLegionContents(legion.getMarkerId(),
                legion.getImageNames());
        }
    }

    void oneRevealLegion(Legion legion, String playerName)
    {
        IClient client = getClient(playerName);
        if (client != null)
        {
            client.setLegionContents(legion.getMarkerId(), 
                legion.getImageNames());
        }
    }

    void allFullyUpdateLegionHeights()
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
                    client.setLegionHeight(legion.getMarkerId(),
                        legion.getHeight());
                }
            }
        }
    }

    void allFullyUpdateOwnLegionContents()
    {
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            Iterator it2 = player.getLegions().iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                oneRevealLegion(legion, player.getName());
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

    // XXX temp, until AIs can predict splits.
    void aiFullyUpdateAllLegionContents()
    {
        Iterator it = game.getAllLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            aiRevealLegion(legion);
        }
    }

    // XXX temp, until AIs can predict splits.
    void aiRevealLegion(Legion legion)
    {
        Iterator it = game.getPlayers().iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            if (player.isAI())
            {
                oneRevealLegion(legion, player.getName());
            }
        }
    }

    void allRevealCreature(Legion legion, String creatureName)
    {
        List names = new ArrayList();
        names.add(creatureName);
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.revealCreatures(legion.getMarkerId(), names);
        }
    }


    // XXX Disallow in network games
    public void newGame()
    {
        game.newGame();
    }

    // XXX Disallow in network games
    public void loadGame(String filename)
    {
        game.loadGame(filename);
    }

    // XXX Disallow in network games
    public void saveGame(String filename)
    {
        game.saveGame(filename);
    }

    /** Used to change a player name after color is assigned. */
    void setPlayerName(String playerName, String newName)
    {
Log.debug("Server.setPlayerName() from " + playerName + " to " + newName);
        IClient client = getClient(playerName);
        client.setPlayerName(newName);
        clientMap.remove(playerName);
        clientMap.put(newName, client);
    }

    synchronized void askPickColor(String playerName, final Set colorsLeft)
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

    /** Hack to set color on load game. */
    void allSetColor()
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

    /** public so that it can be called from Log. */
    public void allLog(String message)
    {
        Iterator it = remoteClients.iterator();
        while (it.hasNext())
        {
            IClient client = (IClient)it.next();
            client.log(message);
        }
    }

    public void relayChatMessage(String target, String text)
    {
        String from = getPlayerName();
        if (target.equals(Constants.all))
        {
            Iterator it = clients.iterator();
            while (it.hasNext())
            {
                IClient client = (IClient)it.next();
                client.showChatMessage(from, text);
            }
        }
        else
        {
            IClient client = getClient(target);
            if (client != null)
            {
                client.showChatMessage(from, text);
            }
            client = getClient(from);
            if (client != null)
            {
                client.showChatMessage(from, text);
            }
        }
    }
}
