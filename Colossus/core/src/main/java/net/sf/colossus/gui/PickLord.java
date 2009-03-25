package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */

final class PickLord extends KDialog
{
    private final List<Chit> chits = new ArrayList<Chit>();
    private String lordType;
    private final SaveWindow saveWindow;

    private PickLord(IOptions options, JFrame parentFrame,
        List<String> imageNames)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        lordType = null;

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());
        pack();
        setBackground(Color.lightGray);

        Iterator<String> it = imageNames.iterator();
        while (it.hasNext())
        {
            final String imageName = it.next();
            Chit chit = new Chit(4 * Scale.get(), imageName);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    lordType = imageName;
                    if (lordType.startsWith(Constants.titan))
                    {
                        lordType = Constants.titan;
                    }
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

    private String getLordType()
    {
        return lordType;
    }

    static synchronized String pickLord(IOptions options, JFrame parentFrame,
        List<String> imageNames)
    {
        PickLord pl = new PickLord(options, parentFrame, imageNames);
        return pl.getLordType();
    }
}