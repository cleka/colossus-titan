import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * author David Ripton
 */

class Negotiate extends JDialog implements MouseListener, ActionListener
{
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Legion attacker;
    private Legion defender;
    private Chit [] attackerChits;
    private Chit [] defenderChits;
    private Chit attackerMarker;
    private Chit defenderMarker;
    private static final int scale = 60;
    private JFrame parentFrame;
    private JButton button1;
    private JButton button2;
    private boolean laidOut = false;
    private Container contentPane;
    private Graphics gBack;
    private Dimension offDimension;
    private Image offImage;


    Negotiate(JFrame parentFrame, Legion attacker, Legion defender)
    {
        super(parentFrame, attacker.getMarkerId() + " Negotiates with " + 
            defender.getMarkerId(), true);

        setResizable(false);
        contentPane = getContentPane();
        contentPane.setLayout(null);

        this.attacker = attacker;
        this.defender = defender;
        this.parentFrame = parentFrame;

        imagesLoaded = false;

        pack();
        setBackground(java.awt.Color.lightGray);
        setSize(getPreferredSize());

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
            - getSize().height / 2));

        addMouseListener(this);

        attackerChits = new Chit[attacker.getHeight()];
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                scale / 2, scale, attacker.getCreature(i).getImageName(),
                this, false);
        }
        
        defenderChits = new Chit[defender.getHeight()];
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i] = new Chit((i + 1) * scale + (scale / 5), 
                2 * scale, scale, defender.getCreature(i).getImageName(),
                this, false);
        }
            
        attackerMarker = new Chit(scale / 5, scale / 2, scale, 
            attacker.getImageName(), this, false);
        
        defenderMarker = new Chit(scale / 5, 2 * scale, scale, 
            defender.getImageName(), this, false);

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
            JOptionPane.showMessageDialog(parentFrame, 
                "waitForAll was interrupted");
        }

        button1 = new JButton("Agree");
        button2 = new JButton("Fight");
        contentPane.add(button1);
        contentPane.add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

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
        if (gBack == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            gBack = offImage.getGraphics();
        }

        attackerMarker.paint(gBack);
        defenderMarker.paint(gBack);

        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i].paint(gBack);
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i].paint(gBack);
        }

        if (!laidOut)
        {
            Insets insets = getInsets(); 
            button1.setBounds(insets.left + d.width / 9, 3 * d.height / 4 - 
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right, 
                3 * d.height / 4 - insets.bottom, d.width / 3, d.height / 8);
            laidOut = true;
        }

        g.drawImage(offImage, 0, 0, this);

        // XXX: Handle via super.paint() instead?
        button1.repaint();
        button2.repaint();
    }
    
    
    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void mousePressed(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            Chit chit = attackerChits[i];
            if (chit.select(point))
            {
                chit.toggleDead();
                Rectangle clip = chit.getBounds();
                repaint(clip.x, clip.y, clip.width, clip.height);
                return;
            }
        }
        for (int i = 0; i < defender.getHeight(); i++)
        {
            Chit chit = defenderChits[i];
            if (chit.select(point))
            {
                chit.toggleDead();
                Rectangle clip = chit.getBounds();
                repaint(clip.x, clip.y, clip.width, clip.height);
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
        if (e.getActionCommand() == "Agree")
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
                JOptionPane.showMessageDialog(parentFrame, 
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

                // If either was the titan stack, its owner dies and gives 
                // half points to the victor.
                if (attacker.numCreature(Creature.titan) == 1)
                {
                    attacker.getPlayer().die(defender.getPlayer());
                }
            
                if (defender.numCreature(Creature.titan) == 1)
                {
                    defender.getPlayer().die(attacker.getPlayer());
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
                        JOptionPane.showMessageDialog(parentFrame, 
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

                // If this was the titan stack, its owner dies and gives half
                // points to the victor.
                if (loser.numCreature(Creature.titan) == 1)
                {
                    loser.getPlayer().die(winner.getPlayer());
                }
            }

            // Exit this dialog.
            dispose();

            // Unselect and repaint the hex.
            hex.unselect();
            hex.repaint();
        }

        else if (e.getActionCommand() == "Fight")
        {
            // Exit this dialog.
            dispose();
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(scale * (Math.max(attacker.getHeight(),
            defender.getHeight()) + 2), 4 * scale);
    }
}
