import java.awt.*;
import java.awt.event.*;

/**
 * Class Turn gets and holds chronological and sequence data for a Titan game.
 * @version $Id$
 * author David Ripton
 */

class Turn extends Dialog implements ActionListener
{
    static Game game;
    Frame parentFrame;
    MasterBoard board;

    Turn(Frame parentFrame, Game game, MasterBoard board)
    {
        super(parentFrame, game.getActivePlayer().getName() + " Turn " +
            game.getTurnNumber());

        this.parentFrame = parentFrame;
        this.game = game;
        this.board = board;

        setSize(300, 250);
        
        setupSplitDialog();

        setVisible(true);
    }

    
    void setupSplitDialog()
    {
        Player player = game.getActivePlayer();

        // If there are no markers available, skip forward to movement.
        if (player.getNumMarkersAvailable() == 0)
        {
            game.advancePhase();
            setupMoveDialog();
        }
        else
        {
            setTitle(player.getName() + " Turn " + game.getTurnNumber());
            removeAll();
            setLayout(new GridLayout(0, 3));

            add(new Label(game.getActivePlayer().getName() + 
                " : Split stacks"));
            Button button1 = new Button("Undo All Splits");
            add(button1);
            button1.addActionListener(this);
            Button button2 = new Button("Done with Splits");
            add(button2);
            button2.addActionListener(this);

            pack();

            // Place this window in the upper right corner.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width - getSize().width, 0));

            // Highlight hexes with 7 high legions.
            for (int i = 0; i < player.getNumLegions(); i++)
            {
                Legion legion = player.legions[i];
                if (legion.getHeight() >= 7)
                {
                    MasterHex hex = legion.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
        }
    }


    void setupMoveDialog()
    {
        removeAll();
        Player player = game.getActivePlayer();
        if (player.getMulligansLeft() > 0)
        {
            setLayout(new GridLayout(0, 5));
        }
        else
        {
            setLayout(new GridLayout(0, 4));
        }

        player.rollMovementDie();

        add(new Label(player.getName() + " : Movement Roll: " + 
            player.getMovementRoll()));

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

        Button button4 = new Button("Done with Moves");
        add(button4);
        button4.addActionListener(this);
           
        pack();

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));

        // Highlight hexes with legions that can move.
        board.highlightUnmovedLegions();
    }

    
    void setupFightDialog()
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
                " : Resolve Engagements"));
            Button button1 = new Button("Done with Engagements");
            add(button1);
            button1.addActionListener(this);

            pack();

            // Place this window in the upper right corner.
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width - getSize().width, 0));
        }
    }


    void setupMusterDialog()
    {
        removeAll();
        setLayout(new GridLayout(0, 2));

        add(new Label(game.getActivePlayer().getName() + " : Muster Recruits"));
        Button button1 = new Button("End Turn");
        add(button1);
        button1.addActionListener(this);

        pack();

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));
        
        // Highlight hexes with legions eligible to muster.

        Player player = game.getActivePlayer();
        for (int i = 0; i < player.getNumLegions(); i++)
        {
            Legion legion = player.legions[i];
            if (legion.getHeight() < 7 && legion.hasMoved())
            {
                Creature [] recruits = new Creature[5];
                if (PickRecruit.findEligibleRecruits(legion, recruits) >= 1)
                {
                    MasterHex hex = legion.getCurrentHex();
                    hex.select();
                    hex.repaint();
                }
            }
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
                new MessageBox(parentFrame, "Must split.");
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
                // XXX: If two or more legions share the same hex, force a
                // move if one is legal.  Otherwise, recombine them.

                for (int i = 0; i < player.getNumLegions(); i++)
                {
                    Legion legion = player.legions[i];
                    MasterHex hex = legion.getCurrentHex();
                    if (hex.getNumFriendlyLegions(player) > 1)
                    {
                        // If there are no legal moves, recombine.
                        if (board.showMoves(legion, player) == 0)
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
        
        else if (e.getActionCommand() == "End Turn")
        {
            // Commit all moves.
            player.commitMoves();

            game.advanceTurn();

            setupSplitDialog();
        }
    }
}
