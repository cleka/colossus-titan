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
import net.sf.colossus.server.Options;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.SaveGameFilter;
import net.sf.colossus.parser.StrategicMapLoader;


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

    /** For easy of mapping to the GUI, hexes will be stored
     *  in a horizSize*vertSize array, with some empty elements. */
    private GUIMasterHex[][] h = null;

    /** For ease of iterating through all hexes, they'll also be
     *  stored in a List. */
    private java.util.List hexes = null;

    /** A static set of non-GUI MasterHexes */
    private static MasterHex[][] plain = null;

    /** For ease of iterating through all hexes, they'll also be
     *  stored in a List. */
    private static java.util.List plainHexes = null;

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

    private Container contentPane;
    private JLabel playerLabel;

    public static final String newGame = "New game";
    public static final String loadGame = "Load game";
    public static final String saveGame = "Save game";
    public static final String saveGameAs = "Save game as";
    public static final String quitGame = "Quit game";
    public static final String saveOptions = "Save options";

    public static final String undoLast = "Undo";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";

    public static final String takeMulligan = "Take Mulligan";
    public static final String concedeBattle = "Concede battle";
    public static final String withdrawFromGame = "Withdraw from Game";

    public static final String viewRecruitInfo = "View Recruit Info";
    public static final String viewBattleMap = "View Battle Map";
    public static final String changeScale = "Change Scale";
    public static final String changeAIDelay = "Change AI Delay";

    public static final String about = "About";

    private static String mapName = GetPlayers.getMapName();

    private AbstractAction newGameAction;
    private AbstractAction loadGameAction;
    private AbstractAction saveGameAction;
    private AbstractAction saveGameAsAction;
    private AbstractAction quitGameAction;
    private AbstractAction saveOptionsAction;

    private AbstractAction undoLastAction;
    private AbstractAction undoAllAction;
    private AbstractAction doneWithPhaseAction;

    private AbstractAction takeMulliganAction;
    private AbstractAction withdrawFromGameAction;

    private AbstractAction viewRecruitInfoAction;
    private AbstractAction viewBattleMapAction;
    private AbstractAction changeScaleAction;
    private AbstractAction changeAIDelayAction;

    private AbstractAction aboutAction;

    /* a Set of label (String) of all Tower hex */
    private static Set towerSet;

    boolean playerLabelDone;


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

        setupHexes();

        setupActions();
        setupPopupMenu();
        setupTopMenu();
        contentPane.add(new JScrollPane(this), BorderLayout.CENTER);

        setupPlayerLabel();

        masterFrame.pack();
        masterFrame.setVisible(true);
    }


    // No-arg constructor for testing and AICopy(), without GUI stuff.
    MasterBoard()
    {
        setupHexes();
    }


    MasterBoard AICopy()
    {
        MasterBoard newBoard = new MasterBoard();
        newBoard.setupActions();
        return newBoard;
    }


    private void setupActions()
    {
        undoLastAction = new AbstractAction(undoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = client.getPhase();
                switch(phase)
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
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = client.getPhase();
                switch(phase)
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
                }
            }
        };


        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                int phase = client.getPhase();
                switch(phase)
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
                }
            }
        };


        takeMulliganAction = new AbstractAction(takeMulligan)
        {
            public void actionPerformed(ActionEvent e)
            {
                if (client.mulligan() == -1)
                {
                    Log.error("Illegal mulligan.");
                }
            }
        };


        withdrawFromGameAction = new AbstractAction(withdrawFromGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
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

        viewRecruitInfoAction = new AbstractAction(viewRecruitInfo)
        {
            public void actionPerformed(ActionEvent e)
            {
                new ShowAllRecruits(masterFrame, client);
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

        newGameAction = new AbstractAction(newGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you with to start a new game?",
                    "New Game?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    client.newGame();
                }
            }
        };

        loadGameAction = new AbstractAction(loadGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you with to load another game?",
                    "Load Game?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    JFileChooser chooser = new JFileChooser(
                        Constants.saveDirname);
                    chooser.setFileFilter(new SaveGameFilter());
                    int returnVal = chooser.showOpenDialog(masterFrame);
                    if (returnVal == JFileChooser.APPROVE_OPTION)
                    {
                        client.loadGame(chooser.getSelectedFile().getName());
                    }
                }
            }
        };

        saveGameAction = new AbstractAction(saveGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                // XXX Offer the ability to re-use a save filename,
                // rather than always making a new one?
                client.saveGame();
            }
        };

        saveGameAsAction = new AbstractAction(saveGameAs)
        {
            // XXX Need a confirmation dialog on overwrite?
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser chooser = new JFileChooser(Constants.saveDirname);
                chooser.setFileFilter(new SaveGameFilter());
                int returnVal = chooser.showSaveDialog(masterFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    String dirname = chooser.getCurrentDirectory().getName();
                    String basename = chooser.getSelectedFile().getName();
                    StringBuffer path = new StringBuffer();
                    path.append(dirname);
                    path.append(File.separator);
                    path.append(basename);
                    // Add default savegame extension.
                    if (!path.toString().endsWith(Constants.saveExtension))
                    {
                        path.append(Constants.saveExtension);
                    }
                    client.saveGame(path.toString());
                }
            }
        };

        saveOptionsAction = new AbstractAction(saveOptions)
        {
            public void actionPerformed(ActionEvent e)
            {
                client.saveOptions();
            }
        };

        quitGameAction = new AbstractAction(quitGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(masterFrame,
                    "Are you sure you wish to quit?",
                    "Quit Game?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    System.exit(0);
                }
            }
        };

        changeScaleAction = new AbstractAction(changeScale)
        {
            public void actionPerformed(ActionEvent e)
            {
                final int oldScale = Scale.get();
                final int newScale = PickScale.pickScale(masterFrame,
                    oldScale);
                if (newScale != oldScale && newScale != -1)
                {
                    client.setStringOption(Options.scale, 
                        new Integer(newScale).toString());
                    Scale.set(newScale);
                    client.rescaleAllWindows();
                }
            }
        };

        changeAIDelayAction = new AbstractAction(changeAIDelay)
        {
            public void actionPerformed(ActionEvent e)
            {
                PickDelay.pickDelay(masterFrame, client, 
                    client.getIntOption(Options.aiDelay));
            }
        };

        aboutAction = new AbstractAction(about)
        {
            public void actionPerformed(ActionEvent e)
            {
                byte [] bytes = new byte[8];  // length of an ISO date
                String version = "unknown";
                try
                {
                    ClassLoader cl = Client.class.getClassLoader();
                    InputStream is = cl.getResourceAsStream("version");
                    is.read(bytes);
                    version = new String(bytes, 0, bytes.length); 
                }
                catch (Exception ex)
                {
                    Log.debug("Problem reading version file " + ex);
                }

                client.showMessageDialog("Colossus build: " + version);
            }
        };
    }


    private void setupPopupMenu()
    {
        popupMenu = new JPopupMenu();
        contentPane.add(popupMenu);

        JMenuItem mi = popupMenu.add(viewRecruitInfoAction);
        mi.setMnemonic(KeyEvent.VK_R);

        mi = popupMenu.add(viewBattleMapAction);
        mi.setMnemonic(KeyEvent.VK_B);
    }

    private ItemListener m_oItemListener = new MasterBoardItemHandler();

    private void addCheckBox(JMenu menu, String name, int mnemonic)
    {
        JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem(name);
        cbmi.setMnemonic(mnemonic);
        cbmi.setSelected(client.getOption(name));

        cbmi.addItemListener(m_oItemListener);
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

        fileMenu.addSeparator();
        mi = fileMenu.add(saveOptionsAction);
        mi.setMnemonic(KeyEvent.VK_O);

        // Phase menu items change by phase and will be set up later.
        phaseMenu = new JMenu("Phase");
        phaseMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(phaseMenu);

        // Game-wide options first

        JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);
        menuBar.add(gameMenu);

        addCheckBox(gameMenu, Options.autosave, KeyEvent.VK_A);
        addCheckBox(gameMenu, Options.allStacksVisible, KeyEvent.VK_S);
        addCheckBox(gameMenu, Options.logDebug, KeyEvent.VK_L);
        mi = gameMenu.add(changeAIDelayAction);
        mi.setMnemonic(KeyEvent.VK_D);

        // Then per-player options

        JMenu playerMenu = new JMenu("Player");
        playerMenu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(playerMenu);

        addCheckBox(playerMenu, Options.autoPickColor, KeyEvent.VK_C);
        addCheckBox(playerMenu, Options.autoPickMarker, KeyEvent.VK_I);
        addCheckBox(playerMenu, Options.autoSplit, KeyEvent.VK_S);
        addCheckBox(playerMenu, Options.autoMasterMove, KeyEvent.VK_M);
        addCheckBox(playerMenu, Options.autoPickEntrySide, KeyEvent.VK_E);
        addCheckBox(playerMenu, Options.autoFlee, KeyEvent.VK_F);
        addCheckBox(playerMenu, Options.autoPickEngagement, KeyEvent.VK_N);
        addCheckBox(playerMenu, Options.autoBattleMove, KeyEvent.VK_B);
        addCheckBox(playerMenu, Options.autoForcedStrike, KeyEvent.VK_K);
        addCheckBox(playerMenu, Options.autoCarrySingle, KeyEvent.VK_Y);
        addCheckBox(playerMenu, Options.autoRangeSingle, KeyEvent.VK_G);
        addCheckBox(playerMenu, Options.autoStrike, KeyEvent.VK_T);
        addCheckBox(playerMenu, Options.autoSummonAngels, KeyEvent.VK_O);
        addCheckBox(playerMenu, Options.autoAcquireAngels, KeyEvent.VK_A);
        addCheckBox(playerMenu, Options.autoRecruit, KeyEvent.VK_R);
        addCheckBox(playerMenu, Options.autoPickRecruiter, KeyEvent.VK_U);
        playerMenu.addSeparator();
        addCheckBox(playerMenu, Options.autoPlay, KeyEvent.VK_P);

        // Then per-client GUI options
        JMenu graphicsMenu = new JMenu("Graphics");
        graphicsMenu.setMnemonic(KeyEvent.VK_G);
        menuBar.add(graphicsMenu);

        addCheckBox(graphicsMenu, Options.showCaretaker, KeyEvent.VK_C);
        addCheckBox(graphicsMenu, Options.showStatusScreen, KeyEvent.VK_G);
        addCheckBox(graphicsMenu, Options.showLogWindow, KeyEvent.VK_L);
        addCheckBox(graphicsMenu, Options.antialias, KeyEvent.VK_N);
        addCheckBox(graphicsMenu, Options.useOverlay, KeyEvent.VK_V);
        mi = graphicsMenu.add(changeScaleAction);
        mi.setMnemonic(KeyEvent.VK_S);

        // Then help menu
        JMenu helpMenu = new JMenu("Help");
        playerMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        mi = helpMenu.add(aboutAction);
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


    private void setupHexes()
    {
        if (plain == null) /* if static array not yet defined */
            setupHexesGameState(false);
        setupHexesGameState(true);
        setupHexesGUI();
        if (towerSet == null) /* if static Set not yet defined */
            setupTowerSet();
    }

    private void setupHexesGUI()
    {
        // There are a total of 96 hexes
        // Their Titan labels are:
        // Middle ring: 1-42
        // Outer ring: 101-142
        // Towers: 100, 200, 300, 400, 500, 600
        // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

        // For easy of mapping to the GUI, they'll initially be stored
        // in a horizSize*vertSize array, with some empty elements.
        // For ease of iterating through all hexes, they'll then be
        // stored in a List.

        int scale = Scale.get();

        int cx = 3 * scale;
        int cy = 0 * scale;

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIMasterHex hex;
                    hex = h[i][j];
                    hex.init(cx + 4 * i * scale,
                        (int) Math.round(cy + (3 * j + (i & 1) *
                            (1 + 2 * (j / 2)) + ((i + 1) & 1) * 2 *
                            ((j + 1) / 2)) * Hex.SQRT3 * scale),
                        scale,
                        isHexInverted(i, j),
                        this);

                    hex.setXCoord(i);
                    hex.setYCoord(j);
                }
            }
        }
    }


    private static boolean isHexInverted(int i, int j)
    {
        return (((i + j) & 1) == 0);
    }

    static MasterHex hexByLabel(MasterHex [][] h, int label)
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


    /** This method only needs to be run once, since the attributes it
     *  sets up are constant for the game. */
    private void setupHexesGameState(boolean isGUI)
    {
        // Add terrain types, id labels, label sides, and exits to hexes.
        MasterHex[][] localH = null;
        try
        {
            ClassLoader cl = Client.class.getClassLoader();
            InputStream mapIS = 
                cl.getResourceAsStream(mapName);
            if (mapIS == null)
            {
                mapIS = new FileInputStream(mapName);
            }
            if (mapIS == null) 
            {
                throw new FileNotFoundException(mapName);
            }
            StrategicMapLoader sml = new StrategicMapLoader(mapIS);
            sml.StrategicMapLoaderInit();
            {
                int[] size = sml.getHexArraySize();
                if ((horizSize == 0) && (vertSize == 0))
                {
                    horizSize = size[0];
                    vertSize = size[1];
                }
                else
                {
                    if ((horizSize != size[0]) ||
                        (vertSize != size[1]))
                        throw new Exception("Map size changed during game !");
                }
            }
            if (show == null)
                show = new boolean[horizSize][vertSize];
            if (!isGUI) /* we're filling the plain hexes */
            {
                plain = new MasterHex[horizSize][vertSize];
                plainHexes = new ArrayList(horizSize * vertSize);
                plainHexes.clear();
                for (int i = 0; i < show.length; i++)
                {
                    for (int j = 0; j < show[i].length; j++)
                    {
                        show[i][j] = false;
                    }
                }
                while (sml.oneCase(plain, plainHexes, show, false) >= 0) {}
                localH = plain;
            }
            else /* we're filling the GUI hexes */
            {
                h = new GUIMasterHex[horizSize][vertSize];
                hexes = new ArrayList(horizSize * vertSize);
                hexes.clear();
                for (int i = 0; i < show.length; i++)
                {
                    for (int j = 0; j < show[i].length; j++)
                    {
                        show[i][j] = false;
                    }
                }
                while (sml.oneCase(h, hexes, show, true) >= 0) {} 
                localH = h;
            }
        }        catch (Exception e) 
        {
            Log.error("Strategic map loading failed : " + e);
            System.exit(1);
        }

        setupExits(localH);
        setupEntrances(localH);
        setupHexLabelSides(localH);
        setupNeighbors(localH);
    }

    private static void setupExits(MasterHex [][] h)
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

    private static void setupOneExit(MasterHex [][] h, int i, int j, int k)
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
                h[i][j].setExitType(2 - ((i + j) & 1),
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
                h[i][j].setExitType(4 + ((i + j) & 1), 
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

    private static void setupEntrances(MasterHex [][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
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
    private static void setupHexLabelSides(MasterHex [][] h)
    {
        // First find the center of the board.
        int width = h.length;
        int height = h[0].length;

        // Subtract 1 to account for 1-based length of 0-based array.
        double midX = (width - 1) / 2.0;
        double midY = (height - 1) / 2.0;

        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
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

    private static void setupNeighbors(MasterHex [][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
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
        requestFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
            JMenuItem mi;
    
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
        requestFocus();

        String activePlayerName = client.getActivePlayerName();
        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Movement Roll: " +
            client.getMovementRoll());

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
            JMenuItem mi;
    
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
    }


    void setupFightMenu()
    {
        unselectAllHexes();
        requestFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Resolve Engagements ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
            JMenuItem mi;

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
        requestFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

        if (client.getPlayerName().equals(activePlayerName))
        {
            JMenuItem mi;
    
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
        java.util.List markerIds = client.getLegionMarkerIds(hexLabel);

        // Put the current player's legions first.
        Collections.sort(markerIds, MarkerComparator.getMarkerComparator(
            client.getShortColor()));

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
        Iterator it = plainHexes.iterator();
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
        selectHexesByLabels(client.findAllUnmovedLegionHexes());
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
                Creature recruit = (Creature)recruits.get(recruits.size() - 1);
                String recruitName = recruit.getName();
                client.addRecruitChit(recruitName, hexLabel);
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
        Set set = client.findSummonableAngels(markerId);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }


    void highlightPossibleRecruits()
    {
        unselectAllHexes();
        selectHexesByLabels(client.findAllEligibleRecruitHexes());
    }


    private void setupIcon()
    {
        java.util.List directories = new java.util.ArrayList();
        directories.add(Constants.imageDirName);
        
        Image image = ResourceLoader.getImage("Colossus", directories);
        
        if (image == null)
        {
            Log.error("ERROR: Couldn't find Colossus icon");
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
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }
        Log.error("Could not find hex " + label);
        return null;
    }

    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    GUIMasterHex getGUIHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.getLabel().equals(label))
            {
                return hex;
            }
        }
        Log.error("Could not find hex " + label);
        return null;
    }


    /** Return the MasterHex that contains the given point, or
     *  null if none does. */
    private GUIMasterHex getHexContainingPoint(Point point)
    {
        // XXX This is completely inefficient.
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
        Iterator it = hexes.iterator();
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
                    new ShowLegion(masterFrame,
                        client.getLongMarkerName(markerId),
                        client.getLegionImageNames(markerId), point);
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
        switch (client.getPhase())
        {
            case Constants.SPLIT:
                client.doSplit(markerId);
                break;

            case Constants.MOVE:
                client.setMoverId(markerId);
                getGUIHexByLabel(hexLabel).repaint();
                highlightMoves(markerId);
                break;

            case Constants.FIGHT:
                client.doFight(hexLabel);
                break;

            case Constants.MUSTER:
                client.doMuster(markerId);
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
            String [] options = new String[2];
            options[0] = "Yes";
            options[1] = "No";
            int answer = JOptionPane.showOptionDialog(masterFrame,
               "Are you sure you wish to quit?",
               "Quit Game?",
               JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
               null, options, options[1]);

            if (answer == JOptionPane.YES_OPTION)
            {
                System.exit(0);
            }
        }
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Rectangle rectClip = g.getClipBounds();

        // Abort if called too early.
        if (rectClip == null)
        {
            return;
        }

        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (rectClip.intersects(hex.getBounds()))
            {
                hex.paint(g);
            }
        }

        // Paint markers in z-order.
        it = client.getMarkers().iterator();
        while (it.hasNext())
        {
            Marker marker = (Marker)it.next();
            if (marker != null && rectClip.intersects(marker.getBounds()))
            {
                marker.paintComponent(g);
            }
        }

        // Paint recruitChits
        it = client.getRecruitChits().iterator();
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            if (chit != null && rectClip.intersects(chit.getBounds()))
            {
                chit.paintComponent(g);
            }
        }

        // Paint MovementDie
        if (client != null)
        {
            MovementDie die = client.getMovementDie();
            if (die != null)
            {
                die.setLocation(0, 0);
                die.paintComponent(g);
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
        setupHexes();
        client.recreateMarkers();
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
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            Hex bh = (Hex)it.next();
            if (bh.getTerrain() == 'T')
                towerSet.add(bh.getLabel());
        }
    }
}
