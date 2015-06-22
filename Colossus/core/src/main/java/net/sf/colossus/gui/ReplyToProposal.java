package net.sf.colossus.gui;


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
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.colossus.common.Options;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Proposal;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * ReplyToProposal allows responding to a negotiation proposal.
 *
 * @author David Ripton
 */
final class ReplyToProposal extends KDialog implements ActionListener
{
    private final Legion attacker;
    private final Legion defender;
    private final List<Chit> attackerChits = new ArrayList<Chit>();
    private final List<Chit> defenderChits = new ArrayList<Chit>();
    private final Marker attackerMarker;
    private final Marker defenderMarker;
    private final ClientGUI gui;
    private Proposal proposal;
    private Point location;
    private final SaveWindow saveWindow;

    ReplyToProposal(JFrame parentframe, ClientGUI gui, String playerName,
        Options options, Proposal proposal)
    {
        super(parentframe, playerName + ": Reply to Proposal", false);

        this.proposal = proposal;
        this.gui = gui;

        attacker = proposal.getAttacker();
        defender = proposal.getDefender();

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        JPanel attackerPane = new JPanel();
        contentPane.add(attackerPane);

        attackerMarker = new Marker(attacker, scale,
            attacker.getLongMarkerId(), gui.getClient(), true);
        attackerPane.add(attackerMarker);

        List<String> attackerImageNames = gui.getOracle().getLegionImageNames(
            attacker);
        Iterator<String> it = attackerImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = Chit.newCreatureChit(scale, imageName);
            attackerChits.add(chit);
            attackerPane.add(chit);
        }

        JPanel defenderPane = new JPanel();
        contentPane.add(defenderPane);

        defenderMarker = new Marker(defender, scale,
            defender.getLongMarkerId(), gui.getClient(), true);
        defenderPane.add(defenderMarker);

        List<String> defenderImageNames = gui.getOracle().getLegionImageNames(
            defender);
        it = defenderImageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            Chit chit = Chit.newCreatureChit(scale, imageName);
            defenderChits.add(chit);
            defenderPane.add(chit);
        }

        if (proposal.isMutual())
        {
            markAllDead(attacker);
            markAllDead(defender);
        }
        else if (attacker.equals(proposal.getWinner()))
        {
            markAllDead(defender);
            markSomeDead(attacker, proposal.getWinnerLosses());
        }
        else if (defender.equals(proposal.getWinner()))
        {
            markAllDead(attacker);
            markSomeDead(defender, proposal.getWinnerLosses());
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

        saveWindow = new SaveWindow(options, "ReplyToProposal");

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

    private void markAllDead(Legion legion)
    {
        Iterator<Chit> it = null;
        if (legion.equals(attacker))
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

    private void markSomeDead(Legion legion, List<String> losses)
    {
        // Don't mess with the original list.
        List<String> creatures = new ArrayList<String>(losses);

        Iterator<Chit> it = null;
        if (legion.equals(attacker))
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
        // Accept or Fight, the Negotiate dialog in which one could make
        // further proposals is not needed any more
        if (proposal != null)
        {
            gui.cleanupNegotiationDialogs();
        }
        gui.negotiateCallback(proposal, false);
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
            proposal = new Proposal(attacker, defender, true, false, null,
                null);

            // Exit this dialog.
            cleanup();
        }
    }
}
