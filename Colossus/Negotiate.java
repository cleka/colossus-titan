import java.awt.*;
import java.awt.event.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

public class Negotiate extends Dialog implements MouseListener, ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Legion attacker;
    private Legion defender;
    private Chit [] attackerChits;
    private Chit [] defenderChits;
    private Marker attackerMarker;
    private Marker defenderMarker;
    private static final int scale = 60;
    private Frame parentFrame;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    public Negotiate(Frame parentFrame, Legion attacker, Legion defender)
    {
        super(parentFrame, attacker.getMarkerId() + " Negotiates with " +
            defender.getMarkerId(), true);

        setLayout(gridbag);

        this.attacker = attacker;
        this.defender = defender;
        this.parentFrame = parentFrame;

        pack();
        setBackground(Color.lightGray);
        setResizable(false);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();

        addMouseListener(this);

        attackerMarker = new Marker(scale, attacker.getImageName(), 
            this, attacker);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(attackerMarker, constraints);
        add(attackerMarker);

        attackerChits = new Chit[attacker.getHeight()];
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i] = new Chit(scale, 
                attacker.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(attackerChits[i], constraints);
            add(attackerChits[i]);
            attackerChits[i].addMouseListener(this);
        }
        
        defenderMarker = new Marker(scale, defender.getImageName(), this, 
            defender);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        add(defenderMarker);

        defenderChits = new Chit[defender.getHeight()];
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i] = new Chit(scale,
                defender.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(defenderChits[i], constraints);
            add(defenderChits[i]);
            defenderChits[i].addMouseListener(this);
        }


        tracker = new MediaTracker(this);

        for (int i = 0; i < attacker.getHeight(); i++)
        {
            tracker.addImage(attackerChits[i].getImage(), 0);
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            tracker.addImage(defenderChits[i].getImage(), 0);
        }
        tracker.addImage(attackerMarker.getImage(), 0);
        tracker.addImage(defenderMarker.getImage(), 0);

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

        Button button1 = new Button("Agree");
        Button button2 = new Button("Fight");

        // Attempt to center the buttons.
        int chitWidth = Math.max(attacker.getHeight(), 
            defender.getHeight()) + 1;
        if (chitWidth < 4)
        {
            constraints.gridwidth = 1;
        }
        else
        {
            constraints.gridwidth = 2;
        }
        int leadSpace = (chitWidth - 2 * constraints.gridwidth) / 2; 
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        constraints.gridy = 2;
        constraints.gridx = leadSpace;
        gridbag.setConstraints(button1, constraints);
        add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        add(button2);
        button2.addActionListener(this);

        pack();
        
        // Use the same location as the preceding Concede dialog.
        Point location = Concede.returnLocation();
        if (location == null)
        {
            location = new Point(d.width / 2 - getSize().width / 2, d.height / 2
                - getSize().height / 2);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }
    
    
    public void cleanup()
    {
        Concede.saveLocation(getLocation());
        dispose();
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
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            Chit chit = attackerChits[i];
            if (chit == source)
            {
                chit.toggleDead();
                chit.repaint();
                return;
            }
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            Chit chit = defenderChits[i];
            if (chit == source)
            {
                chit.toggleDead();
                chit.repaint();
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


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Agree"))
        {
            // Count remaining chits.
            int attackersLeft = 0;
            for (int i = 0; i < attacker.getHeight(); i++)
            {
                if (!attackerChits[i].isDead())
                {
                    attackersLeft++;
                }
            }
            int defendersLeft = 0;
            for (int i = 0; i < defender.getHeight(); i++)
            {
                if (!defenderChits[i].isDead())
                {
                    defendersLeft++;
                }
            }

            // Ensure that at least one legion is completely eliminated.
            if (attackersLeft > 0 && defendersLeft > 0)
            {
                new MessageBox(parentFrame, 
                    "At least one legion must be eliminated.");
                return;
            }

            MasterHex hex = attacker.getCurrentHex();

            // If this is a mutual elimination, remove both legions and
            // give no points.
            if (attackersLeft == 0 && defendersLeft == 0)
            {
                attacker.removeLegion();
                defender.removeLegion();
                
                Game.logEvent(attacker.getMarkerId() + " and " +
                    defender.getMarkerId() + 
                    " agree to mutual elimination"); 

                // If either was the titan stack, its owner dies and gives
                // half points to the victor.
                if (attacker.numCreature(Creature.titan) == 1)
                {
                    attacker.getPlayer().die(defender.getPlayer(), true);
                }

                if (defender.numCreature(Creature.titan) == 1)
                {
                    defender.getPlayer().die(attacker.getPlayer(), true);
                }
            }

            // If this is not a mutual elimination, figure out how many
            // points the victor receives.
            else
            {
                Legion winner;
                Legion loser;
                Chit [] winnerChits;

                if (defendersLeft == 0)
                {
                    winner = attacker;
                    loser = defender;
                    winnerChits = attackerChits;
                }
                else
                {
                    winner = defender;
                    loser = attacker;
                    winnerChits = defenderChits;
                }

                // Ensure that the winning legion doesn't contain a dead
                // Titan.
                for (int i = winner.getHeight() - 1; i >= 0; i--)
                {
                    if (winnerChits[i].isDead() && winner.getCreature(i) ==
                        Creature.titan)
                    {
                        new MessageBox(parentFrame,
                            "Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }

                // Remove all dead creatures from the winning legion.
                for (int i = winner.getHeight() - 1; i >= 0; i--)
                {
                    if (winnerChits[i].isDead())
                    {
                        winner.removeCreature(i);
                    }
                }

                int points = loser.getPointValue();

                // Remove the losing legion.
                loser.removeLegion();

                // Add points, and angels if necessary.
                winner.addPoints(points);
                
                Game.logEvent("Legion " + loser.getMarkerId() + 
                   " is eliminated by legion " + winner.getMarkerId() +
                   " via negotiation");

                // If this was the titan stack, its owner dies and gives half
                // points to the victor.
                if (loser.numCreature(Creature.titan) == 1)
                {
                    loser.getPlayer().die(winner.getPlayer(), true);
                }
            }

            // Exit this dialog.
            cleanup();

            // Unselect and repaint the hex.
            hex.unselect();
            hex.repaint();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            // Exit this dialog.
            cleanup();
        }
    }
}
