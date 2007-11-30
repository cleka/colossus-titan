package net.sf.colossus.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;


/**
 *  Thread to handle one client connection.
 *  @version $Id$
 *  @author David Ripton
 */


final class SocketServerThread extends Thread implements IClient
{
	private static final Logger LOGGER = Logger.getLogger(SocketServerThread.class.getName());

    private Server server;
    private Socket socket;
    private InputStream is;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;
    private List activeSocketList;

    private static final String sep = Constants.protocolTermSeparator;

    SocketServerThread(Server server, Socket socket,
            List activeSocketList)
    {
        super("SocketServerThread");
        this.server = server;
        this.socket = socket;
        this.activeSocketList = activeSocketList;
    }

    public void run()
    {
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
            while ((fromClient = in.readLine()) != null)
            {
                LOGGER.log(Level.FINEST, "From client " + playerName + ": " + fromClient);
                parseLine(fromClient);
            }
            LOGGER.log(Level.FINEST, "End of SocketServerThread while loop");
            server.withdrawFromGame();
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            server.withdrawFromGame();
        }

        // Shut down the client.
        dispose();
        try
        {
            socket.close();
            synchronized (activeSocketList)
            {
                activeSocketList.remove(activeSocketList.indexOf(socket));
            }
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private synchronized void parseLine(String s)
    {
        List li = Split.split(sep, s);
        String method = (String)li.remove(0);
        if (playerName == null && !method.equals(Constants.signOn))
        {
            LOGGER.log(Level.SEVERE, "First packet must be signOn", (Throwable)null);
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
            boolean remote =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            setPlayerName(playerName);
            server.addClient(this, playerName, remote);
        }
        else if (method.equals(Constants.fixName))
        {
            String newName = (String)args.remove(0);
            // Prevent an infinite loop oscillating between two names.
            if (!newName.equals(playerName) &&
                    !newName.startsWith(Constants.byColor))
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
            server.withdrawFromGame();
        }
        else if (method.equals(Constants.disconnect))
        {
            // Ignore. Just for forward compatibility if someone 
            // will use the "new client" (with WebServer and all the 
            // "proper shutdown" related changes).
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
            boolean teleport =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
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
            LOGGER.log(Level.SEVERE, "Bogus packet (Server, method: " +
			method + ", args: " + args + ")", (Throwable)null);
        }
    }

    // IClient methods to sent requests to client over socket.

    public String getPlayerName()
    {
        return playerName;
    }

    public void tellEngagement(String hexLabel, String attackerId,
            String defenderId)
    {
        out.println(Constants.tellEngagement + sep + hexLabel + sep +
                attackerId + sep + defenderId);
    }

    public void tellEngagementResults(String winnerId, String method,
            int points, int turns)
    {
        out.println(Constants.tellEngagementResults + sep + winnerId + sep +
                method + sep + points + sep + turns);
    }

    public void tellMovementRoll(int roll)
    {
        out.println(Constants.tellMovementRoll + sep + roll);
    }

    public void setOption(String optname, String value)
    {
        out.println(Constants.setOption + sep + optname + sep + value);
    }

    public void updatePlayerInfo(List infoStrings)
    {
        out.println(Constants.updatePlayerInfo + sep + Glob.glob(infoStrings));
    }

    public void setColor(String color)
    {
        out.println(Constants.setColor + sep + color);
    }

    public void updateCreatureCount(String creatureName, int count,
            int deadCount)
    {
        out.println(Constants.updateCreatureCount + sep + creatureName + sep +
                count + sep + deadCount);
    }

    public void dispose()
    {
        out.println(Constants.dispose);
    }

    public void removeLegion(String id)
    {
        out.println(Constants.removeLegion + sep + id);
    }

    public void setLegionStatus(String markerId, boolean moved,
            boolean teleported, int entrySide, String lastRecruit)
    {
        out.println(Constants.setLegionStatus + sep + markerId + sep + moved +
                sep + teleported + sep + entrySide + sep + lastRecruit);
    }

    public void addCreature(String markerId, String name, String reason)
    {
        out.println(Constants.addCreature + sep + markerId + sep + name + 
             sep + reason);
    }

    public void removeCreature(String markerId, String name, String reason)
    {
        out.println(Constants.removeCreature + sep + markerId + sep + name + 
             sep + reason);
    }

    public void revealCreatures(String markerId, final List names, 
         String reason)
    {
        out.println(Constants.revealCreatures + sep + markerId + sep +
                Glob.glob(names) + sep + reason);
    }

    /** print the 'revealEngagagedCreature'-message,
     *   args: markerId, isAttacker, list of creature names
     * @param markerId legion marker name that is currently in battle
     * @param names List of creature names in this legion
     * @param isAttacker true for attacker, false for defender
     * @param reason why this was revealed
     * @author Towi, copied from revealCreatures
     */
    public void revealEngagedCreatures(final String markerId, final List names,
        final boolean isAttacker, String reason)
    {
        out.println(Constants.revealEngagedCreatures + sep + markerId
            + sep + isAttacker + sep + Glob.glob(names) + sep + reason);
    }

    public void removeDeadBattleChits()
    {
        out.println(Constants.removeDeadBattleChits);
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
            String hexLabel)
    {
        out.println(Constants.placeNewChit + sep + imageName + sep +
                inverted + sep + tag + sep + hexLabel);
    }

    public void initBoard()
    {
        out.println(Constants.initBoard);
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
        setName(playerName);

        out.println(Constants.setPlayerName + sep + playerName);
    }

    public void createSummonAngel(String markerId)
    {
        out.println(Constants.createSummonAngel + sep + markerId);
    }

    public void askAcquireAngel(String markerId, List recruits)
    {
        out.println(Constants.askAcquireAngel + sep + markerId + sep +
                Glob.glob(recruits));
    }

    public void askChooseStrikePenalty(List choices)
    {
        out.println(Constants.askChooseStrikePenalty + sep +
                Glob.glob(choices));
    }

    public void tellGameOver(String message)
    {
        out.println(Constants.tellGameOver + sep + message);
    }

    public void tellPlayerElim(String playerName, String slayerName)
    {
        out.println(Constants.tellPlayerElim + sep + playerName + sep +
                slayerName);
    }

    public void askConcede(String allyMarkerId, String enemyMarkerId)
    {
        out.println(Constants.askConcede + sep + allyMarkerId + sep +
                enemyMarkerId);
    }

    public void askFlee(String allyMarkerId, String enemyMarkerId)
    {
        out.println(Constants.askFlee + sep + allyMarkerId + sep +
                enemyMarkerId);
    }

    public void askNegotiate(String attackerId, String defenderId)
    {
        out.println(Constants.askNegotiate + sep + attackerId + sep +
                defenderId);
    }

    public void tellProposal(String proposalString)
    {
        out.println(Constants.tellProposal + sep + proposalString);
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
            int strikeNumber, List rolls, int damage, boolean killed,
            boolean wasCarry, int carryDamageLeft, Set carryTargetDescriptions)
    {
        out.println(Constants.tellStrikeResults + sep + strikerTag + sep +
                targetTag + sep + strikeNumber + sep + Glob.glob(rolls) + sep +
                damage + sep + killed + sep + wasCarry + sep + carryDamageLeft +
                sep + Glob.glob(carryTargetDescriptions));
    }

    public void initBattle(String masterHexLabel, int battleTurnNumber,
            String battleActivePlayerName, Constants.BattlePhase battlePhase,
            String attackerMarkerId, String defenderMarkerId)
    {
        out.println(Constants.initBattle + sep + masterHexLabel + sep +
                battleTurnNumber + sep + battleActivePlayerName + sep +
                battlePhase.toInt() + sep + attackerMarkerId + sep +
                defenderMarkerId);
    }

    public void cleanupBattle()
    {
        out.println(Constants.cleanupBattle);
    }

    public void nextEngagement()
    {
        out.println(Constants.nextEngagement);
    }

    public void doReinforce(String markerId)
    {
        out.println(Constants.doReinforce + sep + markerId);
    }

    public void didRecruit(String markerId, String recruitName,
            String recruiterName, int numRecruiters)
    {
        out.println(Constants.didRecruit + sep + markerId + sep +
                recruitName + sep + recruiterName + sep + numRecruiters);
    }

    public void undidRecruit(String markerId, String recruitName)
    {
        out.println(Constants.undidRecruit + sep + markerId + sep +
                recruitName);
    }

    public void setupTurnState(String activePlayerName, int turnNumber)
    {
        out.println(Constants.setupTurnState + sep + activePlayerName + sep +
                turnNumber);
    }

    public void setupSplit(String activePlayerName, int turnNumber)
    {
        out.println(Constants.setupSplit + sep + activePlayerName + sep +
                turnNumber);
    }

    public void setupMove()
    {
        out.println(Constants.setupMove);
    }

    public void setupFight()
    {
        out.println(Constants.setupFight);
    }

    public void setupMuster()
    {
        out.println(Constants.setupMuster);
    }

    public void setupBattleSummon(String battleActivePlayerName,
            int battleTurnNumber)
    {
        out.println(Constants.setupBattleSummon + sep +
                battleActivePlayerName + sep + battleTurnNumber);
    }

    public void setupBattleRecruit(String battleActivePlayerName,
            int battleTurnNumber)
    {
        out.println(Constants.setupBattleRecruit + sep +
                battleActivePlayerName + sep + battleTurnNumber);
    }

    public void setupBattleMove(String battleActivePlayerName,
            int battleTurnNumber)
    {
        out.println(Constants.setupBattleMove + sep +
                battleActivePlayerName + sep + battleTurnNumber);
    }

    public void setupBattleFight(Constants.BattlePhase battlePhase,
            String battleActivePlayerName)
    {
        out.println(Constants.setupBattleFight + sep +
                battlePhase.toInt() + sep + battleActivePlayerName);
    }

    public void tellLegionLocation(String markerId, String hexLabel)
    {
        out.println(Constants.tellLegionLocation + sep + markerId + sep +
                hexLabel);
    }

    public void tellBattleMove(int tag, String startingHexLabel,
            String endingHexLabel, boolean undo)
    {
        out.println(Constants.tellBattleMove + sep + tag + sep +
                startingHexLabel + sep + endingHexLabel + sep + undo);
    }

    public void didMove(String markerId, String startingHexLabel,
            String currentHexLabel, String entrySide, boolean teleport,
            String teleportingLord, boolean splitLegionHasForcedMove)
    {
        out.println(Constants.didMove + sep + markerId + sep +
                startingHexLabel + sep + currentHexLabel + sep + entrySide +
                sep + teleport + sep + 
                (teleportingLord == null ? "null" : teleportingLord) +
                sep + splitLegionHasForcedMove);
    }

    public void undidMove(String markerId, String formerHexLabel,
            String currentHexLabel, boolean splitLegionHasForcedMove)
    {
        out.println(Constants.undidMove + sep + markerId + sep +
                formerHexLabel + sep + currentHexLabel + sep + 
                splitLegionHasForcedMove);
    }

    public void didSummon(String summonerId, String donorId, String summon)
    {
        out.println(Constants.didSummon + sep + summonerId + sep +
                donorId + sep + summon);
    
    }

    public void undidSplit(String splitoffId, String survivorId, int turn)
    {
        out.println(Constants.undidSplit + sep + splitoffId + sep +
                survivorId + sep + turn);
    }

    public void didSplit(String hexLabel, String parentId, String childId,
            int childHeight, List splitoffs, int turn)
    {
        out.println(Constants.didSplit + sep + hexLabel + sep + parentId +
                sep + childId + sep + childHeight + sep +
                Glob.glob(splitoffs) + sep + turn);
    }

    public void askPickColor(List colorsLeft)
    {
        out.println(Constants.askPickColor + sep + Glob.glob(colorsLeft));
    }

    public void askPickFirstMarker()
    {
        out.println(Constants.askPickFirstMarker);
    }

    public void log(String message)
    {
        out.println(Constants.log + sep + message);
    }

    public void nak(String reason, String errmsg)
    {
        out.println(Constants.nak + sep + reason + sep + errmsg);
    }
}
