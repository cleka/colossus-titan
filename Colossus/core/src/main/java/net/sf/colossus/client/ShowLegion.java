package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
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
    ShowLegion(JFrame parentFrame, Marker marker, LegionInfo legion, 
               Point point, JScrollPane pane, int scale, 
               String playerName, boolean onlyOwnLegionsOption)
    {
        super(parentFrame, "Legion " + legion.getMarkerId(), false);

        if (legion.getImageNames().isEmpty())
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);
        addWindowListener(this);


        // this check for owner is duplicated both here and in Autoinspector.
        // Would perhaps be better to have that in the LegionInfoPanel,
        // but I didn't manage to display a JLabel there instead of chits...
        
        String legionOwner = legion.getPlayerName();
        if ( onlyOwnLegionsOption && !playerName.equals(legionOwner) )
        {
        	int count = legion.getHeight();
        	getContentPane().add(new JLabel("Sorry, legion " + marker + " ("
            		+ count + " creatures) is not your legion."));
            // window will be above mouse if not shifted a bit
        	placeRelative(parentFrame, new Point(point.x, point.y + 10), pane);
        }
        else
        {
            getContentPane().add(new LegionInfoPanel(legion, scale, 5, 2, false));
            placeRelative(parentFrame, point, pane);
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