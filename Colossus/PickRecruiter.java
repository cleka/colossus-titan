import java.awt.*;
import java.awt.event.*;

/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


class PickRecruiter extends Dialog implements MouseListener, WindowListener
{
    private int numEligible;
    private Critter [] recruiters;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Player player;
    private Legion legion;
    private Chit [] recruiterChits;
    private Marker legionMarker;
    private Chit [] legionChits;
    private int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private int height;
    private int leadWidth;


    PickRecruiter(Frame parentFrame, Legion legion, int numEligible, 
        Critter [] recruiters)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruiter", true);

        this.legion = legion;
        player = legion.getPlayer();

        this.numEligible = numEligible;
        this.recruiters = recruiters;

        addMouseListener(this);
        addWindowListener(this);

        setLayout(null);

        pack();

        setBackground(Color.lightGray);

        // setSize(scale * (numEligible + 1), (23 * scale / 10));
        height = legion.getHeight();

        setSize(scale * (Math.max(numEligible, height + 1) + 1),
            (23 * scale / 5));

        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        recruiterChits = new Chit[numEligible];
        leadWidth = (getSize().width - (numEligible * scale)) / 2;
        for (int i = 0; i < numEligible; i++)
        {
            recruiterChits[i] = new Chit(leadWidth + scale * i,
                scale * 5 / 2, scale, recruiters[i].getImageName(), this);
        }

        legionChits = new Chit[height];
        for (int i = 0; i < height; i++)
        {
            legionChits[i] = new Chit(scale * (2 * i + 3) / 2,
                scale * 2 / 3, scale, legion.getCritter(i).getImageName(),
                this);
        }

        legionMarker = new Marker(scale / 2, scale * 2 / 3, scale,
            legion.getImageName(), this, legion);

        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(recruiterChits[i].getImage(), 0);
        }
        for (int i = 0; i < height; i++)
        {
            tracker.addImage(legionChits[i].getImage(), 0);
        }
        tracker.addImage(legionMarker.getImage(), 0);

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

        for (int i = 0; i < numEligible;  i++)
        {
            if (rectClip.intersects(recruiterChits[i].getBounds()))
            {
                recruiterChits[i].paint(offGraphics);
            }
        }

        for (int i = 0; i < height; i++)
        {
            legionChits[i].paint(offGraphics);
        }

        legionMarker.paint(offGraphics);

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
            if (recruiterChits[i].select(point))
            {
                // The selected recruiter will be placed in the 0th 
                // position of the array.
                recruiters[0] = recruiters[i];

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
