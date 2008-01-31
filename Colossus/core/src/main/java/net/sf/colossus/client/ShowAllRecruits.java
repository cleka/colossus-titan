package net.sf.colossus.client;


import java.awt.Point;
import java.util.Collection;

import javax.swing.JFrame;

import net.sf.colossus.variant.MasterBoardTerrain;


/**
 * Displays recruit trees for all MasterHex types.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */

final class ShowAllRecruits extends AbstractShowRecruits
{
    private final SaveWindow saveWindow;

    // Avoid showing multiple allTerrains displays.
    private static boolean allTerrainsDisplayActive = false;

    ShowAllRecruits(JFrame parentFrame, IOptions options,
        Collection<MasterBoardTerrain> terrains)
    {
        super(parentFrame);

        if (allTerrainsDisplayActive)
        {
            // TODO another bit of boilout code that ain't no good
            super.dispose();
            saveWindow = null; // otherwise it couldn't be final
            return;
        }
        allTerrainsDisplayActive = true;

        for (MasterBoardTerrain terrain : terrains)
        {
            doOneTerrain(terrain, null);
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

    @Override
    public void dispose()
    {
        allTerrainsDisplayActive = false;
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
}
