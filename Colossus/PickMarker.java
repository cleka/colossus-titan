import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * @author David Ripton
 */


public class PickMarker extends JDialog implements MouseListener, WindowListener
{
    private ArrayList markers = new ArrayList();
    private Player player;


    public PickMarker(JFrame parentFrame, Player player)
    {
        super(parentFrame, player.getName() + ": Pick Legion Marker", true);

        this.player = player;

        addMouseListener(this);
        addWindowListener(this);

        if (player.getNumMarkersAvailable() == 0)
        {
            JOptionPane.showMessageDialog(parentFrame, "No markers available");
            setVisible(false);
            dispose();
        }
        else
        {
            int scale = 60;

            Container contentPane = getContentPane();

            contentPane.setLayout(new GridLayout(0, Math.min(
                player.getNumMarkersAvailable(), 12)));

            pack();

            setBackground(Color.lightGray);
            setResizable(false);

            Collection markerIds = player.getMarkersAvailable();
            
            Iterator it = markerIds.iterator();
            while (it.hasNext())
            {
                String markerId = (String)it.next();
                Chit marker = new Chit(scale, markerId, this);
                markers.add(marker);
                contentPane.add(marker);
                marker.addMouseListener(this);
            }

            pack();
            
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(new Point(d.width / 2 - getSize().width / 2,
                d.height / 2 - getSize().height / 2));

            setVisible(true);
            repaint();
        }
    }


    public void mousePressed(MouseEvent e)
    {
        if (player.getNumMarkersAvailable() == 0)
        {
            player.clearSelectedMarker();
            setVisible(false);
            dispose();
            return;
        }

        Object source = e.getSource();
        Iterator it = markers.iterator();
        while (it.hasNext())
        {
            Chit marker = (Chit)it.next();
            if (marker == source)
            {
                // Select that marker.
                player.selectMarker(marker.getId());

                // Then exit.
                setVisible(false);
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
        setVisible(false);
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


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing PickMarker");
        int scale = 60;
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        Player player = new Player("Test", null);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();

        new PickMarker(frame, player);
    }
}
