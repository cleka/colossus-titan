import java.awt.*;
import java.awt.event.*;

/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * author David Ripton
 */

class ShowLegion extends Dialog implements MouseListener, WindowListener
{
    MediaTracker tracker;
    boolean imagesLoaded;
    Legion legion;
    Chit [] chits;

    ShowLegion(Frame parentFrame, Legion legion, Point point)
    {
        super(parentFrame, "Contents of Legion " + legion.getMarkerId(), true);

        int scale = 60;
        setSize(2 * scale / 5 + scale * legion.getHeight(), 8 * scale / 5);

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
        
        setLayout(null);

        pack();
        addMouseListener(this);

        this.legion = legion;

        imagesLoaded = false;

        chits = new Chit[legion.getHeight()];
        for (int i = 0; i < legion.getHeight(); i++)
        {
            chits[i] = new Chit(i * scale + (scale / 5), scale / 2, scale, 
                legion.creatures[i].getImageName(), this);
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
            new MessageBox(parentFrame, "waitForAll was interrupted");
        }

        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        // No need to mess around with clipping rectangles, since
        //    this window is ephemeral.
        for (int i = legion.getHeight() - 1; i >= 0; i--)
        {
            chits[i].paint(g);
        }
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
        dispose();
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
