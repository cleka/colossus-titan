package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.KDialog;


/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */

final class PickLord extends KDialog implements MouseListener, WindowListener
{
    private List chits = new ArrayList();
    private static String lordType;
    private List imageNames;
    private SaveWindow saveWindow;

    private PickLord(IOptions options, JFrame parentFrame, List imageNames)
    {
        super(parentFrame, "Reveal Which Lord?", true);

        this.imageNames = imageNames;
        lordType = null;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());
        pack();
        setBackground(Color.lightGray);

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(4 * Scale.get(), imageName);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
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

    static String pickLord(IOptions options, JFrame parentFrame,
        List imageNames)
    {
        new PickLord(options, parentFrame, imageNames);
        return lordType;
    }

    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            lordType = (String)imageNames.get(i);
            if (lordType.startsWith(Constants.titan))
            {
                lordType = Constants.titan;
            }
            saveWindow.saveLocation(getLocation());
            dispose();
        }
    }
}
