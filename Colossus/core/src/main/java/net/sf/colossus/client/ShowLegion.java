package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.game.Player;
import net.sf.colossus.util.KDialog;


/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 * @version $Id$
 * @author David Ripton
 */

final class ShowLegion extends KDialog implements MouseListener,
    WindowListener
{
    ShowLegion(JFrame parentFrame, LegionClientSide legion, Point point,
        JScrollPane pane, int scale, Player activePlayer, int viewMode,
        boolean dubiousAsBlanks)
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
            false, viewMode, activePlayer, dubiousAsBlanks, false);
        getContentPane().add(liPanel);

        String valueText = liPanel.getValueText();
        String ownerText = legion.isMyLegion() ? "" : " ["
            + legion.getPlayer().getName() + "]";

        setTitle(legion.getMarkerId() + valueText + ownerText);
        liPanel = null;

        placeRelative(parentFrame, point, pane);

        pack();
        addMouseListener(this);
        setVisible(true);
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
