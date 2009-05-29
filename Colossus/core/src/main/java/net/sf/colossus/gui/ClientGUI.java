package net.sf.colossus.gui;


import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.undo.UndoManager;

import net.sf.colossus.client.Client;
import net.sf.colossus.client.IClientGUI;
import net.sf.colossus.client.IOracle;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariant;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.webclient.WebClient;


@SuppressWarnings("serial")
public class ClientGUI implements IClientGUI
{
    private static final Logger LOGGER = Logger.getLogger(ClientGUI.class
        .getName());

    /* This is a number of seconds to wait for connection check
     * confirmation message before assuming connection is broken and
     * displaying a message telling so.
     */
    private final static int CONN_CHECK_TIMEOUT = 5;

    private Timer connectionCheckTimer;

    // TODO the naming of these classes is confusing, they should be clearly named
    // as dialogs
    private MasterBoard board;
    private StatusScreen statusScreen;
    private CreatureCollectionView caretakerDisplay;
    private MovementDie movementDie;
    private EngagementResults engagementResults;
    private AutoInspector autoInspector;
    private EventViewer eventViewer;
    private PreferencesWindow preferencesWindow;
    private LogWindow logWindow;
    private PickCarry pickCarryDialog = null;

    // For negotiation.  (And AI battle.)
    private Negotiate negotiate;
    private ReplyToProposal replyToProposal;

    private BattleBoard battleBoard;

    private WebClient webClient = null;
    private boolean startedByWebClient = false;

    /**
     * The object which handles what to do next when a game is going to end
     */
    private final WhatNextManager whatNextManager;

    /**
     * Stack of legion marker ID's, to allow multiple levels of undo for
     * splits, moves, and recruits.
     * (for battle actions, the Strings are not actually marker ID's,
     *  it's battle hex ID's there instead).
     *
     * TODO it would probably be good to have a full Command pattern here, similar
     * to Swing's {@link UndoManager} stuff. In the GUI client we could/should
     * probably just use that. A list of objects (which are mostly the string
     * identifiers of something) isn't that safe.
     */
    private final LinkedList<Object> undoStack = new LinkedList<Object>();

    private final List<GUIBattleChit> battleChits = new ArrayList<GUIBattleChit>();

    /** Information on the current moving legion. */
    private Legion mover;

    /** the parent frame for secondary windows */
    private JFrame secondaryParent = null;

    private int replayLastTurn = -1;
    private int replayMaxTurn = 0;

    // TODO change to enums...
    private int viewMode;
    private int recruitChitMode;

    private boolean gameOverMessageAlreadyShown = false;

    protected final Client client;

    // for things other GUI components need to inquire,
    // use the Oracle (on the long run, I guess there will be the
    // GameClientSide class behind it...)
    protected final IOracle oracle;

    // Per-client and per-player options.
    private final Options options;

    public ClientGUI(Client client, Options options,
        WhatNextManager whatNextMgr)
    {
        this.client = client;
        this.oracle = client;
        this.options = options;
        this.whatNextManager = whatNextMgr;
    }

    public void setStartedByWebClient(boolean byWebClient)
    {
        this.startedByWebClient = byWebClient;
    }

    public void setWebClient(WebClient wc)
    {
        this.webClient = wc;
    }

    public void setClientInWebClientNull()
    {
        if (webClient != null)
        {
            webClient.setGameClient(null);
            webClient = null;
        }
    }

    // TODO still needed?
    public MasterBoard getBoard()
    {
        return board;
    }

    public Client getClient()
    {
        return client;
    }

    public IOracle getOracle()
    {
        return client;
    }

    public Game getGame()
    {
        return client.getGame();
    }

    public IOptions getOptions()
    {
        return options;
    }

    private boolean isReplayOngoing()
    {
        return client.isReplayOngoing();
    }

    /*
     * If webclient is just hidden, bring it back;
     * if it had been used, ask whether to restore;
     * Otherwise just do nothing
     */
    public void handleWebClientRestore()
    {
        if (this.webClient != null)
        {
            // was only Hidden, so bring it up without asking
            this.webClient.setVisible(true);
        }
        else
        {
            // webclient never used (local game), or explicitly closed
            // - don't bother user with it
            // If he now said quit -- he probably wants quit.
            // if he now used close or new game, he can get to web client
            // from GetPlayers dialog.
        }
    }

    public void showWebClient()
    {
        if (this.webClient == null)
        {
            this.webClient = new WebClient(whatNextManager, null, -1, null,
                null);
            this.webClient.setGameClient(client);
        }
        else
        {
            this.webClient.setVisible(true);
        }
    }

    public void initBoard()
    {
        String viewModeName = options.getStringOption(Options.viewMode);
        viewMode = options.getNumberForViewMode(viewModeName);

        String rcMode = options
            .getStringOption(Options.showRecruitChitsSubmenu);

        // TODO this can probably be dropped by now.
        if (rcMode == null || rcMode.equals(""))
        {
            // not set: convert from old "showAllRecruitChits" option
            boolean showAll = options.getOption(Options.showAllRecruitChits);
            if (showAll)
            {
                rcMode = Options.showRecruitChitsAll;
            }
            else
            {
                rcMode = Options.showRecruitChitsStrongest;
            }
            // initialize new option
            options.setOption(Options.showRecruitChitsSubmenu, rcMode);
            // clean up obsolete option from cfg file
            options.removeOption(Options.showAllRecruitChits);
        }
        recruitChitMode = options.getNumberForRecruitChitSelection(rcMode);

        ensureEdtSetupClientGUI();

        if (startedByWebClient)
        {
            if (webClient != null)
            {
                webClient.notifyComingUp();
            }
        }
    }

    /**
     * Ensure that setupClientGUI() is run inside the EDT
     */
    private void ensureEdtSetupClientGUI()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            setupClientGUI();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        setupClientGUI();
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e);
            }
            catch (InterruptedException e2)
            {
                LOGGER.log(Level.SEVERE,
                    "Failed to run setupClientGUI with invokeAndWait(): ", e2);
            }

        }
    }

    /**
     * Called via ensureEdtSetupClientGUI() when server sends all clients
     * the initBoard command.
     */
    public void setupClientGUI()
    {
        /*
        disposeEventViewer();
        disposePreferencesWindow();
        disposeEngagementResults();
        disposeInspector();
        disposeCaretakerDisplay();
        disposeLogWindow();
        disposeMasterBoard();
        */

        int scale = options.getIntOption(Options.scale);
        if (scale == -1)
        {
            scale = 15;
            options.setOption(Options.scale, scale);
            options.saveOptions();
        }
        Scale.set(scale);

        board = new MasterBoard(client, this);
        initEventViewer();
        initShowEngagementResults();
        initPreferencesWindow();
        showOrHideAutoInspector(options.getOption(Options.showAutoInspector));
        showOrHideLogWindow(client, options.getOption(Options.showLogWindow));
        showOrHideCaretaker(options.getOption(Options.showCaretaker));

        setupGUIOptionListeners();
        syncCheckboxes();

        focusBoard();
    }

    public void setChosenDevice(GraphicsDevice chosen)
    {
        if (chosen != null)
        {
            secondaryParent = new JFrame(chosen.getDefaultConfiguration());
            disposeStatusScreen();
            updateStatusScreen();
            disposeCaretakerDisplay();
            boolean bval = options.getOption(Options.showCaretaker);
            showOrHideCaretaker(bval);
        }
    }

    private void ensureEdtNewBattleBoard()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            doNewBattleBoard();
        }
        else
        {
            // @TODO: use invokeLater() instead of invokeAndWait() ?
            //
            // Right now I don't dare to use invokeLater() - this way here
            // it preserves the execution order as it was without EDT,
            // but GUI stuff is one in EDT so we are safe from exceptions.
            Exception e = null;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        doNewBattleBoard();
                    }

                });
            }
            catch (InvocationTargetException e1)
            {
                e = e1;
            }
            catch (InterruptedException e2)
            {
                e = e2;
            }

            if (e != null)
            {
                String errorMessage = "Failed to run doNewBattleBoard with "
                    + "invokeAndWait(): ";
                LOGGER.log(Level.SEVERE, errorMessage, e);
            }
        }
    }

    public void actOnInitBattle()
    {
        ensureEdtNewBattleBoard();
    }

    private void doNewBattleBoard()
    {
        if (battleBoard != null)
        {
            LOGGER.warning("Old BattleBoard still in place? Disposing it.");
            battleBoard.dispose();
            battleBoard = null;
        }
        battleBoard = new BattleBoard(this, client.getBattleSite(), client
            .getAttacker(), client.getDefender());
    }

    public void setStrikeNumbers(BattleUnit striker,
        Set<BattleHex> targetHexes)
    {
        for (BattleHex targetHex : targetHexes)
        {
            GUIBattleChit targetChit = getGUIBattleChit(targetHex);
            BattleUnit target = targetChit.getBattleUnit();
            int strikeNr = client.getStrike().getStrikeNumber(striker, target);
            targetChit.setStrikeNumber(strikeNr);
        }
    }

    /** reset all strike numbers on chits */
    public void resetStrikeNumbers()
    {
        for (GUIBattleChit battleChit : getGUIBattleChits())
        {
            battleChit.setStrikeNumber(0);
            battleChit.setStrikeDice(0);
        }
    }

    // TODO create/dispose status screen only on request, in this call here
    // only call the update (if it exists)

    public void updateStatusScreen()
    {
        if (client.getNumPlayers() < 1)
        {
            // Called too early.
            return;
        }
        if (options.getOption(Options.showStatusScreen))
        {
            if (statusScreen != null)
            {
                statusScreen.updateStatusScreen();
            }
            else
            {
                statusScreen = new StatusScreen(getPreferredParent(), client,
                    options, client);
            }
        }
        else
        {
            // It seems board might be null in one AI client when reloading
            //  a game and forceViewBoard set.... so, added null check again
            if (board != null)
            {
                board.adjustCheckboxIfNeeded(Options.showStatusScreen, false);
                if (statusScreen != null)
                {
                    statusScreen.dispose();
                }
                this.statusScreen = null;
            }
        }

        // XXX Should be called somewhere else, just once.
        setupPlayerLabel();
    }

    boolean quitAlreadyTried = false;

    public void menuCloseBoard()
    {
        clearUndoStack();
        doSetWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG, false);
        client.disposeClientOriginated();
    }

    public void menuQuitGame()
    {
        // Note that if this called from webclient, webclient has already
        // beforehand called client to set webclient to null :)
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }

        // as a fallback/safety: if the close/dispose chain does not work,
        // on 2nd attempt directly do System.exit() so that user can somehow
        // get rid of the game "cleanly"...
        if (quitAlreadyTried)
        {
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "Arggh!! Seems the standard Quit procedure does not work.\n"
                    + "Doing System.exit() now the hard way.",
                "Proper quitting failed!", JOptionPane.INFORMATION_MESSAGE);

            System.exit(1);
        }
        quitAlreadyTried = true;

        doSetWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
        client.notifyServer();
    }

    // Used by MasterBoard and by BattleBoard
    void askNewCloseQuitCancel(JFrame frame, boolean fromBattleBoard)
    {
        String[] dialogOptions = new String[4];
        dialogOptions[0] = "New Game";
        dialogOptions[1] = "Quit";
        dialogOptions[2] = "Close";
        dialogOptions[3] = "Cancel";
        int answer = JOptionPane
            .showOptionDialog(
                frame,
                "Choose one of: Play another game, Quit, Close just this board, or Cancel",
                "Play another game?", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, dialogOptions,
                dialogOptions[3]);
        frame = null;
        if (answer == -1 || answer == 3)
        {
            return;
        }
        else
        {
            if (fromBattleBoard)
            {
                client.concede();
            }
        }
        if (answer == 0)
        {
            menuNewGame();
        }
        else if (answer == 1)
        {
            menuQuitGame();
        }
        else if (answer == 2)
        {
            client.disposeClientOriginated();
        }
    }

    /** When user requests it from File menu, this method here
     *  requests the server to send us a confirmation package,
     *  to confirm that connection is still alive and ok.
     */
    synchronized void checkServerConnection()
    {
        if (client.isSctAlreadyDown())
        {
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "No point to send check message - we know already that "
                    + " the socket connection is in 'down' state!",
                "Useless attempt to check connection",
                JOptionPane.INFORMATION_MESSAGE);

            return;
        }
        connectionCheckTimer = new Timer(1000 * CONN_CHECK_TIMEOUT,
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    timeoutAbortsConnectionCheck();
                }
            });
        connectionCheckTimer.start();

        LOGGER.info("Client for player " + getOwningPlayer().getName()
            + " checking server connection (sending request)");

        client.doCheckServerConnection();
    }

    public synchronized void serverConfirmsConnection()
    {
        LOGGER.info("Client for player " + getOwningPlayer().getName()
            + " received confirmation that connection is OK.");
        finishServerConnectionCheck(true);
    }

    public synchronized void timeoutAbortsConnectionCheck()
    {
        finishServerConnectionCheck(false);
    }

    /** Cleanup everything related to the serverConnectionCheck timer,
     *  and show a message telling whether it went ok or not.
     */
    private void finishServerConnectionCheck(boolean success)
    {
        if (connectionCheckTimer == null)
        {
            // race - the other one came nearly same time, and comes now
            // after the first one left this synchronized method.
            return;
        }
        if (connectionCheckTimer.isRunning())
        {
            connectionCheckTimer.stop();
        }
        connectionCheckTimer = null;
        if (success)
        {
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "Received confirmation from server - connection to "
                    + "server is ok!", "Connection check succeeded.",
                JOptionPane.INFORMATION_MESSAGE);
        }
        else
        {
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "Did not receive confirmation message from server within "
                    + CONN_CHECK_TIMEOUT + " seconds - connection to "
                    + "server is probably broken, or something hangs.",
                "Connection check failed!", JOptionPane.ERROR_MESSAGE);

        }
    }

    private void doSetWhatToDoNext(WhatToDoNext whatToDoNext,
        boolean triggerQuitTimer)
    {
        whatNextManager.setWhatToDoNext(whatToDoNext, triggerQuitTimer);
    }

    private void doSetWhatToDoNext(WhatToDoNext whatToDoNext, String loadFile)
    {
        whatNextManager.setWhatToDoNext(whatToDoNext, loadFile);
    }

    // Used by File=>Close and Window closing
    private void setWhatToDoNextForClose()
    {
        if (startedByWebClient)
        {
            doSetWhatToDoNext(WhatToDoNext.START_WEB_CLIENT, false);
        }
        else if (client.isRemote())
        {
            // Remote clients get back to Network Client dialog
            doSetWhatToDoNext(WhatToDoNext.NET_CLIENT_DIALOG, false);
        }
        else
        {
            doSetWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG, false);
        }
    }

    public void menuNewGame()
    {
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }
        setWhatToDoNextForClose();
        client.notifyServer();
    }

    public void menuLoadGame(String filename)
    {
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }
        doSetWhatToDoNext(WhatToDoNext.LOAD_GAME, filename);
        client.notifyServer();
    }

    void menuSaveGame(String filename)
    {
        if (client.isRemote())
        {
            // In practice this should never happen, because in remote
            // clients the File=>Save menu actions should not be visible
            // at all. But who knows...
            JOptionPane
                .showMessageDialog(
                    getMapOrBoardFrame(),
                    "The variable 'localServer' is null, which means you are "
                        + "playing with a remote client. How on earth did you manage "
                        + "to trigger a File=>Save action?\nAnyway, from a remote "
                        + "client I can't do File=>Save for you...",
                    "Save Game not available on remote client",
                    JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            client.locallyInitiateSaveGame(filename);
        }
    }

    private void setupPlayerLabel()
    {
        if (board != null)
        {
            board.setupPlayerLabel();
        }
    }

    private void focusMap()
    {
        if (battleBoard != null)
        {
            battleBoard.reqFocus();
        }
    }

    private void focusBoard()
    {
        board.reqFocus();
    }

    public void highlightEngagements()
    {
        if (isMyTurn())
        {
            focusBoard();
        }
        board.highlightEngagements();
    }

    private JFrame getPreferredParent()
    {
        if ((secondaryParent == null) && (board != null))
        {
            return board.getFrame();
        }
        return secondaryParent;
    }

    public int getViewMode()
    {
        return viewMode;
    }

    public int getRecruitChitMode()
    {
        return recruitChitMode;
    }

    public void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes)
    {
        if (recruitChitMode == Options.showRecruitChitsNumNone)
        {
            return;
        }

        board.addPossibleRecruitChits(legion, hexes);
    }

    /*
     * Trigger side effects after changing an option value.
     *
     * TODO now that there are listeners, many of the other classes could listen to the
     * options relevant to them instead of dispatching it all through the Client class.
     */

    private void setupGUIOptionListeners()
    {
        options.addListener(Options.antialias, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setAntialias(newValue);
                repaintAllWindows();
            }
        });
        options.addListener(Options.useOverlay, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setOverlay(newValue);
                if (board != null)
                {
                    board.repaintAfterOverlayChanged();
                }
            }
        });
        options.addListener(Options.showRecruitChitsSubmenu,
            new IOptions.Listener()
            {
                @Override
                public void stringOptionChanged(String optname,
                    String oldValue, String newValue)
                {
                    recruitChitMode = options
                        .getNumberForRecruitChitSelection(newValue);
                }
            });
        options.addListener(Options.noBaseColor, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                CreatureType.setNoBaseColor(newValue);
                net.sf.colossus.util.StaticResourceLoader.purgeImageCache();
                repaintAllWindows();
            }
        });
        options.addListener(Options.useColoredBorders, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIBattleChit.setUseColoredBorders(newValue);
                repaintAllWindows();
            }
        });
        options.addListener(Options.showCaretaker, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideCaretaker(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showLogWindow, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideLogWindow(client, newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showStatusScreen, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                updateStatusScreen();
            }
        });
        options.addListener(Options.showAutoInspector, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                showOrHideAutoInspector(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showEventViewer, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                eventViewerSetVisibleMaybe();
                syncCheckboxes();
            }
        });
        options.addListener(Options.viewMode, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                viewMode = options.getNumberForViewMode(newValue);
            }
        });
        options.addListener(Options.dubiousAsBlanks, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                autoInspectorSetDubiousAsBlanks(newValue);
            }
        });
        options.addListener(Options.showEngagementResults,
            new IOptions.Listener()
            {
                @Override
                public void booleanOptionChanged(String optname,
                    boolean oldValue, boolean newValue)
                {
                    engagementResultsMaybeShow();
                }
            });
        options.addListener(Options.favoriteLookFeel, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                setLookAndFeel(newValue);
            }
        });
        options.addListener(Options.scale, new IOptions.Listener()
        {
            // TODO check if we could use the intOptionChanged callback here
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                int scale = Integer.parseInt(newValue);
                if (scale > 0)
                {
                    Scale.set(scale);
                    rescaleAllWindows();
                }
            }
        });
    }

    /* Create the event viewer, so that it can collect data from beginning on.
     * EventViewer shows itself depending on whether the option for it is set.
     */
    private void initEventViewer()
    {
        if (eventViewer == null)
        {
            JFrame parent = getPreferredParent();
            eventViewer = new EventViewer(parent, options, client);
        }
    }

    public void eventViewerSetVisibleMaybe()
    {
        // if null: no board (not yet, or not at all) => no eventviewer
        if (eventViewer != null)
        {
            // Eventviewer takes care of showing/hiding itself
            eventViewer.setVisibleMaybe();
        }
    }

    public void autoInspectorSetDubiousAsBlanks(boolean newValue)
    {
        if (autoInspector != null)
        {
            autoInspector.setDubiousAsBlanks(newValue);
        }
    }

    public void engagementResultsMaybeShow()
    {
        // maybeShow decides by itself based on the current value
        // of the option whether to hide or show.
        // null if called too early by optionListener when loading options
        // TODO try change order: first create, then listeners/load options?
        if (engagementResults != null)
        {
            engagementResults.maybeShow();
        }
    }

    public void actOnTellLegionLocation(Legion legion, MasterHex hex)
    {
        // @TODO: this creates it every time, not only when necessary ?
        Marker marker = new Marker(3 * Scale.get(), legion.getMarkerId(),
            client);
        setMarker(legion, marker);

        if (!isReplayOngoing())
        {
            board.alignLegions(hex);
        }
    }

    /** Add the marker to the end of the list and to the LegionInfo.
    If it's already in the list, remove the earlier entry. */
    void setMarker(Legion legion, Marker marker)
    {
        board.setMarkerForLegion(legion, marker);
    }

    public void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex)
    {
        if (!isReplayOngoing())
        {
            eventViewerNewSplitEvent(turn, parent, child);
        }

        Marker marker = new Marker(3 * Scale.get(), child.getMarkerId(),
            client);
        setMarker(child, marker);

        if (isReplayOngoing())
        {
            replayTurnChange(turn);
        }
        else
        {
            board.alignLegions(hex);
        }

        if (client.isMyLegion(child))
        {
            board.clearRecruitedChits();
            board.clearPossibleRecruitChits();
            pushUndoStack(child.getMarkerId());
        }

    }

    public void actOnDidSplitPart2(MasterHex hex)
    {
        if (client.getTurnNumber() == 1 && isMyTurn())
        {
            board.enableDoneAction();
        }

        if (!isReplayOngoing())
        {
            board.alignLegions(hex);
            board.highlightTallLegions();
        }

    }

    public void actOnDoneWithMoves()
    {
        board.clearRecruitedChits();
        board.clearPossibleRecruitChits();
    }

    public void actOnDoneWithSplits()
    {
        board.clearRecruitedChits();
    }

    public void actOnDidRecruit(Legion legion, CreatureType recruit,
        List<CreatureType> recruiters, String reason)
    {
        board.addRecruitedChit(legion);
        board.highlightPossibleRecruitLegionHexes();

        if (eventViewer != null)
        {
            eventViewer.recruitEvent(legion.getMarkerId(), (legion)
                .getHeight(), recruit, recruiters, reason);
        }
    }

    public void actOnRemoveCreature(Legion legion, CreatureType type,
        String reason)
    {
        if (reason.equals(Constants.reasonUndidReinforce))
        {
            LOGGER
                .warning("removeCreature should not be called for undidReinforce!");
        }
        else if (reason.equals(Constants.reasonUndidRecruit))
        {
            // normal undidRecruit does not use this, but during loading a game
            // they appear as add- and removeCreature calls.
            LOGGER.info("removeCreature called for undidRecruit - ignored");
        }
        else
        {
            eventViewer.removeCreature(legion, type);
        }
    }

    public void actOnRemoveCreaturePart2(Legion legion)
    {
        if (!isReplayOngoing())
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }
    }

    public void actOnAddCreature(Legion legion, CreatureType creature,
        String reason)
    {
        if (!isReplayOngoing())
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }

        eventViewer.addCreature(legion.getMarkerId(), creature.getName(),
            reason);
    }

    public void boardActOnUndidSplit(Legion survivor, int turn)
    {
        if (isReplayOngoing())
        {
            replayTurnChange(turn);
        }
        else
        {
            board.alignLegions(survivor.getCurrentHex());
            board.highlightTallLegions();
        }
    }

    public void actOnUndidRecruitPart2(Legion legion,
        boolean wasReinforcement, int turnNumber)
    {
        int eventType = wasReinforcement ? RevealEvent.eventReinforce
            : RevealEvent.eventRecruit;
        board.cleanRecruitedChit((LegionClientSide)legion);
        board.highlightPossibleRecruitLegionHexes();

        eventViewer.undoEvent(eventType, legion.getMarkerId(), null,
            turnNumber);
    }

    public boolean chooseWhetherToTeleport()
    {
        String[] dialogOptions = new String[2];
        dialogOptions[0] = "Teleport";
        dialogOptions[1] = "Move Normally";
        int answer = JOptionPane.showOptionDialog(board, "Teleport?",
            "Teleport?", JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, dialogOptions,
            dialogOptions[1]);

        return (answer == JOptionPane.YES_OPTION);
    }

    public void actOnDidMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, boolean teleport, CreatureType teleportingLord,
        boolean splitLegionHasForcedMove)
    {
        if (teleport)
        {
            eventViewer.newCreatureRevealEvent(RevealEvent.eventTeleport,
                legion.getMarkerId(), legion.getHeight(), teleportingLord
                    .getName(),
                null, 0);
        }

        board.clearPossibleRecruitChits();
        board.alignLegions(startingHex);
        board.alignLegions(currentHex);
        board.highlightUnmovedLegions();
        board.repaint();
        if (client.isMyLegion(legion))
        {
            if (splitLegionHasForcedMove)
            {
                board.disableDoneAction("Split legion needs to move");
            }
            else
            {
                board.enableDoneAction();
            }
        }
    }

    public void actOnUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove,
        boolean didTeleport)
    {
        board.clearPossibleRecruitChits();
        board.alignLegions(formerHex);
        board.alignLegions(currentHex);
        board.highlightUnmovedLegions();
        board.repaint();
        if (isMyTurn())
        {
            if (isUndoStackEmpty())
            {
                board.disableDoneAction("At least one legion must move");
            }
            if (splitLegionHasForcedMove)
            {
                board.disableDoneAction("Split legion needs to move");
            }
        }

        if (didTeleport)
        {
            eventViewer.undoEvent(RevealEvent.eventTeleport, legion
                .getMarkerId(), null, client.getTurnNumber());
        }
    }

    public void actOnNextEngagement()
    {
        // TODO move condition to Client?
        if (client.getGameClientSide().findEngagements().isEmpty())
        {
            board.enableDoneAction();
        }
    }

    public void alignLegionsMaybe(Legion legion)
    {
        if (!isReplayOngoing())
        {
            board.alignLegions(legion.getCurrentHex());
        }
    }

    public void actOnRemoveLegion(Legion legion)
    {
        board.removeMarkerForLegion(legion);
    }

    public void actOnDoSummon()
    {
        highlightEngagements();
        board.repaint();
    }

    public void setLookAndFeel(String lfName)
    {
        try
        {
            UIManager.setLookAndFeel(lfName);
            UIManager.LookAndFeelInfo[] lnfInfos = UIManager
                .getInstalledLookAndFeels();
            boolean exist = false;
            for (LookAndFeelInfo lnfInfo : lnfInfos)
            {
                exist = exist || lnfInfo.getClassName().equals(lfName);
            }
            if (!exist)
            {
                UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(
                    UIManager.getLookAndFeel().getName(), lfName));
            }
            updateEverything();
            LOGGER.log(Level.FINEST, "Switched to Look & Feel: " + lfName);
            options.setOption(Options.favoriteLookFeel, lfName);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Look & Feel " + lfName + " not usable",
                e);
        }
    }

    public void updateEverything()
    {
        board.updateComponentTreeUI();
        board.pack();

        updateTreeAndPack(statusScreen);
        updateTreeAndPack(caretakerDisplay);
        updateTreeAndPack(preferencesWindow);
        repaintAllWindows();
    }

    private void updateTreeAndPack(Window window)
    {
        if (window != null)
        {
            SwingUtilities.updateComponentTreeUI(window);
            window.pack();
        }
    }

    public void replayTurnChange(int nowTurn)
    {
        assert board != null : "board is null in replayTurnChange!";

        if (board != null)
        {
            if (nowTurn != replayLastTurn)
            {
                board.updateReplayText(nowTurn, replayMaxTurn);
                replayLastTurn = nowTurn;
            }
        }
    }

    public void actOnTellReplay(int maxTurn)
    {
        if (isReplayOngoing())
        {
            replayMaxTurn = maxTurn;
            if (board != null)
            {
                board.setReplayMode();
                board.updateReplayText(0, replayMaxTurn);
            }
        }
        else
        {
            if (board != null)
            {
                board.recreateMarkers();
            }
        }
    }

    public void clearUndoStack()
    {
        undoStack.clear();
    }

    public Object popUndoStack()
    {
        return undoStack.removeFirst();
    }

    public void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    private boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }

    public void eventViewerCancelReinforcement(CreatureType recruit, int turnNr)
    {
        eventViewer.cancelReinforcement(recruit, turnNr);
    }

    public void eventViewerDefenderSetCreatureDead(CreatureType creature,
        int height)
    {
        eventViewer.defenderSetCreatureDead(creature, height);
    }

    public void eventViewerAttackerSetCreatureDead(CreatureType creature,
        int height)
    {
        eventViewer.attackerSetCreatureDead(creature.getName(), height);
    }

    public void eventViewerNewSplitEvent(int turn, Legion parent, Legion child)
    {
        eventViewer.newSplitEvent(turn, parent.getMarkerId(), parent
            .getHeight(), null, child.getMarkerId(), child.getHeight());
    }

    public void eventViewerUndoEvent(Legion splitoff, Legion survivor, int turn)
    {
        eventViewer.undoEvent(RevealEvent.eventSplit, survivor.getMarkerId(),
            splitoff.getMarkerId(), turn);
    }

    private void initPreferencesWindow()
    {
        if (preferencesWindow == null)
        {
            preferencesWindow = new PreferencesWindow(options, client);
        }
    }

    public void setPreferencesWindowVisible(boolean val)
    {
        if (preferencesWindow != null)
        {
            preferencesWindow.setVisible(val);
        }
    }

    public void showMarker(Marker marker)
    {
        if (autoInspector != null)
        {
            String markerId = marker.getId();
            Legion legion = client.getLegion(markerId);
            autoInspector.showLegion((LegionClientSide)legion, client
                .isMyLegion(legion));
        }
    }

    private void showOrHideCaretaker(boolean bval)
    {
        if (board == null)
        {
            LOGGER.warning("showOrHideCaretaker attempted but no board yet!");
            // No board yet, or no board at all - nothing to do.
            // Initial show will be done in initBoard.
            return;
        }

        if (bval)
        {
            if (caretakerDisplay == null)
            {
                caretakerDisplay = new CreatureCollectionView(
                    getPreferredParent(), client);
            }
        }
        else
        {
            disposeCaretakerDisplay();
        }
    }

    private void showOrHideAutoInspector(boolean bval)
    {
        JFrame parent = getPreferredParent();
        if (parent == null)
        {
            // No board yet, or no board at all - nothing to do.
            // Initial show will be done in initBoard.
            return;
        }
        if (bval)
        {
            if (autoInspector == null)
            {
                Variant variant = getGame().getVariant();
                IVariant ivariant = getClient();

                autoInspector = new AutoInspector(parent, options, viewMode,
                    options.getOption(Options.dubiousAsBlanks), variant,
                    ivariant);
            }
        }
        else
        {
            disposeInspector();
        }
    }

    public void showHexRecruitTree(GUIMasterHex hex)
    {
        if (autoInspector != null)
        {
            autoInspector.showHexRecruitTree(hex);
        }
    }

    public void didSummon(Legion summoner, Legion donor, CreatureType summon)
    {
        eventViewer.newCreatureRevealEvent(RevealEvent.eventSummon, donor
            .getMarkerId(), (donor).getHeight(), summon.getName(), summoner
            .getMarkerId(), (summoner).getHeight());
    }

    public void repaintBattleBoard()
    {
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    public void repaintAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        if (caretakerDisplay != null)
        {
            caretakerDisplay.repaint();
        }
        board.getFrame().repaint();
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    public void rescaleAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.rescale();
        }
        board.clearRecruitedChits();
        board.clearPossibleRecruitChits();
        board.rescale();
        if (battleBoard != null)
        {
            battleBoard.rescale();
        }
        repaintAllWindows();
    }

    public void disposeInspector()
    {
        if (autoInspector != null)
        {
            autoInspector.setVisible(false);
            autoInspector.dispose();
            autoInspector = null;
        }
    }

    public void updateCreatureCountDisplay()
    {
        if (caretakerDisplay != null && !isReplayOngoing())
        {
            caretakerDisplay.update();
        }
    }

    private void disposeMasterBoard()
    {
        if (board != null)
        {
            board.dispose();
            board = null;
        }
        else
        {
            LOGGER.warning("attempt to dispose board but board is null!");
        }
    }

    private void disposeBattleBoard()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
    }

    public void disposePickCarryDialog()
    {
        if (pickCarryDialog != null)
        {
            if (battleBoard != null)
            {
                battleBoard.unselectAllHexes();
            }
            pickCarryDialog.dispose();
            pickCarryDialog = null;
        }
    }

    private void disposeStatusScreen()
    {
        if (statusScreen != null)
        {
            statusScreen.dispose();
            statusScreen = null;
        }
    }

    private void disposeLogWindow()
    {
        if (logWindow != null)
        {
            logWindow.setVisible(false);
            logWindow.dispose();
            logWindow = null;
        }
    }

    private void disposeEventViewer()
    {
        if (eventViewer != null)
        {
            eventViewer.dispose();
            eventViewer = null;
        }
    }

    private void disposePreferencesWindow()
    {
        if (preferencesWindow != null)
        {
            preferencesWindow.dispose();
            preferencesWindow = null;
        }
    }

    void disposeEngagementResults()
    {
        engagementResults.dispose();
        engagementResults = null;

    }

    private void disposeCaretakerDisplay()
    {
        if (caretakerDisplay != null)
        {
            caretakerDisplay.dispose();
            caretakerDisplay = null;
        }
    }

    public void showNegotiate(Legion attacker, Legion defender)
    {
        negotiate = new Negotiate(this, attacker, defender);
    }

    public void respawnNegotiate()
    {
        if (negotiate != null)
        {
            negotiate.dispose();
        }
        negotiate = new Negotiate(this, client.getAttacker(), client
            .getDefender());
    }

    public void showConcede(Client client, Legion ally, Legion enemy)
    {
        Concede.concede(client, board.getFrame(), ally, enemy);
    }

    public void showFlee(Client client, Legion ally, Legion enemy)
    {
        Concede.flee(client, board.getFrame(), ally, enemy);
    }

    public void initShowEngagementResults()
    {
        JFrame parent = getPreferredParent();
        // no board at all, e.g. AI - nothing to do.
        if (parent == null)
        {
            return;
        }

        engagementResults = new EngagementResults(parent, client, options);
        engagementResults.maybeShow();
    }

    public void tellEngagement(Legion attacker, Legion defender, int turnNumber)
    {
        // remember for end of battle.
        tellEngagementResultsAttackerStartingContents = client
            .getLegionImageNames(attacker);
        tellEngagementResultsDefenderStartingContents = client
            .getLegionImageNames(defender);
        // TODO: I have the feeling that getLegionCertainties()
        //   does not work here.
        //   I always seem to get either ALL true or ALL false.
        tellEngagementResultsAttackerLegionCertainities = client
            .getLegionCreatureCertainties(attacker);
        tellEngagementResultsDefenderLegionCertainities = client
            .getLegionCreatureCertainties(defender);

        highlightBattleSite(client.getBattleSite());

        eventViewer.tellEngagement(attacker, defender, turnNumber);
    }

    void highlightBattleSite(MasterHex battleSite)
    {
        if (battleSite != null)
        {
            board.unselectAllHexes();
            board.selectHex(battleSite);
        }
    }

    public void actOnTellEngagementResults(Legion winner, String method,
        int points, int turns)
    {
        JFrame frame = getMapOrBoardFrame();
        if (frame == null)
        {
            return;
        }

        // If battle was resolved by negotiation, close remaining
        // dialogs (also on the one which did NOT press Accept)
        // as soon as possible, don't let them open until
        // user presses "Done" in Resolve Engagements or picks next
        // engagement. If it stays open and user picks something there,
        // it would cause trouble.
        cleanupNegotiationDialogs();

        eventViewer.tellEngagementResults(winner, method, turns);

        engagementResults
            .addData(winner, method, points, turns,
                tellEngagementResultsAttackerStartingContents,
                tellEngagementResultsDefenderStartingContents,
                tellEngagementResultsAttackerLegionCertainities,
                tellEngagementResultsDefenderLegionCertainities, client
                    .isMyTurn());
    }

    public void setMulliganOldRoll(int movementRoll)
    {
        eventViewer.setMulliganOldRoll(movementRoll);
    }

    public void tellWhatsHappening(String message)
    {
        board.setPhaseInfo(message);
    }

    public void tellMovementRoll(int roll)
    {
        eventViewer.tellMovementRoll(roll);

        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            movementDie = new MovementDie(4 * Scale.get(), MovementDie
                .getDieImageName(roll));

            if (board.getFrame().getExtendedState() != JFrame.ICONIFIED)
            {
                board.repaint();
            }
        }
    }

    private List<String> tellEngagementResultsAttackerStartingContents = null;
    private List<String> tellEngagementResultsDefenderStartingContents = null;
    private List<Boolean> tellEngagementResultsAttackerLegionCertainities = null;
    private List<Boolean> tellEngagementResultsDefenderLegionCertainities = null;

    /* pass revealed info to EventViewer and
     * additionally remember the images list for later, the engagement report
     */
    public void revealEngagedCreatures(Legion legion,
        final List<CreatureType> creatures, boolean isAttacker, String reason)
    {

        // in engagement we need to update the remembered list, too.
        if (isAttacker)
        {
            tellEngagementResultsAttackerStartingContents = client
                .getLegionImageNames(legion);
            // towi: should return a list of trues, right?
            // TODO if comment above is right: add assertion
            tellEngagementResultsAttackerLegionCertainities = client
                .getLegionCreatureCertainties(legion);
        }
        else
        {
            tellEngagementResultsDefenderStartingContents = client
                .getLegionImageNames(legion);
            // towi: should return a list of trues, right?
            // TODO if comment above is right: add assertion
            tellEngagementResultsDefenderLegionCertainities = client
                .getLegionCreatureCertainties(legion);
        }

        eventViewer.revealEngagedCreatures(creatures, isAttacker, reason);
    }

    public void eventViewerRevealCreatures(Legion legion,
        final List<CreatureType> creatures, String reason)
    {
        eventViewer.revealCreatures(legion, Legion
            .extractCreatureNames(creatures), reason);
    }

    private void showOrHideLogWindow(Client client, boolean show)
    {
        if (show)
        {
            if (logWindow == null)
            {
                // the logger with the empty name is parent to all loggers
                // and thus catches all messages
                logWindow = new LogWindow(client, Logger.getLogger(""));
            }
        }
        else
        {
            disposeLogWindow();
        }
    }

    // TODO why is syncCheckBoxes is called to sync *all* checkboxes in each
    // listener, and not only for the one changed option?

    /**
     * Ensure that Player menu checkboxes reflect the correct state.
     * Copied the TODO below from the interface where it's now removed...
     *
     * TODO let the checkboxes have their own listeners instead. Or even
     * better: use a binding framework.
     */

    public void syncCheckboxes()
    {
        Enumeration<String> en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            boolean value = options.getOption(name);
            board.adjustCheckboxIfNeeded(name, value);
        }
    }

    public void doAcquireAngel(Legion legion, List<CreatureType> recruits)
    {
        board.deiconify();
        new AcquireAngel(board.getFrame(), client, legion, recruits);
    }

    public void setBoardActive(boolean val)
    {
        board.setBoardActive(val);
    }

    public SummonInfo doPickSummonAngel(Legion legion,
        SortedSet<Legion> possibleDonors)
    {
        return SummonAngel.summonAngel(this, legion, possibleDonors);
    }

    public List<CreatureType> doPickSplitLegion(Legion parent,
        String childMarker)
    {
        return SplitLegion.splitLegion(this, parent, childMarker);
    }

    public boolean isPickCarryOngoing()
    {
        return pickCarryDialog != null;
    }

    public void doPickCarries(Client client, int carryDamage,
        Set<String> carryTargetDescriptions)
    {
        Set<BattleHex> carryTargetHexes = new HashSet<BattleHex>();
        for (String desc : carryTargetDescriptions)
        {
            carryTargetHexes.add(battleBoard.getBattleHexByLabel(desc
                .substring(desc.length() - 2)));
        }
        battleBoard.highlightPossibleCarries(carryTargetHexes);
        pickCarryDialog = new PickCarry(battleBoard, client, carryDamage,
            carryTargetDescriptions);
    }

    public PickCarry getPickCarryDialog()
    {
        return pickCarryDialog;
    }

    public void handlePickCarry(GUIBattleHex hex)
    {
        String hexLabel = "";
        if (hex != null)
        {
            hexLabel = hex.getHexModel().getLabel();
        }

        String choiceDesc = pickCarryDialog.findCarryChoiceForHex(hexLabel);
        // clicked on possible carry target
        if (choiceDesc != null)
        {
            pickCarryDialog.handleCarryToDescription(choiceDesc);
        }
        else
        {
            // enemy but not carryable to there
        }
    }

    public PlayerColor doPickColor(String playerName,
        List<PlayerColor> colorsLeft)
    {
        PlayerColor color = null;
        board.setPhaseInfo("Pick a color!");
        do
        {
            color = PickColor.pickColor(board.getFrame(), playerName,
                colorsLeft, options);
        }
        while (color == null);
        return color;
    }

    public String doPickMarker(Set<String> markersAvailable)
    {
        return PickMarker.pickMarker(board.getFrame(), client
            .getOwningPlayer(), markersAvailable, options);
    }

    public String doPickMarkerUntilGotOne(Set<String> markersAvailable)
    {
        String markerId = null;
        board.setPhaseInfo("Pick initial marker!");
        do
        {
            markerId = doPickMarker(markersAvailable);
        }
        while (markerId == null);

        return markerId;
    }

    public String doPickRecruit(Legion legion, String hexDescription)
    {
        List<CreatureType> recruits = client.findEligibleRecruits(legion,
            legion.getCurrentHex());

        return PickRecruit.pickRecruit(board.getFrame(), recruits,
            hexDescription, legion, client);
    }

    public String doPickRecruiter(List<String> recruiters,
        String hexDescription, Legion legion)
    {
        String recruiterName = null;

        // Even if PickRecruiter dialog is modal, this only prevents mouse
        // and keyboard into; but with pressing "D" one could still end the
        // recruiting phase which leaves game in inconsisten state...
        // So, forcibly really disable the Done action for that time.
        board.disableDoneAction("Finish 'Pick Recruiter' first");

        recruiterName = PickRecruiter.pickRecruiter(board.getFrame(),
            recruiters, hexDescription, legion, client);
        board.enableDoneAction();

        return recruiterName;
    }

    public EntrySide doPickEntrySide(MasterHex hex, Set<EntrySide> entrySides)
    {
        return PickEntrySide.pickEntrySide(board.getFrame(), hex, entrySides);
    }

    public CreatureType doPickLord(List<CreatureType> lords)
    {
        return PickLord.pickLord(options, board.getFrame(), lords);
    }

    public void doPickStrikePenalty(Client client, List<String> choices)
    {
        new PickStrikePenalty(battleBoard, this, choices);
    }

    public void tellProposal(String proposalString)
    {
        Proposal proposal = Proposal.makeFromString(proposalString, client
            .getGameClientSide());
        if (replyToProposal != null)
        {
            replyToProposal.dispose();
        }
        replyToProposal = new ReplyToProposal(board.getFrame(), this,
            getOwningPlayer().getName(), options, proposal);

    }

    public void cleanupNegotiationDialogs()
    {
        if (negotiate != null)
        {
            negotiate.dispose();
            negotiate = null;
        }
        if (replyToProposal != null)
        {
            replyToProposal.dispose();
            replyToProposal = null;
        }
    }

    public void actOnTurnOrPlayerChange(Client client, int turnNr,
        Player player)
    {
        clearUndoStack();
        cleanupNegotiationDialogs();
        eventViewer.turnOrPlayerChange(client, turnNr, player);
    }

    public void actOnSetupSplit()
    {
        // TODO probably this can be removed?
        if (isMyTurn())
        {
            // for debug purposes. We had a bug where legions remain
            // on the board even if player is dead. So, let's check
            // for this once per turn and clean up.
            validateLegions();
        }

        disposeMovementDie();
        board.setupSplitMenu();
        board.fullRepaint(); // Ensure that movement die goes away
        if (isMyTurn())
        {
            if (client.getTurnNumber() == 1)
            {
                board.disableDoneAction("Split legions in first round");
            }
            focusBoard();
            defaultCursor();

            // TODO I believe the code below is meant for the purpose:
            // "If no legions can be split, directly be done with Split
            //  phase, except if that is the result of the autoSplit"
            //  - so that one can review and undo.
            // But that does not make so much sense, as this is in the
            // "setupSplit" call, so the AI can't have done anything yet?

            if ((getOwningPlayer().getMarkersAvailable().size() < 1 || client
                .findTallLegionHexes(4).isEmpty())
                && !options.getOption(Options.autoSplit))
            {
                client.doneWithSplits();
            }
        }
        else
        {
            waitCursor();
        }

        updateStatusScreen();

    }

    private void validateLegions()
    {
        boolean foundProblem = false;

        for (Player p : client.getGameClientSide().getPlayers())
        {
            if (p.isDead())
            {
                for (Legion l : p.getLegions())
                {
                    LOGGER
                        .warning("Dead player " + p.getName() + " has "
                            + "still legion " + l.getMarkerId()
                            + ". Removing it.");
                    p.removeLegion(l);
                    foundProblem = true;
                }
            }
        }
        if (foundProblem)
        {
            LOGGER.info("Found legion(s) for dead player "
                + "- recreating markers");
            board.recreateMarkers();
        }
    }

    public void actOnSetupMuster()
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        board.setupMusterMenu();
        if (isMyTurn())
        {
            focusBoard();
            defaultCursor();
            if (!options.getOption(Options.autoRecruit)
                && client.getPossibleRecruitHexes().isEmpty())
            {
                client.doneWithRecruits();
            }
        }
        updateStatusScreen();
    }

    public void actOnSetupMove()
    {
        clearUndoStack();
        board.setupMoveMenu();
        // Force showing the updated movement die.
        board.repaint();
        if (isMyTurn())
        {
            defaultCursor();
        }
        updateStatusScreen();
    }

    public void actOnSetupFight()
    {
        clearUndoStack();
        board.setupFightMenu();
        updateStatusScreen();
    }

    public void actOnSetupBattleFight(BattlePhase battlePhase,
        int battleTurnNumber)
    {
        if (battleBoard != null)
        {
            battleBoard.setPhase(battlePhase);
            battleBoard.setTurn(battleTurnNumber);
            if (client.isMyBattlePhase())
            {
                focusMap();
                defaultCursor();
            }
            else
            {
                waitCursor();
            }
            battleBoard.setupFightMenu();
        }
        updateStatusScreen();
    }

    public void actOnSetupBattleMove()
    {
        if (battleBoard != null)
        {
            battleBoard.setPhase(client.getBattlePhase());
            battleBoard.setTurn(client.getBattleTurnNumber());
            if (client.isMyBattlePhase())
            {
                focusMap();
                defaultCursor();
                battleBoard.setupMoveMenu();
            }
        }
        updateStatusScreen();
    }

    public void actOnTellBattleMove(BattleHex startingHex, BattleHex endingHex)
    {
        if (battleBoard != null)
        {
            battleBoard.alignChits(startingHex);
            battleBoard.alignChits(endingHex);
            battleBoard.repaint();
            battleBoard.highlightMobileCritters();
        }
    }

    public void actOnSetupBattleRecruit()
    {
        if (battleBoard != null)
        {
            battleBoard.setPhase(client.getBattlePhase());
            battleBoard.setTurn(client.getBattleTurnNumber());
            if (client.isMyBattlePhase())
            {
                focusMap();
                battleBoard.setupRecruitMenu();
            }
        }
        updateStatusScreen();

    }

    public void actOnSetupBattleSummon()
    {
        if (battleBoard != null)
        {
            battleBoard.setPhase(client.getBattlePhase());
            battleBoard.setTurn(client.getBattleTurnNumber());
            if (client.isMyBattlePhase())
            {
                focusMap();
                battleBoard.setupSummonMenu();
                defaultCursor();
            }
            else
            {
                waitCursor();
            }
        }
        updateStatusScreen();
    }

    private void addBattleChit(GUIBattleChit battleChit)
    {
        battleChits.add(battleChit);
    }

    /**
     * Get a list of all GUIBattleChits (on the current BattleMap)
     * @return The list of GUIBattleChits
     */
    public List<GUIBattleChit> getGUIBattleChits()
    {
        return Collections.unmodifiableList(battleChits);
    }

    /**
     * Find all GUIBattleChits that occupy a specified hex
     * Note that this can be several for the offboard position(s)
     *
     * @param hex The hex to give Chits for
     * @return A List of GUIBattleChits
     */
    public List<GUIBattleChit> getGUIBattleChitsInHex(final BattleHex hex)
    {
        return CollectionHelper.selectAsList(battleChits,
            new Predicate<GUIBattleChit>()
            {
                public boolean matches(GUIBattleChit battleChit)
                {
                    return hex.equals(battleChit.getBattleUnit()
                        .getCurrentHex());
                }
            }
        );
    }

    public GUIBattleChit getGUIBattleChit(BattleHex hex)
    {
        for (GUIBattleChit battleChit : getGUIBattleChits())
        {
            if (hex.equals(battleChit.getBattleUnit().getCurrentHex()))
            {
                return battleChit;
            }
        }
        return null;
    }

    public void actOnPlaceNewChit(String imageName,
        BattleUnit battleUnit, BattleHex hex)
    {
        Legion legion = battleUnit.getLegion();
        PlayerColor playerColor = legion.getPlayer().getColor();

        GUIBattleChit battleChit = new GUIBattleChit(5 * Scale.get(),
            imageName, battleUnit.isDefender(), playerColor, getClient(),
            battleUnit);

        addBattleChit(battleChit);
        // TODO is the "if ( != null)" still needed?
        if (battleBoard != null)
        {
            battleBoard.alignChits(hex);
            // Make sure map is visible after summon or muster.
            focusMap();
        }
    }

    private String getBattleUnitDescription(BattleCritter battleUnit)
    {
        if (battleUnit == null)
        {
            return "";
        }
        BattleHex hex = battleUnit.getCurrentHex();
        return battleUnit.getCreatureType().getName() + " in "
            + hex.getDescription();
    }

    public void actOnTellStrikeResults(boolean wasCarry, int strikeNumber,
        List<String> rolls,BattleCritter striker, BattleCritter target)
    {
        if (battleBoard != null)
        {
            if (!wasCarry)
            {
                battleBoard.addDiceResults(getBattleUnitDescription(striker),
                    getBattleUnitDescription(target), strikeNumber, rolls);
            }
            battleBoard.unselectAllHexes();
        }
    }

    public void highlightCrittersWithTargets()
    {
        if (battleBoard != null)
        {
            battleBoard.highlightCrittersWithTargets();
        }
    }

    public void actOnApplyCarries(BattleHex hex)
    {
        if (battleBoard != null)
        {
            battleBoard.unselectHex(hex);
            battleBoard.repaint();
        }
    }

    public void actOnCleanupBattle()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
        battleChits.clear();
    }

    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            getClient().undoRecruit(client.getLegion(markerId));
        }
    }

    public void undoRecruit(Legion legion)
    {
        if (undoStack.contains(legion.getMarkerId()))
        {
            undoStack.remove(legion.getMarkerId());
        }
        getClient().undoRecruit(legion);
    }

    void undoAllSplits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastSplit();
        }
    }

    void undoLastSplit()
    {
        if (!isUndoStackEmpty())
        {
            String splitoffId = (String)popUndoStack();
            client.undoSplit(client.getLegion(splitoffId));
        }
    }

    public void informSplitRequiredFirstRound()
    {
        // must split in first turn - Done not allowed now
        if (board != null && isMyTurn())
        {
            board.disableDoneAction("Split required in first round");
        }
    }

    void undoLastMove()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            getClient().undoMove(client.getLegion(markerId));
        }
    }

    public void undoLastBattleMove()
    {
        if (!isUndoStackEmpty())
        {
            String hexLabel = (String)popUndoStack();
            BattleHex hex = battleBoard.getBattleHexByLabel(hexLabel);
            getClient().undoBattleMove(hex);
        }
    }

    public void undoAllBattleMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastBattleMove();
        }
    }

    public void undoAllMoves()
    {
        board.clearRecruitedChits();
        board.clearPossibleRecruitChits();
        while (!isUndoStackEmpty())
        {
            undoLastMove();
        }
    }

    public void undoAllRecruits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastRecruit();
        }
    }

    public void defaultCursor()
    {
        board.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public void waitCursor()
    {
        board.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void doCleanupGUI()
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            cleanupGUI();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        cleanupGUI();
                    }
                });
            }
            catch (InterruptedException e)
            {/* ignore */
            }
            catch (InvocationTargetException e2)
            {/* ignore */
            }
        }
    }

    private void disposeMovementDie()
    {
        movementDie = null;
    }

    MovementDie getMovementDie()
    {
        return movementDie;
    }

    private void cleanupGUI()
    {
        try
        {
            disposeInspector();
            disposeCaretakerDisplay();
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE,
                "During disposal of Inspector and Caretaker: " + e.toString(),
                e);
        }

        client.cleanupBattle();
        disposeLogWindow();
        disposeMovementDie();
        disposeStatusScreen();
        disposeEventViewer();
        disposePreferencesWindow();
        disposeEngagementResults();
        disposeBattleBoard();
        disposeMasterBoard();

        // Options must be saved after all satellites are disposed,
        // because they store their location and state (enabled or not, e.g.)
        // in disposal.
        options.saveOptions();

        this.secondaryParent = null;

        client.doAdditionalCleanup();
    }

    /**
     *  Update Board and Status screen to reflect the new game over state.
     *  Show the game over message, or store it to be shown later.
     *  If dispose will follow soon, don't show message immediately
     *  (to avoid having the user to have click two boxes), instead store
     *  it for later to be shown then together with the dispose dialog.
     *
     *  @param message         The message ("XXXX wins", or "Draw")
     *  @param disposeFollows  If true, server will send a dispose message soon
     */
    public void actOnTellGameOver(String message, boolean disposeFollows)
    {
        if (webClient != null)
        {
            webClient.tellGameEnds();
        }

        if (statusScreen != null)
        {
            statusScreen.repaint();
        }
        defaultCursor();
        board.setGameOverState(message);
        if (disposeFollows)
        {
            // tell it later, together with the immediately following
            // server closed connection message
        }
        else
        {
            // show right away. Connection closed might come or not.
            showMessageDialogAndWait(message);
        }
    }

    String getMessage()
    {
        return this.message;
    }

    String message = "";

    public void showMessageDialogAndWait(String message)
    {
        // Don't bother showing messages to AI players.
        if (getOwningPlayer().isAI())
        {
            LOGGER.info("Message for AI player " + getOwningPlayer().getName()
                + ": " + message);
            return;
        }

        if (SwingUtilities.isEventDispatchThread())
        {
            doShowMessageDialog(message);
        }
        else
        {
            this.message = message;
            try
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                        doShowMessageDialog(getMessage());
                    }
                });
            }
            catch (InterruptedException e)
            {/* ignore */
            }
            catch (InvocationTargetException e2)
            {/* ignore */
            }
        }
    }

    void doShowMessageDialog(String message)
    {
        // For humans in autoplay do not show messages...
        if (options.getOption(Options.autoPlay))
        {
            // ... suppress any other messages than the game over message ...
            String goMessage = getClient().getGame().getGameOverMessage();
            if ((goMessage != null && message.contains(goMessage))
            // but suppress even that if autoQuit is on
                // (=> remote stresstest)
                && !options.getOption(Options.autoQuit))
            {
                // go on to showing
            }
            else
            {
                // do not show it, return instead.
                return;
            }
        }
        JFrame frame = getMapOrBoardFrame();
        if (frame != null)
        {
            JOptionPane.showMessageDialog(frame, message);
        }
    }

    // called by WebClient
    public void doConfirmAndQuit()
    {
        // Board does the "Really Quit?" confirmation and initiates
        // then (if user confirmed) the disposal of everything.
        board.doQuitGameAction();
    }

    public void closePerhapsWithMessage()
    {
        defaultCursor();
        board.setServerClosedMessage(client.getGame().isGameOver());

        String dialogMessage = null;
        String dialogTitle = null;

        if (client.getGame().isGameOver())
        {
            if (!gameOverMessageAlreadyShown)
            {
                // don't show again!
                dialogMessage = "Game over: "
                    + client.getGame().getGameOverMessage() + "!\n\n"
                    + "(connection closed from server side)";
                gameOverMessageAlreadyShown = true;
                dialogTitle = "Game Over: Server closed connection";
            }
            else
            {
                dialogMessage = "Connection now closed from server side.";
                dialogTitle = "Game Over: server closed connection";
            }
        }
        else
        {
            dialogMessage = "Connection to server unexpectedly lost?";
            dialogTitle = "Server closed connection";
        }

        JOptionPane.showMessageDialog(getMapOrBoardFrame(), dialogMessage,
            dialogTitle, JOptionPane.INFORMATION_MESSAGE);
    }

    private JFrame getMapOrBoardFrame()
    {
        JFrame frame = null;
        if (battleBoard != null)
        {
            frame = battleBoard;
        }
        else if (board != null)
        {
            frame = board.getFrame();
        }
        return frame;
    }

    // From other GUI components:
    void negotiateCallback(Proposal proposal, boolean respawn)
    {
        getClient().negotiateCallback(proposal, respawn);
    }

    // All kind of other GUI components might need this, too.
    public Player getOwningPlayer()
    {
        return client.getOwningPlayer();
    }

    public String getOwningPlayerName()
    {
        return getOwningPlayer().getName();
    }

    public boolean isMyTurn()
    {
        return client.isMyTurn();
    }

    public Legion getMover()
    {
        return mover;
    }

    public void setMover(Legion legion)
    {
        this.mover = legion;
    }

    public boolean doMove(MasterHex hex)
    {
        return client.doMove(mover, hex);
    }

    public void removeBattleChit(BattleUnit battleUnit)
    {
        for (Iterator<GUIBattleChit> iterator = battleChits.iterator(); iterator
            .hasNext();)
        {
            GUIBattleChit chit = iterator.next();
            if (chit.getBattleUnit().equals(battleUnit))
            {
                iterator.remove();
            }
        }
    }

}
