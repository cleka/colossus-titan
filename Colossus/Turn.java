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
    private Frame parentFrame;
    private MasterBoard board;


    public Turn(Frame parentFrame, Game game, MasterBoard board)
    {
        super(parentFrame, game.getActivePlayer().getName() + " Turn " +
            game.getTurnNumber());

        this.parentFrame = parentFrame;
        this.game = game;
        this.board = board;

        setBackground(Color.lightGray);
            
        // Place this window in the upper left corner.
        setLocation(new Point(0, 0));

        setupSplitDialog();

        setVisible(true);
    }

    
    private void setupSplitDialog()
    {
        Player player = game.getActivePlayer();
        setTitle(player.getName() + " Turn " + game.getTurnNumber());

        // If there are no markers available, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0)
        {
            game.advancePhase();
            setupMoveDialog();
        }
        else
        {
            removeAll();
            setLayout(new GridLayout(0, 5));

            add(new Label(game.getActivePlayer().getName() + 
                " : Split stacks "));
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
            setLayout(new GridLayout(0, 6));
        }
        else
        {
            setLayout(new GridLayout(0, 5));
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
        board.highlightUnmovedLegions();
    }

    
    private void setupFightDialog()
    {
        // Highlight hexes with engagements.
        // If there are no engagements, move forward to the muster phase.
        if (board.highlightEngagements() < 1)
        {
            game.advancePhase();
            setupMusterDialog();
        }
        else
        {
            removeAll();
            setLayout(new GridLayout(0, 2));

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
        if (!game.getActivePlayer().isAlive())
        {
            game.advanceTurn();
            setupSplitDialog();
        }
        else
        {
            removeAll();
            setLayout(new GridLayout(0, 5));

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
            board.highlightPossibleRecruits();
        }
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
                new MessageBox(parentFrame, "Must split.");
                return;
            }
            game.advancePhase();

            setupMoveDialog();
        }

        else if (e.getActionCommand().equals("Undo Last Move"))
        {
            player.undoLastMove();

            // Remove all moves from MasterBoard and show unmoved legions.
            board.highlightUnmovedLegions();
        }

        else if (e.getActionCommand().equals("Undo All Moves"))
        {
            player.undoAllMoves();

            // Remove all moves from MasterBoard and show unmoved legions.
            board.highlightUnmovedLegions();
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
            // If any legions has a legal non-teleport move, then the 
            // player must move at least one legion.
            if (player.legionsMoved() == 0 && 
                player.countMobileLegions() > 0)
            {
                // Highlight all unmoved legions, rather than the
                // locations to which the forced-to-move legion can move. 
                board.highlightUnmovedLegions();
                new MessageBox(parentFrame, "At least one legion must move.");
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
                        if (board.showMoves(legion) == 0)
                        {
                            for (int j = hex.getNumLegions() - 1; j >= 1; j--)
                            {
                                hex.getLegion(j).recombine(hex.getLegion(0));
                            }
                            hex.repaint();
                        }
                        else
                        {
                            // Highlight all unmoved legions, rather than the
                            // locations to which the forced-to-move legion 
                            // can move. 
                            board.highlightUnmovedLegions();
                            new MessageBox(parentFrame, 
                                "Split legions must be separated.");
                            return;
                        }
                    }
                }

                game.advancePhase();

                setupFightDialog();
            }
        }

        else if (e.getActionCommand().equals("Done with Engagements"))
        {
            // Advance only if there are no unresolved engagements.
            if (board.highlightEngagements() == 0)
            {
                game.advancePhase();

                setupMusterDialog();
            }
            else
            {
                new MessageBox(parentFrame, "Must Resolve Engagements.");
            }
        }
        
        else if (e.getActionCommand().equals("Undo Last Recruit"))
        {
            player.undoLastRecruit();

            board.highlightPossibleRecruits();
        }

        else if (e.getActionCommand().equals("Undo All Recruits"))
        {
            player.undoAllRecruits();

            board.highlightPossibleRecruits();
        }
        
        else if (e.getActionCommand().equals("End Turn"))
        {
            // Commit all moves.
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            player.setMulligansLeft(0);

            game.advanceTurn();

            setupSplitDialog();
        }

        else if (e.getActionCommand().equals("Withdraw from Game"))
        {
            new OptionDialog(parentFrame, "Confirm Withdrawal", 
                "Are you sure you with to withdraw from the game?",
                "Yes", "No");
            if (OptionDialog.getLastAnswer() == OptionDialog.YES_OPTION)
            {
               game.getActivePlayer().die(null, true);

               game.advanceTurn();

               setupSplitDialog();
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
}
