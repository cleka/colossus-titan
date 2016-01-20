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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client.ConnectionInitException;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.actions.Recruitment;
import net.sf.colossus.game.actions.Summoning;
import net.sf.colossus.server.IServer;
import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.util.ErrorUtils;
import net.sf.colossus.util.Glob;
import net.sf.colossus.util.InstanceTracker;
import net.sf.colossus.util.Split;
import net.sf.colossus.util.SystemInfo;
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

    private ClientThread clientThread;

    // only needed for reconnect, to check whether there's something still
    // already received but not processed (= still in queue)
    private ClientThread disposedClientThread = null;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean goingDown = false;
    private boolean selfInterrupted = false;
    private boolean serverReceiveTimedout = false;

    /**
     * Those are stored at the moment only to be able to reconnect
     */
    private final String host;
    private final int port;
    private String playerName;
    private final boolean remote;
    private final boolean spectator;
    private final boolean internalSpectator;

    // The two below are only needed for debugging:
    // these here are related to whether replay/redo msgs were *received*;
    // those in ClientThread relate to "messages handled" state.
    private boolean replayReceived = false;
    private boolean redoReceived = false;

    private final boolean _MSG_TRACKING = false;

    private final static String sep = Constants.protocolTermSeparator;

    private String reasonFail = null;
    private String initialLine = null;

    private String variantNameForInit;
    private Collection<String> preliminaryPlayerNames;

    private final Object isWaitingLock = new Object();
    private boolean isWaiting = false;

    private int ownMessageCounter = -1;

    private int connectionId = -1;

    public static SocketClientThread createConnection(String host, int port,
        String initialName, boolean remote, boolean spectator)
        throws ConnectionInitException
    {
        LOGGER.info("SCT: trying recreateConnection to host " + host
            + " at port " + port + " for playerName " + initialName);

        SocketClientThread conn = new SocketClientThread(host, port,
            initialName, remote, spectator, -1);

        String reasonFail = conn.getReasonFail();
        if (reasonFail != null)
        {
            // If this failed here, it is usually a "could not connect"-problem
            // (wrong host or port or server not yet up).
            // In this case we just do cleanup and end.

            LOGGER.warning("Client startup failed: " + reasonFail);
            if (!Options.isStresstest())
            {
                String title = "Socket initialization failed!";
                ErrorUtils.showErrorDialog(null, title, reasonFail);
            }
            throw new ConnectionInitException(reasonFail);
        }

        return conn;
    }

    protected static SocketClientThread recreateConnection(
        IServerConnection prevConnection) throws ConnectionInitException
    {
        SocketClientThread previousConnection = (SocketClientThread)prevConnection;
        int prevConnId = previousConnection.getConnectionId();
        String host = previousConnection.host;
        int port = previousConnection.port;
        // Must already be the real name, not a "<bySomething>" any more
        // - server won't recognize it.
        String playerName = previousConnection.playerName;
        boolean remote = previousConnection.remote;
        boolean spectator = previousConnection.spectator;

        LOGGER.info("SCT: trying recreateConnection to host " + host
            + " at port " + port + " for playerName " + playerName
            + " witgh conectionId " + prevConnId);

        SocketClientThread newConn = new SocketClientThread(host, port,
            playerName, remote, spectator, prevConnId);
        String reasonFail = newConn.getReasonFail();
        if (reasonFail != null)
        {
            LOGGER.warning("Reconnecting to server failed: " + reasonFail);
            /*
            if (!Options.isStresstest())
            {
                String title = "Socket initialialization failed!";
                ErrorUtils.showErrorDialog(null, title, reasonFail);
            }
            */
            throw new ConnectionInitException(reasonFail);
        }

        return newConn;
    }

    /**
     *
     * @param host
     * @param port
     * @param initialName
     * @param isRemote
     * @param spectator
     * @param prevId     Id of connection to replace, or -1 if initial
     */
    SocketClientThread(String host, int port, String initialName,
        boolean isRemote, boolean spectator, int prevId)
    {
        super("SCT-" + initialName);

        this.host = host;
        this.port = port;
        // Note: for a reconnect case we are given already the "real" name,
        //       in first connect it will be replace as soon as server sends
        //       us the setName().
        this.playerName = initialName;
        this.remote = isRemote;
        this.spectator = spectator;
        this.internalSpectator = (spectator && playerName
            .equals(Constants.INTERNAL_DUMMY_CLIENT_NAME));
        this.connectionId = prevId;

        InstanceTracker.register(this, "SCT " + initialName);

        String task = "";

        try
        {
            task = "Creating Socket to connect to " + host + ":" + port;
            LOGGER.log(Level.FINEST, "Next: " + task);
            socket = new Socket(host, port);

            int receiveBufferSize = socket.getReceiveBufferSize();
            LOGGER.info("Client socket receive buffer size for Client "
                + initialName + " is " + receiveBufferSize);

            task = "Preparing BufferedReader";
            LOGGER.log(Level.FINEST, "Next: " + task);
            in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

            task = "Waiting for prompt";
            LOGGER.log(Level.FINEST, "Next: " + task);
            waitForPrompt();

            task = "Preparing PrintWriter";
            LOGGER.log(Level.FINEST, "Next: " + task);
            out = new PrintWriter(socket.getOutputStream(), true);

            task = "Sending signOn message";
            LOGGER.log(Level.FINEST, "Next: " + task);
            signOn(initialName, isRemote, IServer.CLIENT_VERSION,
                BuildInfo.getFullBuildInfoString(), spectator, connectionId);

            task = "Waiting for signOn acknowledge";
            LOGGER.log(Level.FINEST, "Next: " + task);
            reasonFail = waitForSignonOk();

            if (reasonFail == null)
            {
                task = "Sending System Info";
                LOGGER.log(Level.FINEST, "Next: " + task);
                sendSystemInfo();

                task = "Requesting GameInfo";
                LOGGER.log(Level.FINEST, "Next: " + task);
                requestGameInfo();

                task = "Waiting for GameInfo";
                LOGGER.log(Level.FINEST, "Next: " + task);
                reasonFail = waitForGameInfo();
            }
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

            LOGGER.log(Level.INFO, "ConnectException ('" + msg + "') "
                + "in SCT during " + task);
            reasonFail = "ConnectException ('" + e.getMessage() + "') "
                + "during " + task + possReason;

            return;
        }

        // e.g. readLine in initialRead()
        catch (SocketTimeoutException ste)
        {
            String msg = ste.getMessage();
            LOGGER.log(Level.INFO, "SocketTimeoutException ('" + msg + "') "
                + "in SCT during " + task);
            reasonFail = "Server not responding (could connect, "
                + "but didn't got any initial data within 5 seconds. "
                + "Probably the game has already as many clients as "
                + "it expects).";
            return;
        }

        // e.g. setSoTimeout calls in tryInitialRead
        catch (SocketException se)
        {
            String msg = se.getMessage();
            LOGGER.log(Level.SEVERE, "SocketException ('" + msg + "') "
                + "in SCT during " + task + ": ", se);
            reasonFail = "Exception during " + task + ": " + se.toString()
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

    private String readOneLine() throws IOException
    {
        String line = in.readLine();
        showDebugOutput(line);
        return line;
    }

    private boolean msg_tracking()
    {
        return _MSG_TRACKING;
    }

    private void showDebugOutput(String line)
    {
        if (line == null || !msg_tracking())
        {
            return;
        }

        if (line.startsWith(Constants.replayOngoing + " ~ false"))
        {
            replayReceived = false;
        }
        if (line.startsWith(Constants.redoOngoing + " ~ false"))
        {
            redoReceived = false;
        }

        boolean show = false;
        if (playerName.equals("remote") || playerName.equals("spectator"))
        {
            show = true;
        }
        if (internalSpectator)
        {
            show = false;
        }

        if (line.startsWith("setOption ~ ViewMode"))
        {
            // show this as the only one
        }
        else if (line.startsWith(Constants.updateCreatureCount)
            // || line.startsWith(Constants.setLegionStatus)
            // || line.startsWith(Constants.tellLegionLocation)
            || line.startsWith(Constants.serverConnectionOK)
            || line.startsWith(Constants.relayBackProcessedMsg)
            || line.startsWith(Constants.relayBackReceivedMsg)
            // || line.startsWith(Constants.)
            || line.startsWith(Constants.pingRequest)
            || line.startsWith(Constants.syncOption))
        {
            show = false;
        }

        // Logging/tracking of received messages for development purposes
        if (show)
        {
            if (line.startsWith(Constants.setupTurnState))
            {
                System.out.println("");
            }

            String indent = (redoReceived ? "  " : "")
                + (replayReceived ? "  " : "");

            String printLine = line;
            int _MAXLEN = 120;
            int len = line.length();
            if (len > _MAXLEN)
            {
                printLine = line.substring(0, _MAXLEN) + "...";
            }
            System.out.println(indent + "<<<" + printLine);
        }

        if (line.startsWith(Constants.replayOngoing + " ~ true"))
        {
            replayReceived = true;
        }
        if (line.startsWith(Constants.redoOngoing + " ~ true"))
        {
            redoReceived = true;
        }
    }

    public void waitForPrompt() throws SocketTimeoutException,
        SocketException, IOException
    {
        // Directly after connect we should get some first message
        // rather quickly... if not, probably Server has already enough
        // clients and we would hang in the queue...
        socket.setSoTimeout(5000);
        initialLine = readOneLine();
        if (initialLine.startsWith("SignOn:"))
        {
            LOGGER.fine("Got prompt: '" + initialLine + "' - ok!");
            initialLine = null;
        }
        // ... but after we got first data, during game it might take
        // unpredictable time before next thing comes, so reset it to 0
        //  ( = wait forever).
        socket.setSoTimeout(0);

        return;
    }

    private String waitForSignonOk() throws IOException
    {
        String line;
        boolean signonOk = false;
        while (!signonOk)
        {
            line = readOneLine();
            if (line.startsWith("Ack: signOn"))
            {
                LOGGER.fine("Got SignOn ACK: '" + line + "' - ok!");
                signonOk = true;
            }
            else if (line.startsWith(Constants.setConnectionId))
            {
                // sets the connection id we get assigned from server.
                parseLine(line);
            }
            else if (line.startsWith(Constants.nak))
            {
                return "SignOn rejected with NAK: " + line;
            }
            else if (line.startsWith(Constants.log))
            {
                // XXX TODO Handle better
                // Earlier/Normally this would be forwarded to Client to
                // give it to the logger / to LogWindow, but Client is not
                // up / available yet.
                LOGGER.info("ServerLog: " + line);
            }
            else if (line.startsWith(Constants.pingRequest))
            {
                // silently ignore
            }
            else
            {
                LOGGER.warning("Ignoring unexpected line from server: '"
                    + line + "'");
            }
        }

        // Everything is ok:
        return null;
    }

    private String waitForGameInfo() throws IOException
    {
        String line;

        boolean gotInfo = false;
        while (!gotInfo)
        {
            line = readOneLine();

            if (line.startsWith(Constants.gameInitInfo))
            {
                LOGGER.fine("Got initGameInfo: '" + line + "' - ok!");
                parseLine(line);
                gotInfo = true;
            }
            else if (line.startsWith(Constants.nak))
            {
                return "GameInfo request got NAK: " + line;
            }
            else if (line.startsWith(Constants.log))
            {
                // XXX TODO Handle better
                LOGGER.info("ServerLog: " + line);
            }
            else if (line.startsWith(Constants.pingRequest))
            {
                // silently ignore
            }
            else
            {
                LOGGER.warning(getPrintName() + ": got '" + line
                    + "' but no use for it ...");
            }
        }

        // Everything is ok:
        return null;
    }

    public String getReasonFail()
    {
        return reasonFail;
    }

    public void appendToConnectionLog(String s)
    {
        if (clientThread != null)
        {
            clientThread.appendToConnectionLog(s);
        }
    }

    public String getVariantNameForInit()
    {
        return variantNameForInit;
    }

    public Collection<String> getPreliminaryPlayerNames()
    {
        return Collections.unmodifiableCollection(this.preliminaryPlayerNames);
    }

    public IServer getIServer()
    {
        return this;
    }

    public void setClient(Client client)
    {
        this.clientThread = new ClientThread(client);
        clientThread.start();
    }

    public int getDisposedQueueLen()
    {
        if (disposedClientThread != null)
        {
            return disposedClientThread.getQueueLen();
        }
        else
        {
            LOGGER.warning(getPrintName()
                + ": can't ask null disposedClientThread for it's queueLen!");
            return 0;
        }
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
            clientThread = null;
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

        if (clientThread != null)
        {
            disposedClientThread = clientThread;
            clientThread.disposeQueue();
            if (!abandoned)
            {
                clientThread.disposeClient();
            }
            clientThread = null;
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
                        LOGGER.finest("SCT of client '" + getName()
                            + "' got message from server: " + fromServer);
                        parseLine(fromServer);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.log(
                            Level.WARNING,
                            "\n++++++\nSCT SocketClientThread " + getName()
                                + ", parseLine(): got Exception "
                                + ex.toString() + "\n" + ex.getMessage()
                                + "\nline=" + fromServer, ex);
                    }

                    // increment it now, so that during parsing of next it has right value.
                    // It's not safe to do it inside parseLine after processing, because it
                    // would be omitted if an exception occurs.
                    if (ownMessageCounter != -1)
                    {
                        ownMessageCounter++;
                    }
                }
                else
                {
                    LOGGER.warning(getPrintName()
                        + ": got empty message from server?");
                }
            }

            if (fromServer == null)
            {
                LOGGER.info("** SCT after loop, got null line from Server!");
            }
            LOGGER.log(Level.FINE,
                "Clean end of SocketClientThread while loop");
        }

        // just in case...
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING,
                "\n^^^^^^^^^^\nSCT.run() major try/catch???\n", e);
            setWaiting(false);
        }

        // catch this (e.g. out of memory error), so that the user know that
        // something is seriously wrong, ...
        catch (VirtualMachineError vme)
        {
            String message = "Woooah! A Fatal JVM error was caught while "
                + "processing in " + getPrintName()
                + " the input line:\n    === " + fromServer + " ===\n"
                + "\nStack trace:\n" + ErrorUtils.makeStackTraceString(vme)
                + "\n\nGame might be unstable or hang from now on...";
            LOGGER.severe(message);
            ErrorUtils.showExceptionDialog(null, message, "Fatal JVM Error!",
                true);
            setWaiting(false);
            // ... but throw again because all hope is lost anyway, and e.g.
            // assertions and so on (stresstest) should proceed to outside
            // as before.
            throw (vme);
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
                line = readOneLine();
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
                    // clientThread.setClosedByServer();
                    LOGGER
                        .log(Level.WARNING,
                            "SCT SocketClientThread " + getName()
                                + ": got SocketException " + ex.toString());
                }
                goingDown = true;
            }

            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "SCT SocketClientThread " + getName()
                    + ", got an IOException ", ex);
                goingDown = true;
            }
            catch (Exception any)
            {
                LOGGER.log(Level.SEVERE, "SCT SocketClientThread " + getName()
                    + ", got Any Exception ", any);
            }
        }
        setWaiting(false);
        return line;
    }

    public boolean isAlreadyDown()
    {
        return (clientThread == null);
    }

    private void cleanupSocket()
    {
        try
        {
            if (socket != null && !socket.isClosed())
            {
                socket.close();
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
        finally
        {
            socket = null;
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

    /**
     * Client originates the dispose:
     * If done because all is over, player chose close etc, send also a
     * disconnect so that server knows client is "gone". If done because
     * of actually or suspected "connection dead/problems", just shut down
     * the SCT peacefully, do not inform server, client might want to
     * reconnect later with a new SCT / ClientThread pair.
     * @param sendConnect  If true, sends a disconnect message to server
     */
    public void stopSocketClientThread(boolean sendConnect)
    {
        if (goingDown)
        {
            return;
        }
        goingDown = true;

        if (sendConnect)
        {
            sendDisconnect();
        }

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

    // Client told us we are not relevant any more; must not confirm any
    // commit point to server any more, and when SCT thread ends, do not
    // call the "dispose whole client" functionality.
    private boolean abandoned = false;

    public int abandonAndGetMessageCounter()
    {
        if (abandoned)
        {
            return -2;
        }
        abandoned = true;
        stopSocketClientThread(false);
        return ownMessageCounter;
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
        if (method.equals(Constants.pingRequest))
        {
            long requestReceived = new Date().getTime();
            int requestNr = -1;
            long requestSent = -1L;

            if (args.size() >= 2)
            {
                requestNr = Integer.parseInt(args.remove(0));
                requestSent = Long.parseLong(args.remove(0));
            }
            LOGGER.fine("SCT " + getName() + " received ping request #"
                + requestNr + " from server");

            replyToPing(requestNr, requestSent, requestReceived);
            /*
            if (getName().equals("SCT-clemens"))
            {
                if (requestNr >= 3 && requestNr < 6)
                {
                    System.out.println("Purposefully not sending ping reply for request #"
                        + requestNr);
                }
                else
                {
                    replyToPing(requestNr, requestSent, requestReceived);
                }
            }
            else
            {
                replyToPing(requestNr, requestSent, requestReceived);
            }
            */

            if (clientThread != null
                && clientThread.isEngagementStartupOngoing())
            {
                String itemsText = "";
                int len = clientThread.getQueueLen();
                if (len > 0)
                {
                    itemsText = "; items: "
                        + clientThread.getQueueContentSummary();
                }
                logMsgToServer("I", "PingRequest #" + requestNr
                    + ": ClientThread (" + playerName + ") queue length is "
                    + len + itemsText);
            }
        }
        else if (method.equals(Constants.commitPoint))
        {
            int commitPointNr = Integer.parseInt(args.remove(0));
            int messageNr = Integer.parseInt(args.remove(0));

            if (ownMessageCounter == -1)
            {
                LOGGER.fine("SCT " + getName()
                    + ": initializing own counter in commit point #"
                    + commitPointNr);
                ownMessageCounter = messageNr;
            }
            if (messageNr == ownMessageCounter)
            {
                LOGGER.finest(getPrintName() + ": received commit point "
                    + commitPointNr + " msg Nr " + messageNr + " own counter "
                    + ownMessageCounter);
            }
            else
            {
                LOGGER.warning(getPrintName() + ": received commit point "
                    + commitPointNr + " msg Nr " + messageNr
                    + ", but own counter is " + ownMessageCounter
                    + " -adjusting.");
                ownMessageCounter = messageNr;
            }
            if (abandoned)
            {
                LOGGER.warning(getPrintName() + " already "
                    + "abandoned; suppressing confirmCommitPoint for CP# "
                    + commitPointNr);
            }
            else
            {
                sendToServer(Constants.confirmCommitPoint + sep
                    + commitPointNr);
            }
        }
        else if (method.equals(Constants.gameInitInfo))
        {
            this.variantNameForInit = args.remove(0);
            String nameList = args.remove(0);
            this.preliminaryPlayerNames = Split.split(Glob.sep, nameList);
        }

        else if (method.equals(Constants.dispose))
        {
            clientThread.setClosedByServer();
            goingDown = true;
        }

        else if (method.equals(Constants.relayedPeerRequest))
        {
            String requestingClientName = args.get(0);
            int queueLen = clientThread.getQueueLen();
            peerRequestReceived(requestingClientName, queueLen);
            // in this one, both the socket reading thread and the actual
            // client are supposed to respond.
            clientThread.enqueue(method, args);
        }

        else if (method.equals(Constants.setConnectionId))
        {
            this.connectionId = Integer.parseInt(args.remove(0));
            LOGGER.finer("Server told me my connection id " + connectionId);
        }

        else if (method.equals(Constants.nak) && args.size() > 0
            && args.get(0) != null && args.get(0).equals("SignOn"))
        {
            // All other nak's are handled by clientThread/client!
            String reason = args.remove(0);
            String message = args.remove(0);
            goingDown = true;
            String title = "Joining game (" + reason + ") failed!";
            ErrorUtils.showErrorDialog(null, title, message);
        }

        else
        {
            clientThread.enqueue(method, args);
        }
    }

    private String getPrintName()
    {
        // at the moment, initially "SCT-<initialName>", e.g. SCT-<byName>,
        // but as soon as server "fixed" our name it is SCT-<realPlayerName>
        // e.g. SCT-katzer .
        return getName();
    }

    private int getConnectionId()
    {
        return this.connectionId;
    }

    private void sendToServer(String message)
    {
        if (socket != null)
        {
            LOGGER.finer("Client '" + getPrintName() + "' sends to server: "
                + message);
            out.println(message);
            clientThread.notifyUserIfGameIsPaused(message);
        }
        else if (message.startsWith(Constants.replyToPing))
        {
            // silently ignore;
            // replyToPing method(s) write directly to socket.
        }
        else
        {
            if (clientThread != null)
            {
                clientThread.notifyThatNotConnected();
            }
            else if (disposedClientThread != null)
            {
                disposedClientThread.notifyThatNotConnected();
            }
            else
            {
                LOGGER.log(Level.WARNING, getPrintName()
                    + ": Attempt to send message '" + message
                    + "' but the socket is closed and/or client already null"
                    + " and cant't inform any clientThread?");

            }

        }
    }

    // Setup method
    private void signOn(String loginName, boolean isRemote, int version,
        String buildInfo, boolean spectator, int prevConnId)
    {
        out.println(Constants.signOn + sep + loginName + sep + isRemote + sep
            + version + sep + buildInfo + sep + spectator + sep + prevConnId);
    }

    private void sendSystemInfo()
    {
        out.println(Constants.systemInfo + sep + SystemInfo.getOsInfo() + sep
            + SystemInfo.getFullJavaInfo());
    }

    // Setup method
    private void requestGameInfo()
    {
        out.println(Constants.requestGameInfo);
    }

    /* Server tells client changed name, Client calls us to keep in sync */
    public void updatePlayerName(String playerName)
    {
        // was initialized to initialName, which might have been "<bySomething>"
        this.playerName = playerName;

        // Set the thread name
        setName("SCT-" + playerName);
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

    public void doSummon(Summoning event)
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

    public void doRecruit(Recruitment event)
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

    public void requestExtraRoll()
    {
        sendToServer(Constants.requestExtraRoll);
    }

    public void extraRollResponse(boolean approved, int requestId)
    {
        sendToServer(Constants.extraRollResponse + sep + approved + sep + requestId);
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

    public void sendDisconnect()
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

    public void requestToSuspendGame(boolean save)
    {
        sendToServer(Constants.suspendGame);
    }

    public void suspendResponse(boolean approved)
    {
        sendToServer(Constants.suspendResponse + sep + approved);
    }

    public void checkServerConnection()
    {
        sendToServer(Constants.checkConnection);
    }

    public void checkAllConnections(String requestingClientName)
    {
        sendToServer(Constants.checkAllConnections + sep
            + requestingClientName);
    }

    public void clientConfirmedCatchup()
    {
        sendToServer(Constants.catchupConfirmation);
    }

    public void logMsgToServer(String severity, String message)
    {
        sendToServer(Constants.logMsgToServer + sep + severity + sep + message);
    }

    public void cheatModeDestroyLegion(Legion legion)
    {
        sendToServer(Constants.cheatModeDestroyLegion + sep
            + legion.getMarkerId());
    }

    public void joinGame(String playerName)
    {
        sendToServer(Constants.joinGame + sep + playerName);
    }

    public void watchGame()
    {
        sendToServer(Constants.watchGame);
    }

    public void requestSyncDelta(int msgNr, int syncCounter)
    {
        sendToServer(Constants.requestSyncDelta + sep + msgNr + sep
            + syncCounter);
    }

    public void peerRequestReceived(String requestingClientName, int queueLen)
    {
        sendToServer(Constants.peerRequestReceived + sep
            + requestingClientName + sep + queueLen);
    }

    public void peerRequestProcessed(String requestingClientName)
    {
        sendToServer(Constants.peerRequestProcessed + sep
            + requestingClientName);
    }

    public void replyToPing(int requestNr, long requestSent,
        long requestReceived)
    {
        out.println(Constants.replyToPing + sep + requestNr + sep
            + requestSent + sep + requestReceived);
        // sendToServer(Constants.replyToPing);
    }

    public void enforcedConnectionException()
    {
        if (socket == null)
        {
            LOGGER.info(getPrintName()
                + ": socket already null, can't fake disconnect...");
            return;
        }
        LOGGER.fine(getPrintName() + ": doing enforced disconnect!");
        try
        {
            appendToConnectionLog("Disconnecting (closing socket)...");
            LOGGER.fine("Disconnecting (closing socket)...");
            socket.close();
            // TODO we should set it here also to null,
            // but needs some more testing does it break anything
            // socket = null;
            // Other ways than close()...
            // socket.shutdownOutput();
            // socket.shutdownInput();
        }
        catch (IOException e)
        {
            LOGGER.warning(getPrintName()
                + ": hm, did fake disconnect and this time got IOException?");
        }
    }
}
