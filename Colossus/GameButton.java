import java.applet.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.text.*;

/**
 * Class GameButton launches Colossus as an applet.
 * @version $Id$
 * @author David Barr
 */

class GameButton extends Button implements ActionListener 
{
    GameButton() 
    {
        super("Start");
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) 
    {
        Game g = new Game(true);
    }
}
