package net.sf.colossus.webcommon;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.colossus.util.Options;


/** One object of this this class represents one game run at the server, 
 *  starting from state "potential game", over when it is ready to start,
 *  finds and reserves a port for it, starts it in a separate process, 
 *  and when the process terminates, releases the port.
 * 
 *  The same class is also used at client side, but only because
 *  the interface requires it, and only part of the data is used
 *  (e.g. the user has only a name, not a socket).
 *
 *  @version $Id$
 *  @author Clemens Katzer
 */


public class GameInfo extends Thread
{
    // the possible states of a game:
    public final static int Proposed = 1;
    public final static int Running = 3;
    public final static int Ending = 5;

    private int state;

    private IRunWebServer server = null;

    private int portNr = -1;

    private String gameId;

    private String initiator;
    private String variant;
    private String viewmode;

    // those 3 should still be added at client side:
    private String eventExpiring;
    private boolean unlimitedMulligans;
    private boolean balancedTowers;

    // this 2 statically set like this:
    private boolean autoSave = true;
    private boolean logDebug = true;

    private int min;
    private int target;
    private int max;

    private int AIplayers;
    private int enrolledPlayers;

    private String workFilesBaseDir;
    private String template;
    private String javaCommand;
    private String colossusJar;

    private ArrayList players = null;

    private static int nextFreeGameId = 1;

    private File flagFile;

    // used on server side, to create a game proposed by client
    public GameInfo(String initiator, String variant, String viewmode,
        String expire, boolean unlimitedMulligans, boolean balancedTowers,
        int min, int target, int max)
    {
        this.gameId = new Integer(nextFreeGameId).toString();
        nextFreeGameId++;

        this.initiator = initiator;
        this.variant = variant;
        this.viewmode = viewmode;
        this.eventExpiring = expire;
        this.unlimitedMulligans = unlimitedMulligans;
        this.balancedTowers = balancedTowers;
        this.min = min;
        this.target = target;
        this.max = max;

        this.state = Proposed;
        this.enrolledPlayers = 0;
        this.players = new ArrayList();

        this.server = null;
        // System.out.println("A new potential game was created!! - variant " + variant + " viewmode " + viewmode);

    }

    public void setState(int state)
    {
        this.state = state;
    }

    public void setState(Integer state)
    {
        this.state = state.intValue();
    }

    public int getGameState()
    {
        return this.state;
    }

    public String getStateString()
    {
        String stateString = "<unknown>";
        switch (this.state)
        {
            case Proposed:
                stateString = "Proposed";
                break;

            case Running:
                stateString = "Running";
                break;

            case Ending:
                stateString = "Ending";
                break;

            default:
                stateString = "<unknown state: " + state + ">";
        }
        return stateString;
    }

    public String getGameId()
    {
        return this.gameId;
    }

    public void setGameId(String val)
    {
        this.gameId = val;
    }

    /*    
     public void setPort(int portNr)
     {
     this.portNr = portNr;
     }
     
     public int getPort()
     {
     return this.portNr;
     }
     */
    public String getInitiator()
    {
        return initiator;
    }

    public void setInitiator(String val)
    {
        initiator = val;
    }

    public String getVariant()
    {
        return variant;
    }

    public void setVariant(String val)
    {
        variant = val;
    }

    public String getViewmode()
    {
        return viewmode;
    }

    public void setViewmode(String val)
    {
        viewmode = val;
    }

    public String getEventExpiring()
    {
        return eventExpiring;
    }

    public void setEventExpiring(String val)
    {
        eventExpiring = val;
    }

    public boolean getUnlimitedMulligans()
    {
        return unlimitedMulligans;
    }

    public void setUnlimitedMulligans(boolean val)
    {
        unlimitedMulligans = val;
    }

    public boolean getBalancedTowers()
    {
        return balancedTowers;
    }

    public void setBalancedTowers(boolean val)
    {
        balancedTowers = val;
    }

    public Integer getMin()
    {
        return new Integer(min);
    }

    public void setMin(Integer val)
    {
        min = val.intValue();
    }

    public Integer getTarget()
    {
        return new Integer(target);
    }

    public void setTarget(Integer val)
    {
        target = val.intValue();
    }

    public Integer getMax()
    {
        return new Integer(max);
    }

    public void setMax(Integer val)
    {
        max = val.intValue();
    }

    public Integer getEnrolledCount()
    {
        return new Integer(enrolledPlayers);
    }

    public void setEnrolledCount(Integer val)
    {
        enrolledPlayers = val.intValue();
    }

    public ArrayList getPlayers()
    {
        return this.players;
    }

    public String getPlayerListAsString()
    {
        StringBuffer playerList = new StringBuffer("");
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            if (playerList.length() != 0 )
            {
                playerList.append(", ");
            }
            User user = (User)it.next();
            playerList.append(user.getName());
        }

        return playerList.substring(0);
    }

    public boolean isEnrolled(User newUser)
    {
        String newName = newUser.getName();
        Iterator it = players.iterator();
        boolean found = false;
        while (!found && it.hasNext())
        {
            User user = (User)it.next();
            String name = user.getName();
            // System.out.println("comparing against enrolled " + name);
            if (newName.equals(name))
            {
                it.remove();
                found = true;
            }
        }
        it = null;
        if (found)
        {
            // System.out.println("reEnroll: Adding new user to players.");
            players.add(newUser);
        }

        // System.out.println("Players now: " + players.toString());
        return found;
    }

    public void setPlayerList(ArrayList playerlist)
    {
        players = playerlist;

    }

    /*
     * return reason why fail, or null if ok
     */
    public String enroll(User user)
    {
        String reason = null;

        if (enrolledPlayers < max)
        {
            synchronized (players)
            {
                players.add(user);
                enrolledPlayers++;
            }
        }
        else
        {
            reason = "Game is full";
        }

        return reason;
    }

    public String unenroll(User user)
    {
        String reason = null;

        synchronized (players)
        {
            int index = players.indexOf(user);
            if (index != -1)
            {
                players.remove(index);
                enrolledPlayers--;
                if (players.size() != enrolledPlayers)
                {
                    System.out.println("\n\nSEVERE: players.size() != enrolledPlayers!!");
                }
            }
            else
            {
                reason = "Player " + user.getName() +
                    " to unenroll not found in game " + gameId;
            }
        }

        return reason;
    }

    public String toString(String sep)
    {
        StringBuffer playerList = new StringBuffer();
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            playerList.append(sep);
            User user = (User)it.next();
            playerList.append(user.getName());
        }

        String message = gameId + sep + state + sep + initiator + sep + variant +
            sep + viewmode +
            sep + eventExpiring + sep + unlimitedMulligans + sep +
            balancedTowers +
            sep + min + sep + target + sep + max + sep + enrolledPlayers +
            playerList.toString();

        return message;
    }

    public void run()
    {
        if (server != null)
        {
            run_on_server();
        }
        else
        {
            run_on_client();
        }
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

    // ================= now the stuff for running the game on client side ===============

    // used on client side, to restore a proposed game sent
    // by server
    public GameInfo(String gameId)
    {
        this.gameId = gameId;
    }

    public static GameInfo fromString(String[] tokens, HashMap games)
    {
        GameInfo gi;

        // tokens[0] is the command
        String gameId = tokens[1];
        String key = gameId;

        if (games.containsKey(gameId))
        {
            // use the object webclient has created earlier
            gi = (GameInfo)games.get(key);
        }
        else
        {
            gi = new GameInfo(gameId);
            games.put(key, gi);
        }

        gi.state     = Integer.parseInt(tokens[2]);
        gi.initiator = tokens[3];
        gi.variant   = tokens[4];
        gi.viewmode  = tokens[5];
        gi.eventExpiring = tokens[6];
        gi.unlimitedMulligans = Boolean.valueOf(tokens[7]).booleanValue();
        gi.balancedTowers = Boolean.valueOf(tokens[8]).booleanValue();
        gi.min       = Integer.parseInt(tokens[9]);
        gi.target    = Integer.parseInt(tokens[10]);
        gi.max       = Integer.parseInt(tokens[11]);
        int lastIndex = 12;
        gi.enrolledPlayers = Integer.parseInt(tokens[lastIndex]);

        ArrayList players = new ArrayList();
        int i = 1;
        while (i <= gi.enrolledPlayers)
        {
            String name = tokens[lastIndex+i];
            User user = new User(name);
            players.add(user);
            i++;
        }
        gi.players = players;

        return gi;
    }

    /*
     public boolean makeRunningGame(int port)
     {
     this.portNr = port;
     return true;
     }
     */

    public void run_on_client()
    {
        // System.out.println("Running game on client side - dummy");
    }

    // ================= now the stuff for running the game on server side ===============

    public boolean makeRunningGame(IRunWebServer server, String baseDir, String template,
        String javaCommand, String colossusJar, int portNr)
    {
        this.server = server;
        this.AIplayers = 0;  // not supported yet

        this.workFilesBaseDir = baseDir;
        this.template = template;
        this.javaCommand = javaCommand;
        this.colossusJar = colossusJar;

        this.portNr = portNr;
        this.setName("Game at port " + portNr);
        this.state = Running;
        return true;
    }

    public void run_on_server()
    {
        File gameDir = new File(workFilesBaseDir, gameId);
        gameDir.mkdirs();

        String fileName = "Game."+gameId+".running.flag";

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

        String loggingFileArg = propFileOk ?
            "-Djava.util.logging.config.file=" + logPropFile : "";

        String command = javaCommand + " " + loggingFileArg +
            " -Duser.home=" + gameDir +
            " -jar " + colossusJar + " -p " + portNr +
            " -n " + this.enrolledPlayers + " -i " + this.AIplayers +
            " -g --flagfile " + fileName;

        try
        {
            Process p = rt.exec(command, null, gameDir);
            p.getOutputStream().close();
            NullDumper ndout = new NullDumper(p, false, p.getInputStream(), gameId+"_OUT: ").start();
            NullDumper nderr = new NullDumper(p, false, p.getErrorStream(), gameId+"_ERR: ").start();

            superviseGameStartup();

            waitForGameShutdown(p, ndout, nderr);

        }
        catch (Exception e)
        {
            System.out.println("Executing\n" + command +
                "\ndid throw exception:\n" + e.toString());
        }
    }

    private void superviseGameStartup()
    {

        server.tellEnrolledGameStartsSoon(this);
        // System.out.println("\nSeems starting game went ok, informing enrolled players!");

        int timeout = 30; // seconds
        boolean up = waitUntilReadyToAcceptClients(timeout);
        if (up)
        {
            // System.out.println("Game is up - informing clients!");
            server.tellEnrolledGameStartsNow(this, portNr);
            server.allTellGameInfo(this);

            boolean ok = waitUntilGameStartedSuccessfully(10);
            if (ok)
            {
                server.tellEnrolledGameStarted(this);
            }
            else
            {
                System.out.println("\n\n!!!!!!!!!!!!!!!!!\nSEVERE: game started but not all clients came in!!");
            }
        }
        else
        {
            System.out.println("\n\n!!!!!!!!!!!!!!!!!\nSEVERE: game did not came up!!");
        }

    }

    private void waitForGameShutdown(Process p, NullDumper ndout, NullDumper nderr)
    {
        try
        {
            p.waitFor();
            ndout.done();
            nderr.done();
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }

        try
        {
            int exitCode = p.waitFor();

            if (exitCode != 0)
            {
                System.out.println("After waitFor... - exit code is " +
                    exitCode);
            }
        }
        catch (InterruptedException e)
        {
            String reason = "InterruptedException";
            server.gameFailed(this, reason);
        }

        if (flagFile.exists())
        {
            System.out.println("Game " + gameId + " ended but flagfile " +
                flagFile.toString() +
                " does still exist...? Deleting it...");
            flagFile.delete();
        }
        else
        {
            // System.out.println("Game " + gameId + " ended and flagfile " + fileName +
            //        " is gone. Fine!");
        }

        server.unregisterGame(this, portNr);

    }

    private boolean createLoggingPropertiesFromTemplate(File logPropTemplate,
        File logPropFile)
    {
        boolean ok = true;
        String patternLine = "java.util.logging.FileHandler.pattern=";
        try
        {
            String line;
            BufferedReader in =
                new BufferedReader(new InputStreamReader(
                new FileInputStream(logPropTemplate)));

            PrintWriter out = new PrintWriter(new FileOutputStream(logPropFile));

            while ( (line = in.readLine()) != null )
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
                    line = new String(patternLine+"Colossus%g.log");
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

    private boolean createServerCfgFile(File gameDir)
    {
        boolean ok = true;

        String gameDirPath = gameDir.getPath();
        Options gameOptions = new Options("server", gameDirPath + "/.colossus/");

        gameOptions.setOption(Options.variant, this.variant);
        gameOptions.setOption(Options.viewMode, this.viewmode);
        gameOptions.setOption(Options.autosave, this.autoSave);
        gameOptions.setOption(Options.logDebug, this.logDebug);
        gameOptions.setOption(Options.eventExpiring, this.eventExpiring);
        gameOptions.setOption(Options.unlimitedMulligans,
            this.unlimitedMulligans);
        gameOptions.setOption(Options.balancedTowers, this.balancedTowers);

        gameOptions.setOption(Options.autoQuit, true);
        gameOptions.setOption(Options.autoStop, true);

        gameOptions.saveOptions();

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

        for (int i=0 ; !up && i < timeout ; i++)
        {
            up = isSocketUp();
            if (!up)
            {
                // System.out.println("GameInfo.waitURTAC(): socket not up yet - sleeping a second...");
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
            System.out.println("wait for line: IOException: " + e1.toString());
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
            in = new BufferedReader(new InputStreamReader(
                new FileInputStream(flagFile)));
        }
        catch (FileNotFoundException ef)
        {
            System.out.println("FATAL ERROR: FileNotFoundExcepton " +
                ef.toString());
        }

        if (in == null)
        {
            System.out.println("FATAL ERROR: could not open flagfile for reading!!");
            return false;
        }

        int connected = 0;
        int checkInterval = 1000; // every second

        for (int i=0 ; !ok && i < timeout ; i++)
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

            if ( connected >= players.size())
            {
                ok = true;
            }
        }

        if (ok)
        {
            System.out.println("Game started ok - fine!");
        }
        else
        {
            // System.out.println("## RESULT: game started, but not all clients did connect");
            // System.out.println("Got only " + connected + " players");
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
            // System.out.println("InterruptException caught... ignoring it...");
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

        public NullDumper(Process p, boolean toNull, InputStream is, String prefix)
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
                    System.out.println(prefix + line);
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
        }
    } // END Class NullDumper

}

