import java.awt.*;
import java.awt.event.*;

/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * author David Ripton
 */


class PickRecruit extends Dialog implements MouseListener, WindowListener
{
    private int numEligible = 0;
    private Creature [] recruits;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Player player;
    private Legion legion;
    private Chit [] markers;


    PickRecruit(Frame parentFrame, Legion legion)
    {
        super(parentFrame, legion.getPlayer().getName() + 
            ": Pick Recruit", true);
        
        setResizable(false);

        if (!legion.canRecruit())
        {
            dispose();
            return;
        }

        this.legion = legion;
        player = legion.getPlayer();

        recruits = new Creature[5];
        markers = new Chit[recruits.length];

        addMouseListener(this);
        addWindowListener(this);

        int scale = 60;
        setLayout(null);
        setBackground(java.awt.Color.lightGray);

System.out.println("Calling findEligibleRecruits");
        numEligible = findEligibleRecruits(legion, recruits);
System.out.println("numEligible is " + numEligible);
        if (numEligible == 0)
        {
            dispose();
            return;
        }

        setSize(scale * (numEligible + 1), (21 * scale / 10));
            
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, 
            d.height / 2 - getSize().height / 2));

        int cx = scale / 2;
        int cy = scale * 2 / 3;

        for (int i = 0; i < numEligible; i++)
        {
            markers[i] = new Chit(cx + i * (21 * scale / 20), cy, scale,
                recruits[i].getImageName(), this, false);
        }

        imagesLoaded = false;
        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(markers[i].getImage(), 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            new MessageBox(parentFrame, "waitForAll was interrupted");
        }

        pack();
        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    // Returns the number of eligible recruits.
    static int findEligibleRecruits(Legion legion, Creature [] recruits)
    {
        MasterHex hex = legion.getCurrentHex();

        switch (hex.getTerrain())
        {
            case 'B':
                if (legion.numCreature(Creature.gargoyle) >= 1 ||
                    legion.numCreature(Creature.cyclops) >= 1 || 
                    legion.numCreature(Creature.gorgon) >= 1)
                {
                    recruits[0] = Creature.gargoyle;
                }
                if (legion.numCreature(Creature.gargoyle) >= 2 ||
                    legion.numCreature(Creature.cyclops) >= 1 || 
                    legion.numCreature(Creature.gorgon) >= 1)
                {
                    recruits[1] = Creature.cyclops;
                }
                if (legion.numCreature(Creature.cyclops) >= 2 || 
                    legion.numCreature(Creature.gorgon) >= 1)
                {
                    recruits[2] = Creature.gorgon;
                }
                break;

            case 'D':
                if (legion.numCreature(Creature.lion) >= 1 ||
                    legion.numCreature(Creature.griffon) >= 1 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[0] = Creature.lion;
                }
                if (legion.numCreature(Creature.lion) >= 3 ||
                    legion.numCreature(Creature.griffon) >= 1 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[1] = Creature.griffon;
                }
                if (legion.numCreature(Creature.griffon) >= 2 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[2] = Creature.hydra;
                }
                break;

            case 'H':
                if (legion.numCreature(Creature.ogre) >= 1 ||
                    legion.numCreature(Creature.minotaur) >= 1 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[0] = Creature.ogre;
                }
                if (legion.numCreature(Creature.ogre) >= 3 ||
                    legion.numCreature(Creature.minotaur) >= 1 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[1] = Creature.minotaur;
                }
                if (legion.numCreature(Creature.minotaur) >= 2 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[2] = Creature.unicorn;
                }
                break;

            case 'J':
                if (legion.numCreature(Creature.gargoyle) >= 1 ||
                    legion.numCreature(Creature.cyclops) >= 1 || 
                    legion.numCreature(Creature.behemoth) >= 1 ||
                    legion.numCreature(Creature.serpent) >= 1)
                {
                    recruits[0] = Creature.gargoyle;
                }
                if (legion.numCreature(Creature.gargoyle) >= 2 ||
                    legion.numCreature(Creature.cyclops) >= 1 || 
                    legion.numCreature(Creature.behemoth) >= 1 ||
                    legion.numCreature(Creature.serpent) >= 1)
                {
                    recruits[1] = Creature.cyclops;
                }
                if (legion.numCreature(Creature.cyclops) >= 3 || 
                    legion.numCreature(Creature.behemoth) >= 1 ||
                    legion.numCreature(Creature.serpent) >= 1)
                {
                    recruits[2] = Creature.behemoth;
                }
                if (legion.numCreature(Creature.behemoth) >= 2 ||
                    legion.numCreature(Creature.serpent) >= 1)
                {
                    recruits[3] = Creature.serpent;
                }
                break;

            case 'm':
                if (legion.numCreature(Creature.lion) >= 1 ||
                    legion.numCreature(Creature.minotaur) >= 1 || 
                    legion.numCreature(Creature.dragon) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[0] = Creature.lion;
                }
                if (legion.numCreature(Creature.lion) >= 2 ||
                    legion.numCreature(Creature.minotaur) >= 1 || 
                    legion.numCreature(Creature.dragon) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[1] = Creature.minotaur;
                }
                if (legion.numCreature(Creature.minotaur) >= 2 || 
                    legion.numCreature(Creature.dragon) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[2] = Creature.dragon;
                }
                if (legion.numCreature(Creature.dragon) >= 2 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[3] = Creature.colossus;
                }
                break;

            case 'M':
                if (legion.numCreature(Creature.ogre) >= 1 ||
                    legion.numCreature(Creature.troll) >= 1 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[0] = Creature.ogre;
                }
                if (legion.numCreature(Creature.ogre) >= 2 ||
                    legion.numCreature(Creature.troll) >= 1 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[1] = Creature.troll;
                }
                if (legion.numCreature(Creature.troll) >= 2 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[2] = Creature.ranger;
                }
                break;

            case 'P':
                if (legion.numCreature(Creature.centaur) >= 1 ||
                    legion.numCreature(Creature.lion) >= 1 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[0] = Creature.centaur;
                }
                if (legion.numCreature(Creature.centaur) >= 2 ||
                    legion.numCreature(Creature.lion) >= 1 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[1] = Creature.lion;
                }
                if (legion.numCreature(Creature.lion) >= 2 || 
                    legion.numCreature(Creature.ranger) >= 1)
                {
                    recruits[2] = Creature.ranger;
                }
                break;

            case 'S':
                if (legion.numCreature(Creature.troll) >= 1 ||
                    legion.numCreature(Creature.wyvern) >= 1 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[0] = Creature.troll;
                }
                if (legion.numCreature(Creature.troll) >= 3 ||
                    legion.numCreature(Creature.wyvern) >= 1 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[1] = Creature.wyvern;
                }
                if (legion.numCreature(Creature.wyvern) >= 2 || 
                    legion.numCreature(Creature.hydra) >= 1)
                {
                    recruits[2] = Creature.hydra;
                }
                break;

            case 't':
                if (legion.numCreature(Creature.troll) >= 1 ||
                    legion.numCreature(Creature.warbear) >= 1 || 
                    legion.numCreature(Creature.giant) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[0] = Creature.troll;
                }
                if (legion.numCreature(Creature.troll) >= 2 ||
                    legion.numCreature(Creature.warbear) >= 1 || 
                    legion.numCreature(Creature.giant) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[1] = Creature.warbear;
                }
                if (legion.numCreature(Creature.warbear) >= 2 || 
                    legion.numCreature(Creature.giant) >= 1 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[2] = Creature.giant;
                }
                if (legion.numCreature(Creature.giant) >= 2 ||
                    legion.numCreature(Creature.colossus) >= 1)
                {
                    recruits[3] = Creature.colossus;
                }
                break;

            case 'T':
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
                break;

            case 'W':
                if (legion.numCreature(Creature.centaur) >= 1 ||
                    legion.numCreature(Creature.warbear) >= 1 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[0] = Creature.centaur;
                }
                if (legion.numCreature(Creature.centaur) >= 3 ||
                    legion.numCreature(Creature.warbear) >= 1 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[1] = Creature.warbear;
                }
                if (legion.numCreature(Creature.warbear) >= 2 || 
                    legion.numCreature(Creature.unicorn) >= 1)
                {
                    recruits[2] = Creature.unicorn;
                }
                break;
        }

        // Check for availability of chits.
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null && recruits[i].getCount() < 1)
            {
                recruits[i] = null;
            }
        }

        // Pack the recruits array for display.
        for (int i = 0; i < recruits.length - 1; i++)
        {
System.out.println("i is " + i);
            if (recruits[i] == null)
            {
                for (int j = i; j < recruits.length - 1; j++)
                {
System.out.println("j is " + i);
                    recruits[j] = recruits[j + 1];
                }
                recruits[recruits.length - 1] = null;
            }
        }

        int count = 0;
        for (int i = 0; i < recruits.length; i++)
        {
            if (recruits[i] != null)
            {
                count++;
            }
        }
        return count;
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Rectangle rectClip = g.getClipBounds();

        for (int i = numEligible - 1; i >= 0; i--)
        {
            if (rectClip.intersects(markers[i].getBounds()))
            {
                markers[i].paint(g);
            }
        }
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < numEligible; i++)
        {
            if (markers[i].select(point))
            {
                // Select that marker.
                legion.addCreature(recruits[i]);

                // Recruits are one to a customer.
                legion.markRecruited();

                player.markLastLegionRecruited(legion);

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
