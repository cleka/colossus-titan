package net.sf.colossus.server;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.webcommon.InstanceTracker;


/**
 *  Holds all data specific to one client connection.
 *  (Earlier this was the class SocketClientThread, but since changing
 *   to NIO it's not an own thread any more.)
 *   
 *  The code in here is (should be) executed exclusively by the server
 *  thread as reaction to something happening on the selector
 *  - first the client connection being accepted, and then later always
 *  when data from client was received (usually from THIS client, but
 *  there might be other cases).
 *    
 *  @version $Id$
 *  @author David Ripton
 */

final class ClientHandler implements IClient
{
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class
        .getName());

    private final Server server;
    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private String playerName;

    private boolean isGone = false;
    private boolean withdrawnAlready = false;

    private static final String sep = Constants.protocolTermSeparator;

    private static int counter = 0;

    private String incompleteInput = "";
    private String incompleteText = "";
    
    private int processedCtr = 0;
    private boolean clientCantFollow = false;

    // Charset and encoder: by default according to the property, 
    // fallback US-ASCII
    String defaultCharSet = System.getProperty("file.encoding");
    String charsetName = defaultCharSet != null ? defaultCharSet : "US-ASCII";
    private final Charset charset = Charset.forName(charsetName);
    private final CharsetEncoder encoder = charset.newEncoder();
    private final CharsetDecoder decoder = charset.newDecoder();

    // Note that the client send ack every CLIENT_CTR_ACK_EVERY messages 
    private final int MAX_SERVER_AHEAD = 500;
    private final int MIN_CLIENT_CATCHUP = 100;
    private final int CTR_SYNC_EVERY_N = 100;
    
    ClientHandler(Server server, SocketChannel channel, SelectionKey selKey)
    {
        this.server = server;
        this.socketChannel = channel;
        this.selectionKey = selKey;

        String tempId = "<no name yet #" + (counter++) + ">";
        InstanceTracker.register(this, tempId);
    }

    public SelectionKey getKey()
    {
        return selectionKey;
    }

    public SocketChannel getSocketChannel()
    {
        return socketChannel;
    }

    // if "isGone" is true, connection to this client is gone
    // Server uses this to decide whether any nonAI player is 
    // (even if perhaps dead) still connected (= watching).
    public boolean isGone()
    {
        return this.isGone;
    }

    public void setIsGone(boolean val)
    {
        this.isGone = val;
    }

    // Called by Server's select reader
    public void processInput(ByteBuffer byteBuffer)
    {
        try
        {
            CharBuffer charBuff = decoder.decode(byteBuffer);

            String msg = incompleteInput + charBuff.toString();
            incompleteInput = "";
            incompleteText = "";

            LOGGER
                .log(Level.FINEST, "Decoded string is >>>>>" + msg + "<<<<<");

            int processed = 0;

            String lines[] = msg.split("\r\n|\n|\r", -1);
            int len = lines.length;
            for (int i = 0; i < len; i++)
            {
                String line = lines[i];
                if (i < len - 1)
                {
                    LOGGER.finest("before processing cmd '" + line + "'");
                    List<String> li = Split.split(sep, line);
                    String method = li.remove(0);
                    if (playerName == null && !method.equals(Constants.signOn))
                    {
                        LOGGER
                            .log(Level.SEVERE, "First packet must be signOn");
                    }
                    else
                    {
                        callMethod(method, li);
                    }
                    LOGGER.finest("after  processing line '" + line + "'");
                    processed++;
                }
                else if (i == len - 1 && line.equals(""))
                {
                    // Received characters ended with newline, producing one
                    // empty string at the end. Perfect.
                }
                else
                {
                    LOGGER.log(Level.FINEST,
                        "last item incomplete, storing it: '" + line + "'");
                    incompleteInput = line;
                    incompleteText = " (not handled: incomplete input '"
                        + incompleteInput + "')";
                }
            }

            LOGGER.log(Level.FINEST, "Processed " + processed + " commands"
                + incompleteText + ".");
        }
        catch (CharacterCodingException cce)
        {
            LOGGER.log(Level.SEVERE,
                "CharacterCodingException while reading from channel"
                    + socketChannel, cce);

        }
    }

    private int msgCounter = 1;
    private LinkedList<String> sendToClientQueue = new LinkedList<String>();
    
    private void sendViaChannel(String msg)
    {
        // TODO queue is not synchronized - can something happen?
        // in later phase of game not - then all action is initiated
        // via the Selector thread; but in beginning, especially
        // during loading, can something happen then?
        sendToClientQueue.add(msg);
        sendSomethingIfPossible();
    }

    public void sendSomethingIfPossible()
    {
        while(!sendToClientQueue.isEmpty() && !clientCantFollow)
        {
            String sendMsg = sendToClientQueue.removeFirst();
         
            sendViaChannelRaw(sendMsg);
            msgCounter++;
            if (msgCounter % CTR_SYNC_EVERY_N == 0)
            {
                sendViaChannelRaw(Constants.msgCtrToClient + sep + msgCounter);
                msgCounter++;
            }
        
            int serverAhead = msgCounter - processedCtr;
            if (serverAhead > MAX_SERVER_AHEAD && !clientCantFollow)
            {
                LOGGER.info("While trying to send to client " + playerName
                    + ": server is " + serverAhead + " messages ahead ("
                    + msgCounter + " vs. " + processedCtr
                    + ") - setting CLIENT-CANT-FOLLOW TRUE");
                clientCantFollow = true;
                Thread.yield();
            }
        }
    }
    private void sendViaChannelRaw(String msg)
    {
        CharBuffer cb = CharBuffer.allocate(msg.length() + 2);
        cb.put(msg);
        cb.put("\n");
        cb.flip();

        try
        {
            ByteBuffer bb = encoder.encode(cb);
            socketChannel.write(bb);
        }
        catch (CharacterCodingException e)
        {
            LOGGER.log(Level.WARNING, "EncondingException '" + e.getMessage()
                + "'" + " was thrown while encoding String '" + msg + "'"
                + " for writing it to" + " channel for player " + playerName);
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING, "IOException '" + ioe.getMessage() + "'"
                + " was thrown while writing String '" + msg + "'"
                + " to channel for player " + playerName);
        }
        Thread.yield();
    }

    private void callMethod(String method, List<String> args)
    {
        if (method.equals(Constants.signOn))
        {
            String playerName = args.remove(0);
            boolean remote = Boolean.valueOf(args.remove(0)).booleanValue();
            boolean success = server.addClient(this, playerName, remote);
            if (success)
            {
                // this setPlayerName is only send for the reason that the client
                // expects a response quickly
                setPlayerName(playerName);
                // @TODO: move to outside Select loop 
                //   => notify main thread to so this?
                server.startGameIfAllPlayers();
            }
            else
            {
                sendToClient(Constants.signOn + sep + "rejected");
            }
            InstanceTracker.setId(this, playerName);
        }
        else if (method.equals(Constants.fixName))
        {
            String newName = args.remove(0);
            // Prevent an infinite loop oscillating between two names.
            // @TODO: is this still needed?
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
            String markerId = args.remove(0);
            String angelType = args.remove(0);
            server.acquireAngel(resolveLegion(markerId), angelType);
        }
        else if (method.equals(Constants.doSummon))
        {
            String markerId = args.remove(0);
            String donorId = args.remove(0);
            String angel = args.remove(0);
            server.doSummon(resolveLegion(markerId), resolveLegion(donorId),
                angel);
        }
        else if (method.equals(Constants.doRecruit))
        {
            String markerId = args.remove(0);
            String recruitName = args.remove(0);
            String recruiterName = args.remove(0);
            server.doRecruit(resolveLegion(markerId), recruitName,
                recruiterName);
        }
        else if (method.equals(Constants.engage))
        {
            String hexLabel = args.remove(0);
            server.engage(resolveMasterHex(hexLabel));
        }
        else if (method.equals(Constants.concede))
        {
            String markerId = args.remove(0);
            server.concede(resolveLegion(markerId));
        }
        else if (method.equals(Constants.doNotConcede))
        {
            String markerId = args.remove(0);
            server.doNotConcede(resolveLegion(markerId));
        }
        else if (method.equals(Constants.flee))
        {
            String markerId = args.remove(0);
            server.flee(resolveLegion(markerId));
        }
        else if (method.equals(Constants.doNotFlee))
        {
            String markerId = args.remove(0);
            server.doNotFlee(resolveLegion(markerId));
        }
        else if (method.equals(Constants.makeProposal))
        {
            String proposalString = args.remove(0);
            server.makeProposal(proposalString);
        }
        else if (method.equals(Constants.fight))
        {
            String hexLabel = args.remove(0);
            server.fight(resolveMasterHex(hexLabel));
        }
        else if (method.equals(Constants.doBattleMove))
        {
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            server.doBattleMove(tag, hexLabel);
        }
        else if (method.equals(Constants.strike))
        {
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            server.strike(tag, hexLabel);
        }
        else if (method.equals(Constants.applyCarries))
        {
            String hexLabel = args.remove(0);
            server.applyCarries(hexLabel);
        }
        else if (method.equals(Constants.undoBattleMove))
        {
            String hexLabel = args.remove(0);
            server.undoBattleMove(hexLabel);
        }
        else if (method.equals(Constants.assignStrikePenalty))
        {
            String prompt = args.remove(0);
            server.assignStrikePenalty(prompt);
        }
        else if (method.equals(Constants.mulligan))
        {
            server.mulligan();
        }
        else if (method.equals(Constants.undoSplit))
        {
            String splitoffId = args.remove(0);
            server.undoSplit(resolveLegion(splitoffId));
        }
        else if (method.equals(Constants.undoMove))
        {
            String markerId = args.remove(0);
            server.undoMove(resolveLegion(markerId));
        }
        else if (method.equals(Constants.undoRecruit))
        {
            String markerId = args.remove(0);
            server.undoRecruit(resolveLegion(markerId));
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
            if (!withdrawnAlready)
            {
                withdrawnAlready = true;
                server.withdrawFromGame();
            }
        }
        else if (method.equals(Constants.disconnect))
        {
            isGone = true;
            if (!withdrawnAlready)
            {
                LOGGER.log(Level.FINE,
                    "Client disconnected without explicit withdraw - "
                        + "doing automatic withdraw for player " + playerName);
                server.withdrawFromGame();
                withdrawnAlready = true;
            }
            server.disconnect();
        }
        else if (method.equals(Constants.stopGame))
        {
            server.disconnect();
            server.stopGame();
        }
        else if (method.equals(Constants.doSplit))
        {
            String parentId = args.remove(0);
            String childId = args.remove(0);
            String results = args.remove(0);
            server.doSplit(resolveLegion(parentId), childId, results);
        }
        else if (method.equals(Constants.doMove))
        {
            String markerId = args.remove(0);
            String hexLabel = args.remove(0);
            String entrySide = args.remove(0);
            boolean teleport = Boolean.valueOf(args.remove(0)).booleanValue();
            String teleportingLord = args.remove(0);
            server.doMove(resolveLegion(markerId), resolveMasterHex(hexLabel),
                entrySide, teleport, teleportingLord);
        }
        else if (method.equals(Constants.assignColor))
        {
            String color = args.remove(0);
            server.assignColor(color);
        }
        else if (method.equals(Constants.assignFirstMarker))
        {
            String markerId = args.remove(0);
            server.assignFirstMarker(markerId);
        }
        else if (method.equals(Constants.newGame))
        {
            server.newGame();
        }
        else if (method.equals(Constants.loadGame))
        {
            String filename = args.remove(0);
            server.loadGame(filename);
        }
        else if (method.equals(Constants.saveGame))
        {
            String filename = args.remove(0);
            server.saveGame(filename);
        }
        else if (method.equals(Constants.processedCtr))
        {
            processedCtr = Integer.parseInt(args.remove(0));
            int serverAhead = msgCounter - processedCtr;
            if (serverAhead < MIN_CLIENT_CATCHUP && clientCantFollow)
            {
                LOGGER.info("Received processedCounter from client " + playerName
                    + ": server is " + serverAhead + " messages ahead ("
                    + msgCounter + " vs. " + processedCtr
                    + ") - setting client-cant-follow false");

                clientCantFollow = false;
                
                sendViaChannelRaw(Constants.msgCtrToClient + sep + msgCounter);
                msgCounter++;

                while(!clientCantFollow && !sendToClientQueue.isEmpty())
                {
                    // System.out.println("During processing read, client can follow loop...");
                    sendSomethingIfPossible();
                }
                // System.out.println("\n\n*****\nAfter !clientCantFollow loop!\n");
            }
        }
        else if (method.equals(Constants.catchupConfirmation))
        {
            server.clientConfirmedCatchup();
        }
        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Server, method: " + method
                + ", args: " + args + ")");
        }
    }

    private MasterHex resolveMasterHex(String hexLabel)
    {
        return server.getGame().getVariant().getMasterBoard().getHexByLabel(
            hexLabel);
    }

    private Legion resolveLegion(String markerId)
    {
        // TODO: currently doSummon still allows a null legion (and thus legion marker
        //       on the network) to indicate that a summon was skipped. To disallow
        //       having the null values in here we would need to introduce a new
        //       network message such as "doneSummoning".
        if (markerId.equals("null"))
        {
            return null;
        }
        return server.getGame().getLegionByMarkerId(markerId);
    }

    // Wrapper for all the send-over-socket methods:
    public void sendToClient(String message)
    {
        if (isGone || socketChannel == null)
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
            sendViaChannel(message);
            if (server.getGame().isLoadingGame())
            {
                // Give clients some opportunity to process it
                // (especially during replay during loading game)
                Thread.yield();
            }
        }
    }

    // IClient methods to sent requests to client over socket.

    public void dispose()
    {
        // Don't do it again
        if (isGone)
        {
            return;
        }

        isGone = true;
        sendViaChannel(Constants.dispose);
        server.disposeClientHandler(this);
    }

    public void tellEngagement(MasterHex hex, Legion attacker, Legion defender)
    {
        sendToClient(Constants.tellEngagement + sep + hex.getLabel() + sep
            + attacker.getMarkerId() + sep + defender.getMarkerId());
    }

    public void tellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        sendToClient(Constants.tellEngagementResults + sep
            + (winner != null ? winner.getMarkerId() : null) + sep + method
            + sep + points + sep + turns);
    }

    public void tellMovementRoll(int roll)
    {
        sendToClient(Constants.tellMovementRoll + sep + roll);
    }

    public void setOption(String optname, String value)
    {
        sendToClient(Constants.setOption + sep + optname + sep + value);
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        sendToClient(Constants.updatePlayerInfo + sep + Glob.glob(infoStrings));
    }

    public void setColor(String color)
    {
        sendToClient(Constants.setColor + sep + color);
    }

    public void updateCreatureCount(CreatureType type, int count, int deadCount)
    {
        sendToClient(Constants.updateCreatureCount + sep + type.getName()
            + sep + count + sep + deadCount);
    }

    public void removeLegion(Legion legion)
    {
        sendToClient(Constants.removeLegion + sep + legion.getMarkerId());
    }

    public void setLegionStatus(Legion legion, boolean moved,
        boolean teleported, int entrySide, String lastRecruit)
    {
        sendToClient(Constants.setLegionStatus + sep + legion.getMarkerId()
            + sep + moved + sep + teleported + sep + entrySide + sep
            + lastRecruit);
    }

    public void addCreature(Legion legion, String name, String reason)
    {
        sendToClient(Constants.addCreature + sep + legion.getMarkerId() + sep
            + name + sep + reason);
    }

    public void removeCreature(Legion legion, String name, String reason)
    {
        sendToClient(Constants.removeCreature + sep + legion.getMarkerId()
            + sep + name + sep + reason);
    }

    public void revealCreatures(Legion legion, final List<String> names,
        String reason)
    {
        sendToClient(Constants.revealCreatures + sep + legion.getMarkerId()
            + sep + Glob.glob(names) + sep + reason);
    }

    /** print the 'revealEngagagedCreature'-message,
     *   args: markerId, isAttacker, list of creature names
     * @param markerId legion marker name that is currently in battle
     * @param names List of creature names in this legion
     * @param isAttacker true for attacker, false for defender
     * @param reason why this was revealed
     * @author Towi, copied from revealCreatures
     */
    public void revealEngagedCreatures(final Legion legion,
        final List<String> names, final boolean isAttacker, String reason)
    {
        sendToClient(Constants.revealEngagedCreatures + sep
            + legion.getMarkerId() + sep + isAttacker + sep + Glob.glob(names)
            + sep + reason);
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

    public void tellReplay(boolean val, int maxTurn)
    {
        sendToClient(Constants.replayOngoing + sep + val + sep + maxTurn);
    }

    public void initBoard()
    {
        sendToClient(Constants.initBoard);
    }

    public void setPlayerName(String playerName)
    {
        this.playerName = playerName;
        sendToClient(Constants.setPlayerName + sep + playerName);
    }

    public String getPlayerName()
    {
        return this.playerName;
    }

    public void createSummonAngel(Legion legion)
    {
        sendToClient(Constants.createSummonAngel + sep + legion.getMarkerId());
    }

    public void askAcquireAngel(Legion legion, List<String> recruits)
    {
        sendToClient(Constants.askAcquireAngel + sep + legion.getMarkerId()
            + sep + Glob.glob(recruits));
    }

    public void askChooseStrikePenalty(List<String> choices)
    {
        sendToClient(Constants.askChooseStrikePenalty + sep
            + Glob.glob(choices));
    }

    public void tellGameOver(String message)
    {
        sendViaChannelRaw(Constants.msgCtrToClient + sep + msgCounter);
        msgCounter++;

        sendToClient(Constants.tellGameOver + sep + message);
    }

    public void tellPlayerElim(Player player, Player slayer)
    {
        // slayer can be null
        sendToClient(Constants.tellPlayerElim + sep + player.getName() + sep
            + (slayer != null ? slayer.getName() : null));
    }

    public void askConcede(Legion ally, Legion enemy)
    {
        sendToClient(Constants.askConcede + sep + ally.getMarkerId() + sep
            + enemy.getMarkerId());
    }

    public void askFlee(Legion ally, Legion enemy)
    {
        sendToClient(Constants.askFlee + sep + ally.getMarkerId() + sep
            + enemy.getMarkerId());
    }

    public void askNegotiate(Legion attacker, Legion defender)
    {
        sendToClient(Constants.askNegotiate + sep + attacker.getMarkerId()
            + sep + defender.getMarkerId());
    }

    public void tellProposal(String proposalString)
    {
        sendToClient(Constants.tellProposal + sep + proposalString);
    }

    public void tellStrikeResults(int strikerTag, int targetTag,
        int strikeNumber, List<String> rolls, int damage, boolean killed,
        boolean wasCarry, int carryDamageLeft,
        Set<String> carryTargetDescriptions)
    {
        sendToClient(Constants.tellStrikeResults + sep + strikerTag + sep
            + targetTag + sep + strikeNumber + sep + Glob.glob(rolls) + sep
            + damage + sep + killed + sep + wasCarry + sep + carryDamageLeft
            + sep + Glob.glob(carryTargetDescriptions));
    }

    public void initBattle(MasterHex hex, int battleTurnNumber,
        Player battleActivePlayer, Constants.BattlePhase battlePhase,
        Legion attacker, Legion defender)
    {
        sendToClient(Constants.initBattle + sep + hex.getLabel() + sep
            + battleTurnNumber + sep + battleActivePlayer.getName() + sep
            + battlePhase.toInt() + sep + attacker.getMarkerId() + sep
            + defender.getMarkerId());
    }

    public void cleanupBattle()
    {
        sendToClient(Constants.cleanupBattle);
    }

    public void nextEngagement()
    {
        sendToClient(Constants.nextEngagement);
    }

    public void doReinforce(Legion legion)
    {
        sendToClient(Constants.doReinforce + sep + legion.getMarkerId());
    }

    public void didRecruit(Legion legion, String recruitName,
        String recruiterName, int numRecruiters)
    {
        sendToClient(Constants.didRecruit + sep + legion.getMarkerId() + sep
            + recruitName + sep + recruiterName + sep + numRecruiters);
    }

    public void undidRecruit(Legion legion, String recruitName)
    {
        sendToClient(Constants.undidRecruit + sep + legion.getMarkerId() + sep
            + recruitName);
    }

    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        sendToClient(Constants.setupTurnState + sep + activePlayer.getName()
            + sep + turnNumber);
    }

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        sendToClient(Constants.setupSplit + sep + activePlayer.getName() + sep
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

    public void setupBattleSummon(Player battleActivePlayer,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleSummon + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleRecruit(Player battleActivePlayer,
        int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleRecruit + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleMove(Player battleActivePlayer, int battleTurnNumber)
    {
        sendToClient(Constants.setupBattleMove + sep
            + battleActivePlayer.getName() + sep + battleTurnNumber);
    }

    public void setupBattleFight(Constants.BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        sendToClient(Constants.setupBattleFight + sep + battlePhase.toInt()
            + sep + battleActivePlayer.getName());
    }

    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        sendToClient(Constants.tellLegionLocation + sep + legion.getMarkerId()
            + sep + hex.getLabel());
    }

    public void tellBattleMove(int tag, String startingHexLabel,
        String endingHexLabel, boolean undo)
    {
        sendToClient(Constants.tellBattleMove + sep + tag + sep
            + startingHexLabel + sep + endingHexLabel + sep + undo);
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, String entrySide, boolean teleport,
        String teleportingLord, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.didMove + sep + legion.getMarkerId() + sep
            + startingHex.getLabel() + sep + currentHex.getLabel() + sep
            + entrySide + sep + teleport + sep
            + (teleportingLord == null ? "null" : teleportingLord) + sep
            + splitLegionHasForcedMove);
    }

    public void undidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.undidMove + sep + legion.getMarkerId() + sep
            + formerHex.getLabel() + sep + currentHex.getLabel() + sep
            + splitLegionHasForcedMove);
    }

    public void didSummon(Legion summoner, Legion donor, String summon)
    {
        sendToClient(Constants.didSummon + sep + summoner.getMarkerId() + sep
            + donor.getMarkerId() + sep + summon);
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        sendToClient(Constants.undidSplit + sep + splitoff.getMarkerId() + sep
            + survivor.getMarkerId() + sep + turn);
    }

    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<String> splitoffs, int turn)
    {
        // hex can be null when loading a game
        // TODO make sure we always have a hex
        assert parent != null : " Split needs parent";
        assert child != null : " Split needs child";
        assert hex != null : "Split needs location";
        sendToClient(Constants.didSplit + sep + hex.getLabel() + sep
            + parent.getMarkerId() + sep + child.getMarkerId() + sep
            + childHeight + sep + Glob.glob(splitoffs) + sep + turn);
    }

    public void askPickColor(List<String> colorsLeft)
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

    public void confirmWhenCaughtUp()
    {
        sendToClient(Constants.askConfirmCatchUp);
    }

}

