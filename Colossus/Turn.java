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

        // Place this window in the upper right corner.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width - getSize().width, 0));
        
        setLayout(new GridLayout(0, 2));

        add(new Label(game.player[game.activePlayer].name + 
            " : Split stacks"));
        Button button1 = new Button("Done with Splits");
        add(button1);
        button1.addActionListener(this);

        pack();
        setVisible(true);
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
        }

        else if (e.getActionCommand() == "Reset Moves")
        {
            for (int j = 0; j < game.player[game.activePlayer].numLegions; j++)
            {
                // Move this legion back to where it started.
            }
            game.player[game.activePlayer].legionsMoved = 0;
        }

        else if (e.getActionCommand() == "Mulligan")
        {
            if (game.player[game.activePlayer].mulligansLeft > 0)
            {
                game.player[game.activePlayer].mulligansLeft--;
                if (game.player[game.activePlayer].mulligansLeft == 0)
                {
                    // Remove the Mulligan button.

                }
                game.player[game.activePlayer].movementRoll = 
                    (int) Math.ceil(6 * Math.random());
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

        }
    }
}
