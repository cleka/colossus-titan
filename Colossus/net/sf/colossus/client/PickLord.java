package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.server.Constants;

/**
 * Class PickLord allows a player to choose which lord tower teleports.
 * @version $Id$
 * @author David Ripton
 */


final class PickLord extends KDialog implements MouseListener, WindowListener
{
    private java.util.List chits = new ArrayList();
    private static String lordType;
    private java.util.List imageNames;


    private PickLord(JFrame parentFrame, java.util.List imageNames)
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
            Chit chit = new Chit(4 * Scale.get(), imageName, this);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();
        centerOnScreen();
        setVisible(true);
        repaint();
    }


    static String pickLord(JFrame parentFrame, java.util.List imageNames)
    {
        new PickLord(parentFrame, imageNames);
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
            dispose();
        }
    }
}
