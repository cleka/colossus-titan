import java.awt.*;
import java.awt.event.*;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * author David Ripton
 */


class AcquireAngel extends Dialog implements MouseListener, WindowListener
{
    private int numEligible = 0;
    private Creature [] recruits;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Player player;
    private Legion legion;
    private Chit [] markers;


    AcquireAngel(Frame parentFrame, Legion legion, boolean archangel)
    {
        super(parentFrame, legion.getPlayer().getName() + 
            ": Acquire Angel", true);
        
        setResizable(false);

        this.legion = legion;
        player = legion.getPlayer();

        recruits = new Creature[2];
        markers = new Chit[2];

        addMouseListener(this);
        addWindowListener(this);

        int scale = 60;
        setLayout(null);
        setBackground(java.awt.Color.lightGray);

        numEligible = findEligibleRecruits(legion, recruits, archangel);
        if (numEligible == 0)
        {
            dispose();
            return;
        }

        setSize(scale * (numEligible + 1), (21 * scale / 10));
            
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, 
            d.height / 2 - getSize().height / 2));

        int cx = scale / 2;
        int cy = scale * 2 / 3;

        for (int i = 0; i < numEligible; i++)
        {
            markers[i] = new Chit(cx + i * (21 * scale / 20), cy, scale,
                recruits[i].getImageName(), this, false);
        }

        imagesLoaded = false;
        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(markers[i].getImage(), 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, "waitForAll was interrupted");
        }

        pack();
        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    // Returns the number of eligible recruits.
    static int findEligibleRecruits(Legion legion, Creature [] recruits,
        boolean archangel)
    {
        if (legion.getHeight() > 6)
        {
            return 0;
        }

        recruits[0] = Creature.angel;
        if (archangel)
        {
            recruits[1] = Creature.archangel;
        }

        // Check for availability of chits.
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null && recruits[i].getCount() < 1)
            {
                recruits[i] = null;
            }
        }

        // Pack the recruits array for display.
        for (int i = 0; i < recruits.length - 1; i++)
        {
            if (recruits[i] == null)
            {
                for (int j = i; j < recruits.length - 1; j++)
                {
                    recruits[j] = recruits[j + 1];
                }
                recruits[recruits.length - 1] = null;
            }
        }

        int count = 0;
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null)
            {
                count++;
            }
        }
        return count;
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Rectangle rectClip = g.getClipBounds();

        for (int i = numEligible - 1; i >= 0; i--)
        {
            if (rectClip.intersects(markers[i].getBounds()))
            {
                markers[i].paint(g);
            }
        }
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < numEligible; i++)
        {
            if (markers[i].select(point))
            {
                // Select that marker.
                legion.addCreature(recruits[i]);

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


    public void windowActivated(WindowEvent event)
    {
    }

    public void windowClosed(WindowEvent event)
    {
    }

    public void windowClosing(WindowEvent event)
    {
        dispose();
    }

    public void windowDeactivated(WindowEvent event)
    {
    }
                                                         
    public void windowDeiconified(WindowEvent event)
    {
    }

    public void windowIconified(WindowEvent event)
    {
    }

    public void windowOpened(WindowEvent event)
    {
    }
}
