import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class ShowMasterHex displays the terrain type and recruits for a MasterHex.
 * @version $Id$
 * @author David Ripton
 */

public final class ShowMasterHex extends JDialog implements MouseListener,
    WindowListener
{
    public ShowMasterHex(JFrame parentFrame, Game game, MasterHex hex,
        Point point)
    {
        super(parentFrame, hex.getTerrainName() + " Hex " + hex.getLabel(),
            false);

        pack();
        setBackground(Color.lightGray);
        setResizable(false);
        addWindowListener(this);

        // Place dialog relative to parentFrame's origin, and fully on-screen.
        Point parentOrigin = parentFrame.getLocation();
        int scale = 4 * Scale.get();
        Point origin = new Point(point.x + parentOrigin.x - scale, point.y +
            parentOrigin.y - scale);
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

        contentPane.setLayout(new GridLayout(0, 3));

        char terrain = hex.getTerrain();
        ArrayList creatures = Game.getPossibleRecruits(terrain);
        Iterator it = creatures.iterator();
        boolean firstTime = true;
        Creature prevCreature = Creature.getCreatureByName("Titan");
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();

            Chit chit = new Chit(scale, creature.getImageName(), this);
            contentPane.add(chit);
            chit.addMouseListener(this);

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            }
            else
            {
                numToRecruit = Game.numberOfRecruiterNeeded(prevCreature,
                    creature, terrain);
            }

            JLabel numToRecruitLabel = new JLabel("", JLabel.CENTER);
            if (numToRecruit > 0 && numToRecruit <= 3)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
            }

            contentPane.add(numToRecruitLabel);
            numToRecruitLabel.addMouseListener(this);

            int count = game.getCaretaker().getCount(creature);
            JLabel countLabel = new JLabel(Integer.toString(count),
                JLabel.CENTER);
            contentPane.add(countLabel);
            countLabel.addMouseListener(this);

            prevCreature = creature;
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
}
