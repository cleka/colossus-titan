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
        super(parentFrame, game.getActivePlayer().getName() + " Turn 1");

        this.parentFrame = parentFrame;
        this.game = game;
        this.board = board;

        setSize(300, 250);
        
        setupSplitDialog();

        setVisible(true);
    }

    
    void setupSplitDialog()
    {
        removeAll();
        setLayout(new GridLayout(0, 2));

        add(new Label(game.getActivePlayer().getName() + " : Split stacks"));
        Button button1 = new Button("Done with Splits");
        add(button1);
        button1.addActionListener(this);

        pack();

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));
    }


    void setupMoveDialog()
    {
        removeAll();
        Player player = game.getActivePlayer();
        if (player.mulligansLeft > 0)
        {
            setLayout(new GridLayout(0, 4));
        }
        else
        {
            setLayout(new GridLayout(0, 3));
        }

        player.movementRoll = (int) Math.ceil(6 * Math.random());

        add(new Label(player.getName() + " : Movement Roll: " + 
            player.movementRoll));
        Button button1 = new Button("Reset Moves");
        add(button1);
        button1.addActionListener(this);

        if (player.mulligansLeft > 0)
        {
            Button button2 = new Button("Take Mulligan");
            add(button2);
            button2.addActionListener(this);
        }

        Button button3 = new Button("Done with Moves");
        add(button3);
        button3.addActionListener(this);
           
        pack();

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));
    }

    
    public void actionPerformed(ActionEvent e)
    {
        Player player = game.getActivePlayer();

        if (e.getActionCommand() == "Done with Splits")
        {
            if (player.getMaxLegionHeight() > 7)
            {
                new MessageBox(parentFrame, "Must split.");
                return;
            }
            game.advancePhase();

            setupMoveDialog();
        }

        else if (e.getActionCommand() == "Reset Moves")
        {
            player.undoAllMoves();

            // Remove all moves from MasterBoard.
            board.unselectAllHexes();
            board.repaint();
        }

        else if (e.getActionCommand() == "Take Mulligan")
        {
            if (player.mulligansLeft > 0)
            {
                player.movementRoll = (int) Math.ceil(6 * Math.random());

                player.mulligansLeft--;
                if (player.mulligansLeft == 0)
                {
                    // Remove the Take Mulligan button.
                    setupMoveDialog();
                }
                // Remove all moves from MasterBoard.
                board.unselectAllHexes();
            }
        }

        else if (e.getActionCommand() == "Done with Moves")
        {
            if (player.legionsMoved() == 0)
            {
                // XXX: Check for the wacky case where there are
                // no legal moves at all.
            }

            else
            {
                // If two or more legions share the same hex, force a
                // move if one is legal.  Otherwise, recombine them.
            }

        }

        else if (e.getActionCommand() == "End Turn")
        {
            game.advanceTurn();
        }
    }
}
