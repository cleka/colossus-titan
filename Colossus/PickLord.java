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
    private Creature [] lords;


    PickLord(Frame parentFrame, Legion legion)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        setLayout(new FlowLayout());

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
            add(chits[i]);
            chits[i].addMouseListener(this);
        }
        
        tracker = new MediaTracker(this);

        for (int i = 0; i < chits.length; i++)
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
