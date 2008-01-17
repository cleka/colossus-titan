package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import net.sf.colossus.game.Player;
import net.sf.colossus.util.KDialog;


/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * @author David Ripton
 */

final class PickMarker extends KDialog implements MouseListener,
    WindowListener
{
    private final List<Marker> markers = new ArrayList<Marker>();
    private Client client;
    private final SaveWindow saveWindow;

    PickMarker(JFrame parentFrame, Player owner, Set<String> markerIds,
        Client client)
    {
        super(parentFrame, owner.getName() + ": Pick Legion Marker", true);

        this.client = client;

        if (markerIds.isEmpty())
        {
            cleanup(null);
        }

        addMouseListener(this);
        addWindowListener(this);
        Container contentPane = getContentPane();

        int numAvailable = markerIds.size();
        contentPane.setLayout(new GridLayout(0, Math.min(numAvailable, 12)));

        pack();
        setBackground(Color.lightGray);

        Iterator<String> it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = it.next();
            Marker marker = new Marker(4 * Scale.get(), markerId);
            markers.add(marker);
            contentPane.add(marker);
            marker.addMouseListener(this);
        }

        pack();
        saveWindow = new SaveWindow(client.getOptions(), "PickMarker");
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

    /** Pass the chosen marker id, or null if none are available or
     *  the player aborts the selection. */
    private void cleanup(String markerId)
    {
        saveWindow.saveLocation(getLocation());
        removeMouseListener(this);
        removeWindowListener(this);
        dispose();
        client.pickMarkerCallback(markerId);
        client = null;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = markers.indexOf(source);
        if (i != -1)
        {
            Chit chit = markers.get(i);
            cleanup(chit.getId());
        }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        cleanup(null);
    }
}
