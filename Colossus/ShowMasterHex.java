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
    private boolean imagesLoaded;
    private MasterHex hex;
    private Chit [] chits;
    private int numChits;
    private int [] numToRecruit;
    private int [] count;
    int scale = 60;


    ShowMasterHex(Frame parentFrame, MasterHex hex, Point point)
    {
        super(parentFrame, hex.getTerrainName() + " Hex " + hex.getLabel(), 
            true);

        numChits = hex.getNumRecruitTypes();
        
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
        setBackground(java.awt.Color.lightGray);

        pack();
        addMouseListener(this);

        this.hex = hex;

        imagesLoaded = false;

        chits = new Chit[numChits];
        numToRecruit = new int[numChits];
        count = new int[numChits];
        for (int i = 0; i < numChits; i++)
        {
            Creature creature = hex.getRecruit(i);

            numToRecruit[i] = hex.getNumToRecruit(i);
            count[i] = creature.getCount();
            chits[i] = new Chit(scale, scale / 2 + i * scale, scale, 
                creature.getImageName(), this, false);
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
        
        FontMetrics fontMetrics = g.getFontMetrics();
        int fontHeight = fontMetrics.getMaxAscent() + fontMetrics.getLeading();

        for (int i = 0; i < numChits; i++)
        {
            chits[i].paint(g);

            if (numToRecruit[i] > 0)
            {
                String numToRecruitLabel = Integer.toString(numToRecruit[i]);
                g.drawString(numToRecruitLabel, scale / 3, (i + 1) * scale +
                    fontHeight / 2);
            }

            String countLabel = Integer.toString(count[i]);
            g.drawString(countLabel, 7 * scale / 3, (i + 1) * scale + 
                fontHeight / 2);
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
