import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

public class Negotiate extends JDialog implements MouseListener, ActionListener
{
    private Legion attacker;
    private Legion defender;
    private Chit [] attackerChits;
    private Chit [] defenderChits;
    private Marker attackerMarker;
    private Marker defenderMarker;
    private static final int scale = 60;
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    public Negotiate(JFrame parentFrame, Legion attacker, Legion defender)
    {
        super(parentFrame, attacker.getMarkerId() + " Negotiates with " +
            defender.getMarkerId(), true);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

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
        contentPane.add(attackerMarker);

        attackerChits = new Chit[attacker.getHeight()];
        for (int i = 0; i < attacker.getHeight(); i++)
        {
            attackerChits[i] = new Chit(scale, 
                attacker.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(attackerChits[i], constraints);
            contentPane.add(attackerChits[i]);
            attackerChits[i].addMouseListener(this);
        }
        
        defenderMarker = new Marker(scale, defender.getImageName(), this, 
            defender);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        contentPane.add(defenderMarker);

        defenderChits = new Chit[defender.getHeight()];
        for (int i = 0; i < defender.getHeight(); i++)
        {
            defenderChits[i] = new Chit(scale,
                defender.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(defenderChits[i], constraints);
            contentPane.add(defenderChits[i]);
            defenderChits[i].addMouseListener(this);
        }

        JButton button1 = new JButton("Agree");
        JButton button2 = new JButton("Fight");

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
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
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
                
                Game.logEvent(attacker.getMarkerId() + " and " +
                    defender.getMarkerId() + 
                    " agree to mutual elimination"); 

                // If both Titans died, eliminate both players.
                if (attacker.numCreature(Creature.titan) == 1 &&
                    defender.numCreature(Creature.titan) == 1)
                {
                    // Make defender die first, to simplify turn advancing.
                    defender.getPlayer().die(null, false);
                    attacker.getPlayer().die(null, true);
                }

                // If either was the titan stack, its owner dies and gives
                // half points to the victor.
                else if (attacker.numCreature(Creature.titan) == 1)
                {
                    attacker.getPlayer().die(defender.getPlayer(), true);
                }

                else if (defender.numCreature(Creature.titan) == 1)
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
                        JOptionPane.showMessageDialog(parentFrame,
                            "Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }
                
                StringBuffer log = new StringBuffer("Winning legion ");
                log.append(winner.getMarkerId());
                log.append(" loses creatures ");

                // Remove all dead creatures from the winning legion.
                for (int i = winner.getHeight() - 1; i >= 0; i--)
                {
                    if (winnerChits[i].isDead())
                    {
                        log.append(winner.getCreature(i).getName());
                        if (i > 0)
                        {
                            log.append(", ");
                        }
                        winner.removeCreature(i, true, true);
                    }
                }
                Game.logEvent(log.toString());

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
            MasterBoard.unselectHexByLabel(hex.getLabel());
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            // Exit this dialog.
            cleanup();
        }
    }
}
