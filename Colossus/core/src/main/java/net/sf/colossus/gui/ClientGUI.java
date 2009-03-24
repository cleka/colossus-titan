package net.sf.colossus.gui;


import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
import net.sf.colossus.client.IOptions;
import net.sf.colossus.client.IOracle;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.game.BattlePhase;
import net.sf.colossus.game.EntrySide;
import net.sf.colossus.game.Game;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Start.WhatToDoNext;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.LogWindow;
import net.sf.colossus.util.Options;
import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
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

    /** Information on the current moving legion. */
    private LegionClientSide mover;

    /** the parent frame for secondary windows */
    private JFrame secondaryParent = null;

    private int replayLastTurn = -1;
    private int replayMaxTurn = 0;

    private int viewMode;
    private int recruitChitMode;

    private String gameOverMessage;

    protected final Client client;

    // for things other GUI components need to inquire,
    // use the Oracle (on the long run, I guess there will be the
    // GameClientSide class behind it...)
    protected final IOracle oracle;

    // Per-client and per-player options.
    private final Options options;

    public ClientGUI(Client client, Options options)
    {
        this.client = client;
        this.oracle = client;
        this.options = options;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setupClientGUI()
     */
    public void setupClientGUI()
    {
        disposeEventViewer();
        disposePreferencesWindow();
        disposeEngagementResults();
        disposeInspector();
        disposeCaretakerDisplay();
        disposeLogWindow();
        disposeMasterBoard();

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

        focusBoard();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setStartedByWebClient(boolean)
     */
    public void setStartedByWebClient(boolean byWebClient)
    {
        this.startedByWebClient = byWebClient;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setWebClient(net.sf.colossus.webclient.WebClient)
     */
    public void setWebClient(WebClient wc)
    {
        this.webClient = wc;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setClientInWebClientNull()
     */
    public void setClientInWebClientNull()
    {
        if (webClient != null)
        {
            webClient.setGameClient(null);
            webClient = null;
        }
    }

    // TODO still needed?
    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#getBoard()
     */
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
    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#handleWebClientRestore()
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showWebClient()
     */
    public void showWebClient()
    {
        if (this.webClient == null)
        {
            this.webClient = new WebClient(client.getStartObject(), null, -1,
                null, null);
            this.webClient.setGameClient(client);
        }
        else
        {
            this.webClient.setVisible(true);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#initBoard()
     */
    public void initBoard()
    {
        String viewModeName = options.getStringOption(Options.viewMode);
        viewMode = options.getNumberForViewMode(viewModeName);

        String rcMode = options
            .getStringOption(Options.showRecruitChitsSubmenu);
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#ensureEdtSetupClientGUI()
     */
    public void ensureEdtSetupClientGUI()
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#ensureEdtNewBattleBoard()
     */
    public void ensureEdtNewBattleBoard()
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnInitBattle()
     */
    public void actOnInitBattle()
    {
        if (board != null)
        {
            ensureEdtNewBattleBoard();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doNewBattleBoard()
     */
    public void doNewBattleBoard()
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#updateStatusScreen()
     */
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
                if (board != null)
                {
                    statusScreen = new StatusScreen(getPreferredParent(),
                        client, options, client);
                }
            }
        }
        else
        {
            if (board != null)
            {
                board.twiddleOption(Options.showStatusScreen, false);
            }
            if (statusScreen != null)
            {
                statusScreen.dispose();
            }
            this.statusScreen = null;
        }

        // XXX Should be called somewhere else, just once.
        setupPlayerLabel();
    }

    boolean quitAlreadyTried = false;

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#menuCloseBoard()
     */
    public void menuCloseBoard()
    {
        clearUndoStack();
        client.doSetWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG, false);
        client.disposeClientOriginated();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#menuQuitGame()
     */
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

        client.doSetWhatToDoNext(WhatToDoNext.QUIT_ALL, true);
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#serverConfirmsConnection()
     */
    public synchronized void serverConfirmsConnection()
    {
        LOGGER.info("Client for player " + getOwningPlayer().getName()
            + " received confirmation that connection is OK.");
        finishServerConnectionCheck(true);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#timeoutAbortsConnectionCheck()
     */
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

    // Used by File=>Close and Window closing
    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setWhatToDoNextForClose()
     */
    public void setWhatToDoNextForClose()
    {
        if (startedByWebClient)
        {
            client.doSetWhatToDoNext(WhatToDoNext.START_WEB_CLIENT, false);
        }
        else if (client.isRemote())
        {
            // Remote clients get back to Network Client dialog
            client.doSetWhatToDoNext(WhatToDoNext.NET_CLIENT_DIALOG, false);
        }
        else
        {
            client.doSetWhatToDoNext(WhatToDoNext.GET_PLAYERS_DIALOG, false);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#menuNewGame()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#menuLoadGame(java.lang.String)
     */
    public void menuLoadGame(String filename)
    {
        if (webClient != null)
        {
            webClient.dispose();
            webClient = null;
        }
        client.doSetWhatToDoNext(WhatToDoNext.LOAD_GAME, filename);
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#focusMap()
     */
    public void focusMap()
    {
        if (battleBoard != null)
        {
            battleBoard.reqFocus();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#focusBoard()
     */
    public void focusBoard()
    {
        if (board != null)
        {
            board.reqFocus();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#highlightEngagements()
     */
    public void highlightEngagements()
    {
        if (board != null)
        {
            if (isMyTurn())
            {
                focusBoard();
            }
            board.highlightEngagements();
        }
    }

    private JFrame getPreferredParent()
    {
        if ((secondaryParent == null) && (board != null))
        {
            return board.getFrame();
        }
        return secondaryParent;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#getViewMode()
     */
    public int getViewMode()
    {
        return viewMode;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#getRecruitChitMode()
     */
    public int getRecruitChitMode()
    {
        return recruitChitMode;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#addPossibleRecruitChits(net.sf.colossus.client.LegionClientSide, java.util.Set)
     */
    public void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes)
    {
        if (recruitChitMode == Options.showRecruitChitsNumNone)
        {
            return;
        }

        if (board != null)
        {
            board.addPossibleRecruitChits(legion, hexes);
        }
    }

    /*
     * Trigger side effects after changing an option value.
     *
     * TODO now that there are listeners, many of the other classes could listen to the
     * options relevant to them instead of dispatching it all through the Client class.
     */

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setupGUIOptionListeners()
     */
    public void setupGUIOptionListeners()
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
                net.sf.colossus.util.ResourceLoader.purgeImageCache();
                repaintAllWindows();
            }
        });
        options.addListener(Options.useColoredBorders, new IOptions.Listener()
        {
            @Override
            public void booleanOptionChanged(String optname, boolean oldValue,
                boolean newValue)
            {
                BattleChit.setUseColoredBorders(newValue);
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerSetVisibleMaybe()
     */
    public void eventViewerSetVisibleMaybe()
    {
        // if null: no board (not yet, or not at all) => no eventviewer
        if (eventViewer != null)
        {
            // Eventviewer takes care of showing/hiding itself
            eventViewer.setVisibleMaybe();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#autoInspectorSetDubiousAsBlanks(boolean)
     */
    public void autoInspectorSetDubiousAsBlanks(boolean newValue)
    {
        if (autoInspector != null)
        {
            autoInspector.setDubiousAsBlanks(newValue);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#engagementResultsMaybeShow()
     */
    public void engagementResultsMaybeShow()
    {
        // null if there is no board, e.g. AI
        if (engagementResults != null)
        {
            // maybeShow decides by itself based on the current value
            // of the option whether to hide or show.
            engagementResults.maybeShow();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellLegionLocation(net.sf.colossus.game.Legion, net.sf.colossus.variant.MasterHex)
     */
    public void actOnTellLegionLocation(Legion legion, MasterHex hex)
    {
        if (board != null)
        {
            // @TODO: this creates it every time, not only when necessary ?
            Marker marker = new Marker(3 * Scale.get(), legion.getMarkerId(),
                client);
            setMarker(legion, marker);

            client.setMarkerForLegion(legion, marker);

            if (!isReplayOngoing())
            {
                board.alignLegions(hex);
            }
        }
    }

    /** Add the marker to the end of the list and to the LegionInfo.
    If it's already in the list, remove the earlier entry. */
    void setMarker(Legion legion, Marker marker)
    {
        if (board != null)
        {
            board.markerToTop(marker);
        }
        client.setMarkerForLegion(legion, marker);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDidSplit(int, net.sf.colossus.game.Legion, net.sf.colossus.game.Legion, net.sf.colossus.variant.MasterHex)
     */
    public void actOnDidSplit(int turn, Legion parent, Legion child,
        MasterHex hex)
    {
        if (!isReplayOngoing())
        {
            eventViewerNewSplitEvent(turn, parent, child);
        }

        if (board != null)
        {
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
        }

        if (client.isMyLegion(child))
        {
            if (board != null)
            {
                board.clearRecruitedChits();
                board.clearPossibleRecruitChits();
            }

            pushUndoStack(child.getMarkerId());
        }

    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDidSplitPart2(net.sf.colossus.variant.MasterHex)
     */
    public void actOnDidSplitPart2(MasterHex hex)
    {
        if (client.getTurnNumber() == 1 && board != null && isMyTurn())
        {
            board.enableDoneAction();
        }

        if (board != null)
        {
            if (!isReplayOngoing())
            {
                board.alignLegions(hex);
                board.highlightTallLegions();
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDoneWithMoves()
     */
    public void actOnDoneWithMoves()
    {
        if (board != null)
        {
            board.clearRecruitedChits();
            board.clearPossibleRecruitChits();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDoneWithSplits()
     */
    public void actOnDoneWithSplits()
    {
        if (board != null)
        {
            board.clearRecruitedChits();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDidRecruit(net.sf.colossus.game.Legion, java.lang.String, java.util.List, java.lang.String)
     */
    public void actOnDidRecruit(Legion legion, String recruitName,
        List<String> recruiters, String reason)
    {
        if (board != null)
        {
            board.addRecruitedChit(legion);
            board.highlightPossibleRecruitLegionHexes();

            if (eventViewer != null)
            {
                eventViewer.recruitEvent(legion.getMarkerId(), (legion)
                    .getHeight(), recruitName, recruiters, reason);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnRemoveCreature(net.sf.colossus.game.Legion, java.lang.String, java.lang.String)
     */
    public void actOnRemoveCreature(Legion legion, String name, String reason)
    {
        if (eventViewer != null)
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
                LOGGER
                    .info("removeCreature called for undidRecruit - ignored");
            }
            else
            {
                eventViewer.removeCreature(legion.getMarkerId(), name);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnRemoveCreaturePart2(net.sf.colossus.game.Legion)
     */
    public void actOnRemoveCreaturePart2(Legion legion)
    {
        if (board != null && !isReplayOngoing())
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnAddCreature(net.sf.colossus.game.Legion, java.lang.String, java.lang.String)
     */
    public void actOnAddCreature(Legion legion, String name, String reason)
    {
        if (board != null && !isReplayOngoing())
        {
            GUIMasterHex hex = board.getGUIHexByMasterHex(legion
                .getCurrentHex());
            hex.repaint();
        }

        if (eventViewer != null)
        {
            eventViewer.addCreature(legion.getMarkerId(), name, reason);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#boardActOnUndidSplit(net.sf.colossus.game.Legion, int)
     */
    public void boardActOnUndidSplit(Legion survivor, int turn)
    {
        if (board != null)
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
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnUndidRecruitPart2(net.sf.colossus.game.Legion, int, int)
     */
    public void actOnUndidRecruitPart2(Legion legion,
        boolean wasReinforcement, int turnNumber)
    {
        int eventType = wasReinforcement ? RevealEvent.eventReinforce
            : RevealEvent.eventRecruit;
        if (board != null)
        {
            board.cleanRecruitedChit((LegionClientSide)legion);
            board.highlightPossibleRecruitLegionHexes();

            if (eventViewer != null)
            {
                eventViewer.undoEvent(eventType, legion.getMarkerId(), null,
                    turnNumber);
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#chooseWhetherToTeleport()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDidMove(net.sf.colossus.game.Legion, net.sf.colossus.variant.MasterHex, net.sf.colossus.variant.MasterHex, boolean, java.lang.String, boolean)
     */
    public void actOnDidMove(Legion legion, MasterHex startingHex,
        MasterHex currentHex, boolean teleport, String teleportingLord,
        boolean splitLegionHasForcedMove)
    {
        if (teleport)
        {
            if (eventViewer != null)
            {
                eventViewer.newCreatureRevealEvent(RevealEvent.eventTeleport,
                    legion.getMarkerId(), legion.getHeight(), teleportingLord,
                    null, 0);
            }
        }

        if (board != null)
        {
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
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnUndidMove(net.sf.colossus.game.Legion, net.sf.colossus.variant.MasterHex, net.sf.colossus.variant.MasterHex, boolean, boolean)
     */
    public void actOnUndidMove(Legion legion, MasterHex formerHex,
        MasterHex currentHex, boolean splitLegionHasForcedMove,
        boolean didTeleport)
    {
        if (board != null)
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

            if (didTeleport && eventViewer != null)
            {
                eventViewer.undoEvent(RevealEvent.eventTeleport, legion
                    .getMarkerId(), null, client.getTurnNumber());
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnNextEngagement()
     */
    public void actOnNextEngagement()
    {
        if (client.findEngagements().isEmpty() && board != null)
        {
            board.enableDoneAction();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#alignLegionsMaybe(net.sf.colossus.game.Legion)
     */
    public void alignLegionsMaybe(Legion legion)
    {

        if (board != null && !isReplayOngoing())
        {
            board.alignLegions(legion.getCurrentHex());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnRemoveLegion(net.sf.colossus.game.Legion)
     */
    public void actOnRemoveLegion(Legion legion)
    {
        if (board != null)
        {
            board.removeMarkerForLegion(legion);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnDoSummon()
     */
    public void actOnDoSummon()
    {
        if (board != null)
        {
            highlightEngagements();
            board.repaint();
        }
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#updateEverything()
     */
    public void updateEverything()
    {
        if (board != null)
        {
            board.updateComponentTreeUI();
            board.pack();
        }
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#replayTurnChange(int)
     */
    public void replayTurnChange(int nowTurn)
    {
        if (board != null)
        {
            if (nowTurn != replayLastTurn)
            {
                board.updateReplayText(nowTurn, replayMaxTurn);
                replayLastTurn = nowTurn;
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellReplay(int)
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#clearUndoStack()
     */
    public void clearUndoStack()
    {
        undoStack.clear();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#popUndoStack()
     */
    public Object popUndoStack()
    {
        return undoStack.removeFirst();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#pushUndoStack(java.lang.Object)
     */
    public void pushUndoStack(Object object)
    {
        undoStack.addFirst(object);
    }

    private boolean isUndoStackEmpty()
    {
        return undoStack.isEmpty();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerCancelReinforcement(java.lang.String, int)
     */
    public void eventViewerCancelReinforcement(String recruitName, int turnNr)
    {
        if (eventViewer != null)
        {
            eventViewer.cancelReinforcement(recruitName, turnNr);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerDefenderSetCreatureDead(java.lang.String, int)
     */
    public void eventViewerDefenderSetCreatureDead(String name, int height)
    {
        if (eventViewer != null)
        {
            eventViewer.defenderSetCreatureDead(name, height);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerAttackerSetCreatureDead(java.lang.String, int)
     */
    public void eventViewerAttackerSetCreatureDead(String name, int height)
    {
        if (eventViewer != null)
        {
            eventViewer.attackerSetCreatureDead(name, height);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerNewSplitEvent(int, net.sf.colossus.game.Legion, net.sf.colossus.game.Legion)
     */
    public void eventViewerNewSplitEvent(int turn, Legion parent, Legion child)
    {
        if (eventViewer != null)
        {
            eventViewer.newSplitEvent(turn, parent.getMarkerId(), parent
                .getHeight(), null, child.getMarkerId(), child.getHeight());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerUndoEvent(net.sf.colossus.game.Legion, net.sf.colossus.game.Legion, int)
     */
    public void eventViewerUndoEvent(Legion splitoff, Legion survivor, int turn)
    {
        if (eventViewer != null)
        {
            eventViewer.undoEvent(RevealEvent.eventSplit, survivor
                .getMarkerId(), splitoff.getMarkerId(), turn);
        }
        else
        {
            // fine. So this client does not even have eventViewer
            // (probably then not even a masterBoard, i.e. AI)
        }
    }

    private void initPreferencesWindow()
    {
        if (preferencesWindow == null)
        {
            preferencesWindow = new PreferencesWindow(options, client);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setPreferencesWindowVisible(boolean)
     */
    public void setPreferencesWindowVisible(boolean val)
    {
        if (preferencesWindow != null)
        {
            preferencesWindow.setVisible(val);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showMarker(net.sf.colossus.gui.Marker)
     */
    public void showMarker(Marker marker)
    {
        if (autoInspector != null)
        {
            String markerId = marker.getId();
            Legion legion = client.getLegion(markerId);
            autoInspector.showLegion((LegionClientSide)legion);
        }
    }

    private void showOrHideCaretaker(boolean bval)
    {
        if (board == null)
        {
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
                autoInspector = new AutoInspector(parent, options, viewMode,
                    options.getOption(Options.dubiousAsBlanks));
            }
        }
        else
        {
            disposeInspector();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showHexRecruitTree(net.sf.colossus.gui.GUIMasterHex)
     */
    public void showHexRecruitTree(GUIMasterHex hex)
    {
        if (autoInspector != null)
        {
            autoInspector.showHexRecruitTree(hex);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#didSummon(net.sf.colossus.game.Legion, net.sf.colossus.game.Legion, java.lang.String)
     */
    public void didSummon(Legion summoner, Legion donor, String summon)
    {
        if (eventViewer != null)
        {
            eventViewer.newCreatureRevealEvent(RevealEvent.eventSummon, donor
                .getMarkerId(), (donor).getHeight(), summon, summoner
                .getMarkerId(), (summoner).getHeight());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#repaintBattleBoard()
     */
    public void repaintBattleBoard()
    {
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#repaintAllWindows()
     */
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
        if (board != null)
        {
            board.getFrame().repaint();
        }
        if (battleBoard != null)
        {
            battleBoard.repaint();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#rescaleAllWindows()
     */
    public void rescaleAllWindows()
    {
        if (statusScreen != null)
        {
            statusScreen.rescale();
        }
        if (board != null)
        {
            board.clearRecruitedChits();
            board.clearPossibleRecruitChits();
            board.rescale();
        }
        if (battleBoard != null)
        {
            battleBoard.rescale();
        }
        repaintAllWindows();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#disposeInspector()
     */
    public void disposeInspector()
    {
        if (autoInspector != null)
        {
            autoInspector.setVisible(false);
            autoInspector.dispose();
            autoInspector = null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#updateCreatureCountDisplay()
     */
    public void updateCreatureCountDisplay()
    {
        if (board == null)
        {
            return;
        }
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
    }

    private void disposeBattleBoard()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#disposePickCarryDialog()
     */
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
        if (engagementResults != null)
        {
            engagementResults.dispose();
            engagementResults = null;
        }
    }

    private void disposeCaretakerDisplay()
    {
        if (caretakerDisplay != null)
        {
            caretakerDisplay.dispose();
            caretakerDisplay = null;
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showNegotiate(net.sf.colossus.game.Legion, net.sf.colossus.game.Legion)
     */
    public void showNegotiate(Legion attacker, Legion defender)
    {
        negotiate = new Negotiate(this, attacker, defender);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#respawnNegotiate()
     */
    public void respawnNegotiate()
    {
        if (negotiate != null)
        {
            negotiate.dispose();
        }
        negotiate = new Negotiate(this, client.getAttacker(), client
            .getDefender());
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showConcede(net.sf.colossus.client.Client, net.sf.colossus.game.Legion, net.sf.colossus.game.Legion)
     */
    public void showConcede(Client client, Legion ally, Legion enemy)
    {
        Concede.concede(client, board.getFrame(), ally, enemy);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showFlee(net.sf.colossus.client.Client, net.sf.colossus.game.Legion, net.sf.colossus.game.Legion)
     */
    public void showFlee(Client client, Legion ally, Legion enemy)
    {
        Concede.flee(client, board.getFrame(), ally, enemy);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#initShowEngagementResults()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#tellEngagement(net.sf.colossus.game.Legion, net.sf.colossus.game.Legion, int)
     */
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

        if (eventViewer != null)
        {
            eventViewer.tellEngagement(attacker, defender, turnNumber);
        }
    }

    void highlightBattleSite(MasterHex battleSite)
    {
        if (board != null && battleSite != null)
        {
            board.unselectAllHexes();
            board.selectHexByLabel(battleSite.getLabel());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellEngagementResults(net.sf.colossus.game.Legion, java.lang.String, int, int)
     */
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

        if (eventViewer != null)
        {
            eventViewer.tellEngagementResults(winner, method, turns);
        }

        if (engagementResults != null)
        {
            engagementResults.addData(winner, method, points, turns,
                tellEngagementResultsAttackerStartingContents,
                tellEngagementResultsDefenderStartingContents,
                tellEngagementResultsAttackerLegionCertainities,
                tellEngagementResultsDefenderLegionCertainities, client
                    .isMyTurn());
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setMulliganOldRoll(int)
     */
    public void setMulliganOldRoll(int movementRoll)
    {
        if (eventViewer != null)
        {
            eventViewer.setMulliganOldRoll(movementRoll);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#tellWhatsHappening(java.lang.String)
     */
    public void tellWhatsHappening(String message)
    {
        if (board != null)
        {
            board.setPhaseInfo(message);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#tellMovementRoll(int)
     */
    public void tellMovementRoll(int roll)
    {
        if (eventViewer != null)
        {
            eventViewer.tellMovementRoll(roll);
        }

        if (movementDie == null || roll != movementDie.getLastRoll())
        {
            if (board != null)
            {
                movementDie = new MovementDie(4 * Scale.get(), MovementDie
                    .getDieImageName(roll));

                if (board.getFrame().getExtendedState() != JFrame.ICONIFIED)
                {
                    board.repaint();
                }
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
    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#revealEngagedCreatures(net.sf.colossus.game.Legion, java.util.List, boolean, java.lang.String)
     */
    public void revealEngagedCreatures(Legion legion,
        final List<String> names, boolean isAttacker, String reason)
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

        if (eventViewer != null)
        {
            eventViewer.revealEngagedCreatures(names, isAttacker, reason);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#eventViewerRevealCreatures(net.sf.colossus.game.Legion, java.util.List, java.lang.String)
     */
    public void eventViewerRevealCreatures(Legion legion,
        final List<String> names, String reason)
    {
        if (eventViewer != null)
        {
            eventViewer.revealCreatures(legion, names, reason);
        }
    }

    private void showOrHideLogWindow(Client client, boolean show)
    {
        if (board != null && show)
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#syncCheckboxes()
     */
    public void syncCheckboxes()
    {
        if (board == null)
        {
            return;
        }
        Enumeration<String> en = options.propertyNames();
        while (en.hasMoreElements())
        {
            String name = en.nextElement();
            boolean value = options.getOption(name);
            board.twiddleOption(name, value);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doAcquireAngel(net.sf.colossus.game.Legion, java.util.List)
     */
    public void doAcquireAngel(Legion legion, List<String> recruits)
    {
        board.deiconify();
        new AcquireAngel(board.getFrame(), client, legion, recruits);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#setBoardActive(boolean)
     */
    public void setBoardActive(boolean val)
    {
        board.setBoardActive(val);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickSummonAngel(net.sf.colossus.game.Legion)
     */
    public SummonInfo doPickSummonAngel(Legion legion,
        SortedSet<Legion> possibleDonors)
    {
        return SummonAngel.summonAngel(this, legion, possibleDonors);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickSplitLegion(net.sf.colossus.game.Legion, java.lang.String)
     */
    public String doPickSplitLegion(Legion parent, String childMarker)
    {
        return SplitLegion.splitLegion(this, parent, childMarker);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickCarries(net.sf.colossus.client.Client, int, java.util.Set)
     */
    public void doPickCarries(Client client, int carryDamage,
        Set<String> carryTargetDescriptions)
    {
        Set<String> carryTargetHexes = new TreeSet<String>();
        for (String desc : carryTargetDescriptions)
        {
            carryTargetHexes.add(desc.substring(desc.length() - 2));
        }
        battleBoard.highlightPossibleCarries(carryTargetHexes);
        pickCarryDialog = new PickCarry(battleBoard, client, carryDamage,
            carryTargetDescriptions);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#getPickCarryDialog()
     */
    public PickCarry getPickCarryDialog()
    {
        return pickCarryDialog;
    }

    public PlayerColor doPickColor(String playerName,
        List<PlayerColor> colorsLeft)
    {
        PlayerColor color = null;
        if (board != null)
        {
            board.setPhaseInfo("Pick a color!");
        }
        do
        {
            color = PickColor.pickColor(board.getFrame(), playerName,
                colorsLeft, options);
        }
        while (color == null);
        return color;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickMarker(java.util.Set)
     */
    public String doPickMarker(Set<String> markersAvailable)
    {
        return PickMarker.pickMarker(board.getFrame(), client
            .getOwningPlayer(), markersAvailable, options);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickMarkerUntilGotOne(java.util.Set)
     */
    public String doPickMarkerUntilGotOne(Set<String> markersAvailable)
    {
        String markerId = null;
        if (board != null)
        {
            board.setPhaseInfo("Pick initial marker!");
        }
        do
        {
            markerId = doPickMarker(markersAvailable);
        }
        while (markerId == null);

        return markerId;
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickRecruit(net.sf.colossus.game.Legion, java.lang.String)
     */
    public String doPickRecruit(Legion legion, String hexDescription)
    {
        List<CreatureType> recruits = client.findEligibleRecruits(legion,
            legion.getCurrentHex());

        return PickRecruit.pickRecruit(board.getFrame(), recruits,
            hexDescription, legion, client);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickRecruiter(java.util.List, java.lang.String, net.sf.colossus.game.Legion)
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickEntrySide(net.sf.colossus.variant.MasterHex, java.util.Set)
     */
    public EntrySide doPickEntrySide(MasterHex hex, Set<EntrySide> entrySides)
    {
        return PickEntrySide.pickEntrySide(board.getFrame(), hex, entrySides);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickLord(java.util.List)
     */
    public String doPickLord(List<String> lords)
    {
        return PickLord.pickLord(options, board.getFrame(), lords);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doPickStrikePenalty(net.sf.colossus.client.Client, java.util.List)
     */
    public void doPickStrikePenalty(Client client, List<String> choices)
    {
        new PickStrikePenalty(battleBoard, client, choices);
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#tellProposal(java.lang.String)
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#cleanupNegotiationDialogs()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTurnOrPlayerChange(net.sf.colossus.client.Client, int, int)
     */
    public void actOnTurnOrPlayerChange(Client client, int turnNr,
        Player player)
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        if (eventViewer != null)
        {
            eventViewer.turnOrPlayerChange(client, turnNr, player);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupSplit()
     */
    public void actOnSetupSplit()
    {
        if (board != null)
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
        }
        updateStatusScreen();

    }

    private void validateLegions()
    {
        boolean foundProblem = false;

        for (Player p : client.getPlayers())
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupMuster()
     */
    public void actOnSetupMuster()
    {
        clearUndoStack();
        cleanupNegotiationDialogs();

        if (board != null)
        {
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
        }
        updateStatusScreen();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupMove()
     */
    public void actOnSetupMove()
    {
        clearUndoStack();
        if (board != null)
        {
            board.setupMoveMenu();
            // Force showing the updated movement die.
            board.repaint();
        }
        if (isMyTurn())
        {
            defaultCursor();
        }
        updateStatusScreen();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupFight()
     */
    public void actOnSetupFight()
    {
        clearUndoStack();
        if (board != null)
        {
            board.setupFightMenu();
        }
        updateStatusScreen();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupBattleFight(net.sf.colossus.server.Constants.BattlePhase, int)
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupBattleMove()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellBattleMove(java.lang.String, java.lang.String)
     */
    public void actOnTellBattleMove(String startingHexLabel,
        String endingHexLabel)
    {
        if (battleBoard != null)
        {
            battleBoard.alignChits(startingHexLabel);
            battleBoard.alignChits(endingHexLabel);
            battleBoard.repaint();
            battleBoard.highlightMobileCritters();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupBattleRecruit()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnSetupBattleSummon()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnPlaceNewChit(java.lang.String)
     */
    public void actOnPlaceNewChit(String hexLabel)
    {
        if (battleBoard != null)
        {
            battleBoard.alignChits(hexLabel);
            // Make sure map is visible after summon or muster.
            focusMap();
        }
    }

    private String getBattleChitDescription(BattleChit chit)
    {
        if (chit == null)
        {
            return "";
        }
        BattleHex hex = client.getBattleHex(chit);
        return chit.getCreatureName() + " in " + hex.getDescription();
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellStrikeResults(boolean, int, java.util.List, net.sf.colossus.gui.BattleChit, net.sf.colossus.gui.BattleChit)
     */
    public void actOnTellStrikeResults(boolean wasCarry, int strikeNumber,
        List<String> rolls, BattleChit striker, BattleChit target)
    {
        if (battleBoard != null)
        {
            if (!wasCarry)
            {
                battleBoard.addDiceResults(getBattleChitDescription(striker),
                    getBattleChitDescription(target), strikeNumber, rolls);
            }
            battleBoard.unselectAllHexes();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#highlightCrittersWithTargets()
     */
    public void highlightCrittersWithTargets()
    {
        if (battleBoard != null)
        {
            battleBoard.highlightCrittersWithTargets();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnAppliyCarries(java.lang.String)
     */
    public void actOnAppliyCarries(String hexLabel)
    {
        if (battleBoard != null)
        {
            battleBoard.unselectHexByLabel(hexLabel);
            battleBoard.repaint();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnCleanupBattle()
     */
    public void actOnCleanupBattle()
    {
        if (battleBoard != null)
        {
            battleBoard.dispose();
            battleBoard = null;
        }
    }

    void undoLastRecruit()
    {
        if (!isUndoStackEmpty())
        {
            String markerId = (String)popUndoStack();
            client.getServer().undoRecruit(client.getLegion(markerId));
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#undoRecruit(net.sf.colossus.game.Legion)
     */
    public void undoRecruit(Legion legion)
    {
        if (undoStack.contains(legion.getMarkerId()))
        {
            undoStack.remove(legion.getMarkerId());
        }
        client.getServer().undoRecruit(legion);
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#informSplitRequiredFirstRound()
     */
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
            client.getServer().undoMove(client.getLegion(markerId));
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#undoLastBattleMove()
     */
    public void undoLastBattleMove()
    {
        if (!isUndoStackEmpty())
        {
            String hexLabel = (String)popUndoStack();
            client.getServer().undoBattleMove(hexLabel);
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#undoAllBattleMoves()
     */
    public void undoAllBattleMoves()
    {
        while (!isUndoStackEmpty())
        {
            undoLastBattleMove();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#undoAllMoves()
     */
    public void undoAllMoves()
    {
        if (board != null)
        {
            board.clearRecruitedChits();
            board.clearPossibleRecruitChits();
        }
        while (!isUndoStackEmpty())
        {
            undoLastMove();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#undoAllRecruits()
     */
    public void undoAllRecruits()
    {
        while (!isUndoStackEmpty())
        {
            undoLastRecruit();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#defaultCursor()
     */
    public void defaultCursor()
    {
        if (board != null)
        {
            board.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#waitCursor()
     */
    public void waitCursor()
    {
        if (board != null)
        {
            board.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doCleanupGUI()
     */
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#actOnTellGameOver(java.lang.String, boolean)
     */
    public void actOnTellGameOver(String message, boolean disposeFollows)
    {
        if (webClient != null)
        {
            webClient.tellGameEnds();
        }

        if (board != null)
        {
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
                gameOverMessage = message;
            }
            else
            {
                // show right away. Connection closed might come or not.
                showMessageDialog(message);
            }
        }
    }

    String getMessage()
    {
        return this.message;
    }

    String message = "";

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showMessageDialog(java.lang.String)
     */
    public void showMessageDialog(String message)
    {
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showNonModalMessageDialog(java.lang.String)
     */
    public void showNonModalMessageDialog(String message)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            doShowMessageDialog(message);
        }
        else
        {
            this.message = message;
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    doShowMessageDialog(getMessage());
                }
            });
        }
    }

    // called by WebClient
    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#doConfirmAndQuit()
     */
    public void doConfirmAndQuit()
    {
        if (board != null)
        {
            // Board does the "Really Quit?" confirmation and initiates
            // then (if user confirmed) the disposal of everything.
            board.doQuitGameAction();
        }
    }

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#closePerhapsWithMessage()
     */
    public void closePerhapsWithMessage()
    {

        defaultCursor();
        board.setServerClosedMessage(client.isGameOver());

        String dialogMessage = null;
        String dialogTitle = null;

        if (client.isGameOver())
        {
            // don't show again!
            if (gameOverMessage != null)
            {
                dialogMessage = "Game over: " + gameOverMessage + "!\n\n"
                    + "(connection closed from server side)";
                gameOverMessage = null;
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

    // XXX TODO may partly belong back to client???
    void doShowMessageDialog(String message)
    {
        // Don't bother showing messages to AI players.  Perhaps we
        // should log them.
        if (options.getOption(Options.autoPlay))
        {
            boolean isAI = getOwningPlayer().isAI();
            if ((message.equals("Draw") || message.endsWith(" wins")) && !isAI
                && !options.getOption(Options.autoQuit))
            {
                // show it for humans, even in autoplay,
                //  but not when autoQuit set (=> remote stresstest)
            }
            else
            {
                return;
            }
        }
        JFrame frame = getMapOrBoardFrame();
        if (frame != null)
        {
            JOptionPane.showMessageDialog(frame, message);
        }
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

    /* (non-Javadoc)
     * @see net.sf.colossus.gui.IClientGUI#showErrorMessage(java.lang.String, java.lang.String)
     */
    public void showErrorMessage(String reason, String title)
    {
        boolean isDummyFrame = false;
        JFrame f = getMapOrBoardFrame();
        // I do not use null or a simple frame, because then the System.exit(0)
        // does not exit by itself (due to some bug in Swing/AWT).
        if (f == null)
        {
            f = new KFrame("Dummyframe for Client error message dialog");
            isDummyFrame = true;
        }
        JOptionPane.showMessageDialog(f, reason, title,
            JOptionPane.ERROR_MESSAGE);
        if (isDummyFrame)
        {
            f.dispose();
            f = null;
        }
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

    // Called e.g. by SummonAngel
    public String getTitanBaseName(Legion legion)
    {
        return ((LegionClientSide)legion).getTitanBasename();
    }

    public Legion getMover()
    {
        return mover;
    }

    public void setMover(LegionClientSide legion)
    {
        this.mover = legion;
    }

    public boolean doMove(MasterHex hex)
    {
        return client.doMove(mover, hex);
    }

}
