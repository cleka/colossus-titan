import java.awt.*;
import java.awt.event.*;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public class AcquireAngel extends Dialog implements MouseListener,
    WindowListener
{
    private int numEligible;
    private Creature [] recruits;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Player player;
    private Legion legion;
    private Chit [] chits;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    public AcquireAngel(Frame parentFrame, Legion legion, boolean archangel)
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

        numEligible = Game.findEligibleAngels(legion, recruits, archangel);
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
            chits[i] = new Chit(scale, recruits[i].getImageName(), this);
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
        for (int i = 0; i < numEligible; i++)
        {
            if (chits[i] == source)
            {
                // Select that marker.
                Creature creature = recruits[i];
                legion.addCreature(creature, true);

                Game.logEvent("Legion " + legion.getMarkerId() +
                    " acquired an " + creature.getName());

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
