package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.CreatureType;


/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 *
 * @author David Ripton
 */
final class PickRecruit extends KDialog
{
    private final List<Chit> recruitChits = new ArrayList<Chit>();
    private final Marker legionMarker;
    private final List<Chit> legionChits = new ArrayList<Chit>();
    private CreatureType recruit;
    private static boolean active;
    private final SaveWindow saveWindow;

    // next two temporary hack!
    private final ClientGUI gui;
    private final Legion legion;

    private PickRecruit(JFrame parentFrame, List<CreatureType> recruits,
        String hexDescription, Legion legion, ClientGUI gui)
    {
        super(parentFrame, gui.getOwningPlayer().getName()
            + ": Pick Recruit in " + hexDescription, true);

        // Meant to use only temporary for now.
        // TODO Handle the inform gui to mark as skip in better way
        this.gui = gui;
        this.legion = legion;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        int scale = 4 * Scale.get();

        JPanel legionPane = new JPanel();
        String legionId = legion.getMarkerId();
        String text = "Current content of legion " + legionId + ":";
        legionPane.setBorder(BorderFactory.createTitledBorder(text));

        contentPane.add(legionPane);

        legionMarker = new Marker(legion, scale, legion.getLongMarkerId());
        legionPane.add(legionMarker);

        List<String> imageNames = gui.getGameClientSide().getLegionImageNames(
            legion);
        Iterator<String> itName = imageNames.iterator();
        while (itName.hasNext())
        {
            String imageName = itName.next();
            Chit chit = Chit.newCreatureChit(scale, imageName);
            legionChits.add(chit);
            legionPane.add(chit);
        }

        contentPane.add(Box.createRigidArea(new Dimension(0, scale / 4)));
        JLabel label = new JLabel(
            "  Pick one of the following to recruit it, or cancel");
        label.setAlignmentX(FlowLayout.LEADING);
        contentPane.add(label);

        JPanel recruitPane = new JPanel();
        contentPane.add(recruitPane);

        // int i = 0;
        for (final CreatureType recruit : recruits)
        {
            Box vertPane = new Box(BoxLayout.Y_AXIS);
            vertPane.setAlignmentY(0);
            recruitPane.add(vertPane);

            Chit chit = Chit.newCreatureChit(scale, recruit);
            recruitChits.add(chit);

            vertPane.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    // Recruit the chosen creature.
                    PickRecruit.this.recruit = recruit;
                    dispose();
                }
            });

            int count = gui.getGame().getCaretaker()
                .getAvailableCount(recruit);
            JLabel countLabel = new JLabel(Integer.toString(count));
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            vertPane.add(countLabel);
            // i++;
        }

        // Provide the "skip recruit this time for this legion" choice only
        // for normal recruiting, not for reinforcement.
        if (!gui.getGameClientSide().isBattleOngoing())
        {
            JButton nothingButton = new JButton("Nothing");
            nothingButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Handle this better. Return a "NONE" creatureType and let
                    // caller take care of things
                    PickRecruit.this.gui
                        .markLegionAsSkipRecruit(PickRecruit.this.legion);
                    dispose();
                }
            });
            recruitPane.add(nothingButton);
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // Only action is cancel.
                dispose();
            }
        });
        recruitPane.add(cancelButton);

        pack();
        saveWindow = new SaveWindow(gui.getOptions(), "PickRecruit");
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

    private CreatureType getRecruit()
    {
        return recruit;
    }

    /** Return the creature recruited, or null if none. */
    static CreatureType pickRecruit(JFrame parentFrame,
        List<CreatureType> recruits, String hexDescription, Legion legion,
        ClientGUI gui)
    {
        CreatureType recruit = null;
        if (!active)
        {
            active = true;
            PickRecruit pr = new PickRecruit(parentFrame, recruits,
                hexDescription, legion, gui);
            recruit = pr.getRecruit();
            active = false;
        }
        return recruit;
    }
}
