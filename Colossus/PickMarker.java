import java.awt.*;
import java.awt.event.*;

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
            setLayout(new GridLayout(0, Math.min(
                player.getNumMarkersAvailable(), 12)));

            pack();

            setBackground(Color.lightGray);
            setResizable(false);

            for (int i = 0; i < player.getNumMarkersAvailable(); i++)
            {
                markers[i] = new Chit(-1, -1, scale,  
                    "images/" + player.getMarker(i) + ".gif", this);
                add(markers[i]);
                markers[i].addMouseListener(this);
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
                new MessageBox(parentFrame, e.toString() +
                    " waitForAll was interrupted");
            }
            imagesLoaded = true;

            pack();
            
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));

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

        // Create the back buffer only if we don't have a good one.
        if (offGraphics == null || d.width != offDimension.width ||
            d.height != offDimension.height)
        {
            offDimension = d;
            offImage = createImage(2 * d.width, 2 * d.height);
            offGraphics = offImage.getGraphics();
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

        Object source = e.getSource();
        for (int i = 0; i < markers.length; i++)
        {
            if (markers[i] == source)
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


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        player.clearSelectedMarker();
        dispose();
        return;
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
