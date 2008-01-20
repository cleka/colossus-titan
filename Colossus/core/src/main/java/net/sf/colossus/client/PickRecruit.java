package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.game.Legion;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.CreatureType;


/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */

final class PickRecruit extends KDialog implements MouseListener,
    WindowListener, ActionListener
{
    private final List<CreatureType> recruits;
    private final List<Chit> recruitChits = new ArrayList<Chit>();
    private final Marker legionMarker;
    private final List<Chit> legionChits = new ArrayList<Chit>();
    private static String recruit;
    private static boolean active;
    private final SaveWindow saveWindow;

    private PickRecruit(JFrame parentFrame, List<CreatureType> recruits,
        String hexDescription, Legion legion, Client client)
    {
        super(parentFrame, client.getOwningPlayer().getName()
            + ": Pick Recruit in " + hexDescription, true);

        this.recruits = recruits;

        addMouseListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        setBackground(Color.lightGray);
        int scale = 4 * Scale.get();

        JPanel legionPane = new JPanel();
        contentPane.add(legionPane);

        legionMarker = new Marker(scale, legion.getMarkerId());
        legionPane.add(legionMarker);

        List<String> imageNames = client.getLegionImageNames(legion);
        Iterator<String> itName = imageNames.iterator();
        while (itName.hasNext())
        {
            String imageName = itName.next();
            Chit chit = new Chit(scale, imageName);
            legionChits.add(chit);
            legionPane.add(chit);
        }

        JPanel recruitPane = new JPanel();
        contentPane.add(recruitPane);

        Iterator<CreatureType> it = recruits.iterator();
        int i = 0;
        while (it.hasNext())
        {
            Box vertPane = new Box(BoxLayout.Y_AXIS);
            vertPane.setAlignmentY(0);
            recruitPane.add(vertPane);

            CreatureType recruit = it.next();
            String recruitName = recruit.getName();
            Chit chit = new Chit(scale, recruitName);
            recruitChits.add(chit);

            vertPane.add(chit);
            chit.addMouseListener(this);

            int count = client.getCreatureCount(recruitName);
            JLabel countLabel = new JLabel(Integer.toString(count));
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            vertPane.add(countLabel);
            i++;
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        recruitPane.add(cancelButton);

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "PickRecruit");
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

    /** Return the creature recruited, or null if none. */
    static String pickRecruit(JFrame parentFrame, List<CreatureType> recruits,
        String hexDescription, Legion legion, Client client)
    {
        recruit = null;
        if (!active)
        {
            active = true;
            new PickRecruit(parentFrame, recruits, hexDescription, legion,
                client);
            active = false;
        }
        return recruit;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruitChits.indexOf(source);
        if (i != -1)
        {
            // Recruit the chosen creature.
            recruit = (recruits.get(i)).getName();
            dispose();
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        // Only action is cancel.
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    @Override
    public void dispose()
    {
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
}
