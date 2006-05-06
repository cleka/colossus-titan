package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

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
    private List attackerChits = new ArrayList();
    private List defenderChits = new ArrayList();
    private Marker attackerMarker;
    private Marker defenderMarker;
    private Client client;
    private Proposal proposal;
    private Point location;
    private SaveWindow saveWindow;

    ReplyToProposal(Client client, Proposal proposal)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() +
                ": Reply to Proposal", false);

        this.client = client;
        this.proposal = proposal;

        attackerId = proposal.getAttackerId();
        defenderId = proposal.getDefenderId();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        JPanel attackerPane = new JPanel();
        contentPane.add(attackerPane);

        attackerMarker = new Marker(scale, attackerId, this, client);
        attackerPane.add(attackerMarker);

        List attackerImageNames = client.getLegionImageNames(
                attackerId);
        Iterator it = attackerImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            attackerChits.add(chit);
            attackerPane.add(chit);
        }

        JPanel defenderPane = new JPanel();
        contentPane.add(defenderPane);

        defenderMarker = new Marker(scale, defenderId, this, client);
        defenderPane.add(defenderMarker);

        List defenderImageNames = client.getLegionImageNames(
                defenderId);
        it = defenderImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            defenderChits.add(chit);
            defenderPane.add(chit);
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

        JPanel buttonPane = new JPanel();
        contentPane.add(buttonPane);

        JButton button1 = new JButton("Accept");
        button1.setMnemonic(KeyEvent.VK_A);
        JButton button2 = new JButton("Decline");
        button2.setMnemonic(KeyEvent.VK_D);
        JButton button3 = new JButton("Fight");
        button3.setMnemonic(KeyEvent.VK_F);

        buttonPane.add(button1);
        button1.addActionListener(this);
        buttonPane.add(button2);
        button2.addActionListener(this);

        pack();

        saveWindow = new SaveWindow(client, "ReplyToProposal");

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

    private void markSomeDead(String markerId, List losses)
    {
        // Don't mess with the original list.
        List creatures = new ArrayList(losses);

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
                String creatureName = (String)it2.next();
                if (creatureName.equals(chit.getId()))
                {
                    chit.setDead(true);
                    it2.remove();
                    break;
                }
            }
        }
    }

    private void cleanup()
    {
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        client.negotiateCallback(proposal, false);
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
            proposal = new Proposal(attackerId, defenderId, true,
                    false, null, null);

            // Exit this dialog.
            cleanup();
        }
    }
}
