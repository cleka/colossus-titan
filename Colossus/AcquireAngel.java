import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public class AcquireAngel extends JDialog implements MouseListener,
    WindowListener
{
    private int numEligible;
    private Creature [] recruits;
    private Player player;
    private Legion legion;
    private Chit [] chits;


    public AcquireAngel(JFrame parentFrame, Legion legion, boolean archangel)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Acquire Angel", true);

        this.legion = legion;
        player = legion.getPlayer();

        recruits = new Creature[2];
        chits = new Chit[2];

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

        for (int i = 0; i < numEligible; i++)
        {
            chits[i] = new Chit(scale, recruits[i].getImageName(), this);
            contentPane.add(chits[i]);
            chits[i].addMouseListener(this);
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
        for (int i = 0; i < numEligible; i++)
        {
            if (chits[i] == source)
            {
                // Select that marker.
                Creature creature = recruits[i];
                legion.addCreature(creature, true);

                Game.logEvent("Legion " + legion.getMarkerId() +
                    " acquired an " + creature.getName());

                // Then exit.
                dispose();
                return;
            }
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
