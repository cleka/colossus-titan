import java.awt.*;
import java.awt.event.*;

/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * author David Ripton
 */


class PickMarker extends Dialog implements MouseListener
//class PickMarker extends Frame implements MouseListener
{
    Chit [] markers;
    int scale;
    MediaTracker tracker;
    boolean imagesLoaded;
    Player player;

    PickMarker(Frame parentFrame, Player player)
    {
        // Make the dialog modal
        super(parentFrame, "Pick Legion Marker", true);
        //super("Pick Legion Marker");

        this.player = player;

        scale = 17;
        int cx = 5 * scale;
        int cy = 5 * scale;

        pack();
        setSize((scale + 3) * Math.max(12, player.numMarkersAvailable),
            (scale + 3) * (player.numMarkersAvailable / 12) + 1);
        setVisible(true);
        addMouseListener(this);

        markers = new Chit[player.numMarkersAvailable];
        for (int i = 0; i < player.numMarkersAvailable; i++)
        {
            markers[i] = new Chit(cx + (i % 12) * (scale + 3),
                cy + (i / 12) * (scale + 3), scale, 
                player.markersAvailable[i], this);
        }

        imagesLoaded = false;
        tracker = new MediaTracker(this);

        for (int i = 0; i < markers.length; i++)
        {
            tracker.addImage(markers[i].image, 0);
        }

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            System.out.println("waitForAll was interrupted");
        }

        imagesLoaded = true;
        repaint();
    }


    public void mouseClicked(MouseEvent e)
    {
        Point point = e.getPoint();
        for (int i = 0; i < markers.length; i++)
        {
            if (markers[i].select(point))
            {
                // Got a hit.
                // Send that info back by putting it in player.markerSelected.
                player.markerSelected = new String(player.markersAvailable[i]);

                // Then adjust player to show that this marker is taken.
                for (int j = i; j < player.numMarkersAvailable - 1; j++)
                {
                    player.markersAvailable[j] = 
                        new String(player.markersAvailable[j + 1]);
                }
                player.markersAvailable[player.numMarkersAvailable - 1] =
                    new String("");
                player.numMarkersAvailable--;

                // Then exit.
                // XXX Is this the right way for a modal dialog to exit?
                dispose();
            }
        }
    }
    
    
    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }
    
    public void mousePressed(MouseEvent e)
    {
    }
    
    public void mouseReleased(MouseEvent e)
    {
    }
}
