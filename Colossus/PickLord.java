import java.awt.*;
import java.awt.event.*;

/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */


class PickLord extends Dialog implements MouseListener, WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Player player;
    private Legion legion;
    private Chit [] chits;
    private static final int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private int titans;
    private int angels;
    private int archangels;
    private Creature [] lords;


    PickLord(Frame parentFrame, Legion legion)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        setLayout(null);

        pack();

        setBackground(java.awt.Color.lightGray);

        setSize(5 * scale, 3 * scale);

        // XXX: This doesn't work under Solaris.
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        lords = new Creature[3];
        lords[0] = Creature.titan;
        lords[1] = Creature.archangel;
        lords[2] = Creature.angel;

        chits = new Chit[3];
        for (int i = 0; i < 3; i++)
        {
            chits[i] = new Chit(scale * (i + 1), scale, scale, 
                lords[i].getImageName(), this);
        }
        
        titans = legion.numCreature(Creature.titan);
        angels = legion.numCreature(Creature.angel); 
        if (angels > 1)
        {
            angels = 1;
        }
        int archangels = legion.numCreature(Creature.archangel);
        if (archangels > 1)
        {
            archangels = 1;
        }

        if (titans == 0)
        {
            chits[0].setDead(true);
        }
        if (archangels == 0)
        {
            chits[1].setDead(true);
        }
        if (angels == 0)
        {
            chits[2].setDead(true);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < 3; i++)
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


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        Rectangle rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
        d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        for (int i = 0; i < 3;  i++)
        {
            if (rectClip.intersects(chits[i].getBounds()))
            {
                chits[i].paint(offGraphics);
            }
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
        for (int i = 0; i < 3; i++)
        {
            if (chits[i].select(point) && ! chits[i].isDead())
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


    public void windowActivated(WindowEvent event)
    {
    }


    public void windowClosed(WindowEvent event)
    {
    }


    public void windowClosing(WindowEvent event)
    {
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
