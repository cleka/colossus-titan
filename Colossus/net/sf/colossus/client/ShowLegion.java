package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.util.KDialog;


/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

final class ShowLegion extends KDialog implements MouseListener,
            WindowListener
{
    ShowLegion(JFrame parentFrame, String markerId, List imageNames,
            List certain, Point point, JScrollPane pane)
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

        placeRelative(parentFrame, point, pane);

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

        // This fixes a repaint bug under Linux.
        if (imageNames.size() == 1)
        {
            contentPane.add(Box.createRigidArea(new Dimension(scale, scale)));
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
