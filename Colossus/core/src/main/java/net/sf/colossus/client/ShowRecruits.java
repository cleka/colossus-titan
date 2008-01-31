package net.sf.colossus.client;


import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.variant.MasterHex;


/**
 * Displays recruit trees for a single Hex type.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */

final class ShowRecruits extends AbstractShowRecruits
{

    ShowRecruits(JFrame parentFrame, Point point, MasterHex hex,
        JScrollPane pane)
    {
        super(parentFrame);

        doOneTerrain(hex.getTerrain(), hex);

        pack();

        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }

        setVisible(true);
        repaint();
    }

}
