import java.awt.*;
import java.awt.event.*;

/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */


class PickRecruit extends Dialog implements MouseListener, WindowListener
{
    private int numEligible;
    private Creature [] recruits;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Player player;
    private Legion legion;
    private Chit [] recruitChits;
    private Marker legionMarker;
    private Chit [] legionChits;
    private int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private Frame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private boolean dialogLock = false;


    PickRecruit(Frame parentFrame, Legion legion)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruit in " + 
            legion.getCurrentHex().getTerrainName(), true);

        if (!legion.canRecruit())
        {
            dispose();
            return;
        }

        this.parentFrame = parentFrame;

        recruits = new Creature[5];

        numEligible = Game.findEligibleRecruits(legion, recruits);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);

        int height = legion.getHeight();

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
            constraints.gridy = 0;
            gridbag.setConstraints(legionChits[i], constraints);
            add(legionChits[i]);
        }


        recruitChits = new Chit[numEligible];

        // There are height + 1 chits in the top row.  There
        // are numEligible chits / labels to place beneath.
        // So we have (height + 1) - numEligible empty 
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        for (int i = 0; i < numEligible; i++)
        {
            recruitChits[i] = new Chit(scale, recruits[i].getImageName(),
                this);

            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(recruitChits[i], constraints);
            add(recruitChits[i]);
            recruitChits[i].addMouseListener(this);
            int count = recruits[i].getCount();
            Label countLabel = new Label(Integer.toString(count), 
                Label.CENTER);
            constraints.gridy = 2;
            gridbag.setConstraints(countLabel, constraints);
            add(countLabel);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(recruitChits[i].getImage(), 0);
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
            if (recruitChits[i] == source && !dialogLock)
            {
                // Prevent multiple clicks from yielding multiple recruits.
                dialogLock = true;

                Creature recruit = recruits[i];

                // Pick the recruiter(s) if necessary.
                Critter [] recruiters = new Critter[4];
                Critter recruiter;

                int numEligibleRecruiters = Game.findEligibleRecruiters(legion,
                    recruit, recruiters);

                if (numEligibleRecruiters == 1)
                {
                    recruiter = recruiters[0];
                }
                else if (numEligibleRecruiters == 0)
                {
                    // A warm body recruits in a tower.
                    recruiter = null;
                }
                else if (Game.allRecruitersVisible(legion, recruiters))
                {
                    // If all possible recruiters are already visible, don't
                    // bother picking which ones to reveal.
                    recruiter = recruiters[0];
                }
                else
                {
                    new PickRecruiter(parentFrame, legion, 
                        numEligibleRecruiters, recruiters);
                    recruiter = recruiters[0];
                }

                if (recruit != null && (recruiter != null ||
                    numEligibleRecruiters == 0))
                {
                    // Select that marker.
                    legion.addCreature(recruit);

                    // Mark the recruiter(s) as visible.
                    int numRecruiters = Game.numberOfRecruiterNeeded(recruiter,
                        recruit, legion.getCurrentHex().getTerrain());
                    if (numRecruiters >= 1)
                    {
                        legion.revealCreatures(recruiter, numRecruiters);
                    }

                    Game.logEvent("Legion " + legion.getMarkerId() +
                        " in " +  
                        legion.getCurrentHex().getTerrainName() +
                        " hex " + legion.getCurrentHex().getLabel() +
                        " recruits " + recruit.getName() + " with " +
                        (numRecruiters == 0 ? "nothing" :
                        numRecruiters + " " + (numRecruiters > 1 ? 
                        recruiter.getPluralName() : recruiter.getName())));

                    // Recruits are one to a customer.
                    legion.markRecruited();

                    player.markLastLegionRecruited(legion);
                }

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
