package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;
import net.sf.colossus.util.KDialog;


/**
 * ReplyToProposal allows responding to a negotiation proposal.
 * @version $Id$ 
 * @author David Ripton
 */

final class ReplyToProposal extends KDialog implements ActionListener
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


    ReplyToProposal(Client client, Proposal proposal)
    {
        super(client.getBoard().getFrame(), "Reply to Proposal", false);

        this.client = client;
        this.proposal = proposal;

        attackerId = proposal.getAttackerId();
        defenderId = proposal.getDefenderId();

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();
        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        attackerMarker = new Marker(scale, attackerId,
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(attackerMarker, constraints);
        contentPane.add(attackerMarker);

        java.util.List attackerImageNames = client.getLegionImageNames(
            attackerId);
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
        }

        defenderMarker = new Marker(scale, defenderId,
            this, client);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(defenderMarker, constraints);
        contentPane.add(defenderMarker);

        java.util.List defenderImageNames = client.getLegionImageNames(
            defenderId);
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
        }

        if (proposal.isMutual())
        {
            markAllDead(attackerId);
            markAllDead(defenderId);
        }
        else if (attackerId.equals(proposal.getWinnerId()))
        {
            markAllDead(defenderId);
            markSomeDead(attackerId, proposal.getWinnerLosses());
        }
        else if (defenderId.equals(proposal.getWinnerId()))
        {
            markAllDead(attackerId);
            markSomeDead(defenderId, proposal.getWinnerLosses());
        }

        JButton button1 = new JButton("Accept");
        button1.setMnemonic(KeyEvent.VK_A);
        JButton button2 = new JButton("Decline");
        button2.setMnemonic(KeyEvent.VK_D);
        JButton button3 = new JButton("Fight");
        button3.setMnemonic(KeyEvent.VK_F);

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


    private void markAllDead(String markerId)
    {
        Iterator it = null;
        if (markerId.equals(attackerId))
        {
            it = attackerChits.iterator();
        }
        else
        {
            it = defenderChits.iterator();
        }
        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            chit.setDead(true);
        }
    }

    private void markSomeDead(String markerId, java.util.List losses)
    {
        // Don't mess with the original list.
        java.util.List creatures = new ArrayList(losses);

        Iterator it = null;
        if (markerId.equals(attackerId))
        {
            it = attackerChits.iterator();
        }
        else
        {
            it = defenderChits.iterator();
        }

        while (it.hasNext())
        {
            Chit chit = (Chit)it.next();
            Iterator it2 = creatures.iterator();
            while (it2.hasNext())
            {
                Creature creature = (Creature)it2.next();
                if (creature.getName().equals(chit.getId()))
                {
                    chit.setDead(true);
                    it2.remove();
                }
            }
        }
    }


    private void cleanup()
    {
        Concede.saveLocation(getLocation());
        dispose();
        client.negotiateCallback(proposal);
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Accept"))
        {
            // Leave proposal as-is.
            cleanup();
        }

        else if (e.getActionCommand().equals("Decline"))
        {
            proposal = null;
            cleanup();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            String hexLabel = proposal.getHexLabel();
            proposal = new Proposal(attackerId, defenderId, true,
                false, null, null, hexLabel);

            // Exit this dialog.
            cleanup();
        }
    }
}
