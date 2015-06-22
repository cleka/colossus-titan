package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.colossus.common.IOptions;
import net.sf.colossus.game.Legion;
import net.sf.colossus.game.Player;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Class PickMarker allows a player to pick a legion marker,
 * either as initial marker or for splitting a legion.
 *
 * @author David Ripton
 * @author Clemens Katzer
 */

final class PickMarker extends KDialog
{
    private final ClientGUI gui;

    private final Set<String> markerIds;

    private final SaveWindow saveWindow;

    // if null, it's pick initial marker, otherwise it's for split legion
    private final Legion parent;

    PickMarker(ClientGUI gui, Set<String> markerIds, Legion parent)
    {
        super(gui.getBoard().getFrame(), "dummy title", true);
        this.gui = gui;
        this.parent = parent;
        this.markerIds = markerIds;

        IOptions options = gui.getOptions();
        Player owner = gui.getClient().getOwningPlayer();

        if (parent == null)
        {
            setTitle(owner.getName() + ": Pick initial Legion Marker!");
        }
        else
        {
            setTitle(owner.getName() + ": Pick Marker for new Legion!");
        }

        List<Marker> markers = new ArrayList<Marker>();

        if (markerIds.isEmpty())
        {
            cleanup(null);
        }

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup(null);
            }

        });
        Container contentPane = getContentPane();

        int numAvailable = markerIds.size();
        contentPane.setLayout(new GridLayout(0, Math.min(numAvailable, 6)));

        pack();
        setBackground(Color.lightGray);

        MouseAdapter mouseListener = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                Marker marker = (Marker)e.getComponent();
                cleanup(marker.getId());
            }
        };

        for (String markerId : markerIds)
        {
            Marker marker = new Marker(null, 4 * Scale.get(), markerId + "-"
                + owner.getColor().getName());
            markers.add(marker);
            contentPane.add(marker);
            marker.addMouseListener(mouseListener);
        }

        pack();
        saveWindow = new SaveWindow(options, "PickMarker");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
    }

    /**
     * If parent != null, it's about split. Make the GUI initiate the
     * dialog where user chooses creatures to split into new marker.
     *
     * If parent is null, it's about the initial split, thus we insist
     * on getting one, and once one is selected, make the client send
     * the assignFirstMarker to server.
     *
     * @param pickedMarkerId The markerId the user has choosen, or null
     * if dialog was closed without choosing.
     *
     */
    private void cleanup(String pickedMarkerId)
    {
        saveWindow.saveLocation(getLocation());
        dispose();

        if (parent != null)
        {
            // CLient will either initiate the split, or if pickedMarkerId
            // is null, just forget about it :)
            gui.getClient().doTheSplitting(parent, pickedMarkerId);
        }
        else
        {
            if (pickedMarkerId == null)
            {
                new PickMarker(gui, markerIds, parent);
            }
            else
            {
                gui.getClient().assignFirstMarker(pickedMarkerId);
            }
        }
    }

}
