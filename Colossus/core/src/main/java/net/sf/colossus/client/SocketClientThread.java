package net.sf.colossus.client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.IServer;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Split;


/**
 *  Thread to handle server connection on client side.
 *  @version $Id$
 *  @author David Ripton
 */


final class SocketClientThread extends Thread implements IServer
{
    private Client client;
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean goingDown = false;

    private final static String sep = Constants.protocolTermSeparator;

    SocketClientThread(Client client, String host, int port)
    {
        super("Client " + client.getPlayerName());
        this.client = client;
        this.host = host;
        this.port = port;
    }

    public void run()
    {
        Log.debug("About to connect client socket to " + host + ":" + port);
        try
        {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        // UnknownHostException, IOException, IllegalBlockingModeException
        catch (Exception ex)
        {
            Log.error(ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }

        signOn();

        try
        {
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        }
        catch (IOException ex)
        {
            Log.error(ex);
            return;
        }

        String fromServer = null;
        try
        {
            while ((fromServer = in.readLine()) != null)
            {
                if (fromServer.length() > 0)
                {
                    parseLine(fromServer);
                }
            }
            Log.debug("End of SocketClientThread while loop");
        }
        catch (IOException ex)
        {
            Log.error(ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    /*
     * After this is set, SocketClientThread will not call any client
     * method any more (client might dispose board and exit at any moment).
     */
    public void setGoingDown()
    {
        this.goingDown = true;
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
            client.dispose();
        }
        else if (method.equals(Constants.removeLegion))
        {
            String id = (String)args.remove(0);
            client.removeLegion(id);
        }
        else if (method.equals(Constants.setLegionStatus))
        {
            String markerId = (String)args.remove(0);
            boolean moved =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            boolean teleported =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            int entrySide = Integer.parseInt((String)args.remove(0));
            String lastRecruit = (String)args.remove(0);
            client.setLegionStatus(markerId, moved, teleported, entrySide,
                    lastRecruit);
        }
        else if (method.equals(Constants.addCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            client.addCreature(markerId, name);
        }
        else if (method.equals(Constants.removeCreature))
        {
            String markerId = (String)args.remove(0);
            String name = (String)args.remove(0);
            client.removeCreature(markerId, name);
        }
        else if (method.equals(Constants.revealCreatures))
        {
            String markerId = (String)args.remove(0);
            List names = Split.split(Glob.sep, (String)args.remove(0));
            client.revealCreatures(markerId, names);
        }
        else if (method.equals(Constants.revealEngagedCreatures))
        {
            String markerId = (String)args.remove(0);
            boolean isAttacker =
                Boolean.valueOf((String)args.remove(0)).booleanValue();
            List names = Split.split(Glob.sep, (String)args.remove(0));
            client.revealEngagedCreatures(markerId, names, isAttacker);
        }
        else if (method.equals(Constants.removeDeadBattleChits))
        {
            client.removeDeadBattleChits();
        }
        else if (method.equals(Constants.placeNewChit))
        {
            String imageName = (String)args.remove(0);
            boolean inverted =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
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
            boolean killed =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            boolean wasCarry =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
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
            Constants.BattlePhase battlePhase = Constants.BattlePhase.fromInt(
                Integer.parseInt((String)args.remove(0)));
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
            client.setupBattleRecruit(battleActivePlayerName,
                    battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleMove))
        {
            String battleActivePlayerName = (String)args.remove(0);
            int battleTurnNumber = Integer.parseInt((String)args.remove(0));
            client.setupBattleMove(battleActivePlayerName,
                    battleTurnNumber);
        }
        else if (method.equals(Constants.setupBattleFight))
        {
            Constants.BattlePhase battlePhase = Constants.BattlePhase.fromInt(
                Integer.parseInt((String)args.remove(0)));
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
            boolean undo =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            client.tellBattleMove(tag, startingHexLabel, endingHexLabel, undo);
        }
        else if (method.equals(Constants.didMove))
        {
            String markerId = (String)args.remove(0);
            String startingHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            String entrySide = (String)args.remove(0);
            boolean teleport =
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if ( ! args.isEmpty() )
            {
                splitLegionHasForcedMove = 
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            }
            client.didMove(markerId, startingHexLabel, currentHexLabel,
                    entrySide, teleport, splitLegionHasForcedMove);
        }
        else if (method.equals(Constants.undidMove))
        {
            String markerId = (String)args.remove(0);
            String formerHexLabel = (String)args.remove(0);
            String currentHexLabel = (String)args.remove(0);
            boolean splitLegionHasForcedMove = false;
            // servers from older versions might not send this arg
            if ( ! args.isEmpty() )
            {
                splitLegionHasForcedMove = 
                    Boolean.valueOf((String)args.remove(0)).booleanValue();
            }
            client.undidMove(markerId, formerHexLabel, currentHexLabel,
                    splitLegionHasForcedMove);
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
        else if (method.equals(Constants.tellEngagement))
        {
            client.tellEngagement((String)args.remove(0),
                    (String)args.remove(0),
                    (String)args.remove(0));
        }
        else if (method.equals(Constants.tellEngagementResults))
        {
            String winnerId = (String)args.remove(0);
            String resMethod = (String)args.remove(0);
            int points = Integer.parseInt((String)args.remove(0));
            int turns = Integer.parseInt((String)args.remove(0));
            client.tellEngagementResults(winnerId, resMethod, points,turns);
        }
        else
        {
            Log.error("Bogus packet (Client, method: " +
                    method + ", args: " + args + ")");
        }
    }

    // Setup method
    private void signOn()
    {
        out.println(Constants.signOn + sep + client.getPlayerName() + sep +
                client.isRemote());
    }

    /** Set the thread name to playerName, and tell the server so we
     *  can set this playerName in the right SocketServerThread. */
    void fixName(String playerName)
    {
        setName("Client " + playerName);
        out.println(Constants.fixName + sep + playerName);
    }

    // IServer methods, called from client and sent over the
    // socket to the server.

    public void leaveCarryMode()
    {
        out.println(Constants.leaveCarryMode);
    }

    public void doneWithBattleMoves()
    {
        out.println(Constants.doneWithBattleMoves);
    }

    public void doneWithStrikes()
    {
        out.println(Constants.doneWithStrikes);
    }

    public void acquireAngel(String markerId, String angelType)
    {
        out.println(Constants.acquireAngel + sep + markerId + sep + angelType);
    }

    public void doSummon(String markerId, String donorId, String angel)
    {
        out.println(Constants.doSummon + sep + markerId + sep + donorId +
                sep + angel);
    }

    public void doRecruit(String markerId, String recruitName,
            String recruiterName)
    {
        out.println(Constants.doRecruit + sep + markerId + sep + recruitName +
                sep + recruiterName);
    }

    public void engage(String hexLabel)
    {
        out.println(Constants.engage + sep + hexLabel);
    }

    public void concede(String markerId)
    {
        out.println(Constants.concede + sep + markerId);
    }

    public void doNotConcede(String markerId)
    {
        out.println(Constants.doNotConcede + sep + markerId);
    }

    public void flee(String markerId)
    {
        out.println(Constants.flee + sep + markerId);
    }

    public void doNotFlee(String markerId)
    {
        out.println(Constants.doNotFlee + sep + markerId);
    }

    public void makeProposal(String proposalString)
    {
        out.println(Constants.makeProposal + sep + proposalString);
    }

    public void fight(String hexLabel)
    {
        out.println(Constants.fight + sep + hexLabel);
    }

    public void doBattleMove(int tag, String hexLabel)
    {
        out.println(Constants.doBattleMove + sep + tag + sep + hexLabel);
    }

    public synchronized void strike(int tag, String hexLabel)
    {
        out.println(Constants.strike + sep + tag + sep + hexLabel);
    }

    public synchronized void applyCarries(String hexLabel)
    {
        out.println(Constants.applyCarries + sep + hexLabel);
    }

    public void undoBattleMove(String hexLabel)
    {
        out.println(Constants.undoBattleMove + sep + hexLabel);
    }

    public void assignStrikePenalty(String prompt)
    {
        out.println(Constants.assignStrikePenalty + sep + prompt);
    }

    public void mulligan()
    {
        out.println(Constants.mulligan);
    }

    public void undoSplit(String splitoffId)
    {
        out.println(Constants.undoSplit + sep + splitoffId);
    }

    public void undoMove(String markerId)
    {
        out.println(Constants.undoMove + sep + markerId);
    }

    public void undoRecruit(String markerId)
    {
        out.println(Constants.undoRecruit + sep + markerId);
    }

    public void doneWithSplits()
    {
        out.println(Constants.doneWithSplits);
    }

    public void doneWithMoves()
    {
        out.println(Constants.doneWithMoves);
    }

    public void doneWithEngagements()
    {
        out.println(Constants.doneWithEngagements);
    }

    public void doneWithRecruits()
    {
        out.println(Constants.doneWithRecruits);
    }

    public void withdrawFromGame()
    {
        out.println(Constants.withdrawFromGame);
    }

    public void setDonor(String markerId)
    {
        out.println(Constants.setDonor + sep + markerId);
    }

    public void doSplit(String parentId, String childId, String results)
    {
        out.println(Constants.doSplit + sep + parentId + sep + childId + sep +
                results);
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
            boolean teleport, String teleportingLord)
    {
        out.println(Constants.doMove + sep + markerId + sep + hexLabel + sep +
                entrySide + sep + teleport + sep + teleportingLord);
    }

    public void assignColor(String color)
    {
        out.println(Constants.assignColor + sep + color);
    }

    public void assignFirstMarker(String markerId)
    {
        out.println(Constants.assignFirstMarker + sep + markerId);
    }

    // XXX Disallow the following methods in network games
    public void newGame()
    {
        out.println(Constants.newGame);
    }

    public void loadGame(String filename)
    {
        out.println(Constants.loadGame + sep + filename);
    }

    public void saveGame(String filename)
    {
        out.println(Constants.saveGame + sep + filename);
    }
}
