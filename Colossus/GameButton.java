import java.awt.event.*;
import java.awt.*;

/**
 * Class GameButton launches Colossus as an applet.
 * @version $Id$
 * @author David Barr
 */

class GameButton extends Button implements ActionListener 
{
    Game game;


    GameButton() 
    {
        super("Start");
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) 
    {
        // Don't allow multiple clicks to start multiple simultaneous games.
        if (game == null)
        {
            game = new Game(true);
        }
    }
}
