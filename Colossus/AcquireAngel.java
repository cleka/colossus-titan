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
    private boolean imagesLoaded = false;
    private Player player;
    private Legion legion;
    private Chit [] chits;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    AcquireAngel(Frame parentFrame, Legion legion, boolean archangel)
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
        setLayout(new FlowLayout());

        numEligible = findEligibleRecruits(legion, recruits, archangel);
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
            chits[i] = new Chit(-1, -1, scale, recruits[i].getImageName(), 
                this);
            add(chits[i]);
            chits[i].addMouseListener(this);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
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
                " waitForAll was interrupted");
        }

        imagesLoaded = true;
        pack();
        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

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


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < numEligible; i++)
        {
            if (chits[i].select(point))
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
