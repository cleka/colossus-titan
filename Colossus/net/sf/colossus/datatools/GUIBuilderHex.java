package net.sf.colossus.datatools;

import net.sf.colossus.client.BasicGUIBattleHex;

/**
 * Class GUIBuilderHex.
 * @version $Id$
 * @author Romain Dolbeau
 */

class GUIBuilderHex extends BasicGUIBattleHex
{
    private BuilderHexMap map;
    GUIBuilderHex(int cx, int cy, int scale, BuilderHexMap map, int xCoord, int yCoord)
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
