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

    Turn(Frame parentFrame, Game game)
    {
        super(parentFrame, game.player[0].name + " Turn 1");

        this.game = game;
        this.parentFrame = parentFrame;

        setSize(300, 250);
        
        setupSplitDialog();

        setVisible(true);
    }

    
    void setupSplitDialog()
    {
        removeAll();
        setLayout(new GridLayout(0, 2));

        add(new Label(game.player[game.activePlayer].name + 
            " : Split stacks"));
        Button button1 = new Button("Done with Splits");
        add(button1);
        button1.addActionListener(this);

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));

        pack();
    }


    void setupMoveDialog()
    {
        removeAll();
        if (game.player[game.activePlayer].mulligansLeft > 0) 
        {
            setLayout(new GridLayout(0, 4));
        }
        else
        {
            setLayout(new GridLayout(0, 3));
        }

        game.player[game.activePlayer].movementRoll = 
            (int) Math.ceil(6 * Math.random());

        add(new Label(game.player[game.activePlayer].name + 
            " : Movement Roll: " + 
            game.player[game.activePlayer].movementRoll));
        Button button1 = new Button("Reset Moves");
        add(button1);
        button1.addActionListener(this);

        if (game.player[game.activePlayer].mulligansLeft > 0)
        {
            Button button2 = new Button("Take Mulligan");
            add(button2);
            button2.addActionListener(this);
        }

        Button button3 = new Button("Done with Moves");
        add(button3);
        button3.addActionListener(this);
           
        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));

        pack();
    }

    
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Done with Splits")
        {
            for (int j = 0; j < game.player[game.activePlayer].numLegions; j++)
            {
                if (game.player[game.activePlayer].legions[j].height > 7)
                {
                    new MessageBox(parentFrame, "Must split.");
                    return;
                }
            }
            game.phase++;

            setupMoveDialog();
        }

        else if (e.getActionCommand() == "Reset Moves")
        {
            for (int j = 0; j < game.player[game.activePlayer].numLegions; j++)
            {
                // Move this legion back to where it started.
            }
            game.player[game.activePlayer].legionsMoved = 0;
        }

        else if (e.getActionCommand() == "Take Mulligan")
        {
            if (game.player[game.activePlayer].mulligansLeft > 0)
            {
                game.player[game.activePlayer].movementRoll = 
                    (int) Math.ceil(6 * Math.random());

                game.player[game.activePlayer].mulligansLeft--;
                if (game.player[game.activePlayer].mulligansLeft == 0)
                {
                    // Remove the Take Mulligan button.
                    setupMoveDialog();
                }
            }
        }

        else if (e.getActionCommand() == "Done with Moves")
        {
            if (game.player[game.activePlayer].legionsMoved == 0)
            {
                // Check for the wacky case where there are
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
            game.activePlayer++;
            game.activePlayer %= game.numPlayers;
            game.phase = 1;
        }
    }
}
