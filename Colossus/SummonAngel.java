import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class SummonAngel allows a player to Summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public class SummonAngel extends JDialog implements MouseListener, 
    ActionListener, WindowListener
{
    private Player player;
    private Legion legion;
    private MasterBoard board;
    private static final int scale = 60;
    private Chit angelChit;
    private Chit archangelChit;
    private Legion donor;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private JButton button1;
    private JButton button2;
    private boolean summoned;


    public SummonAngel(MasterBoard board, Legion legion)
    {
        super(board.getFrame(), legion.getPlayer().getName() +
            ": Summon Angel into Legion " + legion.getMarkerId(), false);

        this.legion = legion;
        player = legion.getPlayer();
        this.board = board;

        // Paranoia
        if (!legion.canSummonAngel())
        {
            cleanup(null);
            return;
        }

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (board.getGame().highlightSummonableAngels(legion) < 1)
        {
            cleanup(null);
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        angelChit = new Chit(scale, Creature.angel.getImageName(), this);
        constraints.gridy = 0;
        gridbag.setConstraints(angelChit, constraints);
        contentPane.add(angelChit);
        angelChit.addMouseListener(this);

        archangelChit = new Chit(scale, Creature.archangel.getImageName(),
            this);
        constraints.gridy = 0;
        gridbag.setConstraints(archangelChit, constraints);
        contentPane.add(archangelChit);
        archangelChit.addMouseListener(this);

        // X out chits since no legion is selected.
        angelChit.setDead(true);
        archangelChit.setDead(true);

        button1 = new JButton("Summon");
        button1.setMnemonic(KeyEvent.VK_S);
        button2 = new JButton("Cancel");
        button2.setMnemonic(KeyEvent.VK_C);
        constraints.gridy = 1;
        gridbag.setConstraints(button1, constraints);
        contentPane.add(button1);
        button1.addActionListener(this);
        gridbag.setConstraints(button2, constraints);
        contentPane.add(button2);
        button2.addActionListener(this);

        pack();
        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    public Legion getLegion()
    {
        return legion;
    }


    public boolean getSummoned()
    {
        return summoned;
    }


    private void cleanup(Creature creature)
    {
        // Sanity check, just in case this got called twice.
        if (creature != null && legion.canSummonAngel())
        {
            summoned = true;

            // Only one angel can be summoned per turn.
            player.disallowSummoningAngel();
            player.setLastLegionSummonedFrom(donor);

            // Move the angel or archangel.
            donor.removeCreature(creature, false, true);
            legion.addCreature(creature, false);

            // Update the number of creatures displayed in both stacks.
            donor.getCurrentHex().repaint();
            legion.getCurrentHex().repaint();
        
            Game.logEvent("An " + creature.getName() + 
                " was summoned from legion " + donor.getMarkerId() +
                " into legion " + legion.getMarkerId());
        }

        dispose();

        // Let the game know to leave the angel-summoning state.
        board.getGame().finishSummoningAngel();
    }


    public void mousePressed(MouseEvent e)
    {
        donor = player.getDonor();
        if (donor == null)
        {
            return;
        }

        Object source = e.getSource();

        if (angelChit == source && !angelChit.isDead())
        {
            cleanup(Creature.angel);
        }

        else if (archangelChit == source && !archangelChit.isDead())
        {
            cleanup(Creature.archangel);
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


    /** Upstate state of angel and archangel chits to reflect donor */
    public void updateChits()
    {
        donor = player.getDonor();
        if (donor == null)
        {
            return;
        }

        int angels = donor.numCreature(Creature.angel);
        int archangels = donor.numCreature(Creature.archangel);

        angelChit.setDead(angels == 0);
        archangelChit.setDead(archangels == 0);
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Summon"))
        {
            donor = player.getDonor();
            if (donor == null)
            {
                JOptionPane.showMessageDialog(board, "Must select a legion.");
                return;
            }

            int angels = donor.numCreature(Creature.angel);
            int archangels = donor.numCreature(Creature.archangel);

            if (angels == 0 && archangels == 0)
            {
                JOptionPane.showMessageDialog(board, 
                    "No angels are available.");
                return;
            }

            if (archangels == 0)
            {
                // Must take an angel.
                cleanup(Creature.angel);
            }
            else if (angels == 0)
            {
                // Must take an archangel.
                cleanup(Creature.archangel);
            }
            else
            {
                // If both are available, make the player click on one.
                JOptionPane.showMessageDialog(board, 
                    "Select angel or archangel.");
            }
        }

        else if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null);
        }
    }
}
