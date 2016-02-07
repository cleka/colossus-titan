package net.sf.colossus.gui;


import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sf.colossus.appmain.WelcomeDialog;
import net.sf.colossus.client.Client;
import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Constants;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Phase;
import net.sf.colossus.game.Player;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.server.XMLSnapshotFilter;
import net.sf.colossus.util.ArrayHelper;
import net.sf.colossus.util.BuildInfo;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.NullCheckPredicate;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.util.SystemInfo;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
public final class MasterBoard extends JPanel
{
    private static final Logger LOGGER = Logger.getLogger(MasterBoard.class
        .getName());

    private Image offScreenBuffer;
    private boolean overlayChanged = false;

    private GUIMasterHex[][] guiHexArray = null;

    private Client client;
    private final ClientGUI gui;

    private KFrame masterFrame;
    private ShowReadme showReadme;
    private ShowHelpDoc showHelpDoc;
    private JMenu editMenu;
    private JMenu phaseMenu;
    private JPopupMenu popupMenu;
    private JPopupMenu popupMenuWithLegions;
    private Map<String, JCheckBoxMenuItem> checkboxes = new HashMap<String, JCheckBoxMenuItem>();
    private JPanel[] legionFlyouts;

    private final MasterBoardWindowHandler mbwh;
    private InfoPopupHandler iph;

    /** Last point clicked is needed for popup menus. */
    private Point lastPoint;

    /**
     * engage() has been sent to server but answer (tellEngagement()) not
     * received yet; mostly we have this, to be able to react properly when
     * user clicks on an engagement while there is still the server response
     * missing for the last one
     */
    private MasterHex engagingPendingHex = null;

    /**
     * In that time while we got tellEngagement but nothing else
     * (bottom bar just tells engaged, but no other visible notice of what's
     * going on), we might be waiting for the opponent to think about
     * whether to flee or not. So if user is impatient and clicks a hex
     * with any (same, or other) engagement, inform him we are already engaged
     * right now and probably opponent is currently thinking about whether to
     * flee or not. Once we receive showConcede or showNegotiate messages,
     * this time window is over, after that we don't say this about the
     * "perhaps opponent is currently thinking whether to flee" any more.
     */
    private boolean defenderFleePhase = false;

    /**
     * Show the message that "saving during engagement/battle will store the
     * last commit point" only once each game - flag that it has been shown
     */
    private boolean saveDuringEngagementDialogMessageShown = false;

    /**
     *  List of markers which are currently on the board,
     *  for painting in z-order => the end of the list is on top.
     *
     *  Now synchronized access to prevent NPEs when EDT wants to
     *  paint a marker and asks for the legion for it, and
     *  legion has just been removed.
     *  I don't use a synchronizedList, because then I get into
     *  trouble in the recreateMarkers method.
     */
    private final LinkedHashMap<Legion, Marker> legionToMarkerMap = new LinkedHashMap<Legion, Marker>();

    private final Map<Legion, Chit> recruitedChits = new HashMap<Legion, Chit>();
    private final Map<MasterHex, List<Chit>> possibleRecruitChits = new HashMap<MasterHex, List<Chit>>();

    /** The scrollbarspanel, needed to correct lastPoint. */
    private JScrollPane scrollPane;

    private final Container contentPane;

    /** our own little bar implementation */
    private BottomBar bottomBar;

    static Toolkit tk = Toolkit.getDefaultToolkit();
    static long eventMask = AWTEvent.MOUSE_EVENT_MASK
        + AWTEvent.KEY_EVENT_MASK;

    private boolean gameOverStateReached = false;

    private static final String clearRecruitChits = "Clear recruit chits";

    private static final String skipLegion = "Skip legion this time";
    private static final String nextLegion = "Next legion";
    private static final String destroyLegion = "Destroy legion";

    private static final String undoLast = "Undo";
    private static final String undoAll = "Undo All";
    private static final String doneWithPhase = "Done";
    private static final String forcedDoneWithPhase = "Forced Done";
    private static final String kickPhase = "Kick phase";

    private static final String takeMulligan = "Take Mulligan";
    private static final String requestExtraRoll = "Request extra roll";
    private static final String withdrawFromGame = "Withdraw from Game";

    private static final String viewWebClient = "View Web Client";
    private static final String viewFullRecruitTree = "View Full Recruit Tree";
    private static final String viewHexRecruitTree = "View Hex Recruit Tree";
    private static final String viewBattleMap = "View Battle Map";
    private static final String viewLegions = "View Legion(s)";

    private static final String chooseScreen = "Choose Screen For Info Windows";
    private static final String preferences = "Preferences...";

    private static final String about = "About";
    private static final String viewReadme = "Show Variant Readme";
    private static final String viewHelpDoc = "Options Documentation";
    private static final String viewWelcome = "Show Welcome message";

    private AbstractAction newGameAction;
    private AbstractAction loadGameAction;
    private AbstractAction saveGameAction;
    private AbstractAction saveGameAsAction;
    private AbstractAction suspendGameAction;
    private AbstractAction suspendNoSaveGameAction;
    private AbstractAction closeBoardAction;
    private AbstractAction quitGameAction;
    private AbstractAction cleanDisconnectAction;
    private AbstractAction enforcedDisconnectByServerAction;
    private AbstractAction tryReconnectAction;
    private AbstractAction checkConnectionAction;
    private AbstractAction checkAllConnectionsAction;

    private AbstractAction clearRecruitChitsAction;
    private AbstractAction skipLegionAction;
    private AbstractAction nextLegionAction;
    private AbstractAction destroyLegionAction;
    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction forcedDoneWithPhaseAction;
    private AbstractAction kickPhaseAction;
    private AbstractAction takeMulliganAction;
    private AbstractAction requestExtraRollAction;
    private AbstractAction withdrawFromGameAction;

    private AbstractAction viewWebClientAction;
    private AbstractAction viewFullRecruitTreeAction;
    private AbstractAction viewHexRecruitTreeAction;
    private AbstractAction viewBattleMapAction;
    private AbstractAction viewLegionsAction;

    private AbstractAction chooseScreenAction;

    private AbstractAction preferencesAction;

    private AbstractAction aboutAction;
    private AbstractAction viewReadmeAction;
    private AbstractAction viewHelpDocAction;
    private AbstractAction viewWelcomeAction;

    private boolean playerLabelDone;

    private SaveWindow saveWindow;

    private String cachedPlayerName = "<not set yet>";

    EditLegion editLegionOngoing = null;
    EditLegion relocateOngoing = null;

    private final class InfoPopupHandler extends KeyAdapter
    {
        private static final int POPUP_KEY_ALL_LEGIONS = KeyEvent.VK_SHIFT;
        private static final int POPUP_KEY_MY_LEGIONS = KeyEvent.VK_CONTROL;
        private static final int PANEL_MARGIN = 4;
        private static final int PANEL_PADDING = 0;

        private final WeakReference<Client> clientRef;

        private InfoPopupHandler(Client client)
        {
            super();
            this.clientRef = new WeakReference<Client>(client);
            net.sf.colossus.util.InstanceTracker.register(this, gui
                .getOwningPlayer().getName());
        }

        @Override
        public void keyPressed(KeyEvent e)
        {
            Client client = clientRef.get();
            if (client == null)
            {
                return;
            }
            if (e.getKeyCode() == POPUP_KEY_ALL_LEGIONS)
            {
                if (legionFlyouts == null)
                {
                    synchronized (legionToMarkerMap)
                    {
                        createLegionFlyouts(legionToMarkerMap.values());
                    }
                }
            }
            else if (e.getKeyCode() == POPUP_KEY_MY_LEGIONS)
            {
                if (legionFlyouts == null)
                {
                    // copy only local players markers
                    List<Marker> myMarkers = new ArrayList<Marker>();
                    synchronized (legionToMarkerMap)
                    {
                        for (Entry<Legion, Marker> entry : legionToMarkerMap
                            .entrySet())
                        {
                            Legion legion = entry.getKey();
                            if (client.isMyLegion(legion))
                            {
                                myMarkers.add(entry.getValue());
                            }
                        }
                        createLegionFlyouts(myMarkers);
                    }
                }
            }
            else
            {
                super.keyPressed(e);
            }
        }

        private void createLegionFlyouts(Collection<Marker> markers)
        {
            // copy to array so we don't get concurrent modification
            // exceptions when iterating
            Marker[] markerArray = markers.toArray(new Marker[markers.size()]);
            legionFlyouts = new JPanel[markers.size()];
            for (int i = 0; i < markerArray.length; i++)
            {
                Marker marker = markerArray[i];
                LegionClientSide legion = client.getLegion(marker.getId());
                int scale = 2 * Scale.get();

                boolean dubiousAsBlanks = gui.getOptions().getOption(
                    Options.dubiousAsBlanks);
                boolean showMarker = gui.getOptions().getOption(
                    Options.showMarker);

                final JPanel panel = new LegionInfoPanel(legion, scale,
                    PANEL_MARGIN, PANEL_PADDING, true, gui.getEffectiveViewMode(),
                    client.isMyLegion(legion), dubiousAsBlanks, true,
                    showMarker);
                add(panel);
                legionFlyouts[i] = panel;

                panel.setLocation(marker.getLocation());
                panel.setVisible(true);
                DragListener.makeDraggable(panel);

                repaint();
            }
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            if ((e.getKeyCode() == POPUP_KEY_ALL_LEGIONS)
                || (e.getKeyCode() == POPUP_KEY_MY_LEGIONS))
            {
                if (legionFlyouts != null)
                {
                    for (JPanel flyout : legionFlyouts)
                    {
                        remove(flyout);
                    }
                    repaint();
                    legionFlyouts = null;
                }
            }
            else
            {
                super.keyReleased(e);
            }
        }
    }

    public MasterBoard(final Client client, ClientGUI gui)
    {
        setLayout(null);
        this.client = client;
        this.gui = gui;

        net.sf.colossus.util.InstanceTracker.register(this,
            gui.getOwningPlayerName());

        String playerName = gui.getOwningPlayerName();
        if (playerName == null)
        {
            playerName = "unknown";
        }
        masterFrame = new KFrame("MasterBoard " + playerName);
        masterFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        contentPane = masterFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        setOpaque(true);
        setupIcon();
        setBackground(Color.black);
        this.mbwh = new MasterBoardWindowHandler();
        masterFrame.addWindowListener(mbwh);
        addMouseListener(new MasterBoardMouseHandler());
        addMouseMotionListener(new MasterBoardMouseMotionHandler());
        this.iph = new InfoPopupHandler(client);
        this.addKeyListener(this.iph);

        tk.addAWTEventListener(new AWTEventListener()
        {
            @Override
            public void eventDispatched(AWTEvent e)
            {
                if (e instanceof java.awt.event.MouseEvent)
                {
                    // ignore window entry/exit events
                    // (they would also occur if window is in background, but
                    // small part of it is visible between two other windows,
                    // and the mouse is for a short period over that visible
                    // part)
                    if (((MouseEvent)e).getButton() != MouseEvent.NOBUTTON)
                    {
                        MasterBoard.this.gui.markThatSomethingHappened();
                    }
                }
            }
        }, eventMask);

        setupGUIHexes();
        setupActions();
        setupPopupMenus();
        setupTopMenu();

        scrollPane = new JScrollPane(this);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setupPlayerLabel();

        saveWindow = new SaveWindow(gui.getOptions(), "MasterBoardScreen");
        Point loadLocation = saveWindow.loadLocation();

        if (loadLocation == null)
        {
            // Copy of code from KDialog.centerOnScreen();
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            masterFrame.setLocation(new Point(d.width / 2 - getSize().width
                / 2, d.height / 2 - getSize().height / 2));
        }
        else
        {
            masterFrame.setLocation(loadLocation);
        }

        masterFrame.pack();
        masterFrame.setVisible(true);

        // it seems Board needs to request at least once focus,
        // otherwise the pop-up keys (Legion FlyOuts) do not work.
        // YES; here we really mean this.requestFocus(), not the one which
        // does it only if stealFocus option is enabled.
        requestFocus();
    }

    // For HotSeatMode
    public void setBoardActive(boolean val)
    {
        if (val)
        {
            masterFrame.setExtendedState(JFrame.NORMAL);
            masterFrame.repaint();
            maybeRequestFocusAndToFront();
        }
        else
        {
            masterFrame.setExtendedState(JFrame.ICONIFIED);
        }
    }

    public void enableSaveActions()
    {
        saveGameAction.setEnabled(true);
        saveGameAsAction.setEnabled(true);
    }

    /**
     * Inform the user that saving during an engagement will save the last
     * commit point, so loading it will re-set game to just before the
     * engagement was started.
     * Only if user closes the dialog, no save will be done.
     *
     * @return True if saving shall be done anyway
     */
    private boolean saveDuringEngagementDialog()
    {
        if (saveDuringEngagementDialogMessageShown)
        {
            return true;
        }

        saveDuringEngagementDialogMessageShown = true;

        String[] options = new String[1];
        options[0] = "OK";
        int answer = JOptionPane.showOptionDialog(masterFrame,
            "Saving/Loading of game state while an engagement or battle "
                + "is ongoing is not implemented!\n"
                + "Saved game will store the game state "
                + "from the point just before the engagement started.\n\n"
                + "(this message will only be shown once in each game)",
            "Save at this phase not implemented!", JOptionPane.OK_OPTION,
            JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        return (answer == JOptionPane.OK_OPTION);
    }

    private Constants.ConfirmVals confirmDialog(String text, int count)
    {
        String[] options = new String[3];
        options[0] = "Yes";
        options[1] = "No";
        options[2] = "Don't ask again";
        int answer;
        String message = "You have ";
        if (count == 1)
            message = message + "a legion ";
        else
            message = message + count + " legions ";

        answer = JOptionPane.showOptionDialog(this, message + text
            + ", are you sure you are done?", "Confirm done?",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            options, options[1]);

        if (answer == 0)
        {
            return Constants.ConfirmVals.Yes;
        }
        if (answer == 1 || answer == -1)
        {
            return Constants.ConfirmVals.No;
        }
        return Constants.ConfirmVals.DoNotAsk;
    }

    private void setupActions()
    {
        clearRecruitChitsAction = new AbstractAction(clearRecruitChits)
        {
            public void actionPerformed(ActionEvent e)
            {
                clearRecruitedChits();
                clearPossibleRecruitChits();
                // TODO Only repaint needed hexes.
                repaint();
            }
        };

        skipLegionAction = new AbstractAction(skipLegion)
        {
            public void actionPerformed(ActionEvent e)
            {
                markLegionSkip();
            }
        };

        nextLegionAction = new AbstractAction(nextLegion)
        {
            public void actionPerformed(ActionEvent e)
            {
                jumpToNextUnhandledLegion();
            }
        };

        destroyLegionAction = new AbstractAction(destroyLegion)
        {
            public void actionPerformed(ActionEvent e)
            {
                destroyThisLegion();
            }
        };

        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                Phase phase = client.getPhase();
                if (phase == Phase.SPLIT)
                {
                    gui.undoLastSplit();
                    alignAllLegions();
                    highlightTallLegions();
                    repaint();
                }
                else if (phase == Phase.MOVE)
                {
                    clearRecruitedChits();
                    clearPossibleRecruitChits();
                    gui.undoLastMove();
                    highlightUnmovedLegions();
                }
                else if (phase == Phase.FIGHT)
                {
                    LOGGER.log(Level.SEVERE, "called undoLastAction in FIGHT");
                }
                else if (phase == Phase.MUSTER)
                {
                    gui.undoLastRecruit();
                    highlightPossibleRecruitLegionHexes();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase for Undo Last");
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                Phase phase = client.getPhase();
                if (phase == Phase.SPLIT)
                {
                    gui.undoAllSplits();
                    alignAllLegions();
                    highlightTallLegions();
                    repaint();
                }
                else if (phase == Phase.MOVE)
                {
                    gui.undoAllMoves();
                    highlightUnmovedLegions();
                    if (gui.isMyTurn())
                    {
                        updateLegionsLeftToMoveText(true);
                    }
                }
                else if (phase == Phase.FIGHT)
                {
                    LOGGER.log(Level.SEVERE, "called undoAllAction in FIGHT");
                }
                else if (phase == Phase.MUSTER)
                {
                    gui.undoAllRecruits();
                    highlightPossibleRecruitLegionHexes();
                    if (gui.isMyTurn())
                    {
                        updateLegionsLeftToMusterText();
                    }
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase for Undo All");
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (gameOverStateReached)
                {
                    gui.askNewCloseQuitCancel(masterFrame, false);
                }
                else
                {
                    // first set disabled...
                    doneWithPhaseAction.setEnabled(false);
                    // ... because response from server might set it
                    // to enabled again
                    doneWithPhase();
                }
                focusBackToMasterboard();
            }
        };
        // will be enabled if it is player's turn
        doneWithPhaseAction.setEnabled(false);

        forcedDoneWithPhaseAction = new AbstractAction(forcedDoneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                doneWithPhase();
            }
        };
        // make this always be available
        forcedDoneWithPhaseAction.setEnabled(true);

        kickPhaseAction = new AbstractAction(kickPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                gui.client.getAutoplay().switchOnInactivityAutoplay();
                gui.client.getEventExecutor().retriggerEvent();
                // gui.getClient().kickPhase();
            }
        };

        takeMulliganAction = new AbstractAction(takeMulligan)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.mulligan();
            }
        };

        requestExtraRollAction = new AbstractAction(requestExtraRoll)
        {
            public void actionPerformed(ActionEvent e)
            {
                /*
                 * This is needed, because it's too difficult to orchestrate
                 * from server side to undo all moves and clear undoStack
                 * etc. (that problem does not arise with mulligan because
                 * there the undo can be done when sending request).
                 * Didn't want to add yet another 'mulligan approved' message,
                 * and worse, there might be a timing problem (undoMoves
                 * ongoing while new roll arives).
                 */
                if (gui.isUndoStackEmpty())
                {
                    client.requestExtraRoll();
                    requestExtraRollAction.setEnabled(false);
                }
                else
                {
                    JOptionPane.showMessageDialog(masterFrame,
                        "To request the extra roll, you need to undo all moves "
                            + "first (press 'A' or multiple times 'U')!",
                        "Undo all moves first",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };

        withdrawFromGameAction = new AbstractAction(withdrawFromGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.isAlive())
                {
                    JOptionPane.showMessageDialog(masterFrame,
                        "You can't withdraw, "
                            + " because you are already dead!",
                        "You're already dead, dummy!",
                        JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you wish to withdraw from the game?",
                    "Confirm Withdrawal?", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    client.withdrawFromGame();
                }
            }
        };

        viewFullRecruitTreeAction = new AbstractAction(viewFullRecruitTree)
        {
            public void actionPerformed(ActionEvent e)
            {
                Variant variant = gui.getGame().getVariant();
                new ShowAllRecruits(masterFrame, gui.getOptions(), variant,
                    gui);
            }
        };

        viewWebClientAction = new AbstractAction(viewWebClient)
        {
            public void actionPerformed(ActionEvent e)
            {
                gui.showWebClient();
            }
        };

        viewHexRecruitTreeAction = new AbstractAction(viewHexRecruitTree)
        {
            public void actionPerformed(ActionEvent e)
            {
                GUIMasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    // TODO replace with actual ...getVariant() when Variant is
                    //      ready to provide the needed data
                    Variant variant = gui.getGame().getVariant();
                    MasterHex hexModel = hex.getHexModel();
                    new ShowRecruits(masterFrame, lastPoint, hexModel,
                        scrollPane, variant, gui);
                }
            }
        };

        viewBattleMapAction = new AbstractAction(viewBattleMap)
        {
            public void actionPerformed(ActionEvent e)
            {
                GUIMasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    showBattleMap(hex);
                }
            }
        };

        viewLegionsAction = new AbstractAction(viewLegions)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (lastPoint == null)
                {
                    LOGGER.warning("Action viewLegions called but "
                        + "lastPoint is null.");
                    return;
                }
                MasterBoard.this.viewLegions(lastPoint);
            }
        };

        /*
         * After confirmation (if necessary, i.e. not gameover yet),
         * totally quit everything (shut down server and all windows)
         * so that the ViableEntityManager knows it can let the main
         * go to the end, ending the JVM.
         */
        quitGameAction = new AbstractAction(Constants.quitGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean quitAll = false;
                if (gui.getGame().isGameOver())
                {
                    quitAll = true;
                }
                else
                {
                    String[] options = new String[2];
                    options[0] = "Yes";
                    options[1] = "No";
                    int answer = JOptionPane.showOptionDialog(masterFrame,
                        "Are you sure you wish to stop this game and quit?",
                        "Quit Game?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options,
                        options[1]);
                    if (answer == JOptionPane.YES_OPTION)
                    {
                        quitAll = true;
                    }
                }

                if (quitAll)
                {
                    // In startObject, set up what to do next
                    gui.menuQuitGame();
                }
            }
        };

        cleanDisconnectAction = new AbstractAction(Constants.cleanDisconnect)
        {
            public void actionPerformed(ActionEvent e)
            {
                LOGGER.info("Pure disconnect by client (from File Menu)!");
                client.abandonCurrentConnection();
            }
        };

        enforcedDisconnectByServerAction = new AbstractAction(
            Constants.enforcedDisconnectByServer)
        {
            public void actionPerformed(ActionEvent e)
            {
                LOGGER.info("Enforcing disconnect by server!");
                gui.getClient().enforcedDisconnectByServer();
            }
        };

        tryReconnectAction = new AbstractAction(Constants.tryReconnect)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.guiTriggeredTryReconnect();
            }
        };

        newGameAction = new AbstractAction(Constants.newGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!gui.getGame().isGameOver())
                {
                    String[] options = new String[2];
                    options[0] = "Yes";
                    options[1] = "No";
                    int answer = JOptionPane.showOptionDialog(masterFrame,
                        "Are you sure you want to stop this game and "
                            + "start a new one?", "New Game?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options,
                        options[1]);

                    if (answer != JOptionPane.YES_OPTION)
                    {
                        return;
                    }
                }
                gui.menuNewGame();
            }
        };

        loadGameAction = new AbstractAction(Constants.loadGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                // No need for confirmation because the user can cancel
                // from the load game dialog.
                JFileChooser chooser = new JFileChooser(
                    Constants.SAVE_DIR_NAME);
                chooser.setFileFilter(new XMLSnapshotFilter());
                int returnVal = chooser.showOpenDialog(masterFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    gui.menuLoadGame(chooser.getSelectedFile().getPath());
                }
            }
        };

        saveGameAction = new AbstractAction(Constants.saveGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean proceed = true;
                if (gui.getGame().isEngagementOngoing())
                {
                    proceed = saveDuringEngagementDialog();
                }
                if (proceed)
                {
                    gui.menuSaveGame(null);
                }

            }
        };

        saveGameAsAction = new AbstractAction(Constants.saveGameAs)
        {
            // TODO: Need a confirmation dialog on overwrite?
            public void actionPerformed(ActionEvent e)
            {
                if (gui.getGame().isEngagementOngoing())
                {
                    boolean proceed = saveDuringEngagementDialog();
                    if (!proceed)
                    {
                        return;
                    }
                }

                File savesDir = new File(Constants.SAVE_DIR_NAME);
                if (!savesDir.exists())
                {
                    LOGGER.log(Level.INFO, "Trying to make directory "
                        + savesDir.toString());
                    if (!savesDir.mkdirs())
                    {
                        LOGGER.log(Level.SEVERE,
                            "Could not create saves directory");
                        JOptionPane
                            .showMessageDialog(
                                masterFrame,
                                "Could not create directory "
                                    + savesDir
                                    + "\n- FileChooser dialog box will default "
                                    + "to some other (system dependent) directory!",
                                "Creating directory " + savesDir + " failed!",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                else if (!savesDir.isDirectory())
                {
                    LOGGER.log(Level.SEVERE, "Can't create directory "
                        + savesDir.toString()
                        + " - name exists but is not a file");
                    JOptionPane.showMessageDialog(masterFrame,
                        "Can't create directory " + savesDir
                            + " (name exists, but is not a file)\n"
                            + "- FileChooser dialog box will default to "
                            + "some other (system dependent) directory!",
                        "Creating directory " + savesDir + " failed!",
                        JOptionPane.ERROR_MESSAGE);
                }

                JFileChooser chooser = new JFileChooser(savesDir);
                chooser.setFileFilter(new XMLSnapshotFilter());
                int returnVal = chooser.showSaveDialog(masterFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    String dirname = chooser.getCurrentDirectory()
                        .getAbsolutePath();
                    String basename = chooser.getSelectedFile().getName();
                    // Add default savegame extension.
                    if (!basename.endsWith(Constants.XML_EXTENSION))
                    {
                        basename += Constants.XML_EXTENSION;
                    }
                    gui.menuSaveGame(dirname + '/' + basename);
                }
            }
        };

        suspendGameAction = new AbstractAction(Constants.suspendGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean proceed = true;
                /*
                if (gui.getGame().isEngagementOngoing())
                {
                    proceed = saveDuringEngagementDialog();
                }
                */
                if (proceed)
                {
                    gui.menuSuspendGame(true);
                }
            }
        };

        suspendNoSaveGameAction = new AbstractAction(
            Constants.suspendGameNoSave)
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean proceed = true;
                /*
                if (gui.getGame().isEngagementOngoing())
                {
                    proceed = saveDuringEngagementDialog();
                }
                */
                if (proceed)
                {
                    gui.menuSuspendGame(false);
                }
            }
        };

        /*
         * after confirmation, close board and perhaps battle board, but
         * Webclient (and server, if running here), will go on.
         */
        closeBoardAction = new AbstractAction(Constants.closeBoard)
        {
            public void actionPerformed(ActionEvent e)
            {
                boolean closeBoard = false;
                if (gui.getGame().isGameOver() || !client.isAlive()
                    || client.isSpectator())
                {
                    closeBoard = true;
                }
                else
                {
                    String[] options = new String[2];
                    options[0] = "Yes";
                    options[1] = "No";
                    int answer = JOptionPane
                        .showOptionDialog(
                            masterFrame,
                            "Are you sure you wish to withdraw and close the board?",
                            "Close Board?", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);
                    if (answer == JOptionPane.YES_OPTION)
                    {
                        closeBoard = true;
                    }
                }

                if (closeBoard)
                {
                    gui.menuCloseBoard();
                }
            }
        };

        checkConnectionAction = new AbstractAction(Constants.checkConnection)
        {
            public void actionPerformed(ActionEvent e)
            {
                gui.checkServerConnection();
            }
        };

        checkAllConnectionsAction = new AbstractAction(
            Constants.checkAllConnections)
        {
            public void actionPerformed(ActionEvent e)
            {
                gui.checkAllConnections();
            }
        };

        chooseScreenAction = new AbstractAction(chooseScreen)
        {
            public void actionPerformed(ActionEvent e)
            {
                new ChooseScreen(getFrame(), gui);
            }
        };

        preferencesAction = new AbstractAction(preferences)
        {
            public void actionPerformed(ActionEvent e)
            {
                gui.setPreferencesWindowVisible(true);
            }
        };

        aboutAction = new AbstractAction(about)
        {
            public void actionPerformed(ActionEvent e)
            {
                String buildInfo = BuildInfo.getFullBuildInfoString() + "\n"
                    + "user.home:      " + System.getProperty("user.home");
                String colossusHome = System.getProperty("user.home")
                    + File.separator + ".colossus";
                String logDirectory = getLogDirectory();

                JOptionPane.showMessageDialog(masterFrame, ""
                        + "Colossus Version: " + BuildInfo.getReleaseVersion()
                        + "\n" + "Build:  " + buildInfo + "\n"
                        + "Colossus home:  " + colossusHome + "\n"
                        + "Log directory:  " + logDirectory + "\n"
                        + "Java Version:  " + SystemInfo.getDisplayJavaInfo(),
                    "About Colossus", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        viewReadmeAction = new AbstractAction(viewReadme)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (showReadme != null)
                {
                    showReadme.dispose();
                }
                showReadme = new ShowReadme(gui.getGame().getVariant());
            }
        };
        viewHelpDocAction = new AbstractAction(viewHelpDoc)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (showHelpDoc != null)
                {
                    showHelpDoc.dispose();
                }
                showHelpDoc = new ShowHelpDoc();
            }
        };
        viewWelcomeAction = new AbstractAction(viewWelcome)
        {
            public void actionPerformed(ActionEvent e)
            {
                WelcomeDialog.showWelcomeDialog();
            }
        };

    }

    // Derive the logDirectory from the FileHandler.pattern property
    // (in case someone modified the default in logging.properties file)
    // to show it in Help-About dialog.
    private String getLogDirectory()
    {
        String propName = "java.util.logging.FileHandler.pattern";
        String logPattern = LogManager.getLogManager().getProperty(propName);

        if (logPattern == null)
        {
            return "<UNKNOWN> (system property "
                + " \"java.util.logging.FileHandler.pattern\" is not set?)";
        }
        // Replace %t with Java's idea of system temp directory:
        if (logPattern.startsWith("%t"))
        {
            String tempDir = System.getProperty("java.io.tmpdir");
            logPattern = tempDir + logPattern.substring(2);
        }

        // get only the directory part:
        String logDirectory = logPattern;
        try
        {
            File logFileWouldBe = new File(logPattern);
            logDirectory = logFileWouldBe.getParent();
        }
        catch (Exception ex)
        {
            LOGGER.warning("Exception while trying to determine log "
                + "directory from logPattern " + logPattern
                + " - ignoring it.");
        }

        // initialize logPath with what we have so far...
        String logPath = logDirectory;

        // ... and try to resolve "DOCUME~1" stuff to real names on windows:
        try
        {
            File logDir = new File(logDirectory);
            logPath = logDir.getCanonicalPath();
        }
        catch (Exception exc)
        {
            // ignore it...
        }
        return logPath;
    }

    public void doQuitGameAction()
    {
        quitGameAction.actionPerformed(null);
    }

    public void showBattleMap(GUIMasterHex hex)
    {
        new ShowBattleMap(masterFrame, gui, hex);
    }

    private void setupPopupMenus()
    {
        popupMenu = new JPopupMenu();
        contentPane.add(popupMenu);

        JMenuItem mi = popupMenu.add(viewHexRecruitTreeAction);
        mi.setMnemonic(KeyEvent.VK_R);

        mi = popupMenu.add(viewBattleMapAction);
        mi.setMnemonic(KeyEvent.VK_B);

        popupMenuWithLegions = new JPopupMenu();
        contentPane.add(popupMenu);

        mi = popupMenuWithLegions.add(viewHexRecruitTreeAction);
        mi.setMnemonic(KeyEvent.VK_R);

        mi = popupMenuWithLegions.add(viewBattleMapAction);
        mi.setMnemonic(KeyEvent.VK_B);

        mi = popupMenuWithLegions.add(viewLegionsAction);
        mi.setMnemonic(KeyEvent.VK_L);

    }

    public void viewLegions(Point point)
    {
        GUIMasterHex hex = getHexContainingPoint(point);
        if (hex == null)
        {
            return;
        }

        int viewMode = gui.getEffectiveViewMode();
        boolean dubiousAsBlanks = gui.getOptions().getOption(
            Options.dubiousAsBlanks);
        boolean showMarker = gui.getOptions().getOption(Options.showMarker);

        List<Legion> legions = gui.getGame()
            .getLegionsByHex(hex.getHexModel());
        for (Legion legion : legions)
        {
            Marker marker = legionToMarkerMap.get(legion);
            int chitScale = marker.getBounds().width;
            Point markerPoint = marker.getLocation();
            // upper right corner
            markerPoint.setLocation(markerPoint.x + chitScale, markerPoint.y);
            new ShowLegion(masterFrame, (LegionClientSide)legion, markerPoint,
                scrollPane, 4 * Scale.get(), viewMode,
                client.isMyLegion(legion), dubiousAsBlanks, showMarker);
        }
    }

    private ItemListener itemHandler = new MasterBoardItemHandler();

    private JCheckBoxMenuItem addCheckBox(JMenu menu, String name, int mnemonic)
    {
        JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(name);
        cbmi.setMnemonic(mnemonic);
        cbmi.setSelected(gui.getOptions().getOption(name));

        cbmi.addItemListener(itemHandler);
        menu.add(cbmi);
        checkboxes.put(name, cbmi);
        return cbmi;
    }

    private void cleanCBListeners()
    {
        Iterator<String> it = checkboxes.keySet().iterator();
        while (it.hasNext())
        {
            String key = it.next();
            JCheckBoxMenuItem cbmi = checkboxes.get(key);
            cbmi.removeItemListener(itemHandler);
        }
        checkboxes.clear();
        checkboxes = null;
        itemHandler = null;
    }

    private void setupTopMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        masterFrame.setJMenuBar(menuBar);

        // File menu

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        JMenuItem mi;

        mi = fileMenu.add(newGameAction);
        mi.setMnemonic(KeyEvent.VK_N);
        if (!client.isRemote())
        {
            // saving not possible during game setup phase; enable later
            saveGameAction.setEnabled(false);
            saveGameAsAction.setEnabled(false);
            mi = fileMenu.add(loadGameAction);
            mi.setMnemonic(KeyEvent.VK_L);
            mi = fileMenu.add(saveGameAction);
            mi.setMnemonic(KeyEvent.VK_S);
            mi = fileMenu.add(saveGameAsAction);
            mi.setMnemonic(KeyEvent.VK_A);
        }

        boolean showMenuItem = gui.getStartedByWebClient();
        // showMenuItem = true; // for local testing
        if (showMenuItem)
        {
            mi = fileMenu.add(suspendGameAction);
            mi.setMnemonic(KeyEvent.VK_U);

            mi = fileMenu.add(suspendNoSaveGameAction);
            mi.setMnemonic(KeyEvent.VK_O);
        }

        fileMenu.addSeparator();

        mi = fileMenu.add(checkConnectionAction);
        mi.setMnemonic(KeyEvent.VK_K);

        mi = fileMenu.add(checkAllConnectionsAction);
        mi.setMnemonic(KeyEvent.VK_X);

        /* Removed for r-0.13.2, since it does not work - it merely makes the
         * client stop processing stuff from the queue, but ClientSocketThread
         * keeps receiving and ack'ing lines from/to server.
         */
        boolean _CLEAN_DISCONNECT = false;
        if (_CLEAN_DISCONNECT)
        {
            mi = fileMenu.add(cleanDisconnectAction);
            mi.setMnemonic(KeyEvent.VK_Y);
        }

        mi = fileMenu.add(tryReconnectAction);
        mi.setMnemonic(KeyEvent.VK_R);

        // only for debugging/testing
        boolean _DISCONNECT_BY_SERVER = true;
        if (!client.isRemote() && _DISCONNECT_BY_SERVER)
        {
            mi = fileMenu.add(enforcedDisconnectByServerAction);
            mi.setMnemonic(KeyEvent.VK_Z);
        }

        fileMenu.addSeparator();

        mi = fileMenu.add(closeBoardAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi = fileMenu.add(quitGameAction);
        mi.setMnemonic(KeyEvent.VK_Q);

        // Edit menu

        if (gui.getOptions().getOption(Options.enableEditingMode)
            && !client.isRemote())
        {
            editMenu = new JMenu("Edit");
            editMenu.setMnemonic(KeyEvent.VK_E);
            menuBar.add(editMenu);

            addCheckBox(editMenu, Options.editModeActive, KeyEvent.VK_E);
        }

        // Phase menu

        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);

        mi = phaseMenu.add(clearRecruitChitsAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

        mi = phaseMenu.add(skipLegionAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));

        mi = phaseMenu.add(nextLegionAction);
        mi.setMnemonic(KeyEvent.VK_N);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));

        boolean CLEMENS_TEST_2 = false;
        if (CLEMENS_TEST_2)
        {
            mi = phaseMenu.add(destroyLegionAction);
            mi.setMnemonic(KeyEvent.VK_R);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        }

        phaseMenu.addSeparator();

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

        mi = phaseMenu.add(undoAllAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0));

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        mi = phaseMenu.add(forcedDoneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_F);

        boolean CLEMENS_TEST = false;
        if (CLEMENS_TEST)
        {
            mi = phaseMenu.add(kickPhaseAction);
            mi.setMnemonic(KeyEvent.VK_K);
        }

        phaseMenu.addSeparator();

        mi = phaseMenu.add(takeMulliganAction);
        mi.setMnemonic(KeyEvent.VK_M);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));

        mi = phaseMenu.add(requestExtraRollAction);
        mi.setMnemonic(KeyEvent.VK_X);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0));

        phaseMenu.addSeparator();

        if (!client.isSpectator())
        {
            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);
        }

        // Window menu: menu for the "window-related"
        // (satellite windows and graphic actions effecting whole "windows"),
        // Plus the Preferences window as last entry.

        JMenu windowMenu = new JMenu("Window");
        windowMenu.setMnemonic(KeyEvent.VK_W);
        menuBar.add(windowMenu);

        addCheckBox(windowMenu, Options.showCaretaker, KeyEvent.VK_C);
        addCheckBox(windowMenu, Options.showStatusScreen, KeyEvent.VK_G);
        addCheckBox(windowMenu, Options.showEngagementResults, KeyEvent.VK_E);
        addCheckBox(windowMenu, Options.showAutoInspector, KeyEvent.VK_I);
        addCheckBox(windowMenu, Options.showEventViewer, KeyEvent.VK_E);
        addCheckBox(windowMenu, Options.showLogWindow, KeyEvent.VK_L);
        addCheckBox(windowMenu, Options.showConnectionLogWindow, KeyEvent.VK_O);

        // full recruit tree
        mi = windowMenu.add(viewFullRecruitTreeAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
        mi.setMnemonic(KeyEvent.VK_R);

        // web client
        mi = windowMenu.add(viewWebClientAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0));
        mi.setMnemonic(KeyEvent.VK_W);

        windowMenu.addSeparator();

        // Then the "do something to a Window" actions;
        // and Preferences Window as last:

        if (GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getScreenDevices().length > 1)
        {
            mi = windowMenu.add(chooseScreenAction);
        }

        mi = windowMenu.add(preferencesAction);
        mi.setMnemonic(KeyEvent.VK_P);

        // Then help menu

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        mi = helpMenu.add(aboutAction);
        mi = helpMenu.add(viewReadmeAction);
        mi.setMnemonic(KeyEvent.VK_V);

        mi = helpMenu.add(viewHelpDocAction);
        mi = helpMenu.add(viewWelcomeAction);
    }

    /**
     * Find the checkbox for the given (boolean) option name; set it to the
     * new given value (only if different that previous value).
     *
     * @param name The option name to adjust the checkbox for
     * @param enable The should-be state of the checkbox
     */
    void adjustCheckboxIfNeeded(String name, boolean enable)
    {
        JCheckBoxMenuItem cbmi = checkboxes.get(name);
        if (cbmi != null)
        {
            // Only set the selected state if it has changed,
            // to avoid infinite feedback loops.
            boolean previous = cbmi.isSelected();
            if (enable != previous)
            {
                cbmi.setSelected(enable);
            }
        }
    }

    /** Show which player owns this board. */
    void setupPlayerLabel()
    {
        if (playerLabelDone)
        {
            return;
        }
        String playerName = gui.getOwningPlayerName();
        cachedPlayerName = playerName;
        if (bottomBar == null)
        {
            // add a bottom bar
            bottomBar = new BottomBar();
            contentPane.add(bottomBar, BorderLayout.SOUTH);

            // notify
            masterFrame.pack();
        }
        bottomBar.setPlayerName(playerName);

        PlayerColor clientColor = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (clientColor != null)
        {
            Color color = clientColor.getBackgroundColor();
            bottomBar.setPlayerColor(color);
            // Don't do this again.
            playerLabelDone = true;
        }
    }

    private void setupGUIHexes()
    {
        guiHexArray = new GUIMasterHex[getMasterBoard().getHorizSize()][getMasterBoard()
            .getVertSize()];

        int scale = Scale.get();
        int cx = 3 * scale;
        int cy = 0 * scale;

        for (int i = 0; i < guiHexArray.length; i++)
        {
            for (int j = 0; j < guiHexArray[0].length; j++)
            {
                if (getMasterBoard().getShow()[i][j])
                {
                    GUIMasterHex hex = new GUIMasterHex(getMasterBoard()
                        .getPlainHexArray()[i][j]);
                    hex.init(cx + 4 * i * scale, (int)Math.round(cy
                        + (3 * j
                            + ((i + getMasterBoard().getBoardParity()) & 1)
                            * (1 + 2 * (j / 2)) + ((i + 1 + getMasterBoard()
                            .getBoardParity()) & 1) * 2 * ((j + 1) / 2))
                        * GUIHex.SQRT3 * scale), scale, getMasterBoard()
                        .isHexInverted(i, j), this);
                    guiHexArray[i][j] = hex;
                }
            }
        }
    }

    /**
     * TODO this should probably be stored as member, possibly instead of the client class.
     */
    private net.sf.colossus.variant.MasterBoard getMasterBoard()
    {
        return gui.getGame().getVariant().getMasterBoard();
    }

    private void cleanGUIHexes()
    {
        for (int i = 0; i < guiHexArray.length; i++)
        {
            for (int j = 0; j < guiHexArray[0].length; j++)
            {
                if (getMasterBoard().getShow()[i][j])
                {
                    GUIMasterHex hex = guiHexArray[i][j];
                    hex.cleanup();
                    guiHexArray[i][j] = null;
                }
            }
        }
    }

    /**
     * Finishes the current phase.
     *
     * Depending on the current phase this method dispatches to
     * the different done methods.
     *
     * @see Client#doneWithSplits()
     * @see Client#doneWithMoves()
     * @see Client#doneWithEngagements()()
     * @see Client#doneWithRecruits()()
     */
    private void doneWithPhase()
    {
        Constants.ConfirmVals answer;

        if (gui.getGame().isPhase(Phase.SPLIT))
        {
            boolean beDone = true;
            int cnt;
            if ((cnt = client.findTallLegionHexes(7, false).size()) > 0
                && (client.getOwningPlayer().getMarkersAvailable().size() >= 1)
                && gui.getOptions().getOption(Options.confirmNoSplit, true))
            {
                answer = confirmDialog(
                    (cnt == 1) ? "with 7 creatures that has not split"
                        : "with 7 creatures that have not split", cnt);
                if (answer == Constants.ConfirmVals.DoNotAsk)
                {
                    client.setPreferencesCheckBoxValue(Options.confirmNoSplit,
                        false);
                    beDone = true;
                }
                else if (answer == Constants.ConfirmVals.No)
                {
                    beDone = false;
                }
                else
                {
                    beDone = true;
                }
            }
            if (beDone)
            {
                client.doneWithSplits();
            }
            else
            {
                // dispatcher disabled Done button before calling us here
                doneWithPhaseAction.setEnabled(true);
            }
        }
        else if (gui.getGame().isPhase(Phase.MOVE))
        {
            boolean beDone = true;
            int mcModeInt = gui.getLegionMoveConfirmationMode();

            if ((mcModeInt == Options.legionMoveConfirmationNumMove)
                || (mcModeInt == Options.legionMoveConfirmationNumUnvisitedMove))
            {
                int legionStatus[] = { 0, 0, 0, 0 };
                int cnt;

                answer = Constants.ConfirmVals.No;

                client.legionsNotMoved(legionStatus, true);
                if (mcModeInt == Options.legionMoveConfirmationNumUnvisitedMove)
                {
                    cnt = legionStatus[Constants.legionStatusNotVisitedSkippedBlocked];
                    if (cnt > 0)
                    {
                        answer = confirmDialog(
                            (cnt == 1) ? "that has not moved or been visited"
                                : "that have not moved or been visited", cnt);
                    }
                }
                else
                {
                    cnt = legionStatus[Constants.legionStatusCount]
                        - legionStatus[Constants.legionStatusMoved];
                    if (cnt > 0)
                    {
                        answer = confirmDialog(
                            (cnt == 1) ? "that has not moved"
                                : "that have not moved", cnt);
                    }
                }
                beDone = true;
                if (cnt > 0)
                {
                    if (answer == Constants.ConfirmVals.DoNotAsk)
                    {
                        client.setPreferencesRadioButtonValue(
                            Options.legionMoveConfirmationNoMove, false);
                        client.setPreferencesRadioButtonValue(
                            Options.legionMoveConfirmationNoUnvisitedMove,
                            false);
                        client.setPreferencesRadioButtonValue(
                            Options.legionMoveConfirmationNoConfirm, true);
                        beDone = true;
                    }
                    else if (answer == Constants.ConfirmVals.No)
                    {
                        beDone = false;
                    }
                }
            }
            if (beDone)
            {
                client.doneWithMoves();
                bottomBar.setLegionsLeftToMove(0);
            }
            else
            {
                // dispatcher disabled Done button before calling us here
                doneWithPhaseAction.setEnabled(true);
            }
        }
        else if (gui.getGame().isPhase(Phase.FIGHT))
        {
            client.doneWithEngagements();
        }
        else if (gui.getGame().isPhase(Phase.MUSTER))
        {
            boolean beDone = true;
            int cnt;
            if ((cnt = client.getPossibleRecruitHexes().size()) > 0
                && gui.getOptions().getOption(Options.confirmNoRecruit, true))
            {
                answer = confirmDialog((cnt == 1) ? "that has not mustered"
                    : "that have not mustered", cnt);
                if (answer == Constants.ConfirmVals.DoNotAsk)
                {
                    client.setPreferencesCheckBoxValue(
                        Options.confirmNoRecruit, false);
                    beDone = true;
                }
                else if (answer == Constants.ConfirmVals.No)
                {
                    beDone = false;
                }
                else
                {
                    beDone = true;
                }
            }
            if (beDone)
            {
                client.doneWithRecruits();
                bottomBar.setLegionsLeftToMuster(0);
            }
            else
            {
                // dispatcher disabled Done button before calling us here
                doneWithPhaseAction.setEnabled(true);
            }
        }
        else
        {
            throw new IllegalStateException("Client has unknown phase value");
        }
    }

    private void setupPhasePreparations(String titleText)
    {
        if (gui.isMyTurn())
        {
            gui.resetAllLegionFlags();
            setTitleInfoText(titleText);
            bottomBar.setPhase(titleText);
        }
        unselectAllHexes();
    }

    public void updateSplitPendingText(String titleText)
    {
        setTitleInfoText(titleText);
        bottomBar.setPhase(titleText);
    }

    /**
     * Do the setup needed for an inactive player:
     * set the actions which are allowed only for active player to inactive,
     * and update the bottomBar info why "Done" is disabled accordingly
     *
     */
    private void setupAsInactivePlayer()
    {
        undoLastAction.setEnabled(false);
        undoAllAction.setEnabled(false);
        forcedDoneWithPhaseAction.setEnabled(false);
        takeMulliganAction.setEnabled(false);
        requestExtraRollAction.setEnabled(false);
        String text = client.getActivePlayer() + " "
            + client.getPhase().getDoesWhat();
        disableDoneActionActivePlayerDoes(text);
        setTitleInfoText(text);
    }

    void setupSplitMenu()
    {
        setupPhasePreparations("Split stacks");

        if (gui.isMyTurn())
        {
            undoLastAction.setEnabled(true);
            undoAllAction.setEnabled(true);
            forcedDoneWithPhaseAction.setEnabled(true);
            takeMulliganAction.setEnabled(false);
            requestExtraRollAction.setEnabled(false);
            enableDoneAction();

            // If not first turn, then add Marker Count Text to the bottom display bar
            if (client.getTurnNumber() != 1)
            {
                bottomBar.setMarkerCount(client.getOwningPlayer()
                    .getMarkersAvailable().size());
            }
            highlightTallLegions();
        }
        else
        {
            setupAsInactivePlayer();
        }
    }

    void setupMoveMenu()
    {
        setupPhasePreparations("Move legions");

        if (gui.isMyTurn())
        {
            undoLastAction.setEnabled(true);
            undoAllAction.setEnabled(true);
            forcedDoneWithPhaseAction.setEnabled(true);
            boolean mullLeft = (gui.getOwningPlayer().getMulligansLeft() > 0);
            takeMulliganAction.setEnabled(mullLeft ? true : false);
            requestExtraRollAction.setEnabled(true);
            disableDoneAction("At least one legion must move");
            setMovementPhase();
            highlightUnmovedLegions();
            updateLegionsLeftToMoveText(false);
            maybeRequestFocusAndToFront();
        }
        else
        {
            setupAsInactivePlayer();
        }
    }

    public void setMovementPhase()
    {
        bottomBar.setPhase("Move legions");
    }

    void setupFightMenu()
    {
        setupPhasePreparations("Resolve engagements");

        if (gui.isMyTurn())
        {
            undoLastAction.setEnabled(false);
            undoAllAction.setEnabled(false);
            forcedDoneWithPhaseAction.setEnabled(true);
            takeMulliganAction.setEnabled(false);
            requestExtraRollAction.setEnabled(false);
            // if there are no engagements, we are kicked to next phase
            // automatically anyway.
            updateEngagementsLeftText();
            highlightEngagements();
            maybeRequestFocusAndToFront();
        }
        else
        {
            setupAsInactivePlayer();
        }
    }

    public void updateEngagementsLeftText()
    {
        int count = client.getGame().findEngagements().size();
        String ongoing = "";
        MasterHex engagedHex = client.getGame().getBattleSite();
        if (engagedHex != null)
        {
            ongoing = "engaged on " + engagedHex.getDescription()
                + "; after that ";
            count--;
        }
        disableDoneAction(ongoing + count + " more engagement"
            + (count == 1 ? "" : "s") + " to resolve");
    }

    void setupMusterMenu()
    {
        setupPhasePreparations("Muster recruits");

        if (gui.isMyTurn())
        {
            // TODO actually it's not a good idea that the ClearRecruitChits
            // action is also allowed in Muster phase - the chit will be
            // cleared from display, but not unrecruited. Might lead to
            // confusion. But then, if one uses that action then it's
            // his own fault ;-)
            undoLastAction.setEnabled(true);
            undoAllAction.setEnabled(true);
            forcedDoneWithPhaseAction.setEnabled(true);
            takeMulliganAction.setEnabled(false);
            requestExtraRollAction.setEnabled(false);
            enableDoneAction();

            updateLegionsLeftToMusterText();
            highlightPossibleRecruitLegionHexes();
            maybeRequestFocusAndToFront();
        }
        else
        {
            setupAsInactivePlayer();
        }
    }

    /**
     * Highlight all hexes with legions that (still) can do recruiting
     */
    void highlightPossibleRecruitLegionHexes()
    {
        unselectAllHexes();
        selectHexes(client.getPossibleRecruitHexes());
    }

    KFrame getFrame()
    {
        return masterFrame;
    }

    /** This is incredibly inefficient. */
    void alignAllLegions()
    {
        ArrayHelper.findFirstMatch(getMasterBoard().getPlainHexArray(),
            new NullCheckPredicate<MasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(MasterHex hex)
                {
                    alignLegions(hex);
                    return false;
                }
            });
    }

    void alignLegions(MasterHex masterHex)
    {
        GUIMasterHex hex = getGUIHexByMasterHex(masterHex);
        if (hex == null)
        {
            return;
        }
        List<Legion> legions = gui.getGameClientSide().getLegionsByHex(
            masterHex);

        int numLegions = legions.size();
        if (numLegions == 0)
        {
            hex.repaint();
            return;
        }

        LegionClientSide legion = (LegionClientSide)legions.get(0);
        Marker marker = legionToMarkerMap.get(legion);
        if (marker == null)
        {
            hex.repaint();
            return;
        }

        int chitScale = marker.getBounds().width;
        Point startingPoint = hex.getOffCenter();
        Point point = new Point(startingPoint);

        if (numLegions == 1)
        {
            // Place legion in the center of the hex.
            int chitScale2 = chitScale / 2;
            point.x -= chitScale2;
            point.y -= chitScale2;
            marker.setLocation(point);
        }
        else if (numLegions == 2)
        {
            // Place legions in NW and SE corners.
            int chitScale4 = chitScale / 4;
            point.x -= 3 * chitScale4;
            point.y -= 3 * chitScale4;
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale4;
            point.y -= chitScale4;
            legion = (LegionClientSide)legions.get(1);
            marker = legionToMarkerMap.get(legion);
            if (marker != null)
            {
                // Second marker can be null when loading during
                // the engagement phase.
                marker.setLocation(point);
            }
        }
        else if (numLegions == 3)
        {
            // Place legions in NW, SE, NE corners.
            int chitScale4 = chitScale / 4;
            point.x -= 3 * chitScale4;
            point.y -= 3 * chitScale4;
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale4;
            point.y -= chitScale4;
            legion = (LegionClientSide)legions.get(1);
            marker = legionToMarkerMap.get(legion);
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale4;
            point.y -= chitScale;
            legion = (LegionClientSide)legions.get(2);
            marker = legionToMarkerMap.get(legion);
            marker.setLocation(point);
        }

        hex.repaint();
    }

    private void alignLegions(Set<MasterHex> hexes)
    {
        for (MasterHex masterHex : hexes)
        {
            alignLegions(masterHex);
        }
    }

    void highlightTallLegions()
    {
        unselectAllHexes();
        selectHexes(client.findTallLegionHexes());
        selectHexes(client.findPendingSplitHexes(), Color.blue);
        selectHexes(client.findPendingUndoSplitHexes(), Color.blue);
        repaint();
        Thread.yield();
    }

    void highlightUnmovedLegions()
    {
        unselectAllHexes();
        selectHexes(gui.getStillToMoveHexes());
        selectHexes(gui.getPendingMoveHexes(), Color.blue);
        gui.setMover(null);
        repaint();
    }

    void setPendingText(String text)
    {
        bottomBar.setPendingText(text);
    }

    // Add Marker Count Text to the bottom display bar
    void setMarkerCount(int markerCount)
    {
        bottomBar.setMarkerCount(markerCount);
    }

    /** Select hexes where this legion can move. */
    private void highlightMoves(LegionClientSide legion)
    {
        unselectAllHexes();

        Set<MasterHex> teleport = client.listTeleportMoves(legion);
        selectHexes(teleport, HTMLColor.purple);

        Set<MasterHex> normal = client.listNormalMoves(legion);
        selectHexes(normal, Color.white);

        Set<MasterHex> combo = new HashSet<MasterHex>();
        combo.addAll(teleport);
        combo.addAll(normal);

        LOGGER.info("MB: addPossibleRecruitChits(LegionClientSide legion, "
            + "Set<MasterHex> hexes)");
        if (gui.getRecruitChitMode() == Options.showRecruitChitsNumNone)
        {
            return;
        }
        addPossibleRecruitChits(legion, combo);
    }

    void highlightEngagements()
    {
        Set<MasterHex> set = gui.getGameClientSide().findEngagements();
        unselectAllHexes();
        selectHexes(set);
    }

    private void setupIcon()
    {
        List<String> directories = new ArrayList<String>();
        directories.add(Constants.defaultDirName
            + StaticResourceLoader.getPathSeparator()
            + Constants.imagesDirName);

        String[] iconNames = {
            Constants.masterboardIconImage,
            Constants.masterboardIconText + "-Name-"
                + Constants.masterboardIconTextColor,
            Constants.masterboardIconSubscript + "-Subscript-"
                + Constants.masterboardIconTextColor };

        Image image = StaticResourceLoader.getCompositeImage(iconNames,
            directories, 60, 60);

        if (image == null)
        {
            LOGGER.log(Level.WARNING, "Couldn't find Colossus icon");
        }
        else
        {
            masterFrame.setIconImage(image);
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    GUIMasterHex getGUIHexByMasterHex(final MasterHex masterHex)
    {
        return ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)
                {
                    return hex.getHexModel().equals(masterHex);
                }
            });
    }

    /** Return the MasterHex that contains the given point, or
     *  null if none does. */
    private GUIMasterHex getHexContainingPoint(final Point point)
    {
        return ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)
                {
                    return hex.contains(point);
                }
            });
    }

    void setMarkerForLegion(Legion legion, Marker marker)
    {
        synchronized (legionToMarkerMap)
        {
            legionToMarkerMap.remove(legion);
            legionToMarkerMap.put(legion, marker);
        }
    }

    void removeMarkerForLegion(Legion legion)
    {
        synchronized (legionToMarkerMap)
        {
            legionToMarkerMap.remove(legion);
            recruitedChits.remove(legion);
        }
    }

    /** Create new markers in response to a rescale. */
    public void recreateMarkers()
    {
        Set<MasterHex> hexesNeedAligning = new HashSet<MasterHex>();
        synchronized (legionToMarkerMap)
        {
            legionToMarkerMap.clear();
            for (Player player : gui.getGameClientSide().getPlayers())
            {
                for (Legion legion : player.getLegions())
                {
                    Marker marker = new Marker(legion, 3 * Scale.get(),
                        legion.getLongMarkerId(), client, true);
                    legionToMarkerMap.put(legion, marker);
                    hexesNeedAligning.add(legion.getCurrentHex());
                }
            }
        }
        alignLegions(hexesNeedAligning);
    }

    /** Return the topmost Marker that contains the given point, or
     *  null if none does. */
    private Marker getMarkerAtPoint(Point point)
    {
        synchronized (legionToMarkerMap)
        {
            Marker marker = null;
            for (Entry<Legion, Marker> entry : legionToMarkerMap.entrySet())
            {
                if (entry.getValue().getBounds().contains(point))
                {
                    marker = entry.getValue();
                }
            }
            return marker;
        }
    }

    // TODO the next couple of methods iterate through all elements of an array
    // by just returning false as predicate value. It should be a different method
    // without return value IMO -- it didn't look as bad when it was called visitor,
    // but a true Visitor shouldn't have the return value. Now the error is the other
    // way around, but at least generified :-)
    void unselectAllHexes()
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    if (hex.isSelected())
                    {
                        hex.unselect();
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
    }

    void selectHex(final MasterHex modelHex)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)
                {
                    if (!hex.isSelected()
                        && modelHex.equals(hex.getHexModel()))
                    {
                        hex.select();
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
    }

    private void selectHexes(final Set<MasterHex> hexes)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)
                {
                    if (!hex.isSelected() && hexes.contains(hex.getHexModel()))
                    {
                        hex.select();
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
    }

    private void selectHexes(final Set<MasterHex> hexes, final Color color)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    if (hexes.contains(hex.getHexModel()))
                    {
                        hex.select();
                        hex.setSelectColor(color);
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
        repaint();
    }

    /*
    private String getFocusOwnerText()
    {
        KeyboardFocusManager kfm = DefaultKeyboardFocusManager
            .getCurrentKeyboardFocusManager();
        if (kfm != null)
        {
            Component fo = kfm.getFocusOwner();
            if (fo != null)
            {
                return fo.toString();
            }
        }
        return "none";
    }
    */

    public void focusBackToMasterboard()
    {
        /* Enforce focus back to MasterBoard. After pressing done itmight be
         * in the bottom-panel (in particular the Pause button, it seems) and
         *  masterboard doesn't get KeyEvents => legion flyouts don't work
         */
        // ("Fowcus owner WAS: " + getFocusOwnerText());
        this.requestFocusInWindow();
        // ("Fowcus owner NOW: " + getFocusOwnerText());
    }

    void actOnMisclick()
    {
        focusBackToMasterboard();

        Phase phase = gui.getGame().getPhase();
        if (phase == Phase.SPLIT)
        {
            highlightTallLegions();
        }
        else if (phase == Phase.MOVE)
        {
            clearPossibleRecruitChits();
            highlightUnmovedLegions();
        }
        else if (phase == Phase.FIGHT)
        {
            highlightEngagements();
        }
        else if (phase == Phase.MUSTER)
        {
            highlightPossibleRecruitLegionHexes();
        }
    }

    /** Return true if the MouseEvent e came from button 2 or 3.
     *  In theory, isPopupTrigger() is the right way to check for
     *  this.  In practice, the poor design choice of only having
     *  isPopupTrigger() fire on mouse release on Windows makes
     *  it useless here. */
    private static boolean isPopupButton(MouseEvent e)
    {
        int modifiers = e.getModifiers();
        return (((modifiers & InputEvent.BUTTON2_MASK) != 0)
            || ((modifiers & InputEvent.BUTTON3_MASK) != 0) || e.isAltDown() || e
                .isControlDown());
    }

    class MasterBoardMouseHandler extends MouseAdapter
    {
        @Override
        public void mousePressed(MouseEvent e)
        {
            Point point = e.getPoint();
            Marker marker = getMarkerAtPoint(point);
            GUIMasterHex hex = getHexContainingPoint(point);
            if (gui.isPointSideMovementDie(point)
                && gui.getOwningPlayer().getMulligansLeft() > 0)
            {
                client.mulligan();
                return;
            }

            if (marker != null)
            {
                String markerId = marker.getId();

                // Move the clicked-on marker to the top of the z-order.
                LegionClientSide legion = client.getLegion(markerId);
                gui.setMarker(legion, marker);

                // Right-click means to show the contents of the legion.
                if (isPopupButton(e))
                {
                    int viewMode = gui.getEffectiveViewMode();
                    boolean dubiousAsBlanks = gui.getOptions().getOption(
                        Options.dubiousAsBlanks);
                    boolean showMarker = gui.getOptions().getOption(
                        Options.showMarker);
                    if (gui.getOptions().getOption(Options.editModeActive))
                    {
                        new EditLegion(gui, masterFrame, legion, point,
                            scrollPane, 4 * Scale.get(), viewMode,
                            client.isMyLegion(legion), dubiousAsBlanks,
                            showMarker);
                    }
                    else
                    {
                        new ShowLegion(masterFrame, legion, point, scrollPane,
                            4 * Scale.get(), viewMode,
                            client.isMyLegion(legion), dubiousAsBlanks,
                            showMarker);
                    }
                    return;
                }
                else if (client.isMyLegion(legion))
                {
                    if (hex != null)
                    {
                        actOnLegion(legion, hex.getHexModel());
                    }
                    else
                    {
                        LOGGER.log(Level.WARNING,
                            "null hex in MasterBoard.mousePressed()");
                    }
                    return;
                }
            }

            // No hits on chits, so check map.
            if (hex != null)
            {
                if (isPopupButton(e))
                {
                    lastPoint = point;
                    if (gui.getGame().getLegionsByHex(hex.getHexModel())
                        .size() > 0)
                    {
                        popupMenuWithLegions.setLabel(hex.getHexModel()
                            .getDescription());
                        popupMenuWithLegions.show(e.getComponent(), point.x,
                            point.y);
                    }
                    else
                    {
                        popupMenu.setLabel(hex.getHexModel().getDescription());
                        popupMenu.show(e.getComponent(), point.x, point.y);
                    }
                    return;
                }

                // Otherwise, the action to take depends on the phase.
                // Only the current player can manipulate game state.
                if (gui.isMyTurn())
                {
                    actOnHex(hex.getHexModel());
                    hex.repaint();
                    return;
                }
            }

            // No hits on chits or map, so re-highlight.
            if (gui.isMyTurn())
            {
                actOnMisclick();
            }
        }
    }

    class MasterBoardMouseMotionHandler extends MouseMotionAdapter
    {
        @Override
        public void mouseMoved(MouseEvent e)
        {
            Point point = e.getPoint();
            Marker marker = getMarkerAtPoint(point);
            if (marker != null)
            {
                gui.showMarker(marker);
            }
            else
            {
                GUIMasterHex hex = getHexContainingPoint(point);
                if (hex != null)
                {
                    gui.showHexRecruitTree(hex);
                }
            }
        }
    }

    private void actOnLegion(LegionClientSide legion, MasterHex hex)
    {
        if (!gui.isMyTurn())
        {
            return;
        }
        if (!gui.client.ensureThatConnected())
        {
            return;
        }
        Phase phase = gui.getGame().getPhase();
        if (phase == Phase.SPLIT)
        {
            client.doSplit(legion);
        }

        else if (phase == Phase.MOVE)
        {
            legion.setVisitedThisPhase(true);
            updateLegionsLeftToMoveText(true);
            client.setCurrentLegionMarkerId(legion.getMarkerId());

            // Allow spin cycle by clicking on chit again.
            if (legion.equals(gui.getMover()))
            {
                actOnHex(hex);
            }
            else
            {
                gui.setMover(legion);
                getGUIHexByMasterHex(hex).repaint();
                highlightMoves(legion);
            }
        }
        else if (phase == Phase.FIGHT)
        {
            attemptEngage(hex);
        }
        else if (phase == Phase.MUSTER)
        {
            client.doRecruit(legion);
        }
    }

    private void actOnHex(MasterHex hex)
    {
        if (!gui.client.ensureThatConnected())
        {
            return;
        }

        Phase phase = gui.getGame().getPhase();

        if (relocateOngoing != null)
        {
            gui.getClient().editRelocateLegion(
                relocateOngoing.getLegion().getMarkerId(), hex.getLabel());
            relocateOngoing.dispose();
            relocateOngoing = null;
        }
        else if (phase == Phase.SPLIT)
        {
            highlightTallLegions();
        }
        else if (phase == Phase.MOVE)
        {
            // If we're moving, and have selected a legion which
            // has not yet moved, and this hex is a legal
            // destination, move the legion here.
            clearRecruitedChits();
            clearPossibleRecruitChits();
            gui.doMove(hex);
            // Would a simple highlightUnmovedLegions() be good enough?
            // Right now its needed also to set mover null...
            // Would a simple highlightUnmovedLegions() be good enough?
            // Right now its needed also to set mover null
            actOnMisclick(); // Yes, even if the move was good.
        }
        else if (phase == Phase.FIGHT)
        {
            attemptEngage(hex);
        }
    }

    public void actOnEditLegionMaybe(Legion legion)
    {
        if (gui.getOptions().getOption(Options.editModeActive))
        {
            if (editLegionOngoing != null
                && editLegionOngoing.getLegion().equals(legion))
            {
                viewEditLegion((LegionClientSide)legion);
            }
        }
    }

    public void setEditOngoing(EditLegion editLegion)
    {
        editLegionOngoing = editLegion;
    }

    public void setRelocateOngoing(EditLegion editLegion)
    {
        relocateOngoing = editLegion;
    }

    /**
     *
     * @param legion the legion which shall be edited
     */
    public void viewEditLegion(LegionClientSide legion)
    {
        int viewMode = gui.getEffectiveViewMode();
        boolean dubiousAsBlanks = gui.getOptions().getOption(
            Options.dubiousAsBlanks);
        boolean showMarker = gui.getOptions().getOption(Options.showMarker);
        Marker marker = legionToMarkerMap.get(legion);
        Point point = marker.getLocation();

        editLegionOngoing = new EditLegion(gui, masterFrame, legion, point,
            scrollPane, 4 * Scale.get(), viewMode, client.isMyLegion(legion),
            dubiousAsBlanks, showMarker);
    }

    /**
     * tellEngagement calls this, now "engaging" is not pending, instead
     * there is a real engagement to be resolved.
     */
    public void clearEngagingPending()
    {
        client.logMsgToServer("I", "clearEngagingPending, was: "
            + engagingPendingHex);
        engagingPendingHex = null;
        // so, right now defender might be asked whether he wants to flee
        defenderFleePhase = true;
        gui.defaultCursor();
    }

    /**
     * We got showConcede or showNegotiate messages, i.e. the phase in which
     * defender might flee is over, thus the message dialog should not tell
     * that any more. This client here should now have some dialog or even
     * the actual battle map anyway.
     */
    public void clearDefenderFlee()
    {
        defenderFleePhase = false;
    }

    private void attemptEngage(MasterHex hex)
    {
        // If we're in FIGHT phase and there are two opposing legions here,
        // initiate the engaging; if already engaging or engaged, inform
        // about that.
        if (gui.getGame().containsOpposingLegions(hex))
        {
            if (gui.getGame().isEngagementOngoing())
            {
                if (defenderFleePhase)
                {
                    final String msg = "Already engaged on "
                        + gui.getGame().getEngagement()
                        + ";\nprobably other player is "
                        + "asked right now whether to flee.";
                    final String title = "Already engaged!";
                    Runnable messageDialogRunnable = new Runnable()
                    {
                        public void run()
                        {
                            JOptionPane.showMessageDialog(masterFrame, msg,
                                title, JOptionPane.INFORMATION_MESSAGE);
                            gui.getBoard().getToolkit().beep();
                        }
                    };
                    new Thread(messageDialogRunnable).start();
                }
                else
                {
                    // player should now have the concede or negotiate dialog
                    // or even actual battle, don't show any message any more.
                }
            }

            else if (engagingPendingHex != null)
            {
                // server.engage() already sent but tellEngagement() not
                // received yet

                client.logMsgToServer("W", "engagingPendingHex is "
                    + "already set: " + engagingPendingHex);

                final String msg = "Engagement initiated already on hex "
                    + engagingPendingHex + ";\n"
                    + "still waiting for server reply.";
                final String title = "Already engaging!";

                Runnable messageDialogRunnable = new Runnable()
                {
                    public void run()
                    {
                        JOptionPane.showMessageDialog(masterFrame, msg, title,
                            JOptionPane.INFORMATION_MESSAGE);
                        gui.getBoard().getToolkit().beep();
                    }
                };
                new Thread(messageDialogRunnable).start();
            }
            else
            {
                // nothing yet, ok: engage
                if (gui.getClient().ensureThatConnected())
                {
                    gui.waitCursor();
                    engagingPendingHex = hex;
                    client.engage(hex);
                }
            }
        }
        else
        {
            // ignore all other clicks
            LOGGER.warning("You clicked on hex " + hex + " - ignored.");
        }
    }

    class MasterBoardItemHandler implements ItemListener
    {
        public MasterBoardItemHandler()
        {
            super();
            net.sf.colossus.util.InstanceTracker.register(this,
                cachedPlayerName);
        }

        public void itemStateChanged(ItemEvent e)
        {
            JMenuItem source = (JMenuItem)e.getSource();
            String text = source.getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            gui.getOptions().setOption(text, selected);
        }
    }

    class MasterBoardWindowHandler extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            gui.askNewCloseQuitCancel(masterFrame, false);
        }
    }

    void repaintAfterOverlayChanged()
    {
        overlayChanged = true;
        this.getFrame().repaint();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        // Abort if called too early.
        if (g.getClipBounds() == null)
        {
            return;
        }

        if (offScreenBuffer == null
            || overlayChanged
            || (!(offScreenBuffer.getWidth(this) == this.getSize().width && offScreenBuffer
                .getHeight(this) == this.getSize().height)))
        {
            overlayChanged = false;
            offScreenBuffer = this.createImage(this.getWidth(),
                this.getHeight());
            Graphics g_im = offScreenBuffer.getGraphics();
            super.paintComponent(g_im);

            try
            {
                paintHexes(g_im);
            }
            catch (ConcurrentModificationException ex)
            {
                LOGGER.log(Level.FINEST, "harmless " + ex.toString());
                // Don't worry about it -- we'll just paint again.
            }
        }

        g.drawImage(offScreenBuffer, 0, 0, this);
        try
        {
            paintHighlights((Graphics2D)g);
            paintMarkers(g);
            paintRecruitedChits(g);
            paintPossibleRecruitChits(g);
            paintMovementDie(g);
        }
        catch (ConcurrentModificationException ex)
        {
            LOGGER.log(Level.FINEST, "harmless " + ex.toString());
            // Don't worry about it -- we'll just paint again.
        }
    }

    private void paintHexes(final Graphics g)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    hex.paint(g);
                    return false; // keep going
                }
            });
    }

    private void paintHighlights(final Graphics2D g)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    hex.paintHighlightIfNeeded(g);
                    return false; // keep going
                }
            });
    }

    /** Paint markers in z-order. */
    private void paintMarkers(Graphics g)
    {
        synchronized (legionToMarkerMap)
        {
            for (Marker marker : legionToMarkerMap.values())
            {
                if (g.getClipBounds().intersects(marker.getBounds()))
                {
                    marker.paintComponent(g);
                }
            }
        }
    }

    public void paintRecruitedChits(Graphics g)
    {
        for (Chit chit : recruitedChits.values())
        {
            chit.paintComponent(g);
        }
    }

    // all hexes
    public void addPossibleRecruitChits(LegionClientSide legion,
        Set<MasterHex> hexes)
    {
        clearPossibleRecruitChits();

        // set is a set of possible target hexes
        List<CreatureType> oneElemList = new ArrayList<CreatureType>();

        for (MasterHex hex : hexes)
        {
            List<CreatureType> recruits = client.findEligibleRecruits(legion,
                hex);

            if (recruits != null && recruits.size() > 0)
            {
                switch (gui.getRecruitChitMode())
                {
                    case Options.showRecruitChitsNumAll:
                        break;

                    case Options.showRecruitChitsNumRecruitHint:
                        oneElemList.clear();
                        CreatureType hint = client.chooseBestPotentialRecruit(
                            legion, hex, recruits);
                        oneElemList.add(hint);
                        recruits = oneElemList;
                        break;

                    case Options.showRecruitChitsNumStrongest:
                        oneElemList.clear();
                        CreatureType strongest = recruits
                            .get(recruits.size() - 1);
                        oneElemList.add(strongest);
                        recruits = oneElemList;
                        break;
                }
                addPossibleRecruitChits(recruits, hex);
            }
        }
    }

    void addRecruitedChit(Legion legion)
    {
        if (legion.getRecruit() != null)
        {
            MasterHex masterHex = legion.getCurrentHex();
            int scale = 2 * Scale.get();
            GUIMasterHex hex = getGUIHexByMasterHex(masterHex);
            Chit chit = Chit.newCreatureChit(scale, legion.getRecruit());
            recruitedChits.put(legion, chit);
            Point startingPoint = hex.getOffCenter();
            Point point = new Point(startingPoint);
            point.x -= scale / 2;
            point.y -= scale / 2;
            chit.setLocation(point);
        }
        repaint();
    }

    void cleanRecruitedChit(LegionClientSide legion)
    {
        recruitedChits.remove(legion);
        repaint();
    }

    // all possible recruit chits, one hex
    private void addPossibleRecruitChits(List<CreatureType> imageNameList,
        MasterHex masterHex)
    {
        List<Chit> list = new ArrayList<Chit>();
        int size = imageNameList.size();
        int num = size;
        for (CreatureType creatureType : imageNameList)
        {
            int scale = 2 * Scale.get();
            GUIMasterHex hex = getGUIHexByMasterHex(masterHex);
            Chit chit = Chit.newCreatureChit(scale, creatureType);
            Point startingPoint = hex.getOffCenter();
            Point point = new Point(startingPoint);
            point.x -= scale / 2;
            point.y -= scale / 2;
            int offset = (num - ((size / 2) + 1));
            point.x += ((offset * scale) + ((size % 2 == 0 ? (scale / 2) : 0)))
                / size;
            point.y += ((offset * scale) + ((size % 2 == 0 ? (scale / 2) : 0)))
                / size;
            num--;
            chit.setLocation(point);
            list.add(chit);
        }
        possibleRecruitChits.put(masterHex, list);
    }

    public void clearRecruitedChits()
    {
        for (Chit chit : recruitedChits.values())
        {
            remove(chit);
        }
        recruitedChits.clear();
    }

    public void clearPossibleRecruitChits()
    {
        Set<MasterHex> hexes = possibleRecruitChits.keySet();
        possibleRecruitChits.clear();
        for (MasterHex hex : hexes)
        {
            GUIMasterHex guiHex = getGUIHexByMasterHex(hex);
            guiHex.repaint();
        }
    }

    private void paintPossibleRecruitChits(Graphics g)
    {
        // Each returned list is the list of chits for one hex
        for (List<Chit> chits : possibleRecruitChits.values())
        {
            for (Chit chit : chits)
            {
                chit.paintComponent(g);
            }
        }
    }

    private void paintMovementDie(Graphics g)
    {
        if (client != null)
        {
            MovementDie die = gui.getMovementDie();
            if (die != null)
            {
                die.setLocation(0, 0);
                die.paintComponent(g);
            }
            else
            {
                // Paint a black square in the upper-left corner.
                g.setColor(Color.black);
                g.fillRect(0, 0, 4 * Scale.get(), 4 * Scale.get());
            }
        }
    }

    public void markLegionSkip()
    {
        Legion activeLegion = null;

        if (gui.getGame().isPhase(Phase.SPLIT))
        {
            // Not implemented yet
            // ("Mark Legion Skip Split");
        }
        else if (gui.getGame().isPhase(Phase.MOVE))
        {
            if ((activeLegion = gui.getMover()) != null
                && !activeLegion.getSkipThisTime() && !activeLegion.hasMoved())
            {
                activeLegion.setSkipThisTime(true);
                gui.pushUndoStack(activeLegion.getMarkerId());
                updateLegionsLeftToMoveText(true);
                clearPossibleRecruitChits();
                highlightUnmovedLegions();
                repaint();
            }
            else
            {
                //  no active legion, or active legion already marked
            }
        }
        else if (gui.getGame().isPhase(Phase.MUSTER))
        {
            // implemented as part of the PickRecruit dialog
            // System.out.println("Mark Legion Skip Move Muster");

        }
        else
        {
            LOGGER.warning("Mark Legion Skip not meaningful in phase "
                + gui.getGame().getPhase());
        }
    }

    /**
     *  user pressed "N". Find the next legion that "deserves" handling, activate
     *  (as if user had clicked it), and position the mouse cursor over it.
     */
    private void jumpToNextUnhandledLegion()
    {
        Phase phase = client.getPhase();
        if (!(phase == Phase.MOVE || phase == Phase.SPLIT || phase == Phase.MUSTER)
            || !gui.isMyTurn())
        {
            // Not my Split, Move or Recruit phase - nothing to do.
            return;
        }

        boolean allSplitableLegions = false;
        Player player = gui.getClient().getActivePlayer();

        if (phase == Phase.SPLIT)
        {
            Set<String> markersAvailable = player.getMarkersAvailable();
            // Need a legion marker to split.
            if (markersAvailable.size() < 1)
            {
                return;
            }
            // On the first turn, can only split once, the 8 count legion
            // Need flag set to false, or, after splitting, next will then
            // cycle through the two size 4 legions.
            allSplitableLegions = client.getTurnNumber() == 1 ? false : gui
                .getOptions().getOption(Options.nextSplitAllSplitable, true);
        }

        boolean first = true;
        boolean found = false;
        String markerId = null;
        String curMarkerId = client.getCurrentLegionMarkerId();
        Legion nextLegion = null;
        if (curMarkerId != null)
        {
            nextLegion = client.getLegion(curMarkerId);
        }

        for (Legion legion : player.getLegions())
        {
            if (first)
            {
                // Handle case where current legion is not in set
                // or current legion is last entry in set
                if ((phase == Phase.MOVE && !legion.hasMoved())
                    || (phase == Phase.SPLIT && !allSplitableLegions && (legion
                        .getHeight() >= 7))
                    || (phase == Phase.SPLIT && allSplitableLegions && (legion
                        .getHeight() >= 4))
                    || (phase == Phase.MUSTER && client.canRecruit(legion)))
                {
                    nextLegion = legion;
                    first = false;
                }
            }
            if (found && !legion.getSkipThisTime())
            {
                if ((phase == Phase.MOVE && !legion.hasMoved())
                    || (phase == Phase.SPLIT && !allSplitableLegions && (legion
                        .getHeight() == 7))
                    || (phase == Phase.SPLIT && allSplitableLegions && (legion
                        .getHeight() >= 4))
                    || (phase == Phase.MUSTER && client.canRecruit(legion)))
                {
                    nextLegion = legion;
                    break;
                }
            }
            markerId = legion.getMarkerId();
            if (markerId.equals(curMarkerId))
            {
                found = true;
            }
        }

        // Eclipse thinks nextLegion might be null ...
        // to be on safe side, do nothing in that case.
        if (!first && nextLegion != null)
        {
            activateNextLegionAndPlaceMouse(nextLegion);
        }
    }

    /**
     * @param nextLegion
     */
    private void activateNextLegionAndPlaceMouse(Legion nextLegion)
    {
        assert nextLegion != null : "nextLegion must not be null when "
            + "calling this!";
        LegionClientSide newCurLegion = client.getLegion(nextLegion
            .getMarkerId());
        MasterHex newHex = newCurLegion.getCurrentHex();
        GUIMasterHex newGHex = getGUIHexByMasterHex(newHex);
        Point point = newGHex.findCenter();
        try
        {
            Robot robot = new Robot();
            Rectangle rect = new Rectangle(point.x - 250, point.y - 250, 500,
                500);
            scrollRectToVisible(rect);
            SwingUtilities.convertPointToScreen(point, MasterBoard.this);
            robot.mouseMove(point.x, point.y);

            client.setCurrentLegionMarkerId(nextLegion.getMarkerId());

            Phase phase = client.getPhase();
            if ((phase == Phase.MUSTER && gui.getOptions().getOption(
                Options.nextMuster, true))
                || (phase == Phase.MOVE && gui.getOptions().getOption(
                    Options.nextMove, true))
                || (phase == Phase.SPLIT && (gui.getNextSplitClickMode() == Options.nextSplitNumLeftClick)))
            {
                actOnLegion(newCurLegion, nextLegion.getCurrentHex());
                GUIMasterHex hex = getGUIHexByMasterHex(nextLegion
                    .getCurrentHex());
                hex.select();
                hex.setSelectColor(Color.black);
                hex.repaint();
            }
            else if (phase == Phase.SPLIT
                && (gui.getNextSplitClickMode() == Options.nextSplitNumRightClick))
            {
                int viewMode = gui.getEffectiveViewMode();
                LegionClientSide clientSideLegion = client
                    .getLegion(nextLegion.getMarkerId());

                new ShowLegion(masterFrame, clientSideLegion, point,
                    scrollPane, 4 * Scale.get(), viewMode,
                    client.isMyLegion(clientSideLegion), false, true);
            }
        }
        catch (AWTException exception)
        {
            LOGGER.log(Level.WARNING, "Robot creation failed");
        }
    }

    private void destroyThisLegion()
    {
        if (gui.getGame().isPhase(Phase.MOVE))
        {
            Legion activeLegion = gui.getMover();
            if (activeLegion != null)
            {
                if (activeLegion.hasTitan())
                {
                    LOGGER.warning("Ignored attempt to destroy Titan Legion!");
                }
                else
                {
                    gui.getClient().destroyLegion(activeLegion);
                }
            }
        }
        else
        {
            LOGGER.warning("destroyThisLegion called, but not move phase.");
        }
    }

    @Override
    public Dimension getMinimumSize()
    {
        int scale = Scale.get();
        return new Dimension(((getMasterBoard().getHorizSize() + 1) * 4)
            * scale, (getMasterBoard().getVertSize() * 7) * scale);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    void rescale()
    {
        setupGUIHexes();
        recreateMarkers();
        setSize(getPreferredSize());
        masterFrame.pack();
        repaint();
    }

    void deiconify()
    {
        if (masterFrame.getState() == JFrame.ICONIFIED)
        {
            masterFrame.setState(JFrame.NORMAL);
        }
    }

    public void dispose()
    {
        setVisible(false);
        setEnabled(false);
        saveWindow.saveLocation(masterFrame.getLocation());
        saveWindow = null;
        cleanCBListeners();
        masterFrame.setVisible(false);
        masterFrame.setEnabled(false);
        masterFrame.removeWindowListener(mbwh);
        masterFrame.dispose();
        masterFrame = null;
        scrollPane = null;

        removeKeyListener(this.iph);
        if (showReadme != null)
        {
            showReadme.dispose();
            showReadme = null;
        }
        if (showHelpDoc != null)
        {
            showHelpDoc.dispose();
            showHelpDoc = null;
        }
        offScreenBuffer = null;
        iph = null;
        cleanGUIHexes();

        // not those, they are static (common for all objects)
        // first client disposing would set it null, others get NPE's ...
        //plainHexArray = null;
        //towerSet = null;

        this.client = null;
    }

    void pack()
    {
        masterFrame.pack();
    }

    void updateComponentTreeUI()
    {
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.updateComponentTreeUI(masterFrame);
    }

    void fullRepaint()
    {
        masterFrame.repaint();
        repaint();
    }

    /**
     * If and only if stealFocus option is enabled, this does both
     * requestFocus and getFrame().toFront().
     */
    void maybeRequestFocusAndToFront()
    {
        if (gui.getOptions().getOption(Options.stealFocus))
        {
            requestFocus();
            getFrame().toFront();
        }
    }

    void myTurnBottomBarActions(boolean isDue)
    {
        bottomBar.myTurnActions(isDue);
    }

    public void updateLegionsLeftToMusterText()
    {
        int legionCount = client.getPossibleRecruitHexes().size();
        bottomBar.setLegionsLeftToMuster(legionCount);
    }

    // When enter Move phase, do not yet have roll, therefore can not
    // determine which units are blocked.  Attempting to do so yields
    // bad results, and a null pointer exception on the first turn, so
    // send flag to prevent this.
    public void updateLegionsLeftToMoveText(boolean have_roll)
    {
        int legionStatus[] = { 0, 0, 0, 0 };
        client.legionsNotMoved(legionStatus, have_roll);
        bottomBar.setLegionsStatus(legionStatus);
    }

    class BottomBar extends JPanel
    {
        private final JLabel playerLabel;

        private final Color originalBackgroundColor;

        /** quick access button to the doneWithPhase action.
         *  must be en- and disabled often.
         */
        private final JButton doneButton;

        private final JButton pauseButton;

        /** display the current phase in the bottom bar */
        private final JLabel phaseLabel;

        /**
         * Displays reasons why "Done" can not be used.
         */
        private final JLabel todoLabel;

        /*
         * Displays count of actions still to do. e.g. legions to muster
         */
        private final JLabel countLabel;

        public void setPlayerName(String s)
        {
            playerLabel.setText(s);
        }

        public void setPlayerColor(Color color)
        {
            playerLabel.setForeground(color);
        }

        public void myTurnActions(boolean isDue)
        {
            this.setBackground(isDue ? Color.yellow : originalBackgroundColor);
        }

        public void setPhase(String s)
        {
            phaseLabel.setText(s);
        }

        public void setPendingText(String text)
        {
            phaseLabel.setText(text);
        }

        public void setReasonForDisabledDone(String reason)
        {
            todoLabel.setText("(" + reason + ")");
        }

        public void setLegionsLeftToMuster(int legionCount)
        {
            if (legionCount == 0)
                countLabel.setText("");
            else if (legionCount == 1)
                countLabel.setText(legionCount + " legion to muster");
            else
                countLabel.setText(legionCount + " legions to muster");
        }

        public void setLegionsStatus(int legionStatus[])
        {
            int unmoved = legionStatus[Constants.legionStatusCount]
                - legionStatus[Constants.legionStatusMoved];
            if (unmoved > 0)
            {
                String labelText = legionStatus[Constants.legionStatusCount]
                    + " legions:  " + unmoved
                    + (unmoved == 1 ? " has not moved" : " have not moved");
                if (legionStatus[Constants.legionStatusBlocked] > 0)
                {
                    if (unmoved == 1)
                    {
                        labelText = labelText + ", it is blocked";
                    }
                    else
                    {
                        labelText = labelText
                            + ", "
                            + legionStatus[Constants.legionStatusBlocked]
                            + (legionStatus[Constants.legionStatusBlocked] == 1 ? " of those is blocked"
                                : " of those are blocked");
                    }
                }
                if (legionStatus[Constants.legionStatusNotVisitedSkippedBlocked] > 0)
                {
                    if (unmoved == 1)
                    {
                        labelText = labelText
                            + ", it has not been visited or skipped";
                    }
                    else
                    {
                        labelText = labelText
                            + ", "
                            + legionStatus[Constants.legionStatusNotVisitedSkippedBlocked]
                            + (legionStatus[Constants.legionStatusNotVisitedSkippedBlocked] == 1 ? " of those has not been visited or skipped"
                                : " of those have not been visited or skipped");
                    }
                }
                labelText = labelText + ".";
                countLabel.setText(labelText);
            }
            else
            {
                countLabel.setText(legionStatus[Constants.legionStatusCount]
                    + " legions, all have moved");
            }
        }

        public void setLegionsLeftToMove(int legionCount)
        {
            if (legionCount == 0)
                countLabel.setText("");
            else if (legionCount == 1)
                countLabel.setText(legionCount + " legion to move");
            else
                countLabel.setText(legionCount + " legions to move");
        }

        // Add Marker Count Text to the bottom display bar
        public void setMarkerCount(int markerCount)
        {
            countLabel.setText("(" + markerCount + " marker"
                + ((markerCount == 1) ? "" : "s") + ")");
        }

        public BottomBar()
        {
            super();

            setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

            String text = gui.getClient().isPaused() ? "Continue" : "Pause";
            pauseButton = new JButton(text);
            pauseButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MasterBoard.this.bottomBar.toggleSuspend();
                    focusBackToMasterboard();
                }
            });

            // Not useful in any remote client
            if (!client.isRemote())
            {
                add(pauseButton);
            }

            originalBackgroundColor = this.getBackground();
            playerLabel = new JLabel("- player -");
            add(playerLabel);

            doneButton = new JButton(doneWithPhaseAction);
            doneWithPhaseAction
                .addPropertyChangeListener(new PropertyChangeListener()
                {
                    public void propertyChange(PropertyChangeEvent evt)
                    {
                        if (evt.getPropertyName().equals("enabled")
                            && evt.getNewValue().equals(Boolean.TRUE))
                        {
                            todoLabel.setText("");
                        }
                    }
                });
            add(doneButton);

            phaseLabel = new JLabel("- phase -");
            add(phaseLabel);

            todoLabel = new JLabel();
            add(todoLabel);

            countLabel = new JLabel();
            add(countLabel);
        }

        public void toggleSuspend()
        {
            boolean oldState = gui.getClient().isPaused();
            boolean newState = !oldState;
            gui.getClient().setPauseState(newState);
            String text = gui.getClient().isPaused() ? "Continue" : "Pause";
            pauseButton.setText(text);
        }

        /**
         *  Called when game is over; enable done button again and make it call
         *  "close", which brings then the "New/Close/Quit/Cancel" dialog,
         *  so that one does not have to pick it from menu or right upper corner.
         */
        private void makeDoneCloseWindow()
        {
            gameOverStateReached = true;
            enableDoneAction();
            doneButton.setText("Close");
        }
    }

    public void enableDoneAction()
    {
        doneWithPhaseAction.setEnabled(true);
    }

    /**
     * Disable the Done action, and update the reason text in bottomBar
     *
     * @param reason Information why one is not ready to be Done
     */
    public void disableDoneAction(String reason)
    {
        doneWithPhaseAction.setEnabled(false);
        bottomBar.setReasonForDisabledDone(reason);
    }

    /**
     * Clear bottomBar phase text and call disableDoneAction, as reason the
     * standard text "&lt;active player> doesWhat"
     *
     * @param doesWhat Information what the active player currently does
     */
    private void disableDoneActionActivePlayerDoes(String doesWhat)
    {
        bottomBar.setPhase("");
        // String name = gui.getClient().getActivePlayer().getName();
        disableDoneAction(doesWhat);
    }

    public void setServerClosedMessage(boolean gameOver)
    {
        if (gameOver)
        {
            bottomBar.setPhase("Game over");
            disableDoneAction("game server closed connection");
        }
        else
        {
            bottomBar.setPhase("Unable to continue game");
            disableDoneAction("connection to server lost");
        }
    }

    public void setReconnectedMessage()
    {
        bottomBar.setPhase("-suspended-");
        disableDoneAction("Connection restored - resyc needed....");
    }

    public void setReplayMode()
    {
        disableDoneAction("please wait...");
    }

    public void updateReplayText(int currTurn, int maxTurn)
    {
        bottomBar.setPhase("Replay ongoing, processing turn " + currTurn
            + " of " + maxTurn);
    }

    /**
     * Updates the window's title. For active player, tell what to do;
     * for inactive player, inform what active player is doing.
     * OwningPlayer: Split stacks
     * OwningPlayer: OtherPlayer musters
     *
     * @param text a string like 'Split stacks' or "player xxx moves"
     */
    private void setTitleInfoText(String text)
    {
        masterFrame.setTitle(client.getOwningPlayer().getName() + ": Turn "
            + client.getTurnNumber() + " - " + text);
    }

    public void setTempDisconnectedState(String message)
    {
        setTitleInfoText("Problem -- " + message);
        bottomBar.setPhase("Problem: " + message);
        disableDoneAction("connection with server interrupted");
    }

    public void setGameOverState(String message)
    {
        setTitleInfoText(message);
        bottomBar.setPhase(message);
        disableDoneAction("connection closed from server side");
        bottomBar.makeDoneCloseWindow();
    }

    public void setPhaseInfo(String message)
    {
        bottomBar.setPhase(message);
    }
}
