import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

public class ShowLegion extends JDialog implements MouseListener, WindowListener
{
    private Legion legion;
    private Chit [] chits;


    public ShowLegion(JFrame parentFrame, Legion legion, Point point, boolean
        allVisible)
    {
        super(parentFrame, "Contents of Legion " + legion.getMarkerId(), false);

        int scale = 60;

        pack();
        setBackground(Color.lightGray);
        setResizable(false);
        addWindowListener(this);

        // Place dialog relative to parentFrame's origin, and fully on-screen.
        Point parentOrigin = parentFrame.getLocation();
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

        contentPane.setLayout(new FlowLayout());

        this.legion = legion;

        chits = new Chit[legion.getHeight()];

        for (int i = 0; i < legion.getHeight(); i++)
        {
            Critter critter = legion.getCritter(i);
            String imageName;
            if (!allVisible && !critter.isVisible())
            {
                imageName = Chit.getImagePath("Question");
            }
            else
            {
                imageName = critter.getImageName();
            }

            chits[i] = new Chit(scale, imageName, this);
            contentPane.add(chits[i]);
            chits[i].addMouseListener(this);
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
