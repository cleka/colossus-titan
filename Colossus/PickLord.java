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
    private static final int scale = 60;
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
        if (legion.numCreature(Creature.titan) > 0)
        {
            lords.add(legion.getCritter(Creature.titan));
        }
        if (legion.numCreature(Creature.archangel) > 0)
        {
            lords.add(legion.getCritter(Creature.archangel));
        }
        if (legion.numCreature(Creature.angel) > 0)
        {
            lords.add(legion.getCritter(Creature.angel));
        }

        setResizable(false);

        Iterator it = lords.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
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


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing PickLord");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('B');

        Player player = new Player("Test", null);
        Legion legion = new Legion("Bk01", null, hex, hex,
            Creature.titan, Creature.archangel, Creature.archangel,
            Creature.angel, Creature.angel, Creature.warlock,
            Creature.guardian, null, player);

        Creature creature = PickLord.pickLord(frame, legion);
        System.out.println("Chose " + creature);
    }
}
