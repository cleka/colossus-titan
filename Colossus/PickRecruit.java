import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */


public final class PickRecruit extends JDialog implements MouseListener,
    WindowListener
{
    private ArrayList recruits = new ArrayList();
    private Player player;
    private Legion legion;
    private ArrayList recruitChits = new ArrayList();
    private Marker legionMarker;
    private ArrayList legionChits = new ArrayList();
    private static final int scale = 60;
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static Creature recruit;
    private static boolean active;


    private PickRecruit(JFrame parentFrame, Legion legion)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruit in " + legion.getCurrentHex().getDescription(),
            true);

        if (!legion.canRecruit())
        {
            dispose();
            return;
        }

        this.parentFrame = parentFrame;

        recruits = Game.findEligibleRecruits(legion);
        int numEligible = recruits.size();

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);

        setResizable(false);

        legionMarker = new Marker(scale, legion.getImageName(), this, legion);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        gridbag.setConstraints(legionMarker, constraints);
        contentPane.add(legionMarker);

        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            legionChits.add(chit);
            constraints.gridy = 0;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        int height = critters.size();
        // There are height + 1 chits in the top row.  There
        // are numEligible chits / labels to place beneath.
        // So we have (height + 1) - numEligible empty
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        it = recruits.iterator();
        int i = 0;
        while (it.hasNext())
        {
            Creature recruit = (Creature)it.next();
            Chit chit = new Chit(scale, recruit.getImageName(), this);
            recruitChits.add(chit);

            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
            int count = recruit.getCount();
            JLabel countLabel = new JLabel(Integer.toString(count),
                JLabel.CENTER);
            constraints.gridy = 2;
            gridbag.setConstraints(countLabel, constraints);
            contentPane.add(countLabel);
            i++;
        }

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    /** Return the creature recruited, or null if none. */
    public static Creature pickRecruit(JFrame parentFrame, Legion legion)
    {
        recruit = null;
        if (!active)
        {
            active = true;
            new PickRecruit(parentFrame, legion);
            active = false;
        }
        return recruit;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruitChits.indexOf(source);
        if (i != -1)
        {
            // Recruit the chosen creature.
            recruit = (Creature)recruits.get(i);
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
        JFrame frame = new JFrame("testing PickRecruit");
        frame.setSize(new Dimension(20 * scale, 20 * scale));
        frame.pack();
        frame.setVisible(true);

        MasterHex hex = new MasterHex(0, 0, 0, false, null);
        hex.setTerrain('B');

        Player player = new Player("Test", null);
        Legion legion = new Legion("Bk01", null, hex, hex,
            Creature.titan, Creature.gargoyle, Creature.gargoyle,
            Creature.cyclops, Creature.cyclops, null,
            null, null, player);

        Creature creature = PickRecruit.pickRecruit(frame, legion);
        System.out.println("recruited " + creature);
    }
}
