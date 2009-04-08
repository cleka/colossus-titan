package net.sf.colossus.gui;


import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import net.sf.colossus.common.IVariant;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Displays recruit trees for a single Hex type.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */

final class ShowRecruits extends AbstractShowRecruits
{

    ShowRecruits(JFrame parentFrame, Point point, MasterHex hex,
        JScrollPane pane, Variant variant, IVariant ivariant)
    {
        super(parentFrame);

        doOneTerrain(hex.getTerrain(), hex, variant, ivariant);

        pack();

        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }

        setVisible(true);
        repaint();
    }

}
