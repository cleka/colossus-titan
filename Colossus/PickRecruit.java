import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */


class PickRecruit extends JDialog implements MouseListener, WindowListener
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
    private JFrame parentFrame;


    PickRecruit(JFrame parentFrame, Legion legion)
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

        getContentPane().setLayout(null);

        pack();

        setBackground(java.awt.Color.lightGray);

        // setSize(scale * (numEligible + 1), (23 * scale / 10));
        height = legion.getHeight();

        setSize(scale * (Math.max(numEligible, height + 1) + 1),
            (23 * scale / 5));

        // XXX: This doesn't work under Solaris.
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
                scale * 2 / 3, scale, legion.getCreature(i).getImageName(),
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
            JOptionPane.showMessageDialog(parentFrame,
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

        setVisible(true);
        repaint();
    }


    // Returns the number of the given recruiter needed to muster the given recruit
    // in the given terrain.  Returns -1 on error.
    public static int numberOfRecruiterNeeded(Creature recruiter, Creature recruit, 
        char terrain)
    {
        switch (terrain)
        {
            case 'B':
                if (recruit == Creature.gargoyle)
                {
                    if (recruiter == Creature.gargoyle || 
                        recruiter == Creature.cyclops ||
                        recruiter == Creature.gorgon)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.cyclops)
                {
                    if (recruiter == Creature.gargoyle)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.cyclops ||
                             recruiter == Creature.gorgon)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.gorgon)
                {
                    if (recruiter == Creature.cyclops)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.gorgon)
                    {
                        return 1;
                    }
                }
                break;
            
            case 'D':
                if (recruit == Creature.lion)
                {
                    if (recruiter == Creature.lion || 
                        recruiter == Creature.griffon ||
                        recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.griffon)
                {
                    if (recruiter == Creature.lion)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.griffon ||
                             recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.hydra)
                {
                    if (recruiter == Creature.griffon)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                break;

            case 'H':
                if (recruit == Creature.ogre)
                {
                    if (recruiter == Creature.ogre || 
                        recruiter == Creature.minotaur ||
                        recruiter == Creature.unicorn)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.minotaur)
                {
                    if (recruiter == Creature.ogre)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.minotaur ||
                             recruiter == Creature.unicorn)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.unicorn)
                {
                    if (recruiter == Creature.minotaur)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.unicorn)
                    {
                        return 1;
                    }
                }
                break;

            case 'J':
                if (recruit == Creature.gargoyle)
                {
                    if (recruiter == Creature.gargoyle || 
                        recruiter == Creature.cyclops ||
                        recruiter == Creature.behemoth ||
                        recruiter == Creature.serpent)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.cyclops)
                {
                    if (recruiter == Creature.gargoyle)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.cyclops ||
                             recruiter == Creature.behemoth ||
                             recruiter == Creature.serpent)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.behemoth)
                {
                    if (recruiter == Creature.cyclops)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.behemoth ||
                             recruiter == Creature.serpent)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.serpent)
                {
                    if (recruiter == Creature.behemoth)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.serpent)
                    {
                        return 1;
                    }
                }
                break;

            case 'm':
                if (recruit == Creature.lion)
                {
                    if (recruiter == Creature.lion || 
                        recruiter == Creature.minotaur ||
                        recruiter == Creature.dragon ||
                        recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.minotaur)
                {
                    if (recruiter == Creature.lion)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.minotaur ||
                             recruiter == Creature.dragon ||
                             recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.dragon)
                {
                    if (recruiter == Creature.minotaur)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.dragon ||
                             recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.colossus)
                {
                    if (recruiter == Creature.dragon)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                break;

            case 'M':
                if (recruit == Creature.ogre)
                {
                    if (recruiter == Creature.ogre || 
                        recruiter == Creature.troll ||
                        recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.troll)
                {
                    if (recruiter == Creature.ogre)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.troll ||
                             recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.ranger)
                {
                    if (recruiter == Creature.troll)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                break;

            case 'P':
                if (recruit == Creature.centaur)
                {
                    if (recruiter == Creature.centaur || 
                        recruiter == Creature.lion ||
                        recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.lion)
                {
                    if (recruiter == Creature.centaur)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.lion ||
                             recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.ranger)
                {
                    if (recruiter == Creature.lion)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.ranger)
                    {
                        return 1;
                    }
                }
                break;

            case 'S':
                if (recruit == Creature.troll)
                {
                    if (recruiter == Creature.troll || 
                        recruiter == Creature.wyvern ||
                        recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.wyvern)
                {
                    if (recruiter == Creature.troll)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.wyvern ||
                             recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.hydra)
                {
                    if (recruiter == Creature.wyvern)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.hydra)
                    {
                        return 1;
                    }
                }
                break;

            case 't':
                if (recruit == Creature.troll)
                {
                    if (recruiter == Creature.troll || 
                        recruiter == Creature.warbear ||
                        recruiter == Creature.giant ||
                        recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.warbear)
                {
                    if (recruiter == Creature.troll)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.warbear ||
                             recruiter == Creature.giant ||
                             recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.giant)
                {
                    if (recruiter == Creature.warbear)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.giant ||
                             recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.colossus)
                {
                    if (recruiter == Creature.giant)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.colossus)
                    {
                        return 1;
                    }
                }
                break;

            case 'T':
                if (recruit == Creature.centaur ||
                    recruit == Creature.gargoyle ||
                    recruit == Creature.ogre)
                {
                    return 0;
                }
                else if (recruit == Creature.warlock)
                {
                    if (recruiter == Creature.titan ||
                        recruiter == Creature.warlock)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.guardian)
                {
                    if (recruiter == Creature.behemoth ||
                        recruiter == Creature.centaur ||
                        recruiter == Creature.colossus ||
                        recruiter == Creature.cyclops ||
                        recruiter == Creature.dragon ||
                        recruiter == Creature.gargoyle ||
                        recruiter == Creature.giant ||
                        recruiter == Creature.gorgon ||
                        recruiter == Creature.griffon ||
                        recruiter == Creature.hydra ||
                        recruiter == Creature.lion ||
                        recruiter == Creature.minotaur ||
                        recruiter == Creature.ogre ||
                        recruiter == Creature.ranger ||
                        recruiter == Creature.serpent ||
                        recruiter == Creature.troll ||
                        recruiter == Creature.unicorn ||
                        recruiter == Creature.warbear ||
                        recruiter == Creature.wyvern)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.guardian)
                    {
                        return 1;
                    }
                }
                break;

            case 'W':
                if (recruit == Creature.centaur)
                {
                    if (recruiter == Creature.centaur || 
                        recruiter == Creature.warbear ||
                        recruiter == Creature.unicorn)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.warbear)
                {
                    if (recruiter == Creature.centaur)
                    {
                        return 3;
                    }
                    else if (recruiter == Creature.warbear ||
                             recruiter == Creature.unicorn)
                    {
                        return 1;
                    }
                }
                else if (recruit == Creature.unicorn)
                {
                    if (recruiter == Creature.warbear)
                    {
                        return 2;
                    }
                    else if (recruiter == Creature.unicorn)
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
    public static int findEligibleRecruiters(Legion legion, Creature recruit, 
        Creature [] recruiters)
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
            if (recruit == Creature.guardian)
            {
                for (int i = 0; i < Creature.creatures.length; i++)
                {
                    Creature creature = Creature.creatures[i];
                    if (creature == Creature.guardian && 
                        legion.numCreature(creature) >= 1)
                    {
                        recruiters[count++] = creature;
                    }
                    else if (!creature.isImmortal() &&
                        legion.numCreature(creature) >= 3)
                    {
                        recruiters[count++] = creature;
                    }
                }
            }
            else if (recruit == Creature.warlock)
            {
                if (legion.numCreature(Creature.titan) >= 1)
                {
                    recruiters[count++] = Creature.titan;
                }
                if (legion.numCreature(Creature.warlock) >= 1)
                {
                    recruiters[count++] = Creature.warlock;
                }
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

            for (int i = 0; i < numRecruitTypes; i++)
            {
                if (recruit == hex.getRecruit(i))
                {
                    int numToRecruit = hex.getNumToRecruit(i);
                    if (numToRecruit > 0 && 
                        legion.numCreature(hex.getRecruit(i - 1)) >= 
                        numToRecruit) 
                    {
                        // Can recruit up.
                        recruiters[count++] = hex.getRecruit(i - 1);
                    }
                    for (int j = i; j < numRecruitTypes; j++)
                    {
                        if (legion.numCreature(hex.getRecruit(j)) >= 1)
                        {
                            // Can recruit down or level.
                            recruiters[count++] = hex.getRecruit(j);
                        }
                    }
                    break;
                }
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


    // Attempt to free resources to work around Java memory leaks.
    private void cleanup()
    {
        setVisible(false);

        if (offImage != null)
        {
            offImage.flush();
            offGraphics.dispose();
        }

        if (imagesLoaded)
        {
            for (int i = 0; i < numEligible; i++)
            {
                tracker.removeImage(recruitChits[i].getImage());
                recruitChits[i].getImage().flush();
            }
        }

        dispose();
        System.gc();
        try
        {
            finalize();
        }
        catch (Throwable e)
        {
            System.out.println("caught " + e.toString());
        }
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
                Creature recruiters [] = new Creature[4];
                Creature recruiter;

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
                cleanup();
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
        cleanup();
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
