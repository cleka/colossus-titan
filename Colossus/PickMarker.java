import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * author David Ripton
 */


class PickMarker extends Dialog implements MouseListener, WindowListener
{
    private Chit [] markers;
    private MediaTracker tracker;
    private boolean imagesLoaded = false;
    private Player player;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;


    PickMarker(Frame parentFrame, Player player)
    {
        super(parentFrame, player.getName() + ": Pick Legion Marker", true);

        this.player = player;
        markers = new Chit[player.getNumMarkersAvailable()];

        addMouseListener(this);
        addWindowListener(this);

        if (player.getNumMarkersAvailable() == 0)
        {
            new MessageBox(parentFrame, "No markers available");
            dispose();
        }
        else
        {
            int scale = 60;
            setLayout(null);

            pack();

            setBackground(Color.lightGray);
            setSize((21 * scale / 20) * (Math.min(12,
                player.getNumMarkersAvailable()) + 1), (21 * scale / 20) *
                ((player.getNumMarkersAvailable() - 1) / 12 + 2));
            setResizable(false);

            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));

            int cx = scale / 2;
            int cy = scale * 2 / 3;

            for (int i = 0; i < player.getNumMarkersAvailable(); i++)
            {
                markers[i] = new Chit(cx + (i % 12) * (21 * scale / 20),
                    cy + (i / 12) * (21 * scale / 20), scale,
                    "images" + File.separator + player.getMarker(i) +
                    ".gif", this);
            }

            tracker = new MediaTracker(this);

            for (int i = 0; i < markers.length; i++)
            {
                tracker.addImage(markers[i].getImage(), 0);
            }

            try
            {
                tracker.waitForAll();
            }
            catch (InterruptedException e)
            {
                new MessageBox(parentFrame, "waitForAll was interrupted");
            }
            imagesLoaded = true;
            setVisible(true);
            repaint();
        }
    }


    public void update(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        Dimension d = getSize();
        Rectangle rectClip = g.getClipBounds();

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
        }

        for (int i = 0; i < markers.length; i++)
        {
            if (rectClip.intersects(markers[i].getBounds()))
            {
                markers[i].paint(offGraphics);
            }
        }

        g.drawImage(offImage, 0, 0, this);
    }


    public void paint(Graphics g)
    {
        // Double-buffer everything.
        update(g);
    }


    public void mousePressed(MouseEvent e)
    {
        if (player.getNumMarkersAvailable() == 0)
        {
            player.clearSelectedMarker();
            dispose();
            return;
        }

        Point point = e.getPoint();
        for (int i = 0; i < markers.length; i++)
        {
            if (markers[i].select(point))
            {
                // Select that marker.
                player.selectMarker(i);

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


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent event)
    {
    }


    public void windowClosed(WindowEvent event)
    {
    }


    public void windowClosing(WindowEvent event)
    {
        player.clearSelectedMarker();
        dispose();
        return;
    }


    public void windowDeactivated(WindowEvent event)
    {
    }


    public void windowDeiconified(WindowEvent event)
    {
    }


    public void windowIconified(WindowEvent event)
    {
    }


    public void windowOpened(WindowEvent event)
    {
    }
}
