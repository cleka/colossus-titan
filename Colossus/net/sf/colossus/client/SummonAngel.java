package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Allows a player to summon an angel or archangel.
 * @version $Id$
 * @author David Ripton
 */


final class SummonAngel extends JDialog implements MouseListener,
    ActionListener, WindowListener
{
    private String markerId;
    private Chit angelChit;
    private Chit archangelChit;
    private JButton cancelButton;
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
        contentPane.setLayout(new FlowLayout());

        pack();

        setBackground(Color.lightGray);

        int scale = 4 * Scale.get();

        angelChit = new Chit(scale, "Angel", this);
        contentPane.add(angelChit);
        angelChit.addMouseListener(this);

        archangelChit = new Chit(scale, "Archangel", this);
        contentPane.add(archangelChit);
        archangelChit.addMouseListener(this);

        // X out chits since no legion is selected.
        angelChit.setDead(true);
        archangelChit.setDead(true);

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        contentPane.add(cancelButton);
        cancelButton.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }

    static SummonAngel summonAngel(Client client, String markerId,
        String longMarkerName)
    {
        if (!active)
        {
            active = true;
            return new SummonAngel(client, markerId, longMarkerName);
        }
        return null;
    }


    String getMarkerId()
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
    void updateChits()
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
        if (e.getActionCommand().equals("Cancel"))
        {
            cleanup(null, null);
        }
    }
}
