import java.awt.*;
import java.awt.event.*;

/**
 * Class SummonAngel allows a player to Summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


class SummonAngel extends Dialog implements MouseListener, ActionListener,
    WindowListener
{
    private MediaTracker tracker;
    private Player player;
    private Legion legion;
    private MasterBoard board;
    private static final int scale = 60;
    private Chit angelChit;
    private Chit archangelChit;
    private boolean imagesLoaded = false;
    private Legion donor;
    private Graphics offGraphics;
    private Dimension offDimension;
    private Image offImage;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();


    SummonAngel(MasterBoard board, Legion legion)
    {
        super(board, legion.getPlayer().getName() +
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
        if (board.highlightSummonableAngels(legion) < 1)
        {
            cleanup(null);
            return;
        }

        addMouseListener(this);
        addWindowListener(this);

        setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);
        setResizable(false);

        angelChit = new Chit(scale, Creature.angel.getImageName(), this);
        constraints.gridy = 0;
        gridbag.setConstraints(angelChit, constraints);
        add(angelChit);
        angelChit.addMouseListener(this);

        archangelChit = new Chit(scale, Creature.archangel.getImageName(),
            this);
        constraints.gridy = 0;
        gridbag.setConstraints(archangelChit, constraints);
        add(archangelChit);
        archangelChit.addMouseListener(this);

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
            new MessageBox(board, e.toString() +
                "waitForAll was interrupted");
        }
        imagesLoaded = true;

        Button button1 = new Button("Summon");
        Button button2 = new Button("Cancel");
        constraints.gridy = 1;
        gridbag.setConstraints(button1, constraints);
        add(button1);
        button1.addActionListener(this);
        gridbag.setConstraints(button2, constraints);
        add(button2);
        button2.addActionListener(this);

        pack();
        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    Legion getLegion()
    {
        return legion;
    }


    private void cleanup(Creature creature)
    {
        // Sanity check, just in case this got called twice.
        if (creature != null && legion.canSummonAngel())
        {
            // Only one angel can be summoned per turn.
            player.disallowSummoningAngel();
            legion.markSummoned();
            player.setLastLegionSummonedFrom(donor);

            // Move the angel or archangel.
            donor.removeCreature(creature);
            legion.addCreature(creature);

            // Update the number of creatures displayed in both stacks.
            donor.getCurrentHex().repaint();
            legion.getCurrentHex().repaint();
        }

        dispose();

        // Let the MasterBoard know to leave the angel-summoning state.
        board.finishSummoningAngel();
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

        // Prevent repaint loop by only calling repaint if the
        // donor is new.
        Legion oldDonor = donor;
        donor = player.getSelectedLegion();
        if (donor != null && donor != oldDonor)
        {
            int angels = donor.numCreature(Creature.angel);
            angelChit.setDead(angels == 0);
            angelChit.repaint();

            int archangels = donor.numCreature(Creature.archangel);
            archangelChit.setDead(archangels == 0);
            archangelChit.repaint();
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
        donor = player.getSelectedLegion();
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


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Summon"))
        {
            donor = player.getSelectedLegion();
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
                new MessageBox(board, "Select angel or archangel.");
            }
        }

        else if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null);
        }
    }
}
