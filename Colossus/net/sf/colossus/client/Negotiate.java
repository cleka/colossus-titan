package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;


/**
 * Negotiate allows making a new proposal to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

final class Negotiate extends KDialog implements MouseListener, ActionListener
{
    private String attackerId;
    private String defenderId;
    private java.util.List attackerChits = new ArrayList();
    private java.util.List defenderChits = new ArrayList();
    private Marker attackerMarker;
    private Marker defenderMarker;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private Client client;
    private Proposal proposal;
    private String hexLabel;


    Negotiate(Client client, String attackerLongMarkerName,
        String defenderLongMarkerName, String attackerId,
        String defenderId, java.util.List attackerImageNames,
        java.util.List defenderImageNames, String hexLabel)
    {
        super(client.getBoard().getFrame(), attackerLongMarkerName +
            " Negotiates with " + defenderLongMarkerName, false);

        this.client = client;
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.hexLabel = hexLabel;

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();
        setBackground(Color.lightGray);
        addMouseListener(this);

        int scale = 4 * Scale.get();

        attackerMarker = new Marker(scale, attackerId,
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(attackerMarker, constraints);
        contentPane.add(attackerMarker);

        Iterator it = attackerImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            attackerChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        defenderMarker = new Marker(scale, defenderId,
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        contentPane.add(defenderMarker);

        it = defenderImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            defenderChits.add(chit);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        JButton button1 = new JButton("Offer");
        button1.setMnemonic(KeyEvent.VK_O);
        JButton button2 = new JButton("Fight");
        button2.setMnemonic(KeyEvent.VK_F);

        // Attempt to center the buttons.
        int chitWidth = 1 + Math.max(attackerImageNames.size(),
            defenderImageNames.size());
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
            centerOnScreen();
            location = getLocation();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
        repaint();
    }


    void cleanup()
    {
        Concede.saveLocation(getLocation());
        dispose();
        client.negotiateCallback(proposal);
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

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Offer"))
        {
            // Count remaining chits.
            boolean attackersLeft = false;
            Iterator it = attackerChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
                {
                    attackersLeft = true;
                    break;
                }
            }

            boolean defendersLeft = false;
            it = defenderChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                if (!chit.isDead())
                {
                    defendersLeft = true;
                    break;
                }
            }

            // Ensure that at least one legion is completely eliminated.
            if (attackersLeft && defendersLeft)
            {
                client.showMessageDialog(
                    "At least one legion must be eliminated.");
                return;
            }

            if (!attackersLeft && !defendersLeft)
            {
                // Mutual destruction.
                proposal = new Proposal(attackerId, defenderId, 
                    false, true, null, null, hexLabel);
            }
            else
            {
                String winnerMarkerId;
                String loserMarkerId;
                java.util.List winnerChits;

                if (!defendersLeft)
                {
                    winnerMarkerId = attackerId;
                    loserMarkerId = defenderId;
                    winnerChits = attackerChits;
                }
                else
                {
                    winnerMarkerId = defenderId;
                    loserMarkerId = attackerId;
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
                        client.showMessageDialog(
                            "Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }

                // Remove all dead creatures from the winning legion.
                java.util.List winnerLosses = new ArrayList();
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
                        // XXX bug   There can be more than one of the
                        // same type of creature, so we need to track
                        // quantity.
                        winnerLosses.add(Creature.getCreatureByName(name));
                    }
                }
                proposal = new Proposal(attackerId, defenderId, 
                    false, false, winnerMarkerId, winnerLosses, hexLabel);
            }

            // Exit this dialog.
            cleanup();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            proposal = new Proposal(attackerId, defenderId, true,
                false, null, null, hexLabel);

            // Exit this dialog.
            cleanup();
        }
    }
}
