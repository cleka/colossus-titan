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

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.IServer;
import net.sf.colossus.util.ChildThreadManager;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;


/**
 *  Thread to handle server connection on client side.
 *  @version $Id$
 *  @author David Ripton
 */

final class SocketClientThread extends Thread implements IServer
{
    private static final Logger LOGGER = Logger
        .getLogger(SocketClientThread.class.getName());

    private Client client;
    private ChildThreadManager threadMgr;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean goingDown = false;
    private boolean selfInterrupted = false;
    private boolean serverReceiveTimedout = false;

    private final static String sep = Constants.protocolTermSeparator;

    private String reasonFail = null;
    private String initialLine = null;

    SocketClientThread(Client client, String host, int port)
    {
        super("Client " + client.getPlayerName());

        this.client = client;
        this.threadMgr = client.getThreadMgr();
        net.sf.colossus.webcommon.FinalizeManager.register(this, "SCT "
            + client.getPlayerName());

        String task = "";

        try
        {
            task = "creating Socket to connect to " + host + ":" + port;
            LOGGER.log(Level.FINEST, "Next: " + task);
            socket = new Socket(host, port);

            task = "preparing PrintWriter";
            LOGGER.log(Level.FINEST, "Next: " + task);
            out = new PrintWriter(socket.getOutputStream(), true);

            task = "signing on";
            LOGGER.log(Level.FINEST, "Next: " + task);
            signOn();

            task = "preparing BufferedReader";
            LOGGER.log(Level.FINEST, "Next: " + task);
            in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

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

        // probably IOException
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

    public void tryInitialRead()
    {
        try
        {
            // Directly after connect we should get some first message
            // rather quickly... if not, probably Server has already enough
            // clients and we would hang in the queue...
            socket.setSoTimeout(5000);
            initialLine = in.readLine();
            // ... but after we got first data, during game it might take
            // unpredictable time before next thing comes, so reset it to 0
            //  ( = wait forever).
            socket.setSoTimeout(0);
            reasonFail = null;
        }
        catch (SocketTimeoutException ex)
        {
            reasonFail = "Server not responding (could connect, "
                + "but didn't got any initial data within 5 seconds. "
                + "Probably the game has already as many clients as "
                + "it expects).";
            return;

        }
        catch (Exception ex)
        {
            reasonFail = "Unanticipated exception during"
                + " reading first line from server: " + ex.toString();

            return;
        }

        threadMgr.registerToThreadManager(this);

        return;
    }

    public String getReasonFail()
    {
        return reasonFail;
    }

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

        tryInitialRead();
        if (reasonFail != null)
        {
            goingDown = true;
            String message = "Server not responding (could connect, "
                + "but didn't got any initial data within 5 seconds. "
                + "Probably the game has already as many clients as it expects).";
            String title = "Joining game failed!";
            client.showErrorMessage(message, title);
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
            client
                .showErrorMessage(
                    "No messages from server for very long time. "
                        + "Right now this should never happen because in normal game "
                        + "situation we work with infinite timeout... ??",
                    "No messages from server!");
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

        threadMgr.unregisterFromThreadManager(this);
        threadMgr = null;

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

    private Object isWaitingLock = new Object();
    private boolean isWaiting = false;

    private void setWaiting(boolean val)
    {
        synchronized (isWaitingLock)
        {
            isWaiting = val;
        }
    }

    /*    
     private void dummy()
     {
     //
     }
     */

    private String waitForLine()
    {
        String line = null;

        setWaiting(true);

        // try { Thread.sleep(100); } catch (InterruptedException ex) { dummy(); }

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
                    // @TODO: message to user?
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
        if (socket != null)
        {
            try
            {
                socket.close();
                socket = null;

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
        else
        {
            LOGGER.log(Level.FINEST, "SCT Closing socket not needed in "
                + getName());
        }
    }

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
        List li = Split.split(sep, s);
        if (!goingDown)
        {
            String method = (String)li.remove(0);
            callMethod(method, li);
        }
    }

    private void callMethod(String method, List args)
    {
        if (method.equals(Constants.tellMovementRoll))
        {
            int roll = Integer.parseInt((String)args.remove(0));
            client.tellMovementRoll(roll);
        }
        else if (method.equals(Constants.setOption))
        {
            String optname = (String)args.remove(0);
            String value = (String)args.remove(0);
            client.setOption(optname, value);
        }
        else if (method.equals(Constants.updatePlayerInfo))
        {
            List infoStrings = Split.split(Glob.sep, (String)args.remove(0));
            client.updatePlayerInfo(infoStrings);
        }
        else if (method.equals(Constants.setColor))
        {
            String color = (String)args.remove(0);
            client.setColor(color);
        }
        else if (method.equals(Constants.updateCreatureCount))
        {
            String creatureName = (String)args.remove(0);
            int count = Integer.parseInt((String)args.remove(0));
            int deadCount = Integer.parseInt((String)args.remove(0));
            client.updateCreatureCount(creatureName, count, deadCount);
        }
        else if (method.equals(Constants.dispose))
        {
            client.setClosedByServer();
            goingDown = true;
        }
        else if (method.equals(Constants.removeLegion))
        {
            String id = (String)args.remove(0);
            client.removeLegion(id);
        }
        else if (method.equals(Constants.setLegionStatus))
        {
            String markerId = (String)args.remove(0);
            boolean moved = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            boolean teleported = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            int entrySide = Integer.parseInt((String)args.remove(0));
            String lastRecruit = (String)args.remove(0);
            client.setLegionStatus(markerId, moved, teleported, entrySide,
                lastRecruit);
        }
        else if (method.equals(Constants.addCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            String reason = args.isEmpty() ? new String("<Unknown>")
                : (String)args.remove(0);
            client.addCreature(markerId, name, reason);
        }
        else if (method.equals(Constants.removeCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            String reason = args.isEmpty() ? new String("<Unknown>")
                : (String)args.remove(0);
            client.removeCreature(markerId, name, reason);
        }
        else if (method.equals(Constants.revealCreatures))
        {
            String markerId = (String)args.remove(0);
            String namesString = (String)args.remove(0);
            List names = Split.split(Glob.sep, namesString);

            // safeguard against getting empty string list from server
            // TODO: should split be fixed instead??
            if (namesString.equals("") && names.size() > 0
                && names.get(0).equals(""))
            {
                names.remove(0);
            }
            String reason = args.isEmpty() ? new String("<Unknown>")
                : (String)args.remove(0);
            client.revealCreatures(markerId, names, reason);
        }
        else if (method.equals(Constants.revealEngagedCreatures))
        {
            String markerId = (String)args.remove(0);
            boolean isAttacker = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            List names = Split.split(Glob.sep, (String)args.remove(0));
            String reason = args.isEmpty() ? new String("<Unknown>")
                : (String)args.remove(0);
            client.revealEngagedCreatures(markerId, names, isAttacker, reason);
        }
        else if (method.equals(Constants.removeDeadBattleChits))
        {
            client.removeDeadBattleChits();
        }
        else if (method.equals(Constants.placeNewChit))
        {
            String imageName = (String)args.remove(0);
            boolean inverted = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            int tag = Integer.parseInt((String)args.remove(0));
            String hexLabel = (String)args.remove(0);
            client.placeNewChit(imageName, inverted, tag, hexLabel);
        }
        else if (method.equals(Constants.initBoard))
        {
            client.initBoard();
        }
        else if (method.equals(Constants.setPlayerName))
        {
            String playerName = (String)args.remove(0);
            client.setPlayerName(playerName);
        }
        else if (method.equals(Constants.createSummonAngel))
        {
            String markerId = (String)args.remove(0);
            client.createSummonAngel(markerId);
        }
        else if (method.equals(Constants.askAcquireAngel))
        {
            String markerId = (String)args.remove(0);
            List recruits = Split.split(Glob.sep, (String)args.remove(0));
            client.askAcquireAngel(markerId, recruits);
        }
        else if (method.equals(Constants.askChooseStrikePenalty))
        {
            List choices = Split.split(Glob.sep, (String)args.remove(0));
            client.askChooseStrikePenalty(choices);
        }
        else if (method.equals(Constants.tellGameOver))
        {
            String message = (String)args.remove(0);
            client.tellGameOver(message);
        }
        else if (method.equals(Constants.tellPlayerElim))
        {
            String playerName = (String)args.remove(0);
            String slayerName = (String)args.remove(0);
            client.tellPlayerElim(playerName, slayerName);
        }
        else if (method.equals(Constants.askConcede))
        {
            String allyMarkerId = (String)args.remove(0);
            String enemyMarkerId = (String)args.remove(0);
            client.askConcede(allyMarkerId, enemyMarkerId);
        }
        else if (method.equals(Constants.askFlee))
        {
            String allyMarkerId = (String)args.remove(0);
            String enemyMarkerId = (String)args.remove(0);
            client.askFlee(allyMarkerId, enemyMarkerId);
        }
        else if (method.equals(Constants.askNegotiate))
        {
            String attackerId = (String)args.remove(0);
            String defenderId = (String)args.remove(0);
            client.askNegotiate(attackerId, defenderId);
        }
        else if (method.equals(Constants.tellProposal))
        {
            String proposalString = (String)args.remove(0);
            client.tellProposal(proposalString);
        }
        else if (method.equals(Constants.tellStrikeResults))
        {
            int strikerTag = Integer.parseInt((String)args.remove(0));
            int targetTag = Integer.parseInt((String)args.remove(0));
            int strikeNumber = Integer.parseInt((String)args.remove(0));
            List rolls = Split.split(Glob.sep, (String)args.remove(0));
            int damage = Integer.parseInt((String)args.remove(0));
            boolean killed = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            boolean wasCarry = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            int carryDamageLeft = Integer.parseInt((String)args.remove(0));

            Set carryTargetDescriptions = new HashSet();
            if (!args.isEmpty())
            {
                String buf = (String)args.remove(0);
                if (buf != null && buf.length() > 0)
                {
                    List ctdList = Split.split(Glob.sep, buf);
                    carryTargetDescriptions.addAll(ctdList);
                }
            }

            client.tellStrikeResults(strikerTag, targetTag, strikeNumber,
                rolls, damage, killed, wasCarry, carryDamageLeft,
                carryTargetDescriptions);
        }
        else if (method.equals(Constants.initBattle))
        {
            String masterHexLabel = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            String battleActivePlayerName = (String)args.remove(0);
            Constants.BattlePhase battlePhase = Constants.BattlePhase
                .fromInt(Integer.parseInt((String)args.remove(0)));
            String attackerMarkerId = (String)args.remove(0);
            String defenderMarkerId = (String)args.remove(0);
            client.initBattle(masterHexLabel, battleTurnNumber,
                battleActivePlayerName, battlePhase, attackerMarkerId,
                defenderMarkerId);
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
            String markerId = (String)args.remove(0);
            client.doReinforce(markerId);
        }
        else if (method.equals(Constants.didRecruit))
        {
            String markerId = (String)args.remove(0);
            String recruitName = (String)args.remove(0);
            String recruiterName = (String)args.remove(0);
            int numRecruiters = Integer.parseInt((String)args.remove(0));
            client.didRecruit(markerId, recruitName, recruiterName,
                numRecruiters);
        }
        else if (method.equals(Constants.undidRecruit))
        {
            String markerId = (String)args.remove(0);
            String recruitName = (String)args.remove(0);
            client.undidRecruit(markerId, recruitName);
        }
        else if (method.equals(Constants.setupTurnState))
        {
            String activePlayerName = (String)args.remove(0);
            int turnNumber = Integer.parseInt((String)args.remove(0));
            client.setupTurnState(activePlayerName, turnNumber);
        }
        else if (method.equals(Constants.setupSplit))
        {
            String activePlayerName = (String)args.remove(0);
            int turnNumber = Integer.parseInt((String)args.remove(0));
            client.setupSplit(activePlayerName, turnNumber);
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
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client.setupBattleSummon(battleActivePlayerName, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleRecruit))
        {
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client
                .setupBattleRecruit(battleActivePlayerName, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleMove))
        {
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client.setupBattleMove(battleActivePlayerName, battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleFight))
        {
            Constants.BattlePhase battlePhase = Constants.BattlePhase
                .fromInt(Integer.parseInt((String)args.remove(0)));
            String battleActivePlayerName = (String)args.remove(0);
            client.setupBattleFight(battlePhase, battleActivePlayerName);
        }
        else if (method.equals(Constants.tellLegionLocation))
        {
            String markerId = (String)args.remove(0);
            String hexLabel = (String)args.remove(0);
            client.tellLegionLocation(markerId, hexLabel);
        }
        else if (method.equals(Constants.tellBattleMove))
        {
            int tag = Integer.parseInt((String)args.remove(0));
            String startingHexLabel = (String)args.remove(0);
            String endingHexLabel = (String)args.remove(0);
            boolean undo = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            client.tellBattleMove(tag, startingHexLabel, endingHexLabel, undo);
        }
        else if (method.equals(Constants.didMove))
        {
            String markerId = (String)args.remove(0);
            String startingHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            String entrySide = (String)args.remove(0);
            boolean teleport = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            // servers from older versions might not send this arg
            String teleportingLord = null;
            if (!args.isEmpty())
            {
                teleportingLord = (String)args.remove(0);
                if (teleportingLord.equals("null"))
                {
                    teleportingLord = null;
                }
            }
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(
                    (String)args.remove(0)).booleanValue();
            }
            client
                .didMove(markerId, startingHexLabel, currentHexLabel,
                    entrySide, teleport, teleportingLord,
                    splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.undidMove))
        {
            String markerId = (String)args.remove(0);
            String formerHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if (!args.isEmpty())
            {
                splitLegionHasForcedMove = Boolean.valueOf(
                    (String)args.remove(0)).booleanValue();
            }
            client.undidMove(markerId, formerHexLabel, currentHexLabel,
                splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.didSummon))
        {
            String summonerId = (String)args.remove(0);
            String donorId = (String)args.remove(0);
            String summon = (String)args.remove(0);
            client.didSummon(summonerId, donorId, summon);
        }
        else if (method.equals(Constants.undidSplit))
        {
            String splitoffId = (String)args.remove(0);
            String survivorId = (String)args.remove(0);
            int turn = Integer.parseInt((String)args.remove(0));
            client.undidSplit(splitoffId, survivorId, turn);
        }
        else if (method.equals(Constants.didSplit))
        {
            String hexLabel = (String)args.remove(0);
            String parentId = (String)args.remove(0);
            String childId = (String)args.remove(0);
            int childHeight = Integer.parseInt((String)args.remove(0));
            List splitoffs = new ArrayList();
            if (!args.isEmpty())
            {
                List soList = Split.split(Glob.sep, (String)args.remove(0));
                splitoffs.addAll(soList);
            }
            int turn = Integer.parseInt((String)args.remove(0));
            client.didSplit(hexLabel, parentId, childId, childHeight,
                splitoffs, turn);
        }
        else if (method.equals(Constants.askPickColor))
        {
            List clList = Split.split(Glob.sep, (String)args.remove(0));
            List colorsLeft = new ArrayList();
            colorsLeft.addAll(clList);
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
                String message = (String)args.remove(0);
                client.log(message);
            }
        }
        else if (method.equals(Constants.nak))
        {
            client.nak((String)args.remove(0), (String)args.remove(0));
        }
        else if (method.equals(Constants.boardVisible))
        {
            boolean val = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            client.setBoardVisibility(val);
        }
        else if (method.equals(Constants.tellEngagement))
        {
            client.tellEngagement((String)args.remove(0), (String)args
                .remove(0), (String)args.remove(0));
        }
        else if (method.equals(Constants.tellEngagementResults))
        {
            String winnerId = (String)args.remove(0);
            String resMethod = (String)args.remove(0);
            int points = Integer.parseInt((String)args.remove(0));
            int turns = Integer.parseInt((String)args.remove(0));
            client.tellEngagementResults(winnerId, resMethod, points, turns);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Client, method: " + method
                + ", args: " + args + ")");
        }
    }

    private void sendToServer(String message)
    {
        if (socket != null)
        {
            out.println(message);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Attempt to send message '" + message
                + "' but the socket is closed??");
        }
    }

    // Setup method
    private void signOn()
    {
        sendToServer(Constants.signOn + sep + client.getPlayerName() + sep
            + client.isRemote());
    }

    /** Set the thread name to playerName, and tell the server so we
     *  can set this playerName in the right SocketServerThread. */
    void fixName(String playerName)
    {
        setName("Client " + playerName);
        sendToServer(Constants.fixName + sep + playerName);
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

    public void acquireAngel(String markerId, String angelType)
    {
        sendToServer(Constants.acquireAngel + sep + markerId + sep + angelType);
    }

    public void doSummon(String markerId, String donorId, String angel)
    {
        sendToServer(Constants.doSummon + sep + markerId + sep + donorId + sep
            + angel);
    }

    public void doRecruit(String markerId, String recruitName,
        String recruiterName)
    {
        sendToServer(Constants.doRecruit + sep + markerId + sep + recruitName
            + sep + recruiterName);
    }

    public void engage(String hexLabel)
    {
        sendToServer(Constants.engage + sep + hexLabel);
    }

    public void concede(String markerId)
    {
        sendToServer(Constants.concede + sep + markerId);
    }

    public void doNotConcede(String markerId)
    {
        sendToServer(Constants.doNotConcede + sep + markerId);
    }

    public void flee(String markerId)
    {
        sendToServer(Constants.flee + sep + markerId);
    }

    public void doNotFlee(String markerId)
    {
        sendToServer(Constants.doNotFlee + sep + markerId);
    }

    public void makeProposal(String proposalString)
    {
        sendToServer(Constants.makeProposal + sep + proposalString);
    }

    public void fight(String hexLabel)
    {
        sendToServer(Constants.fight + sep + hexLabel);
    }

    public void doBattleMove(int tag, String hexLabel)
    {
        sendToServer(Constants.doBattleMove + sep + tag + sep + hexLabel);
    }

    public synchronized void strike(int tag, String hexLabel)
    {
        sendToServer(Constants.strike + sep + tag + sep + hexLabel);
    }

    public synchronized void applyCarries(String hexLabel)
    {
        sendToServer(Constants.applyCarries + sep + hexLabel);
    }

    public void undoBattleMove(String hexLabel)
    {
        sendToServer(Constants.undoBattleMove + sep + hexLabel);
    }

    public void assignStrikePenalty(String prompt)
    {
        sendToServer(Constants.assignStrikePenalty + sep + prompt);
    }

    public void mulligan()
    {
        sendToServer(Constants.mulligan);
    }

    public void undoSplit(String splitoffId)
    {
        sendToServer(Constants.undoSplit + sep + splitoffId);
    }

    public void undoMove(String markerId)
    {
        sendToServer(Constants.undoMove + sep + markerId);
    }

    public void undoRecruit(String markerId)
    {
        sendToServer(Constants.undoRecruit + sep + markerId);
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

    public void setDonor(String markerId)
    {
        sendToServer(Constants.setDonor + sep + markerId);
    }

    public void doSplit(String parentId, String childId, String results)
    {
        sendToServer(Constants.doSplit + sep + parentId + sep + childId + sep
            + results);
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord)
    {
        sendToServer(Constants.doMove + sep + markerId + sep + hexLabel + sep
            + entrySide + sep + teleport + sep + teleportingLord);
    }

    public void assignColor(String color)
    {
        sendToServer(Constants.assignColor + sep + color);
    }

    public void assignFirstMarker(String markerId)
    {
        sendToServer(Constants.assignFirstMarker + sep + markerId);
    }

    // XXX Disallow the following methods in network games
    public void newGame()
    {
        sendToServer(Constants.newGame);
    }

    public void loadGame(String filename)
    {
        sendToServer(Constants.loadGame + sep + filename);
    }

    public void saveGame(String filename)
    {
        sendToServer(Constants.saveGame + sep + filename);
    }
}
