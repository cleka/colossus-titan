package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.io.*;

import net.sf.colossus.util.Log;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.XMLSnapshotFilter;
import net.sf.colossus.server.ConfigFileFilter;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.parser.StrategicMapLoader;
import net.sf.colossus.parser.TerrainRecruitLoader;
import net.sf.colossus.parser.ParseException;


/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

public final class MasterBoard extends JPanel
{
    private static int horizSize = 0;
    private static int vertSize = 0;

    /** "parity" of the board, so that Hexes are displayed the proper way */
    private static int boardParity = 0;

    private GUIMasterHex[][] guiHexArray = null;
    private java.util.List guiHexList = null;
    private static MasterHex[][] plainHexArray = null;
    private static java.util.List plainHexList = null;

    /** The hexes in the horizSize*vertSize array that actually exist are
     *  represented by true. */
    private static boolean[][] show = null;

    private Client client;

    private JFrame masterFrame;
    private JMenu phaseMenu;
    private JPopupMenu popupMenu;
    private Map checkboxes = new HashMap();

    /** Last point clicked is needed for popup menus. */
    private Point lastPoint;

    /**The scrollbarspanel, needed to correct lastPoint.*/
    private JScrollPane scrollPane;

    private Container contentPane;
    private JLabel playerLabel;

    public static final String saveGameAs = "Save game as";

    public static final String clearRecruitChits = "Clear recruit chits";

    public static final String undoLast = "Undo";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";

    public static final String takeMulligan = "Take Mulligan";
    public static final String concedeBattle = "Concede battle";
    public static final String withdrawFromGame = "Withdraw from Game";

    public static final String viewFullRecruitTree = "View Full Recruit Tree";
    public static final String viewHexRecruitTree = "View Hex Recruit Tree";
    public static final String viewBattleMap = "View Battle Map";
    public static final String changeScale = "Change Scale";

    public static final String chooseScreen = "Choose Screen For Info Windows";

    public static final String about = "About";

    private AbstractAction newGameAction;
    private AbstractAction loadGameAction;
    private AbstractAction saveGameAction;
    private AbstractAction saveGameAsAction;
    private AbstractAction quitGameAction;

    private AbstractAction clearRecruitChitsAction;

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;

    private AbstractAction takeMulliganAction;
    private AbstractAction withdrawFromGameAction;

    private AbstractAction viewFullRecruitTreeAction;
    private AbstractAction viewHexRecruitTreeAction;
    private AbstractAction viewBattleMapAction;
    private AbstractAction changeScaleAction;

    private AbstractAction chooseScreenAction;

    private AbstractAction aboutAction;

    /* a Set of label (String) of all Tower hex */
    private static Set towerSet = null;

    private boolean playerLabelDone;

    private static StrategicMapLoader sml = null;

    private JMenu lfMenu;

    /** Must ensure that variant is loaded before referencing this class,
     *  since readMapData() needs it. */
    public synchronized static void staticMasterboardInit()
    {
        // variant can changes those
        horizSize = 0;
        vertSize = 0;
        boardParity = 0;
        plainHexArray = null;
        plainHexList = null;
        show = null;
        towerSet = null;
        sml = null;

        try
        {
            readMapData();
        }
        catch (Exception e)
        {
            Log.error("Reading map data for non-GUI failed : " + e);
            System.exit(1);
        }

        Log.debug("Setting up static arrays in MasterBoard");

        setupPlainHexes();

        Log.debug("Setting up static TowerSet in MasterBoard");

        setupTowerSet();
    }

    MasterBoard(Client client)
    {
        this.client = client;

        masterFrame = new JFrame("MasterBoard");
        masterFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        contentPane = masterFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        setOpaque(true);
        setupIcon();
        setBackground(Color.black);
        masterFrame.addWindowListener(new MasterBoardWindowHandler());
        addMouseListener(new MasterBoardMouseHandler());

        setupGUIHexes();

        setupActions();
        setupPopupMenu();
        setupTopMenu();

        scrollPane = new JScrollPane(this);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setupPlayerLabel();

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
                int phase = client.getPhase();
                switch (phase)
                {
                    case Constants.SPLIT:
                        client.undoLastSplit();
                        alignAllLegions();
                        highlightTallLegions();
                        repaint();
                        break;

                    case Constants.MOVE:
                        client.undoLastMove();
                        highlightUnmovedLegions();
                        break;

                    case Constants.FIGHT:
                        Log.error("called undoLastAction in FIGHT");
                        break;

                    case Constants.MUSTER:
                        client.undoLastRecruit();
                        highlightPossibleRecruits();
                        break;

                    default:
                        Log.error("Bogus phase");
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = client.getPhase();
                switch (phase)
                {
                    case Constants.SPLIT:
                        client.undoAllSplits();
                        alignAllLegions();
                        highlightTallLegions();
                        repaint();
                        break;

                    case Constants.MOVE:
                        client.undoAllMoves();
                        highlightUnmovedLegions();
                        break;

                    case Constants.FIGHT:
                        Log.error("called undoAllAction in FIGHT");
                        break;

                    case Constants.MUSTER:
                        client.undoAllRecruits();
                        highlightPossibleRecruits();
                        break;

                    default:
                        Log.error("Bogus phase");
                }
            }
        };

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = client.getPhase();
                switch (phase)
                {
                    case Constants.SPLIT:
                        client.doneWithSplits();
                        break;

                    case Constants.MOVE:
                        client.doneWithMoves();
                        break;

                    case Constants.FIGHT:
                        client.doneWithEngagements();
                        break;

                    case Constants.MUSTER:
                        client.doneWithRecruits();
                        break;

                    default:
                        Log.error("Bogus phase");
                }
            }
        };

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
                        "Confirm Withdrawal?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[1]);

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
                new ShowAllRecruits(masterFrame,
                        TerrainRecruitLoader.getTerrains(), null, null,
                        scrollPane);
            }
        };

        viewHexRecruitTreeAction = new AbstractAction(viewHexRecruitTree)
        {
            public void actionPerformed(ActionEvent e)
            {
                GUIMasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    String[] terrains = { hex.getTerrain() };
                    new ShowAllRecruits(masterFrame, terrains, lastPoint,
                            hex.getLabel(), scrollPane);
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
                    new ShowBattleMap(masterFrame, hex.getLabel());
                    // Work around a Windows JDK 1.3 bug.
                    hex.repaint();
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
                            "Are you sure you with to start a new game?",
                            "New Game?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE, null, options,
                            options[1]);

                    if (answer != JOptionPane.YES_OPTION)
                    {
                        return;
                    }
                }
                client.newGame();
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
                    client.loadGame(chooser.getSelectedFile().getPath());
                }
            }
        };

        saveGameAction = new AbstractAction(Constants.saveGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.saveGame(null);
            }
        };

        saveGameAsAction = new AbstractAction(saveGameAs)
        {
            // XXX Need a confirmation dialog on overwrite?
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser(Constants.saveDirname);
                chooser.setFileFilter(new XMLSnapshotFilter());
                int returnVal = chooser.showSaveDialog(masterFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    String dirname =
                            chooser.getCurrentDirectory().getAbsolutePath();
                    String basename = chooser.getSelectedFile().getName();
                    // Add default savegame extension.
                    if (!basename.endsWith(Constants.xmlExtension))
                    {
                        basename += Constants.xmlExtension;
                    }
                    client.saveGame(dirname + '/' + basename);
                }
            }
        };

        quitGameAction = new AbstractAction(Constants.quit)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (client.isGameOver())
                {
                    System.exit(0);
                }

                String[] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                        "Are you sure you wish to quit?",
                        "Quit Game?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[1]);
                if (answer == JOptionPane.YES_OPTION)
                {
                    client.withdrawFromGame();
                    System.exit(0);
                }
            }
        };

        changeScaleAction = new AbstractAction(changeScale)
        {
            public void actionPerformed(ActionEvent e)
            {
                final int oldScale = Scale.get();
                final int newScale = PickIntValue.pickIntValue(masterFrame,
                        oldScale, "Pick scale", 5, 25, 1);
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
                ChooseScreen cs = new ChooseScreen(getFrame(), client);
            }
        };

        aboutAction = new AbstractAction(about)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.showMessageDialog(
                        "Colossus build: " + Client.getVersion() +
                        "\n" +
                        "user.home:      " + System.getProperty("user.home") +
                        "\n" +
                        "java.version:   " + System.getProperty("java.version"));
            }
        };
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

    private void addCheckBox(JMenu menu, String name, int mnemonic)
    {
        JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(name);
        cbmi.setMnemonic(mnemonic);
        cbmi.setSelected(client.getOption(name));

        cbmi.addItemListener(itemHandler);
        menu.add(cbmi);
        checkboxes.put(name, cbmi);
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
        mi = fileMenu.add(loadGameAction);
        mi.setMnemonic(KeyEvent.VK_L);
        mi = fileMenu.add(saveGameAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi = fileMenu.add(saveGameAsAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi = fileMenu.add(quitGameAction);
        mi.setMnemonic(KeyEvent.VK_Q);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);

        // Then per-player options

        JMenu playerMenu = new JMenu("Player");
        playerMenu.setMnemonic(KeyEvent.VK_P);
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

        addCheckBox(graphicsMenu, Options.stealFocus, KeyEvent.VK_F);
        addCheckBox(graphicsMenu, Options.showCaretaker, KeyEvent.VK_C);
        addCheckBox(graphicsMenu, Options.showStatusScreen, KeyEvent.VK_G);
        addCheckBox(graphicsMenu, Options.showLogWindow, KeyEvent.VK_L);
        addCheckBox(graphicsMenu, Options.antialias, KeyEvent.VK_N);
        addCheckBox(graphicsMenu, Options.useOverlay, KeyEvent.VK_V);
        addCheckBox(graphicsMenu, Options.noBaseColor, KeyEvent.VK_W);
        addCheckBox(graphicsMenu, Options.useColoredBorders, 0);
        addCheckBox(graphicsMenu, Options.doNotInvertDefender, 0);
        addCheckBox(graphicsMenu, Options.showAllRecruitChits, 0);
        mi = graphicsMenu.add(changeScaleAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi = graphicsMenu.add(viewFullRecruitTreeAction);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
        mi.setMnemonic(KeyEvent.VK_R);
        if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length >
                1)
        {
            mi = graphicsMenu.add(chooseScreenAction);
        }

        // Then Look & Feel
        lfMenu = new JMenu("Look & Feel");
        menuBar.add(lfMenu);
        UIManager.LookAndFeelInfo[] lfInfo =
                UIManager.getInstalledLookAndFeels();
        String currentLF = UIManager.getLookAndFeel().getName();
        for (int i = 0; i < lfInfo.length; i++)
        {
            AbstractAction lfAction =
                    new ChangeLookFeelAction(lfInfo[i].getName(),
                    lfInfo[i].getClassName());
            JCheckBoxMenuItem temp = new JCheckBoxMenuItem(lfAction);
            lfMenu.add(temp);
            temp.setState(lfInfo[i].getName().equals(currentLF));
        }

        // Then help menu
        JMenu helpMenu = new JMenu("Help");
        playerMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        mi = helpMenu.add(aboutAction);
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
        if (playerLabel == null)
        {
            playerLabel = new JLabel(playerName);
            contentPane.add(playerLabel, BorderLayout.SOUTH);
            masterFrame.pack();
        }
        else
        {
            playerLabel.setText(playerName);
        }

        String colorName = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            playerLabel.setForeground(color);
            // Don't do this again.
            playerLabelDone = true;
        }
    }

    private void setupGUIHexes()
    {
        guiHexArray = new GUIMasterHex[horizSize][vertSize];
        guiHexList = new ArrayList(plainHexList.size());

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
                    hex.init(
                            cx + 4 * i * scale,
                            (int)Math.round(cy + (3 * j + ((i + boardParity) & 1)
                            * (1 + 2 * (j / 2)) + ((i + 1 + boardParity) & 1)
                            * 2 * ((j + 1) / 2)) * Hex.SQRT3 * scale),
                            scale,
                            isHexInverted(i, j),
                            this);
                    guiHexArray[i][j] = hex;
                    guiHexList.add(hex);
                }
            }
        }
        setupNeighbors(guiHexArray);
    }

    private static boolean isHexInverted(int i, int j)
    {
        return (((i + j) & 1) == boardParity);
    }

    static MasterHex hexByLabel(MasterHex[][] h, int label)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (show[i][j])
                {
                    if (h[i][j].getLabel().equals(Integer.toString(label)))
                    {
                        return h[i][j];
                    }
                }
            }
        }
        Log.warn("Couldn't find Masterhex labeled " + label);
        return null;
    }

    private static void readMapData()
            throws Exception
    {
        java.util.List directories = VariantSupport.getVarDirectoriesList();
        InputStream mapIS = ResourceLoader.getInputStream(
                VariantSupport.getMapName(), directories);
        if (mapIS == null)
        {
            throw new FileNotFoundException(VariantSupport.getMapName());
        }
        sml = new StrategicMapLoader(mapIS);
        int[] size = sml.getHexArraySize();
        horizSize = size[0];
        vertSize = size[1];
        show = new boolean[horizSize][vertSize];
    }

    /** Add terrain types, id labels, label sides, and exits to hexes.
     *  Side effects on passed list. */
    private static synchronized void setupPlainHexes()
    {
        plainHexArray = new MasterHex[horizSize][vertSize];
        plainHexList = new ArrayList(horizSize * vertSize);
        for (int i = 0; i < show.length; i++)
        {
            for (int j = 0; j < show[i].length; j++)
            {
                show[i][j] = false;
            }
        }
        try
        {
            while (sml.oneCase(plainHexArray, plainHexList, show) >= 0)
            {
            }
        }
        catch (Exception e)
        {
            Log.error("Strategic map loading failed : " + e);
            System.exit(1);
        }

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
            Log.error("null pointer ; i=" + i + ", j=" + j + ", k=" + k);
            System.exit(1);
        }
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
                Log.warn("bad exit ; i=" + i + ", j=" + j + ", k=" + k);
            }
        }
        else if (dh.getXCoord() == (i + 1))
        {
            if (dh.getYCoord() == j)
            {
                h[i][j].setExitType(2 - ((i + j + boardParity) & 1),
                        h[i][j].getBaseExitType(k));
            }
            else
            {
                Log.warn("bad exit ; i=" + i + ", j=" + j + ", k=" + k);
            }
        }
        else if (dh.getXCoord() == (i - 1))
        {
            if (dh.getYCoord() == j)
            {
                h[i][j].setExitType(4 + ((i + j + boardParity) & 1),
                        h[i][j].getBaseExitType(k));
            }
            else
            {
                Log.warn("bad exit ; i=" + i + ", j=" + j + ", k=" + k);
            }
        }
        else
        {
            Log.warn("bad exit ; i=" + i + ", j=" + j + ", k=" + k);
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
                                    Log.error("Bogus hexside");
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
                    double deltaY = (j - midY) * (double)width / height;

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

                    if (hex.getExitType(0) != Constants.NONE ||
                            hex.getEntranceType(0) != Constants.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != Constants.NONE ||
                            hex.getEntranceType(1) != Constants.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != Constants.NONE ||
                            hex.getEntranceType(2) != Constants.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != Constants.NONE ||
                            hex.getEntranceType(3) != Constants.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != Constants.NONE ||
                            hex.getEntranceType(4) != Constants.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != Constants.NONE ||
                            hex.getEntranceType(5) != Constants.NONE)
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

        masterFrame.setTitle(activePlayerName + " Turn " +
                client.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
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

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightTallLegions();
        }
    }

    void setupMoveMenu()
    {
        unselectAllHexes();
        reqFocus();

        String activePlayerName = client.getActivePlayerName();
        masterFrame.setTitle(activePlayerName + " Turn " +
                client.getTurnNumber() + " : Movement Roll: " +
                client.getMovementRoll());

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
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
        }

        // Force showing the updated movement die.
        repaint();
    }

    void setupFightMenu()
    {
        unselectAllHexes();
        reqFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
                client.getTurnNumber() + " : Resolve Engagements ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
            JMenuItem mi;

            mi = phaseMenu.add(clearRecruitChitsAction);
            mi.setMnemonic(KeyEvent.VK_C);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(doneWithPhaseAction);
            mi.setMnemonic(KeyEvent.VK_D);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightEngagements();
        }
    }

    void setupMusterMenu()
    {
        unselectAllHexes();
        reqFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
                client.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
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

            phaseMenu.addSeparator();

            mi = phaseMenu.add(withdrawFromGameAction);
            mi.setMnemonic(KeyEvent.VK_W);

            highlightPossibleRecruits();
        }
    }

    void highlightPossibleRecruits()
    {
        unselectAllHexes();
        selectHexesByLabels(client.getPossibleRecruitHexes());
    }

    JFrame getFrame()
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
        java.util.List markerIds = client.getLegionsByHex(hexLabel);

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
        Iterator it = plainHexList.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            alignLegions(hex.getLabel());
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

        showBestRecruit(markerId, combo);
    }

    private void showBestRecruit(String markerId, Set set)
    {
        client.clearRecruitChits();
        Iterator it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            java.util.List recruits = client.findEligibleRecruits(markerId,
                    hexLabel);
            if (recruits != null && recruits.size() > 0)
            {
                if ((!client.showAllRecruitChits) ||
                        (recruits.size() == 1))
                {
                    Creature recruit =
                            (Creature)recruits.get(recruits.size() - 1);
                    String recruitName = recruit.getName();
                    client.addRecruitChit(recruitName, hexLabel);
                }
                else
                {
                    client.addRecruitChit(recruits, hexLabel);
                }
            }
        }
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
        java.util.List directories = new java.util.ArrayList();
        directories.add(Constants.defaultDirName +
                ResourceLoader.getPathSeparator() +
                Constants.imagesDirName);

        String[] iconNames = { Constants.masterboardIconImage,
            Constants.masterboardIconText +
                    "-Name-" +
                    Constants.masterboardIconTextColor,
            Constants.masterboardIconSubscript +
                    "-Subscript-" +
                    Constants.masterboardIconTextColor };

        Image image =
                ResourceLoader.getCompositeImage(iconNames,
                directories,
                60, 60);

        if (image == null)
        {
            Log.error("Couldn't find Colossus icon");
            System.exit(1);
        }
        else
        {
            masterFrame.setIconImage(image);
        }
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    public static MasterHex getHexByLabel(String label)
    {
        Iterator it = plainHexList.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }
        return null;
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    GUIMasterHex getGUIHexByLabel(String label)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }
        return null;
    }

    /** Return the MasterHex that contains the given point, or
     *  null if none does. */
    private GUIMasterHex getHexContainingPoint(Point point)
    {
        // XXX This is completely inefficient.
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.contains(point))
            {
                return hex;
            }
        }
        return null;
    }

    /** Return the topmost Marker that contains the given point, or
     *  null if none does. */
    private Marker getMarkerAtPoint(Point point)
    {
        java.util.List markers = client.getMarkers();
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
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void unselectHexByLabel(String label)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }

    void unselectHexesByLabels(Set labels)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }

    void selectHexByLabel(String label)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (!hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }

    void selectHexesByLabels(Set labels)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }

    void selectHexesByLabels(Set labels, Color color)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.setSelectColor(color);
                hex.repaint();
            }
        }
    }

    void actOnMisclick()
    {
        switch (client.getPhase())
        {
            case Constants.SPLIT:
                highlightTallLegions();
                break;

            case Constants.MOVE:
                client.clearRecruitChits();
                client.setMoverId(null);
                highlightUnmovedLegions();
                break;

            case Constants.FIGHT:
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
                break;

            case Constants.MUSTER:
                highlightPossibleRecruits();
                break;

            default:
                break;
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
        return (((modifiers & InputEvent.BUTTON2_MASK) != 0) ||
                ((modifiers & InputEvent.BUTTON3_MASK) != 0) ||
                e.isAltDown() || e.isControlDown());
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
                    new ShowLegion(masterFrame, markerId,
                            client.getLegionImageNames(markerId),
                            client.getLegionCreatureCertainties(markerId),
                            point, scrollPane);
                    return;
                }
                else if (client.isMyLegion(markerId))
                {
                    if (hex != null)
                    {
                        actOnLegion(markerId, hex.getLabel());
                    }
                    else
                    {
                        Log.warn("null hex in MasterBoard.mousePressed()");
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
                    popupMenu.setLabel(hex.getDescription());
                    popupMenu.show(e.getComponent(), point.x, point.y);
                    return;
                }

                // Otherwise, the action to take depends on the phase.
                // Only the current player can manipulate game state.
                if (client.getPlayerName().equals(
                        client.getActivePlayerName()))
                {
                    actOnHex(hex.getLabel());
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

    private void actOnLegion(String markerId, String hexLabel)
    {
        if (!client.isMyTurn())
        {
            return;
        }
        switch (client.getPhase())
        {
            case Constants.SPLIT:
                client.doSplit(markerId);
                break;

            case Constants.MOVE:
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
                break;

            case Constants.FIGHT:
                client.doFight(hexLabel);
                break;

            case Constants.MUSTER:
                client.doRecruit(markerId);
                break;
        }
    }

    private void actOnHex(String hexLabel)
    {
        switch (client.getPhase())
        {
            // If we're moving, and have selected a legion which
            // has not yet moved, and this hex is a legal
            // destination, move the legion here.
            case Constants.MOVE:
                client.clearRecruitChits();
                client.doMove(hexLabel);
                actOnMisclick();   // Yes, even if the move was good.
                break;

            // If we're fighting and there is an engagement here,
            // resolve it.  If an angel is being summoned, mark
            // the donor legion instead.
            case Constants.FIGHT:
                client.engage(hexLabel);
                break;

            default:
                break;
        }
    }

    class MasterBoardItemHandler implements ItemListener
    {
        public void itemStateChanged(ItemEvent e)
        {
            JMenuItem source = (JMenuItem)e.getSource();
            String text = source.getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            client.setOption(text, selected);
        }
    }


    class MasterBoardWindowHandler extends WindowAdapter
    {
        public void windowClosing(WindowEvent e)
        {
            // XXX This code is a duplicate of quitGameAction.  Find out
            // how to invoke that action correctly from here.
            if (client.isGameOver())
            {
                client.withdrawFromGame();
                System.exit(0);
            }

            String[] options = new String[2];
            options[0] = "Yes";
            options[1] = "No";
            int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you wish to quit?",
                    "Quit Game?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

            if (answer == JOptionPane.YES_OPTION)
            {
                client.withdrawFromGame();
                System.exit(0);
            }
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Abort if called too early.
        if (g.getClipBounds() == null)
        {
            return;
        }

        try
        {
            paintHexes(g);
            paintMarkers(g);
            paintRecruitChits(g);
            paintMovementDie(g);
        }
        catch (ConcurrentModificationException ex)
        {
            Log.debug("harmless " + ex.toString());
            // Don't worry about it -- we'll just paint again.
        }
    }

    private void paintHexes(Graphics g)
    {
        Iterator it = guiHexList.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (g.getClipBounds().intersects(hex.getBounds()))
            {
                hex.paint(g);
            }
        }
    }

    /** Paint markers in z-order. */
    private void paintMarkers(Graphics g)
    {
        Iterator it = client.getMarkers().iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker != null &&
                    g.getClipBounds().intersects(marker.getBounds()))
            {
                marker.paintComponent(g);
            }
        }
    }

    private void paintRecruitChits(Graphics g)
    {
        Iterator it = client.getRecruitChits().iterator();
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
        return new Dimension(((horizSize + 1) * 4) * scale,
                (vertSize * 7) * scale);
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
        setVisible(false);
        setEnabled(false);
        masterFrame.setVisible(false);
        masterFrame.setEnabled(false);
        masterFrame.dispose();
    }

    public static Set getTowerSet()
    {
        return Collections.unmodifiableSet(towerSet);
    }

    private static void setupTowerSet()
    {
        towerSet = new HashSet();
        Iterator it = plainHexList.iterator();
        while (it.hasNext())
        {
            Hex bh = (Hex)it.next();
            if (HexMap.terrainIsTower(bh.getTerrain()))
            {
                towerSet.add(bh.getLabel());
            }
        }
    }

    JScrollPane getScrollPane()
    {
        return scrollPane;
    }

    /** Return a set of all hex labels. */
    static Set getAllHexLabels()
    {
        Set set = new HashSet();
        Iterator it = plainHexList.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            set.add(hex.getLabel());
        }
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
}
