package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.util.KDialog;


/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * @author David Ripton
 */


final class PickMarker extends KDialog implements MouseListener, WindowListener
{
    private java.util.List markers = new ArrayList();
    private Client client;


    PickMarker(JFrame parentFrame, String name, Set markerIds, Client client)
    {
        super(parentFrame, name + ": Pick Legion Marker", false);

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

        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            Marker marker = new Marker(4 * Scale.get(), markerId, this, null);
            markers.add(marker);
            contentPane.add(marker);
            marker.addMouseListener(this);
        }

        pack();
        centerOnScreen();
        setVisible(true);
    }


    /** Pass the chosen marker id, or null if none are available or
     *  the player aborts the selection. */
    private void cleanup(String markerId)
    {
        dispose();
        client.pickMarkerCallback(markerId);
    }

    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = markers.indexOf(source);
        if (i != -1)
        {
            Chit chit = (Chit)markers.get(i);
            cleanup(chit.getId());
        }
    }

    public void windowClosing(WindowEvent e)
    {
        cleanup(null);
    }
}
