import java.awt.*;
import java.awt.event.*;

/**
 * Class ShowMasterHex displays the terrain type and recruits for a MasterHex.
 * @version $Id$
 * author David Ripton
 */

class ShowMasterHex extends Dialog implements MouseListener, WindowListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private MasterHex hex;
    private Chit [] chits;
    private int numChits;
    private int [] numToRecruit;
    private int [] count;
    private int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    ShowMasterHex(Frame parentFrame, MasterHex hex, Point point)
    {
        super(parentFrame, hex.getTerrainName() + " Hex " + hex.getLabel(),
            false);

        numChits = hex.getNumRecruitTypes();

        pack();

        setBackground(java.awt.Color.lightGray);
        setSize(3 * scale, numChits * scale + 3 * scale / 4);

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

        this.hex = hex;

        chits = new Chit[numChits];
        numToRecruit = new int[numChits];
        count = new int[numChits];
        for (int i = 0; i < numChits; i++)
        {
            Creature creature = hex.getRecruit(i);

            numToRecruit[i] = hex.getNumToRecruit(i);
            count[i] = creature.getCount();
            chits[i] = new Chit(scale, scale / 2 + i * scale, scale,
                creature.getImageName(), this);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < numChits; i++)
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

        FontMetrics fontMetrics = offGraphics.getFontMetrics();
        int fontHeight = fontMetrics.getMaxAscent() + fontMetrics.getLeading();

        for (int i = 0; i < numChits; i++)
        {
            chits[i].paint(offGraphics);

            if (numToRecruit[i] > 0)
            {
                String numToRecruitLabel = Integer.toString(numToRecruit[i]);
                offGraphics.drawString(numToRecruitLabel, scale / 3, (i + 1) *
                scale + fontHeight / 2);
            }

            String countLabel = Integer.toString(count[i]);
            offGraphics.drawString(countLabel, 7 * scale / 3, (i + 1) * scale +
                fontHeight / 2);
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
