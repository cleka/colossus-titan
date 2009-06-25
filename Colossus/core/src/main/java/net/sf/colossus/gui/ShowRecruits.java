package net.sf.colossus.gui;


import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Displays recruit trees for a single Hex type.
 *
 * @author David Ripton
 * @author Barrie Treloar
 */
final class ShowRecruits extends AbstractShowRecruits
{

    ShowRecruits(JFrame parentFrame, Point point, MasterHex hex,
        JScrollPane pane, Variant variant, ClientGUI gui)
    {
        super(parentFrame);

        doOneTerrain(hex.getTerrain(), hex, variant, gui);

        pack();

        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }

        setVisible(true);
        repaint();
    }

}
