import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;

/**
 * Class SummonAngel allows a player to Summon an angel or archangel.
 * @version $Id$
 * author David Ripton
 */


class SummonAngel extends JDialog implements MouseListener, ActionListener,
    WindowListener
{
    private int numEligible = 0;
    private Creature [] recruits;
    private MediaTracker tracker;
    private Player player;
    private Legion legion;
    private MasterBoard board;
    private JButton button1;
    private JButton button2;
    private static final int scale = 60;
    private boolean laidOut = false;
    private Chit angelChit;
    private Chit archangelChit;
    private boolean imagesLoaded = false;
    private Legion donor;


    SummonAngel(MasterBoard board, Legion legion)
    {
        super(board, legion.getPlayer().getName() + 
            ": Summon Angel into Legion " + legion.getMarkerId(), false);
        
        this.legion = legion;
        player = legion.getPlayer();
        this.board = board;

        if (player.canSummonAngel() == false || legion.getHeight() > 6)
        {
            cleanup(false);
            return;
        }

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (board.highlightSummonableAngels(legion) < 1)
        {
            cleanup(false);
            return;
        }

        setResizable(false);
        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(null);
        setBackground(java.awt.Color.lightGray);

        pack();
        setSize(getPreferredSize());
            
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, 
            d.height / 2 - getSize().height / 2));

        angelChit = new Chit(2 * scale, scale, scale, 
            Creature.angel.getImageName(),
            this, false);
        archangelChit = new Chit(5 * scale, scale, scale, 
            Creature.archangel.getImageName(), this, false);
        // X out chits since no legion is selected.
        angelChit.setDead(true);
        archangelChit.setDead(true);

        tracker = new MediaTracker(this);
        tracker.addImage(angelChit.getImage(), 0);
        tracker.addImage(archangelChit.getImage(), 0);
        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            JOptionPane.showMessageDialog(board, "waitForAll was interrupted");
        }

        button1 = new JButton("Summon");
        button2 = new JButton("Cancel");
        contentPane.add(button1);
        contentPane.add(button2);
        button1.addActionListener(this);
        button2.addActionListener(this);

        imagesLoaded = true;
        setVisible(true);
        repaint();
    }


    Legion getLegion()
    {
        return legion;
    }


    void cleanup(boolean summoned)
    {
        if (summoned)
        {
            // Only one angel can be summoned per turn.
            player.disallowSummoningAngel();
            legion.markSummoned();
            player.setLastLegionSummonedFrom(donor);
        }

        board.finishSummoningAngel();

        dispose();
    }


    public void paint(Graphics g)
    {
        if (!imagesLoaded)
        {
            return;
        }

        donor = player.getSelectedLegion();
        if (donor != null)
        {
            int angels = donor.numCreature(Creature.angel);
            angelChit.setDead(angels == 0);

            int archangels = donor.numCreature(Creature.archangel);
            archangelChit.setDead(archangels == 0);
        }

        angelChit.paint(g);
        archangelChit.paint(g);

        if (!laidOut)
        {
            Insets insets = getInsets();
            Dimension d = getSize();
            button1.setBounds(insets.left + d.width / 9, 3 * d.height / 4 - 
                insets.bottom, d.width / 3, d.height / 8);
            button2.setBounds(5 * d.width / 9 - insets.right, 
                3 * d.height / 4 - insets.bottom, d.width / 3, d.height / 8);
            laidOut = true;
        }

        button1.repaint();
        button2.repaint();
    }


    public void mousePressed(MouseEvent e)
    {
        donor = player.getSelectedLegion();
        if (donor == null)
        {
            return;
        }

        Point point = e.getPoint();

        if (angelChit.select(point) && !angelChit.isDead())
        {
            donor.removeCreature(Creature.angel);
            legion.addCreature(Creature.angel);
            cleanup(true);
        }
        
        else if (archangelChit.select(point) && !archangelChit.isDead())
        {
            donor.removeCreature(Creature.archangel);
            legion.addCreature(Creature.archangel);
            cleanup(true);
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
        cleanup(false);
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
            donor = player.getSelectedLegion();
            if (donor == null) 
            {
                JOptionPane.showMessageDialog(board, "Must select a legion.");
                return;
            }

            int angels = donor.numCreature(Creature.angel);
            int archangels = donor.numCreature(Creature.archangel);

            if (angels == 0 && archangels == 0)
            {
                JOptionPane.showMessageDialog(board, "No angels are available.");
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
                // If both are available, make the player click on one.
                JOptionPane.showMessageDialog(board, 
                    "Select angel or archangel.");
                return;
            }

            cleanup(true);
        }

        else if (e.getActionCommand() == "Cancel")
        {
            cleanup(false);
        }
    }


    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(8 * scale, 3 * scale);
    }
}
