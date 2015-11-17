package net.sf.colossus.appmain;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.colossus.client.Client;
import net.sf.colossus.cmdline.CmdLine;
import net.sf.colossus.cmdline.Opt;
import net.sf.colossus.cmdline.Opts;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.guiutil.DebugMethods;
import net.sf.colossus.server.GameLoading;
import net.sf.colossus.server.GameServerSide;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.util.ViableEntityManager;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.webclient.WebClient;


/**
 * Class Start contains code to start the different types of games.
 *
 * @author David Ripton
 * @author Clemens Katzer (rewritten big parts of the main() method)
 */
public final class Start
{
    private static final Logger LOGGER = Logger.getLogger(Start.class
        .getName());

    private CmdLine cmdLine;

    private final WhatNextManager whatNextManager;

    // Options remembered only inside this running application,
    // related to server/port/name startup settings; initialized
    // from command line options, perhaps modified by dialogs.
    // We never save those startOptions, but some of them are copied
    // to the server options in GetPlayers when the user initiates
    // a related action.
    private final Options startOptions;

    /**
     * To create the one "Start" object which handles initiates the
     * "whatToDoNext" action according to what the user wants.
     *
     * Brings up one of the dialogs, or starts a Game, a Network client
     * or a Web Client.
     */
    public Start(String[] args)
    {
        this.startOptions = new Options(Constants.OPTIONS_START);
        // initialize it from the -D..forceViewBoard  cmdline settings,
        // defaulting to false if no such argument given
        startOptions
            .setOption(Options.FORCE_BOARD, Constants.FORCE_VIEW_BOARD);
        this.whatNextManager = new WhatNextManager(startOptions);
        commandLineProcessing(args);
    }

    public Options getStartOptions()
    {
        return startOptions;
    }

    public WhatNextManager getWhatNextManager()
    {
        return whatNextManager;
    }

    public WhatToDoNext getWhatToDoNext()
    {
        return whatNextManager.getWhatToDoNext();
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

    /**
     * Prepare the "Opts" object to parse all options from command line.
     * As result, creates/sets the instance variable CmdLine object "cmdLine"
     * from which one can then query which options were set, and their value
     * if they require one.
     *
     * @param args The String-Array given to main()
     * @return
     */
    private void commandLineProcessing(String[] args)
    {
        Opts opts = new Opts();

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
            opts.addOption('C', "spectatorclient", false,
                "Run network spectator client");
            opts.addOption('w', "webclient", false, "Run web client instead");
            opts.addOption('F', "flagfile", true,
                "Create flagfile when socket up");
            opts.addOption('s', "server", true, "Server name or IP");
            opts.addOption('S', "autosave", false, "Autosave");
            opts.addOption('A', "autoplay", false, "Autoplay");
            opts.addOption('N', "non-random-battle-dice", false,
                "Use non-random battle dice");
            opts.addOption('R', "resetOptions", false, "Reset options");
            opts.addOption('m', "myname", true, "My player name");
            opts.addOption('O', "noobserver", false, "Go on without observer");

            cmdLine = opts.parse(args);
        }
        catch (Exception ex)
        {
            LOGGER.log(Level.SEVERE,
                "Exception during commandline processing: " + ex.toString(),
                ex);
            System.exit(1);
            return; // just to avoid the warning "cl might be null" ...
        }

        if (cmdLine.optIsSet('h'))
        {
            usage(opts);
            System.exit(0);
        }
    }

    /**
     * Based on command line options -c, -w, possibly -g, set
     * startObject to the right "whatToDoNext" action and
     * set in startOptions the related values.
     * Expects that server (cf) options are already loaded.
     *
     *
     */
    public void setInitialAction(Options serverOptions,
        Options netclientOptions)
    {
        // just as shortcut...
        CmdLine cl = cmdLine;

        // Host, port and player name are stored back only to the startObject.
        // They would be copied to the server cf file in GetPlayers, when one
        // starts a client (host, player, and client-connects-to-port),
        // or when runs a game (load or new game) for serve-at-port.
        // The result is that the command line options given to -g do not
        // immediately modify the cf file settings -- unless player
        // "confirms" them by one of the GetPlayers actions.

        String hostname = null;
        if (cl.optIsSet('s'))
        {
            hostname = cl.getOptValue('s');
        }
        else if (cl.optIsSet('g') && (cl.optIsSet('c') || cl.optIsSet('C')))
        {
            // Host empty means for NetworkClientDialog: no command line wish given
            //  => it will initialize host name box based on mostly LRU list.
            // If no -s given, but -g, we must here do the same preferredHost
            // evaluation what NetworkClientDialog would do, so that the automatic
            // Client starting has a host != null.
            // And I want that "with or without -g" behaves (apart from the
            // whether one has to click somewhere or not) otherwise same,
            //   ( = would end up same host).

            Set<String> dummy = new TreeSet<String>();
            String preferred = NetworkClientDialog.initServerNames(hostname,
                dummy, netclientOptions);
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

        if (cl.optIsSet('C'))
        {
            startOptions.setOption(Options.runSpectatorClient, true);
        }

        // Ports (both serves-at-port and client-connects-to-port):
        // take from cf file (not set: defaultPort), overridable from cmdline
        int cp = netclientOptions.getIntOption(Options.runClientPort);
        int sp = serverOptions.getIntOption(Options.serveAtPort);
        // WebPort: only hand over the cmdline wish. If none, wEWebClient
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
            whatNextManager.setWhatToDoNext(WhatToDoNext.LOAD_GAME, false,
                false);
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
            if (cl.optIsSet('c') || cl.optIsSet('C'))
            {
                whatNextManager.setWhatToDoNext(WhatToDoNext.START_NET_CLIENT,
                    false);
            }
            else if (cl.optIsSet('w'))
            {
                whatNextManager.setWhatToDoNext(WhatToDoNext.START_WEB_CLIENT,
                    false);
            }
            else
            {
                whatNextManager
                    .setWhatToDoNext(WhatToDoNext.START_GAME, false);
            }
        }
        else
        {
            if (cl.optIsSet('c') || cl.optIsSet('C'))
            {
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.NET_CLIENT_DIALOG, false);
            }
            else if (cl.optIsSet('w'))
            {
                whatNextManager.setWhatToDoNext(WhatToDoNext.START_WEB_CLIENT,
                    false);
            }
            else
            {
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
            }
        }
    }

    /**
     * Bring up the GetPlayers dialog, and then wait until is has set
     * startObject to the next action to do and notified us to continue.
     *
     * @param options The "server side" main options Object which holds the
     *    information what kind of game to play next (variant, which players)
     *    and the "Game options" for the to-be-started game, like
     *    unlimitedMulligans, viewmode, balancedTowers, ...)
     */
    private void runGetPlayersDialogAndWait(Options options)
    {
        Object mutex = new Object();
        new GetPlayers(options, mutex, getWhatNextManager(), false);

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
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
            }
        }
        mutex = null;
    }

    /*
     * Modify options from command-line args if possible.
     * Return false if something is wrong.
     */
    private boolean setupOptionsFromCommandLine(CmdLine cl,
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
            options.setOption(Options.fixedSequenceBattleDice, true);
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
        if (numAIs < (numSimpleAIs + numRationalAIs + numMilvangAIs))
        {
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
            if (numSimpleAIs > 0)
            {
                options.setOption(Options.playerType + k, "SimpleAI");
                numSimpleAIs--;
            }
            else if (numRationalAIs > 0)
            {
                options.setOption(Options.playerType + k, "RationalAI");
                numRationalAIs--;
            }
            else if (numMilvangAIs > 0)
            {
                options.setOption(Options.playerType + k, "MilvangAI");
                numMilvangAIs--;
            }
            else
            {
                options.setOption(Options.playerType + k, Constants.defaultAI);
            }
        }

        return true;
    }

    /**
     * Do the setup of the various Options objects (server, netclient),
     * some more preparations, and then it stays in the loop which
     * - waits for user input what to do next
     * - initiates that action and waits until it completes (or if canceled,
     *   like closing the network client dialog, bring up back the main
     *   (=GetPlayers) dialog, or if user requests Quit, exit the loop;
     * and when it exited the loop control will return back to main()
     * and the JVM should terminate sooner or later ;-)
     */
    private void setupAndLoop()
    {
        // Read option settings (from Server cf file)
        Options serverOptions = new Options(Constants.OPTIONS_SERVER_NAME);
        serverOptions.loadOptions();

        // Read netclient option settings (from own cf file)
        Options netclientOptions = new Options(
            Constants.OPTIONS_NET_CLIENT_NAME);
        netclientOptions.loadOptions();

        // set in startObject and startOptions the values related to
        // what-to-do, host, port, playerName:
        setInitialAction(serverOptions, netclientOptions);

        // Set in options the remaining options
        // Needs startOptions only to get the player name to use in some cases.
        if (!setupOptionsFromCommandLine(cmdLine, startOptions, serverOptions))
        {
            LOGGER.log(Level.SEVERE,
                "setupOptionsFromCommandLine signalled error, "
                    + "continuing anyway.", (Throwable)null);
        }

        boolean oneClientRunOnly = false;
        if (cmdLine.optIsSet('c') && cmdLine.optIsSet('g')
            && cmdLine.optIsSet('q'))
        {
            oneClientRunOnly = true;
        }

        // Command line arguments have effect only to first game
        // - or are stored within the options or startOptions.
        cmdLine = null;

        // Make sure "AIs stop when no humans left" is off when stresstesting
        if (Options.isStresstest())
        {
            startOptions.setOption(Options.autoStop, false);
            serverOptions.setOption(Options.autoStop, false);
        }
        boolean dontWait = false;

        // Now loop until user requested Quitting the whole application:
        while (getWhatToDoNext() != WhatToDoNext.QUIT_ALL)
        {
            // re-initialize options, except in first loop round,
            // there they have been loaded already and modified
            // according to command line options
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
                netclientOptions = new Options(
                    Constants.OPTIONS_NET_CLIENT_NAME);
                netclientOptions.loadOptions();
            }

            // Unless there is already something selected what to do
            // (e.g. in in first round on command line, or user ended
            // a game/closed board with selecting Load Game etc.),
            // as first thing come up with the dialog to ask what to do:

            if (getWhatToDoNext() == WhatToDoNext.GET_PLAYERS_DIALOG)
            {
                runGetPlayersDialogAndWait(serverOptions);
            }

            // intentionally not else if - short way if user selected
            // in GetPlayers dialog the "Run network client" button.
            if (getWhatToDoNext() == WhatToDoNext.NET_CLIENT_DIALOG)
            {
                runNetClientDialogAndWait();
            }

            // ----------------------------------------------------------------
            // Longish if-elseif-else - now we do the thing user wants:

            // TODO change to switch statement

            if (getWhatToDoNext() == WhatToDoNext.GET_PLAYERS_DIALOG
                || getWhatToDoNext() == WhatToDoNext.NET_CLIENT_DIALOG)
            {
                // ok, just done. Need if also in this else-if chain
                // otherwise the "else" would complain...
                dontWait = true;
            }

            else if (getWhatToDoNext() == WhatToDoNext.START_GAME)
            {
                whatNextManager.decrementHowManyGamesLeft();

                // TODO is this re-setting it needed?
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
                int port = startOptions.getIntOption(Options.serveAtPort);
                serverOptions.setOption(Options.serveAtPort, port);
                String webGameFlagFileName = startOptions
                    .getStringOption(Options.webFlagFileName);
                startOptions.removeOption(Options.webFlagFileName);

                String variantName = serverOptions
                    .getStringOption(Options.variant);
                Variant variant = VariantSupport.loadVariantByName(
                    variantName, true);
                GameServerSide game = GameServerSide.newGameServerSide(
                    getWhatNextManager(), serverOptions, variant);
                if (webGameFlagFileName != null
                    && !webGameFlagFileName.equals(""))
                {
                    whatNextManager.setWhatToDoNext(WhatToDoNext.QUIT_ALL,
                        false);
                    game.setFlagFilename(webGameFlagFileName);
                }
                game.startNewGameAndWaitUntilOver(null);
            }

            else if (getWhatToDoNext() == WhatToDoNext.LOAD_GAME)
            {
                boolean wasInteractive = whatNextManager.isInteractive();
                whatNextManager.decrementHowManyGamesLeft();

                // TODO is this re-setting it needed?
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
                int port = startOptions.getIntOption(Options.serveAtPort);
                serverOptions.setOption(Options.serveAtPort, port);
                String loadFileName = startOptions
                    .getStringOption(Options.loadGameFileName);

                if (loadFileName != null && loadFileName.length() > 0)
                {
                    GameLoading loader = new GameLoading();
                    String reasonForFailure = loader.loadGame(loadFileName);
                    if (reasonForFailure == null)
                    {
                        GameServerSide game = GameServerSide
                            .newGameServerSide(getWhatNextManager(),
                                serverOptions, loader.getVariant());
                        game.setWasLoaded(true);
                        serverOptions.clearPlayerInfo();
                        String webGameFlagFileName = startOptions
                            .getStringOption(Options.webFlagFileName);
                        startOptions.removeOption(Options.webFlagFileName);

                        if (webGameFlagFileName != null
                            && !webGameFlagFileName.equals(""))
                        {
                            whatNextManager.setWhatToDoNext(
                                WhatToDoNext.QUIT_ALL, false);
                            game.setFlagFilename(webGameFlagFileName);
                        }
                        game.loadGameAndWaitUntilOver(loader.getRoot());
                    }
                    else
                    {
                        if (wasInteractive)
                        {
                            JOptionPane.showMessageDialog(null,
                                "Error loading game: " + reasonForFailure,
                                "Loading game failed!",
                                JOptionPane.ERROR_MESSAGE);
                        }
                        LOGGER.severe("GameLoading returned failure: "
                            + reasonForFailure);
                    }
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
            else if (getWhatToDoNext() == WhatToDoNext.START_NET_CLIENT)
            {
                whatNextManager.decrementHowManyGamesLeft();

                // by default (if user does not say anything other when ending),
                // after that come back to NetClient dialog.
                if (oneClientRunOnly)
                {
                    whatNextManager.setWhatToDoNext(WhatToDoNext.QUIT_ALL,
                        false);
                }
                else
                {
                    whatNextManager.setWhatToDoNext(
                        WhatToDoNext.NET_CLIENT_DIALOG, false);
                }
                dontWait = startNetClient(startOptions);
            }

            else if (getWhatToDoNext() == WhatToDoNext.START_WEB_CLIENT)
            {
                // By default get back to Main dialog.
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);

                String hostname = startOptions
                    .getStringOption(Options.webServerHost);
                int port = startOptions.getIntOption(Options.webServerPort);
                String login = startOptions
                    .getStringOption(Options.webClientLogin);
                String password = null;
                new WebClient(getWhatNextManager(), hostname, port, login,
                    password);
            }

            // User clicked Quit in GetPlayers (this loop round),
            //  --or--
            // User selected File=>Quit in the game started from previous
            //  loop round.
            else if (getWhatToDoNext() == WhatToDoNext.QUIT_ALL)
            {
                // Nothing to do, loop will end.
                dontWait = true;
            }

            // What else??
            else
            {
                LOGGER.log(Level.SEVERE, "Unknown value '" + getWhatToDoNext()
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

            // ResourceLoader has static String telling the server; if not reset,
            // for a remote client closing while game is not over, he set next
            // to do to GetPlayers dialog, dialog wants to load Variant Readme,
            // resourceloader would fail.
            net.sf.colossus.util.StaticResourceLoader.resetDataServer();

            // DebugStuff.doCleanupStuff(false);

            // For Stresstesting (controlled by a system property):
            if (Options.isStresstest()
                && whatNextManager.getHowManyGamesLeft() > 0)
            {
                String loadFileName = startOptions
                    .getStringOption(Options.loadGameFileName);
                if (loadFileName != null)
                {
                    whatNextManager.setWhatToDoNext(WhatToDoNext.LOAD_GAME,
                        false);
                }
                else
                {
                    whatNextManager.setWhatToDoNext(WhatToDoNext.START_GAME,
                        false);
                }
            }
            else
            {
                serverOptions = null;
                netclientOptions = null;
            }

        } // end WHILE not QuitAll
    }

    private void runNetClientDialogAndWait()
    {
        Object mutex = new Object();
        new NetworkClientDialog(mutex, getWhatNextManager());

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
                whatNextManager.setWhatToDoNext(
                    WhatToDoNext.GET_PLAYERS_DIALOG, false);
            }
        }
        mutex = null;
    }

    private boolean startNetClient(Options startOptions)
    {
        boolean dontWait = false;

        String playerName = startOptions
            .getStringOption(Options.runClientPlayer);
        String hostname = startOptions.getStringOption(Options.runClientHost);
        int port = startOptions.getIntOption(Options.runClientPort);
        boolean spectator = startOptions.getOption(Options.runSpectatorClient);

        try
        {
            String type = Constants.aiPackage + Constants.network;
            Client.createClient(hostname, port, playerName, type,
                whatNextManager, null, false, false, true, spectator);
        }
        catch (Exception e)
        {
            LOGGER.warning("Creating the network CLIENT failed, reason: "
                + e.getMessage());
            dontWait = true;
        }

        // If starting net client succeeded, main() shall wait until
        // it ends. But if it fails, main() shall not wait, so that user
        // gets a new dialog immediately.
        return dontWait;
    }

    /* **********************************************************************
     *
     *                 The  m a i n ()  of the Start Class
     *
     * **********************************************************************
     */
    public static void main(String[] args)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        LOGGER.log(
            Level.INFO,
            "Start for Colossus version '"
                + BuildInfo.getFullBuildInfoString() + "' at "
                + dateFormat.format(new Date()));

        Start startObject = new Start(args);

        // Setup the various options objects, and loop until user
        // requests quitting the application
        startObject.setupAndLoop();

        // After there is no reference to the startObject, the GC should
        // be able to clean up the while object tree. We will see :)
        startObject = null;

        WelcomeDialog.disposeDialogIfNecessary();

        // ==================================================================
        // Application-ending related processing, mostly for debug purposes

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
            net.sf.colossus.util.InstanceTracker.printStatistics();
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

}
