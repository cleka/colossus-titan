import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class SummonAngel allows a player to Summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


public final class SummonAngel extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private Player player;
    private Legion legion;
    private MasterBoard board;
    private Game game;
    private Chit angelChit;
    private Chit archangelChit;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private JButton button1;
    private JButton button2;
    private static boolean active;


    private SummonAngel(Game game, Legion legion)
    {
        super(game.getMasterFrame(), legion.getPlayerName() +
            ": Summon Angel into Legion " + legion.getLongMarkerName(), false);

        this.game = game;
        this.legion = legion;
        player = legion.getPlayer();
        this.board = game.getBoard();

        // Paranoia
        if (!legion.canSummonAngel())
        {
            cleanup(null, null);
            return;
        }

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (game.highlightSummonableAngels(legion) < 1)
        {
            cleanup(null, null);
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        int scale = 4 * Scale.get();

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

    public static SummonAngel summonAngel(Game game, Legion legion)
    {
        if (!active)
        {
            active = true;
            return new SummonAngel(game, legion);
        }
        return null;
    }


    public Legion getLegion()
    {
        return legion;
    }

    private void cleanup(Legion donor, Creature angel)
    {
        game.doSummon(legion, donor, angel);
        dispose();
        active = false;
    }


    public void mousePressed(MouseEvent e)
    {
        Legion donor = player.getDonor();
        if (donor == null)
        {
            return;
        }
        Object source = e.getSource();
        if (angelChit == source && !angelChit.isDead())
        {
            cleanup(donor, Creature.angel);
        }
        else if (archangelChit == source && !archangelChit.isDead())
        {
            cleanup(donor, Creature.archangel);
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
        cleanup(null, null);
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
        Legion donor = player.getDonor();
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
            Legion donor = player.getDonor();
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
                cleanup(donor, Creature.angel);
            }
            else if (angels == 0)
            {
                // Must take an archangel.
                cleanup(donor, Creature.archangel);
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
            cleanup(null, null);
        }
    }
}
