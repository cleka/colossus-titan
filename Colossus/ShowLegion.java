import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

public final class ShowLegion extends JDialog implements MouseListener,
    WindowListener
{
    private static final int scale = 60;


    public ShowLegion(JFrame parentFrame, Legion legion, Point point, boolean
        allStacksVisible)
    {
        super(parentFrame, "Legion " + legion.getLongMarkerName(), false);

        pack();
        setBackground(Color.lightGray);
        setResizable(false);
        addWindowListener(this);

        // Place dialog relative to parentFrame's origin, and fully on-screen.
        Point parentOrigin = parentFrame.getLocation();
        Point origin = new Point(point.x + parentOrigin.x - scale, point.y +
            parentOrigin.y);
        if (origin.x < 0)
        {
            origin.x = 0;
        }
        if (origin.y < 0)
        {
            origin.y = 0;
        }
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int adj = origin.x + getSize().width - d.width;
        if (adj > 0)
        {
            origin.x -= adj;
        }
        adj = origin.y + getSize().height - d.height;
        if (adj > 0)
        {
            origin.y -= adj;
        }
        setLocation(origin);

        Container contentPane = getContentPane();

        contentPane.setLayout(new FlowLayout());

        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            String imageName;
            if (!allStacksVisible && !critter.isVisible())
            {
                imageName = "Unknown";
            }
            else
            {
                imageName = critter.getImageName();
            }

            Chit chit = new Chit(scale, imageName, this);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();

        addMouseListener(this);

        setVisible(true);
        repaint();
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mousePressed(MouseEvent e)
    {
        dispose();
    }


    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
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
        JFrame frame = new JFrame("testing ShowLegion");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('B');
        hex.setLabel(130);

        Player player = new Player("Test", null);
        Legion legion = new Legion("Bk01", null, hex.getLabel(),
            hex.getLabel(), Creature.titan, Creature.gargoyle,
            Creature.gargoyle, Creature.cyclops, Creature.cyclops, null,
            null, null, player);
        Point point = new Point(10 * scale, 10 * scale);

        new ShowLegion(frame, legion, point, true);
    }
}
