import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class PickMarker allows a player to pick a legion marker.
 * @version $Id$
 * @author David Ripton
 */


public final class PickMarker extends JDialog implements MouseListener,
    WindowListener
{
    private ArrayList markers = new ArrayList();
    private static String markerId;
    private Game game;


    private PickMarker(JFrame parentFrame, String name, Collection markerIds,
        Game game)
    {
        super(parentFrame, name + ": Pick Legion Marker", true);

        this.game = game;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        int numAvailable = markerIds.size();
        contentPane.setLayout(new GridLayout(0, Math.min(numAvailable, 12)));

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        Iterator it = markerIds.iterator();
        while (it.hasNext())
        {
            String markerId = (String)it.next();
            Marker marker = new Marker(4 * Scale.get(), markerId, this, game);
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


    /** Return the chosen marker id, or null if none are available or
     *  the player aborts the selection. */
    public static String pickMarker(JFrame parentFrame, String name,
        Collection markerIds, Game game)
    {
        if (markerIds.isEmpty())
        {
            return null;
        }

        markerId = null;

        new PickMarker(parentFrame, name, markerIds, game);

        return markerId;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = markers.indexOf(source);
        if (i != -1)
        {
            Chit chit = (Chit)markers.get(i);
            markerId = chit.getId();
            dispose();
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
        dispose();
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
        int scale = Scale.get();
        frame.setSize(new Dimension(80 * scale, 80 * scale));
        frame.pack();
        frame.setVisible(true);

        Game game = new Game();
        game.addPlayer("Test");
        Player player = game.getPlayer(0);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();

        String choice = PickMarker.pickMarker(frame, player.getName(),
            player.getMarkersAvailable(), game);
        // XXX Pass a deep clone rather than the original list.
        // Or at least call Collections.unmodifyableSortedSet()
        Game.logEvent("Chose " + choice);
    }
}
