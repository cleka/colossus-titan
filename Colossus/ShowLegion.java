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
        setLocation(new Point(point.x - scale, point.y - scale));
        setSize(2 * scale / 5 + scale * legion.getHeight(), 8 * scale / 5);
        
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
