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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.IClient;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;


/**
 *  Holds all data specific to one client connection.
 *  (Earlier this was the class ServerSocketThread, but since changing
 *   to NIO it's not an own thread any more.)
 *
 *  The code in here is (should be) executed exclusively by the server
 *  thread as reaction to something happening on the selector
 *  - first the client connection being accepted, and then later always
 *  when data from client was received (usually from THIS client, but
 *  there might be other cases).
 *
 *  @author David Ripton
 */
final class ClientHandler implements IClient
{
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class
        .getName());

    private final Server server;
    private final SocketChannel socketChannel;
    private final SelectionKey selectorKey;
    private String playerName;
    private String signonName;
    private int clientVersion = 0;

    private boolean isGone = false;
    private boolean withdrawnAlready = false;
    private int isGoneMessageRepeated = 0;
    private boolean temporarilyDisconnected = false;

    private static final String sep = Constants.protocolTermSeparator;

    private static int counter = 0;

    private String incompleteInput = "";
    private String incompleteText = "";

    // Charset and encoder: by default according to the property,
    // fallback US-ASCII
    private static final String DEFAULT_CHAR_SET = System
        .getProperty("file.encoding");
    private final String CHARSET_NAME = DEFAULT_CHAR_SET != null ? DEFAULT_CHAR_SET
        : "US-ASCII";
    private final Charset charset = Charset.forName(CHARSET_NAME);
    private final CharsetEncoder encoder = charset.newEncoder();
    private final CharsetDecoder decoder = charset.newDecoder();

    // sync-when-disconnected stuff
    private int messageCounter = 0;
    private int commitPointCounter = 0;

    private final ArrayList<MessageForClient> redoQueue = new ArrayList<MessageForClient>(
        50);

    private final static int MAX_KEEP_LINES = 5;
    private final ArrayList<String> recentlyProcessedLines = new ArrayList<String>(
        MAX_KEEP_LINES);

    // Note that the client (SocketClientThread) sends ack every
    // CLIENT_CTR_ACK_EVERY messages (currently 20)
    // The two values above and the client value must fit together
    // that it does not cause a deadlock.

    ClientHandler(Server server, SocketChannel channel, SelectionKey selKey)
    {
        this.server = server;
        this.socketChannel = channel;
        this.selectorKey = selKey;

        String tempId = "<no name yet #" + (counter++) + ">";
        InstanceTracker.register(this, tempId);
    }

    public SelectionKey getSelectorKey()
    {
        return selectorKey;
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

    public void setTemporarilyDisconnected()
    {
        temporarilyDisconnected = true;
    }

    /* Not really needed at the moment; every reconnect creates a new ClientHandler
    private void clearTemporarilyDisconnected()
    {
        temporarilyDisconnected = false;
    }
    */

    public boolean isTemporarilyDisconnected()
    {
        return temporarilyDisconnected;
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
                    if (signonName == null && !method.equals(Constants.signOn))
                    {
                        LOGGER
                            .log(Level.SEVERE,
                                "First packet must be signOn, but it is "
                                    + method);
                    }
                    else
                    {
                        doCallMethodInTryBlock(line, method, li);
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

    private void sendViaChannel(String msg)
    {
        sendViaChannelRaw(msg);
    }

    /**
     * here starts some stuff needed for the "synchronization in disconnect case"
     */
    private class MessageForClient
    {
        private final int messageNumber;
        private final int commitNumber;
        private final String message;

        public MessageForClient(int messageNr, int commitNr, String message)
        {
            this.messageNumber = messageNr;
            this.commitNumber = commitNr;
            this.message = message;
        }

        public int getMessageNr()
        {
            return messageNumber;
        }

        public int getCommitNumber()
        {
            return commitNumber;
        }

        public String getMessage()
        {
            return message;
        }
    }

    private void enqueueToRedoQueue(int messageNr, String message)
    {
        if (supportsReconnect())
        {
            redoQueue.add(new MessageForClient(messageNr,
                (isCommitPoint ? commitPointCounter : 0), message));
        }
    }

    private boolean isCommitPoint = false;

    private void commitPoint()
    {
        if (supportsReconnect())
        {
            commitPointCounter++;
            isCommitPoint = true;
            sendToClient(Constants.commitPoint + sep + commitPointCounter
                + sep + messageCounter);
            isCommitPoint = false;
        }
    }

    /**
     * Remove the messages in redoQueue prior to given commit point
     * @param confirmedNr Commit point from which we now know that client has
     * successfully received it
     */
    private void confirmCommitPoint(int confirmedNr)
    {
        int found = -1;
        int size = redoQueue.size();
        for(int i = 0; i < size && found == -1; i++)
        {
            MessageForClient mfc = redoQueue.get(i);
            if (mfc.getCommitNumber() == confirmedNr)
            {
                found = i;
            }
        }

        if (found != -1)
        {
            /*
             * TODO: If we would subclass ArrayList, could use the more
             * efficient method removeRange (it is "protected")
             */
            int deleted = 0;
            for (int i = 0; i <= found; i++)
            {
                deleted++;
                redoQueue.get(0);
                redoQueue.remove(0);
            }
        }
    }

    public boolean supportsReconnect()
    {
        return clientVersion >= IServer.CLIENT_VERSION_CAN_RECONNECT;
    }

    public boolean canHandleBattleMoveNak()
    {
        return clientVersion >= 4;
    }

    public void cloneRedoQueue(ClientHandler oldCH)
    {
        // Remove the reconnect-related messages
        redoQueue.clear();
        redoQueue.addAll(oldCH.redoQueue);
        commitPointCounter = oldCH.commitPointCounter;
    }

    /**
     * Re-send all data after the message nr from which we know client got it
     *
     * @param lastReceivedMessageNr Last messagewhich client did still receive
     */
    public void syncAfterReconnect(int lastReceivedMessageNr)
    {
        // to get client out of initial readlines loop, to get it into
        // normal "read from socket and parse line" loop
        setPlayerName(signonName);

        int size = redoQueue.size();
        for (int i = 0; i < size; i++)
        {
            MessageForClient mfc = redoQueue.get(i);
            int queueMsgNr = mfc.getMessageNr();
            if (queueMsgNr > lastReceivedMessageNr)
            {
                String message = mfc.getMessage();
                sendViaChannelRaw(message);
                messageCounter = queueMsgNr;
            }
        }
        commitPoint();
    }

    /**
     * only for testing/development purposes
     */
    public void fakeDisconnectClient()
    {
        LOGGER.warning("fake disconnect by server side not implemented yet.");
    }

    ByteBuffer bb;
    String encodedMsg; // used only for logging
    int should;
    int writtenTotal;

    int previousRetries = 0;

    private long temporarilyInTrouble = -1;

    public boolean isTemporarilyInTrouble()
    {
        return (temporarilyInTrouble != -1);
    }

    public long howLongAlreadyInTrouble()
    {
        if (temporarilyInTrouble == -1)
        {
            return 0;
        }
        long now = new Date().getTime();
        return now - temporarilyInTrouble;
    }

    private void setTemporarilyInTrouble()
    {
        long now = new Date().getTime();
        temporarilyInTrouble = now;
        server.othersTellOneHasNetworkTrouble(this);
        selectorKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void clearTemporarilyInTrouble()
    {
        temporarilyInTrouble = -1;
        server.othersTellOnesTroubleIsOver(this);
    }

    private void handleEncoding(String msg)
    {
        try
        {
            encodedMsg = msg;
            String dataToSend = msg + "\n";
            CharBuffer cb = CharBuffer.allocate(dataToSend.length());
            cb.put(dataToSend);
            cb.flip();

            bb = encoder.encode(cb);
            should = bb.limit();
            writtenTotal = 0;
        }
        catch (CharacterCodingException e)
        {
            LOGGER.log(Level.WARNING, "EncondingException '" + e.getMessage()
                + "'" + " was thrown while encoding String '" + msg + "'"
                + " for writing it to" + " channel for player " + playerName
                + "; details follow", e);
        }

    }

    /** The queue in which messages are stored, until they were really written.
     *  Usually empty; stuff piles up only when writing to socket fails,
     *  e.g. network or client too slow.
     */
    LinkedList<String> queue = new LinkedList<String>();

    private void sendViaChannelRaw(String msg)
    {
        // Something left undone last time. Postpone this here for a moment.
        // Except if it is null, then caller called us dedicatedly to give
        // us a chance to finish earlier stuff.
        if (msg != null)
        {
            queue.add(msg);
        }

        if (isTemporarilyInTrouble())
        {
            return;
        }

        if (previousRetries > 0)
        {
            // Try writing old stuff away.
            attemptWritingToChannel();
        }

        // If there was no problem and/or now it went well, proceed with new
        // stuff... if there was.
        while (previousRetries == 0 && queue.size() > 0)
        {
            String queueMsg = queue.poll();
            handleEncoding(queueMsg);
            attemptWritingToChannel();
        }

        if (previousRetries > 0)
        {
            setTemporarilyInTrouble();
        }
        else
        {
            if (isTemporarilyInTrouble())
            {
                LOGGER.warning("temporaryInTrouble still true for player "
                    + getPlayerName() + "? This should never happen.");
            }
            temporarilyInTrouble = -1;
        }
    }

    private void attemptWritingToChannel()
    {
        // Attempt to write away what is in buffer
        try
        {
            int written = socketChannel.write(bb);
            if (written > 0)
            {
                writtenTotal += written;
            }

            if (writtenTotal < should)
            {
                // Not all written
                previousRetries += 1;
                LOGGER.warning("trouble writing, temporarily giving up "
                    + "writing to client " + getPlayerName());
            }
            else
            {
                // OK, now all was written
                // TODO nowadays we do only one try, can this be a boolean instead?
                if (previousRetries > 0)
                {
                    LOGGER.info("Now succeeded, attempt = " + previousRetries);
                }
                previousRetries = 0;
            }
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING,
                "IOException '" + ioe.getMessage() + "'"
                    + " was thrown while writing String '" + encodedMsg + "'"
                    + " to channel for player " + playerName
                    + "; details follow:", ioe);

            if (this.supportsReconnect())
            {
                setTemporarilyDisconnected();
            }
            else
            {
                setIsGone(true);
                withdrawnAlready = true;
                server.withdrawFromGame(playerName);
                server.queueClientHandlerForChannelChanges(this);
                server.clientWontConfirmCatchup(this,
                    "IO Exception while writing to client " + playerName);
            }
        }
        Thread.yield();
    }

    private void doCallMethodInTryBlock(String line, String method,
        List<String> li)
    {
        try
        {
            while (recentlyProcessedLines.size() >= MAX_KEEP_LINES)
            {
                recentlyProcessedLines.remove(0);
            }
            recentlyProcessedLines.add(line);
            callMethod(method, li);
        }
        catch (Exception e)
        {
            String message = "Woooah! An exception was caught while "
                + "processing from client " + getPlayerName()
                + " the input line:\n    === " + line + " ===\n"
                + "\nStack trace:\n" + ErrorUtils.makeStackTraceString(e)
                + "\n\nGame might be unstable or hang from now on...";
            LOGGER.severe(message);
            LOGGER.info(dumpLastProcessedLines());
            ErrorUtils.showExceptionDialog(null, message, "Exception caught!",
                true);
        }
    }

    public String dumpLastProcessedLines()
    {
        StringBuffer sb = new StringBuffer("## Last " + MAX_KEEP_LINES
            + " processed lines were:");
        int i = 0;
        for (String rLine : recentlyProcessedLines)
        {
            i++;
            sb.append("\n      #" + i + ": " + rLine);
        }
        return sb.toString();
    }
    private void callMethod(String method, List<String> args)
    {
        if (method.equals(Constants.signOn))
        {
            String signonTryName = args.remove(0);
            boolean remote = Boolean.valueOf(args.remove(0)).booleanValue();
            String buildInfo;
            if (args.size() < 2)
            {
                LOGGER.info("Connecting client with signonName "
                    + signonTryName + " did not send version/build info - "
                    + "treating that as version -1, build info NONE.");
                clientVersion = -1;
                buildInfo = "NONE";
            }
            else
            {
                clientVersion = Integer.parseInt(args.remove(0));
                buildInfo = args.remove(0);
            }
            String reasonFail = server.addClient(this, signonTryName, remote,
                clientVersion, buildInfo);
            if (reasonFail == null)
            {
                sendToClient("Ack: signOn");
                this.signonName = signonTryName;
            }
            else
            {
                LOGGER.info("Rejecting client " + signonTryName);
                nak("SignOn", reasonFail);
            }
            InstanceTracker.setId(this, signonTryName);
        }
        else if (method.equals(Constants.joinGame))
        {
            String playerName = args.remove(0);
            if (!playerName.equals(signonName))
            {
                LOGGER.severe("Joining game with different name '"
                    + playerName + "' than signonName + '" + signonName
                    + "' is currently not supported!");
                return;
            }
            setPlayerName(signonName);
            server.joinGame(signonName);
        }
        else if (method.equals(Constants.requestGameInfo))
        {
            server.replyToRequestGameInfo();
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
            server.acquireAngel(resolveLegion(markerId),
                resolveCreatureTypeNullOk(angelType));
        }
        else if (method.equals(Constants.doSummon))
        {
            Legion legion = resolveLegion(args.remove(0));
            if (legion == null)
            {
                server.doSummon(null);
                return;
            }
            Legion donor = resolveLegion(args.remove(0));
            CreatureType creatureType = resolveCreatureType(args.remove(0));
            server.doSummon(new Summoning(legion, donor, creatureType));
        }
        else if (method.equals(Constants.doRecruit))
        {
            Legion legion = resolveLegion(args.remove(0));
            // Refusing a reinforcement sends as "recruited" null.
            CreatureType recruited = resolveCreatureTypeNullOk(args.remove(0));
            CreatureType recruiter = resolveCreatureTypeNullOk(args.remove(0));
            server.doRecruit(new Recruitment(legion, recruited, recruiter));
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
            BattleHex hex = resolveBattleHex(hexLabel);
            server.doBattleMove(tag, hex);
        }
        else if (method.equals(Constants.strike))
        {
            int tag = Integer.parseInt(args.remove(0));
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.strike(tag, hex);
        }
        else if (method.equals(Constants.applyCarries))
        {
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.applyCarries(hex);
        }
        else if (method.equals(Constants.undoBattleMove))
        {
            String hexLabel = args.remove(0);
            BattleHex hex = resolveBattleHex(hexLabel);
            server.undoBattleMove(hex);
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
            withdrawIfNeeded(true);
        }
        else if (method.equals(Constants.disconnect))
        {
            setIsGone(true);
            withdrawIfNeeded(false);
            server.disconnect();
        }
        else if (method.equals(Constants.stopGame))
        {
            setIsGone(true);
            server.disconnect();
            server.stopGame();
        }
        else if (method.equals(Constants.doSplit))
        {
            String parentId = args.remove(0);
            String childId = args.remove(0);
            String results = args.remove(0);
            List<CreatureType> creatures = new ArrayList<CreatureType>();
            for (String name : results.split(","))
            {
                creatures.add(resolveCreatureType(name));
            }
            server.doSplit(resolveLegion(parentId), childId, creatures);
        }
        else if (method.equals(Constants.doMove))
        {
            String markerId = args.remove(0);
            String hexLabel = args.remove(0);
            EntrySide entrySide = EntrySide.fromLabel(args.remove(0));
            boolean teleport = Boolean.valueOf(args.remove(0)).booleanValue();
            CreatureType teleportingLord = resolveCreatureTypeNullOk(args
                .remove(0));
            server.doMove(resolveLegion(markerId), resolveMasterHex(hexLabel),
                entrySide, teleport, teleportingLord);
        }
        else if (method.equals(Constants.assignColor))
        {
            String color = args.remove(0);
            server.assignColor(PlayerColor.getByName(color));
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

        else if (method.equals(Constants.checkConnection))
        {
            server.checkServerConnection();
        }

        else if (method.equals(Constants.requestSyncDelta))
        {
            int lastReceivedMsgNr = Integer.parseInt(args.remove(0));
            server.requestSyncDelta(lastReceivedMsgNr);
        }

        else if (method.equals(Constants.catchupConfirmation))
        {
            server.clientConfirmedCatchup();
        }

        else if (method.equals(Constants.replyToPing))
        {
            LOGGER.fine("Client " + playerName
                + " replied to ping request - fine!");
        }

        else if (method.equals(Constants.confirmCommitPoint))
        {
            int cpNr = Integer.parseInt(args.remove(0));
            confirmCommitPoint(cpNr);
        }

        else
        {
            LOGGER.log(Level.SEVERE, "Bogus packet (Server, method: " + method
                + ", args: " + args + ")");
        }
    }

    private BattleHex resolveBattleHex(String hexLabel)
    {
        return server.getGame().getBattleSS().getLocation().getTerrain()
            .getHexByLabel(hexLabel);
    }

    // TODO resolveX methods are on both sides of the network, they should
    // be extracted into some resolver object (or a base class)
    private CreatureType resolveCreatureType(String name)
    {
        return server.getGame().getVariant().getCreatureByName(name);
    }

    /**
     * There are cases where "null" comes over network and is not meant to
     * be resolved to a CreatureType, namely:
     * teleportingLord if no teleport; null recruiter; decline Acquire.
     * TODO What to do with the "Anything"?
     * @param name Name of the creatureType to find, might be "null"
     * @return CreatureType for that name, or null if name is "null"
     */
    private CreatureType resolveCreatureTypeNullOk(String name)
    {
        return name.equals("null") ? null : resolveCreatureType(name);
    }

    /**
     * Make sure player is withdrawn from game.
     * Explicit if via Withdraw message from client, implicit because
     * of disconnect message or connection problems.
     * This is just a wrapper for the both situations where for
     * !withdrawnAlready should be checked
     *
     * @param explicit Whether client has requested withdraw explicitly
     */
    private void withdrawIfNeeded(boolean explicit)
    {
        if (!withdrawnAlready)
        {
            if (!explicit)
            {
                LOGGER.log(Level.FINE,
                    "Client disconnected without explicit withdraw - "
                        + "doing automatic withdraw (if needed) for player "
                        + playerName);
            }
            withdrawnAlready = true;
            server.withdrawFromGame();
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
        // Comment (Clemens): one day I would like to get Summon as part of
        // the move phase, not as "do it or decline it request" from server.
        // When we get there, there is no need for null nor for explicit
        // doneSummoning.
        if (markerId.equals("null"))
        {
            return null;
        }
        return server.getGame().getLegionByMarkerId(markerId);
    }

    // Wrapper for all the send-over-socket methods:
    public void sendToClient(String message)
    {
        enqueueToRedoQueue(messageCounter, message);
        messageCounter++;

        if (isGone || socketChannel == null)
        {
            // do not send any more
            if (isGoneMessageRepeated < 3)
            {
                LOGGER.info("Attempt to send to player " + playerName
                    + " when client connection already gone - message: "
                    + message);
                isGoneMessageRepeated++;
            }
        }
        else
        {
            if (Constants.USE_RECORDER)
            {
                server.getRecorder().recordMessageToClient(this, message);
            }
            if (server.getGame().isGameOver())
            {
                LOGGER.info("Sending to " + playerName + ": " + message);
            }
            sendViaChannel(message);

            if (server == null)
            {
                LOGGER.severe("server null");
            }
            else if (server.getGame() == null)
            {
                LOGGER.severe("game null");
            }

            else if (server.getGame().isLoadingGame())
            {
                // Give clients some opportunity to process it
                // (especially during replay during loading game)
                Thread.yield();
            }
        }
    }

    // IClient methods to sent requests to client over socket.

    /**
     * Server side disposes a client (and informs it about it first)
     */
    public void dispose()
    {
        // Don't do it again
        if (isGone)
        {
            return;
        }

        setIsGone(true);
        sendViaChannel(Constants.dispose);
        server.queueClientHandlerForChannelChanges(this);
        server.clientWontConfirmCatchup(this,
            "Client disposed from server side.");
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

    public void tellWhatsHappening(String message)
    {
        sendToClient(Constants.tellWhatsHappening + sep + message);
    }

    public void tellMovementRoll(int roll)
    {
        sendToClient(Constants.tellMovementRoll + sep + roll);
    }

    public void syncOption(String optname, String value)
    {
        sendToClient(Constants.syncOption + sep + optname + sep + value);
    }

    public void updatePlayerInfo(List<String> infoStrings)
    {
        sendToClient(Constants.updatePlayerInfo + sep + Glob.glob(infoStrings));
    }

    public void setColor(PlayerColor color)
    {
        sendToClient(Constants.setColor + sep + color.getName());
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
        boolean teleported, EntrySide entrySide, CreatureType lastRecruit)
    {
        sendToClient(Constants.setLegionStatus + sep + legion.getMarkerId()
            + sep + moved + sep + teleported + sep + entrySide.ordinal() + sep
            + lastRecruit);
    }

    public void addCreature(Legion legion, CreatureType creature, String reason)
    {
        sendToClient(Constants.addCreature + sep + legion.getMarkerId() + sep
            + creature + sep + reason);
    }

    public void removeCreature(Legion legion, CreatureType creature,
        String reason)
    {
        sendToClient(Constants.removeCreature + sep + legion + sep + creature
            + sep + reason);
    }

    public void revealCreatures(Legion legion,
        final List<CreatureType> creatures, String reason)
    {
        sendToClient(Constants.revealCreatures + sep + legion.getMarkerId()
            + sep + Glob.glob(creatures) + sep + reason);
    }

    /** print the 'revealEngagagedCreature'-message,
     *   args: markerId, isAttacker, list of creature names
     * @param markerId legion marker name that is currently in battle
     * @param creatures List of creatures in this legion
     * @param isAttacker true for attacker, false for defender
     * @param reason why this was revealed
     * @author Towi, copied from revealCreatures
     */
    public void revealEngagedCreatures(final Legion legion,
        final List<CreatureType> creatures, final boolean isAttacker,
        String reason)
    {
        sendToClient(Constants.revealEngagedCreatures + sep
            + legion.getMarkerId() + sep + isAttacker + sep
            + Glob.glob(creatures) + sep + reason);
    }

    public void removeDeadBattleChits()
    {
        sendToClient(Constants.removeDeadBattleChits);
    }

    public void placeNewChit(String imageName, boolean inverted, int tag,
        BattleHex hex)
    {
        sendToClient(Constants.placeNewChit + sep + imageName + sep + inverted
            + sep + tag + sep + hex.getLabel());
    }

    public void tellReplay(boolean val, int maxTurn)
    {
        sendToClient(Constants.replayOngoing + sep + val + sep + maxTurn);
    }

    public void tellRedo(boolean val)
    {
        sendToClient(Constants.redoOngoing + sep + val);
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

    public String getSignonName()
    {
        return this.signonName;
    }

    public String getPlayerName()
    {
        if (this.playerName == null)
        {
            LOGGER.warning("CH.playerName still null, returning signOnName '" + signonName + "'");
        }
        return this.playerName;
    }

    public void createSummonAngel(Legion legion)
    {
        sendToClient(Constants.createSummonAngel + sep + legion.getMarkerId());
    }

    public void askAcquireAngel(Legion legion, List<CreatureType> recruits)
    {
        sendToClient(Constants.askAcquireAngel + sep + legion.getMarkerId()
            + sep + Glob.glob(recruits));
    }

    public void askChooseStrikePenalty(List<String> choices)
    {
        sendToClient(Constants.askChooseStrikePenalty + sep
            + Glob.glob(choices));
    }

    public void tellGameOver(String message, boolean disposeFollows)
    {
        sendToClient(Constants.tellGameOver + sep + message + sep
            + disposeFollows);
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

    public void tellSlowResults(int targetTag, int slowValue)
    {
        sendToClient(Constants.tellSlowResults + sep + targetTag + sep
            + slowValue);
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
        Player battleActivePlayer, BattlePhase battlePhase, Legion attacker,
        Legion defender)
    {
        sendToClient(Constants.initBattle + sep + hex.getLabel() + sep
            + battleTurnNumber + sep + battleActivePlayer.getName() + sep
            + battlePhase.ordinal() + sep + attacker.getMarkerId() + sep
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

    public void didRecruit(Legion legion, CreatureType recruit,
        CreatureType recruiter, int numRecruiters)
    {
        sendToClient(Constants.didRecruit + sep + legion.getMarkerId() + sep
            + recruit + sep + recruiter + sep + numRecruiters);
    }

    public void undidRecruit(Legion legion, CreatureType recruit)
    {
        sendToClient(Constants.undidRecruit + sep + legion + sep + recruit);
    }

    public void setupTurnState(Player activePlayer, int turnNumber)
    {
        commitPoint();
        sendToClient(Constants.setupTurnState + sep + activePlayer.getName()
            + sep + turnNumber);
    }

    public void setupSplit(Player activePlayer, int turnNumber)
    {
        commitPoint();
        sendToClient(Constants.setupSplit + sep + activePlayer.getName() + sep
            + turnNumber);
    }

    public void setupMove()
    {
        commitPoint();
        sendToClient(Constants.setupMove);
    }

    public void setupFight()
    {
        commitPoint();
        sendToClient(Constants.setupFight);
    }

    public void setupMuster()
    {
        commitPoint();
        sendToClient(Constants.setupMuster);
    }

    public void kickPhase()
    {
        sendToClient(Constants.kickPhase);
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

    public void setupBattleFight(BattlePhase battlePhase,
        Player battleActivePlayer)
    {
        sendToClient(Constants.setupBattleFight + sep + battlePhase.ordinal()
            + sep + battleActivePlayer.getName());
    }

    public void tellLegionLocation(Legion legion, MasterHex hex)
    {
        sendToClient(Constants.tellLegionLocation + sep + legion.getMarkerId()
            + sep + hex.getLabel());
    }

    public void tellBattleMove(int tag, BattleHex startingHex,
        BattleHex endingHex, boolean undo)
    {
        sendToClient(Constants.tellBattleMove + sep + tag + sep
            + startingHex.getLabel() + sep + endingHex.getLabel() + sep + undo);
    }

    public void didMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, EntrySide entrySide, boolean teleport,
        CreatureType teleportingLord, boolean splitLegionHasForcedMove)
    {
        sendToClient(Constants.didMove + sep + legion.getMarkerId() + sep
            + startingHex.getLabel() + sep + currentHex.getLabel() + sep
            + entrySide.getLabel() + sep + teleport + sep
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

    public void didSummon(Legion summoner, Legion donor, CreatureType summon)
    {
        sendToClient(Constants.didSummon + sep + summoner + sep + donor + sep
            + summon);
    }

    public void undidSplit(Legion splitoff, Legion survivor, int turn)
    {
        sendToClient(Constants.undidSplit + sep + splitoff.getMarkerId() + sep
            + survivor.getMarkerId() + sep + turn);
    }

    public void didSplit(MasterHex hex, Legion parent, Legion child,
        int childHeight, List<CreatureType> splitoffs, int turn)
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

    public void askPickColor(List<PlayerColor> colorsLeft)
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

    public void tellInitialGameInfo(String variantName,
        Collection<String> playerNames)
    {
        String allPlayerNames = Glob.glob(playerNames);
        sendToClient(Constants.gameInitInfo + sep + variantName + sep
            + allPlayerNames);
    }

    public void confirmWhenCaughtUp()
    {
        LOGGER.info("Sending request to confirm catchup to client "
            + playerName);
        sendToClient(Constants.askConfirmCatchUp);
    }

    public void serverConfirmsConnection()
    {
        LOGGER.info("Sending server connection confirmation to client "
            + playerName);
        sendToClient(Constants.serverConnectionOK);
    }

    public void pingRequest()
    {
        if (clientVersion >= IServer.CLIENT_VERSION_UNDERSTANDS_PING)
        {
            sendToClient(Constants.pingRequest);
        }
    }

    public void messageFromServer(String message)
    {
        sendToClient(Constants.messageFromServer + sep + message);
    }

    public void appendToConnectionLog(String s)
    {
        // dummy, only needed on client side
    }
}
