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
    private final String attackerId;
    private final String defenderId;
    private final List<Chit> attackerChits = new ArrayList<Chit>();
    private final List<Chit> defenderChits = new ArrayList<Chit>();
    private final Marker attackerMarker;
    private final Marker defenderMarker;
    private final Client client;
    private Proposal proposal;
    private Point location;
    private final SaveWindow saveWindow;

    ReplyToProposal(Client client, Proposal proposal)
    {
        super(client.getBoard().getFrame(), client.getOwningPlayer().getName()
            + ": Reply to Proposal", false);

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

        attackerMarker = new Marker(scale, attackerId, client);
        attackerPane.add(attackerMarker);

        List<String> attackerImageNames = client.getLegionImageNames(client
            .getLegion(attackerId));
        Iterator<String> it = attackerImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = new Chit(scale, imageName);
            attackerChits.add(chit);
            attackerPane.add(chit);
        }

        JPanel defenderPane = new JPanel();
        contentPane.add(defenderPane);

        defenderMarker = new Marker(scale, defenderId, client);
        defenderPane.add(defenderMarker);

        List<String> defenderImageNames = client.getLegionImageNames(client
            .getLegion(defenderId));
        it = defenderImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = new Chit(scale, imageName);
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

        saveWindow = new SaveWindow(client.getOptions(), "ReplyToProposal");

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
        Iterator<Chit> it = null;
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
            Chit chit = it.next();
            chit.setDead(true);
        }
    }

    private void markSomeDead(String markerId, List<String> losses)
    {
        // Don't mess with the original list.
        List<String> creatures = new ArrayList<String>(losses);

        Iterator<Chit> it = null;
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
            Chit chit = it.next();
            Iterator<String> it2 = creatures.iterator();
            while (it2.hasNext())
            {
                String creatureName = it2.next();
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
            proposal = new Proposal(attackerId, defenderId, true, false, null,
                null);

            // Exit this dialog.
            cleanup();
        }
    }
}
