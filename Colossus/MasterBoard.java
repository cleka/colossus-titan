import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import java.io.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 */

public final class MasterBoard extends JPanel
{
    /** For easy of mapping to the GUI, hexes will be stored
     *  in a 15x8 array, with some empty elements. */
    private GUIMasterHex[][] h = new GUIMasterHex[15][8];

    /** For ease of iterating through all hexes, they'll also be
     *  stored in an ArrayList. */
    private ArrayList hexes = new ArrayList(96);

    /** A static set of non-GUI MasterHexes */
    private static MasterHex[][] plain = new MasterHex[15][8];

    /** For ease of iterating through all hexes, they'll also be
     *  stored in an ArrayList. */
    private static ArrayList plainHexes = new ArrayList(96);

    /** The hexes in the 15x8 array that actually exist are
     *  represented by true. */
    private static final boolean[][] show =
    {
        {false, false, false, true, true, false, false, false},
        {false, false, true, true, true, true, false, false},
        {false, true, true, true, true, true, true, false},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {true, true, true, true, true, true, true, true},
        {false, true, true, true, true, true, true, false},
        {false, false, true, true, true, true, false, false},
        {false, false, false, true, true, false, false, false}
    };

    private Client client;

    private JFrame masterFrame;
    private JMenu phaseMenu;
    private JPopupMenu popupMenu;
    private HashMap checkboxes = new HashMap();

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


    static
    {
        plainHexes.clear();

        // Initialize plain hexes.
        for (int i = 0; i < plain.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHex hex = new MasterHex();
                    plain[i][j] = hex;
                    plainHexes.add(hex);
                }
            }
        }

        setupHexesGameState(plain);
    }


    public MasterBoard(Client client)
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
        contentPane.add(playerLabel, BorderLayout.SOUTH);

        masterFrame.pack();
        masterFrame.setVisible(true);
    }


    // No-arg constructor for testing and AICopy(), without GUI stuff.
    public MasterBoard()
    {
        setupHexes();
    }


    public MasterBoard AICopy(Game game)
    {
        MasterBoard newBoard = new MasterBoard();
        newBoard.setupActions();
        return newBoard;
    }


    public void setClient(Client client)
    {
        this.client = client;
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
                    case Game.SPLIT:
                        client.undoLastSplit();
                        alignAllLegions();
                        highlightTallLegions();
                        repaint();
                        break;

                    case Game.MOVE:
                        client.undoLastMove();
                        highlightUnmovedLegions();
                        break;

                    case Game.FIGHT:
                        Log.error("called undoLastAction in FIGHT");
                        break;

                    case Game.MUSTER:
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
                    case Game.SPLIT:
                        client.undoAllSplits();
                        alignAllLegions();
                        highlightTallLegions();
                        repaint();
                        break;

                    case Game.MOVE:
                        client.undoAllMoves();
                        highlightUnmovedLegions();
                        break;

                    case Game.FIGHT:
                        Log.error("called undoAllAction in FIGHT");
                        break;

                    case Game.MUSTER:
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
                    case Game.SPLIT:
                        client.doneWithSplits();
                        break;

                    case Game.MOVE:
                        client.doneWithMoves();
                        break;

                    case Game.FIGHT:
                        client.doneWithEngagements();
                        break;

                    case Game.MUSTER:
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
                GUIMasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    // XXX Remove direct caretaker reference.
                    new ShowMasterHex(masterFrame, client.getCaretaker(), 
                        hex, lastPoint);
                    // Work around a Windows JDK 1.3 bug.
                    hex.repaint();
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
                    JFileChooser chooser = new JFileChooser(Game.saveDirname);
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
                JFileChooser chooser = new JFileChooser(Game.saveDirname);
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
                    if (!path.toString().endsWith(Game.saveExtension))
                    {
                        path.append(Game.saveExtension);
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

        // Its a little wasteful to create a separate object
        // but a little cleaner to have a separate inner class
        // ditto for mouse and window listeners, where we saved
        // LOC by extending adapter
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
        // XXX We need one save options action for each options menu,
        // as well as a global one that saves them all.
        // Ditto for load options.
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
        addCheckBox(graphicsMenu, Options.antialias, KeyEvent.VK_N);
        mi = graphicsMenu.add(changeScaleAction);
        mi.setMnemonic(KeyEvent.VK_S);
    }


    public void twiddleOption(String name, boolean enable)
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
    public void setupPlayerLabel()
    {
        String playerName = client.getPlayerName();
        if (playerLabel == null)
        {
            playerLabel = new JLabel(playerName);
        }

        String colorName = client.getColor();
        // If we call this before player colors are chosen, just use
        // the defaults.
        if (colorName != null)
        {
            Color color = PickColor.getBackgroundColor(colorName);
            playerLabel.setForeground(color);
        }
        playerLabel.repaint();
    }


    private void setupHexes()
    {
        setupHexesGUI();
        setupHexesGameState(h);
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
        // in a 15x8 array, with some empty elements.
        // For ease of iterating through all hexes, they'll then be
        // stored in an ArrayList.

        int scale = Scale.get();

        int cx = 3 * scale;
        int cy = 0 * scale;

        hexes.clear();

        // Initialize hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[0].length; j++)
            {
                if (show[i][j])
                {
                    GUIMasterHex hex = new GUIMasterHex();
                    hex.init(cx + 4 * i * scale,
                        (int) Math.round(cy + (3 * j + (i & 1) *
                            (1 + 2 * (j / 2)) + ((i + 1) & 1) * 2 *
                            ((j + 1) / 2)) * Hex.SQRT3 * scale),
                        scale,
                        ((i + j) & 1) == 0,
                        this);

                    hex.setXCoord(i);
                    hex.setYCoord(j);

                    h[i][j] = hex;
                    hexes.add(hex);
                }
            }
        }
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
    private static void setupHexesGameState(MasterHex [][] h)
    {
        // Add terrain types, id labels, label sides, and exits to hexes.

        try 
        {
            ClassLoader cl = Game.class.getClassLoader();
            InputStream mapIS = 
                cl.getResourceAsStream(mapName);
            if (mapIS == null)
            {
                mapIS = new FileInputStream(mapName);
            }
            if (mapIS == null) 
            {
                Log.error("Strategic map loading failed for map " +
                    mapName);
                System.exit(1);
            }
            StrategicMapLoader sml = new StrategicMapLoader(mapIS);
            sml.StrategicMapLoaderInit();
            while (sml.oneCase(h) >= 0) {}
        } 
        catch (Exception e) 
        {
            Log.error("Strategic map loading failed : " + e);
            System.exit(1);
        }
        
        for (int i = 0; i < h.length; i++) 
        {
            for (int j = 0; j < h[i].length; j++) 
            {
                if (show[i][j]) 
                {
                    for (int k = 0; k < 3; k++) 
                    {
                        if (h[i][j].getBaseExitType(k) != MasterHex.NONE) 
                        {
                            MasterHex dh = hexByLabel(h, 
                                h[i][j].getBaseExitLabel(k));
                            if (dh == null) 
                            {
                                Log.error("null pointer ; i="+i+", j="+j+
                                    ", k="+k);
                                System.exit(1);
                            }
                            if (dh.getXCoord() == i) 
                            {
                                if (dh.getYCoord() == (j - 1)) 
                                {
                                    h[i][j].setExitType(0, 
                                        h[i][j].getBaseExitType(k));
                                } 
                                else if (dh.getYCoord() == (j + 1)) 
                                {
                                    h[i][j].setExitType(3, 
                                        h[i][j].getBaseExitType(k));
                                } 
                                else 
                                {
                                    Log.warn("bad exit ; i="+i+", j="+j+ 
                                        ", k="+k);
                                }
                            } 
                            else if (dh.getXCoord() == (i + 1)) 
                            {
                                if (dh.getYCoord() == j) 
                                {
                                    h[i][j].setExitType(2 - ((i + j) % 2), 
                                        h[i][j].getBaseExitType(k));
                                } 
                                else 
                                {
                                    Log.warn("bad exit ; i="+i+", j="+j+ 
                                        ", k="+k);
                                }
                            } 
                            else if (dh.getXCoord() == (i - 1)) 
                            {
                                if (dh.getYCoord() == j) 
                                {
                                    h[i][j].setExitType(4 + ((i + j) % 2), 
                                        h[i][j].getBaseExitType(k));
                                } 
                                else 
                                {
                                    Log.warn("bad exit ; i="+i+", j="+j+ 
                                        ", k="+k);
                                }
                            } 
                            else 
                            {
                                Log.warn("bad exit ; i="+i+", j="+j+ 
                                    ", k="+k);
                            }
                        }
                    }
                }
            }
        }
        // Couldn' find better way (yet)
        h[0][3].setLabelSide(2);
        h[0][4].setLabelSide(1);
        h[1][2].setLabelSide(2);
        h[1][3].setLabelSide(5);
        h[1][4].setLabelSide(4);
        h[1][5].setLabelSide(1);
        h[2][1].setLabelSide(2);
        h[2][2].setLabelSide(5);
        h[2][3].setLabelSide(2);
        h[2][4].setLabelSide(1);
        h[2][5].setLabelSide(4);
        h[2][6].setLabelSide(1);
        h[3][0].setLabelSide(2);
        h[3][1].setLabelSide(5);
        h[3][2].setLabelSide(2);
        h[3][3].setLabelSide(5);
        h[3][4].setLabelSide(4);
        h[3][5].setLabelSide(1);
        h[3][6].setLabelSide(4);
        h[3][7].setLabelSide(1);
        h[4][0].setLabelSide(3);
        h[4][1].setLabelSide(2);
        h[4][2].setLabelSide(5);
        h[4][3].setLabelSide(2);
        h[4][4].setLabelSide(1);
        h[4][5].setLabelSide(4);
        h[4][6].setLabelSide(1);
        h[4][7].setLabelSide(0);
        h[5][0].setLabelSide(0);
        h[5][1].setLabelSide(3);
        h[5][2].setLabelSide(2);
        h[5][3].setLabelSide(5);
        h[5][4].setLabelSide(4);
        h[5][5].setLabelSide(1);
        h[5][6].setLabelSide(0);
        h[5][7].setLabelSide(3);
        h[6][0].setLabelSide(3);
        h[6][1].setLabelSide(0);
        h[6][2].setLabelSide(3);
        h[6][3].setLabelSide(2);
        h[6][4].setLabelSide(1);
        h[6][5].setLabelSide(0);
        h[6][6].setLabelSide(3);
        h[6][7].setLabelSide(0);
        h[7][0].setLabelSide(0);
        h[7][1].setLabelSide(3);
        h[7][2].setLabelSide(0);
        h[7][3].setLabelSide(3);
        h[7][4].setLabelSide(0);
        h[7][5].setLabelSide(3);
        h[7][6].setLabelSide(0);
        h[7][7].setLabelSide(3);
        h[8][0].setLabelSide(3);
        h[8][1].setLabelSide(0);
        h[8][2].setLabelSide(3);
        h[8][3].setLabelSide(4);
        h[8][4].setLabelSide(5);
        h[8][5].setLabelSide(0);
        h[8][6].setLabelSide(3);
        h[8][7].setLabelSide(0);
        h[9][0].setLabelSide(0);
        h[9][1].setLabelSide(3);
        h[9][2].setLabelSide(4);
        h[9][3].setLabelSide(1);
        h[9][4].setLabelSide(2);
        h[9][5].setLabelSide(5);
        h[9][6].setLabelSide(0);
        h[9][7].setLabelSide(3);
        h[10][0].setLabelSide(3);
        h[10][1].setLabelSide(4);
        h[10][2].setLabelSide(1);
        h[10][3].setLabelSide(4);
        h[10][4].setLabelSide(5);
        h[10][5].setLabelSide(2);
        h[10][6].setLabelSide(5);
        h[10][7].setLabelSide(0);
        h[11][0].setLabelSide(4);
        h[11][1].setLabelSide(1);
        h[11][2].setLabelSide(4);
        h[11][3].setLabelSide(1);
        h[11][4].setLabelSide(2);
        h[11][5].setLabelSide(5);
        h[11][6].setLabelSide(2);
        h[11][7].setLabelSide(5);
        h[12][1].setLabelSide(4);
        h[12][2].setLabelSide(1);
        h[12][3].setLabelSide(4);
        h[12][4].setLabelSide(5);
        h[12][5].setLabelSide(2);
        h[12][6].setLabelSide(5);
        h[13][2].setLabelSide(4);
        h[13][3].setLabelSide(1);
        h[13][4].setLabelSide(2);
        h[13][5].setLabelSide(5);
        h[14][3].setLabelSide(4);
        h[14][4].setLabelSide(5);

        // Derive entrances from exits.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    for (int k = 0; k < 6; k++)
                    {
                        int gateType = h[i][j].getExitType(k);
                        if (gateType != MasterHex.NONE)
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

        // Add references to neighbor hexes.
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < plain[0].length; j++)
            {
                if (show[i][j])
                {
                    MasterHex hex = h[i][j];

                    if (hex.getExitType(0) != MasterHex.NONE ||
                        hex.getEntranceType(0) != MasterHex.NONE)
                    {
                        hex.setNeighbor(0, h[i][j - 1]);
                    }
                    if (hex.getExitType(1) != MasterHex.NONE ||
                        hex.getEntranceType(1) != MasterHex.NONE)
                    {
                        hex.setNeighbor(1, h[i + 1][j]);
                    }
                    if (hex.getExitType(2) != MasterHex.NONE ||
                        hex.getEntranceType(2) != MasterHex.NONE)
                    {
                        hex.setNeighbor(2, h[i + 1][j]);
                    }
                    if (hex.getExitType(3) != MasterHex.NONE ||
                        hex.getEntranceType(3) != MasterHex.NONE)
                    {
                        hex.setNeighbor(3, h[i][j + 1]);
                    }
                    if (hex.getExitType(4) != MasterHex.NONE ||
                        hex.getEntranceType(4) != MasterHex.NONE)
                    {
                        hex.setNeighbor(4, h[i - 1][j]);
                    }
                    if (hex.getExitType(5) != MasterHex.NONE ||
                        hex.getEntranceType(5) != MasterHex.NONE)
                    {
                        hex.setNeighbor(5, h[i - 1][j]);
                    }
                }
            }
        }
    }


    public void setupSplitMenu()
    {
        unselectAllHexes();
        requestFocus();

        Player player = client.getActivePlayer();

        masterFrame.setTitle(player.getName() + " Turn " +
            client.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

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

        if (client.getPlayerName().equals(player.getName()))
        {
            highlightTallLegions();
        }
    }


    public void setupMoveMenu()
    {
        unselectAllHexes();
        requestFocus();

        Player player = client.getActivePlayer();
        masterFrame.setTitle(player.getName() + " Turn " +
            client.getTurnNumber() + " : Movement Roll: " +
            player.getMovementRoll());

        phaseMenu.removeAll();

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

        if (player.getMulligansLeft() > 0)
        {
            phaseMenu.addSeparator();
            mi = phaseMenu.add(takeMulliganAction);
            mi.setMnemonic(KeyEvent.VK_M);
            mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
        }

        phaseMenu.addSeparator();

        mi = phaseMenu.add(withdrawFromGameAction);
        mi.setMnemonic(KeyEvent.VK_W);

        if (client.getPlayerName().equals(player.getName()))
        {
            highlightUnmovedLegions();
        }
    }


    public void setupFightMenu()
    {
        unselectAllHexes();
        requestFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Resolve Engagements ");

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(doneWithPhaseAction);
        mi.setMnemonic(KeyEvent.VK_D);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0));

        phaseMenu.addSeparator();

        mi = phaseMenu.add(withdrawFromGameAction);
        mi.setMnemonic(KeyEvent.VK_W);

        if (client.getPlayerName().equals(activePlayerName))
        {
            highlightEngagements();
        }
    }


    public void setupMusterMenu()
    {
        unselectAllHexes();
        requestFocus();

        String activePlayerName = client.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            client.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

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

        if (client.getPlayerName().equals(activePlayerName))
        {
            highlightPossibleRecruits();
        }
    }


    public JFrame getFrame()
    {
        return masterFrame;
    }


    /** Create markers for all existing legions. */
    public void loadInitialMarkerImages()
    {
        Iterator it = client.getAllLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            client.addMarker(markerId);
        }
        alignAllLegions();
        masterFrame.setVisible(true);
        repaint();
    }


    public void alignLegions(String hexLabel)
    {
        GUIMasterHex hex = getGUIHexByLabel(hexLabel);
        if (hex == null)
        {
            return;
        }
        ArrayList markerIds = client.getLegionMarkerIds(hexLabel);
        Player player = client.getActivePlayer();
        if (player == null)
        {
            return;
        }

        // Put the current player's legions first.
        Collections.sort(markerIds, player.getMarkerComparator());

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

    public void alignLegions(Set hexLabels)
    {
        Iterator it = hexLabels.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            alignLegions(hexLabel);
        }
    }

    /** This is incredibly inefficient. */
    public void alignAllLegions()
    {
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            alignLegions(hex.getLabel());
        }
    }


    private void highlightTallLegions()
    {
        selectHexesByLabels(client.findTallLegionHexes());
    }

    public void highlightUnmovedLegions()
    {
        unselectAllHexes();
        Player player = client.getActivePlayer();
        selectHexesByLabels(client.findAllUnmovedLegionHexes());
        repaint();
    }


    /** Select hexes where this legion can move. */
    private void highlightMoves(String markerId)
    {
        Set set = client.listMoves(markerId);
        unselectAllHexes();
        selectHexesByLabels(set);
        showBestRecruit(markerId, set);
    }


    private void showBestRecruit(String markerId, Set set)
    {
        client.clearRecruitChits();
        Iterator it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            ArrayList recruits = client.findEligibleRecruits(markerId, 
                hexLabel);
            if (recruits != null && recruits.size() > 0)
            {
                Creature recruit = (Creature)recruits.get(recruits.size() - 1);
                client.addRecruitChit(recruit.getImageName(), hexLabel);
            }
        }
    }


    /** Return number of engagements found. */
    public void highlightEngagements()
    {
        Set set = client.findEngagements();
        unselectAllHexes();
        selectHexesByLabels(set);
    }


    /** Return number of legions with summonable angels. */
    public int highlightSummonableAngels(String markerId)
    {
        Set set = client.findSummonableAngels(markerId);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }


    private void highlightPossibleRecruits()
    {
        selectHexesByLabels(client.findAllEligibleRecruitHexes());
    }


    private void setupIcon()
    {
        try
        {
            masterFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(Chit.getImagePath(
                Creature.getCreatureByName("Colossus").getImageName()))));
        }
        catch (NullPointerException e)
        {
            Log.error(e.toString() + " Couldn't find " +
                Creature.getCreatureByName("Colossus").getImageName());
            System.exit(1);
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
    public GUIMasterHex getGUIHexByLabel(String label)
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

    /** Do a brute-force search through the hex array, looking for
     *  a hex with the proper terrain type.  Return the hex, or null
     *  if none is found. */
    public static MasterHex getAnyHexWithTerrain(char terrain)
    {
        Iterator it = plainHexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.getTerrain() == terrain)
            {
                return hex;
            }
        }

        Log.error("Could not find hex with terrain " + terrain);
        return null;
    }

    /** Do a brute-force search through the hex array, looking for
     *  a hex with the proper terrain type.  Return the hex, or null
     *  if none is found. */
    public GUIMasterHex getAnyGUIHexWithTerrain(char terrain)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            GUIMasterHex hex = (GUIMasterHex)it.next();
            if (hex.getTerrain() == terrain)
            {
                return hex;
            }
        }

        Log.error("Could not find hex with terrain " + terrain);
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


    public void unselectAllHexes()
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

    public void unselectHexByLabel(String label)
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

    public void unselectHexesByLabels(Set labels)
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

    public void selectHexByLabel(String label)
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

    public void selectHexesByLabels(Set labels)
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


    public void actOnMisclick()
    {
        switch (client.getPhase())
        {
            case Game.MOVE:
                client.clearRecruitChits();
                client.setMoverId(null);
                highlightUnmovedLegions();
                break;

            case Game.FIGHT:
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

            case Game.MUSTER:
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
                    actOnLegion(markerId, hex.getLabel());
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
            case Game.SPLIT:
                client.doSplit(markerId);
                break;

            case Game.MOVE:
                client.setMoverId(markerId);
                getGUIHexByLabel(hexLabel).repaint();
                highlightMoves(markerId);
                break;

            case Game.FIGHT:
                client.doFight(hexLabel);
                break;

            case Game.MUSTER:
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
            case Game.MOVE:
                client.clearRecruitChits();
                String moverId = client.getMoverId();
                if (client.doMove(moverId, hexLabel))
                {
                    highlightUnmovedLegions();
                }
                else
                {
                    actOnMisclick();
                }
                break;

            // If we're fighting and there is an engagement here,
            // resolve it.  If an angel is being summoned, mark
            // the donor legion instead.
            case Game.FIGHT:
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
        return new Dimension(64 * scale, 56 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    public void rescale()
    {
        // XXX setupHexesGUI() should be sufficient but fails.
        setupHexes();
        loadInitialMarkerImages();
    }


    public void deiconify()
    {
        if (masterFrame.getState() == JFrame.ICONIFIED)
        {
            masterFrame.setState(JFrame.NORMAL);
        }
        masterFrame.show();
    }


    public void dispose()
    {
        setVisible(false);
        setEnabled(false);
        masterFrame.setVisible(false);
        masterFrame.setEnabled(false);
        masterFrame.dispose();
    }
}
