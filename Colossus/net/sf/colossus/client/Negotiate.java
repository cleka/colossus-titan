package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;


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
    private Client client;
    private Proposal proposal;
    private String hexLabel;
    private Point location;
    private SaveWindow saveWindow;


    Negotiate(Client client, String attackerId, String defenderId)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() + ": " +
            attackerId + " Negotiates with " + defenderId, false);

        this.client = client;
        this.attackerId = attackerId;
        this.defenderId = defenderId;
        this.hexLabel = hexLabel;

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);
        addMouseListener(this);

        int scale = 4 * Scale.get();

        JPanel attackerPane = new JPanel();
        contentPane.add(attackerPane);

Log.debug("making attacker marker " + scale + " " + attackerId);
        attackerMarker = new Marker(scale, attackerId, this, client);
        attackerPane.add(attackerMarker);

        Iterator it = client.getLegionImageNames(attackerId).iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            attackerChits.add(chit);
            attackerPane.add(chit);
            chit.addMouseListener(this);
        }

        JPanel defenderPane = new JPanel();
        contentPane.add(defenderPane);

        defenderMarker = new Marker(scale, defenderId, this, client);
        defenderPane.add(defenderMarker);

        it = client.getLegionImageNames(defenderId).iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            defenderChits.add(chit);
            defenderPane.add(chit);
            chit.addMouseListener(this);
        }

        JButton button1 = new JButton("Offer");
        button1.setMnemonic(KeyEvent.VK_O);
        JButton button2 = new JButton("Fight");
        button2.setMnemonic(KeyEvent.VK_F);

        JPanel buttonPane = new JPanel();
        contentPane.add(buttonPane);
        buttonPane.add(button1);
        button1.addActionListener(this);
        buttonPane.add(button2);
        button2.addActionListener(this);

        pack();

        saveWindow = new SaveWindow(client, "Negotiate");

        location = saveWindow.loadLocation();
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
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        client.negotiateCallback(proposal, true);
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
                    if (chit.isDead() && chit.getId().startsWith(
                        Constants.titan))
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
                        if (name.startsWith(Constants.titan))
                        {
                            name = Constants.titan;
                        }
                        winnerLosses.add(name);
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
