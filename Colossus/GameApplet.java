import java.awt.*;
import javax.swing.*;

/**
 * Class GameApplet allows launching Colossus as an applet.
 * @version $Id$
 * @author David Barr
 */


public final class GameApplet extends JApplet
{
    public void init()
    {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new GameButton(this), BorderLayout.CENTER);
    }

    public void destroy()
    {
        stop();
        Container contentPane = getContentPane();
        contentPane.removeAll();
    }
}
