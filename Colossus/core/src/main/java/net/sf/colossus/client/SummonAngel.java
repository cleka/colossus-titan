package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.Legion;
import net.sf.colossus.util.KDialog;


/**
 * Allows a player to summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

final class SummonAngel extends KDialog implements MouseListener,
    ActionListener, WindowListener
{
    private static final Logger LOGGER = Logger.getLogger(SummonAngel.class
        .getName());

    private String markerId;
    private List<Chit> sumChitList = new ArrayList<Chit>();
    private JButton cancelButton;
    private static boolean active;
    private Client client;
    private static final String baseSummonString = ": Summon Angel into Legion ";
    private static final String sourceSummonString = " Selected Legion is ";
    private static final String noSourceSummonString = " No selected Legion";
    private SaveWindow saveWindow;

    private SummonAngel(Client client, String markerId)
    {
        super(client.getBoard().getFrame(), client.getPlayerName()
            + baseSummonString + Legion.getLongMarkerName(markerId)
            + noSourceSummonString, false);

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

        List<Creature> summonableList = Creature.getSummonableCreatures();
        Iterator<Creature> it = summonableList.iterator();
        sumChitList.clear();
        while (it.hasNext())
        {
            Chit tempChit;
            Creature c = it.next();
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

        saveWindow = new SaveWindow(client, "SummonAngel");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }

        setVisible(true);
        repaint();
    }

    static SummonAngel summonAngel(Client client, String markerId)
    {
        LOGGER.log(Level.FINEST, "called summonAngel for " + markerId);
        if (!active)
        {
            active = true;
            LOGGER.log(Level.FINEST, "returning new SummonAngel dialog for "
                + markerId);
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
        saveWindow.saveLocation(getLocation());
        dispose();
        active = false;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }

        Object source = e.getSource();
        Iterator<Chit> it = sumChitList.iterator();
        boolean done = false;
        while (it.hasNext() && !done)
        {
            Chit c = it.next();
            if ((source == c) && !(c.isDead()))
            {
                cleanup(donorId, c.getId());
                done = true;
            }
        }
    }

    @Override
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
            setTitle(client.getPlayerName() + baseSummonString
                + Legion.getLongMarkerName(markerId) + noSourceSummonString);
            return;
        }
        else
        {
            setTitle(client.getPlayerName() + baseSummonString
                + Legion.getLongMarkerName(markerId) + sourceSummonString
                + Legion.getLongMarkerName(donorId));
        }
        Iterator<Chit> it = sumChitList.iterator();
        while (it.hasNext())
        {
            Chit c = it.next();
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
