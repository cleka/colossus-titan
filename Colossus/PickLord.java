import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */


public final class PickLord extends JDialog implements MouseListener,
    WindowListener
{
    private Player player;
    private Legion legion;
    private ArrayList chits = new ArrayList();
    private ArrayList lords = new ArrayList();
    private static Creature lord;


    private PickLord(JFrame parentFrame, Legion legion)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        lord = null;

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(new FlowLayout());

        pack();

        setBackground(Color.lightGray);

        // Need to use Critters instead of Creatures to display
        // titan power correctly.
        if (legion.numCreature(Creature.getCreatureByName("Titan")) > 0)
        {
            lords.add(legion.getCritter(Creature.getCreatureByName("Titan")));
        }
        if (legion.numCreature(Creature.getCreatureByName("Archangel")) > 0)
        {
            lords.add(legion.getCritter(Creature.getCreatureByName("Archangel")));
        }
        if (legion.numCreature(Creature.getCreatureByName("Angel")) > 0)
        {
            lords.add(legion.getCritter(Creature.getCreatureByName("Angel")));
        }

        setResizable(false);

        Iterator it = lords.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(4 * Scale.get(), critter.getImageName(),
                this);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    public static Creature pickLord(JFrame parentFrame, Legion legion)
    {
        new PickLord(parentFrame, legion);
        return lord;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            lord = (Creature)lords.get(i);
            dispose();
        }
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
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
}
