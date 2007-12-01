package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

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
    ShowLegion(JFrame parentFrame, LegionInfo legion, 
               Point point, JScrollPane pane, int scale, 
               String playerName, int viewMode, boolean dubiousAsBlanks)
    {
        super(parentFrame, legion.getMarkerId(), false);

        if (legion.getImageNames().isEmpty())
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);
        addWindowListener(this);

        LegionInfoPanel liPanel = new LegionInfoPanel(legion, scale, 5, 2, 
                false, viewMode, playerName, dubiousAsBlanks);
        getContentPane().add(liPanel);
        
        String valueText = liPanel.getValueText();
        setTitle(legion.getMarkerId() + valueText);
        liPanel = null;
                
        placeRelative(parentFrame, point, pane);

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
