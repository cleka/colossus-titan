import java.awt.*;
import java.awt.event.*;

/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

public class ShowLegion extends Dialog implements MouseListener, WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Legion legion;
    private Chit [] chits;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    public ShowLegion(Frame parentFrame, Legion legion, Point point, boolean
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

        setLayout(new FlowLayout());

        this.legion = legion;

        chits = new Chit[legion.getHeight()];

        for (int i = 0; i < legion.getHeight(); i++)
        {
            Critter critter = legion.getCritter(i);
            String imageName;
            if (!allVisible && !critter.isVisible())
            {
                imageName = "images/Question.gif"; 
            }
            else
            {
                imageName = critter.getImageName();
            }

            chits[i] = new Chit(scale, imageName, this);
            add(chits[i]);
            chits[i].addMouseListener(this);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < legion.getHeight(); i++)
        {
            tracker.addImage(chits[i].getImage(), 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, e.toString() +
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

        pack();

        addMouseListener(this);

        setVisible(true);
        repaint();
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
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
