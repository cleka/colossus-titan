package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.CreatureType;


/**
 * Class PickLord allows a player to choose which lord tower teleports.
 *
 * @author David Ripton
 */

final class PickLord extends KDialog
{
    private final List<Chit> chits = new ArrayList<Chit>();
    private CreatureType lordType;
    private final SaveWindow saveWindow;

    private PickLord(IOptions options, JFrame parentFrame,
        List<CreatureType> choices)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        lordType = null;

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());
        pack();
        setBackground(Color.lightGray);

        for (final CreatureType creatureType : choices)
        {
            Chit chit = Chit.newCreatureChit(4 * Scale.get(), creatureType);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    lordType = creatureType;
                    saveWindow.saveLocation(getLocation());
                    dispose();
                }
            });
        }

        pack();
        saveWindow = new SaveWindow(options, "PickLord");
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

    private CreatureType getLordType()
    {
        return lordType;
    }

    static CreatureType pickLord(IOptions options, JFrame parentFrame,
        List<CreatureType> choices)
    {
        PickLord pl = new PickLord(options, parentFrame, choices);
        return pl.getLordType();
    }
}