import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * author David Ripton
 */

class ShowLegion extends JDialog implements MouseListener, WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Legion legion;
    private Chit [] chits;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    ShowLegion(JFrame parentFrame, Legion legion, Point point)
    {
        super(parentFrame, "Contents of Legion " + legion.getMarkerId(), false);

        int scale = 60;

        pack();

        setBackground(java.awt.Color.lightGray);
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
        
        getContentPane().setLayout(null);

        this.legion = legion;

        chits = new Chit[legion.getHeight()];
        for (int i = 0; i < legion.getHeight(); i++)
        {
            chits[i] = new Chit(i * scale + (scale / 5), scale / 2, scale, 
                legion.getCreature(i).getImageName(), this, false);
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
            JOptionPane.showMessageDialog(parentFrame, 
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

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

        for (int i = 0; i < legion.getHeight(); i++)
        {
            chits[i].paint(offGraphics);
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    // Attempt to free resources to work around Java memory leaks.
    private void cleanup()
    {
        setVisible(false);

        if (offImage != null)
        {
            offImage.flush();
            offGraphics.dispose();
        }
        
        if (imagesLoaded)
        {
            for (int i = 0; i < legion.getHeight(); i++)
            {
                tracker.removeImage(chits[i].getImage());
                chits[i].getImage().flush();
            }
        }

        dispose();
        System.gc();
    }  
    

    public void mouseClicked(MouseEvent e)
    {
        cleanup();
    }
    
    
    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
        cleanup();
    }
    

    public void mousePressed(MouseEvent e)
    {
        cleanup();
    }

    
    public void mouseReleased(MouseEvent e)
    {
        cleanup();
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        cleanup();
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
