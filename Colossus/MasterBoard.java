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

    // XXX This direct Game reference needs to be eliminated.
    private Game game;

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
    public static final String redoLast = "Redo";
    public static final String undoAll = "Undo All";
    public static final String doneWithPhase = "Done";

    public static final String takeMulligan = "Take Mulligan";
    public static final String concedeBattle = "Concede battle";
    public static final String withdrawFromGame = "Withdraw from Game";

    public static final String viewRecruitInfo = "View Recruit Info";
    public static final String viewBattleMap = "View Battle Map";
    public static final String changeScale = "Change Scale";

    private AbstractAction newGameAction;
    private AbstractAction loadGameAction;
    private AbstractAction saveGameAction;
    private AbstractAction saveGameAsAction;
    private AbstractAction quitGameAction;
    private AbstractAction saveOptionsAction;

    private AbstractAction undoLastAction;
    private AbstractAction redoLastAction;
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

        // XXX temporary
        this.game = client.getGame();

        masterFrame = new JFrame("MasterBoard");
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
        newBoard.setGame(game);
        newBoard.setupActions();
        return newBoard;
    }


    public void setGame(Game game)
    {
        this.game = game;
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
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }

                int phase = game.getPhase();
                switch(phase)
                {
                    case Game.SPLIT:
                        // Peek at the undo stack so we know where to align
                        String splitoffId = (String)Client.topUndoStack();
                        Legion splitoff = game.getLegionByMarkerId(splitoffId);
                        String hexLabel = splitoff.getCurrentHexLabel();
                        player.undoLastSplit();
                        alignLegions(hexLabel);
                        highlightTallLegions(player);
                        repaint();
                        break;

                    case Game.MOVE:
                        player.undoLastMove();
                        highlightUnmovedLegions();
                        break;

                    case Game.FIGHT:
                        Log.error("called undoLastAction in FIGHT");
                        break;

                    case Game.MUSTER:
                        player.undoLastRecruit();
                        highlightPossibleRecruits();
                        break;
                }
            }
        };

        undoAllAction = new AbstractAction(undoAll)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }

                int phase = game.getPhase();
                switch(phase)
                {
                    case Game.SPLIT:
                        // peek at the undo stack so we know where to align
                        player.undoAllSplits();
                        alignAllLegions();
                        highlightTallLegions(player);
                        repaint();
                        break;

                    case Game.MOVE:
                        player.undoAllMoves();
                        highlightUnmovedLegions();
                        break;

                    case Game.FIGHT:
                        Log.error("called undoAllAction in FIGHT");
                        break;

                    case Game.MUSTER:
                        player.undoAllRecruits();
                        highlightPossibleRecruits();
                        break;
                }
            }
        };

/* TODO
        redoLastAction = new AbstractAction(redoLast)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }

                int phase = game.getPhase();
                switch(phase)
                {
                    case Game.SPLIT:
                        player.redoLastSplit();
                        alignLegions(hexLabel);
                        highlightTallLegions(player);
                        repaint();
                        break;

                    case Game.MOVE:
                        player.redoLastMove();
                        highlightUnmovedLegions();
                        break;

                    case Game.FIGHT:
                        Log.error("called redoLastAction in FIGHT");
                        break;

                    case Game.MUSTER:
                        player.redoLastRecruit();
                        highlightPossibleRecruits();
                        break;
                }
            }
        };
*/

        doneWithPhaseAction = new AbstractAction(doneWithPhase)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }

                int phase = game.getPhase();
                switch(phase)
                {
                    case Game.SPLIT:
                        // Initial legions must be split.
                        if (game.getTurnNumber() == 1 &&
                            player.getNumLegions() == 1)
                        {
                            client.showMessageDialog("Must split.");
                        }
                        else
                        {
                            game.advancePhase(Game.SPLIT);
                        }
                        break;

                    case Game.MOVE:
                        // If any legion has a legal non-teleport move, then
                        // the player must move at least one legion.
                        if (player.legionsMoved() == 0 &&
                            player.countMobileLegions() > 0)
                        {
                            highlightUnmovedLegions();
                            client.showMessageDialog(
                                "At least one legion must move.");
                        }
                        else
                        {
                            // If legions share a hex and have a legal
                            // non-teleport move, force one of them to take it.
                            if (player.splitLegionHasForcedMove())
                            {
                                highlightUnmovedLegions();
                                client.showMessageDialog(
                                    "Split legions must be separated.");
                            }
                            // Otherwise, recombine all split legions still in
                            // the same hex, and move on to the next phase.
                            else
                            {
                                player.undoAllSplits();
                                client.clearRecruitChits();
                                game.advancePhase(Game.MOVE);
                            }
                        }
                        break;

                    case Game.FIGHT:
                        // Advance only if there are no unresolved engagements.
                        if (game.findEngagements().size() == 0)
                        {
                            game.advancePhase(Game.FIGHT);
                        }
                        else
                        {
                            client.showMessageDialog(
                                "Must Resolve Engagements.");
                        }
                        break;

                    case Game.MUSTER:
                        player.commitMoves();
                        // Mulligans are only allowed on turn 1.
                        player.setMulligansLeft(0);
                        game.advancePhase(Game.MUSTER);
                        break;
                }
            }
        };


        takeMulliganAction = new AbstractAction(takeMulligan)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }
                player.takeMulligan();

                // Reroll movement die.  Remove Take Mulligan option
                // if applicable.
                game.setupPhase();
            }
        };







        // TODO Let inactive players withdraw from the game.
        withdrawFromGameAction = new AbstractAction(withdrawFromGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (!player.getName().equals(client.getPlayerName()))
                {
                    return;
                }
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
                   player.die(null, true);
                   game.advancePhase(game.getPhase());
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
                    new ShowMasterHex(masterFrame, game, hex, lastPoint);
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
                    game.newGame();
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
                        game.loadGame(chooser.getSelectedFile().getName());
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
                game.saveGame();
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
                    game.saveGame(path.toString());
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
                    "Are you sure you with to quit?",
                    "Quit Game?",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);

                if (answer == JOptionPane.YES_OPTION)
                {
                    game.dispose();
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
        addCheckBox(graphicsMenu, Options.showDice, KeyEvent.VK_D);
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

        Player player = game.getPlayer(playerName);
        String colorName = player.getColor();
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

    /** This method only needs to be run once, since the attributes it
     *  sets up are constant for the game. */
    private static void setupHexesGameState(MasterHex [][] h)
    {
        // Add terrain types, id labels, label sides, and exits to hexes.
        h[0][3].setTerrain('S');
        h[0][3].setLabel(132);
        h[0][3].setLabelSide(2);
        h[0][3].setExitType(1, MasterHex.ARROWS);

        h[0][4].setTerrain('P');
        h[0][4].setLabel(133);
        h[0][4].setLabelSide(1);
        h[0][4].setExitType(0, MasterHex.ARROWS);

        h[1][2].setTerrain('B');
        h[1][2].setLabel(130);
        h[1][2].setLabelSide(2);
        h[1][2].setExitType(1, MasterHex.ARROWS);

        h[1][3].setTerrain('M');
        h[1][3].setLabel(131);
        h[1][3].setLabelSide(5);
        h[1][3].setExitType(0, MasterHex.ARROWS);
        h[1][3].setExitType(2, MasterHex.ARCH);

        h[1][4].setTerrain('B');
        h[1][4].setLabel(134);
        h[1][4].setLabelSide(4);
        h[1][4].setExitType(1, MasterHex.ARCH);
        h[1][4].setExitType(5, MasterHex.ARROWS);

        h[1][5].setTerrain('J');
        h[1][5].setLabel(135);
        h[1][5].setLabelSide(1);
        h[1][5].setExitType(0, MasterHex.ARROWS);

        h[2][1].setTerrain('D');
        h[2][1].setLabel(128);
        h[2][1].setLabelSide(2);
        h[2][1].setExitType(1, MasterHex.ARROWS);

        h[2][2].setTerrain('P');
        h[2][2].setLabel(129);
        h[2][2].setLabelSide(5);
        h[2][2].setExitType(0, MasterHex.ARROWS);
        h[2][2].setExitType(2, MasterHex.ARCH);

        h[2][3].setTerrain('H');
        h[2][3].setLabel(32);
        h[2][3].setLabelSide(2);
        h[2][3].setExitType(3, MasterHex.ARROWS);
        h[2][3].setExitType(5, MasterHex.BLOCK);

        h[2][4].setTerrain('J');
        h[2][4].setLabel(33);
        h[2][4].setLabelSide(1);
        h[2][4].setExitType(2, MasterHex.ARROWS);
        h[2][4].setExitType(4, MasterHex.BLOCK);

        h[2][5].setTerrain('M');
        h[2][5].setLabel(136);
        h[2][5].setLabelSide(4);
        h[2][5].setExitType(1, MasterHex.ARCH);
        h[2][5].setExitType(5, MasterHex.ARROWS);

        h[2][6].setTerrain('B');
        h[2][6].setLabel(137);
        h[2][6].setLabelSide(1);
        h[2][6].setExitType(0, MasterHex.ARROWS);

        h[3][0].setTerrain('M');
        h[3][0].setLabel(126);
        h[3][0].setLabelSide(2);
        h[3][0].setExitType(1, MasterHex.ARROWS);

        h[3][1].setTerrain('B');
        h[3][1].setLabel(127);
        h[3][1].setLabelSide(5);
        h[3][1].setExitType(0, MasterHex.ARROWS);
        h[3][1].setExitType(2, MasterHex.ARCH);

        h[3][2].setTerrain('T');
        h[3][2].setLabel(500);
        h[3][2].setLabelSide(2);
        h[3][2].setExitType(1, MasterHex.ARROW);
        h[3][2].setExitType(3, MasterHex.ARROW);
        h[3][2].setExitType(5, MasterHex.ARROW);

        h[3][3].setTerrain('B');
        h[3][3].setLabel(31);
        h[3][3].setLabelSide(5);
        h[3][3].setExitType(0, MasterHex.ARCH);
        h[3][3].setExitType(4, MasterHex.ARROWS);

        h[3][4].setTerrain('P');
        h[3][4].setLabel(34);
        h[3][4].setLabelSide(4);
        h[3][4].setExitType(1, MasterHex.ARROWS);
        h[3][4].setExitType(3, MasterHex.ARCH);

        h[3][5].setTerrain('T');
        h[3][5].setLabel(600);
        h[3][5].setLabelSide(1);
        h[3][5].setExitType(0, MasterHex.ARROW);
        h[3][5].setExitType(2, MasterHex.ARROW);
        h[3][5].setExitType(4, MasterHex.ARROW);

        h[3][6].setTerrain('P');
        h[3][6].setLabel(138);
        h[3][6].setLabelSide(4);
        h[3][6].setExitType(1, MasterHex.ARCH);
        h[3][6].setExitType(5, MasterHex.ARROWS);

        h[3][7].setTerrain('D');
        h[3][7].setLabel(139);
        h[3][7].setLabelSide(1);
        h[3][7].setExitType(0, MasterHex.ARROWS);

        h[4][0].setTerrain('J');
        h[4][0].setLabel(125);
        h[4][0].setLabelSide(3);
        h[4][0].setExitType(2, MasterHex.ARROWS);

        h[4][1].setTerrain('J');
        h[4][1].setLabel(26);
        h[4][1].setLabelSide(2);
        h[4][1].setExitType(3, MasterHex.ARROWS);
        h[4][1].setExitType(5, MasterHex.BLOCK);

        h[4][2].setTerrain('M');
        h[4][2].setLabel(27);
        h[4][2].setLabelSide(5);
        h[4][2].setExitType(2, MasterHex.ARROWS);
        h[4][2].setExitType(4, MasterHex.ARCH);

        h[4][3].setTerrain('W');
        h[4][3].setLabel(30);
        h[4][3].setLabelSide(2);
        h[4][3].setExitType(3, MasterHex.ARCH);
        h[4][3].setExitType(5, MasterHex.ARROWS);

        h[4][4].setTerrain('D');
        h[4][4].setLabel(35);
        h[4][4].setLabelSide(1);
        h[4][4].setExitType(0, MasterHex.ARCH);
        h[4][4].setExitType(2, MasterHex.ARROWS);

        h[4][5].setTerrain('B');
        h[4][5].setLabel(38);
        h[4][5].setLabelSide(4);
        h[4][5].setExitType(3, MasterHex.ARROWS);
        h[4][5].setExitType(5, MasterHex.ARCH);

        h[4][6].setTerrain('W');
        h[4][6].setLabel(39);
        h[4][6].setLabelSide(1);
        h[4][6].setExitType(2, MasterHex.ARROWS);
        h[4][6].setExitType(4, MasterHex.BLOCK);

        h[4][7].setTerrain('M');
        h[4][7].setLabel(140);
        h[4][7].setLabelSide(0);
        h[4][7].setExitType(5, MasterHex.ARROWS);

        h[5][0].setTerrain('P');
        h[5][0].setLabel(124);
        h[5][0].setLabelSide(0);
        h[5][0].setExitType(1, MasterHex.ARROWS);
        h[5][0].setExitType(3, MasterHex.ARCH);

        h[5][1].setTerrain('W');
        h[5][1].setLabel(25);
        h[5][1].setLabelSide(3);
        h[5][1].setExitType(0, MasterHex.BLOCK);
        h[5][1].setExitType(4, MasterHex.ARROWS);

        h[5][2].setTerrain('S');
        h[5][2].setLabel(28);
        h[5][2].setLabelSide(2);
        h[5][2].setExitType(1, MasterHex.ARCH);
        h[5][2].setExitType(3, MasterHex.ARROWS);

        h[5][3].setTerrain('P');
        h[5][3].setLabel(29);
        h[5][3].setLabelSide(5);
        h[5][3].setExitType(2, MasterHex.ARCH);
        h[5][3].setExitType(4, MasterHex.ARROWS);

        h[5][4].setTerrain('M');
        h[5][4].setLabel(36);
        h[5][4].setLabelSide(4);
        h[5][4].setExitType(1, MasterHex.ARCH);
        h[5][4].setExitType(3, MasterHex.ARROWS);

        h[5][5].setTerrain('H');
        h[5][5].setLabel(37);
        h[5][5].setLabelSide(1);
        h[5][5].setExitType(2, MasterHex.ARCH);
        h[5][5].setExitType(4, MasterHex.ARROWS);

        h[5][6].setTerrain('J');
        h[5][6].setLabel(40);
        h[5][6].setLabelSide(0);
        h[5][6].setExitType(1, MasterHex.ARROWS);
        h[5][6].setExitType(3, MasterHex.BLOCK);

        h[5][7].setTerrain('B');
        h[5][7].setLabel(141);
        h[5][7].setLabelSide(3);
        h[5][7].setExitType(0, MasterHex.ARCH);
        h[5][7].setExitType(4, MasterHex.ARROWS);

        h[6][0].setTerrain('B');
        h[6][0].setLabel(123);
        h[6][0].setLabelSide(3);
        h[6][0].setExitType(2, MasterHex.ARROWS);

        h[6][1].setTerrain('B');
        h[6][1].setLabel(24);
        h[6][1].setLabelSide(0);
        h[6][1].setExitType(1, MasterHex.ARCH);
        h[6][1].setExitType(5, MasterHex.ARROWS);

        h[6][2].setTerrain('H');
        h[6][2].setLabel(23);
        h[6][2].setLabelSide(3);
        h[6][2].setExitType(0, MasterHex.ARROWS);
        h[6][2].setExitType(4, MasterHex.ARCH);

        h[6][3].setTerrain('m');
        h[6][3].setLabel(5000);
        h[6][3].setLabelSide(2);
        h[6][3].setExitType(1, MasterHex.ARROW);
        h[6][3].setExitType(3, MasterHex.ARROW);
        h[6][3].setExitType(5, MasterHex.BLOCK);

        h[6][4].setTerrain('t');
        h[6][4].setLabel(6000);
        h[6][4].setLabelSide(1);
        h[6][4].setExitType(0, MasterHex.ARROW);
        h[6][4].setExitType(2, MasterHex.ARROW);
        h[6][4].setExitType(4, MasterHex.BLOCK);

        h[6][5].setTerrain('S');
        h[6][5].setLabel(42);
        h[6][5].setLabelSide(0);
        h[6][5].setExitType(1, MasterHex.ARROWS);
        h[6][5].setExitType(5, MasterHex.ARCH);

        h[6][6].setTerrain('M');
        h[6][6].setLabel(41);
        h[6][6].setLabelSide(3);
        h[6][6].setExitType(0, MasterHex.ARROWS);
        h[6][6].setExitType(2, MasterHex.ARCH);

        h[6][7].setTerrain('S');
        h[6][7].setLabel(142);
        h[6][7].setLabelSide(0);
        h[6][7].setExitType(5, MasterHex.ARROWS);

        h[7][0].setTerrain('M');
        h[7][0].setLabel(122);
        h[7][0].setLabelSide(0);
        h[7][0].setExitType(1, MasterHex.ARROWS);
        h[7][0].setExitType(3, MasterHex.ARCH);

        h[7][1].setTerrain('T');
        h[7][1].setLabel(400);
        h[7][1].setLabelSide(3);
        h[7][1].setExitType(0, MasterHex.ARROW);
        h[7][1].setExitType(2, MasterHex.ARROW);
        h[7][1].setExitType(4, MasterHex.ARROW);

        h[7][2].setTerrain('M');
        h[7][2].setLabel(22);
        h[7][2].setLabelSide(0);
        h[7][2].setExitType(3, MasterHex.ARCH);
        h[7][2].setExitType(5, MasterHex.ARROWS);

        h[7][3].setTerrain('t');
        h[7][3].setLabel(4000);
        h[7][3].setLabelSide(3);
        h[7][3].setExitType(0, MasterHex.BLOCK);
        h[7][3].setExitType(2, MasterHex.ARROW);
        h[7][3].setExitType(4, MasterHex.ARROW);

        h[7][4].setTerrain('m');
        h[7][4].setLabel(1000);
        h[7][4].setLabelSide(0);
        h[7][4].setExitType(1, MasterHex.ARROW);
        h[7][4].setExitType(3, MasterHex.BLOCK);
        h[7][4].setExitType(5, MasterHex.ARROW);

        h[7][5].setTerrain('P');
        h[7][5].setLabel(1);
        h[7][5].setLabelSide(3);
        h[7][5].setExitType(0, MasterHex.ARCH);
        h[7][5].setExitType(2, MasterHex.ARROWS);

        h[7][6].setTerrain('T');
        h[7][6].setLabel(100);
        h[7][6].setLabelSide(0);
        h[7][6].setExitType(1, MasterHex.ARROW);
        h[7][6].setExitType(3, MasterHex.ARROW);
        h[7][6].setExitType(5, MasterHex.ARROW);

        h[7][7].setTerrain('P');
        h[7][7].setLabel(101);
        h[7][7].setLabelSide(3);
        h[7][7].setExitType(0, MasterHex.ARCH);
        h[7][7].setExitType(4, MasterHex.ARROWS);

        h[8][0].setTerrain('S');
        h[8][0].setLabel(121);
        h[8][0].setLabelSide(3);
        h[8][0].setExitType(2, MasterHex.ARROWS);

        h[8][1].setTerrain('P');
        h[8][1].setLabel(20);
        h[8][1].setLabelSide(0);
        h[8][1].setExitType(3, MasterHex.ARROWS);
        h[8][1].setExitType(5, MasterHex.ARCH);

        h[8][2].setTerrain('D');
        h[8][2].setLabel(21);
        h[8][2].setLabelSide(3);
        h[8][2].setExitType(2, MasterHex.ARCH);
        h[8][2].setExitType(4, MasterHex.ARROWS);

        h[8][3].setTerrain('m');
        h[8][3].setLabel(3000);
        h[8][3].setLabelSide(4);
        h[8][3].setExitType(1, MasterHex.BLOCK);
        h[8][3].setExitType(3, MasterHex.ARROW);
        h[8][3].setExitType(5, MasterHex.ARROW);

        h[8][4].setTerrain('t');
        h[8][4].setLabel(2000);
        h[8][4].setLabelSide(5);
        h[8][4].setExitType(0, MasterHex.ARROW);
        h[8][4].setExitType(2, MasterHex.BLOCK);
        h[8][4].setExitType(4, MasterHex.ARROW);

        h[8][5].setTerrain('W');
        h[8][5].setLabel(2);
        h[8][5].setLabelSide(0);
        h[8][5].setExitType(1, MasterHex.ARCH);
        h[8][5].setExitType(3, MasterHex.ARROWS);

        h[8][6].setTerrain('B');
        h[8][6].setLabel(3);
        h[8][6].setLabelSide(3);
        h[8][6].setExitType(2, MasterHex.ARROWS);
        h[8][6].setExitType(4, MasterHex.ARCH);

        h[8][7].setTerrain('B');
        h[8][7].setLabel(102);
        h[8][7].setLabelSide(0);
        h[8][7].setExitType(5, MasterHex.ARROWS);

        h[9][0].setTerrain('B');
        h[9][0].setLabel(120);
        h[9][0].setLabelSide(0);
        h[9][0].setExitType(1, MasterHex.ARROWS);
        h[9][0].setExitType(3, MasterHex.ARCH);

        h[9][1].setTerrain('J');
        h[9][1].setLabel(19);
        h[9][1].setLabelSide(3);
        h[9][1].setExitType(0, MasterHex.BLOCK);
        h[9][1].setExitType(4, MasterHex.ARROWS);

        h[9][2].setTerrain('W');
        h[9][2].setLabel(16);
        h[9][2].setLabelSide(4);
        h[9][2].setExitType(1, MasterHex.ARROWS);
        h[9][2].setExitType(5, MasterHex.ARCH);

        h[9][3].setTerrain('P');
        h[9][3].setLabel(15);
        h[9][3].setLabelSide(1);
        h[9][3].setExitType(0, MasterHex.ARROWS);
        h[9][3].setExitType(4, MasterHex.ARCH);

        h[9][4].setTerrain('M');
        h[9][4].setLabel(8);
        h[9][4].setLabelSide(2);
        h[9][4].setExitType(1, MasterHex.ARROWS);
        h[9][4].setExitType(5, MasterHex.ARCH);

        h[9][5].setTerrain('D');
        h[9][5].setLabel(7);
        h[9][5].setLabelSide(5);
        h[9][5].setExitType(0, MasterHex.ARROWS);
        h[9][5].setExitType(4, MasterHex.ARCH);

        h[9][6].setTerrain('H');
        h[9][6].setLabel(4);
        h[9][6].setLabelSide(0);
        h[9][6].setExitType(1, MasterHex.ARROWS);
        h[9][6].setExitType(3, MasterHex.BLOCK);

        h[9][7].setTerrain('M');
        h[9][7].setLabel(103);
        h[9][7].setLabelSide(3);
        h[9][7].setExitType(0, MasterHex.ARCH);
        h[9][7].setExitType(4, MasterHex.ARROWS);

        h[10][0].setTerrain('P');
        h[10][0].setLabel(119);
        h[10][0].setLabelSide(3);
        h[10][0].setExitType(2, MasterHex.ARROWS);

        h[10][1].setTerrain('H');
        h[10][1].setLabel(18);
        h[10][1].setLabelSide(4);
        h[10][1].setExitType(1, MasterHex.BLOCK);
        h[10][1].setExitType(5, MasterHex.ARROWS);

        h[10][2].setTerrain('B');
        h[10][2].setLabel(17);
        h[10][2].setLabelSide(1);
        h[10][2].setExitType(0, MasterHex.ARROWS);
        h[10][2].setExitType(2, MasterHex.ARCH);

        h[10][3].setTerrain('S');
        h[10][3].setLabel(14);
        h[10][3].setLabelSide(4);
        h[10][3].setExitType(3, MasterHex.ARCH);
        h[10][3].setExitType(5, MasterHex.ARROWS);

        h[10][4].setTerrain('H');
        h[10][4].setLabel(9);
        h[10][4].setLabelSide(5);
        h[10][4].setExitType(0, MasterHex.ARCH);
        h[10][4].setExitType(2, MasterHex.ARROWS);

        h[10][5].setTerrain('P');
        h[10][5].setLabel(6);
        h[10][5].setLabelSide(2);
        h[10][5].setExitType(1, MasterHex.ARCH);
        h[10][5].setExitType(5, MasterHex.ARROWS);

        h[10][6].setTerrain('J');
        h[10][6].setLabel(5);
        h[10][6].setLabelSide(5);
        h[10][6].setExitType(0, MasterHex.ARROWS);
        h[10][6].setExitType(2, MasterHex.BLOCK);

        h[10][7].setTerrain('J');
        h[10][7].setLabel(104);
        h[10][7].setLabelSide(0);
        h[10][7].setExitType(5, MasterHex.ARROWS);

        h[11][0].setTerrain('D');
        h[11][0].setLabel(118);
        h[11][0].setLabelSide(4);
        h[11][0].setExitType(3, MasterHex.ARROWS);

        h[11][1].setTerrain('M');
        h[11][1].setLabel(117);
        h[11][1].setLabelSide(1);
        h[11][1].setExitType(2, MasterHex.ARROWS);
        h[11][1].setExitType(4, MasterHex.ARCH);

        h[11][2].setTerrain('T');
        h[11][2].setLabel(300);
        h[11][2].setLabelSide(4);
        h[11][2].setExitType(1, MasterHex.ARROW);
        h[11][2].setExitType(3, MasterHex.ARROW);
        h[11][2].setExitType(5, MasterHex.ARROW);

        h[11][3].setTerrain('M');
        h[11][3].setLabel(13);
        h[11][3].setLabelSide(1);
        h[11][3].setExitType(0, MasterHex.ARCH);
        h[11][3].setExitType(4, MasterHex.ARROWS);

        h[11][4].setTerrain('B');
        h[11][4].setLabel(10);
        h[11][4].setLabelSide(2);
        h[11][4].setExitType(1, MasterHex.ARROWS);
        h[11][4].setExitType(3, MasterHex.ARCH);

        h[11][5].setTerrain('T');
        h[11][5].setLabel(200);
        h[11][5].setLabelSide(5);
        h[11][5].setExitType(0, MasterHex.ARROW);
        h[11][5].setExitType(2, MasterHex.ARROW);
        h[11][5].setExitType(4, MasterHex.ARROW);

        h[11][6].setTerrain('B');
        h[11][6].setLabel(106);
        h[11][6].setLabelSide(2);
        h[11][6].setExitType(3, MasterHex.ARROWS);
        h[11][6].setExitType(5, MasterHex.ARCH);

        h[11][7].setTerrain('P');
        h[11][7].setLabel(105);
        h[11][7].setLabelSide(5);
        h[11][7].setExitType(4, MasterHex.ARROWS);

        h[12][1].setTerrain('B');
        h[12][1].setLabel(116);
        h[12][1].setLabelSide(4);
        h[12][1].setExitType(3, MasterHex.ARROWS);

        h[12][2].setTerrain('P');
        h[12][2].setLabel(115);
        h[12][2].setLabelSide(1);
        h[12][2].setExitType(2, MasterHex.ARROWS);
        h[12][2].setExitType(4, MasterHex.ARCH);

        h[12][3].setTerrain('J');
        h[12][3].setLabel(12);
        h[12][3].setLabelSide(4);
        h[12][3].setExitType(1, MasterHex.BLOCK);
        h[12][3].setExitType(5, MasterHex.ARROWS);

        h[12][4].setTerrain('W');
        h[12][4].setLabel(11);
        h[12][4].setLabelSide(5);
        h[12][4].setExitType(0, MasterHex.ARROWS);
        h[12][4].setExitType(2, MasterHex.BLOCK);

        h[12][5].setTerrain('M');
        h[12][5].setLabel(108);
        h[12][5].setLabelSide(2);
        h[12][5].setExitType(3, MasterHex.ARROWS);
        h[12][5].setExitType(5, MasterHex.ARCH);

        h[12][6].setTerrain('D');
        h[12][6].setLabel(107);
        h[12][6].setLabelSide(5);
        h[12][6].setExitType(4, MasterHex.ARROWS);

        h[13][2].setTerrain('J');
        h[13][2].setLabel(114);
        h[13][2].setLabelSide(4);
        h[13][2].setExitType(3, MasterHex.ARROWS);

        h[13][3].setTerrain('B');
        h[13][3].setLabel(113);
        h[13][3].setLabelSide(1);
        h[13][3].setExitType(2, MasterHex.ARROWS);
        h[13][3].setExitType(4, MasterHex.ARCH);

        h[13][4].setTerrain('P');
        h[13][4].setLabel(110);
        h[13][4].setLabelSide(2);
        h[13][4].setExitType(3, MasterHex.ARROWS);
        h[13][4].setExitType(5, MasterHex.ARCH);

        h[13][5].setTerrain('B');
        h[13][5].setLabel(109);
        h[13][5].setLabelSide(5);
        h[13][5].setExitType(4, MasterHex.ARROWS);

        h[14][3].setTerrain('M');
        h[14][3].setLabel(112);
        h[14][3].setLabelSide(4);
        h[14][3].setExitType(3, MasterHex.ARROWS);

        h[14][4].setTerrain('S');
        h[14][4].setLabel(111);
        h[14][4].setLabelSide(5);
        h[14][4].setExitType(4, MasterHex.ARROWS);

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

        Player player = game.getActivePlayer();

        masterFrame.setTitle(player.getName() + " Turn " +
            game.getTurnNumber() + " : Split stacks");

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

/* TODO
        mi = phaseMenu.add(redoLastAction);
        mi.setMnemonic(KeyEvent.VK_R);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
*/

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
            highlightTallLegions(player);
        }
    }


    public void setupMoveMenu()
    {
        unselectAllHexes();
        requestFocus();

        Player player = game.getActivePlayer();
        masterFrame.setTitle(player.getName() + " Turn " +
            game.getTurnNumber() + " : Movement Roll: " +
            player.getMovementRoll());

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

/*
        mi = phaseMenu.add(redoLastAction);
        mi.setMnemonic(KeyEvent.VK_R);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
*/

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

        String activePlayerName = game.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            game.getTurnNumber() + " : Resolve Engagements ");

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

        String activePlayerName = game.getActivePlayerName();

        masterFrame.setTitle(activePlayerName + " Turn " +
            game.getTurnNumber() + " : Muster Recruits ");

        phaseMenu.removeAll();

        JMenuItem mi;

        mi = phaseMenu.add(undoLastAction);
        mi.setMnemonic(KeyEvent.VK_U);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0));

/*
        mi = phaseMenu.add(redoLastAction);
        mi.setMnemonic(KeyEvent.VK_R);
        mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
*/

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
        Iterator it = game.getAllLegionIds().iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            client.addMarker(markerId);
            Legion legion = game.getLegionByMarkerId(markerId);
            String hexLabel = legion.getCurrentHexLabel();
            alignLegions(hexLabel);
        }
        masterFrame.setVisible(true);
        repaint();
    }


    public void alignLegions(String hexLabel)
    {
        GUIMasterHex hex = getGUIHexByLabel(hexLabel);
        ArrayList markerIds = game.getLegionMarkerIds(hexLabel);
        Player player = game.getActivePlayer();
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


    private void highlightTallLegions(Player player)
    {
        HashSet set = new HashSet();

        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (legion.getHeight() >= 7)
            {
                MasterHex hex = legion.getCurrentHex();
                set.add(hex.getLabel());
            }
        }
        selectHexesByLabels(set);
    }

    private void highlightUnmovedLegions()
    {
        unselectAllHexes();
        Player player = game.getActivePlayer();
        HashSet set = new HashSet();
        Iterator it = player.getLegions().iterator();
        while (it.hasNext())
        {
            Legion legion = (Legion)it.next();
            if (!legion.hasMoved())
            {
                set.add(legion.getCurrentHexLabel());
            }
        }
        selectHexesByLabels(set);
        repaint();
    }


    /** Select hexes where this legion can move. */
    private void highlightMoves(Legion legion)
    {
        Set set = game.listMoves(legion, true, true, false);
        unselectAllHexes();
        selectHexesByLabels(set);
        showBestRecruit(legion, set);
    }


    private void showBestRecruit(Legion legion, Set set)
    {
        client.clearRecruitChits();
        Iterator it = set.iterator();
        while (it.hasNext())
        {
            String hexLabel = (String)it.next();
            ArrayList recruits = game.findEligibleRecruits(legion, hexLabel);
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
        Set set = game.findEngagements();
        unselectAllHexes();
        selectHexesByLabels(set);
    }


    /** Return number of legions with summonable angels. */
    public int highlightSummonableAngels(Legion legion)
    {
        Set set = game.findSummonableAngels(legion);
        unselectAllHexes();
        selectHexesByLabels(set);
        return set.size();
    }


    private void highlightPossibleRecruits()
    {
        int count = 0;
        Player player = game.getActivePlayer();

        HashSet set = new HashSet();

        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.getLegion(i);
            if (legion.hasMoved() && legion.canRecruit())
            {
                ArrayList recruits = game.findEligibleRecruits(legion);
                if (!recruits.isEmpty())
                {
                    MasterHex hex = getHexByLabel(legion.getCurrentHexLabel());
                    set.add(hex.getLabel());
                    count++;
                }
            }
        }

        if (count > 0)
        {
            selectHexesByLabels(set);
        }
    }


    private void setupIcon()
    {
        try
        {
            masterFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(Chit.getImagePath(
                Creature.colossus.getImageName()))));
        }
        catch (NullPointerException e)
        {
            Log.error(e.toString() + " Couldn't find " +
                Creature.colossus.getImageName());
            game.dispose();
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
        switch (game.getPhase())
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
                    highlightSummonableAngels(summonAngel.getLegion());
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
            e.isAltDown());
    }

    class MasterBoardMouseHandler extends MouseAdapter
    {
        public void mousePressed(MouseEvent e)
        {
            Point point = e.getPoint();
            Marker marker = getMarkerAtPoint(point);
            if (marker != null)
            {
                String markerId = marker.getId();
                Legion legion = game.getLegionByMarkerId(markerId);
                String playerName = legion.getPlayerName();

                // Move the clicked-on marker to the top of the z-order.
                client.setMarker(markerId, marker);

                // What to do depends on which mouse button was used
                // and the current phase of the turn.

                // Right-click means to show the contents of the legion.
                if (isPopupButton(e))
                {
                    // TODO We need a client-side legion class that doesn't
                    // know the full contents of every enemy legion.
                    legion.sortCritters();
                    new ShowLegion(masterFrame, legion, point,
                        client.getOption(Options.allStacksVisible) ||
                        playerName == client.getPlayerName());
                    return;
                }
                else
                {
                    // Only the current player can manipulate his legions.
                    if (playerName.equals(client.getPlayerName()) &&
                        playerName.equals(game.getActivePlayerName()))
                    {
                        actOnLegion(legion);
                        return;
                    }
                }
            }

            // No hits on chits, so check map.
            GUIMasterHex hex = getHexContainingPoint(point);
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
                if (client.getPlayerName().equals(game.getActivePlayerName()))
                {
                    actOnHex(hex.getLabel());
                    hex.repaint();
                    return;
                }
            }

            // No hits on chits or map, so re-highlight.
            if (client.getPlayerName().equals(game.getActivePlayerName()))
            {
                actOnMisclick();
            }
        }
    }


    private void actOnLegion(Legion legion)
    {
        switch (game.getPhase())
        {
            case Game.SPLIT:
                game.doSplit(legion, legion.getPlayer());
                break;

            case Game.MOVE:
                client.setMoverId(legion.getMarkerId());
                getGUIHexByLabel(legion.getCurrentHexLabel()).repaint();
                highlightMoves(legion);
                break;

            case Game.FIGHT:
                client.doFight(legion.getCurrentHexLabel());
                break;

            case Game.MUSTER:
                client.doMuster(legion);
                break;
        }
    }

    private void actOnHex(String hexLabel)
    {
        Player player = game.getActivePlayer();

        switch (game.getPhase())
        {
            // If we're moving, and have selected a legion which
            // has not yet moved, and this hex is a legal
            // destination, move the legion here.
            case Game.MOVE:
                client.clearRecruitChits();
                String moverId = client.getMoverId();
                if (game.doMove(moverId, hexLabel))
                {
                    Legion mover = game.getLegionByMarkerId(moverId);
                    getGUIHexByLabel(hexLabel).repaint();
                    getGUIHexByLabel(mover.getStartingHexLabel()).repaint();
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
             game.dispose();
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
        setupHexesGUI();
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


    public static void main(String [] args)
    {
        Game game = new Game();
        game.initServerAndClients();
        new MasterBoard(game.getServer().getClient(0));
    }
}
