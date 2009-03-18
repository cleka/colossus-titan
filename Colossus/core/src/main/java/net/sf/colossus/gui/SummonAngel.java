package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.SummonInfo;
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

    private final Legion target;
    private final List<Chit> sumChitList = new ArrayList<Chit>();
    private final JButton cancelButton;
    private static boolean active;
    private static final String baseSummonString = ": Summon Angel into Legion ";
    private final SaveWindow saveWindow;
    private static SummonInfo summonInfo = null;
    private final Map<Chit, Legion> chitToDonor = new HashMap<Chit, Legion>();

    private SummonAngel(ClientGUI gui, Legion legion,
        SortedSet<Legion> possibleDonors)
    {
        super(gui.getBoard().getFrame(), gui.getOwningPlayer().getName()
            + baseSummonString + legion, false);

        this.target = legion;

        // TODO this should really not happen in a constructor

        if (possibleDonors.size() < 1)
        {
            LOGGER
                .warning("SummonAngel constructor still gets empty donor list???");
            cleanup(null, null);
            // trying to keep things final despite awkward exit point
            saveWindow = null;
            cancelButton = null;
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));
        Box txtBox = new Box(BoxLayout.X_AXIS);
        txtBox.add(Box.createRigidArea(new Dimension(8, scale / 8)));
        txtBox.add(new JLabel("The following legions contain summonable "
            + "creatures (they have a red border):   "));
        txtBox.add(Box.createHorizontalGlue());
        contentPane.add(txtBox);
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));

        sumChitList.clear();

        for (Legion donor : possibleDonors)
        {
            Box box = new Box(BoxLayout.X_AXIS);
            Marker marker = new Marker(scale, donor.getMarkerId());
            box.add(Box.createRigidArea(new Dimension(scale / 8, 0)));
            box.add(marker);
            box.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
            for (Creature creature : donor.getCreatures())
            {
                String name = creature.getType().getName();
                if (creature.getType().isTitan())
                {
                    name = gui.getTitanBaseName(legion);
                }
                Chit chit = new Chit(scale, name);
                box.add(chit);
                if (creature.getType().isSummonable())
                {
                    chit.setBorder(true);
                    chit.setBorderColor(Color.red);
                    chit.addMouseListener(this);
                    sumChitList.add(chit);
                    chitToDonor.put(chit, donor);
                }
            }
            box.add(Box.createHorizontalGlue());
            contentPane.add(box);
            contentPane.add(Box.createRigidArea(new Dimension(0, scale / 8)));
        }

        txtBox = new Box(BoxLayout.X_AXIS);
        txtBox.add(Box.createRigidArea(new Dimension(scale / 8, 0)));
        txtBox.add(Box.createHorizontalGlue());
        txtBox.add(new JLabel("Click a summonable to summon it to your "
            + "legion, or Cancel to not summon anything."));
        txtBox.add(Box.createHorizontalGlue());
        contentPane.add(txtBox);
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(this);

        Box btnBox = new Box(BoxLayout.X_AXIS);
        btnBox.add(Box.createHorizontalGlue());
        btnBox.add(cancelButton);
        btnBox.add(Box.createHorizontalGlue());
        contentPane.add(btnBox);
        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));

        pack();

        saveWindow = new SaveWindow(gui.getOptions(), "SummonAngel");
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

    // TODO Now that the SummonInfo object should never be null,
    // we could make returning null being the signal for canceling
    // a summoning, and then this could be part of the move phase,
    // instead of the "do it now or never" style as it is now.

    /** 
     * Returns a SummonInfo object, which contains Summoner, Donor legion
     * and the summoned unit, _OR_ the flag noSummoningWanted is set.
     */
    static SummonInfo summonAngel(ClientGUI gui, Legion legion,
        SortedSet<Legion> possibleDonors)
    {
        // Default constructor creates an info with the flag
        // "noSummoningWanted" set to true
        summonInfo = new SummonInfo();

        LOGGER.log(Level.FINER, "called summonAngel for " + legion);
        if (!active)
        {
            active = true;
            LOGGER.log(Level.FINEST, "creating new SummonAngel dialog for "
                + legion);

            SummonAngel saDialog = new SummonAngel(gui, legion, possibleDonors);

            synchronized (saDialog)
            {
                try
                {
                    saDialog.wait();
                }
                catch (InterruptedException e)
                {
                    LOGGER.log(Level.WARNING,
                        "While static SummonAngel() was "
                            + "waiting for SummonAngel dialog to complete, "
                            + "received InterruptedException?");
                }
            }
            saDialog = null;
            active = false;
        }
        LOGGER.log(Level.FINEST, "summonAngel returning "
            + summonInfo.toString());

        return summonInfo;
    }

    private void cleanup(Legion donor, String angel)
    {
        LOGGER.log(Level.FINEST, "SummonAngel.cleanup " + donor + " " + angel);
        if (donor != null && angel != null)
        {
            summonInfo = new SummonInfo(target, donor, angel);
        }
        else
        {
            // Default constructor creates an info with the flag
            // "noSummoningWanted" set to true
            summonInfo = new SummonInfo();
        }
        // Shound not happen any more since I fixed that we can't get
        // empty donor list. Still, to be sure...
        if (saveWindow != null)
        {
            saveWindow.saveLocation(getLocation());
        }
        dispose();
        synchronized (this)
        {
            this.notify();
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        for (Chit c : sumChitList)
        {
            if ((source == c) && !(c.isDead()))
            {
                Legion donor = chitToDonor.get(c);
                cleanup(donor, c.getId());
                return;
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        cleanup(null, null);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
