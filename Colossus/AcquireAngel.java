import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class AcquireAngel allows a player to acquire an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public final class AcquireAngel extends JDialog implements MouseListener,
    WindowListener
{
    private ArrayList chits = new ArrayList();
    private static final int scale = 60;
    private static String recruit;
    private ArrayList recruits;
    private static boolean active;


    private AcquireAngel(JFrame parentFrame, String name, ArrayList recruits)
    {
        super(parentFrame, name + ": Acquire Angel", true);

        this.recruits = recruits;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        pack();
        setBackground(Color.lightGray);
        setResizable(false);

        Iterator it = recruits.iterator();
        while (it.hasNext())
        {
            String creatureName = (String)it.next();
            Creature recruit = Creature.getCreatureFromName(creatureName);
            Chit chit = new Chit(scale, recruit.getImageName(), this);
            chits.add(chit);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    public static String acquireAngel(JFrame parentFrame, String name,
        ArrayList recruits)
    {
        recruit = null;
        if (recruits.isEmpty())
        {
            return null;
        }
        if (!active)
        {
            active = true;
            new AcquireAngel(parentFrame, name, recruits);
            active = false;
        }
        return recruit;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = chits.indexOf(source);
        if (i != -1)
        {
            recruit = (String)recruits.get(i);

            // Then exit.
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
        JFrame frame = new JFrame("testing AcquireAngel");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('B');
        hex.setLabel(130);

        Player player = new Player("Test", null);
        Legion legion = new Legion("Bk01", null, hex.getLabel(),
            hex.getLabel(), Creature.titan, Creature.gargoyle,
            Creature.cyclops, Creature.behemoth, Creature.serpent,
            Creature.warlock, null, null, player);

        ArrayList recruits = new ArrayList();
        recruits.add("Archangel");
        recruits.add("Angel");

        String type = AcquireAngel.acquireAngel(frame, player.getName(),
            recruits);

        Game.logEvent("Chose " + type);
    }
}
