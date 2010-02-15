package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.guiutil.KDialog;


/**
 * Class ShowLegion displays the chits of the Creatures in a Legion
 *
 * @author David Ripton
 */
final class ShowLegion extends KDialog
{
    ShowLegion(JFrame parentFrame, LegionClientSide legion, Point point,
        JScrollPane pane, int scale, int viewMode, boolean isMyLegion,
        boolean dubiousAsBlanks, boolean showMarker)
    {
        super(parentFrame, legion.getMarkerId(), false);

        if (legion.getImageNames().isEmpty())
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }
        });

        LegionInfoPanel liPanel = new LegionInfoPanel(legion, scale, 5, 2,
            false, viewMode, isMyLegion, dubiousAsBlanks, false, showMarker);
        getContentPane().add(liPanel);

        String valueText = liPanel.getValueText();
        String ownerText = isMyLegion ? "" : " ["
            + legion.getPlayer().getName() + "]";

        setTitle(legion.getMarkerId() + valueText + ownerText);
        liPanel = null;

        placeRelative(parentFrame, point, pane);

        pack();
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });
        setVisible(true);
        repaint();
    }
}
