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
    private String markerId;
    private Chit angelChit;
    private Chit archangelChit;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private JButton button1;
    private JButton button2;
    private static boolean active;
    private Client client;


    private SummonAngel(Client client, String markerId, String longMarkerName)
    {
        super(client.getBoard().getFrame(), client.getPlayerName() +
            ": Summon Angel into Legion " + longMarkerName, false);

        this.client = client;
        this.markerId = markerId;

        // Count and highlight legions with summonable angels, and put
        // board into a state where those legions can be selected.
        if (client.getBoard().highlightSummonableAngels(markerId) < 1)
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

        angelChit = new Chit(scale, 
            Creature.getCreatureByName("Angel").getImageName(), this);
        constraints.gridy = 0;
        gridbag.setConstraints(angelChit, constraints);
        contentPane.add(angelChit);
        angelChit.addMouseListener(this);

        archangelChit = new Chit(scale, 
            Creature.getCreatureByName("Archangel").getImageName(),
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

    public static SummonAngel summonAngel(Client client, String markerId,
        String longMarkerName)
    {
        if (!active)
        {
            active = true;
            return new SummonAngel(client, markerId, longMarkerName);
        }
        return null;
    }


    public String getMarkerId()
    {
        return markerId;
    }

    private void cleanup(String donorId, String angel)
    {
        client.doSummon(markerId, donorId, angel);
        dispose();
        active = false;
    }


    public void mousePressed(MouseEvent e)
    {
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }
        Object source = e.getSource();
        if (angelChit == source && !angelChit.isDead())
        {
            cleanup(donorId, "Angel");
        }
        else if (archangelChit == source && !archangelChit.isDead())
        {
            cleanup(donorId, "Archangel");
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
        String donorId = client.getDonorId();
        if (donorId == null)
        {
            return;
        }

        angelChit.setDead(!client.donorHasAngel());
        archangelChit.setDead(!client.donorHasArchangel());
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Summon"))
        {
            String donorId = client.getDonorId();
            if (donorId == null)
            {
                client.showMessageDialog("Must select a legion.");
                return;
            }

            boolean angels = client.donorHasAngel();
            boolean archangels = client.donorHasArchangel();

            if (!angels && !archangels)
            {
                client.showMessageDialog("No angels are available.");
                return;
            }

            if (!archangels)
            {
                // Must take an angel.
                cleanup(donorId, "Angel");
            }
            else if (!angels)
            {
                // Must take an archangel.
                cleanup(donorId, "Archangel");
            }
            else
            {
                // If both are available, make the player click on one.
                client.showMessageDialog("Select angel or archangel.");
            }
        }

        else if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
