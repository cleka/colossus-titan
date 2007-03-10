package net.sf.colossus.client;


import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JScrollPane;


/**
 * Displays recruit trees for all MasterHex types.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */

final class ShowAllRecruits extends AbstractShowRecruits
{
    private SaveWindow saveWindow;
    
    // Avoid showing multiple allTerrains displays.
    private static boolean allTerrainsDisplayActive = false;

    ShowAllRecruits(JFrame parentFrame, IOptions options, String[] terrains, 
        JScrollPane pane)
    {
        super(parentFrame);

        if (allTerrainsDisplayActive)
        {
            super.dispose();
            return;
        }
        allTerrainsDisplayActive = true;
        
        for (int i = 0; i < terrains.length; i++)
        {
            doOneTerrain(terrains[i], null);
        }

        pack();

        saveWindow = new SaveWindow(options, "RecruitsScreen");
        Point loadLocation = saveWindow.loadLocation();
        if (loadLocation == null) 
        {
            lowerRightCorner();
        }
        else 
        {
            setLocation(loadLocation);
        }

        setVisible(true);
        repaint();
    }

    public void dispose()
    {
        allTerrainsDisplayActive = false;
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
}