import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class MasterBoard implements the GUI for a Titan masterboard.
 * @version $Id$
 * @author David Ripton
 */

public class MasterBoard extends JPanel implements MouseListener,
    WindowListener
{
    // There are a total of 96 hexes
    // Their Titan labels are:
    // Middle ring: 1-42
    // Outer ring: 101-142
    // Towers: 100, 200, 300, 400, 500, 600
    // Inner ring: 1000, 2000, 3000, 4000, 5000, 6000

    // For easy of mapping to the GUI, they'll be stored
    // in a 15x8 array, with some empty elements.

    private static MasterHex[][] h = new MasterHex[15][8];
    private static ArrayList hexes = new ArrayList();

    private static int scale;
    private static Game game;

    private JFrame masterFrame;
    private JPopupMenu popupMenu;
    private JMenuItem menuItemRecruitInfo; 
    private JMenuItem menuItemBattleMap;

    /** Last point clicked is needed for popup menus. */
    private Point lastPoint;
    private Container contentPane;

    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu actionMenu;
    private JMenu optionsMenu;

    static final String newGame = "<html><b>N</b>ew game";
    static final String openGame = "<html><b>O</b>pen game";
    static final String saveGame = "<html><b>S</b>ave game";
    static final String saveGameAs = "<html>Save game as";
    static final String concedeBattle = "<html><b>C</b>oncede battle";
    static final String autosave = "<html>Autosave";
    static final String allStacksVisible = "<html>All stacks visible";
    static final String autopickRecruiter = "<html>Autopick recruiter";
    static final String showGameStatus = "<html>Show game status";
    static final String showDice = "<html>Show dice";

    static final String undoLastSplit = "<html>Undo <b>L</b>ast Split";
    static final String undoAllSplits = "<html>Undo <b>A</b>ll Splits";
    static final String withdrawFromGame = "<html><b>W</b>ithdraw from Game";
    static final String doneWithSplits = "<html><b>D</b>one with Splits";
    static final String undoLastMove = "<html>Undo <b>L</b>ast Move";
    static final String undoAllMoves = "<html>Undo <b>A</b>ll Moves";
    static final String takeMulligan = "<html>Take <b>M</b>ulligan";
    static final String doneWithMoves = "<html><b>D</b>one with Moves";
    static final String doneWithEngagements = 
        "<html><b>D</b>one with Engagements";
    static final String undoLastRecruit = "<html>Undo <b>L</b>ast Recruit";
    static final String undoAllRecruits = "<html>Undo <b>A</b>ll Recruits";
    static final String doneWithTurn = "<html><b>D</b>one with Turn";
    static final String viewRecruitInfo = "<html>View Recruit Info";
    static final String viewBattleMap = "<html>View Battle Map";

    AbstractAction undoLastSplitAction;
    AbstractAction undoAllSplitsAction;
    AbstractAction doneWithSplitsAction;
    AbstractAction undoLastMoveAction;
    AbstractAction undoAllMovesAction;
    AbstractAction takeMulliganAction;
    AbstractAction doneWithMovesAction;
    AbstractAction doneWithEngagementsAction;
    AbstractAction undoLastRecruitAction;
    AbstractAction undoAllRecruitsAction;
    AbstractAction doneWithTurnAction;
    AbstractAction withdrawFromGameAction;
    AbstractAction viewRecruitInfoAction;
    AbstractAction viewBattleMapAction;


    public MasterBoard(Game game)
    {
        masterFrame = new JFrame("MasterBoard");

        this.game = game;

        contentPane = masterFrame.getContentPane();
        contentPane.setLayout(new BorderLayout());

        scale = getScale();

        setupIcon();

        setBackground(Color.black);

        masterFrame.addWindowListener(this);
        addMouseListener(this);

        setupActions();
        initializePopupMenu();
        initializeTopMenu();

        SetupMasterHexes.setupHexes(h, this, hexes);
        
        contentPane.add(this, BorderLayout.CENTER);
        
        masterFrame.pack();

        masterFrame.setVisible(true);
    }


    public void setupActions()
    {
        undoLastSplitAction = new AbstractAction(undoLastSplit)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoLastSplit();
                repaint();
            }
        };

        undoAllSplitsAction = new AbstractAction(undoAllSplits)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoAllSplits();
                repaint();
            }
        };

        doneWithSplitsAction = new AbstractAction(doneWithSplits)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                if (player.getMaxLegionHeight() > 7)
                {
                    JOptionPane.showMessageDialog(MasterBoard.this, 
                        "Must split.");
                    return;
                }
                advancePhase();
            }
        };

        undoLastMoveAction = new AbstractAction(undoLastMove)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoLastMove();
                // Remove all moves from MasterBoard and show unmoved legions.
                game.highlightUnmovedLegions();
            }
        };

        undoAllMovesAction = new AbstractAction(undoAllMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoAllMoves();
                // Remove all moves from MasterBoard and show unmoved legions.
                game.highlightUnmovedLegions();
            }
        };

        takeMulliganAction = new AbstractAction(takeMulligan)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.takeMulligan();
                if (player.getMulligansLeft() == 0)
                {
                    // Remove the Take Mulligan button, and reroll movement. 
                    setupMoveDialog();
                }
                // Remove all moves from MasterBoard.
                unselectAllHexes();
            }
        };

        doneWithMovesAction = new AbstractAction(doneWithMoves)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                // If any legions has a legal non-teleport move, then the 
                // player must move at least one legion.
                if (player.legionsMoved() == 0 && 
                    player.countMobileLegions() > 0)
                {
                    // Highlight all unmoved legions, rather than the
                    // locations to which the forced-to-move legion can move. 
                    game.highlightUnmovedLegions();
                    JOptionPane.showMessageDialog(MasterBoard.this, 
                        "At least one legion must move.");
                    return;
                }
                else
                {
                    // If two or more legions share the same hex, force a
                    // move if one is legal.  Otherwise, recombine them.
                    Collection legions = player.getLegions();
                    Iterator it = legions.iterator();
                    while (it.hasNext())
                    {
                        Legion legion = (Legion)it.next();
                        MasterHex hex = legion.getCurrentHex();
                        if (hex.getNumFriendlyLegions(player) > 1)
                        {
                            // If there are no legal moves, recombine.
                            if (game.countConventionalMoves(legion) == 0)
                            {
                                hex.recombineAllLegions();
                            }
                            else
                            {
                                // Highlight all unmoved legions, rather than 
                                // the locations to which the forced-to-move 
                                // legion can move. 
                                game.highlightUnmovedLegions();
                                JOptionPane.showMessageDialog(MasterBoard.this,
                                    "Split legions must be separated.");
                                return;
                            }
                        }
                    }
                    advancePhase();
                }
            }
        };

        doneWithEngagementsAction = new AbstractAction(doneWithEngagements)
        {
            public void actionPerformed(ActionEvent e)
            {
                // Advance only if there are no unresolved engagements.
                if (game.highlightEngagements() == 0)
                {
                    advancePhase();
                }
                else
                {
                    JOptionPane.showMessageDialog(MasterBoard.this, 
                        "Must Resolve Engagements."); 
                }
            }
        };

        undoLastRecruitAction = new AbstractAction(undoLastRecruit)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoLastRecruit();
                game.highlightPossibleRecruits();
            }
        };

        undoAllRecruitsAction = new AbstractAction(undoAllRecruits)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                player.undoAllRecruits();
                game.highlightPossibleRecruits();
            }
        };

        doneWithTurnAction = new AbstractAction(doneWithTurn)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                // Commit all moves.
                player.commitMoves();
                // Mulligans are only allowed on turn 1.
                player.setMulligansLeft(0);
                advancePhase();
            }
        };

        withdrawFromGameAction = new AbstractAction(withdrawFromGame)
        {
            public void actionPerformed(ActionEvent e)
            {
                Player player = game.getActivePlayer();
                String [] options = new String[2];
                options[0] = "Yes";
                options[1] = "No";
                int answer = JOptionPane.showOptionDialog(MasterBoard.this,
                    "Are you sure you with to withdraw from the game?",
                    "Confirm Withdrawal?", 
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);
    
                if (answer == JOptionPane.YES_OPTION)
                {
                   player.die(null, true);
                   advancePhase();
                }
            }
        };

        viewRecruitInfoAction = new AbstractAction(viewRecruitInfo)
        {
            public void actionPerformed(ActionEvent e)
            {
                MasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    new ShowMasterHex(masterFrame, hex, lastPoint);
                }
            }
        };
        
        viewBattleMapAction = new AbstractAction(viewBattleMap)
        {
            public void actionPerformed(ActionEvent e)
            {
                MasterHex hex = getHexContainingPoint(lastPoint);
                if (hex != null)
                {
                    new ShowBattleMap(masterFrame, hex);
                }
            }
        };
    }


    // XXX temp
    private void advancePhase()
    {
    }
    private void setupMoveDialog()
    {
    }


    public JFrame getFrame()
    {
        return masterFrame;
    }


    private void initializePopupMenu()
    {
        popupMenu = new JPopupMenu();
        menuItemRecruitInfo = popupMenu.add(viewRecruitInfoAction);
        menuItemBattleMap = popupMenu.add(viewBattleMapAction);
        contentPane.add(popupMenu);
    }


    private void initializeTopMenu()
    {
        JMenuItem mi;

        menuBar = new JMenuBar();
        masterFrame.setJMenuBar(menuBar);

        fileMenu = new JMenu("File");
        mi = new JMenuItem(newGame);
        fileMenu.add(mi);
        mi = new JMenuItem(openGame);
        fileMenu.add(mi);
        mi = new JMenuItem(saveGame);
        fileMenu.add(mi);
        mi = new JMenuItem(saveGameAs);
        fileMenu.add(mi);
        menuBar.add(fileMenu);

        // XXX These change by phase.
        actionMenu = new JMenu("Action");
        mi = new JMenuItem(undoLastSplit);
        actionMenu.add(mi);
        mi = new JMenuItem(undoAllSplits);
        actionMenu.add(mi);
        mi = new JMenuItem(doneWithSplits);
        actionMenu.add(mi);

        mi = new JMenuItem(takeMulligan);
        actionMenu.add(mi);
        mi = new JMenuItem(concedeBattle);
        actionMenu.add(mi);
        mi = new JMenuItem(withdrawFromGame);
        actionMenu.add(mi);
        menuBar.add(actionMenu);

        optionsMenu = new JMenu("Options");
        mi = new JCheckBoxMenuItem(autosave);
        optionsMenu.add(mi);
        mi = new JCheckBoxMenuItem(allStacksVisible);
        optionsMenu.add(mi);
        mi = new JCheckBoxMenuItem(autopickRecruiter);
        optionsMenu.add(mi);
        mi = new JCheckBoxMenuItem(showGameStatus);
        optionsMenu.add(mi);
        mi = new JCheckBoxMenuItem(showDice);
        optionsMenu.add(mi);
        menuBar.add(optionsMenu);
    }
                
                
    public void loadInitialMarkerImages()
    {
        Collection players = game.getPlayers();
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            Collection legions = player.getLegions();
            Iterator it2 = legions.iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                Marker marker = new Marker(3 * scale, legion.getImageName(),
                    this, null);
                legion.setMarker(marker);
                MasterHex hex = legion.getCurrentHex();
                hex.alignLegions();
            }
        }
    }
    
    
    private void setupIcon()
    {
        if (game != null && !game.isApplet())
        {
            try
            {
                masterFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Chit.getImagePath(
                    Creature.colossus.getImageName()))));
            }
            catch (NullPointerException e)
            {
                System.out.println(e.toString() + " Couldn't find " +
                    Creature.colossus.getImageName());
                game.dispose();
            }
        }
    }


    public static int getScale()
    {
        int scale = 17;

        // Make sure that the board fits on the screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        if (d.height < 1000)
        {
            scale = scale * d.height / 1000;
        }

        return scale;
    }


    public Game getGame()
    {
        return game;
    }


    /** Do a brute-force search through the hex array, looking for
     *  a match.  Return the hex, or null if none is found. */
    public static MasterHex getHexFromLabel(int label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.getLabel().equals(Integer.toString(label)))
            {
                return hex;
            }
        }

        System.out.println("Could not find hex " + label);
        return null;
    }


    /** Return the MasterHex that contains the given point, or
     *  null if none does. */
    private MasterHex getHexContainingPoint(Point point)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.contains(point))
            {
                return hex;
            }
        }

        return null;
    }
    
    
    /** Return the Legion whose marker contains the given point, or null 
     *  if none does. */
    private Legion getLegionWithMarkerContainingPoint(Point point)
    {
        Collection players = game.getPlayers();
        Iterator it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            Collection legions = player.getLegions();
            Iterator it2 = legions.iterator();
            while (it2.hasNext())
            {
                Legion legion = (Legion)it2.next();
                Marker marker = legion.getMarker();
                if (marker != null && marker.contains(point))
                {
                    return legion;
                }
            }
        }

        return null;
    }


    public static void unselectAllHexes()
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.isSelected())
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }


    public static void unselectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
                return;
            }
        }
    }


    public static void unselectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.unselect();
                hex.repaint();
            }
        }
    }
    
    
    public static void selectHexByLabel(String label)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (!hex.isSelected() && label.equals(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
                return;
            }
        }
    }


    public static void selectHexesByLabels(Set labels)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (!hex.isSelected() && labels.contains(hex.getLabel()))
            {
                hex.select();
                hex.repaint();
            }
        }
    }


    /** Clear all entry side and teleport information from all hexes occupied
     *  by one or fewer legions. */
    public void clearAllNonFriendlyOccupiedEntrySides(Player player)
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            if (hex.getNumFriendlyLegions(player) == 0)
            {
                hex.clearAllEntrySides();
                hex.setTeleported(false);
            }
        }
    }


    /** Clear all entry side and teleport information from all hexes. */
    public void clearAllEntrySides()
    {
        Iterator it = hexes.iterator();
        while (it.hasNext())
        {
            MasterHex hex = (MasterHex)it.next();
            hex.clearAllEntrySides();
            hex.setTeleported(false);
        }
    }


    public void deiconify()
    {
        masterFrame.setState(JFrame.NORMAL);
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();

        Legion legion = getLegionWithMarkerContainingPoint(point);

        if (legion != null)
        {
            Player player = legion.getPlayer();

            // What to do depends on which mouse button was used
            // and the current phase of the turn.

            // Right-click means to show the contents of the 
            // legion.
            if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
            {
                new ShowLegion(masterFrame, legion, point,
                    game.getAllVisible() || player == game.getActivePlayer());
                return;
            }
            else
            {
                // Only the current player can manipulate his legions.
                if (player == game.getActivePlayer())
                {
                    game.actOnLegion(legion);
                    return;
                }
            }
        }

        // No hits on chits, so check map.

        MasterHex hex = getHexContainingPoint(point);
        if (hex != null)
        {
            if (((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
                InputEvent.BUTTON2_MASK) || ((e.getModifiers() &
                InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK))
            {
                lastPoint = point;
                popupMenu.setLabel(hex.getDescription());
                popupMenu.show(e.getComponent(), point.x, point.y);

                return;
            }
            
            // Otherwise, the action to take depends on the phase.
            game.actOnHex(hex);
            hex.repaint();
            return;
        }

        // No hits on chits or map, so re-highlight.
        game.actOnMisclick();
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        game.dispose();
    }


    public void windowDeactivated(WindowEvent e)
    {
    }


    public void windowDeiconified(WindowEvent e)
    {
    }


    public void windowIconified(WindowEvent e)
    {
    }


    public void windowOpened(WindowEvent e)
    {
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
            MasterHex hex = (MasterHex)it.next();
            if (rectClip.intersects(hex.getBounds()))
            {
                hex.paint(g);
            }
        }

        Collection players = game.getPlayers();
        it = players.iterator();
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            java.util.List legions = player.getLegions();

            // Paint in reverse order to make visible z-order match clicks.
            ListIterator lit = legions.listIterator(legions.size());
            while (lit.hasPrevious())
            {
                Legion legion = (Legion)lit.previous();
                Marker marker = legion.getMarker();
                if (marker != null && rectClip.intersects(marker.getBounds()))
                {
                    marker.paintComponent(g);
                }
            }
        }
    }
    
    
    public Dimension getMinimumSize()
    {
        return new Dimension(64 * scale, 56 * scale);
    }


    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }


    public static void main(String [] args)
    {
        new MasterBoard(null);
    }
}
