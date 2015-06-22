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
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.CreatureType;


/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 *
 * @author David Ripton
 */
final class SplitLegion extends KDialog
{
    private final List<Chit> oldChits = new ArrayList<Chit>(8);
    private final List<Chit> newChits = new ArrayList<Chit>(8);

    private final Marker oldMarker;
    private final Marker newMarker;

    private final ClientGUI gui;
    private static boolean active;

    private List<CreatureType> creaturesToSplit;

    private final Box oldBox;
    private final Box newBox;
    private final Box buttonBox;

    private final JButton doneButton;
    private final JButton cancelButton;
    private final JButton skipButton;

    private final int totalChits;
    private final int scale;

    private final SaveWindow saveWindow;

    private SplitLegion(ClientGUI gui, Legion parent, String selectedMarkerId)
    {
        super(gui.getBoard().getFrame(), gui.getOwningPlayer().getName()
            + ": Split Legion " + parent, true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        this.gui = gui;

        if (selectedMarkerId == null)
        {
            // TODO this should probably just throw an exception or
            // even use an assert statement
            oldMarker = null;
            newMarker = null;
            oldBox = null;
            newBox = null;
            buttonBox = null;
            doneButton = null;
            cancelButton = null;
            skipButton = null;
            scale = 0;
            totalChits = 0;
            saveWindow = null;
            dispose();
            return;
        }

        setBackground(Color.lightGray);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cancel();
            }
        });

        scale = 4 * Scale.get();

        oldBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(oldBox);

        newBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(newBox);

        buttonBox = new Box(BoxLayout.X_AXIS);
        contentPane.add(buttonBox);

        oldMarker = new Marker(parent, scale, parent.getLongMarkerId());
        oldBox.add(oldMarker);
        oldBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        oldBox.add(Box.createHorizontalGlue());

        List<String> imageNames = gui.getClient().getLegionImageNames(parent);
        totalChits = imageNames.size();

        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = it.next();
            final Chit chit = Chit.newCreatureChit(scale, imageName);
            oldChits.add(chit);
            oldBox.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    int i = oldChits.indexOf(chit);
                    if (i != -1)
                    {
                        moveChitToOtherLine(oldChits, newChits, oldBox,
                            newBox, i);
                    }
                    else
                    {
                        i = newChits.indexOf(chit);
                        assert i != -1 : "Chit should be either in list of old or new chits.";
                        moveChitToOtherLine(newChits, oldChits, newBox,
                            oldBox, i);
                    }
                }
            });
        }

        // TODO XXX marker argument still missing!!!
        newMarker = new Marker(null, scale, selectedMarkerId + "-"
            + parent.getPlayer().getColor().getName());
        newBox.add(newMarker);
        newBox.add(Box.createRigidArea(new Dimension(scale / 4, 0)));
        newBox.add(Box.createHorizontalGlue());

        // Add chit-sized invisible spacers.
        for (int i = 0; i < totalChits; i++)
        {
            newBox.add(Box.createRigidArea(new Dimension(scale, scale)));
        }

        doneButton = new JButton("Done");
        doneButton.setEnabled(false);
        doneButton.setMnemonic(KeyEvent.VK_D);
        doneButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (!isSplitLegal(true))
                {
                    return;
                }
                returnSplitResults();
            }
        });
        buttonBox.add(doneButton);

        skipButton = new JButton("Skip");
        skipButton.setMnemonic(KeyEvent.VK_S);
        skipButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                markSkip();
            }
        });
        if (gui.getGame().getTurnNumber() > 1)
        {
            // not much use to allowing mark as skip in first turn :)
            buttonBox.add(skipButton);
        }

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                cancel();
            }
        });
        buttonBox.add(cancelButton);

        pack();
        saveWindow = new SaveWindow(gui.getOptions(), "SplitLegion");
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
    }

    /**
     * Opens a dialog to select the creatures to split and returns the result.
     *
     * @return The list of creature types selected for the split or null if the split was cancelled.
     */
    static List<CreatureType> splitLegion(ClientGUI gui, Legion parent,
        String selectedMarkerId)
    {
        if (!active)
        {
            active = true;
            SplitLegion dialog = new SplitLegion(gui, parent, selectedMarkerId);
            active = false;
            return dialog.creaturesToSplit;
        }
        return null;
    }

    /** Move a chit to the end of the other line. */
    private void moveChitToOtherLine(List<Chit> fromChits, List<Chit> toChits,
        Container fromBox, Container toBox, int oldPosition)
    {
        Chit chit = fromChits.remove(oldPosition);
        fromBox.remove(chit);
        fromBox.add(Box.createRigidArea(new Dimension(scale, 0)));

        toBox.remove(toBox.getComponentCount() - 1);
        toChits.add(chit);
        // Account for the marker and the spacer.
        toBox.add(chit, toChits.size() + 1);

        doneButton.setEnabled(isSplitLegal(false));

        pack();
        repaint();
    }

    /** Return true if the split is legal.  Each legion must have
     *  height >= 2.  If this was an initial split, each legion
     *  must have height == 4 and one lord. */
    private boolean isSplitLegal(boolean showMessage)
    {
        if (oldChits.size() < 2 || newChits.size() < 2)
        {
            if (showMessage)
            {
                gui.showMessageDialogAndWait("Legion too short.");
            }
            return false;
        }
        if (oldChits.size() + newChits.size() == 8)
        {
            if (oldChits.size() != newChits.size())
            {
                if (showMessage)
                {
                    gui.showMessageDialogAndWait("Initial split must be 4-4.");
                }
                return false;
            }

            int numLords = 0;
            Iterator<Chit> it = oldChits.iterator();
            while (it.hasNext())
            {
                Chit chit = it.next();
                String id = chit.getId();
                if (id.startsWith(Constants.titan)
                    || id.startsWith(gui.getGame().getVariant()
                        .getPrimaryAcquirable()))
                {
                    numLords++;
                }
            }
            if (numLords != 1)
            {
                if (showMessage)
                {
                    gui.showMessageDialogAndWait("Each stack must have one lord.");
                }
                return false;
            }
        }
        return true;
    }

    private void cancel()
    {
        creaturesToSplit = null;
        dispose();
    }

    private void markSkip()
    {
        // empty list to signal: skip split.
        creaturesToSplit = new ArrayList<CreatureType>();
        dispose();
    }

    private void returnSplitResults()
    {
        creaturesToSplit = new ArrayList<CreatureType>();
        Iterator<Chit> it = newChits.iterator();
        while (it.hasNext())
        {
            Chit chit = it.next();
            String creatureName = chit.getId();
            if (creatureName.startsWith(Constants.titan))
            {
                creatureName = Constants.titan;
            }
            creaturesToSplit.add(gui.getGame().getVariant()
                .getCreatureByName(creatureName));
        }
        dispose();
    }
}
