import java.applet.*;
import java.awt.*;

/**
 * Class GameApplet allows launching Colossus as an applet.
 * @version $Id$ 
 * @author David Barr
 */


public class GameApplet extends Applet 
{
    public void init() 
    {
        setLayout(new BorderLayout());
        add(new GameButton(), BorderLayout.CENTER);
    }

    public void destroy() 
    {
        removeAll();
    }
}
