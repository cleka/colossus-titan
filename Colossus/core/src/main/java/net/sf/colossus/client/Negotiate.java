package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.sf.colossus.game.Legion;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Negotiate allows making a new proposal to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

final class Negotiate extends KDialog implements MouseListener, ActionListener
{
    private final Legion attacker;
    private final Legion defender;
    private final List<Chit> attackerChits = new ArrayList<Chit>();
    private final List<Chit> defenderChits = new ArrayList<Chit>();
    private final Client client;
    private Proposal proposal;
    private Point location;
    private final SaveWindow saveWindow;
    private Marker attackerMarker;
    private Marker defenderMarker;


    Negotiate(Client client, Legion attacker, Legion defender)
    {
        super(client.getBoard().getFrame(), client.getOwningPlayer().getName()
            + ": " + attacker + " Negotiates with " + defender, false);

        this.client = client;
        this.attacker = attacker;
        this.defender = defender;

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);
        addMouseListener(this);

        attackerMarker = showLegion(attacker, attackerChits);
        defenderMarker = showLegion(defender, defenderChits);

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

        saveWindow = new SaveWindow(client.getOptions(), "Negotiate");

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

    private Marker showLegion(Legion legion, List<Chit> chits)
    {
        Box pane = new Box(BoxLayout.X_AXIS);
        pane.setAlignmentX(0);
        getContentPane().add(pane);

        int scale = 4 * Scale.get();

        Marker marker = new Marker(scale, legion.getMarkerId());
        marker.addMouseListener(this);
        pane.add(marker);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        List<String> imageNames = client.getLegionImageNames(legion);
        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = new Chit(scale, imageName);
            chit.addMouseListener(this);
            chits.add(chit);
            pane.add(chit);
        }
        return marker;
    }

    void cleanup()
    {
        location = getLocation();
        saveWindow.saveLocation(location);
        dispose();
        client.negotiateCallback(proposal, true);
    }

    /*
     * If not all are dead yet, mark all as dead;
     * but if all are dead, unmark all 
     */  
    private void toggleAllDead(List <Chit> chits)
    {
        boolean allDead = true;
        for (Chit c : chits)
        {
            if (!c.isDead())
            {
                allDead = false;
            }
        }
        for (Chit c : chits)
        {
            c.setDead(!allDead);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        if (source == attackerMarker)
        {
            toggleAllDead(attackerChits);
        }
        else if (source == defenderMarker)
        {
            toggleAllDead(defenderChits);
        }
        else if (attackerChits.contains(source) || defenderChits.contains(source))
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
            Iterator<Chit> it = attackerChits.iterator();
            while (it.hasNext())
            {
                Chit chit = it.next();
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
                Chit chit = it.next();
                if (!chit.isDead())
                {
                    defendersLeft = true;
                    break;
                }
            }

            // Ensure that at least one legion is completely eliminated.
            if (attackersLeft && defendersLeft)
            {
                client
                    .showMessageDialog("At least one legion must be eliminated.");
                return;
            }

            if (!attackersLeft && !defendersLeft)
            {
                // Mutual destruction.
                proposal = new Proposal(attacker.getMarkerId(), defender
                    .getMarkerId(), false, true, null, null);
            }
            else
            {
                Legion winnerLegion;
                List<Chit> winnerChits;

                if (!defendersLeft)
                {
                    winnerLegion = attacker;
                    winnerChits = attackerChits;
                }
                else
                {
                    winnerLegion = defender;
                    winnerChits = defenderChits;
                }

                // Ensure that the winning legion doesn't contain a dead
                // Titan.
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = it.next();
                    if (chit.isDead()
                        && chit.getId().startsWith(Constants.titan))
                    {
                        client
                            .showMessageDialog("Titan cannot die unless his whole stack dies.");
                        return;
                    }
                }

                // Remove all dead creatures from the winning legion.
                List<String> winnerLosses = new ArrayList<String>();
                it = winnerChits.iterator();
                while (it.hasNext())
                {
                    Chit chit = it.next();
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
                proposal = new Proposal(attacker.getMarkerId(), defender
                    .getMarkerId(), false, false, winnerLegion.getMarkerId(),
                    winnerLosses);
            }

            // Exit this dialog.
            cleanup();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            proposal = new Proposal(attacker.getMarkerId(), defender
                .getMarkerId(), true, false, null, null);

            // Exit this dialog.
            cleanup();
        }
    }
}
