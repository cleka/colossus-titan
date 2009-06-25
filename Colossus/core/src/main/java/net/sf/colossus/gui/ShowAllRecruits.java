package net.sf.colossus.gui;


import java.awt.Point;
import java.util.Collection;

import javax.swing.JFrame;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.guiutil.SaveWindow;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.Variant;


/**
 * Displays recruit trees for all MasterHex types.
 *
 * @author David Ripton
 * @author Barrie Treloar
 */
final class ShowAllRecruits extends AbstractShowRecruits
{
    private final SaveWindow saveWindow;

    // Avoid showing multiple allTerrains displays.
    private static boolean allTerrainsDisplayActive = false;

    ShowAllRecruits(JFrame parentFrame, IOptions options, Variant variant,
        ClientGUI gui)
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

        Collection<MasterBoardTerrain> terrains = variant.getTerrains();
        for (MasterBoardTerrain terrain : terrains)
        {
            if (!terrain.isAlias())
            {
                doOneTerrain(terrain, null, variant, gui);
            }
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
