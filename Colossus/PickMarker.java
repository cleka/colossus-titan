import java.awt.*;
import java.awt.event.*;

/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * author David Ripton
 */


class PickMarker extends Dialog implements MouseListener
{
    Chit [] markers;
    MediaTracker tracker;
    boolean imagesLoaded;
    Player player;

    PickMarker(Frame parentFrame, Player player)
    {
        super(parentFrame, player.name + ": Pick Legion Marker", true);
        
        this.player = player;
        markers = new Chit[player.numMarkersAvailable];

        addMouseListener(this);


        if (player.numMarkersAvailable == 0)
        {
            new MessageBox(parentFrame, "No markers available");
        }
        else
        {

            int scale = 60;
            setLayout(null);

            setSize((21 * scale / 20) * (Math.min(12, 
                player.numMarkersAvailable) + 1), (21 * scale / 20) * 
                ((player.numMarkersAvailable - 1) / 12 + 2));
            
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2, 
                d.height / 2 - getSize().height / 2));

            int cx = scale / 2;
            int cy = scale * 2 / 3;

            for (int i = 0; i < player.numMarkersAvailable; i++)
            {
                markers[i] = new Chit(cx + (i % 12) * (scale + 3),
                    cy + (i / 12) * (scale + 3), scale, 
                    "images/" + player.markersAvailable[i] + ".gif", this);
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
                new MessageBox(parentFrame, "waitForAll was interrupted");
            }
        }

        pack();
        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Rectangle rectClip = g.getClipBounds();

        for (int i = markers.length - 1; i >= 0; i--)
        {
            if (rectClip.intersects(markers[i].getBounds()))
            {
                markers[i].paint(g);
            }
        }
    }


    public void mouseClicked(MouseEvent e)
    {
        if (player.numMarkersAvailable == 0)
        {
            player.markerSelected = null;
            dispose();
            return;
        }

        Point point = e.getPoint();
        for (int i = 0; i < markers.length; i++)
        {
            if (markers[i].select(point))
            {
                // Got a hit.  Send that info back by putting it in 
                //     player.markerSelected.
                player.markerSelected = new String(player.markersAvailable[i]);

                // Then adjust player to show that this marker is taken.
                for (int j = i; j < player.numMarkersAvailable - 1; j++)
                {
                    player.markersAvailable[j] = new 
                        String(player.markersAvailable[j + 1]);
                }
                player.markersAvailable[player.numMarkersAvailable - 1] = new 
                    String("");
                player.numMarkersAvailable--;

                // Then exit.
                dispose();
                return;
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
