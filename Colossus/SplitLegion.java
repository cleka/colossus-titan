import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Class SplitLegion allows a player to split a Legion into two Legions.
 * @version $Id$
 * @author David Ripton
 */

public final class SplitLegion extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private Legion oldLegion;
    private ArrayList oldChits = new ArrayList(8);
    private ArrayList newChits = new ArrayList(8);

    // Use Chits instead of markers since we won't paint heights.
    private Chit oldMarker;
    private Chit newMarker;

    private Player player;
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private static boolean active;
    private String selectedMarkerId;

    /** new marker id,creature1,creature2... */
    private static String results;


    private SplitLegion(JFrame parentFrame, Legion oldLegion, String name,
        String selectedMarkerId)
    {
        super(parentFrame, name + ": Split Legion " +
            oldLegion.getLongMarkerName(), true);

        Container contentPane = getContentPane();
        contentPane.setLayout(gridbag);

        this.oldLegion = oldLegion;
        this.parentFrame = parentFrame;
        Game game = oldLegion.getGame();
        player = oldLegion.getPlayer();

        if (selectedMarkerId == null)
        {
            dispose();
            return;
        }

        pack();

        int scale = 4 * Scale.get();

        newMarker = new Chit(scale, selectedMarkerId, this);

        setBackground(Color.lightGray);
        setResizable(false);

        addMouseListener(this);
        addWindowListener(this);

        oldMarker = new Chit(scale, oldLegion.getImageName(), this);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        gridbag.setConstraints(oldMarker, constraints);
        contentPane.add(oldMarker);

        Collection critters = oldLegion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            oldChits.add(chit);
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
        }

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        gridbag.setConstraints(newMarker, constraints);
        contentPane.add(newMarker);

        JButton button1 = new JButton("Done");
        button1.setMnemonic(KeyEvent.VK_D);
        JButton button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);

         // Attempt to center the buttons.
        int chitWidth = Math.max(oldChits.size(),
            newChits.size()) + 1;
        if (chitWidth < 4)
        {
            constraints.gridwidth = 1;
        }
        else
        {
            constraints.gridwidth = 2;
        }
        int leadSpace = (chitWidth - 2 * constraints.gridwidth) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        constraints.gridx = leadSpace;
        constraints.gridy = 2;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);
        constraints.gridx = leadSpace + constraints.gridwidth;
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
        button2.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
    }


    public static String splitLegion(JFrame parentFrame, Legion oldLegion,
        String selectedMarkerId)
    {
        if (!active)
        {
            active = true;
            new SplitLegion(parentFrame, oldLegion, oldLegion.getPlayerName(),
                selectedMarkerId);
            active = false;
            return results;
        }
        return null;
    }


    // Move a chit to the end of the other line.
    private void moveChitToOtherLine(ArrayList fromChits, ArrayList
        toChits, int oldPosition, int gridy)
    {
        Chit chit = (Chit)fromChits.remove(oldPosition);
        remove(chit);

        toChits.add(chit);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = gridy;
        constraints.gridwidth = 1;
        gridbag.setConstraints(chit, constraints);

        Container contentPane = getContentPane();
        contentPane.add(chit);

        pack();
        repaint();
    }


    private void cancel()
    {
        results = null;
        dispose();
    }

    private void performSplit()
    {
        StringBuffer buf = new StringBuffer(newMarker.getId());
        Iterator it = newChits.iterator();
        while (it.hasNext())
        {
            buf.append(",");
            Chit chit = (Chit)it.next();
            buf.append(chit.getId());
        }
        results = buf.toString();
        dispose();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = oldChits.indexOf(source);
        if (i != -1)
        {
            moveChitToOtherLine(oldChits, newChits, i, 1);
            return;
        }
        else
        {
            i = newChits.indexOf(source);
            if (i != -1)
            {
                moveChitToOtherLine(newChits, oldChits, i, 0);
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
        cancel();
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


    /**
     *  Return true if the split is legal. Each legion must have
     *  height >= 2.  If this was an initial split, each legion
     *  must have height == 4 and one lord.
     */
    private boolean isSplitLegal()
    {
        if (oldChits.size() < 2 || newChits.size() < 2)
        {
            JOptionPane.showMessageDialog(parentFrame, "Legion too short.");
            return false;
        }
        if (oldChits.size() + newChits.size() == 8)
        {
            if (oldChits.size() != newChits.size())
            {
                JOptionPane.showMessageDialog(parentFrame,
                    "Initial split must be 4-4.");
                return false;
            }

            int numLords = 0;
            Iterator it = oldChits.iterator();
            while (it.hasNext())
            {
                Chit chit = (Chit)it.next();
                String id = chit.getId();
                if (id.startsWith("Titan") || id.equals("Angel"))
                {
                    numLords++;
                }
            }
            if (numLords != 1)
            {
                JOptionPane.showMessageDialog(parentFrame,
                    "Each stack must have one lord.");
                return false;
            }
        }
        return true;
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Done"))
        {
            if (!isSplitLegal())
            {
                return;
            }
            performSplit();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            cancel();
        }
    }


    public static void main(String [] args)
    {
        JFrame frame = new JFrame("testing SplitLegion");
        int scale = Scale.get();
        frame.setSize(new Dimension(80 * scale, 80 * scale));
        frame.pack();
        frame.setVisible(true);

        Game game = new Game();
        game.initBoard();
        MasterHex hex = game.getBoard().getHexByLabel("130");
        game.addPlayer("Test");
        Player player = game.getPlayer(0);
        player.setTower(1);
        player.setColor("Red");
        player.initMarkersAvailable();
        String selectedMarkerId = player.selectMarkerId("Rd01");
        Legion legion = Legion.getStartingLegion(selectedMarkerId,
            hex.getLabel(), player.getName(), game);
        player.addLegion(legion);
        Marker marker = new Marker(4 * scale, selectedMarkerId, frame, game);
        legion.setMarker(marker);

        String retval = SplitLegion.splitLegion(frame, legion,
            player.pickMarker());
        System.out.println(retval);
        System.exit(0);
    }
}
