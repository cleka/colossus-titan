package net.sf.colossus.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;
import net.sf.colossus.webcommon.FinalizeManager;


/**
 *  Thread to handle one client connection.
 *  @version $Id$
 *  @author David Ripton
 */

final class SocketServerThread extends Thread implements IClient
{
    private static final Logger LOGGER = Logger
        .getLogger(SocketServerThread.class.getName());

    private Server server;
    private Socket socket;
    private InputStream is;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;

    private boolean done = false;
    private boolean isGone = false;
    private boolean toldToTerminate = false;
    private boolean withdrawnAlready = false;
    private boolean selfInterrupted = false;

    private static final String sep = Constants.protocolTermSeparator;

    private static int counter = 0;

    private Object isWaitingLock = new Object();
    private boolean isWaiting = false;

    SocketServerThread(Server server, Socket socket)
    {
        super("SocketServerThread");
        this.server = server;
        this.socket = socket;
        String tempId = "<no name yet #" + (counter++) + ">";
        FinalizeManager.register(this, tempId);
        // We must register already in constructor. Otherwise, clients
        // might have connected, but none yet registered to threadmgr.
        // So Server continues with "waitUntilAllGone", there are no
        // threads [yet, but thinks "not any more"],
        // and does cleanup, set game to null, etc...
        server.getThreadMgr().registerToThreadManager(this);
    }

    public void run()
    {
        try
        {
            // especially the unregister is after a catch-all
            // block, to make sure we unregister in any case.
            try
            {
                is = socket.getInputStream();
                in = new BufferedReader(new InputStreamReader(is));
                out = new PrintWriter(socket.getOutputStream(), true);
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                return;
            }

            try
            {
                String fromClient;
                setWaiting(true);
                while (!done && (fromClient = in.readLine()) != null && !done)
                {
                    setWaiting(false);
                    LOGGER.log(Level.FINEST, "From client " + playerName
                        + ": " + fromClient);
                    parseLine(fromClient);
                    setWaiting(true);
                }
                setWaiting(false);
                LOGGER.log(Level.FINEST,
                    "End of SocketServerThread while loop");
            }
            catch (InterruptedIOException ex)
            {
                LOGGER.log(Level.SEVERE,
                    "InterruptedIOException while read loop; ", ex);
                Thread.currentThread().interrupt();
            }
            catch (SocketException ex)
            {
                if (selfInterrupted)
                {
                    // all right. Server (some other SST) interrupted us 
                    // in order to make us stop waiting on the socket...
                }
                else if (done)
                {
                    // all right. Client told it will disconnect.
                }
                else
                {
                    // ooops. Exception, perhaps client closed his side?
                    LOGGER.log(Level.FINE, "SocketException in " + getName()
                        + " while "
                        + "read loop (client terminated unexpectedly?)");
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "IOException while read loop; ", ex);
            }

            isGone = true;

            try
            {
                server.unregisterSocket(socket);
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "Exception while unregisterSocket; ",
                    e);
            }

            if (selfInterrupted)
            {
                // no need to withdraw any more
            }
            else if (toldToTerminate)
            {
                // told to terminate - ok
            }
            else if (!withdrawnAlready)
            {
                withdrawnAlready = true;
                server.withdrawFromGame();
            }

            // Still tell client to shutdown... if necessary.
            dispose();

            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "IOException while socket.close; ",
                    ex);
            }

            socket = null;
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Exception in major try/catch; ", e);
        }
        // just to be sure...
        setWaiting(false);
        isGone = true;

        server.getThreadMgr().unregisterFromThreadManager(this);

        this.server = null;
        this.socket = null;
    }

    private void setWaiting(boolean val)
    {
        synchronized (isWaitingLock)
        {
            isWaiting = val;
        }
    }

    public synchronized void tellToTerminate()
    {
        toldToTerminate = true;
        done = true;
        try
        {
            out.println(Constants.dispose);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        if (Thread.currentThread() == this)
        {
            // no need to interrupt ourselves; we'll get to the top of loop
            // after parseLine completed anyway.
        }
        else
        {
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
    }

    // if true, connection to this client is gone
    // Server uses this to decide whether any nonAI player is 
    // (even if perhaps dead) still connected (= watching).
    public boolean isGone()
    {
        return this.isGone;
    }

    private synchronized void parseLine(String s)
    {
        List li = Split.split(sep, s);
        String method = (String)li.remove(0);
        if (playerName == null && !method.equals(Constants.signOn))
        {
            LOGGER.log(Level.SEVERE, "First packet must be signOn");
        }
        else
        {
            callMethod(method, li);
        }
    }

    private void callMethod(String method, List args)
    {
        if (method.equals(Constants.signOn))
        {
            String playerName = (String)args.remove(0);
            boolean remote = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            setPlayerName(playerName);
            server.addClient(this, playerName, remote);
            FinalizeManager.setId(this, playerName);
        }
        else if (method.equals(Constants.fixName))
        {
            String newName = (String)args.remove(0);
            // Prevent an infinite loop oscillating between two names.
            if (!newName.equals(playerName)
                && !newName.startsWith(Constants.byColor))
            {
                setPlayerName(newName);
            }
        }
        else if (method.equals(Constants.leaveCarryMode))
        {
            server.leaveCarryMode();
        }
        else if (method.equals(Constants.doneWithBattleMoves))
        {
            server.doneWithBattleMoves();
        }
        else if (method.equals(Constants.doneWithStrikes))
        {
            server.doneWithStrikes();
        }
        else if (method.equals(Constants.acquireAngel))
        {
            String markerId = (String)args.remove(0);
            String angelType = (String)args.remove(0);
            server.acquireAngel(markerId, angelType);
        }
        else if (method.equals(Constants.doSummon))
        {
            String markerId = (String)args.remove(0);
            String donorId = (String)args.remove(0);
            String angel = (String)args.remove(0);
            server.doSummon(markerId, donorId, angel);
        }
        else if (method.equals(Constants.doRecruit))
        {
            String markerId = (String)args.remove(0);
            String recruitName = (String)args.remove(0);
            String recruiterName = (String)args.remove(0);
            server.doRecruit(markerId, recruitName, recruiterName);
        }
        else if (method.equals(Constants.engage))
        {
            String hexLabel = (String)args.remove(0);
            server.engage(hexLabel);
        }
        else if (method.equals(Constants.concede))
        {
            String markerId = (String)args.remove(0);
            server.concede(markerId);
        }
        else if (method.equals(Constants.doNotConcede))
        {
            String markerId = (String)args.remove(0);
            server.doNotConcede(markerId);
        }
        else if (method.equals(Constants.flee))
        {
            String markerId = (String)args.remove(0);
            server.flee(markerId);
        }
        else if (method.equals(Constants.doNotFlee))
        {
            String markerId = (String)args.remove(0);
            server.doNotFlee(markerId);
        }
        else if (method.equals(Constants.makeProposal))
        {
            String proposalString = (String)args.remove(0);
            server.makeProposal(proposalString);
        }
        else if (method.equals(Constants.fight))
        {
            String hexLabel = (String)args.remove(0);
            server.fight(hexLabel);
        }
        else if (method.equals(Constants.doBattleMove))
        {
            int tag = Integer.parseInt((String)args.remove(0));
            String hexLabel = (String)args.remove(0);
            server.doBattleMove(tag, hexLabel);
        }
        else if (method.equals(Constants.strike))
        {
            int tag = Integer.parseInt((String)args.remove(0));
            String hexLabel = (String)args.remove(0);
            server.strike(tag, hexLabel);
        }
        else if (method.equals(Constants.applyCarries))
        {
            String hexLabel = (String)args.remove(0);
            server.applyCarries(hexLabel);
        }
        else if (method.equals(Constants.undoBattleMove))
        {
            String hexLabel = (String)args.remove(0);
            server.undoBattleMove(hexLabel);
        }
        else if (method.equals(Constants.assignStrikePenalty))
        {
            String prompt = (String)args.remove(0);
            server.assignStrikePenalty(prompt);
        }
        else if (method.equals(Constants.mulligan))
        {
            server.mulligan();
        }
        else if (method.equals(Constants.undoSplit))
        {
            String splitoffId = (String)args.remove(0);
            server.undoSplit(splitoffId);
        }
        else if (method.equals(Constants.undoMove))
        {
            String markerId = (String)args.remove(0);
            server.undoMove(markerId);
        }
        else if (method.equals(Constants.undoRecruit))
        {
            String markerId = (String)args.remove(0);
            server.undoRecruit(markerId);
        }
        else if (method.equals(Constants.doneWithSplits))
        {
            server.doneWithSplits();
        }
        else if (method.equals(Constants.doneWithMoves))
        {
            server.doneWithMoves();
        }
        else if (method.equals(Constants.doneWithEngagements))
        {
            server.doneWithEngagements();
        }
        else if (method.equals(Constants.doneWithRecruits))
        {
            server.doneWithRecruits();
        }
        else if (method.equals(Constants.withdrawFromGame))
        {
            withdrawnAlready = true;
            server.withdrawFromGame();
        }
        else if (method.equals(Constants.disconnect))
        {
            if (withdrawnAlready)
            {
                LOGGER.log(Level.FINE,
                    "Client disconnected without explicit withdraw - "
                        + "doing automatic withdraw for player " + playerName);
                server.withdrawFromGame();
                withdrawnAlready = true;
            }
            done = true;
            server.disconnect();
        }
        else if (method.equals(Constants.stopGame))
        {
            // client will dispose itself soon, 
            // do not attempt to further read from there.
            done = true;
            server.stopGame();
        }
        else if (method.equals(Constants.setDonor))
        {
            String markerId = (String)args.remove(0);
            server.setDonor(markerId);
        }
        else if (method.equals(Constants.doSplit))
        {
            String parentId = (String)args.remove(0);
            String childId = (String)args.remove(0);
            String results = (String)args.remove(0);
            server.doSplit(parentId, childId, results);
        }
        else if (method.equals(Constants.doMove))
        {
            String markerId = (String)args.remove(0);
            String hexLabel = (String)args.remove(0);
            String entrySide = (String)args.remove(0);
            boolean teleport = Boolean.valueOf((String)args.remove(0))
                .booleanValue();
            String teleportingLord = (String)args.remove(0);
            server.doMove(markerId, hexLabel, entrySide, teleport,
                teleportingLord);
        }
        else if (method.equals(Constants.assignColor))
        {
            String color = (String)args.remove(0);
            server.assignColor(color);
        }
        else if (method.equals(Constants.assignFirstMarker))
        {
            String markerId = (String)args.remove(0);
            server.assignFirstMarker(markerId);
        }
        else if (method.equals(Constants.newGame))
        {
            server.newGame();
        }
        else if (method.equals(Constants.loadGame))
        {
            String filename = (String)args.remove(0);
            server.loadGame(filename);
        }
        else if (method.equals(Constants.saveGame))
        {
            String filename = (String)args.remove(0);
            server.saveGame(filename);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Server, method: " + method
                + ", args: " + args + ")");
        }
    }

    // Wrapper for all the send-over-socket methods:
    public void sendToClient(String message)
    {
        if (done || isGone || out == null)
        {
            // do not send any more
            /*
             LOGGER.log(Level.WARNING, 
             "Attempt to send to player " + playerName + 
             " when client connection already gone - message: " + message);
             */
        }
        else
        {
            out.println(message);
        }
    }

    // IClient methods to sent requests to client over socket.

    public void tellEngagement(String hexLabel, String attackerId,
        String defenderId)
    {
        sendToClient(Constants.tellEngagement + sep + hexLabel + sep
            + attackerId + sep + defenderId);
    }

    public void tellEngagementResults(String winnerId, String method,
        int points, int turns)
    {
        sendToClient(Constants.tellEngagementResults + sep + winnerId + sep
            + method + sep + points + sep + turns);
    }

    public void tellMovementRoll(int roll)
    {
        sendToClient(Constants.tellMovementRoll + sep + roll);
    }

    public void setOption(String optname, String value)
    {
        sendToClient(Constants.setOption + sep + optname + sep + value);
    }

    public void updatePlayerInfo(List infoStrings)
    {
        sendToClient(Constants.updatePlayerInfo + sep + Glob.glob(infoStrings));
    }

    public void setColor(String color)
    {
        sendToClient(Constants.setColor + sep + color);
    }

    public void updateCreatureCount(String creatureName, int count,
        int deadCount)
    {
        sendToClient(Constants.updateCreatureCount + sep + creatureName + sep
            + count + sep + deadCount);
    }

    public void dispose()
    {
        if (!done)
        {
            tellToTerminate();
        }
    }

    public void removeLegion(String id)
    {
        sendToClient(Constants.removeLegion + sep + id);
    }

    public void setLegionStatus(String markerId, boolean moved,
        boolean teleported, int entrySide, String lastRecruit)
    {
        sendToClient(Constants.setLegionStatus + sep + markerId + sep + moved
            + sep + teleported + sep + entrySide + sep + lastRecruit);
    }

    public void addCreature(String markerId, String name, String reason)
    {
        sendToClient(Constants.addCreature + sep + markerId + sep + name + sep
            + reason);
    }

    public void removeCreature(String markerId, String name, String reason)
    {
        sendToClient(Constants.removeCreature + sep + markerId + sep + name
            + sep + reason);
    }

    public void revealCreatures(String markerId, final List names,
        String reason)
    {
        sendToClient(Constants.revealCreatures + sep + markerId + sep
            + Glob.glob(names) + sep + reason);
    }

    /** print the 'revealEngagagedCreature'-message,
     *   args: markerId, isAttacker, list of creature names
     * @param markerId legion marker name that is currently in battle
     * @param names List of creature names in this legion
     * @param isAttacker true for attacker, false for defender
     * @param reason why this was revealed
     * @author Towi, copied from revealCreatures
     */
    public void revealEngagedCreatures(final String markerId,
        final List names, final boolean isAttacker, String reason)
    {
        sendToClient(Constants.revealEngagedCreatures + sep + markerId + sep
            + isAttacker + sep + Glob.glob(names) + sep + reason);
    }

    public void removeDeadBattleChits()
    {
        sendToClient(Constants.removeDeadBattleChits);
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
        String hexLabel)
    {
        sendToClient(Constants.placeNewChit + sep + imageName + sep + inverted
            + sep + tag + sep + hexLabel);
    }

    public void initBoard()
    {
        sendToClient(Constants.initBoard);
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
        setName(playerName);

        sendToClient(Constants.setPlayerName + sep + playerName);
    }

    public void createSummonAngel(String markerId)
    {
        sendToClient(Constants.createSummonAngel + sep + markerId);
    }

    public void askAcquireAngel(String markerId, List recruits)
    {
        sendToClient(Constants.askAcquireAngel + sep + markerId + sep
            + Glob.glob(recruits));
    }

    public void askChooseStrikePenalty(List choices)
    {
        sendToClient(Constants.askChooseStrikePenalty + sep
            + Glob.glob(choices));
    }

    public void tellGameOver(String message)
    {
        sendToClient(Constants.tellGameOver + sep + message);
    }

    public void tellPlayerElim(String playerName, String slayerName)
    {
        sendToClient(Constants.tellPlayerElim + sep + playerName + sep
            + slayerName);
    }

    public void askConcede(String allyMarkerId, String enemyMarkerId)
    {
        sendToClient(Constants.askConcede + sep + allyMarkerId + sep
            + enemyMarkerId);
    }

    public void askFlee(String allyMarkerId, String enemyMarkerId)
    {
        sendToClient(Constants.askFlee + sep + allyMarkerId + sep
            + enemyMarkerId);
    }

    public void askNegotiate(String attackerId, String defenderId)
    {
        sendToClient(Constants.askNegotiate + sep + attackerId + sep
            + defenderId);
    }

    public void tellProposal(String proposalString)
    {
        sendToClient(Constants.tellProposal + sep + proposalString);
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft, Set carryTargetDescriptions)
    {
        sendToClient(Constants.tellStrikeResults + sep + strikerTag + sep
            + targetTag + sep + strikeNumber + sep + Glob.glob(rolls) + sep
            + damage + sep + killed + sep + wasCarry + sep + carryDamageLeft
            + sep + Glob.glob(carryTargetDescriptions));
    }

    public void initBattle(String masterHexLabel, int battleTurnNumber,
        String battleActivePlayerName, Constants.BattlePhase battlePhase,
        String attackerMarkerId, String defenderMarkerId)
    {
        sendToClient(Constants.initBattle + sep + masterHexLabel + sep
            + battleTurnNumber + sep + battleActivePlayerName + sep
            + battlePhase.toInt() + sep + attackerMarkerId + sep
            + defenderMarkerId);
    }

    public void cleanupBattle()
    {
        sendToClient(Constants.cleanupBattle);
    }

    public void nextEngagement()
    {
        sendToClient(Constants.nextEngagement);
    }

    public void doReinforce(String markerId)
    {
        sendToClient(Constants.doReinforce + sep + markerId);
    }

    public void didRecruit(String markerId, String recruitName,
        String recruiterName, int numRecruiters)
    {
        sendToClient(Constants.didRecruit + sep + markerId + sep + recruitName
            + sep + recruiterName + sep + numRecruiters);
    }

    public void undidRecruit(String markerId, String recruitName)
    {
        sendToClient(Constants.undidRecruit + sep + markerId + sep
            + recruitName);
    }

    public void setupTurnState(String activePlayerName, int turnNumber)
    {
        sendToClient(Constants.setupTurnState + sep + activePlayerName + sep
            + turnNumber);
    }

    public void setupSplit(String activePlayerName, int turnNumber)
    {
        sendToClient(Constants.setupSplit + sep + activePlayerName + sep
            + turnNumber);
    }

    public void setupMove()
    {
        sendToClient(Constants.setupMove);
    }

    public void setupFight()
    {
        sendToClient(Constants.setupFight);
    }

    public void setupMuster()
    {
        sendToClient(Constants.setupMuster);
    }

    public void setupBattleSummon(String battleActivePlayerName,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleSummon + sep
            + battleActivePlayerName + sep + battleTurnNumber);
    }

    public void setupBattleRecruit(String battleActivePlayerName,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleRecruit + sep
            + battleActivePlayerName + sep + battleTurnNumber);
    }

    public void setupBattleMove(String battleActivePlayerName,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleMove + sep + battleActivePlayerName
            + sep + battleTurnNumber);
    }

    public void setupBattleFight(Constants.BattlePhase battlePhase,
        String battleActivePlayerName)
    {
        sendToClient(Constants.setupBattleFight + sep + battlePhase.toInt()
            + sep + battleActivePlayerName);
    }

    public void tellLegionLocation(String markerId, String hexLabel)
    {
        sendToClient(Constants.tellLegionLocation + sep + markerId + sep
            + hexLabel);
    }

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo)
    {
        sendToClient(Constants.tellBattleMove + sep + tag + sep
            + startingHexLabel + sep + endingHexLabel + sep + undo);
    }

    public void didMove(String markerId, String startingHexLabel,
        String currentHexLabel, String entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.didMove + sep + markerId + sep
            + startingHexLabel + sep + currentHexLabel + sep + entrySide + sep
            + teleport + sep
            + (teleportingLord == null ? "null" : teleportingLord) + sep
            + splitLegionHasForcedMove);
    }

    public void undidMove(String markerId, String formerHexLabel,
        String currentHexLabel, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.undidMove + sep + markerId + sep
            + formerHexLabel + sep + currentHexLabel + sep
            + splitLegionHasForcedMove);
    }

    public void didSummon(String summonerId, String donorId, String summon)
    {
        sendToClient(Constants.didSummon + sep + summonerId + sep + donorId
            + sep + summon);
    }

    public void undidSplit(String splitoffId, String survivorId, int turn)
    {
        sendToClient(Constants.undidSplit + sep + splitoffId + sep
            + survivorId + sep + turn);
    }

    public void didSplit(String hexLabel, String parentId, String childId,
        int childHeight, List splitoffs, int turn)
    {
        sendToClient(Constants.didSplit + sep + hexLabel + sep + parentId
            + sep + childId + sep + childHeight + sep + Glob.glob(splitoffs)
            + sep + turn);
    }

    public void askPickColor(List colorsLeft)
    {
        sendToClient(Constants.askPickColor + sep + Glob.glob(colorsLeft));
    }

    public void askPickFirstMarker()
    {
        sendToClient(Constants.askPickFirstMarker);
    }

    public void log(String message)
    {
        sendToClient(Constants.log + sep + message);
    }

    public void nak(String reason, String errmsg)
    {
        sendToClient(Constants.nak + sep + reason + sep + errmsg);
    }
    
    public void setBoardActive(boolean val)
    {
        sendToClient(Constants.boardActive + sep + val);
    }
}
