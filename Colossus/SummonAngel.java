import java.awt.*;
import java.awt.event.*;

/**
 * Class SummonAngel allows a player to Summon an angel or archangel.
 * @version $Id$
 * author David Ripton
 */


class SummonAngel extends Dialog implements ActionListener, WindowListener
{
    private int numEligible = 0;
    private Creature [] recruits;
    private MediaTracker tracker;
    private Player player;
    private Legion legion;
    MasterBoard board;
    Button button1;
    Button button2;
    private final int scale = 60;
    boolean laidOut = false;


    SummonAngel(MasterBoard board, Legion legion)
    {
        super(board, legion.getPlayer().getName() + 
            ": Summon Angel", false);
        
        this.legion = legion;
        player = legion.getPlayer();
        this.board = board;

        if (player.canSummonAngel() == false || legion.getHeight() > 6)
        {
            dispose();
            return;
        }

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (board.highlightSummonableAngels(legion) < 1)
        {
            dispose();
            return;
        }

        setResizable(false);
        addWindowListener(this);
        setLayout(null);

        setSize(getPreferredSize());
            
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, 
            d.height / 2 - getSize().height / 2));

        button1 = new Button("Summon");
        button2 = new Button("Cancel");
        add(button1);
        add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

        pack();
        setVisible(true);
        repaint();
    }


    public void paint(Graphics g)
    {
        if (!laidOut)
        {
            Insets insets = getInsets();
            Dimension d = getSize();
            button1.setBounds(insets.left + d.width / 9, 7 * d.height / 8 - 
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right, 
                7 * d.height / 8 - insets.bottom, d.width / 3, d.height / 8);
        }
    }

    public void windowActivated(WindowEvent event)
    {
    }

    public void windowClosed(WindowEvent event)
    {
    }

    public void windowClosing(WindowEvent event)
    {
        board.finishSummoningAngel();
        dispose();
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


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand() == "Summon")
        {
            Legion donor = player.getSelectedLegion();
            if (donor == null) 
            {
                new MessageBox(board, "Must select a legion.");
                return;
            }

            int angels = donor.numCreature(Creature.angel);
            int archangels = donor.numCreature(Creature.archangel);

            if (angels == 0 && archangels == 0)
            {
                new MessageBox(board, "No angels are available.");
                return;
            }

            if (archangels == 0)
            {
                // Must take an angel.
                donor.removeCreature(Creature.angel);
                legion.addCreature(Creature.angel);
            }
            else if (angels == 0)
            {
                // Must take an archangel.
                donor.removeCreature(Creature.archangel);
                legion.addCreature(Creature.archangel);
            }
            else
            {
                // XXX: If both are available, let the player choose.
                donor.removeCreature(Creature.archangel);
                legion.addCreature(Creature.archangel);
            }

            // Only one angel can be summoned per turn.
            player.disallowSummoningAngel();
            board.finishSummoningAngel();
            dispose();
        }

        else if (e.getActionCommand() == "Cancel")
        {
            board.finishSummoningAngel();
            dispose();
        }
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(8 * scale, 2 * scale);
    }
}
