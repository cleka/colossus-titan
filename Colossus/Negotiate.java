import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

public class Negotiate extends JDialog implements MouseListener, ActionListener
{
    private Legion attacker;
    private Legion defender;
    private ArrayList attackerChits = new ArrayList();
    private ArrayList defenderChits = new ArrayList();
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

        Collection critters = attacker.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            attackerChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }
        
        defenderMarker = new Marker(scale, defender.getImageName(), this, 
            defender);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        contentPane.add(defenderMarker);

        critters = defender.getCritters();
        it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            defenderChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        JButton button1 = new JButton("Agree");
        button1.setMnemonic(KeyEvent.VK_A);
        JButton button2 = new JButton("Fight");
        button2.setMnemonic(KeyEvent.VK_F);

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
        setVisible(false);
        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        if (attackerChits.contains(source) || defenderChits.contains(source))
        {
            Chit chit = (Chit)source;
            chit.toggleDead();
            chit.repaint();
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

            Iterator it = attackerChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
                {
                    attackersLeft++;
                }
            }

            int defendersLeft = 0;
            it = defenderChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
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
                attacker.remove();
                defender.remove();
                
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
                ArrayList winnerChits;

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
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = (Chit)it.next();
                    if (chit.isDead() && chit.getId().startsWith("Titan"))
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
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = (Chit)it.next();
                    if (chit.isDead())
                    {
                        String name = chit.getId();
                        if (name.startsWith("Titan"))
                        {
                            name = "Titan";
                        }
                        log.append(name);
                        if (!it.hasNext())
                        {
                            log.append(", ");
                        }
                        winner.removeCreature(
                            Creature.getCreatureFromName(name), true, true);
                    }
                }
                Game.logEvent(log.toString());

                int points = loser.getPointValue();

                // Remove the losing legion.
                loser.remove();

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


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing Negotiate");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('B');

        Player player = new Player("Attacker", null);
        player.setScore(1400);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();
        player.selectMarker("Rd01");
        Legion attacker = new Legion(player.getSelectedMarker(), null, hex,
            Creature.titan, Creature.colossus, Creature.serpent,
            Creature.archangel, Creature.hydra, Creature.giant,
            Creature.dragon, null, player);
        Marker marker = new Marker(scale, player.getSelectedMarker(),
            frame, null);
        attacker.setMarker(marker);

        player = new Player("Defender", null);
        player.setTower(2);
        player.setColor("Blue");
        player.initMarkersAvailable();
        player.selectMarker("Bl01");
        Legion defender = new Legion(player.getSelectedMarker(), null, hex,
            Creature.ogre, Creature.centaur, Creature.gargoyle,
            null, null, null, null, null, player);
        marker = new Marker(scale, player.getSelectedMarker(),
            frame, null);
        defender.setMarker(marker);

        new Negotiate(frame, attacker, defender);
    }
}
