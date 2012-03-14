package net.sf.colossus.gui;


import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import net.sf.colossus.client.GameClientSide;
import net.sf.colossus.client.IClientGUI;
import net.sf.colossus.client.IOracle;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.common.WhatNextManager;
import net.sf.colossus.common.WhatNextManager.WhatToDoNext;
import net.sf.colossus.game.BattleCritter;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.util.CollectionHelper;
import net.sf.colossus.util.Predicate;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;
import net.sf.colossus.webclient.WebClient;


@SuppressWarnings("serial")
public class ClientGUI implements IClientGUI, GUICallbacks
{
    private static final Logger LOGGER = Logger.getLogger(ClientGUI.class
        .getName());

    /* This is a number of seconds to wait for connection check
     * confirmation message before assuming connection is broken and
     * displaying a message telling so.
     */
    private final static int CONN_CHECK_TIMEOUT = 5;

    private final Object connectionCheckMutex = new Object();

    private Timer connectionCheckTimer;

    private long lastConnectionCheckPackageSent = -1;

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
    private ConnectionLogWindow connectionLogWindow;
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

    private final LinkedList<PendingMove> pendingMoves = new LinkedList<PendingMove>();
    private final Set<MasterHex> pendingMoveHexes = new HashSet<MasterHex>();
    private boolean recoveredFromMoveNak = false;

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
    private int legionMoveConfirmationMode;
    private int nextSplitClickMode;

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

    public boolean getStartedByWebClient()
    {
        return this.startedByWebClient;
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

    public boolean hasBoard()
    {
        return board != null;
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

    public GameClientSide getGameClientSide()
    {
        return (GameClientSide)client.getGame();
    }

    public IOptions getOptions()
    {
        return options;
    }

    boolean isReplayOngoing()
    {
        return getClient().isReplayOngoing();
    }

    boolean isRedoOngoing()
    {
        return getClient().isRedoOngoing();
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

        String mcMode = options
            .getStringOption(Options.legionMoveConfirmationSubMenu);
        if (mcMode == null || mcMode.equals(""))
        {
            mcMode = Options.legionMoveConfirmationNoUnvisitedMove;
            // initialize new option
            options.setOption(Options.legionMoveConfirmationSubMenu, mcMode);
            // clean up obsolete option from cfg file
            options.removeOption(Options.confirmNoMove);
        }

        legionMoveConfirmationMode = options
            .getNumberForLegionMoveConfirmation(mcMode);

        String nextSplitMode = options
            .getStringOption(Options.nextSplitSubMenu);
        if (nextSplitMode == null || nextSplitMode.equals(""))
        {
            nextSplitMode = Options.nextSplitLeftClick;
            // initialize new option
            options.setOption(Options.nextSplitSubMenu, nextSplitMode);
            // clean up obsolete option from cfg file
        }

        nextSplitClickMode = options.getNumberForNextSplit(nextSplitMode);

        ensureEdtSetupClientGUI();

        if (startedByWebClient)
        {
            if (webClient != null)
            {
                webClient.notifyComingUp(true);
            }
        }
    }

    public void actOnGameStartingFailed()
    {
        if (webClient != null)
        {
            webClient.notifyComingUp(false);
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
        showOrHideLogWindow(options.getOption(Options.showLogWindow));
        showOrHideConnectionLogWindow(options
            .getOption(Options.showConnectionLogWindow));
        showOrHideCaretaker(options.getOption(Options.showCaretaker));

        setupGUIOptionListeners();
        syncCheckboxes();

        board.maybeRequestFocusAndToFront();
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
        battleBoard = new BattleBoard(this, getGame().getEngagement());
    }

    public void setStrikeNumbers(BattleUnit striker, Set<BattleHex> targetHexes)
    {
        for (BattleHex targetHex : targetHexes)
        {
            GUIBattleChit targetChit = getGUIBattleChit(targetHex);
            BattleUnit target = targetChit.getBattleUnit();
            int strikeNr = getGame().getBattleStrike().getStrikeNumber(
                striker, target);
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
                statusScreen = new StatusScreen(getPreferredParent(), this,
                    options);
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

    // Used now only by MasterBoard
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
                return;
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
    void checkServerConnection()
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

        Runnable checker = new Runnable()
        {
            public void run()
            {
                synchronized (connectionCheckMutex)
                {
                    initiateConnectionCheck();
                }
            }
        };
        new Thread(checker).start();
    }

    private void initiateConnectionCheck()
    {
        connectionCheckTimer = new Timer(1000 * CONN_CHECK_TIMEOUT,
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    timeoutAbortsConnectionCheck();
                }
            });
        lastConnectionCheckPackageSent = new Date().getTime();
        connectionCheckTimer.start();

        LOGGER.info("Client for player " + getOwningPlayer().getName()
            + " checking server connection (sending request)");
        client.doCheckServerConnection();
    }

    public void serverConfirmsConnection()
    {
        synchronized (connectionCheckMutex)
        {
            LOGGER.info("Client for player " + getOwningPlayer().getName()
                + " received confirmation that connection is OK.");
            finishServerConnectionCheck(true);
        }
    }

    public void timeoutAbortsConnectionCheck()
    {
        synchronized (connectionCheckMutex)
        {
            LOGGER.info("Client for player " + getOwningPlayer().getName()
                + " - timeout reached, stopping now to wait for the reply.");
            finishServerConnectionCheck(false);
        }
    }

    /** Cleanup everything related to the serverConnectionCheck timer,
     *  and show a message telling whether it went ok or not.
     *
     *  Called by either serverConfirmsConnection() or
     *  timeoutAbortsConnectionCheck(), which both synchronize on the
     *  connectionCheckMutex.
     *
     */
    private void finishServerConnectionCheck(boolean success)
    {
        if (connectionCheckTimer == null)
        {
            // race - the other one came nearly same time, and comes now
            // after the first one left this synchronize'd method.
            return;
        }
        if (connectionCheckTimer.isRunning())
        {
            connectionCheckTimer.stop();
        }
        connectionCheckTimer = null;
        if (success)
        {
            long responseReceivedTime = new Date().getTime();
            long roundTripTime = responseReceivedTime
                - lastConnectionCheckPackageSent;
            JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                "Received confirmation from server after " + roundTripTime
                        + " ms - connection to "
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

    public void highlightEngagements()
    {
        if (isMyTurn())
        {
            board.maybeRequestFocusAndToFront();
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

    public int getLegionMoveConfirmationMode()
    {
        return legionMoveConfirmationMode;
    }

    public int getNextSplitClickMode()
    {
        return nextSplitClickMode;
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
        GUIHex.setAntialias(options.getOption(Options.antialias));
        options.addListener(Options.antialias, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setAntialias(newValue);
                options.setOption(Options.antialias, newValue);
                repaintAllWindows();
            }
        });

        GUIHex.setOverlay(options.getOption(Options.useOverlay));
        options.addListener(Options.useOverlay, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIHex.setOverlay(newValue);
                options.setOption(Options.useOverlay, newValue);
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

        options.addListener(Options.legionMoveConfirmationSubMenu,
            new IOptions.Listener()
            {
                @Override
                public void stringOptionChanged(String optname,
                    String oldValue, String newValue)
                {
                    legionMoveConfirmationMode = options
                        .getNumberForLegionMoveConfirmation(newValue);
                }
            });

        options.addListener(Options.nextSplitSubMenu, new IOptions.Listener()
        {
            @Override
            public void stringOptionChanged(String optname, String oldValue,
                String newValue)
            {
                nextSplitClickMode = options.getNumberForNextSplit(newValue);
            }
        });

        CreatureType.setNoBaseColor(options.getOption(Options.noBaseColor));
        options.addListener(Options.noBaseColor, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                CreatureType.setNoBaseColor(newValue);
                options.setOption(Options.noBaseColor, newValue);
                net.sf.colossus.util.StaticResourceLoader.purgeImageCache();
                repaintAllWindows();
            }
        });

        GUIBattleChit.setUseColoredBorders(options
            .getOption(Options.useColoredBorders));
        options.addListener(Options.useColoredBorders, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                GUIBattleChit.setUseColoredBorders(newValue);
                options.setOption(Options.useColoredBorders, newValue);
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
                showOrHideLogWindow(newValue);
                syncCheckboxes();
            }
        });
        options.addListener(Options.showConnectionLogWindow,
            new IOptions.Listener()
            {
                @Override
                public void booleanOptionChanged(String optname,
                    boolean oldValue, boolean newValue)
                {
                    showOrHideConnectionLogWindow(newValue);
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
        Marker marker = new Marker(legion, 3 * Scale.get(), legion
            .getLongMarkerId(), client, (client != null));
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
        // TODO move if block to eventviewer itself?
        // Not during replay, but during redo:
        if (!isReplayOngoing() || isRedoOngoing())
        {
            eventViewerNewSplitEvent(turn, parent, child);
        }

        Marker marker = new Marker(child, 3 * Scale.get(),
                child.getLongMarkerId(), client, (client != null));
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
        postRecruitStuff(legion);

        board.addRecruitedChit(legion);
        board.highlightPossibleRecruitLegionHexes();

        if (eventViewer != null)
        {
            eventViewer.recruitEvent(legion, recruit, recruiters, reason);
        }
    }

    /**
     * Do what is needed after recruit (or mark as skip recruit):
     * push to undo stack, update legions left to muster, hightlight remaining ones,
     *
     * @param legion
     */
    private void postRecruitStuff(Legion legion)
    {
        if (client.isMyLegion(legion))
        {
            pushUndoStack(legion.getMarkerId());
            if (!getGameClientSide().isBattleOngoing())
            {
                board.updateLegionsLeftToMusterText();
            }
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
            eventViewer.removeCreature(legion, type, reason);
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
        board.actOnEditLegionMaybe(legion);
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

        board.actOnEditLegionMaybe(legion);

        eventViewer.addCreature(legion, creature, reason);
    }

    public void actOnUndidSplit(Legion survivor, int turn)
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

    public void actOnUndidRecruitPart(Legion legion, boolean wasReinforcement,
        int turnNumber)
    {
        int eventType = wasReinforcement ? RevealEvent.eventReinforce
            : RevealEvent.eventRecruit;
        board.cleanRecruitedChit((LegionClientSide)legion);
        board.highlightPossibleRecruitLegionHexes();
        if (client.isMyLegion(legion))
        {
            if (!wasReinforcement)
            {
                board.updateLegionsLeftToMusterText();
            }
        }
        eventViewer.undoEvent(eventType, legion, null, turnNumber);
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
                legion, teleportingLord, null);
        }

        if (client.isMyLegion(legion))
        {
            setMoveCompleted(legion, startingHex, currentHex);
            updatePendingText();
        }
        board.clearPossibleRecruitChits();
        board.alignLegions(startingHex);
        board.alignLegions(currentHex);
        board.highlightUnmovedLegions();
        board.repaint();
        if (client.isMyLegion(legion))
        {
            pushUndoStack(legion.getMarkerId());
            if (splitLegionHasForcedMove)
            {
                board.disableDoneAction("Split legion needs to move");
            }
            else
            {
                board.enableDoneAction();
            }
            board.updateLegionsLeftToMoveText(true);
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
            else if (splitLegionHasForcedMove)
            {
                board.disableDoneAction("Split legion needs to move");
            }
            else
            {
                board.enableDoneAction();
            }
            board.updateLegionsLeftToMoveText(true);
        }

        if (didTeleport)
        {
            eventViewer.undoEvent(RevealEvent.eventTeleport, legion, null,
                client.getTurnNumber());
        }
    }

    public void actOnNoMoreEngagements()
    {
        board.setPhaseInfo("Press \"Done\" to end the engagements phase");
        board.enableDoneAction();
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
            // Switching to replay mode
            replayMaxTurn = maxTurn;
            if (board != null)
            {
                board.setReplayMode();
                board.updateReplayText(0, replayMaxTurn);
            }
        }
        else
        {
            // Replay mode now over
            if (board != null)
            {
                board.recreateMarkers();
            }
        }
    }

    public void actOnTellRedoChange()
    {
        // Nothing to do right now (was needed temporarily)
    }

    private void clearUndoStack()
    {
        undoStack.clear();
    }

    private Object popUndoStack()
    {
        return undoStack.removeFirst();
    }

    void pushUndoStack(Object object)
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

    public void eventViewerSetCreatureDead(BattleUnit battleUnit)
    {
        eventViewer.setCreatureDead(battleUnit);
    }

    public void eventViewerNewSplitEvent(int turn, Legion parent, Legion child)
    {
        eventViewer.newSplitEvent(turn, parent, null, child);
    }

    public void eventViewerUndoEvent(Legion splitoff, Legion survivor, int turn)
    {
        eventViewer
            .undoEvent(RevealEvent.eventSplit, survivor, splitoff, turn);
    }

    public void setPreferencesCheckBoxValue(String name, boolean value)
    {
        preferencesWindow.setCheckBoxValue(name, value);
    }

    public void setPreferencesRadioButtonValue(String name, boolean value)
    {
        preferencesWindow.setRadioButtonValue(name, value);
    }

    private void initPreferencesWindow()
    {
        if (preferencesWindow == null)
        {
            preferencesWindow = new PreferencesWindow(options, this);
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
                    getPreferredParent(), this);
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
                autoInspector = new AutoInspector(parent, options, viewMode,
                    options.getOption(Options.dubiousAsBlanks), variant, this);
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
        eventViewer.newCreatureRevealEvent(RevealEvent.eventSummon, donor, summon, summoner);
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
        // highlight legions that could do something,
        // and e.g. in movephase, set mover to null
        board.actOnMisclick();
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

    private void disposeConnectionLogWindow()
    {
        if (connectionLogWindow != null)
        {
            connectionLogWindow.setVisible(false);
            connectionLogWindow.dispose();
            connectionLogWindow = null;
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
        board.clearDefenderFlee();
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
        board.clearDefenderFlee();
        Concede.concede(this, board.getFrame(), ally, enemy);
        myTurnNotificationActions(ally);
    }

    public void showFlee(Client client, Legion ally, Legion enemy)
    {
        Concede.flee(this, board.getFrame(), ally, enemy);
        myTurnNotificationActions(ally);
    }

    private void myTurnNotificationActions(Legion ally)
    {
        if (getGame().getDefender().equals(ally))
        {
            if (options.getOption(Options.turnStartBeep))
            {
                board.getToolkit().beep();
            }
            if (options.getOption(Options.turnStartToFront))
            {
                board.getFrame().toFront();
            }
        }
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

        board.updateEngagementsLeftText();
        board.clearEngagingPending();
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

        // just in case, make sure this get cleared. Might still be set
        // if opponent decided to flee.
        board.clearDefenderFlee();

        eventViewer.tellEngagementResults(winner, method, turns);

        engagementResults
            .addData(winner, method, points, turns,
                tellEngagementResultsAttackerStartingContents,
                tellEngagementResultsDefenderStartingContents,
                tellEngagementResultsAttackerLegionCertainities,
                tellEngagementResultsDefenderLegionCertainities, client
                    .isMyTurn());
    }

    public void actOnEngagementCompleted()
    {
        board.updateEngagementsLeftText();
    }

    public void setMulliganOldRoll(int movementRoll)
    {
        eventViewer.setMulliganOldRoll(movementRoll);
    }

    public void tellWhatsHappening(String message)
    {
        board.setPhaseInfo(message);
    }

    public void actOnTellMovementRoll(int roll)
    {
        // TODO move if block to eventviewer itself?
        // Not during replay, but during redo:
        if (!isReplayOngoing() || isRedoOngoing())
        {
            eventViewer.tellMovementRoll(roll);
        }

        board.setupTitleForMovementRoll(roll);

        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            movementDie = new MovementDie(4 * Scale.get(), MovementDie
                .getDieImageName(roll));
            // TODO why do we not repaint if iconified?
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
        eventViewer.revealCreatures(legion, creatures, reason);
    }

    private void showOrHideLogWindow(boolean show)
    {
        if (show)
        {
            if (logWindow == null)
            {
                // the logger with the empty name is parent to all loggers
                // and thus catches all messages
                logWindow = new LogWindow(options, Logger.getLogger(""));
            }
        }
        else
        {
            disposeLogWindow();
        }
    }

    private void showOrHideConnectionLogWindow(boolean show)
    {
        if (show)
        {
            if (connectionLogWindow == null)
            {
                // the logger with the empty name is parent to all loggers
                // and thus catches all messages
                connectionLogWindow = new ConnectionLogWindow(options);
            }
            else
            {
                connectionLogWindow.setVisible(true);
            }
        }
        else
        {
            if (connectionLogWindow != null)
            {
                connectionLogWindow.setVisible(false);
            }

        }
    }

    public void appendToConnectionLog(String s)
    {
        // Creates or make visible, if needed:
        options.setOption(Options.showConnectionLogWindow, true);
        connectionLogWindow.append(s);
    }

    public void actOnReconnectCompleted()
    {
        if (getGame().isPhase(Phase.MOVE))
        {
            pendingMoves.clear();
            board.highlightUnmovedLegions();
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
        new AcquireAngel(board.getFrame(), this, legion, recruits);
    }

    public void setBoardActive(boolean val)
    {
        board.setBoardActive(val);
    }

    public void doPickSummonAngel(Legion legion,
        List<Legion> possibleDonors)
    {
        new SummonAngel(this, legion, possibleDonors);
    }

    public List<CreatureType> doPickSplitLegion(Legion parent,
        String childMarker)
    {
        List<CreatureType> creaturesToSplit = SplitLegion.splitLegion(this, parent, childMarker);
        // null means cancel, empty list to signal "mark as skip".
        if (creaturesToSplit != null && creaturesToSplit.isEmpty())
        {
            markLegionAsSkipSplit(parent);
            // give back null to client to make client do nothing any more.
            creaturesToSplit = null;
        }
        return creaturesToSplit;
    }

    private void markLegionAsSkipSplit(Legion legion)
    {
        legion.setSkipThisTime(true);
        pushUndoStack(legion.getMarkerId());
        board.clearPossibleRecruitChits();
        board.highlightTallLegions();
    }

    public void resetAllLegionFlags()
    {
        for (Legion l : getOwningPlayer().getLegions())
        {
            l.setSkipThisTime(false);
            l.setVisitedThisPhase(false);
        }
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
        pickCarryDialog = new PickCarry(battleBoard, this, carryDamage,
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

    public void doPickColor(final String playerName,
        final List<PlayerColor> colorsLeft)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            bringUpPickColorDialog(playerName, colorsLeft);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    bringUpPickColorDialog(playerName, colorsLeft);
                }
            });
        }
    }

    public void bringUpPickColorDialog(String playerName,
        List<PlayerColor> colorsLeft)
    {
        board.setPhaseInfo("Pick a color!");
        PickColor.PickColorCallback callback = new PickColor.PickColorCallback()
        {
            @Override
            public void tellPickedColor(PlayerColor color)
            {
                // method that passes it on to client
                answerPickColor(color);
            }
        };
        // Do not allow null: for pick initial color keep asking if one chosen
        new PickColor(board.getFrame(), playerName, colorsLeft, options,
            callback, false);
    }

    public void doPickSplitMarker(Legion parent, Set<String> markersAvailable)
    {
        createPickMarkerDialog(this, markersAvailable, parent);
    }

    public void doPickInitialMarker(Set<String> markersAvailable)
    {
        board.setPhaseInfo("Pick initial marker!");
        createPickMarkerDialog(this, markersAvailable, null);
    }

    public void createPickMarkerDialog(final ClientGUI gui,
        final Set<String> markerIds, final Legion parent)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            new PickMarker(gui, markerIds, parent);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    new PickMarker(gui, markerIds, parent);
                }
            });
        }
    }



    public CreatureType doPickRecruit(Legion legion, String hexDescription)
    {
        List<CreatureType> recruits = client.findEligibleRecruits(legion,
            legion.getCurrentHex());

        return PickRecruit.pickRecruit(board.getFrame(), recruits,
            hexDescription, legion, this);
    }

    /**
     * TODO This is just a HACK.
     * PickRecruit calls this to mark a legion as that user wants to not
     * recruit anything this turn.
     * Better would be, if that dialog could return a "NONE" CreatureType
     * and the caller does the work cleanly...
     * (postponed for now because the NONE-CreatureType would be so much
     * work right now...)
     * @param legion
     */
    public void markLegionAsSkipRecruit(Legion legion)
    {
        legion.setSkipThisTime(true);
        postRecruitStuff(legion);
        // TODO : if we one day handle skip recruit as a special
        // "NONE"-CreatureType, show recruit chit and highlight could
        // both be part of the postRecruit call, highlight not needed
        // here separately.
        board.highlightPossibleRecruitLegionHexes();
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
            recruiters, hexDescription, legion, this);
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
        cleanupNegotiationDialogs();
        if (isMyTurn())
        {
            board.myTurnStartsActions();
        }
        eventViewer.turnOrPlayerChange(turnNr, player);
    }

    public void actOnGameStarting()
    {
        if (!client.isRemote())
        {
            board.enableSaveActions();
        }
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
        clearUndoStack();
        board.setupSplitMenu();
        board.fullRepaint(); // Ensure that movement die goes away
        if (isMyTurn())
        {
            // TODO replace all those "when xxx happens then set label to yyy"
            // with proper "set game state to state xxx and trigger redisplaying
            // of labels and setting actions dis-/enabled accordingly"...
            if (client.getTurnNumber() == 1)
            {
                board.disableDoneAction("Split legions in first round");
            }
            board.maybeRequestFocusAndToFront();
            defaultCursor();

            // TODO I believe the code below is meant for the purpose:
            // "If no legions can be split, directly be done with Split
            //  phase, except if that is the result of the autoSplit"
            //  - so that one can review and undo.
            // But that does not make so much sense, as this is in the
            // "setupSplit" call, so the AI can't have done anything yet?
            //
            // 2009 June, Clemens: added "&& !isUndoStackEmpty()". If one did
            // split all possible legions just before save and load it again,
            // then this one here would otherwise automatically do the
            // "doneWithSplit()".
            if ((getOwningPlayer().getMarkersAvailable().size() < 1 || client
                .findTallLegionHexes(4, true).isEmpty())
                && !options.getOption(Options.autoSplit) && isUndoStackEmpty())
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
            board.maybeRequestFocusAndToFront();
            defaultCursor();
        }
        updateStatusScreen();
    }

    public void actOnSetupMove()
    {
        clearUndoStack();
        pendingMoves.clear();
        pendingMoveHexes.clear();
        recoveredFromMoveNak = false;
        board.setupMoveMenu();
        // Force showing the updated movement die.
        // taken repaint away because actOnTellMovementRoll does it anyway
        // board.repaint();
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

    public void actOnSetupBattleFight()
    {
        if (battleBoard != null)
        {
            battleBoard.updatePhaseAndTurn();
            if (client.isMyBattlePhase())
            {
                battleBoard.reqFocus();
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
        // needed/better to do here, than when finishing a battle turn,
        // for two reasons: 1) done with moves quickly after a move (before
        // server response received and handled) move would/might get into
        // stack again, and worse, 2) concede during battle undoStack remained
        // with content as well, causing "Undo battle move errors" (#3497085)
        clearUndoStack();
        if (battleBoard != null)
        {
            battleBoard.updatePhaseAndTurn();
            if (client.isMyBattlePhase())
            {
                battleBoard.reqFocus();
                defaultCursor();
                battleBoard.setupMoveMenu();
            }
        }
        updateStatusScreen();
    }

    public void actOnTellBattleMove(BattleHex startingHex,
        BattleHex endingHex, boolean rememberForUndo)
    {
        if (rememberForUndo)
        {
            pushUndoStack(endingHex.getLabel());
        }

        if (battleBoard != null)
        {
            battleBoard.alignChits(startingHex);
            battleBoard.alignChits(endingHex);
            battleBoard.repaint();
            actOnPendingBattleMoveOver();
        }
    }

    public void actOnPendingBattleMoveOver()
    {
        battleBoard.actOnPendingBattleMoveOver();
    }

    public void actOnDoneWithBattleMoves()
    {
        clearUndoStack();
    }

    public void actOnSetupBattleRecruit()
    {
        if (battleBoard != null)
        {
            battleBoard.updatePhaseAndTurn();
            if (client.isMyBattlePhase())
            {
                battleBoard.reqFocus();
                battleBoard.setupRecruitMenu();
            }
        }
        updateStatusScreen();

    }

    public void actOnSetupBattleSummon()
    {
        if (battleBoard != null)
        {
            battleBoard.updatePhaseAndTurn();
            if (client.isMyBattlePhase())
            {
                battleBoard.reqFocus();
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
            });
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

    public void actOnPlaceNewChit(String imageName, BattleUnit battleUnit,
        BattleHex hex)
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
            // Make sure BattleBoard is visible after summon or muster.
            battleBoard.reqFocus();
        }
    }

    private String getBattleUnitDescription(BattleCritter battleUnit)
    {
        if (battleUnit == null)
        {
            return "";
        }
        BattleHex hex = battleUnit.getCurrentHex();
        return battleUnit.getType().getName() + " in " + hex.getDescription();
    }

    public void actOnTellStrikeResults(boolean wasCarry, int strikeNumber,
        List<String> rolls, BattleCritter striker, BattleCritter target)
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

    public void actOnHitsSet(BattleUnit target)
    {
        battleBoard.actOnHitsSet(target.getCurrentHex());
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

    /**
     * For the topmost item on undo stack, undo the done recruit,
     * or reset the skipThisTime flag if set.
     */
    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            Legion legion = client.getLegion(markerId);
            handleUndoRecruit(legion);
        }
    }

    /**
     * For a specific clicked legion, undo the done recruit,
     * or reset the skipThisTime flag if set.
     * @param legion The legion for which to undo the recruit
     */
    public void undoRecruit(Legion legion)
    {
        if (undoStack.contains(legion.getMarkerId()))
        {
            undoStack.remove(legion.getMarkerId());
        }
        handleUndoRecruit(legion);
    }

    /**
     * This does the actual work for undoing a recruit
     * @param legion The legion for which to undo the recruit
     */
    private void handleUndoRecruit(Legion legion)
    {
        if (legion.hasRecruited())
        {
            getClient().undoRecruit(legion);
        }
        else
        {
            legion.setSkipThisTime(false);
            if (client.isMyLegion(legion))
            {
                board.updateLegionsLeftToMusterText();
            }
        }
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
            Legion legion = client.getLegion(splitoffId);
            if (legion.getSkipThisTime())
            {
                legion.setSkipThisTime(false);
                board.alignLegions(legion.getCurrentHex());
                board.highlightTallLegions();
            }
            else
            {
                client.undoSplit(legion);
            }
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
        if (pendingMoves.size() > 0)
        {
            displayNoUndoWhilePendingMovesInfo();
            return;
        }

        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            Legion legion = client.getLegion(markerId);
            if (legion.hasMoved())
            {
                getClient().undoMove(legion);
            }
            else
            {
                legion.setSkipThisTime(false);
            }
            board.updateLegionsLeftToMoveText(true);
        }
    }

    public void undoLastBattleMove()
    {
        if (!isUndoStackEmpty())
        {
            String hexLabel = (String)popUndoStack();
            BattleHex hex = battleBoard.getBattleHexByLabel(hexLabel);
            undoBattleMove(hex);
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
        if (pendingMoves.size() > 0)
        {
            displayNoUndoWhilePendingMovesInfo();
            return;
        }
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

    private void displayNoUndoWhilePendingMovesInfo()
    {
        JOptionPane.showMessageDialog(getMapOrBoardFrame(),
            "For some moves is still the confirmation from server and "
                + "screen update missing!\n"
                + "Undo can't be done beofre all moves are completed "
                + "(see message beside the Done button).", "Pending Moves!",
            JOptionPane.INFORMATION_MESSAGE);
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
        // For now, that one shall NOT be reopened by default on next start
        options.setOption(Options.showConnectionLogWindow, false);
        disposeConnectionLogWindow();
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
        // In odd situations, e.g. web game did not start properly, the
        // game client is set in webclient, but board is still null
        if (board != null)
        {
            // Board does the "Really Quit?" confirmation and initiates
            // then (if user confirmed) the disposal of everything.
            board.doQuitGameAction();
        }
        else
        {
            menuQuitGame();
        }
    }

    /**
     * This is for permanent, non-reversible closed connections
     */
    public void showConnectionClosedMessage()
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
        for (PendingMove pendingMove : pendingMoves)
        {
            if (mover.equals(pendingMove.getLegion()))
            {
                JOptionPane.showMessageDialog(getMapOrBoardFrame(),
                    "Legion already moved, but screen update happens only "
                        + "after confirmation from server was received.",
                    "Already moved!", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        }
        return client.doMove(mover, hex);
    }

    public void actOnMoveNak()
    {
        pendingMoves.clear();
        pendingMoveHexes.clear();
        recoveredFromMoveNak = true;
    }

    // GUI keeps track for which doMove()'s server has not ackknowledged yet,
    // so that we can catch cases when user attempts to click/move them again
    // (e.g. when server response is slow)

    private class PendingMove
    {
        public Legion mover;
        public MasterHex currentHex;
        public MasterHex targetHex;

        public PendingMove(Legion mover, MasterHex current, MasterHex target)
        {
            this.mover = mover;
            this.currentHex = current;
            this.targetHex = target;
        }

        public boolean matches(Legion mover, MasterHex current,
            MasterHex target)
        {
            return this.mover == mover && this.currentHex.equals(current)
                && this.targetHex.equals(target);
        }

        public Legion getLegion()
        {
            return mover;
        }
    }

    // doMove was sent to server, store it in list
    public void setMovePending(Legion mover, MasterHex currentHex,
        MasterHex targetHex)
    {
        // board.visualizeMoveTarget(mover, currentHex, targetHex);
        PendingMove move = new PendingMove(mover, currentHex, targetHex);
        pendingMoves.add(move);
        pendingMoveHexes.add(targetHex);
        updatePendingText();
    }

    private void updatePendingText()
    {
        int count = pendingMoves.size();

        if (count > 0)
        {
            String morePendingText = " (" + count + " move"
                + (count != 1 ? "s" : "") + " pending)";
            board.setPendingText(morePendingText);
        }
        else
        {
            board.setMovementPhase();
        }
    }

    public Set<MasterHex> getPendingMoveHexes()
    {
        return pendingMoveHexes;
    }

    public Set<MasterHex> getStillToMoveHexes()
    {
        HashSet<Legion> pendingLegions = new HashSet<Legion>();
        for (PendingMove move : pendingMoves)
        {
            pendingLegions.add(move.getLegion());
        }
        return client.findUnmovedLegionHexes(true, pendingLegions);
    }

    // Search and remove this pendingMove from list
    public void setMoveCompleted(Legion mover, MasterHex current,
        MasterHex target)
    {
        pendingMoveHexes.remove(target);
        PendingMove foundMove = null;
        for (PendingMove move : pendingMoves)
        {
            if (move.matches(mover, current, target))
            {
                foundMove = move;
                continue;
            }
        }

        if (foundMove != null)
        {
            pendingMoves.remove(foundMove);
        }
        else if (isRedoOngoing())
        {
            // move was probably done before saving
        }
        else if (recoveredFromMoveNak)
        {
            // ok, recover from Nak wiped out wiped out pendingMoves list
            // (because one move was sent to server but did not really happen,
            // and it's hard to figure out which).
            // This way we loose some "safety checking", but at least no
            // legal moves are prevented.
        }
        else
        {
            LOGGER.warning("Could not find pending move for legion " + mover
                + " from hex " + current + " to hex " + target);
        }
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

    public GUICallbacks getCallbackHandler()
    {
        return this;
    }

    public void answerPickColor(PlayerColor color)
    {
        getClient().answerPickColor(color);
    }

    public void leaveCarryMode()
    {
        getClient().leaveCarryMode();
    }

    public void applyCarries(BattleHex hex)
    {
        getClient().applyCarries(hex);
    }

    public void acquireAngelCallback(Legion legion, CreatureType angelType)
    {
        getClient().acquireAngelCallback(legion, angelType);
    }

    public void answerFlee(Legion ally, boolean answer)
    {
        getClient().answerFlee(ally, answer);
    }

    public void answerConcede(Legion legion, boolean answer)
    {
        getClient().answerConcede(legion, answer);
    }

    public void doBattleMove(int tag, BattleHex hex)
    {
        getClient().doBattleMove(tag, hex);
    }

    public void undoBattleMove(BattleHex hex)
    {
        getClient().undoBattleMove(hex);
    }

    public void strike(int tag, BattleHex hex)
    {
        getClient().strike(tag, hex);
    }

    public void doneWithBattleMoves()
    {
        getClient().doneWithBattleMoves();
    }

    public void doneWithStrikes()
    {
        getClient().doneWithStrikes();
    }

    public void concede()
    {
        getClient().concede();
    }
}
