package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * @author David Ripton
 */


final class PickMarker extends JDialog implements MouseListener, WindowListener
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

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

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

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
        cleanup(null);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
}
