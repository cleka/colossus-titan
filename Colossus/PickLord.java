import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */


public class PickLord extends JDialog implements MouseListener, WindowListener
{
    private Player player;
    private Legion legion;
    private Chit [] chits;
    private static final int scale = 60;
    private Creature [] lords;


    public PickLord(JFrame parentFrame, Legion legion)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(new FlowLayout());

        pack();

        setBackground(Color.lightGray);

        lords = new Critter[3];

        int numLordTypes = 0;
        if (legion.numCreature(Creature.titan) > 0)
        {
            lords[numLordTypes] = legion.getCritter(Creature.titan);
            numLordTypes++;
        }
        if (legion.numCreature(Creature.archangel) > 0)
        {
            lords[numLordTypes] = legion.getCritter(Creature.archangel);
            numLordTypes++;
        }
        if (legion.numCreature(Creature.angel) > 0)
        {
            lords[numLordTypes] = legion.getCritter(Creature.angel);
            numLordTypes++;
        }
        
        setResizable(false);

        chits = new Chit[numLordTypes];

        for (int i = 0; i < chits.length; i++)
        {
            chits[i] = new Chit(scale, lords[i].getImageName(), this);
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
        for (int i = 0; i < chits.length; i++)
        {
            if (chits[i] == source)
            {
                legion.revealCreatures(lords[i], 1);
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
