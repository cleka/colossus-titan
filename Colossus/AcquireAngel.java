import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public class AcquireAngel extends JDialog implements MouseListener,
    WindowListener
{
    private int numEligible;
    private ArrayList recruits = new ArrayList();
    private Player player;
    private Legion legion;
    private ArrayList chits = new ArrayList();


    public AcquireAngel(JFrame parentFrame, Legion legion, boolean archangel)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Acquire Angel", true);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        int scale = 60;

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        numEligible = Game.findEligibleAngels(legion, recruits, archangel);
        if (numEligible == 0)
        {
            dispose();
            return;
        }

        pack();
        setBackground(Color.lightGray);
        setResizable(false);

        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            Chit chit = new Chit(scale, recruit.getImageName(), this);
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


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            // Select that marker.
            Creature creature = (Creature)recruits.get(i);
            legion.addCreature(creature, true);

            Game.logEvent("Legion " + legion.getMarkerId() +
                " acquired an " + creature.getName());

            // Then exit.
            dispose();
            return;
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
}
