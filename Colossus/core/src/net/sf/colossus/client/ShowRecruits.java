package net.sf.colossus.client;


import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JScrollPane;


/**
 * Displays recruit trees for a single Hex type.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */

final class ShowRecruits extends AbstractShowRecruits
{

    ShowRecruits(JFrame parentFrame, String terrain, Point point,
            String terrainHexLabel, JScrollPane pane)
    {
        super(parentFrame);

        doOneTerrain(terrain, terrainHexLabel);

        pack();

        if (point != null)
        {
            placeRelative(parentFrame, point, pane);
        }

        setVisible(true);
        repaint();
    }

}
