package net.sf.colossus.client;


import java.awt.*;
import java.util.*;
import java.awt.geom.*;


/**
 * Class GUIBattleHex holds GUI info for one battle hex, and add the ability
 * to repaint itself in an HexMap.
 * @version $Id$
 * @author David Ripton
 */

final class GUIBattleHex extends BasicGUIBattleHex
{
    private HexMap map;
    GUIBattleHex(int cx, int cy, int scale, HexMap map, int xCoord, int yCoord)
    {
        super(cx, cy, scale, xCoord, yCoord);

        this.map = map;
    }

    public void repaint()
    {
        // If an entrance needs repainting, paint the whole map.
        if (isEntrance())
        {
            map.repaint();
        }
        else
        {
            map.repaint(getBounds().x, getBounds().y, getBounds().width,
                        getBounds().height);
        }
    }
}
