import java.awt.*;
import java.awt.event.*;

/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


public class PickRecruiter extends Dialog implements MouseListener,
    WindowListener
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
    private GridBagLayout gridbag = new GridBagLayout(); 
    private GridBagConstraints constraints = new GridBagConstraints();


    public PickRecruiter(Frame parentFrame, Legion legion, int numEligible,
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

        setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);

        height = legion.getHeight();

        setResizable(false);

        legionMarker = new Marker(scale, legion.getImageName(), this, legion);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        gridbag.setConstraints(legionMarker, constraints);
        add(legionMarker);
        
        legionChits = new Chit[height];
        for (int i = 0; i < height; i++)
        {
            legionChits[i] = new Chit(scale, 
                legion.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            gridbag.setConstraints(legionChits[i], constraints);
            add(legionChits[i]);
        }

        recruiterChits = new Chit[numEligible];

        // There are height + 1 chits in the top row.  There
        // are numEligible chits to place beneath.
        // So we have (height + 1) - numEligible empty 
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        for (int i = 0; i < numEligible; i++)
        {
            recruiterChits[i] = new Chit(scale, recruiters[i].getImageName(),
                this);
            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(recruiterChits[i], constraints);
            add(recruiterChits[i]);
            recruiterChits[i].addMouseListener(this);
        }


        tracker = new MediaTracker(this);

        tracker.addImage(legionMarker.getImage(), 0);
        for (int i = 0; i < height; i++)
        {
            tracker.addImage(legionChits[i].getImage(), 0);
        }
        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(recruiterChits[i].getImage(), 0);
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
            if (recruiterChits[i] == source)
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
