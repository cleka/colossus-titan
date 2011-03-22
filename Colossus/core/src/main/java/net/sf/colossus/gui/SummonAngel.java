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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.SummonInfo;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.CreatureType;


/**
 * Allows a player to summon an angel or archangel.
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */
final class SummonAngel extends KDialog
{
    private static final Logger LOGGER = Logger.getLogger(SummonAngel.class
        .getName());

    private static final String BASE_SUMMON_STRING = ": Summon Angel into Legion ";

    private static boolean active;
    private static SummonInfo summonInfo = null;

    private final Legion target;
    private final List<Chit> sumChitList = new ArrayList<Chit>();
    private final JButton cancelButton;
    private final SaveWindow saveWindow;
    private final Map<Chit, Legion> chitToDonor = new HashMap<Chit, Legion>();

    private SummonAngel(ClientGUI gui, Legion legion,
        List<Legion> possibleDonors)
    {
        super(gui.getBoard().getFrame(), gui.getOwningPlayer().getName()
            + BASE_SUMMON_STRING + legion, false);

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

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup(null, null);
            }
        });

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

        SortedSet<Legion> sortedDonors = new TreeSet<Legion>(
            Legion.ORDER_TITAN_THEN_POINTS_THEN_MARKER);
        sortedDonors.addAll(possibleDonors);

        for (Legion donor : sortedDonors)
        {
            Box box = new Box(BoxLayout.X_AXIS);
            Marker marker = new Marker(legion, scale, donor.getLongMarkerId());
            box.add(Box.createRigidArea(new Dimension(scale / 8, 0)));
            box.add(marker);
            box.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
            for (Creature creature : donor.getCreatures())
            {
                final CreatureType type = creature.getType();
                String name = type.getName();
                if (type.isTitan())
                {
                    name = legion.getPlayer().getTitanBasename();
                }
                else if (name.equals("Angel"))
                {
                    name = legion.getPlayer().getAngelBasename();
                }
                final Chit chit = Chit.newCreatureChit(scale, name);
                box.add(chit);
                if (type.isSummonable())
                {
                    chit.setBorder(true);
                    chit.setBorderColor(Color.red);
                    chit.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mousePressed(MouseEvent e)
                        {
                            if (!chit.isDead())
                            {
                                Legion donor = chitToDonor.get(chit);
                                cleanup(donor, type);
                            }
                        }
                    });
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
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                cleanup(null, null);
            }
        });

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
        List<Legion> possibleDonors)
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

    private void cleanup(Legion donor, CreatureType angel)
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
        dispose();
        synchronized (this)
        {
            this.notify();
        }
    }
}
