import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class MovementDie displays dice rolls during a battle.
 * @version $Id$
 * @author David Ripton
 */


public final class MovementDie extends JFrame implements WindowListener
{
    private Game game;
    private static final int scale = 60;
    private static Point location;


    public MovementDie(Game game)
    {
        super("Movement Die");
        this.game = game;
        setVisible(false);
        setupIcon();
        addWindowListener(this);
        pack();
        setBackground(Color.lightGray);
        setResizable(false);

        // Move dialog to saved location, or upper right of screen.
        if (location == null)
        {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            location = new Point(d.width - getSize().width, 0);
        }
        setLocation(location);
    }


    private String getDieImageName(int roll)
    {
        StringBuffer basename = new StringBuffer("Hit");
        basename.append(roll);

        return basename.toString();
    }


    /** Initialize and layout the components, in response to new data. */
    public void showRoll(int roll)
    {
        Chit die = new Chit(scale, getDieImageName(roll), this);
        Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.add(die);
        pack();
        setVisible(true);
        repaint();
    }


    private void setupIcon()
    {
        if (game != null && !game.isApplet())
        {
            try
            {
                setIconImage(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource(Chit.getImagePath(
                    Creature.colossus.getImageName()))));
            }
            catch (NullPointerException e)
            {
                Game.logError(e.toString() + " Couldn't find " +
                    Creature.colossus.getImageName());
                dispose();
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
        if (game != null)
        {
            game.setOption(Game.showDice, false);
        }
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


    /** Never grab the keyboard focus. */
    public boolean isFocusTraversable()
    {
        return false;
    }


    public static void main(String [] args)
    {
        MovementDie movementDie = new MovementDie(null);
        movementDie.showRoll(6);
    }
}
