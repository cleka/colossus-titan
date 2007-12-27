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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.server.XMLSnapshotFilter;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.xmlparser.StrategicMapLoader;
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
    private static int horizSize = 0;
    private static int vertSize = 0;
    private boolean overlayChanged = false;

    /** "parity" of the board, so that Hexes are displayed the proper way */
    private static int boardParity = 0;

    private GUIMasterHex[][] guiHexArray = null;
    private static MasterHex[][] plainHexArray = null;

    /** The hexes in the horizSize*vertSize array that actually exist are
     *  represented by true. */
    private static boolean[][] show = null;

    private Client client;

    private KFrame masterFrame;
    private ShowReadme showReadme;
    private ShowHelpDoc showHelpDoc;
    private JMenu phaseMenu;
    private JPopupMenu popupMenu;
    private Map checkboxes = new HashMap();
    private JPanel[] legionFlyouts;

    private MasterBoardWindowHandler mbwh;
    private InfoPopupHandler iph;

    /** Last point clicked is needed for popup menus. */
    private Point lastPoint;

    /** The scrollbarspanel, needed to correct lastPoint. */
    private JScrollPane scrollPane;

    private Container contentPane;

    /** our own little bar implementation */
    private BottomBar bottomBar;

    private KFrame preferencesWindow;
    
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
    public static final String changeScale = "Change Scale";

    public static final String chooseScreen = "Choose Screen For Info Windows";
    public static final String preferences = "Preferences";

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
    private AbstractAction changeScaleAction;

    private AbstractAction chooseScreenAction;

    private AbstractAction preferencesAction;
    
    private AbstractAction aboutAction;
    private AbstractAction viewReadmeAction;
    private AbstractAction viewHelpDocAction;

    /* a Set of label (String) of all Tower hex */
    private static Set towerSet = null;

    private boolean playerLabelDone;

    private JMenu lfMenu;
    private SaveWindow saveWindow;

    private String cachedPlayerName = "<not set yet>";

    private final class InfoPopupHandler extends KeyAdapter
    {
        private static final int POPUP_KEY_ALL_LEGIONS = KeyEvent.VK_SHIFT;
        private static final int POPUP_KEY_MY_LEGIONS = KeyEvent.VK_CONTROL;
        private static final int PANEL_MARGIN = 4;
        private static final int PANEL_PADDING = 0;

        private final WeakReference clientRef;

        private InfoPopupHandler(Client client)
        {
            super();
            this.clientRef = new WeakReference(client);
            net.sf.colossus.webcommon.FinalizeManager.register(this, client
                .getPlayerName());
        }

        public void keyPressed(KeyEvent e)
        {
            Client client = (Client)clientRef.get();
            if (client == null)
            {
                return;
            }
            if (e.getKeyCode() == POPUP_KEY_ALL_LEGIONS)
            {
                if (legionFlyouts == null)
                {
                    createLegionFlyouts(client.getMarkers());
                }
            }
            else if (e.getKeyCode() == POPUP_KEY_MY_LEGIONS)
            {
                if (legionFlyouts == null)
                {
                    // copy only local players markers
                    List myMarkers = new ArrayList();
                    for (Iterator iterator = client.getMarkers().iterator(); iterator
                        .hasNext();)
                    {
                        Marker marker = (Marker)iterator.next();
                        LegionInfo legionInfo = client.getLegionInfo(marker
                            .getId());
                        if (legionInfo.isMyLegion())
                        {
                            myMarkers.add(marker);
                        }
                    }
                    createLegionFlyouts(myMarkers);
                }
            }
            else
            {
                super.keyPressed(e);
            }
        }

        private void createLegionFlyouts(List markers)
        {
            // copy to array so we don't get concurrent modification
            // exceptions when iterating
            Marker[] markerArray = (Marker[])markers
                .toArray(new Marker[markers.size()]);
            legionFlyouts = new JPanel[markers.size()];
            for (int i = 0; i < markerArray.length; i++)
            {
                Marker marker = markerArray[i];
                LegionInfo legion = client.getLegionInfo(marker.getId());
                int scale = 2 * Scale.get();

                boolean dubiousAsBlanks = client
                    .getOption(Options.dubiousAsBlanks);
                final JPanel panel = new LegionInfoPanel(legion, scale,
                    PANEL_MARGIN, PANEL_PADDING, true, client.getViewMode(),
                    client.getPlayerName(), dubiousAsBlanks, true);
                add(panel);
                legionFlyouts[i] = panel;

                panel.setLocation(marker.getLocation());
                panel.setVisible(true);
                DragListener.makeDraggable(panel);

                repaint();
            }
        }

        public void keyReleased(KeyEvent e)
        {
            if ((e.getKeyCode() == POPUP_KEY_ALL_LEGIONS)
                || (e.getKeyCode() == POPUP_KEY_MY_LEGIONS))
            {
                if (legionFlyouts != null)
                {
                    for (int i = 0; i < legionFlyouts.length; i++)
                    {
                        remove(legionFlyouts[i]);
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

    private static interface MasterHexVisitor
    {

        /** Returns true iff the Hex matches **/
        boolean visitHex(MasterHex hex);
    }

    private static interface GUIMasterHexVisitor
    {

        /** Returns true iff the Hex matches **/
        boolean visitHex(GUIMasterHex hex);
    }

    private static MasterHex visitMasterHexes(MasterHexVisitor visitor)
    {
        for (int i = 0; i < plainHexArray.length; i++)
        {
            for (int j = 0; j < plainHexArray[i].length; j++)
            {
                MasterHex hex = plainHexArray[i][j];
                if (hex == null)
                {
                    continue;
                }
                boolean hexFound = visitor.visitHex(hex);
                if (hexFound)
                {
                    return hex;
                }
            }
        }
        return null;
    }

    private GUIMasterHex visitGUIMasterHexes(GUIMasterHexVisitor visitor)
    {
        for (int i = 0; i < guiHexArray.length; i++)
        {
            for (int j = 0; j < guiHexArray[i].length; j++)
            {
                GUIMasterHex hex = guiHexArray[i][j];
                if (hex == null)
                {
                    continue;
                }
                boolean hexFound = visitor.visitHex(hex);
                if (hexFound)
                {
                    return hex;
                }
            }
        }
        return null;
    }

    /** Must ensure that variant is loaded before referencing this class,
     *  since readMapData() needs it. */
    public synchronized static void staticMasterboardInit()
    {
        // variant can change these
        horizSize = 0;
        vertSize = 0;
        boardParity = 0;
        plainHexArray = null;
        show = null;
        towerSet = null;

        try
        {
            readMapData();
        }
        catch (Exception e)
        {
            LOGGER
                .log(Level.SEVERE, "Reading map data for non-GUI failed.", e);
            e.printStackTrace();
            System.exit(1);
        }

        LOGGER.log(Level.FINEST, "Setting up static TowerSet in MasterBoard");
        setupTowerSet();
    }

    MasterBoard(final Client client)
    {
        this.client = client;
        net.sf.colossus.webcommon.FinalizeManager.register(this, client
            .getPlayerName());

        String pname = client.getPlayerName();
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

        preferencesWindow = new PreferencesWindow(client);
        
        scrollPane = new JScrollPane(this);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setupPlayerLabel();

        saveWindow = new SaveWindow(client, "MasterBoardScreen");
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
                new ShowAllRecruits(masterFrame, client, TerrainRecruitLoader
                    .getTerrains());
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
                    MasterHex hexModel = hex.getMasterHexModel();
                    new ShowRecruits(masterFrame, hexModel.getTerrain(),
                        lastPoint, hexModel.getLabel(), scrollPane);
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
                    new ShowBattleMap(masterFrame, hex.getMasterHexModel()
                        .getLabel(), hex);
                    // Work around a Windows JDK 1.3 bug.
                    hex.repaint();
                }
            }
        };

        /*
         * After confirmation (if necessary, i.e. not gameover yet), 
         * totally quit everything (shut down server and all windows)
         * so that the SystemExitManager knows it can let the main
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
                client.closeBoardAfterConfirm(masterFrame, false);
            }
        };

        changeScaleAction = new AbstractAction(changeScale)
        {
            public void actionPerformed(ActionEvent e)
            {
                final int oldScale = Scale.get();
                final int newScale = PickIntValue.pickIntValue(masterFrame,
                    oldScale, "Pick scale", 5, 25, 1, client);
                if (newScale != oldScale)
                {
                    client.setOption(Options.scale, newScale);
                    Scale.set(newScale);
                    net.sf.colossus.util.ResourceLoader.purgeImageCache();
                    client.rescaleAllWindows();
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
                preferencesWindow.setVisible(true);
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
                showReadme = new ShowReadme(client);
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
        cbmi.setSelected(client.getOption(name));

        cbmi.addItemListener(itemHandler);
        menu.add(cbmi);
        checkboxes.put(name, cbmi);
        return cbmi;
    }

    private void cleanCBListeners()
    {
        Iterator it = checkboxes.keySet().iterator();
        while (it.hasNext())
        {
            String key = (String)it.next();
            JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem)checkboxes.get(key);
            cbmi.removeItemListener(itemHandler);
        }
        checkboxes.clear();
        checkboxes = null;
        itemHandler = null;
    }

    private ItemListener rcmHandler = new MasterBoardRecruitChitMenuHandler();

    private void addRadioButton(JMenu menu, ButtonGroup group, String name)
    {
        JRadioButtonMenuItem rbmi = new JRadioButtonMenuItem(name);
        rbmi.addItemListener(rcmHandler);
        group.add(rbmi);
        menu.add(rbmi);
        boolean selected = false;
        if (name.equals(client
            .getStringOption(Options.showRecruitChitsSubmenu)))
        {
            selected = true;
        }
        rbmi.setSelected(selected);
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

        // Then per-player options

        JMenu playerMenu = new JMenu("Autoplay");
        playerMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(playerMenu);

        addCheckBox(playerMenu, Options.autoPickColor, KeyEvent.VK_C);
        addCheckBox(playerMenu, Options.autoPickMarker, KeyEvent.VK_I);
        addCheckBox(playerMenu, Options.autoPickEntrySide, KeyEvent.VK_E);
        addCheckBox(playerMenu, Options.autoForcedStrike, KeyEvent.VK_K);
        addCheckBox(playerMenu, Options.autoCarrySingle, KeyEvent.VK_Y);
        addCheckBox(playerMenu, Options.autoRangeSingle, KeyEvent.VK_G);
        addCheckBox(playerMenu, Options.autoSummonAngels, KeyEvent.VK_O);
        addCheckBox(playerMenu, Options.autoAcquireAngels, KeyEvent.VK_A);
        addCheckBox(playerMenu, Options.autoRecruit, KeyEvent.VK_R);
        addCheckBox(playerMenu, Options.autoPickRecruiter, KeyEvent.VK_U);
        addCheckBox(playerMenu, Options.autoReinforce, KeyEvent.VK_N);
        addCheckBox(playerMenu, Options.autoPlay, KeyEvent.VK_P);

        // Then per-client GUI options
        JMenu graphicsMenu = new JMenu("Graphics");
        graphicsMenu.setMnemonic(KeyEvent.VK_G);
        menuBar.add(graphicsMenu);

        addCheckBox(graphicsMenu, Options.antialias, KeyEvent.VK_N);
        addCheckBox(graphicsMenu, Options.useOverlay, KeyEvent.VK_V);
        addCheckBox(graphicsMenu, Options.useSVG, KeyEvent.VK_S);
        addCheckBox(graphicsMenu, Options.noBaseColor, KeyEvent.VK_W);
        addCheckBox(graphicsMenu, Options.useColoredBorders, 0);
        addCheckBox(graphicsMenu, Options.doNotInvertDefender, 0);
        addCheckBox(graphicsMenu, Options.hideAdjStrikeDiceRangeStrike, 0);
        graphicsMenu.addSeparator();

        // The "dubious as blanks" option makes only sense with the 
        //   "view what SplitPrediction tells us" mode => otherwise inactive.
        JCheckBoxMenuItem cbmi = addCheckBox(graphicsMenu,
            Options.dubiousAsBlanks, KeyEvent.VK_D);
        if (client.getViewMode() != Options.viewableEverNum)
        {
            cbmi.setEnabled(false);
        }

        // "Show recruit preview chits ..." submenu
        JMenu srcSubmenu = new JMenu(Options.showRecruitChitsSubmenu);
        ButtonGroup group = new ButtonGroup();
        addRadioButton(srcSubmenu, group, Options.showRecruitChitsNone);
        addRadioButton(srcSubmenu, group, Options.showRecruitChitsStrongest);
        addRadioButton(srcSubmenu, group, Options.showRecruitChitsRecruitHint);
        addRadioButton(srcSubmenu, group, Options.showRecruitChitsAll);
        graphicsMenu.add(srcSubmenu);

        // Menu for the "window-related"
        // (satellite windows and graphic options effecting whole "windows")
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

        // "Window"-behavior and look&feel related ones:
        addCheckBox(windowMenu, Options.stealFocus, KeyEvent.VK_F);

        // change scale
        mi = windowMenu.add(changeScaleAction);
        mi.setMnemonic(KeyEvent.VK_S);

        if (GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getScreenDevices().length > 1)
        {
            mi = windowMenu.add(chooseScreenAction);
        }

        // Look & Feel now under other menu instead own
        lfMenu = new JMenu("Look & Feel");
        // lfMenu.setMnemonic(KeyEvent.VK_L);
        windowMenu.add(lfMenu);

        UIManager.LookAndFeelInfo[] lfInfo = UIManager
            .getInstalledLookAndFeels();
        String currentLF = UIManager.getLookAndFeel().getName();
        for (int i = 0; i < lfInfo.length; i++)
        {
            AbstractAction lfAction = new ChangeLookFeelAction(lfInfo[i]
                .getName(), lfInfo[i].getClassName());
            JCheckBoxMenuItem temp = new JCheckBoxMenuItem(lfAction);
            lfMenu.add(temp);
            temp.setState(lfInfo[i].getName().equals(currentLF));
        }

        // Setting menu
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(settingsMenu);

        mi = settingsMenu.add(preferencesAction);
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

    class ChangeLookFeelAction extends AbstractAction
    {
        String className;

        ChangeLookFeelAction(String t, String className)
        {
            super(t);
            this.className = className;
        }

        public void actionPerformed(ActionEvent e)
        {
            client.setLookAndFeel(className);
            String currentLF = UIManager.getLookAndFeel().getName();
            for (int i = 0; i < lfMenu.getItemCount(); i++)
            {
                JCheckBoxMenuItem it = (JCheckBoxMenuItem)lfMenu.getItem(i);
                it.setState(it.getText().equals(currentLF));
            }
        }
    }

    void twiddleOption(String name, boolean enable)
    {
        JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem)checkboxes.get(name);
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
        String playerName = client.getPlayerName();
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
        guiHexArray = new GUIMasterHex[horizSize][vertSize];

        int scale = Scale.get();
        int cx = 3 * scale;
        int cy = 0 * scale;

        for (int i = 0; i < guiHexArray.length; i++)
        {
            for (int j = 0; j < guiHexArray[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIMasterHex hex = new GUIMasterHex(plainHexArray[i][j]);
                    hex
                        .init(
                            cx + 4 * i * scale,
                            (int)Math
                                .round(cy
                                    + (3 * j + ((i + boardParity) & 1)
                                        * (1 + 2 * (j / 2)) + ((i + 1 + boardParity) & 1)
                                        * 2 * ((j + 1) / 2)) * GUIHex.SQRT3
                                    * scale), scale, isHexInverted(i, j), this);
                    guiHexArray[i][j] = hex;
                }
            }
        }
    }

    private void cleanGUIHexes()
    {
        for (int i = 0; i < guiHexArray.length; i++)
        {
            for (int j = 0; j < guiHexArray[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIMasterHex hex = guiHexArray[i][j];
                    hex.cleanup();
                    guiHexArray[i][j] = null;
                }
            }
        }
    }

    private static boolean isHexInverted(int i, int j)
    {
        return (((i + j) & 1) == boardParity);
    }

    /** Reference to the 'h' the cache was built for.
     *  We have to rebuild the cache for a new 'h'
     */
    private static MasterHex[][] _hexByLabel_last_h = null;

    /** the cache used inside 'hexByLabel'. */
    private static java.util.Vector _hexByLabel_cache = null;

    /**
     * towi changes: here is now a cache implemented so that the nested
     *   loop is not executed at every call. the cache is implemented with
     *   an array. it will work as long as the hex-labels-strings can be
     *   converted to int. this must be the case anyway since the
     *   param 'label' is an int here.
     */
    static MasterHex hexByLabel(MasterHex[][] h, int label)
    {
        // if the 'h' was the same last time we can use the cache
        if (_hexByLabel_last_h != h)
        {
            // alas, we have to rebuild the cache
            LOGGER.log(Level.FINEST,
                "new 'MasterHex[][] h' in MasterBoard.hexByLabel()");
            _hexByLabel_last_h = h;
            // write all 'h' elements by their int-value into an Array.
            // we can do that here, because the 'label' arg is an int. if it
            // were a string we could not rely on that all h-entries are ints.
            // (Vector: lots of unused space, i am afraid. about 80kB...)
            _hexByLabel_cache = new java.util.Vector(1000);
            for (int i = 0; i < h.length; i++)
            {
                for (int j = 0; j < h[i].length; j++)
                {
                    if (show[i][j])
                    {
                        final int iLabel = Integer
                            .parseInt(h[i][j].getLabel());
                        if (_hexByLabel_cache.size() <= iLabel)
                        {
                            _hexByLabel_cache.setSize(iLabel + 1);
                        }
                        _hexByLabel_cache.set(iLabel, h[i][j]);
                    }
                }
            }
        }
        // the cache is built and looks like this:
        //   _hexByLabel_cache[0...] =
        //      [ h00,h01,h02, ..., null, null, ..., h30,h31,... ]
        final MasterHex found = (MasterHex)_hexByLabel_cache.get(label);
        if (found == null)
        {
            LOGGER.log(Level.WARNING, "Couldn't find Masterhex labeled "
                + label);
        }
        return found;
    }

    private static synchronized void readMapData() throws Exception
    {
        List directories = VariantSupport.getVarDirectoriesList();
        InputStream mapIS = ResourceLoader.getInputStream(VariantSupport
            .getMapName(), directories);
        if (mapIS == null)
        {
            throw new FileNotFoundException(VariantSupport.getMapName());
        }
        StrategicMapLoader sml = new StrategicMapLoader(mapIS);
        horizSize = sml.getHorizSize();
        vertSize = sml.getVertSize();
        show = sml.getShow();
        plainHexArray = sml.getHexes();

        computeBoardParity();
        setupExits(plainHexArray);
        setupEntrances(plainHexArray);
        setupHexLabelSides(plainHexArray);
        setupNeighbors(plainHexArray);
    }

    private static void computeBoardParity()
    {
        boardParity = 0;
        for (int x = 0; x < horizSize; x++)
        {
            for (int y = 0; y < vertSize - 1; y++)
            {
                if (show[x][y] && show[x][y + 1])
                {
                    boardParity = 1 - ((x + y) & 1);
                    return;
                }
            }
        }
    }

    private static void setupExits(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 3; k++)
                    {
                        if (h[i][j].getBaseExitType(k) != Constants.NONE)
                        {
                            setupOneExit(h, i, j, k);
                        }
                    }
                }
            }
        }
    }

    private static void setupOneExit(MasterHex[][] h, int i, int j, int k)
    {
        MasterHex dh = hexByLabel(h, h[i][j].getBaseExitLabel(k));
        if (dh == null)
        {
            LOGGER.log(Level.SEVERE, "null pointer ; i=" + i + ", j=" + j
                + ", k=" + k);
            System.exit(1);
        }
        // Static analysis of Eclipse doesn't grok System.exit()
        assert dh != null;
        if (dh.getXCoord() == i)
        {
            if (dh.getYCoord() == (j - 1))
            {
                h[i][j].setExitType(0, h[i][j].getBaseExitType(k));
            }
            else if (dh.getYCoord() == (j + 1))
            {
                h[i][j].setExitType(3, h[i][j].getBaseExitType(k));
            }
            else
            {
                LOGGER.log(Level.WARNING, "bad exit ; i=" + i + ", j=" + j
                    + ", k=" + k);
            }
        }
        else if (dh.getXCoord() == (i + 1))
        {
            if (dh.getYCoord() == j)
            {
                h[i][j].setExitType(2 - ((i + j + boardParity) & 1), h[i][j]
                    .getBaseExitType(k));
            }
            else
            {
                LOGGER.log(Level.WARNING, "bad exit ; i=" + i + ", j=" + j
                    + ", k=" + k);
            }
        }
        else if (dh.getXCoord() == (i - 1))
        {
            if (dh.getYCoord() == j)
            {
                h[i][j].setExitType(4 + ((i + j + boardParity) & 1), h[i][j]
                    .getBaseExitType(k));
            }
            else
            {
                LOGGER.log(Level.WARNING, "bad exit ; i=" + i + ", j=" + j
                    + ", k=" + k);
            }
        }
        else
        {
            LOGGER.log(Level.WARNING, "bad exit ; i=" + i + ", j=" + j
                + ", k=" + k);
        }
    }

    private static void setupEntrances(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].getExitType(k);
                        if (gateType != Constants.NONE)
                        {
                            switch (k)
                            {
                                case 0:
                                    h[i][j - 1].setEntranceType(3, gateType);
                                    break;

                                case 1:
                                    h[i + 1][j].setEntranceType(4, gateType);
                                    break;

                                case 2:
                                    h[i + 1][j].setEntranceType(5, gateType);
                                    break;

                                case 3:
                                    h[i][j + 1].setEntranceType(0, gateType);
                                    break;

                                case 4:
                                    h[i - 1][j].setEntranceType(1, gateType);
                                    break;

                                case 5:
                                    h[i - 1][j].setEntranceType(2, gateType);
                                    break;

                                default:
                                    LOGGER.log(Level.SEVERE, "Bogus hexside");
                            }
                        }
                    }
                }
            }
        }
    }

    /** If the shortest hexside closest to the center of the board
     *  is a short hexside, set the label side to it.
     *  Else set the label side to the opposite hexside. */
    private static void setupHexLabelSides(MasterHex[][] h)
    {
        // First find the center of the board.
        int width = h.length;
        int height = h[0].length;

        // Subtract 1 to account for 1-based length of 0-based array.
        double midX = (width - 1) / 2.0;
        double midY = (height - 1) / 2.0;

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    double deltaX = i - midX;
                    // Adjust for aspect ratio of h array, which has roughly
                    // twice as many horizontal as vertical elements even
                    // though the board is roughly square.
                    double deltaY = (j - midY) * width / height;

                    double ratio;

                    // Watch for division by zero.
                    if (deltaY == 0)
                    {
                        ratio = deltaX * 99999999;
                    }
                    else
                    {
                        ratio = deltaX / deltaY;
                    }

                    // Derive the exact number if needed.
                    if (Math.abs(ratio) < 0.6)
                    {
                        // Vertically dominated, so top or bottom hexside.
                        // top, unless inverted
                        if (isHexInverted(i, j))
                        {
                            h[i][j].setLabelSide(3);
                        }
                        else
                        {
                            h[i][j].setLabelSide(0);
                        }
                    }
                    else
                    {
                        // One of the left or right side hexsides.
                        if (deltaX >= 0)
                        {
                            if (deltaY >= 0)
                            {
                                // 2 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(5);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(2);
                                }
                            }
                            else
                            {
                                // 4 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(1);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(4);
                                }
                            }
                        }
                        else
                        {
                            if (deltaY >= 0)
                            {
                                // 4 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(1);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(4);
                                }
                            }
                            else
                            {
                                // 2 unless inverted
                                if (isHexInverted(i, j))
                                {
                                    h[i][j].setLabelSide(5);
                                }
                                else
                                {
                                    h[i][j].setLabelSide(2);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void setupNeighbors(MasterHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHex hex = h[i][j];

                    if (hex.getExitType(0) != Constants.NONE
                        || hex.getEntranceType(0) != Constants.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != Constants.NONE
                        || hex.getEntranceType(1) != Constants.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != Constants.NONE
                        || hex.getEntranceType(2) != Constants.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != Constants.NONE
                        || hex.getEntranceType(3) != Constants.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != Constants.NONE
                        || hex.getEntranceType(4) != Constants.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != Constants.NONE
                        || hex.getEntranceType(5) != Constants.NONE)
                    {
                        hex.setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }

    void setupSplitMenu()
    {
        unselectAllHexes();
        reqFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
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
            bottomBar.setPhase("(" + activePlayerName + " splits)");
        }
    }

    void setupMoveMenu()
    {
        unselectAllHexes();
        reqFocus();

        String activePlayerName = client.getActivePlayerName();
        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Movement Roll: "
            + client.getMovementRoll());

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
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

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Resolve Engagements ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
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

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn "
            + client.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
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

    void alignLegions(String hexLabel)
    {
        GUIMasterHex hex = getGUIHexByLabel(hexLabel);
        if (hex == null)
        {
            return;
        }
        List markerIds = client.getLegionsByHex(hexLabel);

        int numLegions = markerIds.size();
        if (numLegions == 0)
        {
            hex.repaint();
            return;
        }

        String markerId = (String)markerIds.get(0);
        Marker marker = client.getMarker(markerId);
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
            markerId = (String)markerIds.get(1);
            marker = client.getMarker(markerId);
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
            markerId = (String)markerIds.get(1);
            marker = client.getMarker(markerId);
            marker.setLocation(point);

            point = new Point(startingPoint);
            point.x -= chitScale4;
            point.y -= chitScale;
            markerId = (String)markerIds.get(2);
            marker = client.getMarker(markerId);
            marker.setLocation(point);
        }

        hex.repaint();
    }

    void alignLegions(Set hexLabels)
    {
        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            alignLegions(hexLabel);
        }
    }

    /** This is incredibly inefficient. */
    void alignAllLegions()
    {
        visitMasterHexes(new MasterHexVisitor()
        {
            public boolean visitHex(MasterHex hex)
            {
                alignLegions(hex.getLabel());
                return false;
            }
        });
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
    private void highlightMoves(String markerId)
    {
        unselectAllHexes();

        Set teleport = client.listTeleportMoves(markerId);
        selectHexesByLabels(teleport, HTMLColor.purple);

        Set normal = client.listNormalMoves(markerId);
        selectHexesByLabels(normal, Color.white);

        Set combo = new HashSet();
        combo.addAll(teleport);
        combo.addAll(normal);

        client.addPossibleRecruitChits(markerId, combo);
    }

    void highlightEngagements()
    {
        Set set = client.findEngagements();
        unselectAllHexes();
        selectHexesByLabels(set);
    }

    /** Return number of legions with summonable angels. */
    int highlightSummonableAngels(String markerId)
    {
        Set set = client.findSummonableAngelHexes(markerId);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }

    private void setupIcon()
    {
        List directories = new ArrayList();
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
    public static MasterHex getHexByLabel(final String label)
    {
        return visitMasterHexes(new MasterHexVisitor()
        {
            public boolean visitHex(MasterHex hex)
            {
                if (hex.getLabel().equals(label))
                {
                    return true;
                }
                return false;
            }
        });
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    GUIMasterHex getGUIHexByLabel(final String label)
    {
        return visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                return hex.getMasterHexModel().getLabel().equals(label);
            }
        });
    }

    /** Return the MasterHex that contains the given point, or
     *  null if none does. */
    private GUIMasterHex getHexContainingPoint(final Point point)
    {
        return visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                return hex.contains(point);
            }
        });
    }

    /** Return the topmost Marker that contains the given point, or
     *  null if none does. */
    private Marker getMarkerAtPoint(Point point)
    {
        List markers = client.getMarkers();
        ListIterator lit = markers.listIterator(markers.size());
        while (lit.hasPrevious())
        {
            Marker marker = (Marker)lit.previous();
            if (marker != null && marker.contains(point))
            {
                return marker;
            }
        }
        return null;
    }

    void unselectAllHexes()
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
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
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                if (hex.isSelected()
                    && label.equals(hex.getMasterHexModel().getLabel()))
                {
                    hex.unselect();
                    hex.repaint();
                    return true;
                }
                return false; // keep going
            }
        });
    }

    void unselectHexesByLabels(final Set labels)
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                if (hex.isSelected()
                    && labels.contains(hex.getMasterHexModel().getLabel()))
                {
                    hex.unselect();
                    hex.repaint();
                }
                return false; // keep going
            }
        });
    }

    void selectHexByLabel(final String label)
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                if (!hex.isSelected()
                    && label.equals(hex.getMasterHexModel().getLabel()))
                {
                    hex.select();
                    hex.repaint();
                }
                return false; // keep going
            }
        });
    }

    void selectHexesByLabels(final Set labels)
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                if (!hex.isSelected()
                    && labels.contains(hex.getMasterHexModel().getLabel()))
                {
                    hex.select();
                    hex.repaint();
                }
                return false; // keep going
            }
        });
    }

    void selectHexesByLabels(final Set labels, final Color color)
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                if (labels.contains(hex.getMasterHexModel().getLabel()))
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
            client.setMoverId(null);
            highlightUnmovedLegions();
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            SummonAngel summonAngel = client.getSummonAngel();
            if (summonAngel != null)
            {
                highlightSummonableAngels(summonAngel.getMarkerId());
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
        public void mousePressed(MouseEvent e)
        {
            Point point = e.getPoint();
            Marker marker = getMarkerAtPoint(point);
            GUIMasterHex hex = getHexContainingPoint(point);
            if (marker != null)
            {
                String markerId = marker.getId();

                // Move the clicked-on marker to the top of the z-order.
                client.setMarker(markerId, marker);

                // What to do depends on which mouse button was used
                // and the current phase of the turn.

                // Right-click means to show the contents of the legion.
                if (isPopupButton(e))
                {
                    LegionInfo legion = client.getLegionInfo(markerId);
                    String playerName = client.getPlayerName();
                    int viewMode = client.getViewMode();
                    boolean dubiousAsBlanks = client
                        .getOption(Options.dubiousAsBlanks);
                    new ShowLegion(masterFrame, legion, point, scrollPane,
                        4 * Scale.get(), playerName, viewMode, dubiousAsBlanks);
                    return;
                }
                else if (client.isMyLegion(markerId))
                {
                    if (hex != null)
                    {
                        actOnLegion(markerId, hex.getMasterHexModel()
                            .getLabel());
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
                    popupMenu.setLabel(hex.getMasterHexModel()
                        .getDescription());
                    popupMenu.show(e.getComponent(), point.x, point.y);
                    return;
                }

                // Otherwise, the action to take depends on the phase.
                // Only the current player can manipulate game state.
                if (client.getPlayerName()
                    .equals(client.getActivePlayerName()))
                {
                    actOnHex(hex.getMasterHexModel().getLabel());
                    hex.repaint();
                    return;
                }
            }

            // No hits on chits or map, so re-highlight.
            if (client.getPlayerName().equals(client.getActivePlayerName()))
            {
                actOnMisclick();
            }
        }
    }

    class MasterBoardMouseMotionHandler extends MouseMotionAdapter
    {
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

    private void actOnLegion(String markerId, String hexLabel)
    {
        if (!client.isMyTurn())
        {
            return;
        }

        Constants.Phase phase = client.getPhase();
        if (phase == Constants.Phase.SPLIT)
        {
            client.doSplit(markerId);
        }
        else if (phase == Constants.Phase.MOVE)
        {
            // Allow spin cycle by clicking on chit again.
            if (markerId.equals(client.getMoverId()))
            {
                actOnHex(hexLabel);
            }
            else
            {
                client.setMoverId(markerId);
                getGUIHexByLabel(hexLabel).repaint();
                highlightMoves(markerId);
            }
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            client.doFight(hexLabel);
        }
        else if (phase == Constants.Phase.MUSTER)
        {
            client.doRecruit(markerId);
        }
    }

    private void actOnHex(String hexLabel)
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
            client.doMove(hexLabel);
            actOnMisclick(); // Yes, even if the move was good.
        }
        else if (phase == Constants.Phase.FIGHT)
        {
            // If we're fighting and there is an engagement here,
            // resolve it.  If an angel is being summoned, mark
            // the donor legion instead.
            client.engage(hexLabel);
        }
    }

    class MasterBoardItemHandler implements ItemListener
    {
        public MasterBoardItemHandler()
        {
            super();
            net.sf.colossus.webcommon.FinalizeManager.register(this,
                cachedPlayerName);
        }

        public void itemStateChanged(ItemEvent e)
        {
            JMenuItem source = (JMenuItem)e.getSource();
            String text = source.getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            client.setOption(text, selected);
        }
    }

    class MasterBoardRecruitChitMenuHandler implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            JMenuItem source = (JMenuItem)e.getSource();
            String text = source.getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            if (selected && text != null)
            {
                client.setOption(Options.showRecruitChitsSubmenu, text);
            }
        }
    }

    class MasterBoardWindowHandler extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            closeBoardAction.actionPerformed(null);
        }
    }

    void repaintAfterOverlayChanged()
    {
        overlayChanged = true;
        this.getFrame().repaint();
    }

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
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                hex.paint(g);
                return false; // keep going
            }
        });
    }

    private void paintHighlights(final Graphics2D g)
    {
        visitGUIMasterHexes(new GUIMasterHexVisitor()
        {
            public boolean visitHex(GUIMasterHex hex)
            {
                hex.paintHighlightIfNeeded(g);
                return false; // keep going
            }
        });
    }

    /** Paint markers in z-order. */
    private void paintMarkers(Graphics g)
    {
        Iterator it = client.getMarkers().iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker != null
                && g.getClipBounds().intersects(marker.getBounds()))
            {
                marker.paintComponent(g);
            }
        }
    }

    private void paintRecruitedChits(Graphics g)
    {
        Iterator it = client.getRecruitedChits().iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            if (chit != null && g.getClipBounds().intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }
    }

    private void paintPossibleRecruitChits(Graphics g)
    {
        Iterator it = client.getPossibleRecruitChits().iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
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

    public Dimension getMinimumSize()
    {
        int scale = Scale.get();
        return new Dimension(((horizSize + 1) * 4) * scale, (vertSize * 7)
            * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    void rescale()
    {
        setupGUIHexes();
        client.recreateMarkers();
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
        preferencesWindow.dispose();
        preferencesWindow = null;

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

    public static Set getTowerSet()
    {
        return Collections.unmodifiableSet(towerSet);
    }

    private static void setupTowerSet()
    {
        towerSet = new HashSet();
        visitMasterHexes(new MasterHexVisitor()
        {
            public boolean visitHex(MasterHex hex)
            {
                if (HexMap.terrainIsTower(hex.getTerrain()))
                {
                    towerSet.add(hex.getLabel());
                }
                return false;
            }
        });
    }

    JScrollPane getScrollPane()
    {
        return scrollPane;
    }

    /** Return a set of all hex labels. */
    static Set getAllHexLabels()
    {
        final Set set = new HashSet();
        visitMasterHexes(new MasterHexVisitor()
        {
            public boolean visitHex(MasterHex hex)
            {
                set.add(hex.getLabel());
                return false;
            }
        });
        return set;
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
        if (client.getOption(Options.stealFocus))
        {
            requestFocus();
            getFrame().toFront();
        }
    }

    class PreferencesWindow extends KFrame
    {
        public PreferencesWindow(IOptions options)
        {
            super("Preferences");
            getContentPane().add(new JLabel("Dummy"));
            
            setDefaultCloseOperation(KFrame.HIDE_ON_CLOSE);
            pack();
            
            setPreferredSize(getSize());
            
            useSaveWindow(options, "Preferences", null);
        }
        
        public void dispose()
        {
            super.dispose();
        }
    }

    class BottomBar extends JPanel
    {
        private JLabel playerLabel;

        /** quick access button to the doneWithPhase action.
         *  must be en- and disabled often.
         */
        private JButton doneButton;

        /** display the current phase in the bottom bar */
        private JLabel phaseLabel;

        /**
         * Displays reasons why "Done" can not be used.
         */
        private JLabel todoLabel;

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
