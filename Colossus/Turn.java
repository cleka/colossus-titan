import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class Turn holds control buttons.
 * @version $Id$
 * @author David Ripton
 */

public class Turn extends JDialog implements ActionListener, WindowListener,
    KeyListener
{
    private Game game;
    private MasterBoard board;
    private static Point location;
    private JButton button;

    private static final String undoLastSplit =
        "<html>Undo <b>L</b>ast Split";
    private static final String undoAllSplits =
        "<html>Undo <b>A</b>ll Splits";
    private static final String withdrawFromGame =
        "<html><b>W</b>ithdraw from Game";
    private static final String doneWithSplits =
        "<html><b>D</b>one with Splits";

    private static final String undoLastMove = 
        "<html>Undo <b>L</b>ast Move";
    private static final String undoAllMoves = 
        "<html>Undo <b>A</b>ll Moves";
    private static final String takeMulligan = 
        "<html>Take <b>M</b>ulligan";
    private static final String doneWithMoves =
        "<html><b>D</b>one with Moves";

    private static final String doneWithEngagements = 
        "<html><b>D</b>one with Engagements";

    private static final String undoLastRecruit =
        "<html>Undo <b>L</b>ast Recruit";
    private static final String undoAllRecruits =
        "<html>Undo <b>A</b>ll Recruits";
    private static final String doneWithTurn =
        "<html><b>D</b>one with Turn";


    public Turn(Game game, MasterBoard board)
    {
        super(board, game.getActivePlayer().getName() + " Turn " +
            game.getTurnNumber());

        this.game = game;
        this.board = board;

        setBackground(Color.lightGray);

        addWindowListener(this);
        addKeyListener(this);
            
        // Move dialog to saved location, or just to the right of board.
        if (location == null)
        {
            location = board.getLocation();
            location.x += board.getSize().getWidth();
        }
        setLocation(location);

        setupSplitDialog();

        // Since we repeatedly call removeAll() and pack(), which reset
        // the window size, don't let the user resize the window. 
        setResizable(false);

        requestFocus();

        setVisible(true);
    }


    public static Point getSavedLocation()
    {
        if (location == null)
        {
            location = new Point();
        }
        return location;
    }

    
    private void setupSplitDialog()
    {
        Player player = game.getActivePlayer();
        setTitle(player.getName() + " Turn " + game.getTurnNumber());

        // If there are no markers available, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0)
        {
            advancePhase();
        }
        else
        {
            Container contentPane = getContentPane();
            contentPane.removeAll();

            contentPane.setLayout(new GridLayout(5, 0));

            contentPane.add(new JLabel(player.getName() + " : Split stacks "));
            button = new JButton(undoLastSplit);
            button.setMnemonic(KeyEvent.VK_L);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(undoAllSplits);
            button.setMnemonic(KeyEvent.VK_A);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(withdrawFromGame);
            button.setMnemonic(KeyEvent.VK_W);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(doneWithSplits);
            button.setMnemonic(KeyEvent.VK_D);
            contentPane.add(button);
            button.addActionListener(this);

            pack();

            // Highlight hexes with legions that are 7 high.
            player.highlightTallLegions();  
        }
    }


    private void setupMoveDialog()
    {
        Container contentPane = getContentPane();
        contentPane.removeAll();
        Player player = game.getActivePlayer();
        if (player.getMulligansLeft() > 0)
        {
            contentPane.setLayout(new GridLayout(6, 0));
        }
        else
        {
            contentPane.setLayout(new GridLayout(5, 0));
        }

        player.rollMovement();

        contentPane.add(new JLabel(player.getName() + " : Movement Roll: " + 
            player.getMovementRoll() + " "));

        button = new JButton(undoLastMove);
        button.setMnemonic(KeyEvent.VK_L);
        contentPane.add(button);
        button.addActionListener(this);

        button = new JButton(undoAllMoves);
        button.setMnemonic(KeyEvent.VK_A);
        contentPane.add(button);
        button.addActionListener(this);

        if (player.getMulligansLeft() > 0)
        {
            button = new JButton(takeMulligan);
            button.setMnemonic(KeyEvent.VK_M);
            contentPane.add(button);
            button.addActionListener(this);
        }

        button = new JButton(withdrawFromGame);
        button.setMnemonic(KeyEvent.VK_W);
        contentPane.add(button);
        button.addActionListener(this);

        button = new JButton(doneWithMoves);
        button.setMnemonic(KeyEvent.VK_D);
        contentPane.add(button);
        button.addActionListener(this);
           
        pack();

        // Highlight hexes with legions that can move.
        game.highlightUnmovedLegions();
    }

    
    private void setupFightDialog()
    {
        // Highlight hexes with engagements.
        // If there are no engagements, move forward to the muster phase.
        if (game.highlightEngagements() < 1)
        {
            advancePhase();
        }
        else
        {
            Container contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(2, 0));

            contentPane.add(new JLabel(game.getActivePlayer().getName() + 
                " : Resolve Engagements "));
            button = new JButton(doneWithEngagements);
            button.setMnemonic(KeyEvent.VK_D);
            contentPane.add(button);
            button.addActionListener(this);

            pack();
        }
    }


    private void setupMusterDialog()
    {
        if (game.getActivePlayer().isDead())
        {
            advancePhase();
        }
        else
        {
            Container contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(5, 0));

            contentPane.add(new JLabel(game.getActivePlayer().getName() + 
                " : Muster Recruits "));
            button = new JButton(undoLastRecruit);
            button.setMnemonic(KeyEvent.VK_L);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(undoAllRecruits);
            button.setMnemonic(KeyEvent.VK_A);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(withdrawFromGame);
            button.setMnemonic(KeyEvent.VK_W);
            contentPane.add(button);
            button.addActionListener(this);

            button = new JButton(doneWithTurn);
            button.setMnemonic(KeyEvent.VK_D);
            contentPane.add(button);
            button.addActionListener(this);

            pack();
        
            // Highlight hexes with legions eligible to muster.
            game.highlightPossibleRecruits();
        }
    }


    private void setupPhaseDialog()
    {
        switch (game.getPhase())
        {
            case Game.SPLIT:
                setupSplitDialog();
                break;
            case Game.MOVE:
                setupMoveDialog();
                break;
            case Game.FIGHT:
                setupFightDialog();
                break;
            case Game.MUSTER:
                setupMusterDialog();
                break;
            default:
                System.out.println("Bogus phase");
        }
    }


    private void advancePhase()
    {
        game.advancePhase();
        setupPhaseDialog();
        requestFocus();
    }
    

    // XXX Merge these with the ones in MasterBoard.
    public void actionPerformed(ActionEvent e)
    {
        Player player = game.getActivePlayer();

        if (e.getActionCommand().equals(undoLastSplit))
        {
            player.undoLastSplit();
            board.repaint();
        }
        
        else if (e.getActionCommand().equals(undoAllSplits))
        {
            player.undoAllSplits();
            board.repaint();
        }

        else if (e.getActionCommand().equals(doneWithSplits))
        {
            if (player.getMaxLegionHeight() > 7)
            {
                JOptionPane.showMessageDialog(board, "Must split.");
                return;
            }
            advancePhase();
        }

        else if (e.getActionCommand().equals(undoLastMove))
        {
            player.undoLastMove();

            // Remove all moves from MasterBoard and show unmoved legions.
            game.highlightUnmovedLegions();
        }

        else if (e.getActionCommand().equals(undoAllMoves))
        {
            player.undoAllMoves();

            // Remove all moves from MasterBoard and show unmoved legions.
            game.highlightUnmovedLegions();
        }

        else if (e.getActionCommand().equals(takeMulligan))
        {
            player.takeMulligan();

            if (player.getMulligansLeft() == 0)
            {
                // Remove the Take Mulligan button, and reroll movement die.
                setupMoveDialog();
            }
            // Remove all moves from MasterBoard.
            board.unselectAllHexes();
        }

        else if (e.getActionCommand().equals(doneWithMoves))
        {
            // XXX Save location before moving on to engagement phase,
            // so that BattleTurns will end up in the right place.
            // This is non-ideal; we really should save it after
            // every move, or pass a reference to Turn into BattleTurn
            // so it can call nonstatic getLocation() directly.
            location = getLocation();

            // If any legions has a legal non-teleport move, then the 
            // player must move at least one legion.
            if (player.legionsMoved() == 0 && 
                player.countMobileLegions() > 0)
            {
                // Highlight all unmoved legions, rather than the
                // locations to which the forced-to-move legion can move. 
                game.highlightUnmovedLegions();
                JOptionPane.showMessageDialog(board, 
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
                            JOptionPane.showMessageDialog(board, 
                                "Split legions must be separated.");
                            return;
                        }
                    }
                }

                advancePhase();
            }
        }

        else if (e.getActionCommand().equals(doneWithEngagements))
        {
            // Advance only if there are no unresolved engagements.
            if (game.highlightEngagements() == 0)
            {
                advancePhase();
            }
            else
            {
                JOptionPane.showMessageDialog(board, 
                    "Must Resolve Engagements."); 
            }
        }
        
        else if (e.getActionCommand().equals(undoLastRecruit))
        {
            player.undoLastRecruit();

            game.highlightPossibleRecruits();
        }

        else if (e.getActionCommand().equals(undoAllRecruits))
        {
            player.undoAllRecruits();

            game.highlightPossibleRecruits();
        }
        
        else if (e.getActionCommand().equals(doneWithTurn))
        {
            // Commit all moves.
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            player.setMulligansLeft(0);

            advancePhase();
        }

        else if (e.getActionCommand().equals(withdrawFromGame))
        {
            String [] options = new String[2];
            options[0] = "Yes";
            options[1] = "No";
            int answer = JOptionPane.showOptionDialog(board, 
                "Are you sure you with to withdraw from the game?",
                "Confirm Withdrawal?", 
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, 
                null, options, options[1]);

            if (answer == JOptionPane.YES_OPTION)
            {
               game.getActivePlayer().die(null, true);

               advancePhase();
            }
        }
    }


    /** Allow keyboard shortcuts for most button actions. */ 
    public void keyTyped(KeyEvent e)
    {
        char ch = e.getKeyChar();
        Game.logEvent("Turn got key input:" + ch);
    }

    public void keyPressed(KeyEvent e)
    {
    }
    
    public void keyReleased(KeyEvent e)
    {
    }
    

    public void windowActivated(WindowEvent e)
    {
        requestFocus();
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
        requestFocus();
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
        requestFocus();
    }


    public Dimension getMinimumSize()
    {
        int scale = MasterBoard.getScale();
        return new Dimension(12 * scale, 12 * scale);
    }


    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }


    /** Focus is required for keyboard shortcuts */
    public boolean isFocusTraversable()
    {
        return true;
    }
}
