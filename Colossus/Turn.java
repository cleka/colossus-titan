import java.awt.*;
import java.awt.event.*;

/**
 * Class Turn gets and holds chronological and sequence data for a Titan game.
 * @version $Id$
 * @author David Ripton
 */

public class Turn extends Dialog implements ActionListener, WindowListener
{
    private static Game game;
    private static MasterBoard board;
    private static Point location;


    public Turn(Game game, MasterBoard board)
    {
        super(board, game.getActivePlayer().getName() + " Turn " +
            game.getTurnNumber());

        this.game = game;
        this.board = board;

        setBackground(Color.lightGray);

        addWindowListener(this);
            
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
            removeAll();
            setLayout(new GridLayout(5, 0));

            add(new Label(player.getName() + " : Split stacks "));
            Button button1 = new Button("Undo Last Split");
            add(button1);
            button1.addActionListener(this);
            Button button2 = new Button("Undo All Splits");
            add(button2);
            button2.addActionListener(this);
            Button button3 = new Button("Withdraw from Game");
            add(button3);
            button3.addActionListener(this);
            Button button4 = new Button("Done with Splits");
            add(button4);
            button4.addActionListener(this);

            pack();

            // Highlight hexes with legions that are 7 high.
            player.highlightTallLegions();  
        }
    }


    private void setupMoveDialog()
    {
        removeAll();
        Player player = game.getActivePlayer();
        if (player.getMulligansLeft() > 0)
        {
            setLayout(new GridLayout(6, 0));
        }
        else
        {
            setLayout(new GridLayout(5, 0));
        }

        player.rollMovement();

        add(new Label(player.getName() + " : Movement Roll: " + 
            player.getMovementRoll() + " "));

        Button button1 = new Button("Undo Last Move");
        add(button1);
        button1.addActionListener(this);

        Button button2 = new Button("Undo All Moves");
        add(button2);
        button2.addActionListener(this);

        if (player.getMulligansLeft() > 0)
        {
            Button button3 = new Button("Take Mulligan");
            add(button3);
            button3.addActionListener(this);
        }

        Button button4 = new Button("Withdraw from Game");
        add(button4);
        button4.addActionListener(this);

        Button button5 = new Button("Done with Moves");
        add(button5);
        button5.addActionListener(this);
           
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
            removeAll();
            setLayout(new GridLayout(2, 0));

            add(new Label(game.getActivePlayer().getName() + 
                " : Resolve Engagements "));
            Button button1 = new Button("Done with Engagements");
            add(button1);
            button1.addActionListener(this);

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
            removeAll();
            setLayout(new GridLayout(5, 0));

            add(new Label(game.getActivePlayer().getName() + 
                " : Muster Recruits "));
            Button button1 = new Button("Undo Last Recruit");
            add(button1);
            button1.addActionListener(this);
            Button button2 = new Button("Undo All Recruits");
            add(button2);
            button2.addActionListener(this);
            Button button3 = new Button("Withdraw from Game");
            add(button3);
            button3.addActionListener(this);
            Button button4 = new Button("End Turn");
            add(button4);
            button4.addActionListener(this);

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
    }
    

    public void actionPerformed(ActionEvent e)
    {
        Player player = game.getActivePlayer();

        if (e.getActionCommand().equals("Undo Last Split"))
        {
            player.undoLastSplit();
            board.repaint();
        }
        
        else if (e.getActionCommand().equals("Undo All Splits"))
        {
            player.undoAllSplits();
            board.repaint();
        }

        else if (e.getActionCommand().equals("Done with Splits"))
        {
            if (player.getMaxLegionHeight() > 7)
            {
                new MessageBox(board, "Must split.");
                return;
            }
            advancePhase();
        }

        else if (e.getActionCommand().equals("Undo Last Move"))
        {
            player.undoLastMove();

            // Remove all moves from MasterBoard and show unmoved legions.
            game.highlightUnmovedLegions();
        }

        else if (e.getActionCommand().equals("Undo All Moves"))
        {
            player.undoAllMoves();

            // Remove all moves from MasterBoard and show unmoved legions.
            game.highlightUnmovedLegions();
        }

        else if (e.getActionCommand().equals("Take Mulligan"))
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

        else if (e.getActionCommand().equals("Done with Moves"))
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
                new MessageBox(board, "At least one legion must move.");
                return;
            }

            else
            {
                // If two or more legions share the same hex, force a
                // move if one is legal.  Otherwise, recombine them.
                for (int i = 0; i < player.getNumLegions(); i++)
                {
                    Legion legion = player.getLegion(i);
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
                            // Highlight all unmoved legions, rather than the
                            // locations to which the forced-to-move legion 
                            // can move. 
                            game.highlightUnmovedLegions();
                            new MessageBox(board, 
                                "Split legions must be separated.");
                            return;
                        }
                    }
                }

                advancePhase();
            }
        }

        else if (e.getActionCommand().equals("Done with Engagements"))
        {
            // Advance only if there are no unresolved engagements.
            if (game.highlightEngagements() == 0)
            {
                advancePhase();
            }
            else
            {
                new MessageBox(board, "Must Resolve Engagements."); 
            }
        }
        
        else if (e.getActionCommand().equals("Undo Last Recruit"))
        {
            player.undoLastRecruit();

            game.highlightPossibleRecruits();
        }

        else if (e.getActionCommand().equals("Undo All Recruits"))
        {
            player.undoAllRecruits();

            game.highlightPossibleRecruits();
        }
        
        else if (e.getActionCommand().equals("End Turn"))
        {
            // Commit all moves.
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            player.setMulligansLeft(0);

            advancePhase();
        }

        else if (e.getActionCommand().equals("Withdraw from Game"))
        {
            new OptionDialog(board, "Confirm Withdrawal", 
                "Are you sure you with to withdraw from the game?",
                "Yes", "No");
            if (OptionDialog.getLastAnswer() == OptionDialog.YES_OPTION)
            {
               game.getActivePlayer().die(null, true);

               advancePhase();
            }
        }
    }


    public void windowActivated(WindowEvent e)
    {
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
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
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
}
