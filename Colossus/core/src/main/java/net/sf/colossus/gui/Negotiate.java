package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Negotiate allows making a new proposal to settle an engagement.
 *
 * @author David Ripton
 */
final class Negotiate extends KDialog
{
    private final Legion attacker;
    private final Legion defender;
    private final List<Chit> attackerChits = new ArrayList<Chit>();
    private final List<Chit> defenderChits = new ArrayList<Chit>();
    private final ClientGUI gui;
    private Proposal proposal;
    private Point location;
    private final SaveWindow saveWindow;
    private final Marker attackerMarker;
    private final Marker defenderMarker;

    Negotiate(ClientGUI gui, Legion attacker, Legion defender)
    {
        super(gui.getBoard().getFrame(), gui.getOwningPlayer().getName()
            + ": " + attacker + " Negotiates with " + defender, false);

        this.gui = gui;
        this.attacker = attacker;
        this.defender = defender;

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);
        // Don't allow closing without explicit decision:
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        attackerMarker = showLegion(attacker, attackerChits);
        attackerMarker.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                toggleAllDead(attackerChits);
            }
        });
        defenderMarker = showLegion(defender, defenderChits);
        defenderMarker.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                toggleAllDead(defenderChits);
            }
        });

        JButton button1 = new JButton("Offer");
        button1.setMnemonic(KeyEvent.VK_O);
        JButton button2 = new JButton("Fight");
        button2.setMnemonic(KeyEvent.VK_F);

        JPanel buttonPane = new JPanel();
        contentPane.add(buttonPane);
        buttonPane.add(button1);
        button1.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                doOffer();
            }

        });
        buttonPane.add(button2);
        button2.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                doFight();
            }
        });

        pack();

        saveWindow = new SaveWindow(gui.getOptions(), "Negotiate");

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

        Marker marker = new Marker(legion, scale, legion.getLongMarkerId(),
            gui.getClient(), true);
        pane.add(marker);
        pane.add(Box.createRigidArea(new Dimension(scale / 4, 0)));

        List<String> imageNames = ((gui.getOracle())
            .getLegionImageNames(legion));
        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            final Chit chit = Chit.newCreatureChit(scale, imageName);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    chit.toggleDead();
                    chit.repaint();
                }
            });
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
        gui.negotiateCallback(proposal, true);
    }

    /*
     * If not all are dead yet, mark all as dead;
     * but if all are dead, unmark all
     */
    private void toggleAllDead(List<Chit> chits)
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

    private void doOffer()
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
            gui.showMessageDialogAndWait("At least one legion must"
                + " be eliminated.");
            return;
        }

        if (!attackersLeft && !defendersLeft)
        {
            // Mutual destruction.
            proposal = new Proposal(attacker, defender, false, true, null,
                null);
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
                if (chit.isDead() && chit.getId().startsWith(Constants.titan))
                {
                    gui.showMessageDialogAndWait("Titan cannot die unless his"
                        + " whole stack dies.");
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
            proposal = new Proposal(attacker, defender, false, false,
                winnerLegion, winnerLosses);
        }

        // Exit this dialog.
        cleanup();
    }

    private void doFight()
    {
        proposal = new Proposal(attacker, defender, true, false, null, null);

        // Exit this dialog.
        cleanup();
    }
}
