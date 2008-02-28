package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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

import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.server.Constants;
import net.sf.colossus.server.XMLSnapshotFilter;
import net.sf.colossus.util.ArrayHelper;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.NullCheckPredicate;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.xmlparser.TerrainRecruitLoader;


/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
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

    private KFrame masterFrame;
    private ShowReadme showReadme;
    private ShowHelpDoc showHelpDoc;
    private JMenu phaseMenu;
    private JPopupMenu popupMenu;
    private Map<String, JCheckBoxMenuItem> checkboxes = new HashMap<String, JCheckBoxMenuItem>();
    private JPanel[] legionFlyouts;

    private final MasterBoardWindowHandler mbwh;
    private InfoPopupHandler iph;

    /** Last point clicked is needed for popup menus. */
    private Point lastPoint;

    /** List of markers which are currently on the board, 
     *  for painting in z-order => the end of the list is on top.
     *  
     *  Now synchronized access to prevent NPEs when EDT wants to
     *  paint a marker and asks for the legion for it, and
     *  legion has just been removed.
     *  I don't use a synchronizedList, because then I get into 
     *  trouble in the recreateMarkers method.
     *  @TODO: Perhaps the whole list should be legions instead
     *         of markers.
     */
    private final List<Marker> markersOnBoard = new ArrayList<Marker>();

    /** The scrollbarspanel, needed to correct lastPoint. */
    private JScrollPane scrollPane;

    private final Container contentPane;

    /** our own little bar implementation */
    private BottomBar bottomBar;

    public static final String saveGameAs = "Save game as";

    public static final String clearRecruitChits = "Clear recruit chits";

    public static final String undoLast = "Undo";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";
    public static final String forcedDoneWithPhase = "Forced Done";

    public static final String takeMulligan = "Take Mulligan";
    public static final String concedeBattle = "Concede battle";
    public static final String withdrawFromGame = "Withdraw from Game";

    public static final String viewWebClient = "View Web Client";
    public static final String viewFullRecruitTree = "View Full Recruit Tree";
    public static final String viewHexRecruitTree = "View Hex Recruit Tree";
    public static final String viewBattleMap = "View Battle Map";

    public static final String chooseScreen = "Choose Screen For Info Windows";
    public static final String preferences = "Preferences...";

    public static final String about = "About";
    public static final String viewReadme = "Show Variant Readme";
    public static final String viewHelpDoc = "Options Documentation";

    private AbstractAction newGameAction;
    private AbstractAction loadGameAction;
    private AbstractAction saveGameAction;
    private AbstractAction saveGameAsAction;
    private AbstractAction closeBoardAction;
    private AbstractAction quitGameAction;

    private AbstractAction clearRecruitChitsAction;

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;
    private AbstractAction forcedDoneWithPhaseAction;

    private AbstractAction takeMulliganAction;
    private AbstractAction withdrawFromGameAction;

    private AbstractAction viewWebClientAction;
    private AbstractAction viewFullRecruitTreeAction;
    private AbstractAction viewHexRecruitTreeAction;
    private AbstractAction viewBattleMapAction;

    private AbstractAction chooseScreenAction;

    private AbstractAction preferencesAction;

    private AbstractAction aboutAction;
    private AbstractAction viewReadmeAction;
    private AbstractAction viewHelpDocAction;

    private boolean playerLabelDone;

    private SaveWindow saveWindow;

    private String cachedPlayerName = "<not set yet>";

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
            net.sf.colossus.webcommon.InstanceTracker.register(this, client
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
                    synchronized (markersOnBoard)
                    {
                        createLegionFlyouts(markersOnBoard);
                    }
                }
            }
            else if (e.getKeyCode() == POPUP_KEY_MY_LEGIONS)
            {
                if (legionFlyouts == null)
                {
                    // copy only local players markers
                    List<Marker> myMarkers = new ArrayList<Marker>();
                    synchronized (markersOnBoard)
                    {
                        for (Marker marker : markersOnBoard)
                        {
                            Legion legion = client.getLegion(marker.getId());
                            if (((LegionClientSide)legion).isMyLegion())
                            {
                                myMarkers.add(marker);
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

        private void createLegionFlyouts(List<Marker> markers)
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

                boolean dubiousAsBlanks = client.getOptions().getOption(
                    Options.dubiousAsBlanks);
                final JPanel panel = new LegionInfoPanel(legion, scale,
                    PANEL_MARGIN, PANEL_PADDING, true, client.getViewMode(),
                    dubiousAsBlanks, true);
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

    MasterBoard(final Client client)
    {
        this.client = client;
        net.sf.colossus.webcommon.InstanceTracker.register(this, client
            .getOwningPlayer().getName());

        String pname = client.getOwningPlayer().getName();
        if (pname == null)
        {
            pname = "unknown";
        }
        masterFrame = new KFrame("MasterBoard " + pname);
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
        masterFrame.addKeyListener(this.iph);

        setupGUIHexes();
        setupActions();
        setupPopupMenu();
        setupTopMenu();

        scrollPane = new JScrollPane(this);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setupPlayerLabel();

        saveWindow = new SaveWindow(client.getOptions(), "MasterBoardScreen");
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
    }

    // For HotSeatMode
    public void setBoardActive(boolean val)
    {
        if (val)
        {
            masterFrame.setExtendedState(JFrame.NORMAL);
            masterFrame.repaint();
            reqFocus();
        }
        else
        {
            masterFrame.setExtendedState(JFrame.ICONIFIED);
        }
    }

    private void setupActions()
    {
        clearRecruitChitsAction = new AbstractAction(clearRecruitChits)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.clearRecruitChits();
            }
        };

        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                Constants.Phase phase = client.getPhase();
                if (phase == Constants.Phase.SPLIT)
                {
                    client.undoLastSplit();
                    alignAllLegions();
                    highlightTallLegions();
                    repaint();
                }
                else if (phase == Constants.Phase.MOVE)
                {
                    client.undoLastMove();
                    highlightUnmovedLegions();
                }
                else if (phase == Constants.Phase.FIGHT)
                {
                    LOGGER.log(Level.SEVERE, "called undoLastAction in FIGHT");
                }
                else if (phase == Constants.Phase.MUSTER)
                {
                    client.undoLastRecruit();
                    highlightPossibleRecruits();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase");
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                Constants.Phase phase = client.getPhase();
                if (phase == Constants.Phase.SPLIT)
                {
                    client.undoAllSplits();
                    alignAllLegions();
                    highlightTallLegions();
                    repaint();
                }
                else if (phase == Constants.Phase.MOVE)
                {
                    client.undoAllMoves();
                    highlightUnmovedLegions();
                }
                else if (phase == Constants.Phase.FIGHT)
                {
                    LOGGER.log(Level.SEVERE, "called undoAllAction in FIGHT");
                }
                else if (phase == Constants.Phase.MUSTER)
                {
                    client.undoAllRecruits();
                    highlightPossibleRecruits();
                }
                else
                {
                    LOGGER.log(Level.SEVERE, "Bogus phase");
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                // first set disabled...
                doneWithPhaseAction.setEnabled(false);
                // because response from server might set it to enabled again
                client.doneWithPhase();
            }
        };
        // will be enabled if it is player's turn
        doneWithPhaseAction.setEnabled(false);

        forcedDoneWithPhaseAction = new AbstractAction(forcedDoneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.doneWithPhase();
            }
        };
        // make this always be available
        forcedDoneWithPhaseAction.setEnabled(true);

        takeMulliganAction = new AbstractAction(takeMulligan)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.mulligan();
            }
        };

        withdrawFromGameAction = new AbstractAction(withdrawFromGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you with to withdraw from the game?",
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
                new ShowAllRecruits(masterFrame, client.getOptions(),
                    TerrainRecruitLoader.getTerrains());
            }
        };

        viewWebClientAction = new AbstractAction(viewWebClient)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.showWebClient();
            }
        };

        viewHexRecruitTreeAction = new AbstractAction(viewHexRecruitTree)
        {
            public void actionPerformed(ActionEvent e)
            {
                GUIMasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    MasterHex hexModel = hex.getHexModel();
                    new ShowRecruits(masterFrame, lastPoint, hexModel,
                        scrollPane);
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
                    new ShowBattleMap(masterFrame, client, hex);
                }
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
                if (client.isGameOver())
                {
                    quitAll = true;
                }
                else
                {
                    String[] options = new String[2];
                    options[0] = "Yes";
                    options[1] = "No";
                    int answer = JOptionPane.showOptionDialog(masterFrame,
                        "Are you sure you wish to quit?", "Quit Game?",
                        JOptionPane.YES_NO_OPTION,
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
                    client.menuQuitGame();
                }
            }
        };

        newGameAction = new AbstractAction(Constants.newGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!client.isGameOver())
                {
                    String[] options = new String[2];
                    options[0] = "Yes";
                    options[1] = "No";
                    int answer = JOptionPane.showOptionDialog(masterFrame,
                        "Are you sure you want to quit this game and "
                            + "start a new one?", "New Game?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options,
                        options[1]);

                    if (answer != JOptionPane.YES_OPTION)
                    {
                        return;
                    }
                }
                client.menuNewGame();
            }
        };

        loadGameAction = new AbstractAction(Constants.loadGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                // No need for confirmation because the user can cancel
                // from the load game dialog.
                JFileChooser chooser = new JFileChooser(Constants.saveDirname);
                chooser.setFileFilter(new XMLSnapshotFilter());
                int returnVal = chooser.showOpenDialog(masterFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    client.menuLoadGame(chooser.getSelectedFile().getPath());
                }
            }
        };

        saveGameAction = new AbstractAction(Constants.saveGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.menuSaveGame(null);
            }
        };

        saveGameAsAction = new AbstractAction(saveGameAs)
        {
            // TODO: Need a confirmation dialog on overwrite?
            public void actionPerformed(ActionEvent e)
            {
                File savesDir = new File(Constants.saveDirname);
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
                    if (!basename.endsWith(Constants.xmlExtension))
                    {
                        basename += Constants.xmlExtension;
                    }
                    client.menuSaveGame(dirname + '/' + basename);
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
                if (client.isGameOver() || !client.isAlive())
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
                    client.menuCloseBoard();
                }
            }
        };

        chooseScreenAction = new AbstractAction(chooseScreen)
        {
            public void actionPerformed(ActionEvent e)
            {
                new ChooseScreen(getFrame(), client);
            }
        };

        preferencesAction = new AbstractAction(preferences)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.setPreferencesWindowVisible(true);
            }
        };

        aboutAction = new AbstractAction(about)
        {
            public void actionPerformed(ActionEvent e)
            {
                JOptionPane.showMessageDialog(masterFrame, "Colossus build: "
                    + Client.getVersion() + "\n" + "user.home:      "
                    + System.getProperty("user.home") + "\n"
                    + "java.version:   " + System.getProperty("java.version"));
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
                showReadme = new ShowReadme(client.getOptions());
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
    }

    public void doQuitGameAction()
    {
        quitGameAction.actionPerformed(null);
    }

    private void setupPopupMenu()
    {
        popupMenu = new JPopupMenu();
        contentPane.add(popupMenu);

        JMenuItem mi = popupMenu.add(viewHexRecruitTreeAction);
        mi.setMnemonic(KeyEvent.VK_R);

        mi = popupMenu.add(viewBattleMapAction);
        mi.setMnemonic(KeyEvent.VK_B);
    }

    private ItemListener itemHandler = new MasterBoardItemHandler();

    private JCheckBoxMenuItem addCheckBox(JMenu menu, String name, int mnemonic)
    {
        JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(name);
        cbmi.setMnemonic(mnemonic);
        cbmi.setSelected(client.getOptions().getOption(name));

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
            mi = fileMenu.add(loadGameAction);
            mi.setMnemonic(KeyEvent.VK_L);
            mi = fileMenu.add(saveGameAction);
            mi.setMnemonic(KeyEvent.VK_S);
            mi = fileMenu.add(saveGameAsAction);
            mi.setMnemonic(KeyEvent.VK_A);
        }
        mi = fileMenu.add(closeBoardAction);
        mi.setMnemonic(KeyEvent.VK_C);
        mi = fileMenu.add(quitGameAction);
        mi.setMnemonic(KeyEvent.VK_Q);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);

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
    }

    void twiddleOption(String name, boolean enable)
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
        String playerName = client.getOwningPlayer().getName();
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

        String colorName = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
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
                            .getBoardParity()) & 1)
                            * 2 * ((j + 1) / 2)) * GUIHex.SQRT3 * scale),
                        scale, getMasterBoard().isHexInverted(i, j), this);
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
        return client.getGame().getVariant().getMasterBoard();
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

    void setupSplitMenu()
    {
        unselectAllHexes();
        reqFocus();

        Player activePlayer = client.getActivePlayer();

        masterFrame.setTitle(activePlayer.getName() + " Turn "
            + client.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

        if (client.getOwningPlayer().equals(activePlayer))
        {
            bottomBar.setPhase("Split stacks");
            enableDoneAction();

            JMenuItem mi;

            mi = phaseMenu.add(clearRecruitChitsAction);
            mi.setMnemonic(KeyEvent.VK_C);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

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
            // mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightTallLegions();
        }
        else
        {
            bottomBar.setPhase("(" + activePlayer.getName() + " splits)");
        }
    }

    void setupMoveMenu()
    {
        unselectAllHexes();
        reqFocus();

        Player activePlayer = client.getActivePlayer();
        String activePlayerName = activePlayer.getName();
        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Movement Roll: "
            + client.getMovementRoll());

        phaseMenu.removeAll();

        if (client.getOwningPlayer().equals(activePlayer))
        {
            bottomBar.setPhase("Movement");

            JMenuItem mi;

            mi = phaseMenu.add(clearRecruitChitsAction);
            mi.setMnemonic(KeyEvent.VK_C);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

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
            // mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            if (client.getMulligansLeft() > 0)
            {
                phaseMenu.addSeparator();
                mi = phaseMenu.add(takeMulliganAction);
                mi.setMnemonic(KeyEvent.VK_M);
                mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
            }

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightUnmovedLegions();

            disableDoneAction("At least one legion must move");
        }
        else
        {
            bottomBar.setPhase("(" + activePlayerName + " moves)");
        }

        // Force showing the updated movement die.
        repaint();
    }

    void setupFightMenu()
    {
        unselectAllHexes();
        reqFocus();

        Player activePlayer = client.getActivePlayer();
        String activePlayerName = activePlayer.getName();

        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Resolve Engagements ");

        phaseMenu.removeAll();

        if (client.getOwningPlayer().equals(activePlayer))
        {
            bottomBar.setPhase("Resolve Engagements");

            JMenuItem mi;

            mi = phaseMenu.add(clearRecruitChitsAction);
            mi.setMnemonic(KeyEvent.VK_C);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(doneWithPhaseAction);
            mi.setMnemonic(KeyEvent.VK_D);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            mi = phaseMenu.add(forcedDoneWithPhaseAction);
            mi.setMnemonic(KeyEvent.VK_F);
            // mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightEngagements();
        }
        else
        {
            bottomBar.setPhase("(" + activePlayerName + " fights)");
        }
    }

    void setupMusterMenu()
    {
        unselectAllHexes();
        reqFocus();

        Player activePlayer = client.getActivePlayer();
        String activePlayerName = activePlayer.getName();

        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

        if (client.getOwningPlayer().equals(activePlayer))
        {
            bottomBar.setPhase("Muster Recruits");
            enableDoneAction();

            JMenuItem mi;

            mi = phaseMenu.add(clearRecruitChitsAction);
            mi.setMnemonic(KeyEvent.VK_C);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

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
            // mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightPossibleRecruits();
        }
        else
        {
            bottomBar.setPhase("(" + activePlayerName + " musters)");
        }
    }

    void highlightPossibleRecruits()
    {
        unselectAllHexes();
        selectHexesByLabels(client.getPossibleRecruitHexes());
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
        List<LegionClientSide> legions = client.getLegionsByHex(masterHex);

        int numLegions = legions.size();
        if (numLegions == 0)
        {
            hex.repaint();
            return;
        }

        LegionClientSide legion = legions.get(0);
        Marker marker = legion.getMarker();
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
            legion = legions.get(1);
            marker = (legion).getMarker();
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
            legion = legions.get(1);
            marker = (legion).getMarker();
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale4;
            point.y -= chitScale;
            legion = legions.get(2);
            marker = (legion).getMarker();
            marker.setLocation(point);
        }

        hex.repaint();
    }

    void alignLegions(Set<MasterHex> hexes)
    {
        for (MasterHex masterHex : hexes)
        {
            alignLegions(masterHex);
        }
    }

    void highlightTallLegions()
    {
        unselectAllHexes();
        selectHexesByLabels(client.findTallLegionHexes());
    }

    void highlightUnmovedLegions()
    {
        unselectAllHexes();
        selectHexesByLabels(client.findUnmovedLegionHexes());
        repaint();
    }

    /** Select hexes where this legion can move. */
    private void highlightMoves(LegionClientSide legion)
    {
        unselectAllHexes();

        Set<MasterHex> teleport = client.listTeleportMoves(legion);
        selectHexesByLabels(teleport, HTMLColor.purple);

        Set<MasterHex> normal = client.listNormalMoves(legion);
        selectHexesByLabels(normal, Color.white);

        Set<MasterHex> combo = new HashSet<MasterHex>();
        combo.addAll(teleport);
        combo.addAll(normal);

        client.addPossibleRecruitChits(legion, combo);
    }

    void highlightEngagements()
    {
        Set<MasterHex> set = client.findEngagements();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Return number of legions with summonable angels. */
    int highlightSummonableAngels(Legion legion)
    {
        Set<MasterHex> set = client.findSummonableAngelHexes(legion);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }

    private void setupIcon()
    {
        List<String> directories = new ArrayList<String>();
        directories.add(Constants.defaultDirName
            + ResourceLoader.getPathSeparator() + Constants.imagesDirName);

        String[] iconNames = {
            Constants.masterboardIconImage,
            Constants.masterboardIconText + "-Name-"
                + Constants.masterboardIconTextColor,
            Constants.masterboardIconSubscript + "-Subscript-"
                + Constants.masterboardIconTextColor };

        Image image = ResourceLoader.getCompositeImage(iconNames, directories,
            60, 60);

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

    List<Marker> getMarkers()
    {
        // Note: whoever uses this, should access it in synchronized way!
        return markersOnBoard;
    }

    void markerToTop(Marker marker)
    {
        synchronized (markersOnBoard)
        {
            markersOnBoard.remove(marker);
            markersOnBoard.add(marker);
        }
    }

    void removeMarkerForLegion(Legion legion)
    {
        Marker marker = ((LegionClientSide)legion).getMarker();
        synchronized (markersOnBoard)
        {
            markersOnBoard.remove(marker);
        }
    }

    /** Create new markers in response to a rescale. */
    void recreateMarkers()
    {
        Set<MasterHex> hexesNeedAligning = new HashSet<MasterHex>();
        synchronized (markersOnBoard)
        {
            markersOnBoard.clear();
            for (Player player : client.getPlayers())
            {
                for (Legion legion : player.getLegions())
                {
                    String markerId = legion.getMarkerId();
                    Marker marker = new Marker(3 * Scale.get(), markerId,
                        client);
                    ((LegionClientSide)legion).setMarker(marker);
                    markersOnBoard.add(marker);
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
        synchronized (markersOnBoard)
        {
            ListIterator<Marker> lit = markersOnBoard
                .listIterator(markersOnBoard.size());
            while (lit.hasPrevious())
            {
                Marker marker = lit.previous();
                if (marker != null && marker.contains(point))
                {
                    return marker;
                }
            }
        }
        return null;
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

    void unselectHexByLabel(final String label)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    if (hex.isSelected()
                        && label.equals(hex.getHexModel().getLabel()))
                    {
                        hex.unselect();
                        hex.repaint();
                        return true;
                    }
                    return false; // keep going
                }
            });
    }

    void unselectHexesByLabels(final Set<String> labels)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)

                {
                    if (hex.isSelected()
                        && labels.contains(hex.getHexModel().getLabel()))
                    {
                        hex.unselect();
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
    }

    // TODO make typesafe
    void selectHexByLabel(final String label)
    {
        ArrayHelper.findFirstMatch(guiHexArray,
            new NullCheckPredicate<GUIMasterHex>(false)
            {
                @Override
                public boolean matchesNonNullValue(GUIMasterHex hex)
                {
                    if (!hex.isSelected()
                        && label.equals(hex.getHexModel().getLabel()))
                    {
                        hex.select();
                        hex.repaint();
                    }
                    return false; // keep going
                }
            });
    }

    // TODO rename since we don't use labels anymore
    void selectHexesByLabels(final Set<MasterHex> hexes)
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

    // TODO rename since we don't use labels anymore
    void selectHexesByLabels(final Set<MasterHex> hexes, final Color color)
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
    }

    void actOnMisclick()
    {
        Constants.Phase phase = client.getPhase();
        if (phase == Constants.Phase.SPLIT)
        {
            highlightTallLegions();
        }
        else if (phase == Constants.Phase.MOVE)
        {
            client.clearRecruitChits();
            client.setMover(null);
            highlightUnmovedLegions();
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            SummonAngel summonAngel = client.getSummonAngel();
            if (summonAngel != null)
            {
                highlightSummonableAngels(summonAngel.getLegion());
                summonAngel.repaint();
            }
            else
            {
                highlightEngagements();
            }
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            highlightPossibleRecruits();
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
            if (marker != null)
            {
                String markerId = marker.getId();

                // Move the clicked-on marker to the top of the z-order.
                LegionClientSide legion = client.getLegion(markerId);
                client.setMarker(legion, marker);

                // Right-click means to show the contents of the legion.
                if (isPopupButton(e))
                {
                    int viewMode = client.getViewMode();
                    boolean dubiousAsBlanks = client.getOptions().getOption(
                        Options.dubiousAsBlanks);
                    new ShowLegion(masterFrame, legion, point, scrollPane,
                        4 * Scale.get(), viewMode, dubiousAsBlanks);
                    return;
                }
                else if (legion.isMyLegion())
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
                    popupMenu.setLabel(hex.getHexModel().getDescription());
                    popupMenu.show(e.getComponent(), point.x, point.y);
                    return;
                }

                // Otherwise, the action to take depends on the phase.
                // Only the current player can manipulate game state.
                if (client.getOwningPlayer().equals(client.getActivePlayer()))
                {
                    actOnHex(hex.getHexModel());
                    hex.repaint();
                    return;
                }
            }

            // No hits on chits or map, so re-highlight.
            if (client.getOwningPlayer().equals(client.getActivePlayer()))
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
                client.showMarker(marker);
            }
            else
            {
                GUIMasterHex hex = getHexContainingPoint(point);
                if (hex != null)
                {
                    client.showHexRecruitTree(hex);
                }
            }
        }
    }

    private void actOnLegion(LegionClientSide legion, MasterHex hex)
    {
        if (!client.isMyTurn())
        {
            return;
        }

        Constants.Phase phase = client.getPhase();
        if (phase == Constants.Phase.SPLIT)
        {
            client.doSplit(legion);
        }
        else if (phase == Constants.Phase.MOVE)
        {
            // Allow spin cycle by clicking on chit again.
            if (legion.equals(client.getMover()))
            {
                actOnHex(hex);
            }
            else
            {
                client.setMover(legion);
                getGUIHexByMasterHex(hex).repaint();
                highlightMoves(legion);
            }
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            client.doFight(hex);
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            client.doRecruit(legion);
        }
    }

    private void actOnHex(MasterHex hex)
    {
        Constants.Phase phase = client.getPhase();
        if (phase == Constants.Phase.SPLIT)
        {
            highlightTallLegions();
        }
        else if (phase == Constants.Phase.MOVE)
        {
            // If we're moving, and have selected a legion which
            // has not yet moved, and this hex is a legal
            // destination, move the legion here.
            client.clearRecruitChits();
            client.doMove(hex);
            actOnMisclick(); // Yes, even if the move was good.
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            // If we're fighting and there is an engagement here,
            // resolve it.  If an angel is being summoned, mark
            // the donor legion instead.
            client.engage(hex);
        }
    }

    class MasterBoardItemHandler implements ItemListener
    {
        public MasterBoardItemHandler()
        {
            super();
            net.sf.colossus.webcommon.InstanceTracker.register(this,
                cachedPlayerName);
        }

        public void itemStateChanged(ItemEvent e)
        {
            JMenuItem source = (JMenuItem)e.getSource();
            String text = source.getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            client.getOptions().setOption(text, selected);
        }
    }

    class MasterBoardWindowHandler extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            client.askNewCloseQuitCancel(masterFrame, false);
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
            offScreenBuffer = this.createImage(this.getWidth(), this
                .getHeight());
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
            paintPossibleRecruitChits(g);
            paintRecruitedChits(g);
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
        synchronized (markersOnBoard)
        {
            Iterator<Marker> it = markersOnBoard.iterator();
            while (it.hasNext())
            {
                Marker marker = it.next();
                if (marker != null
                    && g.getClipBounds().intersects(marker.getBounds()))
                {
                    marker.paintComponent(g);
                }
            }
        }
    }

    private void paintRecruitedChits(Graphics g)
    {
        Iterator<Chit> it = client.getRecruitedChits().iterator();
        while (it.hasNext())
        {
            Chit chit = it.next();
            if (chit != null && g.getClipBounds().intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }
    }

    private void paintPossibleRecruitChits(Graphics g)
    {
        Iterator<Chit> it = client.getPossibleRecruitChits().iterator();
        while (it.hasNext())
        {
            Chit chit = it.next();
            if (chit != null && g.getClipBounds().intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }
    }

    private void paintMovementDie(Graphics g)
    {
        if (client != null)
        {
            MovementDie die = client.getMovementDie();
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

    JScrollPane getScrollPane()
    {
        return scrollPane;
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

    void reqFocus()
    {
        if (client.getOptions().getOption(Options.stealFocus))
        {
            requestFocus();
            getFrame().toFront();
        }
    }

    class BottomBar extends JPanel
    {
        private final JLabel playerLabel;

        /** quick access button to the doneWithPhase action.
         *  must be en- and disabled often.
         */
        private final JButton doneButton;

        /** display the current phase in the bottom bar */
        private final JLabel phaseLabel;

        /**
         * Displays reasons why "Done" can not be used.
         */
        private final JLabel todoLabel;

        public void setPlayerName(String s)
        {
            playerLabel.setText(s);
        }

        public void setPlayerColor(Color color)
        {
            playerLabel.setForeground(color);
        }

        public void setPhase(String s)
        {
            phaseLabel.setText(s);
        }

        public void setReasonForDisabledDone(String reason)
        {
            todoLabel.setText("(" + reason + ")");
        }

        public BottomBar()
        {
            super();

            setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

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
        }
    }

    public void enableDoneAction()
    {
        doneWithPhaseAction.setEnabled(true);
    }

    public void disableDoneAction(String reason)
    {
        doneWithPhaseAction.setEnabled(false);
        bottomBar.setReasonForDisabledDone(reason);
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
}
