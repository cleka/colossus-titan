package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;


/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

final class ShowLegion extends KDialog implements MouseListener,
    WindowListener
{
    ShowLegion(JFrame parentFrame, String markerId, java.util.List imageNames, 
        java.util.List certain, Point point)
    {
        super(parentFrame, "Legion " + markerId, false);

        if (imageNames.isEmpty())
        {
            dispose();
            return;
        }

        pack();
        setBackground(Color.lightGray);
        addWindowListener(this);

        placeRelative(parentFrame, point);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        int scale = 4 * Scale.get();

        Iterator it = imageNames.iterator();
        Iterator it2 = certain.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();

            boolean sure = ((Boolean)it2.next()).booleanValue();
            
            Chit chit = new Chit(scale, imageName, this, false, !sure);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();
        addMouseListener(this);
        setVisible(true);
        repaint();
    }


    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
