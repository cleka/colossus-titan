package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.server.Creature;


/**
 * Class Negotiate allows two players to settle an engagement.
 * @version $Id$
 * @author David Ripton
 */

final class Negotiate extends JDialog implements MouseListener, ActionListener
{
    private String attackerMarkerId;
    private String defenderMarkerId;
    private ArrayList attackerChits = new ArrayList();
    private ArrayList defenderChits = new ArrayList();
    private Marker attackerMarker;
    private Marker defenderMarker;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private Client client;
    private static NegotiationResults results;


    private Negotiate(Client client, String attackerLongMarkerName,
        String defenderLongMarkerName, String attackerMarkerId,
        String defenderMarkerId, java.util.List attackerImageNames,
        java.util.List defenderImageNames)
    {
        super(client.getBoard().getFrame(), attackerLongMarkerName +
            " Negotiates with " + defenderLongMarkerName, true);

        this.client = client;
        this.attackerMarkerId = attackerMarkerId;
        this.defenderMarkerId = defenderMarkerId;

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);
        pack();
        setBackground(Color.lightGray);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        addMouseListener(this);

        int scale = 4 * Scale.get();

        attackerMarker = new Marker(scale, attackerMarkerId,
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

        defenderMarker = new Marker(scale, defenderMarkerId,
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
            location = new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2);
        }
        setLocation(location);

        setVisible(true);
        repaint();
    }


    /** Display a dialog allowing one player to offer a settlement to
     *  an engagement.  Return a NegotiationResults.
     */
    static NegotiationResults negotiate(Client client, 
        String attackerLongMarkerName, String defenderLongMarkerName, 
        String attackerMarkerId, String defenderMarkerId,
        java.util.List attackerImageNames, java.util.List defenderImageNames)
    {
        new Negotiate(client, attackerLongMarkerName, defenderLongMarkerName,
            attackerMarkerId, defenderMarkerId, attackerImageNames,
            defenderImageNames);
        return results;
    }


    void cleanup()
    {
        Concede.saveLocation(getLocation());
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
                results = new NegotiationResults(attackerMarkerId,
                    defenderMarkerId, false, true, null, null);
            }

            // If this is not a mutual elimination, figure out how many
            // points the victor receives.
            else
            {
                String winnerMarkerId;
                String loserMarkerId;
                ArrayList winnerChits;

                if (!defendersLeft)
                {
                    winnerMarkerId = attackerMarkerId;
                    loserMarkerId = defenderMarkerId;
                    winnerChits = attackerChits;
                }
                else
                {
                    winnerMarkerId = defenderMarkerId;
                    loserMarkerId = attackerMarkerId;
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
                Set winnerLosses = new HashSet();
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
                        winnerLosses.add(Creature.getCreatureByName(name));
                    }
                }
                results = new NegotiationResults(attackerMarkerId,
                    defenderMarkerId, false, false,
                    winnerMarkerId, winnerLosses);
            }

            // Exit this dialog.
            cleanup();
        }

        else if (e.getActionCommand().equals("Fight"))
        {
            results = new NegotiationResults(attackerMarkerId,
                defenderMarkerId, true, false, null, null);

            // Exit this dialog.
            cleanup();
        }
    }
}
