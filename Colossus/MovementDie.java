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
    private static Point location;
    private int lastRoll = 0;
    private Client client;


    public MovementDie(Client client)
    {
        super("Movement Die");
        this.client = client;
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
        lastRoll = roll;
        Chit die = new Chit(4 * Scale.get(), getDieImageName(roll), this);
        Container contentPane = getContentPane();
        contentPane.removeAll();
        contentPane.add(die);
        pack();
        setVisible(true);
        repaint();
    }

    public void rescale()
    {
        showRoll(lastRoll);
    }


    private void setupIcon()
    {
        try
        {
            setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource(Chit.getImagePath(
                Creature.colossus.getImageName()))));
        }
        catch (NullPointerException e)
        {
            Log.error(e.toString() + " Couldn't find " +
                Creature.colossus.getImageName());
            dispose();
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
        if (client != null)
        {
            client.setOption(Options.showDice, false);
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
        movementDie.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        movementDie.showRoll(6);
    }
}
