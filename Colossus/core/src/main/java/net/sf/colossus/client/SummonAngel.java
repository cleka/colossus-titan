package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Legion;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;


/**
 * Allows a player to summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */


final class SummonAngel extends KDialog implements MouseListener,
            ActionListener, WindowListener
{
    private String markerId;
    private List sumChitList = new ArrayList();
    private JButton cancelButton;
    private static boolean active;
    private Client client;
    private static final String baseSummonString =
            ": Summon Angel into Legion ";
    private static final String sourceSummonString =
            "<p>Selected Legion is ";
    private static final String noSourceSummonString =
            "<p>No selected Legion";

    private SummonAngel(Client client, String markerId)
    {
        super(client.getBoard().getFrame(),
                "<html>" + client.getPlayerName() +
                baseSummonString + Legion.getLongMarkerName(markerId) +
                noSourceSummonString + "</html>",
                false);

        this.client = client;
        this.markerId = markerId;

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (client.getBoard().highlightSummonableAngels(markerId) < 1)
        {
            cleanup(null, null);
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        List summonableList = Creature.getSummonableCreatures();
        java.util.Iterator it = summonableList.iterator();
        sumChitList.clear();
        while (it.hasNext())
        {
            Chit tempChit;
            Creature c = (Creature)it.next();
            tempChit = new Chit(scale, c.getName());
            contentPane.add(tempChit);
            tempChit.addMouseListener(this);

            // X out chits since no legion is selected.
            tempChit.setDead(true);

            sumChitList.add(tempChit);
        }

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }

    static SummonAngel summonAngel(Client client, String markerId)
    {
        Log.debug("called summonAngel for " + markerId);
        if (!active)
        {
            active = true;
            Log.debug("returning new SummonAngel dialog for " + markerId);
            return new SummonAngel(client, markerId);
        }
        return null;
    }

    String getMarkerId()
    {
        return markerId;
    }

    private void cleanup(String donorId, String angel)
    {
        client.doSummon(markerId, donorId, angel);
        dispose();
        active = false;
    }

    public void mousePressed(MouseEvent e)
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }

        Object source = e.getSource();
        java.util.Iterator it = sumChitList.iterator();
        boolean done = false;
        while (it.hasNext() && !done)
        {
            Chit c = (Chit)it.next();
            if ((source == c) && !(c.isDead()))
            {
                cleanup(donorId, c.getId());
                done = true;
            }
        }
    }

    public void windowClosing(WindowEvent e)
    {
        cleanup(null, null);
    }

    /** Upstate state of angel and archangel chits to reflect donor */
    void updateChits()
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            label.setText("<html>" + client.getPlayerName() +
                    baseSummonString +
                    Legion.getLongMarkerName(markerId) +
                    noSourceSummonString + "</html>");
            return;
        }
        else
        {
            label.setText("<html>" + client.getPlayerName() +
                    baseSummonString +
                    Legion.getLongMarkerName(markerId) +
                    sourceSummonString +
                    Legion.getLongMarkerName(donorId) + "</html>");
        }
        java.util.Iterator it = sumChitList.iterator();
        while (it.hasNext())
        {
            Chit c = (Chit)it.next();
            c.setDead(!client.donorHas(c.getId()));
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
