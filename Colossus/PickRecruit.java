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
    private int [] counts;
    private int scale = 60;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private int height;
    private int leadWidth;
    private Frame parentFrame;


    PickRecruit(Frame parentFrame, Legion legion)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruit in " + 
            legion.getCurrentHex().getTerrainName().toLowerCase(), true);

        if (!legion.canRecruit())
        {
            dispose();
            return;
        }

        this.parentFrame = parentFrame;

        recruits = new Creature[5];

        numEligible = findEligibleRecruits(legion, recruits);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        setLayout(null);

        pack();

        setBackground(Color.lightGray);

        height = legion.getHeight();

        setSize(scale * (Math.max(numEligible, height + 1) + 1),
            (23 * scale / 5));

        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        recruitChits = new Chit[numEligible];
        counts = new int[numEligible];
        leadWidth = (getSize().width - (numEligible * scale)) / 2;
        for (int i = 0; i < numEligible; i++)
        {
            recruitChits[i] = new Chit(leadWidth + scale * i,
                scale * 5 / 2, scale, recruits[i].getImageName(), this);
            counts[i] = recruits[i].getCount();
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

        setVisible(true);
        repaint();
    }


    // Returns the number of the given recruiter needed to muster the given
    // recruit in the given terrain.  Returns -1 on error.
    public static int numberOfRecruiterNeeded(Critter recruiter, Creature
        recruit, char terrain)
    {
        switch (terrain)
        {
            case 'B':
                if (recruit.getName().equals("Gargoyle"))
                {
                    if (recruiter.getName().equals("Gargoyle") || 
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Cyclops"))
                {
                    if (recruiter.getName().equals("Gargoyle"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Cyclops") ||
                             recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Gorgon"))
                {
                    if (recruiter.getName().equals("Cyclops"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Gorgon"))
                    {
                        return 1;
                    }
                }
                break;
            
            case 'D':
                if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Lion") || 
                        recruiter.getName().equals("Griffon") ||
                        recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Griffon"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Griffon") ||
                             recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Hydra"))
                {
                    if (recruiter.getName().equals("Griffon"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                break;

            case 'H':
                if (recruit.getName().equals("Ogre"))
                {
                    if (recruiter.getName().equals("Ogre") || 
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Minotaur"))
                {
                    if (recruiter.getName().equals("Ogre"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Minotaur") ||
                             recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Unicorn"))
                {
                    if (recruiter.getName().equals("Minotaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                break;

            case 'J':
                if (recruit.getName().equals("Gargoyle"))
                {
                    if (recruiter.getName().equals("Gargoyle") || 
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Behemoth") ||
                        recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Cyclops"))
                {
                    if (recruiter.getName().equals("Gargoyle"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Cyclops") ||
                             recruiter.getName().equals("Behemoth") ||
                             recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Behemoth"))
                {
                    if (recruiter.getName().equals("Cyclops"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Behemoth") ||
                             recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Serpent"))
                {
                    if (recruiter.getName().equals("Behemoth"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Serpent"))
                    {
                        return 1;
                    }
                }
                break;

            case 'm':
                if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Lion") || 
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Dragon") ||
                        recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Minotaur"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Minotaur") ||
                             recruiter.getName().equals("Dragon") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Dragon"))
                {
                    if (recruiter.getName().equals("Minotaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Dragon") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Colossus"))
                {
                    if (recruiter.getName().equals("Dragon"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                break;

            case 'M':
                if (recruit.getName().equals("Ogre"))
                {
                    if (recruiter.getName().equals("Ogre") || 
                        recruiter.getName().equals("Troll") ||
                        recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Ogre"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Troll") ||
                             recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Ranger"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                break;

            case 'P':
                if (recruit.getName().equals("Centaur"))
                {
                    if (recruiter.getName().equals("Centaur") || 
                        recruiter.getName().equals("Lion") ||
                        recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Lion"))
                {
                    if (recruiter.getName().equals("Centaur"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Lion") ||
                             recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Ranger"))
                {
                    if (recruiter.getName().equals("Lion"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Ranger"))
                    {
                        return 1;
                    }
                }
                break;

            case 'S':
                if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Troll") || 
                        recruiter.getName().equals("Wyvern") ||
                        recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Wyvern"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Wyvern") ||
                             recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Hydra"))
                {
                    if (recruiter.getName().equals("Wyvern"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Hydra"))
                    {
                        return 1;
                    }
                }
                break;

            case 't':
                if (recruit.getName().equals("Troll"))
                {
                    if (recruiter.getName().equals("Troll") || 
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Giant") ||
                        recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Warbear"))
                {
                    if (recruiter.getName().equals("Troll"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Warbear") ||
                             recruiter.getName().equals("Giant") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Giant"))
                {
                    if (recruiter.getName().equals("Warbear"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Giant") ||
                             recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Colossus"))
                {
                    if (recruiter.getName().equals("Giant"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Colossus"))
                    {
                        return 1;
                    }
                }
                break;

            case 'T':
                if (recruit.getName().equals("Centaur") ||
                    recruit.getName().equals("Gargoyle") ||
                    recruit.getName().equals("Ogre"))
                {
                    return 0;
                }
                else if (recruit.getName().equals("Warlock"))
                {
                    if (recruiter.getName().equals("Titan") ||
                        recruiter.getName().equals("Warlock"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Guardian"))
                {
                    if (recruiter.getName().equals("Behemoth") ||
                        recruiter.getName().equals("Centaur") ||
                        recruiter.getName().equals("Colossus") ||
                        recruiter.getName().equals("Cyclops") ||
                        recruiter.getName().equals("Dragon") ||
                        recruiter.getName().equals("Gargoyle") ||
                        recruiter.getName().equals("Giant") ||
                        recruiter.getName().equals("Gorgon") ||
                        recruiter.getName().equals("Griffon") ||
                        recruiter.getName().equals("Hydra") ||
                        recruiter.getName().equals("Lion") ||
                        recruiter.getName().equals("Minotaur") ||
                        recruiter.getName().equals("Ogre") ||
                        recruiter.getName().equals("Ranger") ||
                        recruiter.getName().equals("Serpent") ||
                        recruiter.getName().equals("Troll") ||
                        recruiter.getName().equals("Unicorn") ||
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Wyvern"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Guardian"))
                    {
                        return 1;
                    }
                }
                break;

            case 'W':
                if (recruit.getName().equals("Centaur"))
                {
                    if (recruiter.getName().equals("Centaur") || 
                        recruiter.getName().equals("Warbear") ||
                        recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Warbear"))
                {
                    if (recruiter.getName().equals("Centaur"))
                    {
                        return 3;
                    }
                    else if (recruiter.getName().equals("Warbear") ||
                             recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                else if (recruit.getName().equals("Unicorn"))
                {
                    if (recruiter.getName().equals("Warbear"))
                    {
                        return 2;
                    }
                    else if (recruiter.getName().equals("Unicorn"))
                    {
                        return 1;
                    }
                }
                break;

            default:
                return -1;
        }
        return -1;
    }


    // Returns the number of eligible recruits.  The passed-in recruits array
    // should be of length 5, and will be filled with the eligible recruits.
    public static int findEligibleRecruits(Legion legion, Creature [] recruits)
    {
        // Paranoia
        if (recruits.length != 5)
        {
            System.out.println("Bad arg passed to findEligibleRecruits()");
            return 0;
        }
        for (int i = 0; i < recruits.length; i++)
        {
            recruits[i] = null;
        }
        
        MasterHex hex = legion.getCurrentHex();

        // Towers are a special case.
        if (hex.getTerrain() == 'T')
        {
            recruits[0] = Creature.centaur;
            recruits[1] = Creature.gargoyle;
            recruits[2] = Creature.ogre;
            if (legion.numCreature(Creature.behemoth) >= 3 ||
                legion.numCreature(Creature.centaur) >= 3 ||
                legion.numCreature(Creature.colossus) >= 3 ||
                legion.numCreature(Creature.cyclops) >= 3 ||
                legion.numCreature(Creature.dragon) >= 3 ||
                legion.numCreature(Creature.gargoyle) >= 3 ||
                legion.numCreature(Creature.giant) >= 3 ||
                legion.numCreature(Creature.gorgon) >= 3 ||
                legion.numCreature(Creature.griffon) >= 3 ||
                legion.numCreature(Creature.guardian) >= 1 ||
                legion.numCreature(Creature.hydra) >= 3 ||
                legion.numCreature(Creature.lion) >= 3 ||
                legion.numCreature(Creature.minotaur) >= 3 ||
                legion.numCreature(Creature.ogre) >= 3 ||
                legion.numCreature(Creature.ranger) >= 3 ||
                legion.numCreature(Creature.serpent) >= 3 ||
                legion.numCreature(Creature.troll) >= 3 ||
                legion.numCreature(Creature.unicorn) >= 3 ||
                legion.numCreature(Creature.warbear) >= 3 ||
                legion.numCreature(Creature.wyvern) >= 3)
            {
                recruits[3] = Creature.guardian;
            }
            if (legion.numCreature(Creature.titan) >= 1 ||
                legion.numCreature(Creature.warlock) >= 1)
            {
                recruits[4] = Creature.warlock;
            }
        }
        else
        {
            int numRecruitTypes = hex.getNumRecruitTypes();
            Creature [] recruitTypes = new Creature[numRecruitTypes];
            for (int i = 0; i < numRecruitTypes; i++)
            {
                recruitTypes[i] = hex.getRecruit(i);
            }
          
            for (int i = numRecruitTypes - 1; i >= 0; i--)
            {
                int numCreature = legion.numCreature(recruitTypes[i]);
                if (numCreature >= 1)
                {
                    int numToRecruit = hex.getNumToRecruit(i + 1);
                    if (numToRecruit > 0 && numCreature >= numToRecruit)
                    {
                        // We can recruit the next highest creature.
                        recruits[i + 1] = recruitTypes[i + 1];
                    }
                    for (int j = i; j >= 0; j--)
                    {
                        // We can recruit this creature and all below it.
                        recruits[j] = recruitTypes[j];
                    }
                    break;
                }
            }
        }


        // Check for availability of chits.
        int count = 0;

        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null && recruits[i].getCount() < 1)
            {
                recruits[i] = null;
            }
            if (recruits[i] != null)
            {
                count++;
            }
        }

        // Pack the recruits array for display.
        for (int i = 0; i < count; i++)
        {
            while (recruits[i] == null)
            {
                for (int j = i; j < recruits.length - 1; j++)
                {
                    recruits[j] = recruits[j + 1];
                }
                recruits[recruits.length - 1] = null;
            }
        }

        return count;
    }



    // Returns the number of eligible recruiters.  The passed-in recruiters
    // array should be of length 4 and will be filled in with recruiters.
    // We use a Critter array instead of a Creature array so that Titan
    // power is shown properly.
    public static int findEligibleRecruiters(Legion legion, Creature recruit, 
        Critter [] recruiters)
    {
        // Paranoia
        if (recruiters.length != 4)
        {
            System.out.println("Bad arg passed to findEligibleRecruiters()");
            return 0;
        }
        for (int i = 0; i < recruiters.length; i++)
        {
            recruiters[i] = null;
        }

        MasterHex hex = legion.getCurrentHex();

        int count = 0;

        if (hex.getTerrain() == 'T')
        {
            // Towers are a special case.  The recruiter of tower creatures 
            // remains anonymous, so we only deal with guardians and warlocks.
            if (recruit.getName().equals("Guardian"))
            {
                for (int i = 0; i < Creature.creatures.length; i++)
                {
                    Creature creature = Creature.creatures[i];
                    if (creature.getName().equals("Guardian") && 
                        legion.numCreature(creature) >= 1)
                    {
                        recruiters[count++] = legion.getCritter(creature);
                    }
                    else if (!creature.isImmortal() &&
                        legion.numCreature(creature) >= 3)
                    {
                        recruiters[count++] = legion.getCritter(creature);
                    }
                }
            }
            else if (recruit.getName().equals("Warlock"))
            {
                if (legion.numCreature(Creature.titan) >= 1)
                {
                    recruiters[count++] = legion.getCritter(Creature.titan);
                }
                if (legion.numCreature(Creature.warlock) >= 1)
                {
                    recruiters[count++] = legion.getCritter(Creature.warlock);
                }
            }
        }
        else
        {
            int numRecruitTypes = hex.getNumRecruitTypes();

            for (int i = 0; i < numRecruitTypes; i++)
            {
                if (recruit.getName().equals(hex.getRecruit(i).getName()))
                {
                    int numToRecruit = hex.getNumToRecruit(i);
                    if (numToRecruit > 0 && 
                        legion.numCreature(hex.getRecruit(i - 1)) >= 
                        numToRecruit) 
                    {
                        // Can recruit up.
                        recruiters[count++] = legion.getCritter(
                            hex.getRecruit(i - 1));
                    }
                    for (int j = i; j < numRecruitTypes; j++)
                    {
                        if (legion.numCreature(hex.getRecruit(j)) >= 1)
                        {
                            // Can recruit down or level.
                            recruiters[count++] = legion.getCritter(
                                hex.getRecruit(j));
                        }
                    }
                    break;
                }
            }
        }

        return count;
    }


    // Return true if all members of legion who are in recruiters are
    // already visible.
    private boolean allRecruitersVisible(Legion legion, Creature [] recruiters)
    {
        // Paranoia
        if (recruiters.length != 4)
        {
            System.out.println("Bad arg passed to allRecruitersVisible()");
            return false;
        }

        int height = legion.getHeight();

        for (int i = 0; i < height; i++)
        {
            Critter critter = legion.getCritter(i);
            if (!critter.isVisible())
            {
                for (int j = 0; j < recruiters.length; j++)
                {
                    Creature recruiter = recruiters[j];
                    if (recruiter != null && recruiter.getName().equals(
                        critter.getName()))
                    {
                        return false;
                    }
                }
            }
        }

        return true;
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
            if (rectClip.intersects(recruitChits[i].getBounds()))
            {
                recruitChits[i].paint(offGraphics);
            }

            String countLabel = Integer.toString(counts[i]);
            offGraphics.drawString(countLabel, leadWidth + scale * 
                (2 * i + 1) / 2 -
                offGraphics.getFontMetrics().stringWidth(countLabel) / 2,
                23 * scale / 6);
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
            if (recruitChits[i].select(point))
            {
                Creature recruit = recruits[i];

                // Pick the recruiter(s) if necessary.
                Critter [] recruiters = new Critter[4];
                Critter recruiter;

                int numEligibleRecruiters = findEligibleRecruiters(legion, 
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
                else if (allRecruitersVisible(legion, recruiters))
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
                    int numRecruiters = numberOfRecruiterNeeded(recruiter, 
                        recruit, legion.getCurrentHex().getTerrain());
                    if (numRecruiters >= 1)
                    {
                        legion.revealCreatures(recruiter, numRecruiters);
                    }

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
