package net.sf.colossus.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client.ConnectionInitException;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.events.RecruitEvent;
import net.sf.colossus.game.events.SummonEvent;
import net.sf.colossus.server.IServer;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  Thread to handle server connection on client side.
 *
 *  @author David Ripton
 */

final class SocketClientThread extends Thread implements IServer,
    IServerConnection
{
    private static final Logger LOGGER = Logger
        .getLogger(SocketClientThread.class.getName());

    private Client client;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean goingDown = false;
    private boolean selfInterrupted = false;
    private boolean serverReceiveTimedout = false;

    private final static String sep = Constants.protocolTermSeparator;

    private String reasonFail = null;
    private String initialLine = null;

    private final Object isWaitingLock = new Object();
    private boolean isWaiting = false;

    public static SocketClientThread createConnection(String host, int port,
        String playerName, boolean remote) throws ConnectionInitException
    {
        SocketClientThread conn = new SocketClientThread(host, port,
            playerName, remote);

        String reasonFail = conn.getReasonFail();
        if (reasonFail != null)
        {
            // If this failed here, it is usually a "could not connect"-problem
            // (wrong host or port or server not yet up).
            // In this case we just do cleanup and end.

            LOGGER.warning("Client startup failed: " + reasonFail);
            if (!Options.isStresstest())
            {
                String title = "Socket initialialization failed!";
                ErrorUtils.showErrorDialog(null, title, reasonFail);
            }
            throw new ConnectionInitException(reasonFail);
        }

        return conn;
    }

    SocketClientThread(String host, int port, String initialName,
        boolean isRemote)
    {
        super("Client " + initialName);

        InstanceTracker.register(this, "SCT " + initialName);

        String task = "";

        try
        {
            task = "creating Socket to connect to " + host + ":" + port;
            LOGGER.log(Level.FINEST, "Next: " + task);
            socket = new Socket(host, port);

            int receiveBufferSize = socket.getReceiveBufferSize();
            LOGGER.info("Client socket receive buffer size for Client "
                + initialName + " is " + receiveBufferSize);

            task = "preparing PrintWriter";
            LOGGER.log(Level.FINEST, "Next: " + task);
            out = new PrintWriter(socket.getOutputStream(), true);

            task = "signing on";
            LOGGER.log(Level.FINEST, "Next: " + task);
            signOn(initialName, isRemote, IServer.CLIENT_VERSION);

            task = "preparing BufferedReader";
            LOGGER.log(Level.FINEST, "Next: " + task);
            in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

            task = "try initial read";
            LOGGER.log(Level.FINEST, "Next: " + task);
            tryInitialRead();

            reasonFail = null;
        }

        catch (UnknownHostException e)
        {
            LOGGER.log(Level.INFO, "UnknownHostException ('" + e.getMessage()
                + "') " + "in SCT during " + task);
            reasonFail = "UnknownHostException ('" + e.getMessage() + "') "
                + "during " + task + ".\n(This probably means:\n"
                + "You have given a server as name istead of IP address and "
                + "the name cannot be resolved to an address (typo?).";
            return;
        }

        // Could not connect - probably Firewall/NAT, or wrong IP or port
        catch (ConnectException e)
        {
            String msg = e.getMessage();
            String possReason = "";
            if (msg.startsWith("Connection timed out"))
            {
                possReason = ".\n(This probably means: "
                    + "Either you have given wrong Server name or "
                    + "address, or a network issue (firewall, proxy, NAT) is "
                    + "preventing the connection)";
            }
            else if (msg.startsWith("Connection refused"))
            {
                possReason = ".\n(This probably means: "
                    + "Either you have given wrong Server and/or port, "
                    + "or tried it too early and server side wasn't up yet)";
            }
            else
            {
                possReason = ".\n(No typical case is known causing this "
                    + "situation; check the exception details for any "
                    + "information what might be wrong)";
            }

            LOGGER.log(Level.INFO, "ConnectException (\"" + msg + "\") "
                + "in SCT during " + task);
            reasonFail = "ConnectException (\"" + e.getMessage() + "\") "
                + "during " + task + possReason;

            return;
        }

        // e.g. readLine in initialRead()
        catch (SocketTimeoutException ste)
        {
            String msg = ste.getMessage();
            LOGGER.log(Level.INFO, "SocketTimeoutException (\"" + msg + "\") "
                + "in SCT during " + task);
            reasonFail = "Server not responding (could connect, "
                + "but didn't got any initial data within 5 seconds. "
                + "Probably the game has already as many clients as "
                + "it expects).";
            return;
        }

        // e.g. setSoTimeout calls in tryInitialRead
        catch (SocketException e)
        {
            LOGGER.log(Level.SEVERE, "SocketException in SCT during " + task
                + ": ", e);
            reasonFail = "Exception during " + task + ": " + e.toString()
                + "\n(No typical case is known causing this situation; "
                + "check the exception details for any information what "
                + "might be wrong)";
            return;
        }

        // e.g. readLine() in tryInitialRead
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException in SCT during " + task
                + ": ", e);
            reasonFail = "Exception during " + task + ": " + e.toString()
                + "\n(No typical case is known causing this situation; "
                + "check the exception details for any information what "
                + "might be wrong)";
            return;
        }

        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Unusual Exception in SCT during " + task
                + ": ", e);
            reasonFail = "Exception during " + task + ": " + e.toString()
                + "\n(No typical case is known causing this situation; "
                + "check the exception details for any information what "
                + "might be wrong)";
            return;
        }
    }

    public void tryInitialRead() throws SocketTimeoutException,
        SocketException, IOException
    {
        // Directly after connect we should get some first message
        // rather quickly... if not, probably Server has already enough
        // clients and we would hang in the queue...
        socket.setSoTimeout(5000);
        initialLine = in.readLine();
        if (initialLine.startsWith("SignOn:"))
        {
            LOGGER.log(Level.INFO, "Got prompt - ok!");
            initialLine = null;
        }
        // ... but after we got first data, during game it might take
        // unpredictable time before next thing comes, so reset it to 0
        //  ( = wait forever).
        socket.setSoTimeout(0);

        return;
    }

    public String getReasonFail()
    {
        return reasonFail;
    }

    public IServer getIServer()
    {
        return this;
    }

    public void setClient(Client client)
    {
        this.client = client;
    }

    // Implements the method of the "generic" IServerConnection
    public void startThread()
    {
        this.start();
    }

    @Override
    public void run()
    {
        if (reasonFail != null)
        {
            // If SCT setup (constructor or tryInitRead() failed,
            // they set the reasonFail. SCT.start() is then only called
            // so that thread "was run" - otherwise GC would not collect it.
            // Then we end up here, do some cleanup, and that's it...
            cleanupSocket();
            client = null;
            goingDown = true;
            return;
        }

        if (reasonFail != null)
        {
            goingDown = true;
            String message = "Server not responding (could connect, "
                + "but didn't got any initial data within 5 seconds).";
            String title = "Joining game failed!";
            ErrorUtils.showErrorDialog(null, title, message);
        }

        // ---------------------------------------------------------------
        // This is the heart of the whole SocketClientThread.run():

        readAndParseUntilDone();

        // ---------------------------------------------------------------
        // After here: Cleaning up...

        if (serverReceiveTimedout)
        {
            // Right now this should never happen, but since we have
            // the catch and set the flag, let's do something with it:)
            String title = "No messages from server!";
            String message = "No messages from server for very long time. "
                + "Right now this should never happen because in normal game "
                + "situation we work with infinite timeout... ??";
            ErrorUtils.showErrorDialog(null, title, message);
        }

        cleanupSocket();

        if (client != null)
        {
            client.dispose();
            client = null;
        }
        else
        {
            LOGGER.log(Level.WARNING, "SCT run() " + getName()
                + ": after loop, client already null??");
        }

        LOGGER.log(Level.FINEST, "SCT run() ending " + getName());
    }

    private void readAndParseUntilDone()
    {
        // -----------------------------------------
        // Now the "read and parse until done" loop:
        String fromServer = null;
        try
        {
            // first !goingDown: server did send dispose, parseLine did set
            //    goingDown true; when body of loop completes it ends the loop.
            //    Or client side did set it to true while SCT was in parseLine.
            // second !goingDown: Client side did set goingDown to true, while
            //    SCT was waiting for line from socket, and interrupted it.
            //    So SCT returns from waitForLine and shall exit the loop.
            while (!goingDown && (fromServer = waitForLine()) != null
                && !goingDown)
            {
                if (fromServer.length() > 0)
                {
                    try
                    {
                        LOGGER.finest("Client '"
                            + client.getOwningPlayer().getName()
                            + "' got message from server: " + fromServer);
                        parseLine(fromServer);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.log(Level.WARNING,
                            "\n++++++\nSCT SocketClientThread " + getName()
                                + ", parseLine(): got Exception "
                                + ex.toString() + "\n" + ex.getMessage()
                                + "\nline=" + fromServer, ex);
                    }
                }
                else
                {
                    LOGGER.warning("Client '"
                        + client.getOwningPlayer().getName()
                        + "' got empty message from server?");
                }
            }

            LOGGER.log(Level.FINEST,
                "Clean end of SocketClientThread while loop");
        }

        // just in case...
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING,
                "\n^^^^^^^^^^\nSCT.run() major try/catch???\n", e);
            setWaiting(false);
        }
    }

    private void setWaiting(boolean val)
    {
        synchronized (isWaitingLock)
        {
            isWaiting = val;
        }
    }

    private String waitForLine()
    {
        String line = null;

        setWaiting(true);

        // First round, the unhandled line from tryInitialRead:
        if (initialLine != null)
        {
            line = initialLine;
            initialLine = null;
        }
        // if client did set it while we were doing parseLine or
        // waited in line above we can skip the next read.
        else if (!goingDown)
        {
            try
            {
                line = in.readLine();
            }
            catch (SocketTimeoutException ex)
            {
                serverReceiveTimedout = true;
                goingDown = true;
            }
            catch (SocketException ex)
            {
                if (selfInterrupted)
                {
                    // ok, interrupted to go down.
                }
                else
                {
                    client.setClosedByServer();
                    LOGGER
                        .log(Level.WARNING, "SCT SocketClientThread "
                            + getName() + ": got SocketException "
                            + ex.toString());
                }
                goingDown = true;
            }

            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "SCT SocketClientThread " + getName()
                    + ", got Exception ", ex);
                goingDown = true;
            }
        }
        setWaiting(false);
        return line;
    }

    public boolean isAlreadyDown()
    {
        return (client == null);
    }

    private void cleanupSocket()
    {
        try
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
                socket = null;
            }
            else
            {
                LOGGER.log(Level.FINEST, "SCT Closing socket not needed in "
                    + getName());
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.WARNING, "SocketClientThread " + getName()
                + ", during socket.close(), got IOException ", e);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "SocketClientThread " + getName()
                + ", during socket.close(), got Whatever Exception ", e);
        }
    }

    @Override
    public void interrupt()
    {
        super.interrupt();

        try
        {
            if (socket != null)
            {
                socket.close();
            }
        }
        catch (IOException e)
        {
            // quietly close
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "SCT.interrupt() in " + this.getName()
                + ": unexpected Exception.", e);
        }
    }

    /** Client originates the dispose: */
    public void stopSocketClientThread()
    {
        if (goingDown)
        {
            return;
        }
        goingDown = true;

        disconnect();

        synchronized (isWaitingLock)
        {
            // If socketReader is currently waiting on the socket,
            // we need to interrupt it.
            if (isWaiting)
            {
                selfInterrupted = true;
                this.interrupt();
            }
            // Otherwise, it will return to back of loop
            // anyway once it has done the parseLine execution.
            else
            {
                // no need to interrupt - nothing to do.
            }
        }

        // Now cleanup things go same way as if server would have send dispose.
    }

    private synchronized void parseLine(String s)
    {
        if (!goingDown)
        {
            List<String> li = Split.split(sep, s);

            String method = li.remove(0);
            callMethod(method, li);
        }
    }

    private void callMethod(String method, List<String> args)
    {
        LOGGER.finer("Client '" + client.getOwningPlayer().getName()
            + "' processing message: " + method);

        if (method.equals(Constants.tellMovementRoll))
        {
            int roll = Integer.parseInt(args.remove(0));
            client.tellMovementRoll(roll);
        }
        else if (method.equals(Constants.syncOption))
        {
            String optname = args.remove(0);
            String value = args.remove(0);
            client.syncOption(optname, value);
        }
        else if (method.equals(Constants.updatePlayerInfo))
        {
            List<String> infoStrings = Split.split(Glob.sep, args.remove(0));
            client.updatePlayerInfo(infoStrings);
        }
        else if (method.equals(Constants.setColor))
        {
            String colorName = args.remove(0);
            client.setColor(PlayerColor.getByName(colorName));
        }
        else if (method.equals(Constants.updateCreatureCount))
        {
            String creatureName = args.remove(0);
            int count = Integer.parseInt(args.remove(0));
            int deadCount = Integer.parseInt(args.remove(0));
            client.updateCreatureCount(resolveCreatureType(creatureName),
                count, deadCount);
        }
        else if (method.equals(Constants.dispose))
        {
            client.setClosedByServer();
            goingDown = true;
        }
        else if (method.equals(Constants.removeLegion))
        {
            String id = args.remove(0);
            client.removeLegion(resolveLegion(id));
        }
        else if (method.equals(Constants.setLegionStatus))
        {
            String markerId = args.remove(0);
            boolean moved = Boolean.valueOf(args.remove(0)).booleanValue();
            boolean teleported = Boolean.valueOf(args.remove(0))
                .booleanValue();
            int entrySideId = Integer.parseInt(args.remove(0));
            String lastRecruit = args.remove(0);
            client.setLegionStatus(resolveLegion(markerId), moved, teleported,
                EntrySide.fromIntegerId(entrySideId),
                resolveCreatureType(lastRecruit));
        }
        else if (method.equals(Constants.addCreature))
        {
            String markerId = args.remove(0);
            String name = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.addCreature(resolveLegion(markerId),
                resolveCreatureType(name), reason);
        }
        else if (method.equals(Constants.removeCreature))
        {
            String markerId = args.remove(0);
            String name = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.removeCreature(resolveLegion(markerId),
                resolveCreatureType(name), reason);
        }
        else if (method.equals(Constants.revealCreatures))
        {
            String markerId = args.remove(0);
            String namesString = args.remove(0);
            List<String> names = Split.split(Glob.sep, namesString);

            // safeguard against getting empty string list from server
            // TODO: should split be fixed instead??
            if (namesString.equals("") && names.size() > 0
                && names.get(0).equals(""))
            {
                names.remove(0);
            }
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            Player player = client.getGameClientSide().getPlayerByMarkerId(
                markerId);
            Legion legion;
            if (player.hasLegion(markerId))
            {
                legion = player.getLegionByMarkerId(markerId);
            }
            else
            {
                // this can happen on game startup since there is no explicit
                // event creating the first legions
                // TODO try to make this less implicit
                assert client.getTurnNumber() == -1 : "Implicit legion creation should happen only "
                    + "before the first round";
                legion = new LegionClientSide(player, markerId, player
                    .getStartingTower());
                player.addLegion(legion);
            }
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (String name : names)
            {
                creatures.add(resolveCreatureType(name));
            }
            client.revealCreatures(legion, creatures, reason);
        }
        else if (method.equals(Constants.revealEngagedCreatures))
        {
            String markerId = args.remove(0);
            boolean isAttacker = Boolean.valueOf(args.remove(0))
                .booleanValue();
            String names = args.remove(0);
            String reason = args.isEmpty() ? "<Unknown>" : args.remove(0);
            client.revealEngagedCreatures(resolveLegion(markerId),
                resolveCreatureTypes(names),
                isAttacker, reason);
        }
        else if (method.equals(Constants.removeDeadBattleChits))
        {
            client.removeDeadBattleChits();
        }
        else if (method.equals(Constants.placeNewChit))
        {
            String imageName = args.remove(0);
            boolean inverted = Boolean.valueOf(args.remove(0)).booleanValue();
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            BattleHex hex = HexMap.getHexByLabel(client.getGame()
                .getBattleSite().getTerrain(), hexLabel);
            client.placeNewChit(imageName, inverted, tag, hex);
        }
        else if (method.equals(Constants.replayOngoing))
        {
            boolean val = Boolean.valueOf(args.remove(0)).booleanValue();
            // older servers may not send this...
            String turnArgMaybe = args.isEmpty() ? "0" : args.remove(0);
            int maxTurn = Integer.parseInt(turnArgMaybe);
            client.tellReplay(val, maxTurn);
        }
        else if (method.equals(Constants.initBoard))
        {
            client.initBoard();
        }
        else if (method.equals(Constants.setPlayerName))
        {
            String playerName = args.remove(0);
            client.setPlayerName(playerName);
        }
        else if (method.equals(Constants.createSummonAngel))
        {
            String markerId = args.remove(0);
            client.createSummonAngel(resolveLegion(markerId));
        }
        else if (method.equals(Constants.askAcquireAngel))
        {
            String markerId = args.remove(0);
            List<CreatureType> recruits = resolveCreatureTypes(args.remove(0));
            client.askAcquireAngel(resolveLegion(markerId), recruits);
        }
        else if (method.equals(Constants.askChooseStrikePenalty))
        {
            List<String> choices = Split.split(Glob.sep, args.remove(0));
            client.askChooseStrikePenalty(choices);
        }
        else if (method.equals(Constants.tellGameOver))
        {
            String message = args.remove(0);
            boolean disposeFollows = false;
            if (!args.isEmpty())
            {
                disposeFollows = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            client.tellGameOver(message, disposeFollows);
        }
        else if (method.equals(Constants.tellPlayerElim))
        {
            String playerName = args.remove(0);
            String slayerName = args.remove(0);
            // TODO use the "noone" player instead of null if no slayer?
            client.tellPlayerElim(client.getPlayerByName(playerName),
                slayerName.equals("null") ? null : (client.getGameClientSide()
                    .getPlayerByName(slayerName)));
        }
        else if (method.equals(Constants.askConcede))
        {
            String allyMarkerId = args.remove(0);
            String enemyMarkerId = args.remove(0);
            client.askConcede(resolveLegion(allyMarkerId),
                resolveLegion(enemyMarkerId));
        }
        else if (method.equals(Constants.askFlee))
        {
            String allyMarkerId = args.remove(0);
            String enemyMarkerId = args.remove(0);
            client.askFlee(resolveLegion(allyMarkerId),
                resolveLegion(enemyMarkerId));
        }
        else if (method.equals(Constants.askNegotiate))
        {
            String attackerId = args.remove(0);
            String defenderId = args.remove(0);
            client.askNegotiate(resolveLegion(attackerId),
                resolveLegion(defenderId));
        }
        else if (method.equals(Constants.tellProposal))
        {
            String proposalString = args.remove(0);
            client.tellProposal(proposalString);
        }
        else if (method.equals(Constants.tellStrikeResults))
        {
            int strikerTag = Integer.parseInt(args.remove(0));
            int targetTag = Integer.parseInt(args.remove(0));
            int strikeNumber = Integer.parseInt(args.remove(0));
            List<String> rolls = Split.split(Glob.sep, args.remove(0));
            int damage = Integer.parseInt(args.remove(0));
            boolean killed = Boolean.valueOf(args.remove(0)).booleanValue();
            boolean wasCarry = Boolean.valueOf(args.remove(0)).booleanValue();
            int carryDamageLeft = Integer.parseInt(args.remove(0));

            Set<String> carryTargetDescriptions = new HashSet<String>();
            if (!args.isEmpty())
            {
                String buf = args.remove(0);
                if (buf != null && buf.length() > 0)
                {
                    List<String> ctdList = Split.split(Glob.sep, buf);
                    carryTargetDescriptions.addAll(ctdList);
                }
            }

            client.tellStrikeResults(strikerTag, targetTag, strikeNumber,
                rolls, damage, killed, wasCarry, carryDamageLeft,
                carryTargetDescriptions);
        }
        else if (method.equals(Constants.initBattle))
        {
            String masterHexLabel = args.remove(0);
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            String battleActivePlayerName = args.remove(0);
            BattlePhase battlePhase = BattlePhase.values()[Integer
                .parseInt(args.remove(0))];
            String attackerMarkerId = args.remove(0);
            String defenderMarkerId = args.remove(0);
            client.initBattle(resolveHex(masterHexLabel), battleTurnNumber,
                client.getPlayerByName(battleActivePlayerName), battlePhase,
                resolveLegion(attackerMarkerId),
                resolveLegion(defenderMarkerId));
        }
        else if (method.equals(Constants.cleanupBattle))
        {
            client.cleanupBattle();
        }
        else if (method.equals(Constants.nextEngagement))
        {
            client.nextEngagement();
        }
        else if (method.equals(Constants.doReinforce))
        {
            String markerId = args.remove(0);
            client.doReinforce(resolveLegion(markerId));
        }
        else if (method.equals(Constants.didRecruit))
        {
            String markerId = args.remove(0);
            String recruitName = args.remove(0);
            String recruiterName = args.remove(0);
            int numRecruiters = Integer.parseInt(args.remove(0));
            client.didRecruit(resolveLegion(markerId),
                resolveCreatureType(recruitName),
                resolveCreatureType(recruiterName), numRecruiters);
        }
        else if (method.equals(Constants.undidRecruit))
        {
            String markerId = args.remove(0);
            String recruitName = args.remove(0);
            client.undidRecruit(resolveLegion(markerId),
                resolveCreatureType(recruitName));
        }
        else if (method.equals(Constants.setupTurnState))
        {
            String activePlayerName = args.remove(0);
            int turnNumber = Integer.parseInt(args.remove(0));
            client.setupTurnState(client.getPlayerByName(activePlayerName),
                turnNumber);
        }
        else if (method.equals(Constants.setupSplit))
        {
            String activePlayerName = args.remove(0);
            int turnNumber = Integer.parseInt(args.remove(0));
            client.setupSplit(client.getPlayerByName(activePlayerName),
                turnNumber);
        }
        else if (method.equals(Constants.setupMove))
        {
            client.setupMove();
        }
        else if (method.equals(Constants.setupFight))
        {
            client.setupFight();
        }
        else if (method.equals(Constants.setupMuster))
        {
            client.setupMuster();
        }
        else if (method.equals(Constants.setupBattleSummon))
        {
            String battleActivePlayerName = args.remove(0);
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            client.setupBattleSummon(client
                .getPlayerByName(battleActivePlayerName), battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleRecruit))
        {
            String battleActivePlayerName = args.remove(0);
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            client.setupBattleRecruit(client
                .getPlayerByName(battleActivePlayerName), battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleMove))
        {
            String battleActivePlayerName = args.remove(0);
            int battleTurnNumber = Integer.parseInt(args.remove(0));
            client.setupBattleMove(client
                .getPlayerByName(battleActivePlayerName), battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleFight))
        {
            BattlePhase battlePhase = BattlePhase.values()[Integer
                .parseInt(args.remove(0))];
            String battleActivePlayerName = args.remove(0);
            client.setupBattleFight(battlePhase, client
                .getPlayerByName(battleActivePlayerName));
        }
        else if (method.equals(Constants.tellLegionLocation))
        {
            String markerId = args.remove(0);
            String hexLabel = args.remove(0);
            client.tellLegionLocation(resolveLegion(markerId),
                resolveHex(hexLabel));
        }
        else if (method.equals(Constants.tellBattleMove))
        {
            int tag = Integer.parseInt(args.remove(0));
            String startingHexLabel = args.remove(0);
            String endingHexLabel = args.remove(0);
            boolean undo = Boolean.valueOf(args.remove(0)).booleanValue();
            BattleHex startingHex = HexMap.getHexByLabel(client.getGame()
                .getBattleSite().getTerrain(), startingHexLabel);
            BattleHex endingHex = HexMap.getHexByLabel(client.getGame()
                .getBattleSite().getTerrain(), endingHexLabel);
            client.tellBattleMove(tag, startingHex, endingHex, undo);
        }
        else if (method.equals(Constants.didMove))
        {
            String markerId = args.remove(0);
            String startingHexLabel = args.remove(0);
            String currentHexLabel = args.remove(0);
            String entrySideLabel = args.remove(0);
            boolean teleport = Boolean.valueOf(args.remove(0)).booleanValue();
            // servers from older versions might not send this arg
            String teleportingLord = null;
            if (!args.isEmpty())
            {
                teleportingLord = args.remove(0);
                if (teleportingLord.equals("null"))
                {
                    teleportingLord = null;
                }
            }
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            client.didMove(resolveLegion(markerId),
                resolveHex(startingHexLabel), resolveHex(currentHexLabel),
                EntrySide.fromLabel(entrySideLabel), teleport,
                resolveCreatureType(teleportingLord),
                    splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.undidMove))
        {
            String markerId = args.remove(0);
            String formerHexLabel = args.remove(0);
            String currentHexLabel = args.remove(0);
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(args.remove(0))
                    .booleanValue();
            }
            client.undidMove(resolveLegion(markerId),
                resolveHex(formerHexLabel), resolveHex(currentHexLabel),
                splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.didSummon))
        {
            String summonerId = args.remove(0);
            String donorId = args.remove(0);
            String summon = args.remove(0);
            client.didSummon(resolveLegion(summonerId),
                resolveLegion(donorId), resolveCreatureType(summon));
        }
        else if (method.equals(Constants.undidSplit))
        {
            String splitoffId = args.remove(0);
            String survivorId = args.remove(0);
            int turn = Integer.parseInt(args.remove(0));
            client.undidSplit(resolveLegion(splitoffId),
                resolveLegion(survivorId), turn);
        }
        else if (method.equals(Constants.didSplit))
        {
            String hexLabel = args.remove(0);
            String parentId = args.remove(0);
            String childId = args.remove(0);
            int childHeight = Integer.parseInt(args.remove(0));
            List<CreatureType> splitoffs = resolveCreatureTypes(args.remove(0));
            int turn = Integer.parseInt(args.remove(0));
            // create client-side copy of new legion
            MasterHex hex = resolveHex(hexLabel);
            Legion parentLegion = resolveLegion(parentId);
            Player player = parentLegion.getPlayer();
            Legion newLegion = new LegionClientSide(player, childId, hex);
            player.addLegion(newLegion);
            client.didSplit(hex, parentLegion, newLegion, childHeight,
                splitoffs, turn);
        }
        else if (method.equals(Constants.askPickColor))
        {
            List<String> clList = Split.split(Glob.sep, args.remove(0));
            List<PlayerColor> colorsLeft = new ArrayList<PlayerColor>();
            for (String colorName : clList)
            {
                colorsLeft.add(PlayerColor.getByName(colorName));
            }
            client.askPickColor(colorsLeft);
        }
        else if (method.equals(Constants.askPickFirstMarker))
        {
            client.askPickFirstMarker();
        }
        else if (method.equals(Constants.log))
        {
            if (!args.isEmpty())
            {
                String message = args.remove(0);
                client.log(message);
            }
        }
        else if (method.equals(Constants.nak))
        {
            String reason = args.remove(0);
            String message = args.remove(0);
            if (reason.equals("SignOn"))
            {
                goingDown = true;
                String title = "Joining game failed!";
                ErrorUtils.showErrorDialog(null, title, message);
            }
            else
            {
                client.nak(reason, message);
            }
        }
        else if (method.equals(Constants.boardActive))
        {
            boolean val = Boolean.valueOf(args.remove(0)).booleanValue();
            client.setBoardActive(val);
        }
        else if (method.equals(Constants.tellEngagement))
        {
            client.tellEngagement(resolveHex(args.remove(0)), client
                .getLegion(args.remove(0)), client.getLegion(args.remove(0)));
        }
        else if (method.equals(Constants.tellEngagementResults))
        {
            String winnerId = args.remove(0);
            String resMethod = args.remove(0);
            int points = Integer.parseInt(args.remove(0));
            int turns = Integer.parseInt(args.remove(0));
            Legion legion;
            if (winnerId.equals("null"))
            {
                legion = null;
            }
            else
            {
                legion = resolveLegion(winnerId);
            }
            client.tellEngagementResults(legion, resMethod, points, turns);
        }

        else if (method.equals(Constants.tellWhatsHappening))
        {
            String message = args.remove(0);
            client.tellWhatsHappening(message);
        }

        else if (method.equals(Constants.askConfirmCatchUp))
        {
            client.confirmWhenCaughtUp();
        }

        else if (method.equals(Constants.serverConnectionOK))
        {
            LOGGER.info("Received server connection OK message from server "
                + "for player " + client.getOwningPlayer().getName());
            client.serverConfirmsConnection();
        }

        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Client, method: " + method
                + ", args: " + args + ")");
        }
        LOGGER.finer("Client '" + client.getOwningPlayer().getName()
            + "' finished method processing");
    }

    private List<CreatureType> resolveCreatureTypes(String nameList)
    {
        List<CreatureType> creatures = new ArrayList<CreatureType>();
        if (!nameList.equals(""))
        {
            List<String> names = Split.split(Glob.sep, nameList);
            for (String creatureName : names)
            {
                creatures.add(resolveCreatureType(creatureName));
            }
        }
        return creatures;
    }

    private CreatureType resolveCreatureType(String creatureName)
    {
        if ((creatureName == null) || (creatureName.equals("null")))
        {
            return null;
        }
        CreatureType creatureByName = client.getGame().getVariant()
            .getCreatureByName(creatureName);
        assert creatureByName != null : "Client got unknown creature name '"
            + creatureName + "' from server";
        return creatureByName;
    }

    private Legion resolveLegion(String markerId)
    {
        Legion legion = client.getLegion(markerId);

        // it's not just "unknown" - it might also be at any point during
        // the replay of a loaded game that there is no legion for that
        // marker *at that particular moment*.
        // Whereas delayed painting in EDT may cause similar error, they
        // should be carefully distincted.

        if (legion == null)
        {
            // If such a thing happens, there is something seriously wrong,
            // so I don't see a use in continuing the game.
            // This I call it severe, and log it always,
            // not just an assertion.
            LOGGER.severe("SCT ResolveLegion for " + markerId + " in client "
                + client.getOwningPlayer().getName() + " gave null!");
        }

        // Peter made this assertion, I guess...
        assert legion != null : "SocketClientThread.resolveLegion(" + markerId
            + " in client of player " + client.getOwningPlayer().getName()
            + " returned null!";

        return legion;
    }

    private void sendToServer(String message)
    {
        if (socket != null)
        {
            LOGGER.finer("Message to server from '"
                + client.getOwningPlayer().getName() + "':" + message);
            out.println(message);
            if (client.isSuspended())
            {
                LOGGER.info("Game in suspended state - "
                    + "sending message anyway.");
                client.showMessageDialog("NOTE: Game is suspended "
                    + "- server will not confirm/react to any of\n"
                    + "your GUI activities (move, split, ...), and thus "
                    + "they will not show effect on the Board yet!");
            }
        }
        else
        {
            LOGGER.log(Level.SEVERE, "SCT (" + getName() + ")"
                + ": Attempt to send message '" + message
                + "' but the socket is closed and/or client alresady null??");
        }
    }

    // Setup method
    private void signOn(String loginName, boolean isRemote, int version)
    {
        out.println(Constants.signOn + sep + loginName + sep + isRemote + sep
            + version);
    }

    /** Set the thread name to playerName */
    public void updateThreadName(String playerName)
    {
        setName("Client " + playerName);
    }

    // IServer methods, called from client and sent over the
    // socket to the server.

    public void leaveCarryMode()
    {
        sendToServer(Constants.leaveCarryMode);
    }

    public void doneWithBattleMoves()
    {
        sendToServer(Constants.doneWithBattleMoves);
    }

    public void doneWithStrikes()
    {
        sendToServer(Constants.doneWithStrikes);
    }

    public void acquireAngel(Legion legion, CreatureType angelType)
    {
        sendToServer(Constants.acquireAngel + sep + legion.getMarkerId() + sep
            + angelType);
    }

    public void doSummon(SummonEvent event)
    {
        if (event == null)
        {
            sendToServer(Constants.doSummon + sep + "null" + sep + "null"
                + sep + "null");
        }
        else
        {
        sendToServer(Constants.doSummon + sep + event.getLegion() + sep
            + event.getDonor() + sep + event.getAddedCreatureType());
        }
    }

    public void doRecruit(RecruitEvent event)
    {
        CreatureType recruiter = event.getRecruiter();
        CreatureType recruited = event.getRecruited();
        sendToServer(Constants.doRecruit + sep
            + event.getLegion().getMarkerId() + sep
            + ((recruited == null) ? null : recruited.getName()) + sep
            + ((recruiter == null) ? null : recruiter.getName()));
    }

    public void engage(MasterHex hex)
    {
        sendToServer(Constants.engage + sep + hex.getLabel());
    }

    public void concede(Legion legion)
    {
        sendToServer(Constants.concede + sep + legion);
    }

    public void doNotConcede(Legion legion)
    {
        sendToServer(Constants.doNotConcede + sep + legion.getMarkerId());
    }

    public void flee(Legion legion)
    {
        sendToServer(Constants.flee + sep + legion);
    }

    public void doNotFlee(Legion legion)
    {
        sendToServer(Constants.doNotFlee + sep + legion);
    }

    public void makeProposal(String proposalString)
    {
        sendToServer(Constants.makeProposal + sep + proposalString);
    }

    public void fight(MasterHex hex)
    {
        sendToServer(Constants.fight + sep + hex.getLabel());
    }

    public void doBattleMove(int tag, BattleHex hex)
    {
        sendToServer(Constants.doBattleMove + sep + tag + sep + hex.getLabel());
    }

    public synchronized void strike(int tag, BattleHex hex)
    {
        sendToServer(Constants.strike + sep + tag + sep + hex.getLabel());
    }

    public synchronized void applyCarries(BattleHex hex)
    {
        sendToServer(Constants.applyCarries + sep + hex.getLabel());
    }

    public void undoBattleMove(BattleHex hex)
    {
        sendToServer(Constants.undoBattleMove + sep + hex.getLabel());
    }

    public void assignStrikePenalty(String prompt)
    {
        sendToServer(Constants.assignStrikePenalty + sep + prompt);
    }

    public void mulligan()
    {
        sendToServer(Constants.mulligan);
    }

    public void undoSplit(Legion splitoff)
    {
        sendToServer(Constants.undoSplit + sep + splitoff.getMarkerId());
    }

    public void undoMove(Legion legion)
    {
        sendToServer(Constants.undoMove + sep + legion.getMarkerId());
    }

    public void undoRecruit(Legion legion)
    {
        sendToServer(Constants.undoRecruit + sep + legion.getMarkerId());
    }

    public void doneWithSplits()
    {
        sendToServer(Constants.doneWithSplits);
    }

    public void doneWithMoves()
    {
        sendToServer(Constants.doneWithMoves);
    }

    public void doneWithEngagements()
    {
        sendToServer(Constants.doneWithEngagements);
    }

    public void doneWithRecruits()
    {
        sendToServer(Constants.doneWithRecruits);
    }

    public void withdrawFromGame()
    {
        LOGGER.log(Level.FINEST, "SCT " + getName() + " sending withDraw");
        sendToServer(Constants.withdrawFromGame);
    }

    public void disconnect()
    {
        LOGGER.log(Level.FINEST, "SCT " + getName() + " sending disconnect");
        sendToServer(Constants.disconnect);
    }

    public void stopGame()
    {
        LOGGER.log(Level.FINEST, "SCT " + getName() + " sending stopGame");
        sendToServer(Constants.stopGame);
    }

    public void doSplit(Legion parent, String childMarker,
        List<CreatureType> creaturesToSplit)
    {
        sendToServer(Constants.doSplit + sep + parent.getMarkerId() + sep
            + childMarker + sep + Glob.glob(",", creaturesToSplit));
    }

    public void doMove(Legion legion, MasterHex hex, EntrySide entrySide,
        boolean teleport, CreatureType teleportingLord)
    {
        sendToServer(Constants.doMove + sep + legion.getMarkerId() + sep
            + hex.getLabel() + sep + entrySide.getLabel() + sep + teleport
            + sep + teleportingLord);
    }

    public void assignColor(PlayerColor color)
    {
        sendToServer(Constants.assignColor + sep + color.getName());
    }

    public void assignFirstMarker(String markerId)
    {
        sendToServer(Constants.assignFirstMarker + sep + markerId);
    }

    public void newGame()
    {
        sendToServer(Constants.newGame);
    }

    public void loadGame(String filename)
    {
        sendToServer(Constants.loadGame + sep + filename);
    }

    // TODO Can this be removed, because save game is done directly
    // instead of via socket message? Or do we keep it, for games
    // that are run on the "Web Server" ?
    public void saveGame(String filename)
    {
        sendToServer(Constants.saveGame + sep + filename);
    }

    public void checkServerConnection()
    {
        sendToServer(Constants.checkConnection);
    }

    public void clientConfirmedCatchup()
    {
        sendToServer(Constants.catchupConfirmation);
    }

    private MasterHex resolveHex(String label)
    {
        MasterHex hexByLabel = client.getGame().getVariant().getMasterBoard()
            .getHexByLabel(label);
        assert hexByLabel != null : "Client got unknown hex label '" + label
            + "' from server";
        return hexByLabel;
    }
}
