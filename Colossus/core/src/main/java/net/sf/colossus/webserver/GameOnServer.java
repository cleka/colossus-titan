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
import java.util.logging.Level;
import java.util.logging.Logger;


import net.sf.colossus.util.Options;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IRunWebServer;


public class GameOnServer extends Thread
{
    private static final Logger LOGGER = Logger.getLogger(GameOnServer.class
        .getName());

    private int hostingPort;
    private String hostingHost;
    
    private IRunWebServer server;
    private WebServerOptions options;
    private GameInfo gi;
    private String gameId;

    private String workFilesBaseDir;
    private String template;
    private String javaCommand;
    private String colossusJar;

    private int AIplayers = 0;
    private final boolean autoSave = true;

    
    private File flagFile;

    public GameOnServer(IRunWebServer server, WebServerOptions options, GameInfo gi)
    {
        this.server = server;
        this.options = options;
        this.gi = gi;
        this.gameId = gi.getGameId();
        gi.setGameOnServer(this);
    }
    
    // ================= now the stuff for running the game on server side ===============

    public boolean makeRunningGame()
    {
        workFilesBaseDir = options.getStringOption(WebServerConstants.optWorkFilesBaseDir);
        template = options.getStringOption(WebServerConstants.optLogPropTemplate);
        javaCommand = options.getStringOption(WebServerConstants.optJavaCommand);
        colossusJar = options.getStringOption(WebServerConstants.optColossusJar);

                
        hostingPort = gi.getPort();
        hostingHost = gi.getHostingHost();
        
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
    
    @Override
    public void run()
    {
        // TODO FIXME  This still needs to be done...!!!
    }
    
    // used when cancelling: set to null and then start(),
    // then the run() method calls only the client dummy which is a 
    // do-nothing operation.
    // If start() is not run, the GameInfo object will never get 
    // garbage collected and finalized.

    public void setServerNull()
    {
        this.server = null;
    }

    
    public void run_on_server()
    {
        File gameDir = new File(workFilesBaseDir, gameId);
        gameDir.mkdirs();

        String fileName = "Game." + gameId + ".running.flag";

        this.flagFile = new File(gameDir, fileName);
        if (flagFile.exists())
        {
            flagFile.delete();
        }

        File logPropFile = new File(gameDir, "logging.properties");
        File logPropTemplate = new File(template);
        boolean propFileOk = createLoggingPropertiesFromTemplate(
            logPropTemplate, logPropFile);

        createServerCfgFile(gameDir);

        Runtime rt = Runtime.getRuntime();

        String loggingFileArg = propFileOk ? "-Djava.util.logging.config.file="
            + logPropFile
            : "";

        String command = javaCommand + " " + loggingFileArg + " -Duser.home="
            + gameDir + " -jar " + colossusJar + " -p " + hostingPort + " -n "
            + gi.getEnrolledCount()+ " -i " + this.AIplayers
            + " -g --flagfile " + fileName;

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

    private boolean createServerCfgFile(File gameDir)
    {
        boolean ok = true;

        String gameDirPath = gameDir.getPath();
        Options gameOptions = new Options("server", gameDirPath
            + "/.colossus/", false);

        gameOptions.setOption(Options.variant, gi.getVariant());
        gameOptions.setOption(Options.viewMode, gi.getViewmode());
        gameOptions.setOption(Options.autosave, this.autoSave);
        gameOptions.setOption(Options.eventExpiring, gi.getEventExpiring());
        gameOptions.setOption(Options.unlimitedMulligans,
            gi.getUnlimitedMulligans());
        gameOptions.setOption(Options.balancedTowers, gi.getBalancedTowers());

        gameOptions.setOption(Options.autoQuit, true);
        gameOptions.setOption(Options.autoStop, true);

        gameOptions.saveOptions();

        return ok;
    }

    
    private void superviseGameStartup()
    {
        // ACTIVATED
        server.tellEnrolledGameStartsSoon(gi);
        LOGGER.log(Level.FINEST,
            "Seems starting game went ok, informing enrolled players!");

        int timeout = 30; // seconds
        boolean up = waitUntilReadyToAcceptClients(timeout);
        if (up)
        {
            LOGGER.log(Level.FINEST, "Game is up - informing clients!");
            // READY_TO_CONNECT
            
            int port = gi.getPort();
            // if run on GameServer, null.
            // TODO: real remote host there, when runs on players PC
            String hostingHost = null;

            
            server.tellEnrolledGameStartsNow(gi, hostingHost, port);
            server.allTellGameInfo(gi);

            boolean ok = waitUntilGameStartedSuccessfully(10);
            if (ok)
            {
                // RUNNING
                server.tellEnrolledGameStarted(gi);
            }
            else
            {
                LOGGER.log(Level.SEVERE,
                    "  !!! game started but not all clients came in!!!");
            }
        }
        else
        {
            LOGGER.log(Level.SEVERE, "game did not came up!!!");
        }

    }

    private void waitForGameShutdown(Process p, NullDumper ndout,
        NullDumper nderr)
    {
        try
        {
            p.waitFor();
            ndout.done();
            nderr.done();
        }
        catch (Exception e)
        {
            LOGGER
                .log(Level.SEVERE, "Exception durimg waitForGameShutdown", e);
        }

        try
        {
            int exitCode = p.waitFor();

            if (exitCode != 0)
            {
                LOGGER.log(Level.FINEST, "After waitFor... - exit code is "
                    + exitCode);
            }
        }
        catch (InterruptedException e)
        {
            String reason = "InterruptedException";
            server.gameFailed(gi, reason);
        }

        if (flagFile.exists())
        {
            LOGGER.log(Level.WARNING, "Game " + gameId
                + " ended but flagfile " + flagFile.toString()
                + " does still exist...? Deleting it...");
            flagFile.delete();
        }
        else
        {
            LOGGER.log(Level.FINEST, "Game " + gameId + " ended and flagfile "
                + flagFile.toString() + " is gone. Fine!");
        }
        LOGGER.info("Before unregister game " + gi.getGameId());
        server.unregisterGame(gi, hostingPort);
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

    /*
     * Checks whether the game is already started "far enough", i.e.
     * that the serverSocket is ready to accept clients.
     * Game started with --webserver flag will create the flag file
     * after it created the socket.
     */

    public boolean isSocketUp()
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

    public boolean waitUntilGameStartedSuccessfully(int timeout)
    {
        boolean ok = false;

        String line;

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
            LOGGER.log(Level.SEVERE, "could not open flagfile for reading!!");
            return false;
        }

        int connected = 0;
        int checkInterval = 1000; // every second

        for (int i = 0; !ok && i < timeout; i++)
        {
            line = waitForLine(in, checkInterval);
            if (line == null)
            {
                // Wait til next round. If case is only needed to prevent
                // the else-ifs to check against null pointer.
            }
            else if (line.startsWith("Client connected: "))
            {
                connected++;
            }
            else if (line.startsWith("All clients connected"))
            {
                ok = true;
            }

            if (connected >= gi.getPlayers().size())
            {
                ok = true;
            }
        }

        if (ok)
        {
            LOGGER.log(Level.FINEST, "Game started ok - fine!");
        }
        else
        {
            LOGGER.log(Level.WARNING,
                "RESULT: game started, but not all clients did connect\n"
                    + "Got only " + connected + " players");
        }

        try
        {
            in.close();
        }
        catch (IOException e)
        {
            // ignore
        }
        return ok;
    }

    public void sleepFor(long millis)
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

    /*
     * We have to take care to read all what comes on the Game's processes
     * stdout and stderr, otherwise the game would block at some point
     */

    static class NullDumper implements Runnable
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
                if (line != null && !this.toNull)
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
