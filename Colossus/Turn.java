import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class Turn gets and holds chronological and sequence data for a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Turn extends JDialog implements ActionListener
{
    private static Game game;
    private JFrame parentFrame;
    private MasterBoard board;
    private Container contentPane;


    Turn(JFrame parentFrame, Game game, MasterBoard board)
    {
        super(parentFrame, game.getActivePlayer().getName() + " Turn " +
            game.getTurnNumber());

        this.parentFrame = parentFrame;
        this.game = game;
        this.board = board;

        setBackground(java.awt.Color.lightGray);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            
        // Place this window in the upper left corner.
        setLocation(new Point(0, 0));

        setupSplitDialog();

        setVisible(true);
    }

    
    void setupSplitDialog()
    {
System.out.println("setupSplitDialog");
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
            contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(0, 4));

            contentPane.add(new JLabel(game.getActivePlayer().getName() + 
                " : Split stacks "));
            JButton button1 = new JButton("Undo All Splits");
            contentPane.add(button1);
            button1.addActionListener(this);
            JButton button2 = new JButton("Withdraw from Game");
            contentPane.add(button2);
            button2.addActionListener(this);
            JButton button3 = new JButton("Done with Splits");
            contentPane.add(button3);
            button3.addActionListener(this);

            pack();


            // Highlight hexes with legions that are 7 high.
            player.highlightTallLegions();  
        }
    }


    void setupMoveDialog()
    {
System.out.println("setupMoveDialog");
        contentPane = getContentPane();
        contentPane.removeAll();
        Player player = game.getActivePlayer();
        if (player.getMulligansLeft() > 0)
        {
            contentPane.setLayout(new GridLayout(0, 6));
        }
        else
        {
            contentPane.setLayout(new GridLayout(0, 5));
        }

        player.rollMovement();

        contentPane.add(new JLabel(player.getName() + " : Movement Roll: " + 
            player.getMovementRoll() + " "));

        JButton button1 = new JButton("Undo Last Move");
        contentPane.add(button1);
        button1.addActionListener(this);

        JButton button2 = new JButton("Undo All Moves");
        contentPane.add(button2);
        button2.addActionListener(this);

        if (player.getMulligansLeft() > 0)
        {
            JButton button3 = new JButton("Take Mulligan");
            contentPane.add(button3);
            button3.addActionListener(this);
        }

        JButton button4 = new JButton("Withdraw from Game");
        contentPane.add(button4);
        button4.addActionListener(this);

        JButton button5 = new JButton("Done with Moves");
        contentPane.add(button5);
        button5.addActionListener(this);
           
        pack();

        // Highlight hexes with legions that can move.
        board.highlightUnmovedLegions();
    }

    
    void setupFightDialog()
    {
System.out.println("setupFightDialog");
        // Highlight hexes with engagements.
        // If there are no engagements, move forward to the muster phase.
        if (board.highlightEngagements() < 1)
        {
            game.advancePhase();
            setupMusterDialog();
        }
        else
        {
            contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(0, 2));

            contentPane.add(new JLabel(game.getActivePlayer().getName() + 
                " : Resolve Engagements "));
            JButton button1 = new JButton("Done with Engagements");
            contentPane.add(button1);
            button1.addActionListener(this);

            pack();
        }
    }


    void setupMusterDialog()
    {
System.out.println("setupMusterDialog");
        if (!game.getActivePlayer().isAlive())
        {
            game.advanceTurn();
            setupSplitDialog();
        }
        else
        {
            contentPane = getContentPane();
            contentPane.removeAll();
            contentPane.setLayout(new GridLayout(0, 5));

            contentPane.add(new JLabel(game.getActivePlayer().getName() + 
                " : Muster Recruits "));
            JButton button1 = new JButton("Undo Last Recruit");
            contentPane.add(button1);
            button1.addActionListener(this);
            JButton button2 = new JButton("Undo All Recruits");
            contentPane.add(button2);
            button2.addActionListener(this);
            JButton button3 = new JButton("Withdraw from Game");
            contentPane.add(button3);
            button3.addActionListener(this);
            JButton button4 = new JButton("End Turn");
            contentPane.add(button4);
            button4.addActionListener(this);

            pack();
        
            // Highlight hexes with legions eligible to muster.
            board.highlightPossibleRecruits();
        }
    }
    

    public void actionPerformed(ActionEvent e)
    {
        Player player = game.getActivePlayer();

        if (e.getActionCommand() == "Undo All Splits")
        {
            player.undoAllSplits();

            // Remove all moves from MasterBoard.
            board.repaint();
        }

        else if (e.getActionCommand() == "Done with Splits")
        {
            if (player.getMaxLegionHeight() > 7)
            {
                JOptionPane.showMessageDialog(parentFrame, "Must split.");
                return;
            }
            game.advancePhase();

            setupMoveDialog();
        }

        else if (e.getActionCommand() == "Undo Last Move")
        {
            player.undoLastMove();

            // Remove all moves from MasterBoard and show unmoved legions.
            board.highlightUnmovedLegions();
        }

        else if (e.getActionCommand() == "Undo All Moves")
        {
            player.undoAllMoves();

            // Remove all moves from MasterBoard and show unmoved legions.
            board.highlightUnmovedLegions();
        }

        else if (e.getActionCommand() == "Take Mulligan")
        {
            player.takeMulligan();

            if (player.getMulligansLeft() == 0)
            {
                // Remove the Take Mulligan button.
                setupMoveDialog();
            }
            // Remove all moves from MasterBoard.
            board.unselectAllHexes();
        }

        else if (e.getActionCommand() == "Done with Moves")
        {
            if (player.legionsMoved() == 0)
            {
                // XXX: Check for the wacky case where there are
                // no legal moves at all.
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
                            return;
                        }
                    }
                }

                game.advancePhase();

                setupFightDialog();
            }
        }

        else if (e.getActionCommand() == "Done with Engagements")
        {
            // Advance only if there are no unresolved engagements.
            if (board.highlightEngagements() == 0)
            {
                game.advancePhase();

                setupMusterDialog();
            }
        }
        
        else if (e.getActionCommand() == "Undo Last Recruit")
        {
            player.undoLastRecruit();

            board.highlightPossibleRecruits();
        }

        else if (e.getActionCommand() == "Undo All Recruits")
        {
            player.undoAllRecruits();

            board.highlightPossibleRecruits();
        }
        
        else if (e.getActionCommand() == "End Turn")
        {
            // Commit all moves.
            player.commitMoves();

            // Mulligans are only allowed on turn 1.
            player.setMulligansLeft(0);

            game.advanceTurn();

            setupSplitDialog();
        }

        else if (e.getActionCommand() == "Withdraw from Game")
        {
            int answer = JOptionPane.showConfirmDialog(parentFrame, 
                "Are you sure you want to withdraw from the game?");
            if (answer == JOptionPane.YES_OPTION)
            {
               game.getActivePlayer().die(null);

               game.advanceTurn();

               setupSplitDialog();
            }
        }
    }
}
