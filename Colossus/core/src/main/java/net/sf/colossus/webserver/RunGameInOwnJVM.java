package net.sf.colossus.webserver;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.common.Options;
import net.sf.colossus.server.INotifyWebServer;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.GameInfo.GameState;
import net.sf.colossus.webcommon.IGameRunner;
import net.sf.colossus.webcommon.IRunWebServer;
import net.sf.colossus.webcommon.User;


/**
 *  This class runs (starts and supervises) a Game on the Game Server
 *  (as opposed to: on the User's PC).
 *  It finds and reserves a port for it, starts it in a separate process
 *  and when the process terminates, join()s it and releases the port.
 *
 *  If the game is run on a user's PC, the class RunGameInSameJVM will be
 *  used.
 *
 *  @author Clemens Katzer
 */
public class RunGameInOwnJVM extends Thread implements IGameRunner
{
    private static final Logger LOGGER = Logger
        .getLogger(RunGameInOwnJVM.class.getName());

    private int hostingPort;
    private String hostingHost;

    private final IRunWebServer server;
    private final WebServerOptions options;
    private final GameInfo gi;
    private final String gameId;

    private String workFilesBaseDir;
    private String statisticsBaseDir;

    private String template;
    private String javaCommand;
    private String colossusJar;

    private File flagFile;
    private File suspendedFlagfile;

    private boolean alreadyStarted;
    private String reasonStartFailed;

    public RunGameInOwnJVM(IRunWebServer server, WebServerOptions options,
        GameInfo gi)
    {
        this.server = server;
        this.options = options;
        this.gi = gi;
        this.gameId = gi.getGameId();
        this.alreadyStarted = false;
        gi.setGameRunner(this);
    }

    public boolean makeRunningGame()
    {
        workFilesBaseDir = options
            .getStringOption(WebServerConstants.optWorkFilesBaseDir);
        statisticsBaseDir = options
            .getStringOption(WebServerConstants.optStatisticsBaseDir);
        template = options
            .getStringOption(WebServerConstants.optLogPropTemplate);
        if (gi.wantsDetailedLogging())
        {
            template += ".finest";
        }
        javaCommand = options
            .getStringOption(WebServerConstants.optJavaCommand);
        colossusJar = options
            .getStringOption(WebServerConstants.optColossusJar);

        LOGGER.fine("Calling getFreePort for game " + gi.getGameId());

        int port = server.getPortProvider().getFreePort(gi);
        if (port == -1)
        {
            reasonStartFailed = "No free ports!!";
            return false;
        }

        hostingPort = port;
        hostingHost = gi.getHostingHost();

        gi.setPort(port);
        LOGGER.fine("Using port " + port + " for game " + gi.getGameId());

        this.setName("Game at port " + hostingPort);
        return true;
    }

    public int getHostingPort()
    {
        return hostingPort;
    }

    public String getHostingHost()
    {
        return hostingHost;
    }

    public String getReasonStartFailed()
    {
        return reasonStartFailed;
    }

    public boolean tryToStart()
    {
        synchronized (this)
        {
            if (alreadyStarted)
            {
                return false;
            }
            else
            {
                alreadyStarted = true;
                this.start();
                return true;
            }
        }
    }

    @Override
    public void run()
    {
        runInOwnJVM();
    }

    private void runInOwnJVM()
    {
        // Figure out the ??00-??99 part of the path:
        int intGameId = Integer.parseInt(gameId);
        int hundreds = intGameId - (intGameId % 100);
        Integer h00 = Integer.valueOf(hundreds);
        Integer h99 = Integer.valueOf(hundreds + 99);
        String dir0099Part = String.format("%04d-%04d", h00, h99);

        // E.g. game 6789 will be stored as workFilesBaseDir/6700-6799/6789
        String dirPath = workFilesBaseDir + File.separator + dir0099Part;

        // force to be at least 4 digits, i.e. 0049 for game 49:
        String gameId4s = String.format("%04d", Integer.valueOf(intGameId));
        File gameDir = new File(dirPath, gameId4s);
        gameDir.mkdirs();

        String statDirPath = statisticsBaseDir + File.separator + dir0099Part;
        File statDir = new File(statDirPath, gameId4s);
        statDir.mkdirs();
        LOGGER.finest("Creating dir " + statDir.toString());
        File diceStatFile = new File(statDir, "dice-statistics.txt");

        String flagFileName = "Game." + gameId + ".running.flag";

        this.flagFile = new File(gameDir, flagFileName);
        if (flagFile.exists())
        {
            flagFile.delete();
        }

        this.suspendedFlagfile = new File(gameDir, flagFileName + ".suspended");
        if (this.suspendedFlagfile.exists())
        {
            suspendedFlagfile.delete();
        }

        File logPropFile = new File(gameDir, "logging.properties");
        File logPropTemplate = new File(template);
        boolean propFileOk = createLoggingPropertiesFromTemplate(
            logPropTemplate, logPropFile);

        // Stores data from GameInfo into an options object and saves
        // the options to file on disk in the special game directory.
        createServerCfgFile(gameDir, diceStatFile);

        Runtime rt = Runtime.getRuntime();

        String loggingFileArg = propFileOk ? " -Djava.util.logging.config.file="
            + logPropFile
            : "";

        String randomDotOrgArg = "";
        String prop;
        prop = System.getProperty("net.sf.colossus.randomDotOrgDirectory");
        if (prop != null && !prop.equals(""))
        {
            randomDotOrgArg = " -Dnet.sf.colossus.randomDotOrgDirectory="
                + prop;
        }

        String loadOptionString = "";
        String loadFilename = this.gi.getResumeFromFilename();
        if (loadFilename != null)
        {
            loadOptionString = " --latest";
        }

        String command = javaCommand + loggingFileArg + randomDotOrgArg
            + " -Duser.home="
            + gameDir + " -jar " + colossusJar + " -p " + hostingPort
            + " -g --flagfile " + flagFileName + loadOptionString;

        try
        {
            Process p = rt.exec(command, null, gameDir);
            p.getOutputStream().close();
            NullDumper ndout = new NullDumper(p, false, p.getInputStream(),
                gameId + "_OUT: ").start();
            NullDumper nderr = new NullDumper(p, false, p.getErrorStream(),
                gameId + "_ERR: ").start();

            superviseGameStartup();

            waitForGameShutdown(p, ndout, nderr);

        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Executing\n  " + command
                + "\ndid throw exception", e);
        }
    }

    private boolean createServerCfgFile(File gameDir, File diceStatisticsFile)
    {
        boolean ok = true;

        String gameDirPath = gameDir.getPath();
        Options gameOptions = new Options("server", gameDirPath
            + "/.colossus/", false);

        // No local player if run on Webserver...
        String localPlayerName = null;
        boolean noAIs = true;
        gi.storeToOptionsObject(gameOptions, localPlayerName, noAIs);

        gameOptions.setOption(Options.autoQuit, true);
        gameOptions.setOption(Options.keepAccepting, true);
        String statFileName = diceStatisticsFile.toString();
        LOGGER.finest("Statisticsfilename: " + statFileName);
        gameOptions.setOption(Options.diceStatisticsFile, statFileName);

        gameOptions.saveOptions();

        return ok;
    }

    private boolean createLoggingPropertiesFromTemplate(File logPropTemplate,
        File logPropFile)
    {
        boolean ok = true;
        String patternLine = "java.util.logging.FileHandler.pattern=";
        try
        {
            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(logPropTemplate)));

            PrintWriter out = new PrintWriter(
                new FileOutputStream(logPropFile));

            while ((line = in.readLine()) != null)
            {
                if (line.startsWith(patternLine))
                {
                    // String dirSep = File.separator;
                    // if there is no path, it seems it uses current working
                    // directory.
                    // When we put path there, we would have to hard-coded
                    // replace the \ of a Windows directory to /'es,
                    // because as it looks the java logger only accepts those,
                    // and uses backslashes just as quote character...
                    line = patternLine + "Colossus%g.log";
                }
                out.println(line);
            }

            in.close();
            out.close();
        }
        catch (FileNotFoundException e)
        {
            ok = false;
        }
        catch (IOException e)
        {
            ok = false;
        }

        return ok;
    }

    private void superviseGameStartup()
    {
        // ACTIVATED
        LOGGER.log(Level.FINEST,
            "Seems starting game process went ok, sending gameStartsSoon "
                + "to enrolled players: " + gi.getPlayerListAsString());
        server.tellEnrolledGameStartsSoon(gi);

        int timeout = 30; // seconds
        boolean up = waitUntilReadyToAcceptClients(timeout);
        if (up)
        {
            LOGGER.log(Level.FINEST,
                "Game is ready to accept clients - sending gameStartsNow.");
            // READY_TO_CONNECT

            int port = gi.getPort();
            // if run on GameServer, null.
            // TODO: real remote host there, when runs on players PC
            String hostingHost = null;

            server.tellEnrolledGameStartsNow(gi, hostingHost, port);
            server.allTellGameInfo(gi);

            boolean ok = waitUntilGameStartedSuccessfully(30);
            if (ok)
            {
                // RUNNING
                LOGGER.log(Level.FINEST, "Game " + gameId
                    + " started ok - fine!");
                server.gameStarted(gi);
            }
            else
            {
                LOGGER.log(Level.WARNING, "Game " + gameId + ": "
                    + reasonStartFailed);
                server.informAllEnrolledAbout(gi, reasonStartFailed);
            }
        }
        else
        {
            reasonStartFailed = "did not reach READY_TO_CONNECT state!";
            LOGGER.log(Level.SEVERE, "Game " + gameId + " "
                + reasonStartFailed);
        }
    }

    private void waitForGameShutdown(Process p, NullDumper ndout,
        NullDumper nderr)
    {
        try
        {
            LOGGER.log(Level.FINEST, "Waiting for process of game " + gameId
                + " to reap it.");
            int exitCode = p.waitFor();
            if (exitCode != 0)
            {
                LOGGER.log(Level.WARNING, "Non-zero exit code (" + exitCode
                    + ") of process for game " + gameId);
            }
            else
            {
                LOGGER.log(Level.FINEST, "Exit code of process for game "
                    + gameId + " is " + exitCode + " - ok!");
            }
        }
        catch (Exception e)
        {
            String reason = "Exception " + e.getMessage()
                + " during waitForGameShutdown game " + gameId;
            LOGGER.log(Level.WARNING, reason, e);
            server.gameFailed(gi, reason);
        }
        finally
        {
            ndout.done();
            nderr.done();
        }

        if (this.suspendedFlagfile.exists())
        {
            String message = "Game " + gameId + " was suspended.";
            LOGGER.log(Level.INFO, message);
            gi.setState(GameState.SUSPENDED);
        }
        else if (flagFile.exists() && reasonStartFailed != null)
        {
            String message = "Game "
                + gameId
                + " ended but flagfile "
                + flagFile.toString()
                + " does still exist? "
                + "Well, start failed, so it's not that surprising. Renaming it...";
            LOGGER.log(Level.INFO, message);
            flagFile.renameTo(new File(flagFile.getParent(),
                "flagfile.startFailed"));
        }
        else if (flagFile.exists())
        {
            LOGGER.log(Level.WARNING, "Game " + gameId
                + " ended but flagfile " + flagFile.toString()
                + " does still exist...? Renaming it...");
            flagFile.renameTo(new File(flagFile.getParent(), "flagfile.away"));
        }
        else
        {
            LOGGER.log(Level.FINEST, "Game " + gameId + " ended and flagfile "
                + flagFile.toString() + " is gone. Fine!");
        }

        LOGGER.info("Before unregister game " + gameId);
        server.unregisterGame(gi, hostingPort);
    }

    /*
     * Checks whether the game is already started "far enough", i.e.
     * that the serverSocket is ready to accept clients.
     * Game started with --webserver flag will create the flag file
     * after it created the socket.
     */

    private boolean isSocketUp()
    {
        if (flagFile == null)
        {
            return false;
        }
        if (flagFile.exists())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /* Waits until socket is up, i.e. game is ready to accept clients.
     */
    public boolean waitUntilReadyToAcceptClients(int timeout)
    {
        boolean up = false;

        for (int i = 0; !up && i < timeout; i++)
        {
            up = isSocketUp();
            if (!up)
            {
                sleepFor(1000);
            }
            i++;
        }
        return up;
    }

    private String waitForLine(BufferedReader in, int checkInterval)
    {
        String line = null;

        try
        {
            line = in.readLine();
            if (line == null)
            {
                sleepFor(checkInterval);
            }
        }
        catch (IOException e1)
        {
            LOGGER
                .log(Level.SEVERE, "during wait for line: IOException: ", e1);
        }
        catch (RuntimeException e2)
        {
            LOGGER.log(Level.SEVERE,
                "during wait for line: RuntimeException: ", e2);
        }
        catch (Exception e3)
        {
            LOGGER.log(Level.SEVERE,
                "during wait for line: Whatever Exception: ", e3);
        }

        return line;
    }

    private String getMissingPlayers(List<String> names)
    {
        StringBuilder missing = new StringBuilder();
        for (User u : gi.getPlayers())
        {
            String uName = u.getName();
            if (!names.contains(uName))
            {
                if (missing.length() > 0)
                {
                    missing.append(", ");
                }
                missing.append(uName);
            }
        }
        return missing.toString();
    }

    public String listAsString(List<String> names)
    {
        StringBuilder namesSB = new StringBuilder();
        for (String oneName : names)
        {
            if (namesSB.length() > 0)
            {
                namesSB.append(", ");
            }
            namesSB.append(oneName);
        }
        return namesSB.toString();
    }

    public boolean waitUntilGameStartedSuccessfully(int timeout)
    {
        reasonStartFailed = null;

        BufferedReader in = null;
        try
        {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(
                flagFile)));
        }
        catch (FileNotFoundException ef)
        {
            LOGGER.log(Level.SEVERE,
                "while waiting until game started successfully: ", ef);
        }

        if (in == null)
        {
            reasonStartFailed = "could not open flagfile for reading!!";
            return false;
        }

        int connected = 0;
        int checkInterval = 1000; // every second
        String line;

        List<String> names = new LinkedList<String>();

        boolean done = false;
        for (int i = 0; !done && i < timeout;)
        {
            String name = null;
            line = waitForLine(in, checkInterval);
            LOGGER.info("GOT: " + line);
            if (line == null)
            {
                // Didn't get anything => readLine timeout hit
                i++;
            }
            else if (line.startsWith("Local client connected: "))
            {
                name = line.substring(24);
                connected++;
            }
            else if (line.startsWith("Remote client connected: "))
            {
                name = line.substring(25);
                connected++;
            }
            else if (line.startsWith(INotifyWebServer.ALL_CLIENTS_CONNECTED))
            {
                done = true;
            }
            else if (line.startsWith(INotifyWebServer.GAME_STARTUP_COMPLETED))
            {
                done = true;
            }

            // TODO: for now there is only one possible reason, handle better!
            else if (line.startsWith(INotifyWebServer.GAME_STARTUP_FAILED))
            {
                reasonStartFailed = "Game start reported error!";
                done = true;
            }

            if (connected >= gi.getPlayers().size())
            {
                done = true;
            }

            if (name != null)
            {
                names.add(name);
                String message = "Player " + name + " connected to game "
                    + gi.getGameId();
                server.informAllEnrolledAbout(gi, message);
            }
        }

        if (reasonStartFailed == null && !done)
        {
            reasonStartFailed = "Start timed out, done not set? ";
        }

        if (reasonStartFailed != null)
        {
            String connectedPlayers = listAsString(names);
            String missingPlayers = getMissingPlayers(names);
            reasonStartFailed = reasonStartFailed + " Connected: "
                + connectedPlayers + "; not connected: " + missingPlayers;
        }

        try
        {
            in.close();
        }
        catch (IOException e)
        {
            // ignore
        }
        return (reasonStartFailed == null);
    }

    private void sleepFor(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            LOGGER.log(Level.FINEST,
                "sleepFor: InterruptException caught... ignoring it...");
        }
    }

    /**
     * NullDumper is a dummy reader that just consumes all the output
     * produced by a Game's process - similar to /dev/null. That is needed
     * because we have to take care to read all what comes on the
     * Game's processes stdout and stderr, otherwise the game would block
     * at some point.
     *
     * If the boolean argument toNull to constructor is false, it will
     * send the produced output to the log instead.
     *
     * TODO rename to toLog instead. Should toLog be default nowadays that
     * there is not much output any more?
     *
     */
    private static class NullDumper implements Runnable
    {
        Process process;
        boolean toNull;
        BufferedReader reader;
        String prefix;
        Thread thread;

        public NullDumper(Process p, boolean toNull, InputStream is,
            String prefix)
        {
            this.process = p;
            this.toNull = toNull;
            this.reader = new BufferedReader(new InputStreamReader(is));
            this.prefix = prefix;
        }

        public NullDumper start()
        {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
            return this;
        }

        public void run()
        {
            synchronized (this)
            {
                if (thread == null)
                {
                    thread = Thread.currentThread();
                }
                if (process == null)
                {
                    return;
                }
            }
            String line;
            while (true)
            {
                synchronized (this)
                {
                    if (process == null)
                    {
                        return;
                    }
                }
                try
                {
                    line = reader.readLine();
                }
                catch (IOException e)
                {
                    // ignore & end
                    return;
                }

                if (line == null)
                {
                    return;
                }

                if (!this.toNull)
                {
                    LOGGER.log(Level.INFO, prefix + line);
                }
            }
        }

        public void done()
        {
            synchronized (this)
            {
                process = null;
                if (thread != null)
                {
                    thread.interrupt();
                }
                thread = null;
            }
            try
            {
                this.reader.close();
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "Nulldumper reader.close got " + e);
            }
        }
    } // END Class NullDumper
}
