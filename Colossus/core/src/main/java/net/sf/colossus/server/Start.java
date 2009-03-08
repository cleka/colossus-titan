package net.sf.colossus.server;


import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.StartClient;
import net.sf.colossus.cmdline.CmdLine;
import net.sf.colossus.cmdline.Opt;
import net.sf.colossus.cmdline.Opts;
import net.sf.colossus.game.Game;
import net.sf.colossus.util.DebugMethods;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.webclient.WebClient;


/**
 *  Class Start contains code to start the different types of games.
 *  @version $Id$
 *  @author David Ripton
 *  @author Clemens Katzer (rewritten big parts of the main() method)
 */

public final class Start
{
    private static final Logger LOGGER = Logger.getLogger(Start.class
        .getName());

    // game static, so that (a local) Client can ask from Start for 
    // current game, to end it (before perhaps starting a new one).
    private static GameServerSide game = null;

    // static, there's always only one valid "what to do next" object:
    private static Start startObject = null;

    // Numeric values for the different activities that could
    // be "what to do next":
    public final static int StartGame = 1;
    public final static int StartNetClient = 2;
    public final static int StartWebClient = 3;
    public final static int LoadGame = 4;
    public final static int GetPlayersDialog = 5;
    public final static int NetClientDialog = 6;
    public final static int QuitAll = 7;

    // ===============================================================
    // First some "startObject" stuff, and object which holds all data
    // related to "what to do next".

    private int whatToDoNext;
    private final Options stOptions;
    private static int howManyGamesLeft = 0;

    public Start(int whatToDoNext, Options stOptions)
    {
        this.whatToDoNext = whatToDoNext;
        this.stOptions = stOptions;
    }

    public static String valToString(int whatToDoNext)
    {
        String text = "";
        switch (whatToDoNext)
        {
            case StartGame:
                text = "Start Game";
                break;

            case StartNetClient:
                text = "Start Net Client";
                break;

            case StartWebClient:
                text = "Start Web Client";
                break;

            case LoadGame:
                text = "Load Game";
                break;

            case GetPlayersDialog:
                text = "GetPlayers dialog";
                break;

            case NetClientDialog:
                text = "NetClient dialog";
                break;

            case QuitAll:
                text = "Quit all";
                break;

            default:
                text = "<unknown>";
                break;
        }
        return text;
    }

    public int getWhatToDoNext()
    {
        return this.whatToDoNext;
    }

    public void setWhatToDoNext(int whatToDoNext)
    {
        this.whatToDoNext = whatToDoNext;
        LOGGER.log(Level.FINEST, "Set what to do next to " + whatToDoNext
            + " (" + Start.valToString(whatToDoNext) + ")");
    }

    public void setWhatToDoNext(int whatToDoNext, String loadFile)
    {
        setWhatToDoNext(whatToDoNext);
        this.stOptions.setOption(Options.loadGameFileName, loadFile);
    }

    public Options getStartOptions()
    {
        return this.stOptions;
    }

    // ====================================================================
    // Here starts the static, main() related method stuff:

    // shortcut, used by Client:
    public static void setCurrentWhatToDoNext(int whatToDoNext)
    {
        Start startObj = Start.getCurrentStartObject();
        startObj.setWhatToDoNext(whatToDoNext);
    }

    public static void setCurrentWhatToDoNext(int whatToDoNext, String loadFile)
    {
        Start startObj = Start.getCurrentStartObject();
        startObj.setWhatToDoNext(whatToDoNext);
        startObj.getStartOptions().setOption(Options.loadGameFileName,
            loadFile);
    }

    public static Start getCurrentStartObject()
    {
        return startObject;
    }

    /** 
     *  Print a usage string to stdout.  (*Not* to the logfile, where casual
     *  users will miss it.)
     */
    private static void usage(Opts opts)
    {
        System.out.println("Usage: java -jar Colossus.jar [options]");
        Iterator<Opt> it = opts.getOptions().iterator();
        while (it.hasNext())
        {
            Opt opt = it.next();
            // This needs to go to the console, not the log, to be useful.
            System.out.println(opt.toString());
        }
    }

    /* 
     * Bring up the getplayers dialog (depending on startObject it starts 
     * either as GetPlayers or ready switched to Network client),
     * and then we wait here until is has set startObject to the next 
     * action to do and notified us to continue.
     */
    static void runGetPlayersDialogAndWait(Options options, Start startObject)
    {
        Object mutex = new Object();
        new GetPlayers(options, mutex, startObject);

        synchronized (mutex)
        {
            try
            {
                mutex.wait();
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, "Start waiting for GetPlayers "
                    + "to complete, wait interrupted?");
                // just to be sure to do something useful there...
                startObject.setWhatToDoNext(Start.GetPlayersDialog);
            }
        }
        mutex = null;
    }

    static void runNetClientDialogAndWait(Start startObject)
    {
        Object mutex = new Object();
        new StartClient(mutex, startObject);

        synchronized (mutex)
        {
            try
            {
                mutex.wait();
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.WARNING, "Start waiting for GetPlayers "
                    + "to complete, wait interrupted?");
                // just to be sure to do something useful there...
                startObject.setWhatToDoNext(Start.GetPlayersDialog);
            }
        }
        mutex = null;
    }

    private static boolean startNetClient(Options startOptions)
    {
        boolean dontWait = false;

        String playerName = startOptions
            .getStringOption(Options.runClientPlayer);
        String hostname = startOptions.getStringOption(Options.runClientHost);
        int port = startOptions.getIntOption(Options.runClientPort);

        boolean failed = false;
        try
        {
            // a hack to pass something into the Client constructor
            // TODO needs to be constructed properly
            Game dummyGame = new Game(null, new String[0]);

            Client c = new Client(hostname, port, dummyGame, playerName, null,
                false, false);
            failed = c.getFailed();
            c = null;
        }
        catch (Exception e)
        {
            failed = true;
        }
        if (failed)
        {
            // client startup failed for some reason
            dontWait = true;
        }

        // If starting net client succeeded, main() shall wait until
        // it ends. But if it fails, main() shall not wait, so that user
        // gets a new dialog immediately.
        return dontWait;
    }

    /*
     * Based on commandline options -c, -w, possibly -g, set
     * startObject to the right "whatToDoNext" action and
     * set in startOptions the related values. 
     * Expects that server (cf) options are already loaded.
     * 
     */
    public static void setInitialAction(CmdLine cl,
        Options netclientOptions, Options serverOptions, Options startOptions,
        Start startObject)
    {
        // Host, port and playername are stored back only to the startObject.
        // They would be copied to the server cf file in GetPlayers, when one
        // starts a client (host, player, and client-connects-to-port),
        // or when runs a game (load or new game) for serve-at-port.
        // The result is that the cmdline options given to -g do not
        // immediately modify the cf file settings -- unless player
        // "confirms" them by one of the GetPlayers actions.

        String hostname = null;
        if (cl.optIsSet('s'))
        {
            hostname = cl.getOptValue('s');
        }
        else if (cl.optIsSet('g') && cl.optIsSet('c'))
        {
            // Host empty means for StartClient: no cmdline wish given
            //  => it will initialize hostname box based on mostly LRU list.
            // If no -s given, but -g, we must here do the same preferredHost
            // evaluation what StartClient would do, so that the automatic
            // Client starting has a host != null.
            // And I want that "with or without -g" behaves (apart from the
            // whether one has to click somewhere or not) otherwise same,
            //   ( = would end up same host).

            Set<String> dummy = new TreeSet<String>();
            String preferred = StartClient.initServerNames(hostname, dummy,
                netclientOptions);
            hostname = preferred;
            dummy.clear();
        }
        if (hostname == null)
        {
            // Options class does not like null values - use empty instead.
            hostname = "";
        }
        startOptions.setOption(Options.runClientHost, hostname);
        startOptions.setOption(Options.webServerHost, hostname);

        // Ports (both serves-at-port and client-connects-to-port):
        // take from cf file (not set: defaultPort), overridable from cmdline
        int cp = netclientOptions.getIntOption(Options.runClientPort);
        int sp = serverOptions.getIntOption(Options.serveAtPort);
        // WebPort: only handover the cmdline wish. If none, wEWebClient
        // itself handles the "which one to use (cmdline, cf, default)".
        int wp = -1;

        cp = (cp != -1 ? cp : Constants.defaultPort);
        sp = (sp != -1 ? sp : Constants.defaultPort);

        int pOpt;
        if (cl.optIsSet('p'))
        {
            pOpt = Integer.parseInt(cl.getOptValue('p'));
            if (pOpt != -1)
            {
                cp = pOpt;
                sp = pOpt;
                wp = pOpt;
            }
        }
        startOptions.setOption(Options.runClientPort, cp);
        startOptions.setOption(Options.serveAtPort, sp);
        startOptions.setOption(Options.webServerPort, wp);

        String playerName = netclientOptions
            .getStringOption(Options.runClientPlayer);
        String webLogin = "";

        if (playerName == null || playerName.equals(""))
        {
            playerName = Constants.username;
        }
        if (cl.optIsSet('m'))
        {
            playerName = cl.getOptValue('m');
            webLogin = cl.getOptValue('m');
        }
        startOptions.setOption(Options.runClientPlayer, playerName);
        startOptions.setOption(Options.webClientLogin, webLogin);

        String webGameFlagFileName = "";
        if (cl.optIsSet('F'))
        {
            webGameFlagFileName = cl.getOptValue('F');
        }
        startOptions.setOption(Options.webFlagFileName, webGameFlagFileName);

        if (cl.optIsSet('l') || cl.optIsSet('z'))
        {
            startObject.setWhatToDoNext(LoadGame);
            String filename = null;
            if (cl.optIsSet('l'))
            {
                filename = cl.getOptValue('l');
            }
            else if (cl.optIsSet('z'))
            {
                filename = "--latest";
            }
            else
            {
                LOGGER.severe("Unreacheable else block reached??");
                filename = "--latest";
            }
            startOptions.setOption(Options.loadGameFileName, filename);
        }

        else if (cl.optIsSet('g'))
        {
            if (cl.optIsSet('c'))
            {
                startObject.setWhatToDoNext(StartNetClient);
            }
            else if (cl.optIsSet('w'))
            {
                startObject.setWhatToDoNext(StartWebClient);
            }
            else
            {
                startObject.setWhatToDoNext(StartGame);
            }
        }
        else
        {
            if (cl.optIsSet('c'))
            {
                startObject.setWhatToDoNext(NetClientDialog);
            }
            else if (cl.optIsSet('w'))
            {
                startObject.setWhatToDoNext(StartWebClient);
            }
            else
            {
                startObject.setWhatToDoNext(GetPlayersDialog);
            }
        }
    }

    /*
     * Modify options from command-line args if possible.
     * Return false if something is wrong.  
     */
    private static boolean setupOptionsFromCommandLine(CmdLine cl,
        Options startOptions, Options options)
    {
        if (cl == null)
        {
            return true;
        }

        int numHumans = 0;
        int numAIs = 0;
        int numNetworks = 0;
        
        int numSimpleAIs = 0;
        int numRationalAIs = 0;
        int numMilvangAIs = 0;

        options.removeOption(Options.autoPlay);
        options.removeOption(Options.goOnWithoutObserver);

        if (cl.optIsSet('R'))
        {
            options.setOption(Options.autosave, false);
            options.setOption(Options.autoQuit, false);
            options.setOption(Options.variant, Constants.variantArray[0]);
        }

        if (cl.optIsSet('v'))
        {
            String variantName = cl.getOptValue('v');
            // XXX Check that this variant is in the list.
            options.setOption(Options.variant, variantName);
        }
        else if (options.getStringOption(Options.variant) == null)
        {
            options.setOption(Options.variant, Constants.variantArray[0]);
        }

        if (cl.optIsSet('q'))
        {
            options.setOption(Options.autoQuit, true);
        }
        if (cl.optIsSet('O'))
        {
            // needed basically only for the old stresstest which runs
            // without any window
            options.setOption(Options.goOnWithoutObserver, true);
        }
        if (cl.optIsSet('S'))
        {
            options.setOption(Options.autosave, true);
        }
        if (cl.optIsSet('A'))
        {
            options.setOption(Options.autoPlay, true);
        }
        if (cl.optIsSet('N'))
        {
            options.setOption(Options.nonRandomBattleDice, true);
        }
        if (cl.optIsSet('d'))
        {
            String buf = cl.getOptValue('d');
            int delay = Integer.parseInt(buf);
            options.setOption(Options.aiDelay, delay);
        }
        if (cl.optIsSet('t'))
        {
            String buf = cl.getOptValue('t');
            int limit = Integer.parseInt(buf);
            options.setOption(Options.aiTimeLimit, limit);
        }

        if (cl.optIsSet('u'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('u');
            numHumans = Integer.parseInt(buf);
        }
        if (cl.optIsSet('i'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('i');
            numAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('Z'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('Z');
            numSimpleAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('r'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('r');
            numRationalAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('M'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('M');
            numMilvangAIs = Integer.parseInt(buf);
        }
        if (cl.optIsSet('n'))
        {
            options.clearPlayerInfo();
            String buf = cl.getOptValue('n');
            numNetworks = Integer.parseInt(buf);
        }

        // Quit if values are bogus.
        if (numHumans < 0 || numAIs < 0 || numNetworks < 0
            || numHumans + numAIs + numNetworks > Constants.MAX_MAX_PLAYERS)
        {
            LOGGER.log(Level.SEVERE, "Illegal number of players");
            return false;
        }
        if (numAIs < (numSimpleAIs + numRationalAIs + numMilvangAIs)) {
            LOGGER.log(Level.SEVERE, "Illegal number of specific AIs");
            return false;
        }

        

        for (int i = 0; i < numHumans; i++)
        {
            String name = null;
            String preferredHumanName = startOptions
                .getStringOption(Options.runClientPlayer);
            if (i == 0 && preferredHumanName != null
                && !preferredHumanName.equals(""))
            {
                name = preferredHumanName;
            }
            else
            {
                //                name = Constants.byColor + i;
                name = Constants.byType + i;
            }
            options.setOption(Options.playerName + i, name);
            options.setOption(Options.playerType + i, Constants.human);
        }
        for (int j = numHumans; j < numNetworks + numHumans; j++)
        {
            String name = Constants.byClient + j;
            options.setOption(Options.playerName + j, name);
            options.setOption(Options.playerType + j, Constants.network);
        }
        for (int k = numHumans + numNetworks; k < numAIs + numHumans
            + numNetworks; k++)
        {
            //            String name = Constants.byColor + k;
            String name = Constants.byType + k;
            options.setOption(Options.playerName + k, name);
            if (numSimpleAIs > 0) {
                options.setOption(Options.playerType + k, "SimpleAI");
                numSimpleAIs --;
            } else if (numRationalAIs > 0) {
                options.setOption(Options.playerType + k, "RationalAI");
                numRationalAIs --;
            } else if (numMilvangAIs > 0) {
                options.setOption(Options.playerType + k, "MilvangAI");
                numMilvangAIs --;
            } else {
                options.setOption(Options.playerType + k, Constants.defaultAI);
            }
        }

        return true;
    }

    public static void main(String[] args)
    {
        LOGGER.log(Level.INFO, "Start for Colossus version "
            + Client.getVersion() + " at " + new Date().getTime());

        Opts opts = new Opts();
        CmdLine cl = null;

        // Catch-all block so we can log fatal exceptions.
        try
        {
            opts.addOption('h', "help", false, "Show options help");
            opts.addOption('l', "load", true, "Load savegame");
            opts.addOption('z', "latest", false, "Load latest savegame");
            opts.addOption('g', "go", false, "Skip startup dialogs");
            opts.addOption('v', "variant", true, "Set variant");
            opts.addOption('u', "nhuman", true, "Number of humans");
            opts.addOption('i', "nai", true, "Number of AIs (default: random)");
            opts.addOption('Z', "simpleai", true, "Number of SimpleAIs");
            opts.addOption('r', "rationalai", true, "Number of RationalAIs");
            opts.addOption('M', "milvangai", true, "Number of MilvangAIs");
            opts.addOption('n', "nnetwork", true, "Number of network slots");
            opts.addOption('q', "quit", false, "Quit JVM when game ends");
            opts.addOption('p', "port", true, "Server port number");
            opts.addOption('d', "delay", true, "AI delay in ms");
            opts.addOption('t', "timelimit", true, "AI time limit in s");
            opts.addOption('c', "client", false, "Run network client instead");
            opts.addOption('w', "webclient", false, "Run web client instead");
            opts.addOption('F', "flagfile", true, "Create flagfile when socket up");
            opts.addOption('s', "server", true, "Server name or IP");
            opts.addOption('S', "autosave", false, "Autosave");
            opts.addOption('A', "autoplay", false, "Autoplay");
            opts.addOption('N', "non-random-battle-dice", false, "Use non-random battle dice");
            opts.addOption('R', "resetOptions", false, "Reset options");
            opts.addOption('m', "myname", true, "My player name");
            opts.addOption('O', "noobserver", false, "Go on without observer");

            cl = opts.parse(args);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE,
                "Exception during commandline processing: " + ex.toString(),
                ex);
            System.exit(1);
            return; // just to avoid the warning "cl might be null" ...
        }

        if (cl.optIsSet('h'))
        {
            usage(opts);
            System.exit(0);
        }

        // Read option settings (from Server cf file)
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME);
        serverOptions.loadOptions();

        // Read netclient option settings (from own cf file)
        Options netclientOptions = new Options(Constants.OPTIONS_NET_CLIENT_NAME);
        netclientOptions.loadOptions();

        // Options remembered only inside this running application,
        // related to server/port/name startup settings; initialized
        // from commandline options, perhaps modified by dialogs.
        // We never save those startOptions, but some of them are copied
        // to the server options in GetPlayers when the user initiates
        // a related action.
        Options startOptions = new Options(Constants.OPTIONS_START);

        // The static startObject represents/stores the action we 
        // have to do next; default action: GetPlayers dialog.
        startObject = new Start(GetPlayersDialog, startOptions);

        // set in startObject and startOptions the values related to
        // what-to-do, host, port, playername:
        setInitialAction(cl, serverOptions, netclientOptions, startOptions,
            startObject);

        // Set in options the remaining options
        // Needs startOptions only to get the playername to use in some cases.
        if (!setupOptionsFromCommandLine(cl, startOptions, serverOptions))
        {
            LOGGER.log(Level.SEVERE,
                "setupOptionsFromCommandLine signalled error, "
                    + "continuing anyway.", (Throwable)null);
        }

        boolean oneClientRunOnly = false;
        if (cl.optIsSet('c') && cl.optIsSet('g') && cl.optIsSet('q'))
        {
            oneClientRunOnly = true;
        }
        
        // Cmdline arguments have effect only to first game - or
        // are stored within the options or startOptions.
        cl = null;

        howManyGamesLeft = Options.getHowManyStresstestRoundsProperty();

        boolean dontWait = false;
        int whatToDoNext;

        // Now loop until user requested Quitting the whole application: 
        while ((whatToDoNext = startObject.getWhatToDoNext()) != Start.QuitAll)
        {
            // re-initialize options, except in first loop round,
            // there they have been loaded already and modified 
            // according to cmdline options
            if (serverOptions == null)
            {
                serverOptions = new Options(Constants.OPTIONS_SERVER_NAME);
                serverOptions.loadOptions();
                if (Options.isStresstest())
                {
                    serverOptions.setOption(Options.autoPlay, true);
                }
            }
            if (netclientOptions == null)
            {
                netclientOptions = new Options(Constants.OPTIONS_NET_CLIENT_NAME);
                netclientOptions.loadOptions();
            }

            // Unless there is already something selected what to do
            // (e.g. in in first round on commandline, or user ended 
            // a game/closed board with selecting Load Game etc.), 
            // as first thing come up with the dialog to ask what to do:

            if (whatToDoNext == GetPlayersDialog)
            {
                runGetPlayersDialogAndWait(serverOptions, startObject);
                whatToDoNext = startObject.getWhatToDoNext();
            }

            // intentionally not else if - short way if user selected
            // in GetPlayers dialog the "Run network client" button. 
            if (whatToDoNext == NetClientDialog)
            {
                runNetClientDialogAndWait(startObject);
                whatToDoNext = startObject.getWhatToDoNext();
            }

            // ----------------------------------------------------------------
            // Longish if-elseif-else - now we do the thing user wants:

            if (whatToDoNext == GetPlayersDialog
                || whatToDoNext == NetClientDialog)
            {
                // ok, just done. Need if also in this else-if chain
                // otherwise the "else" would complain...
                dontWait = true;
            }

            else if (whatToDoNext == StartGame)
            {
                startObject.setWhatToDoNext(GetPlayersDialog);
                int port = startOptions.getIntOption(Options.serveAtPort);
                String webGameFlagFileName = startOptions
                    .getStringOption(Options.webFlagFileName);
                startOptions.removeOption(Options.webFlagFileName);

                game = new GameServerSide();
                game.setPort(port);
                game.setOptions(serverOptions);
                if (webGameFlagFileName != null
                    && !webGameFlagFileName.equals(""))
                {
                    startObject.setWhatToDoNext(Start.QuitAll);
                    game.setFlagFilename(webGameFlagFileName);
                }
                game.newGame();
            }

            else if (whatToDoNext == LoadGame)
            {
                startObject.setWhatToDoNext(GetPlayersDialog);
                int port = startOptions.getIntOption(Options.serveAtPort);
                String loadFileName = startOptions
                    .getStringOption(Options.loadGameFileName);
                
                if (loadFileName != null && loadFileName.length() > 0)
                {
                    game = new GameServerSide();
                    game.setPort(port);
                    game.setOptions(serverOptions);
                    serverOptions.clearPlayerInfo();
                    game.loadGame(loadFileName);
                }
                else
                {
                    LOGGER.log(Level.SEVERE,
                        "Selected action LoadGame, but filename is '"
                            + loadFileName + "' (= null or empty)!",
                        (Throwable)null);
                }
            }

            // User clicked "Go" button in the Network Client tab of
            // GetPlayers - GUI stores values in options
            // @TODO: get via startObject instead?
            else if (whatToDoNext == StartNetClient)
            {
                // by default (if user does not say anything other when ending), 
                // after that come back to NetClient dialog.
                if (oneClientRunOnly)
                {
                    startObject.setWhatToDoNext(QuitAll);
                }
                else
                {
                    startObject.setWhatToDoNext(NetClientDialog);
                }
                dontWait = startNetClient(startOptions);
            }

            else if (whatToDoNext == StartWebClient)
            {
                // By default get back to Main dialog.
                startObject.setWhatToDoNext(GetPlayersDialog);

                String hostname = startOptions
                    .getStringOption(Options.webServerHost);
                int port = startOptions.getIntOption(Options.webServerPort);
                String login = startOptions
                    .getStringOption(Options.webClientLogin);
                String password = null;
                new WebClient(hostname, port, login, password);
            }

            // User clicked Quit in GetPlayers (this loop round), 
            //  --or--
            // User selected File=>Quit in the game started from previous
            //  loop round.
            else if (whatToDoNext == QuitAll)
            {
                // Nothing to do, loop will end.
                dontWait = true;
            }

            // What else??
            else
            {
                LOGGER.log(Level.SEVERE, "Unknown value '" + whatToDoNext
                    + "' in main() loop???", (Throwable)null);
            }

            // ----------------------------------------------------------
            // Activity initiated ... or at least attempted to do so.
            // Wait for it to end, except if it was a failed netclient start 
            // or the activity "Quit" from main menu anyway.

            if (dontWait)
            {
                LOGGER.log(Level.FINEST,
                    "QuitAll selected, not waiting for anything to finish.");
                dontWait = false;
            }
            else
            {
                ViableEntityManager.waitUntilAllGone();
            }

            game = null;
            serverOptions = null;
            netclientOptions = null;

            // ResourceLoader has static String telling the server; if not reset,
            // for a remote client closing while game is not over, he set next 
            // to do to GetPlayers dialog, dialog wants to load Variant Readme,
            // resourceloader would fail.
            net.sf.colossus.util.ResourceLoader.resetDataServer();

            // DebugStuff.doCleanupStuff(false);

            // For Stresstesting (controlled by a system property):
            if (howManyGamesLeft > 1)
            {
                // Decrement in here, not in if, otherwise we decrement it
                // until negative infinity ;-)
                howManyGamesLeft--;
                LOGGER.log(Level.ALL, "howManyGamesLeft now "
                    + howManyGamesLeft + "\n");

                String loadFileName = startOptions
                    .getStringOption(Options.loadGameFileName);
                if (loadFileName != null)
                {
                    startObject.setWhatToDoNext(Start.LoadGame);
                }
                else
                {
                    startObject.setWhatToDoNext(Start.StartGame);
                }
            }

        } // end WHILE not QuitAll

        // Probably this could be totally deleted, but...
        // DebugMethods.doCleanupStuff(true);

        final boolean doWaitReturnLoop = false;
        if (doWaitReturnLoop)
        {
            // if this is used, make sure you have debug level for 
            // Root logger and InstanceTracker at least to INFO, 
            // otherwise you won't see any statistics...
            final boolean forceLoopAnyway = false;
            DebugMethods.waitReturnLoop(forceLoopAnyway);
        }

        final boolean printObjectStatistics = false;
        if (printObjectStatistics)
        {
            net.sf.colossus.webcommon.InstanceTracker.printStatistics();
        }

        // If want to have a way to prevent it from straight exit,
        // e.g. look at it with Profiler when everything is supposed to
        // be gone already (Clemens)
        final boolean waitReturnBeforeExiting = false;
        if (waitReturnBeforeExiting)
        {
            LOGGER.log(Level.ALL, "OK, after next RETURN it will really end.");
            DebugMethods.waitReturn();
        }

        LOGGER.log(Level.FINE, "Start.main() at the end "
            + "- JVM should exit now by itself.");
            
        // JVM should do a clean exit now, no System.exit() needed.
        // To be sure, at all places where user selects "Quit", a demon 
        // thread is started that does the System.exit() after a certain
        // delay (currently 10 secs - see class TimedJvmQuit).
    }

    /**
     * Trigger a timed Quit, which will (by using a demon thread) terminate
     * the JVM after a timeout (currently 10 seconds)  
     * - unless the JVM has quit already anyway because cleanup has
     * succeeded as planned.
     */
    public static void triggerTimedQuit()
    {
        if (howManyGamesLeft > 0)
        {
            LOGGER.info("HowManyGamesLeft not zero yet - ignoring the "
                + "request to trigger a timed quit.");
        }
        else
        {
            new TimedJvmQuit().start();
        }
    }

    /**
     * A demon thread which is started by triggerTimedQuit.
     * It will then (currently) sleep 10 seconds, and if it is then
     * still alive, do a System.exit(1) to terminate the JVM.
     * If, however, the game shutdown proceeded successdully as planned,
     * Start.main() will already have reached it's end and there should
     * not be any other non-demon threads alive, so the JVM *should*
     * terminate by itself cleanly.
     * So, if this TimedJvmQuit strikes, it means the "clean shutdown"
     * has somehow failed. 
     */
    public static class TimedJvmQuit extends Thread
    {
        private static final Logger LOGGER = Logger.getLogger(Start.class
            .getName());
        
        private static final String defaultName = "TimedJvmQuit thread";
        private final String name;
        
        private final long timeOutInSecs = 10;
        
        public TimedJvmQuit()
        {
            super();
            this.setDaemon(true);
            this.name = defaultName;
        }

        @Override
        public void run()
        {
            LOGGER.info(this.name + ": started... (sleeping "
                + timeOutInSecs + " seconds)");
            sleepFor(this.timeOutInSecs * 1000);
            LOGGER.warning(this.name + ": JVM still alive? "
                + "Ok, it's time to do System.exit()...");
            System.exit(1);
        }

        public static void sleepFor(long millis)
        {
            try
            {
                Thread.sleep(millis);
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.FINEST,
                    "InterruptException caught... ignoring it...");
            }
        }
    }
}
