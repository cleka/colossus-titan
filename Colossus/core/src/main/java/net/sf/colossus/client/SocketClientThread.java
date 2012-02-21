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
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean goingDown = false;
    private boolean selfInterrupted = false;
    private boolean serverReceiveTimedout = false;

    /**
     * Those are stored at the moment only to be able to reconnect
     */
    private String host;
    private int port;
    private String playerName;
    private boolean remote;

    private final static String sep = Constants.protocolTermSeparator;

    private String reasonFail = null;
    private String initialLine = null;

    private String variantNameForInit;
    private Collection<String> preliminaryPlayerNames;

    private final Object isWaitingLock = new Object();
    private boolean isWaiting = false;

    private int ownMessageCounter = -1;

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

    public static SocketClientThread recreateConnection(
        SocketClientThread previousConnection) throws ConnectionInitException
    {
        String host = previousConnection.host;
        int port = previousConnection.port;
        String playerName = previousConnection.playerName;
        boolean remote = previousConnection.remote;

        LOGGER.info("SCT: trying recreateConnection to host " + host
            + " at port " + port + ".");

        SocketClientThread newConn = new SocketClientThread(host, port,
            playerName, remote);

        String reasonFail = newConn.getReasonFail();
        if (reasonFail != null)
        {
            LOGGER.warning("Reconnecting to server failed: " + reasonFail);
            previousConnection
                .appendToConnectionLog("SCT.recreateConnection failed! Reason: "
                    + reasonFail);
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

    SocketClientThread(String host, int port, String initialName,
        boolean isRemote)
    {
        super("Client " + initialName);

        this.host = host;
        this.port = port;
        this.playerName = initialName;
        this.remote = isRemote;

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
            in = new BufferedReader(new InputStreamReader(socket
                .getInputStream()));

            task = "Waiting for prompt";
            LOGGER.log(Level.FINEST, "Next: " + task);
            waitForPrompt();

            task = "Preparing PrintWriter";
            LOGGER.log(Level.FINEST, "Next: " + task);
            out = new PrintWriter(socket.getOutputStream(), true);

            task = "Sending signOn message";
            LOGGER.log(Level.FINEST, "Next: " + task);
            signOn(initialName, isRemote, IServer.CLIENT_VERSION, BuildInfo
                .getFullBuildInfoString());

            String line;
            task = "Waiting for signOn acknowledge";
            LOGGER.log(Level.FINEST, "Next: " + task);
            boolean signonOk = false;
            while (!signonOk)
            {
                line = in.readLine();
                if (line.startsWith("Ack: signOn"))
                {
                    LOGGER.fine("Got SignOn ACK: '" + line + "' - ok!");
                    signonOk = true;
                }
                else if (line.startsWith(Constants.nak))
                {
                    reasonFail = "SignOn rejected with NAK: " + line;
                    return;
                }
                else if (line.startsWith(Constants.log))
                {
                    // XXX TODO Handle better
                    // Earlier/Normally this would be forwarded to Client to
                    // give it to the logger / to LogWindow, but Client is not
                    // up / available yet.
                    LOGGER.info("ServerLog: " + line);
                }
                else
                {
                    LOGGER.warning("Ignoring unexpected line from server: '"
                        + line + "'");
                }
            }

            task = "Requesting GameInfo";
            LOGGER.log(Level.FINEST, "Next: " + task);
            requestGameInfo();

            task = "Waiting for GameInfo";
            LOGGER.log(Level.FINEST, "Next: " + task);

            boolean gotInfo = false;
            while (!gotInfo)
            {
                line = in.readLine();

                if (line.startsWith(Constants.gameInitInfo))
                {
                    LOGGER.fine("Got initGameInfo: '" + line + "' - ok!");
                    parseLine(line);
                    gotInfo = true;
                }
                else if (line.startsWith(Constants.nak))
                {
                    reasonFail = "GameInfo request got NAK: " + line;
                    return;
                }
                else if (line.startsWith(Constants.log))
                {
                    // XXX TODO Handle better
                    LOGGER.info("ServerLog: " + line);
                }
                else
                {
                    LOGGER.warning("SCT " + getNameMaybe() + " got '" + line
                        + "' but no use for it ...");
                }
            }

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

    public void waitForPrompt() throws SocketTimeoutException,
        SocketException, IOException
    {
        // Directly after connect we should get some first message
        // rather quickly... if not, probably Server has already enough
        // clients and we would hang in the queue...
        socket.setSoTimeout(5000);
        initialLine = in.readLine();
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
            clientThread.dispose();
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
                        LOGGER.log(Level.WARNING,
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
                    LOGGER.warning("Client '"
                        + getNameMaybe()
                        + "' got empty message from server?");
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
                + "processing in client " + getNameMaybe()
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
                    // clientThread.setClosedByServer();
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

        sendDisconnect();

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

    public int getMessageCounter()
    {
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
            LOGGER.finest("Received server ping request in SCT of client "
                + getName());
            replyToPing();
        }
        else if (method.equals(Constants.commitPoint))
        {
            int commitPointNr = Integer.parseInt(args.remove(0));
            int messageNr = Integer.parseInt(args.remove(0));

            if (ownMessageCounter == -1)
            {
                LOGGER.fine("...initializing own counter in commit point #"
                    + commitPointNr);
                ownMessageCounter = messageNr;
            }
            if (messageNr == ownMessageCounter)
            {
                LOGGER.finest("Client " + getNameMaybe()
                    + " received commit point "
                    + commitPointNr + " msg Nr " + messageNr
                    + " own counter " + ownMessageCounter);
            }
            else
            {
                LOGGER.warning("Client " + getNameMaybe()
                    + " received commit point "
                    + commitPointNr + " msg Nr " + messageNr
                    + " own counter " + ownMessageCounter);
                ownMessageCounter = messageNr;
            }
            sendToServer(Constants.confirmCommitPoint + sep
                + commitPointNr);

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

    private String getNameMaybe()
    {
        if (clientThread == null)
        {
            return getName();
        }
        else
        {
            return clientThread.getNameMaybe();
        }
    }

    private void sendToServer(String message)
    {
        if (socket != null)
        {
            LOGGER.finest("Message from SCT '" + getNameMaybe()
                + "' to server:" + message);
            out.println(message);
            clientThread.notifyUserIfGameIsPaused(message);
        }
        else
        {
            LOGGER.log(Level.SEVERE, "SCT (" + getName() + ")"
                + ": Attempt to send message '" + message
                + "' but the socket is closed and/or client alresady null??");
        }
    }

    // Setup method
    private void signOn(String loginName, boolean isRemote, int version,
        String buildInfo)
    {
        out.println(Constants.signOn + sep + loginName + sep + isRemote + sep
            + version + sep + buildInfo);
    }

    // Setup method
    private void requestGameInfo()
    {
        out.println(Constants.requestGameInfo);
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

    public void checkServerConnection()
    {
        sendToServer(Constants.checkConnection);
    }

    public void clientConfirmedCatchup()
    {
        sendToServer(Constants.catchupConfirmation);
    }

    public void joinGame(String playerName)
    {
        sendToServer(Constants.joinGame + sep + playerName);
    }

    public void requestSyncDelta(int msgNr)
    {
        sendToServer(Constants.requestSyncDelta + sep + msgNr);
    }

    public void replyToPing()
    {
        out.println(Constants.replyToPing);
        // sendToServer(Constants.replyToPing);
    }

    public void enforcedDisconnect()
    {
        if (socket == null)
        {
            LOGGER.info("Socket already null, can't fake disconnect...");
            return;
        }
        LOGGER.fine("In SCT " + getNameMaybe() + ": doing fake disconnect!");
        try
        {
            clientThread.appendToConnectionLog("Disconnecting...");
            LOGGER.fine("Shutting down output next...");
            socket.close();
            // socket.shutdownOutput();
            // socket.shutdownInput();
            LOGGER.fine("shutdownOutput done... and still alive :)");
            clientThread.appendToConnectionLog("Disconnected...");
        }
        catch (IOException e)
        {
            LOGGER
                .warning("Hm, did fake disconnect and this time got IOException?");
        }
    }

}
